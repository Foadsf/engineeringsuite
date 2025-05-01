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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections; // Import missing Collections class
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashSet;
import solver.ODEProblemDefinition;
import gui.MaterialList;
import gui.MaterialStore;

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

  // --- COMPLETE Method parseEquations (with debugging added) ---
  /**
   * Replicates parsing logic, printing errors to System.err. Reads equations line by line, checks
   * thermodynamics, performs grammar check, and populates static lists in CheckString (Var,
   * Functions, OdeProblems, CaseVariables). Includes debugging output after the loop.
   *
   * @param equationsText The block of text containing equations.
   * @param materialMethods Instance for thermodynamic lookups.
   * @param checkStringInstance An instance of CheckString (used for calling non-static checkGram).
   * @return true if parsing completed without fatal errors, false otherwise.
   */
  private static boolean parseEquations(String equationsText, MaterialMethods materialMethods,
      CheckString checkStringInstance) {
    String line;
    int lineNumber = 0;
    boolean success = true; // Assume success initially
    BufferedReader reader = new BufferedReader(new StringReader(equationsText));
    try {
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        String originalLine = line; // Keep original for error reporting context
        // Basic cleaning: remove tabs, trim leading/trailing whitespace
        line = line.replace(CheckString.Tab, CheckString.Espacio);
        line = line.trim();

        if (line.isEmpty())
          continue; // Skip empty lines

        // Create version without internal spaces for processing
        String processedLineNoSpaces = line.replace(" ", "");

        // --- Thermodynamic Substitution ---
        String2ME.GramErr thermoResult = searchThermodynamicFunctionCli(processedLineNoSpaces,
            materialMethods, checkStringInstance); // Use the static CLI version
        if (thermoResult.GetTypeError() != 0) {
          System.err.println("ERROR in Thermodynamic Function call on line " + lineNumber
              + ": Substance/Property not found in '" + processedLineNoSpaces + "'");
          executionError = true; // Mark global error
          success = false;
          continue; // Skip further checks for this line
        }
        String lineForGramCheck = thermoResult.getString(); // Use potentially substituted string

        // --- Grammar Check and Variable/Equation Storage ---
        // GramCheck handles ODE detection, populating CaseVariables, Var, Functions, OdeProblems
        String2ME.GramErr gramResult = checkStringInstance.GramCheck(lineForGramCheck); // Pass
                                                                                        // processed
                                                                                        // line

        // --- Check Grammar Result ---
        if (!checkGramCli(gramResult, lineNumber, originalLine)) {
          success = false; // Mark overall parsing as failed if checkGramCli reported error
        }
      } // End while loop through lines

      // --- ADD DEBUGGING HERE ---
      System.out.println("DEBUG: Parsing loop finished. Contents of CheckString.Var.Variables:");
      System.out.print("       [ ");
      if (CheckString.Var != null && CheckString.Var.Variables != null) {
        for (VString v : CheckString.Var.Variables) {
          // Print internal name and count
          System.out.print("'" + v.getVar() + "' (" + v.getCount() + "), ");
        }
      } else {
        System.out.print("CheckString.Var or Var.Variables is null!");
      }
      System.out.println("]");

      System.out.println("DEBUG: Contents of CheckString.OneEquationVar:");
      System.out.print("       [ ");
      if (CheckString.OneEquationVar != null) {
        for (VString v : CheckString.OneEquationVar) {
          // Print internal name and count
          System.out.print("'" + v.getVar() + "' (" + v.getCount() + "), ");
        }
      } else {
        System.out.print("CheckString.OneEquationVar is null!");
      }
      System.out.println("]");


      System.out.println("DEBUG: Contents of CheckString.CaseVariables:");
      System.out.print("       [ ");
      if (CheckString.CaseVariables != null) {
        for (String s : CheckString.CaseVariables) {
          System.out.print("'" + s + "', ");
        }
      } else {
        System.out.print("CheckString.CaseVariables is null!");
      }
      System.out.println("]");
      // --- END DEBUGGING ---

    } catch (IOException e) {
      System.err.println("ERROR reading processed equations: " + e.getMessage());
      executionError = true; // Mark global error
      return false; // Indicate failure
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        /* Ignore close error */ }
    }
    return success; // Return overall status (true if no fatal errors encountered)
  }
  // --- End of Method parseEquations ---


  // --- COMPLETE Method checkGramCli ---
  /**
   * Replicates error checking logic from SolverGUI, printing errors to System.err and setting the
   * global executionError flag.
   * 
   * @param gramResult The result from CheckString.GramCheck.
   * @param lineNumber The line number where the potential error occurred.
   * @param originalLine The original text of the line for context.
   * @return true if no error, false if an error was reported.
   */
  private static boolean checkGramCli(String2ME.GramErr gramResult, int lineNumber,
      String originalLine) {
    byte errorType = gramResult.GetTypeError();
    if (errorType == 0) {
      return true; // No error
    }

    // An error occurred, set the global flag and print details
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
    // Provide context from the original line
    System.err.println("  Context: " + originalLine.trim());
    return false; // Indicate error
  }
  // --- End of Method checkGramCli ---


  // --- COMPLETE Method printResultsToConsole (as provided previously) ---
  /**
   * Helper to format and print results to the console. Skips evaluation and printing of internal
   * ODE integration variables.
   */
  private static void printResultsToConsole() {
    NumberFormat AvgFormat = new DecimalFormat("####0.#######");
    NumberFormat SmallFormat = new DecimalFormat("0.#####E0");
    NumberFormat BigFormat = new DecimalFormat("00000.#####E0");

    // Keep track if any *non-skipped* variable fails evaluation
    boolean evaluationFailedForSomeVariable = false;

    // Combine variables solved individually and those from the main solver list
    List<String2ME.VString> allVars = new ArrayList<>(CheckString.Var.Variables);
    allVars.addAll(CheckString.OneEquationVar);
    // Sort alphabetically for consistent output
    Collections.sort(allVars);

    if (allVars.isEmpty() && !executionError) { // executionError is the global flag for *prior*
                                                // errors
      System.out.println("No variables to solve for or all were inputs.");
      return;
    } else if (allVars.isEmpty() && executionError) {
      System.out.println("No results to display due to errors during execution.");
      return;
    }

    // --- Identify ONLY integration variables to skip ---
    Set<String> odeIntegrationVarsToSkip = new HashSet<>();
    if (CheckString.OdeProblems != null) { // Check if the ODE list has been initialized
      for (ODEProblemDefinition odeDef : CheckString.OdeProblems) {
        if (odeDef.getIntegrationVariable() != null) {
          odeIntegrationVarsToSkip.add(odeDef.getIntegrationVariable().toLowerCase());
        }
      }
    }
    System.out.println("DEBUG: ODE Integration Vars to skip printing: " + odeIntegrationVarsToSkip);
    // --- End identification ---

    System.out.println("Variable          Value");
    System.out.println("----------------|-------------------");

    for (VString vs : allVars) {
      String varNameLower = vs.getVar(); // Internal name is lowercase (might contain Gg)
      String varNameOriginalCase = getVariableCase(varNameLower); // Get original case for display

      // --- Skip evaluation/printing logic (Simpler: Only skip integration var) ---
      if (odeIntegrationVarsToSkip.contains(varNameLower)) {
        System.out.println("DEBUG: Skipping evaluation of internal ODE integration variable: "
            + varNameOriginalCase);
        continue; // Skip to the next variable in the loop
      }
      // --- End skip logic ---

      double result;
      String resultStr;

      try {
        // Evaluate using internal (lowercase, potentially Gg) name
        result = DiffAndEvaluator.Evaluate(varNameLower);

        // Format the numerical result
        if (Double.isNaN(result) || Double.isInfinite(result)) {
          resultStr = "Non-numeric (" + result + ")";
          evaluationFailedForSomeVariable = true; // Consider NaN/Inf as evaluation failure
        } else if (Math.abs(result) < 100000 && Math.abs(result) >= 1e-5) {
          resultStr = AvgFormat.format(result);
        } else if (Math.abs(result) < 1e-5 && Math.abs(result) != 0) {
          resultStr = SmallFormat.format(result);
        } else if (Math.abs(result) >= 100000) {
          resultStr = BigFormat.format(result);
        } else { // Handle exactly zero
          resultStr = AvgFormat.format(result);
        }

      } catch (Exception e) {
        // Catch evaluation errors (like for unevaluated ODE targets or other issues)
        resultStr = "Error evaluating (" + e.getMessage() + ")";
        evaluationFailedForSomeVariable = true; // Mark that an evaluation failed
      }

      // Print the result line using the original-case name
      System.out.printf("%-15s | %s%n", varNameOriginalCase, resultStr);
    } // End of for loop through variables

    // After checking all variables, update the global error flag if any evaluation failed
    if (evaluationFailedForSomeVariable) {
      executionError = true; // Set the global flag indicating issues in this phase
    }
  }
  // --- End of Method printResultsToConsole ---

  // --- COMPLETE Method getVariableCase ---
  /**
   * Helper to get original case variable name from the CaseVariables list, given the internal name
   * (lowercase, potentially with 'Gg').
   * 
   * @param internalVarName The internal variable name (e.g., "tggstart", "k").
   * @return The original case name (e.g., "t_start", "k") or the internal name with 'Gg' converted
   *         back to '_' if no exact match found.
   */
  private static String getVariableCase(String internalVarName) {
    if (internalVarName == null)
      return "null_internal_var"; // Safety check

    // Convert internal name (like 'tggstart') back to original style ('t_start') for comparison
    String nameWithUnderscore = internalVarName.replace("Gg", "_");

    if (CheckString.CaseVariables != null) {
      for (String originalCaseName : CheckString.CaseVariables) {
        // Case-insensitive comparison between underscore version and stored original case
        if (nameWithUnderscore.equalsIgnoreCase(originalCaseName)) {
          return originalCaseName; // Return the originally cased name found
        }
      }
    }
    // Fallback: If no match found in CaseVariables (should generally not happen
    // for variables defined in equations, but might for function internals?)
    // Return the internal name, but ensure Gg is converted back to _ for display.
    // System.err.println("Warning: Could not find original case for internal var: " +
    // internalVarName + " (using '" + nameWithUnderscore + "')");
    return nameWithUnderscore;
  }
  // --- End of Method getVariableCase ---

  // --- COMPLETE Method printResidualsToConsole ---
  /**
   * Helper to format and print residuals. Attempts to reconstruct the original equation form for
   * display.
   */
  private static void printResidualsToConsole() {
    // Combine solved functions if needed (assuming FunctionsSolved holds all relevant ones)
    List<EqStorer> solvedFunctions = new ArrayList<>(CheckString.FunctionsSolved);
    // If ODEs were solved and added to a separate list, include them here if desired.
    // For now, assume FunctionsSolved contains what we need from algebraic solver.

    if (solvedFunctions.isEmpty()) {
      System.out.println("No equations were solved to calculate residuals.");
      return;
    }

    NumberFormat AvgFormat = new DecimalFormat("##0.#####");
    NumberFormat SmallFormat = new DecimalFormat("#.#####E0");
    NumberFormat BigFormat = new DecimalFormat("#####0.##E0");

    System.out.println("Equation Residuals (Internal Form = 0, Target: 0)"); // Clarified header
    System.out.println("------------------------------------------------------------"); // Lengthened
                                                                                        // separator

    Iterator<EqStorer> it = solvedFunctions.iterator();
    CheckString Ch = new CheckString(); // Instance needed for non-static helpers if any

    while (it.hasNext()) {
      EqStorer eqaux = it.next();
      // Evaluate the internal F(x)-G(x)=0 form
      String internalEquation = eqaux.getEquation();
      double residual = 0.0;
      String residualStr = "";
      String displayEquation = "";
      boolean evalErrorOccurred = false;

      try {
        // Evaluate the F(x)-G(x) form directly
        residual = DiffAndEvaluator.Evaluate("N(" + internalEquation + ")");

        if (Double.isNaN(residual) || Double.isInfinite(residual)) {
          residualStr = "Non-numeric (" + residual + ")";
          evalErrorOccurred = true;
        } else if (Math.abs(residual) < 10000 && Math.abs(residual) >= 1e-5) {
          residualStr = AvgFormat.format(residual);
        } else if (Math.abs(residual) < 1e-5 && Math.abs(residual) != 0) {
          residualStr = SmallFormat.format(residual);
        } else if (Math.abs(residual) >= 10000) {
          residualStr = BigFormat.format(residual);
        } else { // Handle exactly zero
          residualStr = AvgFormat.format(residual);
        }

        // Reconstruct display equation (attempt to show something user-friendly)
        int equalsPosInternal = findEqualConsole(internalEquation); // Finds "-1*(" position
        if (equalsPosInternal != -1) {
          // Try to reconstruct A = B from A - 1*(B) = 0
          displayEquation = internalEquation.substring(0, equalsPosInternal) + " = "
              + internalEquation.substring(equalsPosInternal + CheckString.SubsEqual.length(),
                  internalEquation.length() - 1); // Remove
          // trailing
          // ')'
        } else {
          // Assume it was originally in the form F(x) = 0
          displayEquation = internalEquation + " = 0";
        }

        // Clean up internal representations for display
        displayEquation = displayEquation.replace("Degree", ""); // Remove Degree marker if present
        displayEquation = displayEquation.replace("Gg", "_"); // Replace Gg with underscore
        displayEquation = displayEquation.replace("3.141592653589793", "Pi"); // Approximate Pi
                                                                              // display
        displayEquation = displayEquation.replace("2.718281828459045", "E"); // Approximate E
                                                                             // display

        // Attempt to replace internal lowercase/Gg vars with original case vars
        // This is complex - needs to tokenize and lookup in CaseVariables
        displayEquation = replaceInternalVarsWithOriginalCase(displayEquation);


      } catch (Exception e) {
        residualStr = "Error evaluating (" + e.getMessage() + ")";
        displayEquation = internalEquation + " = 0 (Internal Form, Eval Error)"; // Indicate error
        evalErrorOccurred = true;
        // Don't set global executionError here, let the main loop handle it based on printResults
        // failure
      }

      // Print the formatted residual line
      // Truncate display equation if too long for layout
      String truncatedDisplayEq =
          displayEquation.length() > 60 ? displayEquation.substring(0, 57) + "..."
              : displayEquation;
      System.out.printf("%-60s | Residual: %s%n", truncatedDisplayEq, residualStr);

      // Set flag if any residual is high (and wasn't an eval error)
      if (!evalErrorOccurred && Math.abs(residual) > Config.Precision * 10) { // Use tolerance for
                                                                              // warning
        SolverGUI.ResidualsHigh = true;
      }
    } // End while loop

    if (SolverGUI.ResidualsHigh) {
      System.out.println("WARNING: One or more residuals are high, solution may be inaccurate.");
    }
  }
  // --- End of Method printResidualsToConsole ---

  // --- COMPLETE Method findEqualConsole ---
  /**
   * Helper copied/adapted from SolverGUI - finds the '(-1*(' substitution position which represents
   * the original '=' sign in the internal equation format.
   * 
   * @param s The internal equation string (e.g., "a-1*(b)").
   * @return The index where "-1*(" starts, or -1 if not found or format is wrong.
   */
  private static int findEqualConsole(String s) {
    final char OpenP = '(';
    final char CloseP = ')';
    final String marker = "-1*("; // CheckString.SubsEqual - internal representation of '='

    int markerPos = s.indexOf(marker);
    if (markerPos == -1) {
      return -1; // Marker not found, likely equation was F(x)=0 initially
    }

    // The logic here assumes the structure A - 1*(B) where B was the original RHS.
    // It needs to find the matching parenthesis for the one *after* the marker.
    // However, the original '=' sign was located *just before* the marker.
    // So, simply returning the marker position is sufficient.

    // Original complex parenthesis counting logic removed as it wasn't needed
    // if the goal is just to find where the original '=' was.

    return markerPos;
  }
  // --- End of Method findEqualConsole ---

  /**
   * Checks if the input string represents a thermodynamic function call (e.g., "Air.Cp(var1,
   * var2)") and if so, attempts to substitute it with its corresponding formula from the database.
   * Handles variable replacement based on the order defined in the database.
   *
   * @param input The potentially thermodynamic function call string (lowercase, no spaces).
   * @param Materiales The MaterialMethods instance containing the thermodynamic database.
   * @param ch A CheckString instance (needed for helpers).
   * @return A GramErr object. If substitution occurred, GramErr contains the substituted formula
   *         and type 0. If it wasn't a thermo call, it contains the original input and type 0. If
   *         it looked like a thermo call but the substance/property wasn't found or arguments
   *         mismatched, it returns the original input and type 1 (error).
   */
  public static String2ME.GramErr searchThermodynamicFunctionCli(String input,
      MaterialMethods Materiales, CheckString ch) {
    try {
      if (checkSubstanceCli(input, Materiales)) { // Use the static helper
        // --- ADD THESE DECLARATIONS BACK ---
        StringTokenizer lector = new StringTokenizer(input, ".(),", true);
        String aux;
        String material = null; // Declare and initialize
        String property = null; // Declare and initialize
        String PrevToken = null; // Declare and initialize
        LinkedList<String> callingVariables = new LinkedList<String>(); // Declare and initialize
        // --- END ADDED DECLARATIONS ---

        while (lector.hasMoreTokens()) {
          aux = lector.nextToken();
          // Safe check for PrevToken before using equalsIgnoreCase
          if (PrevToken != null) {
            if (aux.equals("."))
              material = PrevToken;
            if (aux.equals("("))
              property = PrevToken;
            if (aux.equals(","))
              callingVariables.add(PrevToken);
            if (aux.equals(")"))
              callingVariables.add(PrevToken);
          }
          PrevToken = aux;
        }

        // Check if material and property were actually found during tokenization
        if (material == null || property == null) {
          System.err.println(
              "Warning: Malformed thermodynamic call syntax (could not identify material/property): "
                  + input);
          return new String2ME.GramErr((byte) 1, input); // Syntax error
        }

        // --- Null check for Materiales and the list it contains ---
        if (Materiales == null || Materiales.getMaterialsList() == null) {
          System.err.println("Error: Material database not loaded or is null.");
          return new String2ME.GramErr((byte) 1, input); // Indicate error
        }

        // Search the database
        for (MaterialList m : Materiales.getMaterialsList()) {
          if (m.getMaterial().equalsIgnoreCase(material)) {
            LinkedList<MaterialStore> properties = m.getPropertyList();
            if (properties == null) {
              System.err.println("Warning: Property list is null for material: " + material);
              return new String2ME.GramErr((byte) 1, input);
            }
            for (MaterialStore ms : properties) {
              if (ms.getProperty().equalsIgnoreCase(property)) {
                // Found match!
                String formula = ms.getFormula();
                String dbVarString = ms.getVariables();
                String[] dbVars = (dbVarString == null || dbVarString.isEmpty()) ? new String[0]
                    : dbVarString.replace(" ", "").split(",");

                // Arity check
                if (callingVariables.size() != dbVars.length) {
                  System.err.println("ERROR: Argument count mismatch for " + material + "."
                      + property + ". Expected " + dbVars.length + " based on '" + dbVarString
                      + "', got " + callingVariables.size());
                  return new String2ME.GramErr((byte) 1, input);
                }

                // Substitute variables from the call into the formula
                String substitutedFormula = substituteThermoVars(formula, dbVars, callingVariables);

                // Add workaround for residuals display if needed later
                // CheckString.ResidualWorkAround.add(new PositionStorer(substitutedFormula,
                // input));
                return new String2ME.GramErr((byte) 0, substitutedFormula);
              }
            }
            // Material found, but property wasn't
            System.err.println(
                "Warning: Property '" + property + "' not found for substance '" + material + "'.");
            return new String2ME.GramErr((byte) 1, input);
          }
        }
        // Material not found
        System.err
            .println("Warning: Substance '" + material + "' not found in thermodynamic database.");
        return new String2ME.GramErr((byte) 1, input);

      }
      // Doesn't look like a substance call
      return new String2ME.GramErr((byte) 0, input);

    } catch (Exception e) {
      System.err.println("Error during thermodynamic function processing for: " + input);
      e.printStackTrace(System.err);
      return new String2ME.GramErr((byte) 1, input); // Return error on unexpected exception
    }
  }
  // --- End of Method searchThermodynamicFunctionCli ---

  // --- COMPLETE Method checkSubstanceCli ---
  /**
   * Checks if the input string *looks like* a potential thermodynamic function call based on the
   * format "Substance.Property(...)". It checks if the potential substance name matches a known
   * material in the database.
   *
   * @param input The input string (ideally lowercase, no spaces).
   * @param Materiales The MaterialMethods instance containing the thermodynamic database.
   * @return true if the format matches and the substance exists, false otherwise.
   */
  public static boolean checkSubstanceCli(String input, MaterialMethods Materiales) {
    int dotPos = input.indexOf(".");
    // Ensure dot exists, is not the first or last char
    if (dotPos <= 0 || dotPos >= input.length() - 1) {
      return false;
    }
    String potentialSubstance = input.substring(0, dotPos);

    // Check against known materials (case-insensitive)
    if (Materiales != null && Materiales.getMaterials() != null) { // Add null checks
      for (String knownMaterial : Materiales.getMaterials()) {
        if (potentialSubstance.equalsIgnoreCase(knownMaterial)) {
          // Found a matching substance name. Now check if it looks like a function call.
          int openParenPos = input.indexOf('(', dotPos);
          // Must have at least one character for property name between '.' and '('
          if (openParenPos > dotPos + 1) {
            int closeParenPos = input.lastIndexOf(')');
            // Basic check for parenthesis pair after the potential property name
            if (closeParenPos > openParenPos) {
              return true; // Looks like Substance.Property(...)
            }
          }
        }
      }
    }
    return false; // Substance name not found or format incorrect
  }
  // --- End of Method checkSubstanceCli ---


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

  // --- COMPLETE Method substituteThermoVars ---
  /**
   * Substitutes database variable names within a formula string with the corresponding variables
   * used in the function call. Substitution is based on order.
   *
   * @param formula The formula string from the database.
   * @param dbVars An array of variable names as defined in the database, in order.
   * @param callVars A LinkedList of variable names as provided in the calling expression, in order.
   * @return The formula string with database variable names replaced by calling variable names.
   */
  private static String substituteThermoVars(String formula, String[] dbVars,
      LinkedList<String> callVars) {
    if (formula == null)
      return ""; // Handle null formula
    if (dbVars == null || callVars == null || dbVars.length != callVars.size()) {
      // Should have been caught by arity check, but double-check
      System.err.println("Internal Error: Mismatched vars in substituteThermoVars.");
      return formula; // Return original formula on error
    }
    if (dbVars.length == 0) {
      return formula; // No variables to substitute
    }

    String result = formula.replace(" ", ""); // Start with formula, no spaces
    StringTokenizer tokenizer = new StringTokenizer(result, "+/*-()[]{} ^=!", true);
    StringBuilder reconstructed = new StringBuilder();
    String token;

    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      int pos = varPositionCli(token, dbVars); // Check if token is a DB variable (case-insensitive)
      if (pos != -1) {
        reconstructed.append(callVars.get(pos)); // Substitute with calling variable
      } else {
        reconstructed.append(token); // Keep original token (operator, number, etc.)
      }
    }
    return reconstructed.toString();
  }
  // --- End of Method substituteThermoVars ---

  // --- COMPLETE Method varPositionCli ---
  /**
   * Finds the position (index) of a variable name within a list of database variable names,
   * ignoring case.
   * 
   * @param var The variable name to find.
   * @param list The array of variable names from the database definition.
   * @return The zero-based index if found, otherwise -1.
   */
  private static int varPositionCli(String var, String[] list) {
    if (var == null || list == null)
      return -1;
    for (int i = 0; i < list.length; i++) {
      if (list[i] != null && list[i].equalsIgnoreCase(var)) {
        return i;
      }
    }
    return -1; // Not found
  }
  // --- End of Method varPositionCli ---

  // --- COMPLETE Method replaceInternalVarsWithOriginalCase ---
  /**
   * Helper method to replace internal variable representations (lowercase, Gg) in a processed
   * equation string with their original user-specified case.
   * 
   * @param processedEquation The equation string potentially containing lowercase/Gg vars.
   * @return The equation string with variables replaced by their original case, if found.
   */
  private static String replaceInternalVarsWithOriginalCase(String processedEquation) {
    if (processedEquation == null || CheckString.CaseVariables == null
        || CheckString.CaseVariables.isEmpty()) {
      return processedEquation; // Nothing to do or no case info available
    }

    // Create a temporary modifiable string
    StringBuilder result = new StringBuilder(processedEquation);

    // Iterate through known original-case variables
    for (String originalCaseVar : CheckString.CaseVariables) {
      // Create the internal representation (lowercase, with Gg for _)
      String internalVar = originalCaseVar.toLowerCase(Locale.ENGLISH).replace("_", "Gg");

      // Find and replace all occurrences of the internal var with the original case var
      // Use regex word boundaries (\b) to avoid partial replacements (e.g., replacing 't' in
      // 't_start')
      // Need to escape regex special chars in the internalVar if any exist (though unlikely with
      // Gg)
      String internalPattern = "\\b" + java.util.regex.Pattern.quote(internalVar) + "\\b";
      // Replacement string needs escaping if it contains $ or \
      String originalEscaped = java.util.regex.Matcher.quoteReplacement(originalCaseVar);

      try {
        // This is tricky with StringBuilder, easier to do on the String directly
        String currentString = result.toString();
        // Use case-insensitive matching since internalVar is lowercase
        String replacedString = java.util.regex.Pattern
            .compile(internalPattern, java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(currentString).replaceAll(originalEscaped);
        if (!replacedString.equals(currentString)) {
          result.setLength(0); // Clear string builder
          result.append(replacedString); // Append replaced string
        }
      } catch (java.util.regex.PatternSyntaxException e) {
        System.err.println("Warning: Regex error trying to replace variable '" + internalVar + "': "
            + e.getMessage());
        // Continue without replacing this specific variable if regex fails
      }
    }
    return result.toString();
  }
  // --- End of Method replaceInternalVarsWithOriginalCase ---

} // End CommandLineRunner Class
