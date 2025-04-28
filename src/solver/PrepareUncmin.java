package solver;

import java.util.LinkedList;
import java.util.List;
import String2ME.CheckString;
import String2ME.EqStorer;
import String2ME.VString;
import String2ME.InitVal;
import gui.Config;
import evaluation.DiffAndEvaluator;
import doglegMethod.*;
import solver.matriz; // Ensure matriz is imported
import solver.vector; // Ensure vector is imported
import solver.nodo;  // Ensure nodo is imported

public class PrepareUncmin {
    // Static fields (required by Uncmin_methods/Lmder_fcn)
    public static vector Fx = new vector();
    public static vector Xk = new vector();
    public static String[][] Jacobian;
    public static LinkedList<String[][]> Hessians;

    // Instance fields
    private vector instanceFx;
    private vector instanceXk;
    private int instanceN;
    private int instanceM;
    private LinkedList<String[][]> instanceHessians;

    // --- Helper Method ---
    private void handleEmptyVectors(String context) {
        System.err.println("ERROR: Cannot initialize PrepareUncmin in " + context + " with empty Xk or Fx vectors.");
        Jacobian = null; // Set static to null
        Hessians = null; // Set static to null
        this.instanceHessians = null; // Set instance to null
    }

     // --- Helper Method for populating instance vectors from indices ---
     private void populateVectorsFromIndices(LinkedList<Integer> FunctionsIndices, LinkedList<Integer> VariablesIndices) {
        this.instanceXk = new vector();
        for (Integer index : VariablesIndices) {
            if (index != null && index >= 0 && index < CheckString.Var.Variables.size()) {
                VString vs = CheckString.Var.Variables.get(index);
                double initialValue = Config.DefaultInitialValue;
                for (InitVal iv : Config.InitValue) {
                    if (iv.getVariable().equalsIgnoreCase(vs.getVar())) {
                        initialValue = iv.getValue();
                        break;
                    }
                }
                this.instanceXk.vector.add(new nodo(initialValue, vs.getVar()));
            } else { System.err.println("ERROR: Invalid variable index: " + index); }
        }
        this.instanceFx = new vector();
        for (Integer index : FunctionsIndices) {
            if (index != null && index >= 0 && index < CheckString.Functions.size()) {
                EqStorer es = CheckString.Functions.get(index);
                this.instanceFx.vector.add(new nodo(es.getEquation()));
            } else { System.err.println("ERROR: Invalid function index: " + index); }
        }
        this.instanceN = this.instanceXk.getSize();
        this.instanceM = this.instanceFx.getSize();
     }

      // --- Helper Method for populating instance vectors for single equation ---
      private void populateVectorsSingle(String Equation, String Var) {
           this.instanceFx = new vector();
           this.instanceXk = new vector();
           this.instanceFx.vector.add(new nodo(Equation));
           this.instanceXk.vector.add(new nodo(Var));
           // Set initial value
           double initialValue = Config.DefaultInitialValue;
           for (InitVal IV : Config.InitValue) {
               if (Var.equalsIgnoreCase(IV.getVariable())) {
                   initialValue = IV.getValue();
                   break;
               }
           }
           if (!this.instanceXk.vector.isEmpty()) {
              this.instanceXk.vector.get(0).SetValue(initialValue);
           }
           this.instanceN = 1;
           this.instanceM = 1;
       }


     // --- Helper Method for creating instance Hessians list ---
     private void CreateInstanceHessianList() {
         this.instanceHessians = new LinkedList<String[][]>();
         if (Jacobian == null || this.instanceM == 0 || this.instanceN == 0 || Jacobian.length != this.instanceM || Jacobian[0].length != this.instanceN) {
             System.err.println("ERROR: Cannot create Hessians list due to invalid Jacobian.");
             return;
         }
         String[][] funcHessian;
         for (int funcIndex = 0; funcIndex < this.instanceM; funcIndex++) {
             funcHessian = new String[this.instanceN][this.instanceN];
             for (int var1Index = 0; var1Index < this.instanceN; var1Index++) {
                 String jacobianEntry = Jacobian[funcIndex][var1Index];
                 if (jacobianEntry == null) {
                      System.err.println("ERROR: Null Jacobian entry ["+funcIndex+"]["+var1Index+"] for Hessian.");
                      for(int k=0; k<this.instanceN; k++) funcHessian[var1Index][k] = "\"ERR_NULL_JAC\"";
                      continue;
                 }
                 int var2Index = 0;
                 for (nodo variable2 : this.instanceXk.vector) {
                     String var2Name = variable2.GetCadena();
                     if (var2Name == null) {
                           funcHessian[var1Index][var2Index] = "\"ERR_NULL_VAR\"";
                           System.err.println("ERROR: Null var name index "+var2Index+" for Hessian.");
                     } else {
                         funcHessian[var1Index][var2Index] = evaluation.DiffAndEvaluator.diff(jacobianEntry, var2Name);
                     }
                     var2Index++;
                 }
             }
             this.instanceHessians.add(funcHessian);
         }
     }


    // --- DISTINCT CONSTRUCTORS ---

    // Constructor 1: System, Optimization Methods (Needs Hessian List)
    public PrepareUncmin(LinkedList<Integer> FunctionsIndices, LinkedList<Integer> VariablesIndices) {
        clear();
        populateVectorsFromIndices(FunctionsIndices, VariablesIndices);
        if (this.instanceN > 0 && this.instanceM > 0) {
            // Set static fields for helpers
            Xk = this.instanceXk;
            Fx = this.instanceFx;
            Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx);
            CreateInstanceHessianList(); // Creates instanceHessians
            Hessians = this.instanceHessians; // Set static Hessians
        } else {
            handleEmptyVectors("PrepareUncmin(List, List)");
        }
    }

    // Constructor 2: System, Levenberg-Marquardt (No Hessian List)
    public PrepareUncmin(LinkedList<Integer> FunctionsIndices, LinkedList<Integer> VariablesIndices, boolean isLM) {
         clear();
         populateVectorsFromIndices(FunctionsIndices, VariablesIndices);
         if (this.instanceN > 0 && this.instanceM > 0) {
            // Set static fields for helpers
            Xk = this.instanceXk;
            Fx = this.instanceFx;
            Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx);
            Hessians = null; // LM doesn't use this list
            this.instanceHessians = null;
         } else {
              handleEmptyVectors("PrepareUncmin(List, List, LM)");
         }
    }

    // Constructor 3: Single Eq, Optimization Methods (Needs Hessian List)
    public PrepareUncmin(String Equation, String Var) {
        clear();
        populateVectorsSingle(Equation, Var);
        if (this.instanceN > 0 && this.instanceM > 0) {
            // Set static fields
            Xk = this.instanceXk;
            Fx = this.instanceFx;
            Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx);
            CreateInstanceHessianList();
            Hessians = this.instanceHessians;
        } else {
             handleEmptyVectors("PrepareUncmin(String, String)");
        }
    }

    // Constructor 4: Single Eq, Levenberg-Marquardt (No Hessian List)
    public PrepareUncmin(String Equation, String Var, boolean isLM) {
         clear();
         populateVectorsSingle(Equation, Var);
         if (this.instanceN > 0 && this.instanceM > 0) {
            // Set static fields
            Xk = this.instanceXk;
            Fx = this.instanceFx;
            Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx);
            Hessians = null;
            this.instanceHessians = null;
         } else {
              handleEmptyVectors("PrepareUncmin(String, String, LM)");
         }
     }


     // --- INSTANCE SOLVER METHODS ---
     // These methods use the INSTANCE fields (instanceN, etc.)
     // but set the STATIC fields right before calling the external library

     public void SolveInstance(int EvaluationMethod) {
         if (this.instanceN <= 0 || this.instanceM <= 0 || this.instanceN != this.instanceM) {
             System.err.println("ERROR: Invalid dimensions for SolveInstance (N=" + this.instanceN + ", M=" + this.instanceM + ")");
             return;
         }
         // Set static fields for Uncmin_methods
         PrepareUncmin.Xk = this.instanceXk;
         PrepareUncmin.Fx = this.instanceFx;
         PrepareUncmin.Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx);
         PrepareUncmin.Hessians = this.instanceHessians; // Use Hessians calculated by constructor

         if (PrepareUncmin.Jacobian == null || PrepareUncmin.Hessians == null) {
              System.err.println("ERROR: Static Jacobian or Hessians null before calling Uncmin Solve.");
              return;
         }

        Uncmin_methods UM = new Uncmin_methods();
        int tam = this.instanceN;
        // ... (rest of variable setup: xpls, fpls, gpls, a, udiag, typsiz, etc.) ...
        double[] xpls = new double[tam + 1];
        double[] fpls = new double[2];
        double[] gpls = new double[tam + 1];
        double[][] a = new double[tam + 1][tam + 1];
        double[] udiag = new double[tam + 1];
        double typsiz[] = new double[tam + 1]; for(int i=1;i<=tam;i++) typsiz[i]=1.0;
        double fscale[]={1.0}; int method[]={EvaluationMethod}; int iexp[]={0};
        int msg[]={80}; int itrmcd[]={0}; int ndigit[]={-1}; int itnlim[]={Config.MaxNumberOfIteration};
        int iagflg[]={1}; int iahflg[]={1}; double dlt[]={Config.TrustRegionRadius};
        double gradtl[]={Config.GradientPrecision}; double stepmx[]={Config.MaxJump};
        double steptl[]={Config.Precision};

        try {
            double[] initialX_1based = this.instanceXk.Vector2Dogleg();
            Uncmin_f77.optif9_f77(tam, initialX_1based, UM, typsiz, fscale, method, iexp, msg, ndigit, itnlim, iagflg, iahflg, dlt, gradtl, stepmx, steptl, xpls, fpls, gpls, itrmcd, a, udiag);
             // Update results
            if (itrmcd[0] == 0 || itrmcd[1] == 1 || itrmcd[1] == 2 || itrmcd[1] == 3) {
                updateStaticXkFromResult(xpls, tam);
                 this.instanceXk = PrepareUncmin.Xk; // Sync instance Xk with static result
            } else { System.err.println("Warning: Uncmin solve did not converge optimally (itrmcd=" + itrmcd[1] + ").");}
        } catch (Exception e) { System.err.println("ERROR during Uncmin execution:"); e.printStackTrace(System.err); }
     }

      public void LMSolveInstance() {
          if (this.instanceN <= 0 || this.instanceM <= 0 || this.instanceM < this.instanceN) {
             System.err.println("ERROR: Invalid dimensions for LMSolveInstance (N=" + this.instanceN + ", M=" + this.instanceM + ")");
             return;
          }
          // Set static fields for Lmder_fcn
          PrepareUncmin.Xk = this.instanceXk;
          PrepareUncmin.Fx = this.instanceFx;
          PrepareUncmin.Jacobian = matriz.JacobianLU(this.instanceXk, this.instanceFx); // Recalculate static Jacobian
          PrepareUncmin.Hessians = null; // Not needed

          if (PrepareUncmin.Jacobian == null) {
               System.err.println("ERROR: Static Jacobian is null before calling Minpack.");
               return;
          }

         Lmder_fcn Lder = new Lmder_fcn();
         int n_vars = this.instanceN;
         int m_funcs = this.instanceM;
         // ... (rest of variable setup: x, fvec, fjac, tol, info, ipvt) ...
         double x[] = this.instanceXk.Vector2Dogleg();
         double fvec[] = new double[m_funcs + 1];
         double fjac[][] = new double[m_funcs + 1][n_vars + 1];
         double tol = Config.Precision;
         int info[] = {0};
         int ipvt[] = new int[n_vars + 1];

         try {
             Minpack_f77.lmder1_f77(Lder, m_funcs, n_vars, x, fvec, fjac, tol, info, ipvt);
             // Update results
             if (info[0] >= 1 && info[0] <= 4) {
                 updateStaticXkFromResult(x, n_vars);
                  this.instanceXk = PrepareUncmin.Xk; // Sync instance Xk
             } else { System.err.println("Warning: Levenberg-Marquardt did not converge optimally (info code: " + info[0] + ")."); }
         } catch (Exception e) { System.err.println("ERROR during Levenberg-Marquardt execution:"); e.printStackTrace(System.err); Config.ErrorFound = true; }
     }

      // Helper to update static Xk vector from a result array (1-based)
      private static void updateStaticXkFromResult(double[] resultArray_1based, int numVars) {
            vector finalXkStatic = new vector();
            for(int i=0; i < numVars; i++) {
                String varName = null;
                 if(Xk != null && i < Xk.getSize()) { // Get name from current static Xk
                    varName = Xk.vector.get(i).GetCadena();
                 }
                 if(varName != null) {
                     finalXkStatic.vector.add(new nodo(resultArray_1based[i+1], varName));
                 } else {
                      System.err.println("Error updating static Xk: could not get variable name for index " + i);
                 }
             }
             Xk = finalXkStatic; // Overwrite static Xk
        }

    // Static clear method
    public static void clear() {
        Fx = new vector();
        Xk = new vector();
        Jacobian = null;
        Hessians = new LinkedList<>();
    }

    // --- Deprecated Static Methods (Keep temporarily if absolutely needed) ---
     /** @deprecated Use instance method SolveInstance instead */
     @Deprecated
     public static void Solve(int EvaluationMethod) {
         System.err.println("WARNING: Calling deprecated static PrepareUncmin.Solve.");
         // Implement simplified call or error based on static fields if necessary
         // This path is now unreliable.
         if (Xk == null || Fx == null || Xk.getSize() == 0 || Fx.getSize() == 0 || Xk.getSize() != Fx.getSize()) {
             System.err.println("ERROR: Static Solve called with invalid/mismatched static Xk/Fx.");
             return;
         }
         // ... (Simplified call using static fields - might still fail if state is wrong) ...
         PrepareUncmin tempInstance = new PrepareUncmin(getIndicesFromStatic(Fx), getIndicesFromStatic(Xk));
         tempInstance.SolveInstance(EvaluationMethod);


     }

      /** @deprecated Use instance method LMSolveInstance instead */
      @Deprecated
      public static void LMSolve() {
          System.err.println("WARNING: Calling deprecated static PrepareUncmin.LMSolve.");
           if (Xk == null || Fx == null || Jacobian == null || Xk.getSize() == 0 || Fx.getSize() == 0 || Fx.getSize() < Xk.getSize()) {
               System.err.println("ERROR: Static LMSolve called with invalid/mismatched static Xk/Fx/Jacobian.");
               return;
           }
          // ... (Simplified call using static fields - might still fail if state is wrong) ...
           PrepareUncmin tempInstance = new PrepareUncmin(getIndicesFromStatic(Fx), getIndicesFromStatic(Xk), true);
           tempInstance.LMSolveInstance();
       }

        // Helper to get indices (assuming static Xk/Fx map correctly to global lists - risky)
        private static LinkedList<Integer> getIndicesFromStatic(vector staticVector) {
             LinkedList<Integer> indices = new LinkedList<>();
             List<?> globalList = (staticVector == Xk) ? CheckString.Var.Variables : CheckString.Functions;
             for (nodo nStatic : staticVector.vector) {
                  boolean found = false;
                  for (int i = 0; i < globalList.size(); i++) {
                       String globalName = (staticVector == Xk) ?
                                           ((VString)globalList.get(i)).getVar() :
                                           ((EqStorer)globalList.get(i)).getEquation(); // Comparing equations is bad, need better mapping
                       String staticName = nStatic.GetCadena();
                       // This comparison is weak, especially for functions
                       if (staticName != null && staticName.equals(globalName)) {
                            indices.add(i);
                            found = true;
                            break;
                       }
                   }
                   if (!found) System.err.println("Warning: Could not map static item back to global index: " + nStatic.GetCadena());
              }
              return indices;
         }

} // End PrepareUncmin Class