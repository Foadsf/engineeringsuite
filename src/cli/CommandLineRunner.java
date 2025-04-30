package cli;

import evaluation.DiffAndEvaluator;
import gui.Config;
import gui.SolverGUI; // To access PopUpError/Warning replacements
import gui.SaveLoad;
import gui.MaterialMethods; // Needed for parsing
import tarjan.PrepareMatrix;
import String2ME.CheckString;
import String2ME.EqStorer;
import String2ME.VString;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Collections; // Import missing Collections class
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

public class CommandLineRunner {

  // Flag to indicate if any error occurred during the process
  private static boolean executionError = false;

  public static void main(String[] args) {
    System.out.println("Engineering Suite CLI Mode");
    System.out.println("==========================");

    if (args.length != 1) {
      printUsage();
      System.exit(1);
    }

    String risFilePath = args[0];
    File risFile = new File(risFilePath);
    if (!risFile.exists() || !risFile.isFile()) {
      System.err.println("ERROR: Input file not found or is not a valid file: " + risFilePath);
      System.exit(1);
    }

    // --- Initialization ---
    try {
      System.out.println("Initializing...");
      Config.getAbsolutePath(); // Ensure path is set if needed (Linux)
      Config config = new Config();
      config.ReadConfig(); // Load solver settings
      Config.makeEpsilon();
      DiffAndEvaluator.PrepareME(); // Initialize symbolic engine
      System.out.println("Using Language: " + Config.Language); // Use loaded language if needed
                                                                // later
      new gui.Translation(); // Load translations (might not be strictly needed for pure console
                             // output)

    } catch (Exception e) {
      System.err.println("ERROR during initialization: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }

    // --- Load .ris File ---
    System.out.println("Loading file: " + risFilePath);
    // We need the raw text content for parsing
    String fileContent = loadRisFileContent(risFilePath);
    if (fileContent == null) {
      System.exit(1); // Error message printed in loadRisFileContent
    }
    // Also load initial values using SaveLoad (modified for console errors)
    SaveLoad loader = new SaveLoad();
    if (!loader.loadInitialValuesFromRis(risFilePath)) {
      System.err.println("Warning: Could not properly load initial values section.");
      // Continue anyway, solver might use defaults
    }

    // --- Parse Equations ---
    System.out.println("Parsing equations...");
    CheckString.PurgeAll(); // Ensure clean state
    MaterialMethods materialMethods = new MaterialMethods(); // Needed for parsing
    CheckString checkStringInstance = new CheckString();
    String equationsText = extractEquationsText(fileContent);
    String cleanedEquations = cleanComments(equationsText);

    if (!parseEquations(cleanedEquations, materialMethods, checkStringInstance)) {
      System.err.println("ERROR: Halting due to parsing errors.");
      System.exit(1);
    }
    System.out.println("Parsing complete.");

    // --- Pre-Check (Informational) ---
    int equationCount = CheckString.Functions.size();
    int initialVarCount = CheckString.Var.getSize();
    int solvedOneVarCount = CheckString.OneEquationVar.size();
    int totalVarCount = initialVarCount + solvedOneVarCount;

    System.out.println("--- Solver Pre-Check ---");
    System.out.println("Equations found: " + equationCount);
    System.out.println("Initial unique variables found: " + initialVarCount);
    System.out.println("Variables solved individually: " + solvedOneVarCount);
    System.out.println("Total unique symbols treated as variables by parser: " + totalVarCount);
    System.out.print("Parser variable list: [ ");
    for (String2ME.VString v : CheckString.Var.Variables) {
      System.out.print(v.getVar() + " ");
    }
    System.out.println("]");

    if (equationCount != totalVarCount) {
      System.out.println(
          "WARNING: Equation count differs from initial variable symbol count. Proceeding anyway.");
    }

    // --- Solve ---
    System.out.println("Starting solver process...");
    try {
      // Reset global error flags before solving
      Config.ErrorFound = false;
      DiffAndEvaluator.TimeLimitExceeded = false;
      SolverGUI.ResidualsHigh = false;
      DiffAndEvaluator.StringErrorEvaluating = null;
      DiffAndEvaluator.IrrealEvaluation = false;

      PrepareMatrix.PreTarjan(); // Solve single-variable equations first
      if (CheckString.Functions.size() > 0 || CheckString.Var.getSize() > 0) {
        // Only run Tarjan/Newton if there are coupled equations left
        PrepareMatrix DF = new PrepareMatrix();
        DF.PreNewton(); // Solves remaining coupled systems
      } else {
        System.out.println("No coupled systems remaining after pre-solving.");
      }
      System.out.println("Solver process finished.");

      // Check for errors flagged during solving
      if (Config.ErrorFound) {
        System.err.println(
            "ERROR: Solver indicated an error occurred during evaluation (check logs/previous messages).");
        executionError = true;
      }
      if (DiffAndEvaluator.TimeLimitExceeded) {
        System.err.println("ERROR: Solver time limit exceeded.");
        executionError = true;
      }

    } catch (Exception e) {
      System.err.println("\nERROR during solving process:");
      e.printStackTrace(System.err);
      executionError = true;
    }

    // --- Output Results ---
    System.out.println("\n--- Results ---");
    printResultsToConsole();

    // --- Output Residuals (Optional) ---
    System.out.println("\n--- Residuals ---");
    printResidualsToConsole(); // Needs implementation similar to GUI

    // --- Final Status ---
    if (executionError) {
      System.err.println("\nExecution finished with errors.");
      System.exit(1);
    } else {
      if (SolverGUI.ResidualsHigh) {
        System.out.println(
            "\nWARNING: Execution finished, but residuals were high. Solution might be inaccurate.");
      } else {
        System.out.println("\nExecution finished successfully.");
      }
      System.exit(0);
    }

  } // End main

  private static void printUsage() {
    System.err.println("Usage: java -cp <classpath> cli.CommandLineRunner <path_to_ris_file>");
    System.err.println(
        "Example: java -cp \".;bin;Dependencies\\*\" cli.CommandLineRunner examples\\01_Introduction.ris");
  }

  // Helper to load the whole file content
  private static String loadRisFileContent(String filePath) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append(System.lineSeparator());
      }
      return content.toString();
    } catch (IOException e) {
      System.err.println("ERROR: Cannot read input file '" + filePath + "': " + e.getMessage());
      return null;
    }
  }

  // Helper to extract only the equation part
  private static String extractEquationsText(String fullContent) {
    int markerPos = fullContent.indexOf("@$@%@EndOfEquationData@$@%@");
    if (markerPos != -1) {
      return fullContent.substring(0, markerPos);
    } else {
      System.err.println(
          "WARNING: EndOfEquationData marker not found. Treating whole file as equations.");
      return fullContent; // Or handle as error?
    }
  }

  // --- Need to reimplement/adapt these methods from SolverGUI or similar ---

  // Replicates comment cleaning logic
  private static String cleanComments(String input) {
    // (Copy the exact logic from SolverGUI.cleanComments method)
    boolean comments = false;
    char c;
    char pc = CheckString.Espacio;
    String aux = new String("");
    StringReader J = new StringReader(input);
    BufferedReader BufJ = new BufferedReader(J);
    String s = new String("");
    int count = 0, line = 0;

    for (int i = 0; i < input.length(); i++) {

      try {// this is only to introduce line-separators while ignoring comments
        if (count == line)
          s = BufJ.readLine();
        if (s != null) {
          line = s.length();
        } else
          line = 0;
      } catch (Exception e) {
        line = 0;
      }

      c = input.charAt(i);
      count++;

      if (pc == CheckString.Slash & c == CheckString.Por) {
        // This is to erase the previous character that was inserted in aux
        aux = aux.substring(0, aux.length() - 1);
        comments = true;
        pc = CheckString.Espacio;
      }

      // Add character only if not inside comments or if it's a newline
      // Add newline even in comments to maintain structure for line number errors
      if (!comments || c == '\n' || c == '\r') {
        if (!(pc == CheckString.Por && c == CheckString.Slash)) { // Don't add the closing */ itself
          aux += c;
        }
      }

      if (pc == CheckString.Por & c == CheckString.Slash)
        comments = false;

      if (c == '\n' || (c == '\r' && (i + 1 == input.length() || input.charAt(i + 1) != '\n'))) {
        count = 0; // Reset line character count on newline
        line = 0; // Force reading next line
      }

      // save previous character
      pc = c;
    }
    try {
      BufJ.close();
      J.close();
    } catch (IOException e) {
    }
    return aux;
  }

  // Replicates parsing logic, printing errors to System.err
  private static boolean parseEquations(String equationsText, MaterialMethods materialMethods,
      CheckString checkStringInstance) {
    String line;
    int lineNumber = 0;
    boolean success = true;
    BufferedReader reader = new BufferedReader(new StringReader(equationsText));
    try {
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        String originalLine = line; // Keep original for error reporting context if needed
        // Erase tabs and spaces for parsing check, but keep original for error context
        line = line.replace(CheckString.Tab, CheckString.Espacio);
        line = line.trim(); // Trim leading/trailing whitespace for the line itself

        if (line.isEmpty())
          continue; // Skip empty lines

        String processedLine = line.replace(" ", ""); // Remove internal spaces for check

        // Substitute thermodynamic functions first
        String2ME.GramErr thermoResult = SolverGUI.searchThermodynamicFunctionCli(processedLine,
            materialMethods, checkStringInstance);
        if (thermoResult.GetTypeError() != 0) {
          System.err.println("ERROR in Thermodynamic Function call on line " + lineNumber
              + ": Substance/Property not found in '" + processedLine + "'");
          executionError = true; // Mark error
          success = false;
          continue; // Skip further checks on this line
        }
        processedLine = thermoResult.getString(); // Use the potentially substituted formula

        // Save variables with case info (does not affect solving directly but populates
        // CaseVariables list)
        checkStringInstance.getVariables(processedLine);

        // Perform grammar check on the processed line
        String2ME.GramErr gramResult = checkStringInstance.GramCheck(processedLine);

        // Check grammar result
        if (!checkGramCli(gramResult, lineNumber, originalLine)) {
          success = false; // Mark failure if checkGramCli reported an error
          // No need to set executionError here, checkGramCli does it
        }
      }
    } catch (IOException e) {
      System.err.println("ERROR reading processed equations: " + e.getMessage());
      executionError = true;
      return false; // Indicate failure
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
      }
    }
    return success; // Return overall status
  }

  // Replicates error checking logic, printing errors to System.err
  private static boolean checkGramCli(String2ME.GramErr gramResult, int lineNumber,
      String originalLine) {
    byte errorType = gramResult.GetTypeError();
    if (errorType == 0) {
      return true; // No error
    }

    // An error occurred, set the flag and print details
    executionError = true;
    System.err.print("ERROR on line " + lineNumber + ": ");

    switch (errorType) {
      case 1:
        System.err.println("Illegal character found <" + gramResult.GetCaracter() + ">");
        break;
      case 2:
        System.err.println("Unexpected character <" + gramResult.GetCaracter()
            + "> (Potential syntax error or duplicate '=')");
        break;
      case 3:
        System.err
            .println("Two or more operators followed near <" + gramResult.GetCaracter() + ">");
        break;
      case 4:
        System.err.println("Dot or comma after a letter near <" + gramResult.GetCaracter() + ">");
        break;
      case 5:
        System.err.println("Equal sign missing.");
        break;
      case 6:
        System.err.println("Parenthesis/bracket mismatch (extra or missing).");
        break;
      case 7:
        System.err.println(
            "Empty function parenthesis or brackets near <" + gramResult.GetCaracter() + ">");
        break;
      case 8:
        System.err.println("Number followed directly by variable/letter (missing operator?) near <"
            + gramResult.getString() + ">");
        break;
      case 9:
        System.err.println(
            "Operator missing operand or at end of line near <" + gramResult.GetCaracter() + ">");
        break;
      case 10:
        System.err.println("Variable name contains invalid characters in <" + gramResult.getString()
            + "> (Only letters, numbers, '_' allowed; must start with letter)");
        break;
      default:
        System.err.println("Unknown parsing error code: " + errorType);
        break;
    }
    System.err.println("  Context: " + originalLine.trim());
    return false; // Indicate error
  }

  // Helper to format and print results
  private static void printResultsToConsole() {
    NumberFormat AvgFormat = new DecimalFormat("####0.#######");
    NumberFormat SmallFormat = new DecimalFormat("0.#####E0");
    NumberFormat BigFormat = new DecimalFormat("00000.#####E0");

    // Combine variables solved individually and those solved by solver
    java.util.List<String2ME.VString> allVars =
        new java.util.ArrayList<>(CheckString.Var.Variables);
    allVars.addAll(CheckString.OneEquationVar);
    // Sort alphabetically (optional, but nice)
    Collections.sort(allVars);

    if (allVars.isEmpty() && !executionError) {
      System.out.println("No variables to solve for or all were inputs.");
      return;
    } else if (allVars.isEmpty() && executionError) {
      System.out.println("No results to display due to errors during execution.");
      return;
    }

    System.out.println("Variable          Value");
    System.out.println("----------------|-------------------");

    for (VString vs : allVars) {
      String varName = getVariableCase(vs.getVar()); // Get original case
      varName = varName.replace("Gg", "_"); // Translate back underscore
      double result;
      String resultStr;

      try {
        result = DiffAndEvaluator.Evaluate(vs.getVar()); // Evaluate using internal (lowercase) name

        if (Math.abs(result) < 100000 && Math.abs(result) >= 1e-5)
          resultStr = AvgFormat.format(result);
        else if (Math.abs(result) < 1e-5 && Math.abs(result) != 0)
          resultStr = SmallFormat.format(result);
        else if (Math.abs(result) >= 100000)
          resultStr = BigFormat.format(result);
        else // Handle exactly zero
          resultStr = AvgFormat.format(result);

      } catch (Exception e) {
        resultStr = "Error evaluating (" + e.getMessage() + ")";
        executionError = true; // Mark error if evaluation fails post-solve
      }

      System.out.printf("%-15s | %s%n", varName, resultStr);
    }
  }

  // Helper to get original case variable name
  private static String getVariableCase(String lowerCaseVar) {
    for (String s : CheckString.CaseVariables) {
      if (lowerCaseVar.equalsIgnoreCase(s)) {
        return s;
      }
    }
    return lowerCaseVar; // Fallback if not found (shouldn't happen ideally)
  }

  // Helper to format and print residuals
  private static void printResidualsToConsole() {
    if (CheckString.FunctionsSolved.isEmpty() && !executionError) { // Also check executionError
                                                                    // flag
      System.out.println("No equations were solved or available to calculate residuals.");
      return;
    } else if (CheckString.FunctionsSolved.isEmpty()) {
      System.out.println("Cannot calculate residuals due to previous errors.");
      return;
    }


    NumberFormat AvgFormat = new DecimalFormat("##0.#####");
    NumberFormat SmallFormat = new DecimalFormat("0.#####E0");
    NumberFormat BigFormat = new DecimalFormat("#####0.##E0");

    System.out.println("Equation Residuals (Internal Form = 0, Target: 0)"); // Clarify it's
                                                                             // internal form
    System.out.println("------------------------------------------------------------");

    Iterator<EqStorer> it = CheckString.FunctionsSolved.iterator();

    while (it.hasNext()) {
      EqStorer eqaux = it.next();
      String internalEquation = eqaux.getEquation(); // This is like F(x) - G(x)
      double residual = 0.0;
      String residualStr = "";
      String displayEquation = ""; // Initialize

      try {
        // Calculate residual first
        residual = DiffAndEvaluator.Evaluate("N(" + internalEquation + ")");

        // Format residual value
        if (Math.abs(residual) < 10000 && Math.abs(residual) >= 1e-5)
          residualStr = AvgFormat.format(residual);
        else if (Math.abs(residual) < 1e-5 && Math.abs(residual) != 0)
          residualStr = SmallFormat.format(residual);
        else if (Math.abs(residual) >= 10000)
          residualStr = BigFormat.format(residual);
        else // Handle exactly zero
          residualStr = AvgFormat.format(residual);

        // --- Display internal form directly ---
        displayEquation = internalEquation.replace("Gg", "_") + " = 0";
        // Attempt to replace internal variable names with original case for readability
        displayEquation = replaceVarsWithCase(displayEquation);
        // --- End direct display ---

      } catch (Exception e) {
        // Error during residual *evaluation*
        residualStr = "Error evaluating (" + e.getClass().getSimpleName() + ")"; // Show exception
                                                                                 // type
        displayEquation = internalEquation.replace("Gg", "_") + " = 0 (Eval Error)";
        displayEquation = replaceVarsWithCase(displayEquation);
        executionError = true; // Mark error occurred during residual calc
      }

      // Print, ensuring displayEquation isn't too long for formatting
      System.out.printf("%-60s | Residual: %s%n",
          displayEquation.substring(0, Math.min(displayEquation.length(), 60)), residualStr);

      // Check if residual is high *only if* evaluation succeeded
      if (!residualStr.contains("Error") && Math.abs(residual) > Config.Precision * 10) { // Use a
                                                                                          // slightly
                                                                                          // larger
                                                                                          // tolerance
                                                                                          // for
                                                                                          // warning
        SolverGUI.ResidualsHigh = true; // Set flag if any residual is high
      }
    } // end while

    if (SolverGUI.ResidualsHigh) {
      System.out.println("\nWARNING: One or more residuals are high, solution may be inaccurate.");
    }
  }

  // Helper copied/adapted from SolverGUI - finds the '(-1*(' substitution
  // position
  private static int findEqualConsole(String s) {
    final char OpenP = '(';
    final char CloseP = ')';
    final String marker = "-1*("; // CheckString.SubsEqual; - using literal for clarity

    int markerPos = s.indexOf(marker);
    if (markerPos == -1)
      return -1; // Marker not found

    int parenLevel = 0;
    int i = markerPos + marker.length(); // Start searching after the marker

    // We are looking for the matching closing parenthesis for the one AFTER the
    // marker
    // Find the first '(' after marker
    int firstOpenParenAfterMarker = -1;
    for (int startSearch = markerPos + marker.length(); startSearch < s.length(); startSearch++) {
      if (s.charAt(startSearch) == OpenP) {
        firstOpenParenAfterMarker = startSearch;
        parenLevel = 1; // Start counting from this parenthesis
        i = firstOpenParenAfterMarker + 1;
        break;
      }
    }
    if (firstOpenParenAfterMarker == -1)
      return -1; // No opening parenthesis found after marker

    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == OpenP) {
        parenLevel++;
      } else if (c == CloseP) {
        parenLevel--;
        if (parenLevel == 0) {
          // Found the matching closing parenthesis for the one after the marker
          // The original '=' was just before the marker
          return markerPos;
        }
      }
      i++;
    }
    return -1; // Matching parenthesis not found
  }

  // Need a CLI version of this helper from SolverGUI
  public static String2ME.GramErr searchThermodynamicFunctionCli(String input,
      MaterialMethods Materiales, CheckString ch) {
    // Simplified version of the GUI one
    try {
      if (SolverGUI.checkSubstanceCli(input, Materiales)) { // Need this helper too
        // (Rest of logic from SolverGUI.searchThermodynamicFunction - find material,
        // property, vars, substitute)
        // ... Implementation of substitution logic needed here ...
        // If successful:
        // String substitutedFormula = ... result of substitution ...;
        // return new String2ME.GramErr((byte) 0, substitutedFormula);
        // If property/material not found after initial check:
        // return new String2ME.GramErr((byte) 1, input); // Error code 1 for not found

        // Placeholder - Full implementation required
        System.out.println("DEBUG: Thermo function call detected: " + input
            + " (Substitution logic not fully implemented in CLI)");
        return new String2ME.GramErr((byte) 0, input); // Pass through for now

      }
      return new String2ME.GramErr((byte) 0, input); // Not a thermo function call

    } catch (Exception e) {
      System.err.println("Error during thermodynamic function check for: " + input);
      return new String2ME.GramErr((byte) 0, input); // Treat as non-thermo on error
    }
  }

  // Need a CLI version of this helper from SolverGUI
  public static boolean checkSubstanceCli(String input, MaterialMethods Materiales) {
    int pos = input.indexOf(".");
    if (pos <= 0 || pos == input.length() - 1) { // Ensure dot exists and isn't first/last char
      return false;
    }
    String potentialSubstance = input.substring(0, pos);
    // Check against known materials (case-insensitive)
    for (String knownMaterial : Materiales.getMaterials()) {
      if (potentialSubstance.equalsIgnoreCase(knownMaterial)) {
        // Now check if the next part looks like a function call
        int openParen = input.indexOf('(', pos);
        if (openParen > pos + 1) { // Must be at least one char for property name
          int closeParen = input.lastIndexOf(')');
          if (closeParen > openParen) { // Basic check for parenthesis pair
            return true;
          }
        }
      }
    }
    return false;
  }

  private static String replaceVarsWithCase(String equation) {
    String result = equation;
    // Sort by length descending to replace longer names first (e.g., Temp before T)
    java.util.List<String> sortedVars = new java.util.ArrayList<>(CheckString.CaseVariables);
    Collections.sort(sortedVars, (s1, s2) -> Integer.compare(s2.length(), s1.length()));

    for (String originalCaseVar : sortedVars) {
      // Use regex word boundaries (\b) to avoid partial replacements (e.g., replacing 't' in 'nt')
      // Need to escape regex special characters if variable names contain them (unlikely here)
      result = result.replaceAll("\\b(?i)" + originalCaseVar.replace("_", "Gg") + "\\b",
          originalCaseVar);
    }
    return result;
  }

} // End CommandLineRunner Class
