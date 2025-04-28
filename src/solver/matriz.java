package solver;

import String2ME.*;

import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.math.linear.Array2DRowRealMatrix;

import evaluation.DiffAndEvaluator;

/**
 * Class to create the Jacobian Matrix
 * 
 * @author Pablo Salinas
 * 
 */
public class matriz {

	public double m[][];

	public vector aux;

	public matriz(vector Fx) {
		this.m = new double[CheckString.Var.Variables.size()][CheckString.Var.Variables
				.size()];

		aux = Fx;

	}

	public void addTerm(int col, int fila, String cadena) {
		double aux = Double.parseDouble(cadena);
		if (aux != 0)
			this.m[fila][col] = aux;
	}

	/**
	 * This will read a string like this {{1.0,2.0},{-0.5,0.0}} and will return
	 * a matrix if something is zero, then it will insert in the matrix a null
	 * 
	 * @param cadena
	 * @return a matrix of doubles
	 * @deprecated
	 */
	public matriz String2Matriz(String cadena, vector Fx) {
		StringTokenizer lector = new StringTokenizer(cadena, ",{}", true);
		String aux, prev = "";
		matriz m = new matriz(Fx);
		int col = 0, fila = 0;
		while (lector.hasMoreTokens()) {
			aux = lector.nextToken();
			if (aux.equals("{")) {/* ignore */
			} else if ((aux.equals(",")) & (prev.equals("}"))) {/* ignore */
			} else if (aux.equals(",")) {
				fila++;
			} else if (aux.equals("}")) {
				col++;
			} else
				m.addTerm(col, fila, aux);
			prev = aux;
		}
		return m;
	}

	/**
	 * Makes a String to introduce in matheclipse with the derivatives that we
	 * already have
	 * 
	 * @deprecated
	 */
	public static String Dev2String() {
		String cadena = new String("");
		// Collections.sort(CheckString.Var.Variables);
		String aux, CurrentVar;
		Iterator<EqStorer> it2 = CheckString.Functions.iterator();
		EqStorer Eqaux;
		while (it2.hasNext()) {
			cadena += "{";
			int n = 0;
			Eqaux = it2.next();
			aux = Eqaux.aux.get(n).GetVar();
			Iterator<VString> it = CheckString.Var.Variables.iterator();
			VString Vaux;
			while (it.hasNext()) {
				Vaux = it.next();
				CurrentVar = Vaux.getVar();
				if (CurrentVar.equals(aux)) {
					cadena += Eqaux.aux.get(n).GetDeriv();
					n++;
					try {
						aux = Eqaux.aux.get(n).GetVar();
					} catch (Exception e) {
						aux = "0";
					}
				} else
					cadena += ("0");
				if (it.hasNext())
					cadena += ",";
				else
					cadena += "}";
			}
			if (it2.hasNext()) {
				cadena += ",";
			}
		}
		return cadena;
	}

	/**
	 * Creates an array[][] of strings of the derivatives of the functions, the
	 * Jacobian
	 * 
	 * @return The Jacobian
	 * @deprecated
	 */
	public static String[][] JacobianGauss() {
		// Cuando haga tarjan esta parte debera adaptarse a cada matriz,el
		// tamaño

		// La matriz debe ser tan grande como el numero de variables que totales
		// que hay
		String[][] result = new String[CheckString.Var.Variables.size() + 1][CheckString.Var.Variables
				.size() + 1];

		String aux, CurrentVar;
		Iterator<EqStorer> it2 = CheckString.Functions.iterator();
		EqStorer Eqaux;
		int fila = 1, col = 1;
		while (it2.hasNext()) {
			int n = 0;
			col = 1;
			Eqaux = it2.next();
			aux = Eqaux.aux.get(n).GetVar();
			Iterator<VString> it = CheckString.Var.Variables.iterator();

			VString Vaux;
			while (it.hasNext()) {
				Vaux = it.next();
				CurrentVar = Vaux.getVar();
				if (CurrentVar.equals(aux)) {

					result[fila][col] = new String(Eqaux.aux.get(n).GetDeriv());

					n++;
					try {
						aux = Eqaux.aux.get(n).GetVar();
					} catch (Exception e) {
						aux = "0";
					}
				} else
					result[fila][col] = new String("0");
				if (it.hasNext())
					col++;
			}
			if (it2.hasNext()) {
				fila++;
			}
		}

		return result;
	}

	/**
	 * Creates an array[][] of strings of the derivatives of the functions, the
	 * Jacobian
	 * 
	 * @return The Jacobian
	 * @deprecated
	 */
	public static String[][] JacobianLU() {
		// Cuando haga tarjan esta parte debera adaptarse a cada matriz,el
		// tamaño

		// La matriz debe ser tan grande como el numero de variables que totales
		// que hay
		String[][] result = new String[CheckString.Var.Variables.size()][CheckString.Var.Variables
				.size()];

		String aux, CurrentVar;
		Iterator<EqStorer> it2 = CheckString.Functions.iterator();
		EqStorer Eqaux;
		int fila = 0, col = 0, n = 0;
		while (it2.hasNext()) {
			n = 0;
			col = 0;
			Eqaux = it2.next();
			aux = Eqaux.aux.get(n).GetVar();
			Iterator<VString> it = CheckString.Var.Variables.iterator();

			VString Vaux;
			while (it.hasNext()) {
				Vaux = it.next();
				CurrentVar = Vaux.getVar();
				if (CurrentVar.equals(aux)) {

					result[fila][col] = new String(Eqaux.aux.get(n).GetDeriv());

					n++;
					try {
						aux = Eqaux.aux.get(n).GetVar();
					} catch (Exception e) {
						aux = "0";
					}
				} else
					result[fila][col] = new String("0");
				if (it.hasNext())
					col++;
			}

			if (it2.hasNext())
				fila++;
		}

		return result;
	}

	    // In class matriz

    /**
     * Calculates the analytic Jacobian matrix d(Fx)/d(Xk).
     * @param Xk Vector containing the n variables (unknowns) for this system.
     * @param Fx Vector containing the m functions for this system.
     * @return The m x n Jacobian matrix as String[][], or null on error.
     */
    public static String[][] JacobianLU(vector Xk, vector Fx) {
        int m = Fx.getSize(); // Number of functions (rows)
        int n = Xk.getSize(); // Number of variables (columns)

        System.out.println("--- DEBUG: Entering JacobianLU ---");
        System.out.println("  Variables (n=" + n + "): " + Xk.toString());
        System.out.println("  Functions (m=" + m + "): " + Fx.toString());

        // Basic check: Cannot compute if no functions or no variables
        if (m == 0 || n == 0) {
            System.err.println("ERROR: Cannot compute Jacobian with zero functions or variables.");
            System.out.println("--- DEBUG: Exiting JacobianLU (Error) ---");
            return null;
        }

        String[][] result = new String[m][n]; // Correct dimensions m x n
        int row = 0;

        for (nodo funcion : Fx.vector) { // Iterate through each function F_i
            String functionString = funcion.GetCadena();
            // System.out.println("    Processing Function (row " + row + "): " + (functionString == null ? "[NULL]" : functionString)); // Verbose

            if (functionString == null) {
                System.err.println("ERROR: Function string is null at row " + row + " in JacobianLU.");
                // Fill row with error markers
                 for(int col=0; col < n; col++) result[row][col] = "\"ERROR_NULL_FUNCTION\"";
            } else {
                int col = 0;
                for (nodo variable : Xk.vector) { // Iterate through each variable X_j passed in Xk
                    String variableString = variable.GetCadena();
                     // System.out.println("      Calculating d["+ functionString + "]/d[" + (variableString == null ? "[NULL]" : variableString) + "]"); // Very verbose

                    if (variableString == null) {
                        System.err.println("ERROR: Variable string is null at col " + col + " for function row " + row + " in JacobianLU.");
                        result[row][col] = "\"ERROR_NULL_VARIABLE\"";
                    } else {
                        // Calculate derivative d(F_i) / d(X_j)
                        result[row][col] = evaluation.DiffAndEvaluator.diff(functionString, variableString);
                    }
                    col++;
                } // end variable loop
            }
            row++;
        } // end function loop

        System.out.println("--- DEBUG: Exiting JacobianLU ---");
        return result;
    }

	/**
	 * Before using this method the values must be introduce previously with the
	 * DiffAndEvaluator.IntroduceValues method. This method is to use previously
	 * to the GaussJordan.matrix2gauss method
	 * 
	 * @param J
	 * @return A jacobian evaluated in a point
	 */
	public static Array2DRowRealMatrix EvaluateJacobian(String[][] J) {

		Array2DRowRealMatrix aux = new Array2DRowRealMatrix(J.length, J.length);

		for (int fila = 0; fila < J.length; fila++)
			for (int col = 0; col < J[0].length; col++)
				aux.setEntry(fila, col, (DiffAndEvaluator
						.Evaluate(J[fila][col])));

		return aux;
	}

}