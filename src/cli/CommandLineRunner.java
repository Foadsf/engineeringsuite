package cli;

// Core Imports
import evaluation.DiffAndEvaluator;
import gui.Config;
import gui.SolverGUI;
import gui.SaveLoad;
import gui.MaterialMethods;
import gui.MaterialList; // Import added
import gui.MaterialStore; // Import added
import tarjan.PrepareMatrix;
import String2ME.CheckString;
import String2ME.EqStorer;
import solver.nodo;
import String2ME.VString;
import String2ME.VList; // Import added
import String2ME.InitVal;
import solver.vector; // Import needed for creating vector objects
// Standard Java Imports
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

public class CommandLineRunner {

    private static boolean executionError = false;
    private static java.util.List<EqStorer> equationsForResiduals = new java.util.ArrayList<>();

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
            Config.getAbsolutePath();
            Config config = new Config();
            config.ReadConfig();
            Config.makeEpsilon();
            DiffAndEvaluator.PrepareME();
            System.out.println("Using Language: " + Config.Language);
            new gui.Translation();
        } catch (Exception e) {
            System.err.println("ERROR during initialization: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // --- Load .ris File ---
        System.out.println("Loading file: " + risFilePath);
        String fileContent = loadRisFileContent(risFilePath);
        if (fileContent == null) {
             System.exit(1);
        }
        SaveLoad loader = new SaveLoad(); // Now accessible
        if (!loader.loadInitialValuesFromRis(risFilePath)) {
             System.err.println("Warning: Potential issues encountered while loading initial values section.");
        }

        // --- Parse Equations ---
        System.out.println("Parsing equations...");
        CheckString.PurgeAll();
        MaterialMethods materialMethods = new MaterialMethods();
        CheckString checkStringInstance = new CheckString();
        String equationsText = extractEquationsText(fileContent);
        String cleanedEquations = cleanComments(equationsText);

        if (!parseEquations(cleanedEquations, materialMethods, checkStringInstance)) {
            System.err.println("ERROR: Halting due to parsing errors.");
            System.exit(1);
        }
        equationsForResiduals.clear();
         if(CheckString.Functions != null) {
             equationsForResiduals.addAll(CheckString.Functions);
         }
         System.out.println("Stored " + equationsForResiduals.size() + " equations for residual check.");
        System.out.println("Parsing complete.");

        // --- Pre-Check (Informational) ---
        int equationCount = CheckString.Functions.size();
        int initialVarCount = CheckString.Var.getSize(); // Use getSize()
        int solvedOneVarCount = CheckString.OneEquationVar.size();
        int totalVarCount = initialVarCount + solvedOneVarCount; // Note: Var count check is still simplistic

        System.out.println("--- Solver Pre-Check ---");
        System.out.println("Equations found (pre-solve): " + equationCount);
        System.out.println("Initial unique variables found: " + initialVarCount);
        System.out.println("Variables likely solved individually: " + solvedOneVarCount);
        System.out.println("Total unique symbols treated as variables by parser: " + totalVarCount);
        System.out.print("Parser variable list (internal names): [ ");
        for (String2ME.VString v : CheckString.Var.Variables) {
             System.out.print(v.getVar() + " ");
        }
         System.out.println("]");

        if (equationCount != initialVarCount) { // Compare equations to initial vars only
            System.out.println("WARNING: Equation count ("+equationCount+") differs from initial variable symbol count ("+initialVarCount+"). Proceeding anyway.");
        }

        // --- Solve ---
        System.out.println("Starting solver process...");
        try {
            Config.ErrorFound = false;
            DiffAndEvaluator.TimeLimitExceeded = false;
            SolverGUI.ResidualsHigh = false;
            DiffAndEvaluator.StringErrorEvaluating = null;
            DiffAndEvaluator.IrrealEvaluation = false;

            PrepareMatrix.PreTarjan();
            System.out.println("Variables solved individually by PreTarjan: " + CheckString.OneEquationVar.size());

            // Check if there are remaining coupled equations to solve
            // Use getSize() instead of isEmpty()
            if (CheckString.Functions.size() > 0 && CheckString.Var.getSize() > 0) {
                System.out.println("Proceeding to solve coupled system (Tarjan/Newton)...");
                PrepareMatrix DF = new PrepareMatrix();
                DF.PreNewton();
            } else if (CheckString.Functions.isEmpty() && CheckString.Var.getSize() == 0 && !CheckString.OneEquationVar.isEmpty()){
                 System.out.println("All equations were solved individually or were direct assignments.");
            } else if (CheckString.Functions.isEmpty() && CheckString.Var.getSize() == 0 && CheckString.OneEquationVar.isEmpty() && !equationsForResiduals.isEmpty()){
                 System.out.println("No variables found requiring solver after parsing.");
            }
             else {
                 System.out.println("No coupled systems to solve.");
            }
            System.out.println("Solver process finished.");

             if (Config.ErrorFound) {
                 System.err.println("ERROR: Solver indicated an error occurred during evaluation.");
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

        // --- Output Residuals (Use the stored list) ---
        System.out.println("\n--- Residuals ---");
        printResidualsToConsole(equationsForResiduals);

        // --- Final Status ---
        if (executionError) {
            System.err.println("\nExecution finished with errors.");
            System.exit(1);
        } else {
             if (SolverGUI.ResidualsHigh) {
                 System.out.println("\nWARNING: Execution finished, but residuals were high. Solution might be inaccurate.");
             } else {
                 System.out.println("\nExecution finished successfully.");
             }
            System.exit(0);
        }
    } // End main

    // --- Helper Methods ---

    private static void printUsage() {
        System.err.println("Usage: java -cp <classpath> cli.CommandLineRunner <path_to_ris_file>");
        System.err.println("Example: java -cp \".;bin;Dependencies\\*\" cli.CommandLineRunner examples\\01_Introduction.ris");
    }

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

     private static String extractEquationsText(String fullContent) {
         int markerPos = fullContent.indexOf("@$@%@EndOfEquationData@$@%@");
         if (markerPos != -1) {
             return fullContent.substring(0, markerPos).trim();
         } else {
             System.err.println("WARNING: EndOfEquationData marker not found. Treating whole file as equations.");
             return fullContent.trim();
         }
     }

    private static String cleanComments(String input) {
        boolean comments = false;
        char c;
        char pc = CheckString.Espacio;
        StringBuilder aux = new StringBuilder();
        StringReader J = new StringReader(input);
        BufferedReader BufJ = new BufferedReader(J);

        try {
             int character;
             while ((character = J.read()) != -1) {
                 c = (char) character;

                 if (pc == CheckString.Slash && c == CheckString.Por) {
                     if (aux.length() > 0) {
                         aux.deleteCharAt(aux.length() - 1);
                     }
                     comments = true;
                     pc = CheckString.Espacio;
                     continue;
                 }

                 if (pc == CheckString.Por && c == CheckString.Slash) {
                     comments = false;
                     pc = CheckString.Espacio;
                     continue;
                 }

                 if (!comments) {
                      aux.append(c);
                 } else if (c == '\n' || c == '\r') { // Keep newlines even in comments
                      aux.append(c);
                 }


                if (!(comments && c == CheckString.Por)) {
                   pc = c;
                } else {
                   pc = c;
                }
            }
        } catch (IOException e) {
             System.err.println("Error reading string for comment cleaning: " + e.getMessage());
        } finally {
             try { BufJ.close(); J.close(); } catch (IOException e) {}
        }
        return aux.toString();
    }


    private static boolean parseEquations(String equationsText, MaterialMethods materialMethods, CheckString checkStringInstance) {
        String line;
        int lineNumber = 0;
        boolean overallSuccess = true;
        BufferedReader reader = new BufferedReader(new StringReader(equationsText));
        try {
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String originalLine = line;
                line = line.trim();

                if (line.isEmpty()) continue;

                String processedLine = line.replace(CheckString.Tab, CheckString.Espacio);

                String2ME.GramErr thermoResult = searchThermodynamicFunctionCli(processedLine, materialMethods, checkStringInstance);
                if (thermoResult.GetTypeError() != 0) {
                     System.err.println("ERROR in Thermodynamic Function call on line " + lineNumber + ": Substance/Property not found or syntax error in '" + originalLine.trim() + "'");
                     executionError = true;
                     overallSuccess = false;
                     continue;
                }
                 processedLine = thermoResult.getString();

                checkStringInstance.getVariables(processedLine);

                String2ME.GramErr gramResult = checkStringInstance.GramCheck(processedLine);

                if (!checkGramCli(gramResult, lineNumber, originalLine)) {
                     overallSuccess = false;
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR reading processed equations: " + e.getMessage());
            executionError = true;
            return false;
        } finally {
             try {reader.close(); } catch (IOException e) {}
        }
        return overallSuccess;
    }

    private static boolean checkGramCli(String2ME.GramErr gramResult, int lineNumber, String originalLine) {
         byte errorType = gramResult.GetTypeError();
         if (errorType == 0) {
             return true;
         }
         executionError = true;
         System.err.print("ERROR parsing line " + lineNumber + ": ");
         switch (errorType) {
             // ... (cases 1 through 10 as before) ...
             case 1: System.err.println("Illegal character found <" + gramResult.GetCaracter() + ">"); break;
             case 2: System.err.println("Unexpected character <" + gramResult.GetCaracter() + "> (Potential syntax error or duplicate '=')"); break;
             case 3: System.err.println("Two or more operators followed near <" + gramResult.GetCaracter() + ">"); break;
             case 4: System.err.println("Dot or comma after a letter near <" + gramResult.GetCaracter() + ">"); break;
             case 5: System.err.println("Equal sign missing."); break;
             case 6: System.err.println("Parenthesis/bracket mismatch (extra or missing)."); break;
             case 7: System.err.println("Empty function parenthesis or brackets near <" + gramResult.GetCaracter() + ">"); break;
             case 8: System.err.println("Number followed directly by variable/letter (missing operator?) near <" + gramResult.getString() + ">"); break;
             case 9: System.err.println("Operator missing operand or at end of line near <" + gramResult.GetCaracter() + ">"); break;
             case 10: System.err.println("Variable name contains invalid characters in <" + gramResult.getString() + "> (Only letters, numbers, '_' allowed; must start with letter)"); break;
             default: System.err.println("Unknown parsing error code: " + errorType); break;
         }
         System.err.println("  Context: " + originalLine.trim());
         return false;
     }

    private static void printResultsToConsole() {
        // ... (Implementation as before) ...
        NumberFormat AvgFormat = new DecimalFormat("####0.#######");
        NumberFormat SmallFormat = new DecimalFormat("0.#####E0");
        NumberFormat BigFormat = new DecimalFormat("00000.#####E0");

        java.util.List<String2ME.VString> allVars = new java.util.ArrayList<>(CheckString.Var.Variables);
        allVars.addAll(CheckString.OneEquationVar);
        Collections.sort(allVars);

        if (allVars.isEmpty()) {
             if (!executionError) System.out.println("No variables required solving.");
             else System.out.println("No results to display due to errors.");
             return;
        }

        System.out.println("Variable          | Value");
        System.out.println("------------------|-------------------");

        for (VString vs : allVars) {
            String varName = getVariableCase(vs.getVar());
            varName = varName.replace("Gg", "_");
            double result; String resultStr;
            try {
                result = DiffAndEvaluator.Evaluate(vs.getVar());
                 if (Double.isNaN(result) || Double.isInfinite(result)) resultStr = String.valueOf(result);
                 else if (Math.abs(result) < 100000 && Math.abs(result) >= 1e-5) resultStr = AvgFormat.format(result);
                 else if (Math.abs(result) < 1e-5 && Math.abs(result) != 0) resultStr = SmallFormat.format(result);
                 else if (Math.abs(result) >= 100000) resultStr = BigFormat.format(result);
                 else resultStr = AvgFormat.format(result);
            } catch (Exception e) { resultStr = "Error evaluating"; executionError = true; }
            System.out.printf("%-17s | %s%n", varName, resultStr);
        }
    }

    private static String getVariableCase(String lowerCaseVar) {
         if (CheckString.CaseVariables == null) return lowerCaseVar.replace("Gg", "_");
         for (String s : CheckString.CaseVariables) {
             if (lowerCaseVar.equalsIgnoreCase(s.replace("_", "Gg"))) return s;
         }
         if (CheckString.OneEquationVar != null) {
              for (String2ME.VString vs : CheckString.OneEquationVar) {
                  if (lowerCaseVar.equalsIgnoreCase(vs.getVar())) return vs.getVar().replace("Gg", "_");
              }
          }
         return lowerCaseVar.replace("Gg", "_");
     }

         // Helper to format and print residuals
     // Add this method to the CommandLineRunner class
     private static void printResidualsToConsole(java.util.List<EqStorer> equationsToCheck) {
        if (equationsToCheck == null || equationsToCheck.isEmpty()) {
            if (!executionError) {
                 System.out.println("No equations processed to calculate residuals.");
            } else {
                 System.out.println("Cannot calculate residuals due to previous errors or no equations processed.");
            }
            return;
        }

        NumberFormat AvgFormat = new DecimalFormat("##0.#####");
        NumberFormat SmallFormat = new DecimalFormat("0.#####E0");
        NumberFormat BigFormat = new DecimalFormat("#####0.##E0");

        System.out.println("Equation Residuals (Internal Form = 0, Target: 0)");
        System.out.println("------------------------------------------------------------");

        /*
        // --- Introduce ALL current variable values before evaluating residuals ---
        try {
            System.out.println("  Introducing solved variable values for residual check...");
            // Combine all solved variables for introduction
            VList allSolvedVars = new VList();
            if(CheckString.Var != null && CheckString.Var.Variables != null) allSolvedVars.Variables.addAll(CheckString.Var.Variables);
            if(CheckString.OneEquationVar != null) allSolvedVars.Variables.addAll(CheckString.OneEquationVar);

            // Introduce values IF there are any variables solved
            if (allSolvedVars.getSize() > 0) {
                 // Create a temporary vector containing only the variable NAMES
                 // and their CURRENT solved values for the ME string.
                 vector valuesToIntroduce = new vector(); // Use imported solver.vector
                 for(VString vs : allSolvedVars.Variables) {
                     try {
                          // Get the solved value by evaluating the variable name
                          double solvedValue = DiffAndEvaluator.Evaluate(vs.getVar());
                          // Create nodo with name AND value - use imported solver.nodo
                          valuesToIntroduce.vector.add(new nodo(solvedValue, vs.getVar()));
                     } catch (Exception evalEx) {
                          System.err.println("WARNING: Could not get solved value for variable '" + vs.getVar() + "' before residual check: " + evalEx.getMessage());
                          // Add with NaN for now.
                          valuesToIntroduce.vector.add(new nodo(Double.NaN, vs.getVar()));
                     }
                 }
                 // Now call IntroduceValues with the vector that contains names AND values
                 DiffAndEvaluator.IntroduceValues(valuesToIntroduce.EvaluateVector2ME());
                 System.out.println("    Introduced: " + valuesToIntroduce.EvaluateVector2ME());
            } else {
                 System.out.println("    No variables to introduce for residuals.");
            }
        } catch (Exception introErr) {
            System.err.println("ERROR: Failed to introduce variable values before residual check: " + introErr.getMessage());
            executionError = true; // Mark error if introduction fails
        }
        */
        // --- End variable introduction ---


        Iterator<EqStorer> it = equationsToCheck.iterator(); // Iterate the passed list
        SolverGUI.ResidualsHigh = false; // Reset flag before checking

        while (it.hasNext()) {
            EqStorer eqaux = it.next();
            String internalEquation = eqaux.getEquation();
            double residual = Double.NaN; // Default to NaN
            String residualStr = "N/A";
            String displayEquation = "";

            try {
                // Calculate residual
                residual = DiffAndEvaluator.Evaluate("N(" + internalEquation + ")");

                // Format residual value
                 if (Double.isNaN(residual) || Double.isInfinite(residual)){
                     residualStr = String.valueOf(residual);
                     executionError = true; // Treat NaN/Inf as error
                 } else if (Math.abs(residual) < 10000 && Math.abs(residual) >= 1e-5)
                    residualStr = AvgFormat.format(residual);
                 else if (Math.abs(residual) < 1e-5 && Math.abs(residual) != 0)
                      residualStr = SmallFormat.format(residual);
                 else if (Math.abs(residual) >= 10000)
                      residualStr = BigFormat.format(residual);
                 else // Handle exactly zero
                      residualStr = AvgFormat.format(residual);

                 // Display internal form directly
                 displayEquation = internalEquation.replace("Gg", "_") + " = 0";
                 // Attempt to replace internal variable names with original case for readability
                 displayEquation = replaceVarsWithCase(displayEquation);

                // Check if residual is high only if calculation was valid
                if (!(Double.isNaN(residual) || Double.isInfinite(residual)) && Math.abs(residual) > Config.Precision * 10) { // Use a slightly larger tolerance for warning
                    SolverGUI.ResidualsHigh = true; // Set flag if any residual is high
                }

            } catch (Exception e) {
                  residualStr = "Error evaluating (" + e.getClass().getSimpleName() + ")";
                  displayEquation = internalEquation.replace("Gg", "_") + " = 0 (Eval Error)";
                  displayEquation = replaceVarsWithCase(displayEquation);
                  executionError = true; // Mark error occurred during residual calc
            }

            // Print, ensuring displayEquation isn't too long for formatting
            System.out.printf("%-60s | Residual: %s%n", displayEquation.substring(0, Math.min(displayEquation.length(), 60)), residualStr);

        } // end while

         if (SolverGUI.ResidualsHigh && !executionError) { // Only show warning if no other errors
             System.out.println("\nWARNING: One or more residuals are high, solution may be inaccurate.");
         }
    } // End modified printResidualsToConsole

    // --- Keep these helper methods ---
    private static String replaceVarsWithCase(String equation) {
         String result = equation;
         if (CheckString.CaseVariables == null) return result.replace("Gg", "_"); // Safety check

         java.util.List<String> sortedVars = new java.util.ArrayList<>(CheckString.CaseVariables);
         Collections.sort(sortedVars, (s1, s2) -> Integer.compare(s2.length(), s1.length()));

         for (String originalCaseVar : sortedVars) {
             // Replace internal name (lowercase, Gg) with original case name
              // Need word boundaries to avoid replacing parts of names
             String internalNamePattern = "\\b" + originalCaseVar.toLowerCase().replace("_", "Gg") + "\\b";
             // Safely escape original name for replacement string if needed (unlikely here)
             result = result.replaceAll(internalNamePattern, originalCaseVar);
         }
         return result.replace("Gg", "_"); // Final Gg cleanup
     }

    private static int findEqualConsole(String s) {
         final char OpenP = '(';
         final char CloseP = ')';
         final String marker = "-1*(";

         int markerPos = s.indexOf(marker);
         if (markerPos == -1) return -1;
         int parenLevel = 0;
         int i = -1; // Initialize i
         int firstOpenParenAfterMarker = -1;

          for (int startSearch = markerPos + marker.length(); startSearch < s.length(); startSearch++) {
               if (s.charAt(startSearch) == OpenP) {
                   firstOpenParenAfterMarker = startSearch;
                   parenLevel = 1;
                   i = firstOpenParenAfterMarker + 1;
                   break;
               }
           }
           if (firstOpenParenAfterMarker == -1) return -1;

         while (i < s.length()) {
             char c = s.charAt(i);
             if (c == OpenP) parenLevel++;
             else if (c == CloseP) {
                 parenLevel--;
                 if (parenLevel == 0) return markerPos;
             }
             i++;
         }
         return -1;
     }

         // Add this static helper method to CommandLineRunner.java
     public static String2ME.GramErr searchThermodynamicFunctionCli(String input, MaterialMethods Materiales, CheckString ch) {
         try {
             if (checkSubstanceCli(input, Materiales)) { // Use the static helper below
                 StringTokenizer lector = new StringTokenizer(input, ".(),", true);
                 String aux, material = null, property = null, PrevToken = null;
                 LinkedList<String> Variables = new LinkedList<String>(); // Variables from the function call in the .ris file
                 boolean insideParens = false;

                 // Parse the input string like Substance.Property(Var1,Var2,...)
                 while (lector.hasMoreTokens()) {
                     aux = lector.nextToken();
                     if (aux.equalsIgnoreCase(".")) {
                         material = new String(PrevToken); // The token before '.' is the substance
                     } else if (aux.equalsIgnoreCase("(")) {
                         property = new String(PrevToken); // The token before '(' is the property
                         insideParens = true;
                     } else if (aux.equalsIgnoreCase(",")) {
                         if (insideParens && PrevToken != null && !PrevToken.equals("(") && !PrevToken.equals(",")) {
                             Variables.add(new String(PrevToken)); // Token before ',' is a variable
                         }
                     } else if (aux.equalsIgnoreCase(")")) {
                         if (insideParens && PrevToken != null && !PrevToken.equals("(") && !PrevToken.equals(",")) {
                             Variables.add(new String(PrevToken)); // Token before ')' is the last variable
                         }
                         insideParens = false; // Closing parenthesis found
                         break; // Stop parsing after closing parenthesis
                     } else if (!aux.equals("(") && !aux.equals(",") && !aux.equals(".") && !aux.equals(")")) {
                         // Keep track of the previous non-delimiter token
                         PrevToken = aux;
                     }
                 }

                 // Validate parsing results
                 if (material == null || property == null) {
                    System.err.println("ERROR: Could not parse material/property from call: " + input);
                    return new String2ME.GramErr((byte) 1, input); // Indicate error
                 }

                 // Use the public getter for the list of materials
                 for (MaterialList m : Materiales.getMaterialLists()) {
                     // Use public getter for material name
                     if (m.getMaterial().equalsIgnoreCase(material)) {
                         // Use public getter for property list
                         for (MaterialStore ms : m.getPropertyList()) {
                             // Use public getter for property name
                             if (ms.getProperty().equalsIgnoreCase(property)) {
                                 // Found the matching property!
                                 // Use public getter for the stored formula
                                 aux = ms.getFormula();
                                 // Use public getter for the variables defined in the database for this property
                                 String[] dbVars = ms.getVariables().replace(" ", "").split(",");
                                 String formulaVarsString = ms.getVariables();

                                 // --- Argument Count Check ---
                                 int expectedArgs = (formulaVarsString == null || formulaVarsString.trim().isEmpty()) ? 0 : formulaVarsString.split(",").length;
                                 if (Variables.size() != expectedArgs) {
                                     System.err.println("ERROR: Argument count mismatch for " + material + "." + property + ". Expected " + expectedArgs + " ("+formulaVarsString+"), but call provided " + Variables.size() + " ("+String.join(",", Variables)+") in '" + input + "'");
                                     return new String2ME.GramErr((byte) 1, input); // Error code 1 for mismatch
                                 }

                                 // Substitute the variables from the call into the formula
                                 String resultado = substituteThermoVars(aux, dbVars, Variables);

                                 // Rewrite the equation into the internal F(x)-G(x)=0 form
                                 // Assume the *first* variable in the CALL is the output variable
                                 if (Variables.isEmpty()) {
                                     // Handle case where property takes no arguments but returns a value (e.g., a constant property)
                                     // If the formula itself is just a number, the equation is OutputVar = Number
                                     // This scenario is less likely for thermo but possible.
                                     // Let's assume the first *parsed* variable from the property call IS the output
                                     // We need a way to designate the output var, assume it's passed first in call
                                     System.err.println("ERROR: Thermodynamic function call has no arguments to determine output variable: " + input);
                                     return new String2ME.GramErr((byte) 1, input);
                                 }
                                 String outputVarInternal = Variables.getFirst().toLowerCase().replace("_", "Gg"); // Internal name
                                 String funcBody = resultado; // This is the RHS of the original formula
                                 String finalEquation = outputVarInternal + "-1*(" + funcBody + ")"; // Create F-G form

                                 System.out.println("  Substituted thermo call '" + input + "' with internal form: " + finalEquation);
                                 return new String2ME.GramErr((byte) 0, finalEquation);
                             }
                         }
                     }
                 }
                 // If loop finishes, the specific material/property combination wasn't found
                 System.err.println("ERROR: Thermodynamic property not found: " + material + "." + property);
                 return new String2ME.GramErr((byte) 1, input);
             }
             // If checkSubstanceCli returned false, it's not a thermo function call
             return new String2ME.GramErr((byte) 0, input);
         } catch (Exception e) {
             System.err.println("ERROR during thermodynamic function processing for: " + input);
             e.printStackTrace(System.err);
             return new String2ME.GramErr((byte) 1, input); // Return error on exception
         }
     }


     public static boolean checkSubstanceCli(String input, MaterialMethods Materiales) {
         // ... (Implementation as before) ...
         int pos = input.indexOf(".");
        if (pos <= 0 || pos == input.length() - 1) return false;
        String potentialSubstance = input.substring(0, pos);
        // Use the public getter for the names list
        for (String knownMaterial : Materiales.getMaterials()) {
            if (potentialSubstance.equalsIgnoreCase(knownMaterial)) {
                int openParen = input.indexOf('(', pos);
                if (openParen > pos) {
                     int closeParen = input.lastIndexOf(')');
                     if (closeParen > openParen) return true;
                }
            }
        }
        return false;
     }

     private static String substituteThermoVars(String formula, String[] dbVars, LinkedList<String> callVars) {
           // ... (Implementation as before) ...
          String result = formula.replace(" ", "");
          StringTokenizer tokenizer = new StringTokenizer(result, "+/*-()[]{} ^=!", true);
          StringBuilder reconstructed = new StringBuilder();
          String token;

          while (tokenizer.hasMoreTokens()) {
               token = tokenizer.nextToken();
               int pos = varPositionCli(token.replace("_","Gg"), dbVars); // Check internal name
               if (pos != -1) {
                   // Substitute with calling variable's internal name
                   reconstructed.append(callVars.get(pos).toLowerCase().replace("_","Gg"));
               } else {
                   reconstructed.append(token); // Keep original token
               }
           }
           return reconstructed.toString();
       }

      private static int varPositionCli(String var, String[] list) {
           // ... (Implementation as before) ...
          for (int i = 0; i < list.length; i++) {
              if (list[i].replace("_","Gg").equalsIgnoreCase(var)) {
                  return i;
              }
          }
          return -1;
      }

} // End CommandLineRunner Class