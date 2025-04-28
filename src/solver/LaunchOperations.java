package solver;

import gui.Config;
import java.util.LinkedList;
import solver.PrepareUncmin;

public class LaunchOperations implements Runnable {
    // Instance fields set by constructor
    private boolean isSingleEquation; // Use clearer boolean name
    private LinkedList<Integer> functionsIndices;
    private LinkedList<Integer> varsIndices;
    private String functionString;
    private String varString;

    // Constructor for Equation Systems (uses indices)
    public LaunchOperations(LinkedList<Integer> functionsIndices, LinkedList<Integer> varsIndices) {
        this.isSingleEquation = false;
        this.functionsIndices = functionsIndices;
        this.varsIndices = varsIndices;
        // Null out single equation fields
        this.functionString = null;
        this.varString = null;
    }

    // Constructor for Single Equations (uses Strings)
    public LaunchOperations(String function, String var) {
        this.isSingleEquation = true;
        this.functionString = function;
        this.varString = var;
        // Null out list fields
        this.functionsIndices = null;
        this.varsIndices = null;
    }


    @Override
    public void run() {
        try {
            PrepareUncmin solverInstance = null; // Instance for the current subsystem

            if (isSingleEquation) { // Single equation path
                 if (functionString == null || varString == null) {
                      System.err.println("ERROR: LaunchOperations called for single equation but strings are null.");
                      return;
                 }
                // Check if it's a simple constant assignment first
                if (!Newton.ConstantSolver(functionString, varString)) {
                    // Not constant, need iterative solver
                    int methodToUse = Config.SingleVariableMethod;
                    boolean useLM = (methodToUse == 4);

                    if (useLM) {
                         solverInstance = new PrepareUncmin(functionString, varString, true);
                         // REMOVED CHECK: Directly call the method
                         solverInstance.LMSolveInstance();
                    } else {
                         solverInstance = new PrepareUncmin(functionString, varString);
                         // REMOVED CHECK: Directly call the method
                         solverInstance.SolveInstance(methodToUse);
                    }
                } else {
                     System.out.println("INFO: ConstantSolver solved: " + varString);
                }
            } else { // System of equations path
                if (functionsIndices == null || varsIndices == null || functionsIndices.isEmpty() || varsIndices.isEmpty()) {
                     System.err.println("ERROR: LaunchOperations called for system but index lists are null/empty.");
                     return;
                }

                // Determine which method set to use (Multi or Single config)
                int methodToUse = (functionsIndices.size() > 1) ? Config.MultiVariableMethod : Config.SingleVariableMethod;
                boolean useLM = (methodToUse == 4);

                if (useLM) {
                     solverInstance = new PrepareUncmin(functionsIndices, varsIndices, true);
                    // REMOVED CHECK: Directly call the method
                     solverInstance.LMSolveInstance();
                } else {
                    solverInstance = new PrepareUncmin(functionsIndices, varsIndices);
                    // REMOVED CHECK: Directly call the method
                    solverInstance.SolveInstance(methodToUse);
                }
            }
        } catch (RuntimeException e) {
             System.err.println("RUNTIME EXCEPTION in LaunchOperations run method:");
            e.printStackTrace(System.err);
             Config.ErrorFound = true; // Mark global error on exception
        } catch (Exception e) {
            System.err.println("EXCEPTION in LaunchOperations run method:");
            e.printStackTrace(System.err);
            Config.ErrorFound = true; // Mark global error on exception
        }
    }

    // Helper to check if PrepareUncmin instance seems valid before solving
    private boolean isValidInstance(PrepareUncmin instance) {
         if (instance == null) {
              System.err.println("ERROR: PrepareUncmin instance is null.");
              return false;
         }
         if (instance.instanceXk == null || instance.instanceFx == null || instance.instanceN <= 0 || instance.instanceM <= 0) {
               System.err.println("ERROR: PrepareUncmin instance vectors/counts invalid.");
              return false;
         }
          // Add more checks if needed (e.g., Jacobian != null for LM)
         return true;
     }

}