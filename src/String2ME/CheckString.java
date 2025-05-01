package String2ME;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import solver.PrepareUncmin;

import evaluation.DiffAndEvaluator;
import gui.Config;
import gui.MaterialMethods;
import gui.SolverGUI;
import solver.ODEProblemDefinition;

/**
 * A class to translate a equation into matheclipse syntaxes
 * 
 * @author Pablo salinas
 */

@SuppressWarnings("all")
public class CheckString {

	/**
	 * Saves the variables and the times a variable appears
	 */
	public static VList Var = new VList();
	/**
	 * Saves the Variables that can be solved in one equation
	 */
	public static LinkedList<String> CaseVariables = new LinkedList<String>();;
	public static LinkedList<VString> OneEquationVar = new LinkedList<VString>();
	/**
	 * List of all equations that are global
	 */
	public static List<EqStorer> Functions = new ArrayList<EqStorer>();// Antes
	// era
	// LinkedList
	/**
	 * List of equations that have been solved. This is only to write them in the log
	 */
	public static List<EqStorer> FunctionsSolved = new ArrayList<EqStorer>();

	/**
	 * List to avoid showing the formula when using a Thermodynamic formula. Like this :
	 * water.enthalpy(Temperature, Pressure)
	 */
	public static LinkedList<PositionStorer> ResidualWorkAround = new LinkedList<PositionStorer>();

	/**
	 * List to store definitions of ODE problems found during parsing.
	 */
	public static List<ODEProblemDefinition> OdeProblems = new ArrayList<>();

	/**
	 * Variables of a single equation
	 */
	public VList VarThisEquation;

	public static final String SubsEqual = new String("-1*(");

	// variables that the equation has
	public static boolean Radianes;// Sets if we are working with degrees or not

	// ASCII values of the characters
	public static final char Por = (char) 42;
	public static final char Igual = (char) 61;
	public static final char Slash = (char) 47;
	public static final char Plus = (char) 43;
	public static final char Menos = (char) 45;
	public static final char Dot = (char) 46;
	public static final char Comma = (char) 44;
	public static final char OpenP = (char) 40;
	public static final char CloseP = (char) 41;
	public static final char Elevado = (char) 94;
	public static final char OpenC = (char) 91;
	public static final char CloseC = (char) 93;
	public static final char Exclamacion = (char) 33;
	public static final char Espacio = (char) 32;
	public static final char Barra = (char) 95;
	public static final char Tab = (char) 9;
	public static final char PuntoYcoma = (char) 59;


	/**
	 * The input string can't contain comments, use before calling this method SolverGUI.cleanComments
	 *
	 * @param cadena The input equation string.
	 * @return A GramErr object indicating success (0) or the type of error.
	 */
	public GramErr GramCheck(String cadena) {

		// Store original line before any modification for potential ODE parsing
		String originalCadena = cadena;

		// --- START ODE CHECK ---
		// Basic cleanup for ODE check (lowercase, no spaces - might need refinement)
		String processedCadenaForODECheck = cadena.toLowerCase().replace(" ", "").replace(Tab, Espacio);
		if (parseAsODE(originalCadena, processedCadenaForODECheck)) {
			// If it was successfully parsed as an ODE, we are done with this line
			// Return success (0) but don't add to algebraic Functions list later
			System.out.println("DEBUG: Line treated as ODE, skipping algebraic parse: " + originalCadena);
			return new GramErr((byte) 0); // Signal success, ODE handled elsewhere
		}
		System.out
				.println("DEBUG: Line not an ODE, proceeding with algebraic parse: " + originalCadena);
		// --- END ODE CHECK ---


		// --- Proceed with Algebraic Equation Parsing ONLY IF NOT an ODE ---
		String aux = new String("");
		char c = Espacio;
		char pc = Espacio;
		int NumC;
		int i = 0;
		int j = 0;
		boolean Change;
		boolean Equalsign = false;

		// --- IMPORTANT: Use the *original* 'cadena' for algebraic parsing ---
		while (i != cadena.length()) {
			Change = false;
			c = cadena.charAt(i);
			// --- Keep the lowercase conversion for internal algebraic processing ---
			c = Character.toLowerCase(c);
			NumC = (int) c;

			// Check for illegal characters (same logic as before)
			if (((NumC <= 57) & (NumC >= 48)) | ((NumC >= 97) & (NumC <= 122)) | (NumC == Igual)
					| (NumC == Plus) | (NumC == Menos) | (NumC == Slash) | (NumC == Espacio) | (NumC == Por)
					| (NumC == OpenP) | (NumC == CloseP) | (NumC == OpenC) | (NumC == CloseC) | (NumC == Dot)
					| (NumC == Comma) | (NumC == Elevado) | (NumC == Barra) | (NumC == Tab)) {
				/* Accepted characters */
			} else
				return (new GramErr((byte) 1, c));

			// Erase tabs
			if (c == Tab) {
				Change = true;
			}

			// Substitute equals sign
			if (c == Igual) {
				if (Equalsign)
					return (new GramErr((byte) 2, c)); // Duplicate equals
				aux += SubsEqual; // Append the internal representation "-1*("
				Equalsign = true;
				Change = true;
			}

			// Substitute underscore
			if (c == Barra) {
				aux += "Gg";
				Change = true;
			}

			// Check for empty parenthesis/brackets
			if (c == CloseP || c == CloseC)
				if (pc == OpenP || pc == OpenC)
					return (new GramErr((byte) 7, c)); // Empty function/group error

			// Check commas and dots validity
			if (((c == Comma) || (c == Dot)) && (!IsNumber(pc))) {
				// Allow dot after ')' or ']' for method calls (like Substance.Property)
				if (!((pc == CloseP || pc == CloseC) && c == Dot)) {
					// Also check if previous was a letter - should be number or ) or ]
					if (IsLetter(pc)) {
						return (new GramErr((byte) 4, c)); // Dot/comma after letter error
					}
					// Allow dot/comma after closing paren/bracket if not followed by operator
					// Let's refine this rule - a dot/comma generally needs a number before it
					// unless it's part of a specific syntax we haven't identified yet.
					// Reverting to the original stricter check for now:
					// return (new GramErr((byte) 2, c)); // Unexpected dot/comma
				}
				// If it's a dot after ) or ], we let it pass for now (might be object.method syntax)
			}


			// Check for two operators followed (excluding some +/- cases)
			if (CheckOperator(c) && CheckOperator(pc)) {
				// Allow things like x^-1 or x*(-1) or x/-2
				if (!((pc == Elevado || pc == Por || pc == Slash || pc == OpenP || pc == OpenC
						|| pc == Igual) && (c == Plus || c == Menos))) {
					// Allow double Minus/Plus like x -- 1 or x*-1
					if (!((pc == Menos || pc == Plus) && (c == Menos || c == Plus))) {
						// Allow x=-y
						if (!(pc == Igual && (c == Menos || c == Plus))) {
							return (new GramErr((byte) 3, c)); // Two operators followed error
						}
					}
				}
			}


			// Check for operator followed by closing parenthesis/bracket
			if (CheckOperator(pc) && (c == CloseP || c == CloseC))
				return (new GramErr((byte) 9, pc)); // Operator missing operand error

			// Check for dot/comma after a letter (re-check, more specific than earlier one)
			if (IsLetter(pc) && (c == Comma || c == Dot))
				return (new GramErr((byte) 4, c)); // Dot or comma after a letter error

			// Convert comma to dot for internal consistency
			if (c == Comma) {
				Change = true;
				aux += ".";
			}

			// Append character if no substitution/change happened
			if (!Change)
				aux += c;

			i++; // Move to next character in original string

			// Skip spaces logic
			if (i != cadena.length()) {
				j = i;
				i = SkipSpaces(cadena, i); // SkipSpaces uses original cadena
				if (j != i) {
					i--; // Adjust index if spaces were skipped
				}
			}
			// Save previous character (non-tab)
			if (c != Tab)
				pc = c;
		} // End while loop

		// Check for trailing operator
		if (CheckOperator(c))
			return (new GramErr((byte) 9, c)); // Operator at end of line error

		// --- Post-processing and adding to Functions list (only for algebraic) ---
		aux += ")"; // Append closing parenthesis for the "-1*(" substitution

		if (Equalsign) {
			VarThisEquation = new VList();
			String aux2 = new String(""); // Will hold the final processed algebraic expression
			String aux3 = new String(""); // Token buffer
			String PrevToken = new String(" "); // Previous token
			StringTokenizer lector = new StringTokenizer(aux, "+/*-()[]{} ^=!", true); // Tokenize
																																									// processed
																																									// string

			Pila p = new Pila(); // Use Pila for parenthesis checking/translation

			while (lector.hasMoreTokens()) {
				aux3 = lector.nextToken();

				if (aux3.equals("ln")) // Specific translation for ln
					aux3 = "log";

				// Check if token is a known function, constant, number, or operator
				if (IsFunction(aux3)) {
					/* Function: will be translated later if needed */
				} else if ((aux3.equals("pi")) || (aux3.equals("e")) || (IsNumber(aux3.charAt(0)))) {
					// Check if it's a valid number or a variable starting with a number
					if (!IsNumber(aux3) && IsNumber(aux3.charAt(0))) {
						return (new GramErr((byte) 8, aux3)); // Number followed by letter error
					}
					/* It's a number or constant Pi/E */
				} else if ((aux3.equals("+")) || (aux3.equals("-")) || (aux3.equals("/"))
						|| (aux3.equals("*")) || (aux3.equals(" ")) || (aux3.equals("^"))
						|| (aux3.equals("!"))) {
					/* Operator or space */
				} else if ((aux3.equals("(")) || (aux3.equals(")")) || (aux3.equals("["))
						|| (aux3.equals("]"))) {
					// Handle parenthesis/bracket translation and stack checking
					try {
						aux3 = P2C(aux3, PrevToken); // Translate ( to [ if needed, check stack on ) or ]
					} catch (Exception e) {
						return new GramErr((byte) 6); // Parenthesis mismatch error from Pila
					}
				} else {
					// Assume it's a variable if none of the above
					if (!checkVariable(aux3)) // Validate variable name syntax
						return (new GramErr((byte) 10, aux3)); // Invalid variable name error

					// Check for disallowed cases like `[ operator`
					if (PrevToken.equals("[")
							&& (aux3.equals("/") || aux3.equals("*") || aux3.equals("^") || aux3.equals("!"))) {
						return new GramErr((byte) 2, aux3.charAt(0)); // Unexpected character after '['
					}

					// Track the variable for this equation
					VarThisEquation.AddVar(aux3);
				}

				// Translate known functions/constants to MathEclipse format if needed
				if ((IsFunction(aux3)) || (aux3.equals("e")) || (aux3.equals("pi"))) {
					aux3 = f2F(aux3); // Translate sin to Sin, pi to Pi, etc.
				}

				// Append the processed token to the final expression string
				aux2 += aux3;
				PrevToken = aux3; // Update previous token

			} // End token processing loop

			// Add variables found in this equation to the global list
			String n;
			for (int m = 0; m < VarThisEquation.getSize(); m++) {
				n = VarThisEquation.getVar(m);
				Var.addCountVar(n, 1); // Track lowercase version globally
				// CaseVariables list is populated by getVariables method calls before GramCheck
			}

			// Add the processed algebraic equation to the Functions list
			Functions.add(new EqStorer(aux2, VarThisEquation));

			// Final check on parenthesis balance
			if (p.GetSize() != -1) { // Pila should be empty if balanced
				return (new GramErr((byte) 6)); // Parenthesis mismatch error
			} else {
				return (new GramErr((byte) 0)); // Success for algebraic equation
			}

		} else { // No equals sign found
			if (emptyString(aux))
				return (new GramErr((byte) 0)); // Empty line is OK
			else
				return (new GramErr((byte) 5)); // Equal sign missing error
		}
	} // End of GramCheck method

	/**
	 * @param c
	 * @return True if an operator is found
	 */
	public boolean CheckOperator(char c) {
		boolean OperatorFound = false;
		switch (c) {
			case Por:
				OperatorFound = true;
				break;
			case Slash:
				OperatorFound = true;
				break;
			case Igual:
				OperatorFound = true;
				break;
			case Plus:
				OperatorFound = true;
				break;
			case Menos:
				OperatorFound = true;
				break;
			case Exclamacion:
				OperatorFound = true;
				break;
			case Dot:
				OperatorFound = true;
				break;
			case Comma:
				OperatorFound = true;
				break;
			case Elevado:
				OperatorFound = true;
				break;
		}
		return OperatorFound;
	}

	// It won't have in consideration letters like ñ or ç
	// This checks if a character is a letter
	/**
	 * If is a letter returns true
	 */
	public boolean IsLetter(char c) {
		boolean LetterFound = false;
		int NumC = (int) c;
		if (((NumC <= 90) & (NumC >= 65)) || ((NumC <= 122) & (NumC >= 97)))
			LetterFound = true;

		return LetterFound;

	}

	/**
	 * Checks that the variable only has, numbers and letters(remember that _ has been translated to
	 * Gg)
	 * 
	 * @param var
	 * @return true if everything is OK
	 */
	public boolean checkVariable(String var) {

		for (int i = 0; i < var.length(); i++)
			if (!IsNumber(var.charAt(i)))
				if (!IsLetter(var.charAt(i)))
					return false;

		return true;
	}

	// This checks if a character is a number
	/**
	 * If is a number returns true
	 */
	public boolean IsNumber(char c) {
		boolean NumFound = false;
		int NumC = (int) c;
		if ((NumC <= 57) & (NumC >= 48))
			NumFound = true;

		return NumFound;
	}

	/**
	 * Checks if a String have something different from a number
	 * 
	 * @param input
	 * @return true if is a number
	 */
	public boolean IsNumber(String input) {

		for (int i = 0; i < input.length(); i++) {
			if (!IsNumber(input.charAt(i)) & !CheckOperator(input.charAt(i)))
				return false;
		}
		return true;
	}

	// Checks if the string is a function
	/**
	 * If the input string is a function like Cos, Sin, Tan, Exp,etc. Returns true.
	 */
	public boolean IsFunction(String aux) {
		if ((aux.equalsIgnoreCase("cos")) | (aux.equalsIgnoreCase("sin"))
				| (aux.equalsIgnoreCase("tan")) | (aux.equalsIgnoreCase("exp"))
				| (aux.equalsIgnoreCase("log")) | (aux.equalsIgnoreCase("sinh"))
				| (aux.equalsIgnoreCase("cosh")) | (aux.equalsIgnoreCase("tanh"))
				| (aux.equalsIgnoreCase("arcsin")) | (aux.equalsIgnoreCase("arccos"))
				| (aux.equalsIgnoreCase("arctan")) | aux.equalsIgnoreCase("ln"))
			return true;
		else
			return false;

	}

	// Skip spaces
	/**
	 * This method is to ignore spaces
	 */
	public int SkipSpaces(String cadena, int i) {
		char c = cadena.charAt(i);
		while ((c == Espacio) & (i < cadena.length())) {// There was problems
			// when the space was at
			// the end of the line
			c = cadena.charAt(i);// i think it it's fixed with the
			// i<cadena.length()
			i++;

		}
		return i;
	}

	/**
	 * This method is to translate The Parenthesis in a usual equation into clasp when its necessary
	 * to matheclipse. I.E:Cos(x*(1)) into Cos[x*(1)]
	 * 
	 */
	public String P2C(String aux, String PrevToken) {

		Pila P = new Pila();
		byte b = (byte) 0;
		switch (aux.charAt(0)) {
			case CloseP:
				b = P.GetTerm();
				break;
			case CloseC:
				b = P.GetTerm();
				break;
			default: {
				if (IsFunction(PrevToken)) {
					if (IsTrigonometric(PrevToken) & (!Radianes)) {
						P.AddTerm((byte) 2);
						return "[Degree(";
					} else {
						P.AddTerm((byte) 1);
						return "[";
					}
				} else {
					P.AddTerm((byte) 0);
					return "(";
				}
			}
		}
		if (b == (byte) 1)
			return "]";
		if (b == (byte) 2)
			return ")]";
		if (b == (byte) 0)
			return ")";
		return "";
	}

	/**
	 * 
	 * @param Input string
	 * @return if the input string is a function like, sin, cos, pi, e, tan,
	 *         exp,sinh,tanh,arcsin,artan,etc. Returns the one that mathEclipse understand like Cos
	 *         instead cos or ArcTan insted of arctan
	 */
	public String f2F(String aux) {
		if (aux.equalsIgnoreCase("cos"))
			return "Cos";
		if (aux.equalsIgnoreCase("sin"))
			return "Sin";
		if (aux.equalsIgnoreCase("tan"))
			return "Tan";
		if (aux.equalsIgnoreCase("pi"))
			return Double.toString(Math.PI);// Matheclipse understand -> "Pi"
		if (aux.equalsIgnoreCase("e"))
			return "E"; // But it gives some errors;
		if (aux.equalsIgnoreCase("exp"))
			return "Exp";
		if (aux.equalsIgnoreCase("log"))
			return "Log";
		if (aux.equalsIgnoreCase("sinh"))
			return "Sinh";
		if (aux.equalsIgnoreCase("cosh"))
			return "Cosh";
		if (aux.equalsIgnoreCase("tanh"))
			return "Tanh";
		if (aux.equalsIgnoreCase("arcsin"))
			return "ArcSin";
		if (aux.equalsIgnoreCase("arccos"))
			return "ArcCos";
		if (aux.equalsIgnoreCase("arctan"))
			return "ArcTan";

		return aux;
	}

	/**
	 * 
	 * @param aux
	 * @return True if the String is a trigonometric function
	 */
	public boolean IsTrigonometric(String aux) {
		if ((aux.equalsIgnoreCase("cos")) | (aux.equalsIgnoreCase("sin"))
				| (aux.equalsIgnoreCase("tan")) | (aux.equalsIgnoreCase("arcsin"))
				| (aux.equalsIgnoreCase("arccos")) | (aux.equalsIgnoreCase("arctan")))
			return true;

		else
			return false;

	}

	/**
	 * Checks if the input string has only spaces and at the end a )
	 * 
	 * @param input
	 * @return True if the string has only spaces
	 */
	private boolean emptyString(String input) {
		boolean empty = true;
		Character c;
		for (int i = 0; i < input.length() - 1; i++) {
			c = input.charAt(i);
			if (!c.equals(Espacio))
				empty = false;
		}

		return empty;
	}

	/**
	 * 
	 * @param cadena Saves the variables of an equation in an array, but this method saves the case
	 *        information; This method ignores the thermodynamic call functions
	 */
	public void getVariables(String cadena) {

		String aux = new String("");
		char c, pc;
		int i = 0;
		int j = 0;
		boolean Change;
		boolean Equalsign = false;

		while (i != cadena.length()) {
			Change = false;
			c = cadena.charAt(i);

			if (!false) {
				// At first erase tabs
				if (c == Tab) {
					Change = true;
				}

				// This is the way i decided to make a equation to look like
				// this 0=something from this
				// something=other something
				if (c == Igual) {
					aux += SubsEqual;
					Equalsign = true;
					Change = true;
				}
				if (c == OpenC) {
					aux += "(";
					Change = true;
				}
				if (c == CloseC) {
					aux += ")";
					Change = true;
				}
				// As matheclipse can't use "_" we make here a change to make it
				// possible, later we must
				// translate a "Gg" to a _ to show in the results
				if (c == Barra) {
					aux += "Gg";
					Change = true;
				}

				// Because of the MathEclipse libraries does not use the commas,
				// only dots, i make this conversion to
				// allow both possibilities
				if (c == Comma) {
					Change = true;
					aux += ".";
				}

				// Write the character only if there were no changes
				if (!Change)
					aux += c;

			}
			i++;

			if (i != cadena.length()) {// If something fails, maybe is this XD
				j = i;
				i = SkipSpaces(cadena, i);
				if (j != i) {
					i--;
				}
			}
			// Save previous character
			if (c != Tab)
				pc = c;
		}
		aux += ")";
		try {
			if (Equalsign) {
				String[] variables = DiffAndEvaluator.getVariables(aux);
				// Saves the variable only if it is not already in the List
				// and if it is not an special function like: cos, sin, pi, etc.
				for (String s : variables) {
					Change = true;
					if (IsFunction(s) | s.equalsIgnoreCase("e") | s.equalsIgnoreCase("Pi")
							| s.equalsIgnoreCase(""))
						Change = false;

					if (Change)
						for (String S : CheckString.CaseVariables)
							if (s.equalsIgnoreCase(S))
								Change = false;

					if (Change)
						CheckString.CaseVariables.add(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the variables of an equation in an array, but this method saves the case information and
	 * have in consideration the thermodynamic calls.
	 * 
	 * @param cadena
	 * @param Materiales
	 * @param ch
	 */
	public void getVariables(String cadena, MaterialMethods Materiales, CheckString ch) {

		SolverGUI SGUI = new SolverGUI();
		// If the equation is a thermodynamic call it will be translated to it's
		// formula
		cadena = SGUI.searchThermodynamicFunction(cadena, Materiales, ch).getString();
		String aux = new String("");
		char c, pc;
		int i = 0;
		int j = 0;
		boolean Change;
		boolean Equalsign = false;

		while (i != cadena.length()) {
			Change = false;
			c = cadena.charAt(i);

			if (!false) {
				// At first erase tabs
				if (c == Tab) {
					Change = true;
				}

				// This is the way i decided to make a equation to look like
				// this 0=something from this
				// something=other something
				if (c == Igual) {
					aux += SubsEqual;
					Equalsign = true;
					Change = true;
				}
				if (c == OpenC) {
					aux += "(";
					Change = true;
				}
				if (c == CloseC) {
					aux += ")";
					Change = true;
				}
				// As matheclipse can't use "_" we make here a change to make it
				// possible, later we must
				// translate a "Gg" to a _ to show in the results
				if (c == Barra) {
					aux += "Gg";
					Change = true;
				}

				// Because of the MathEclipse libraries does not use the commas,
				// only dots, i make this conversion to
				// allow both possibilities
				if (c == Comma) {
					Change = true;
					aux += ".";
				}

				// Write the character only if there were no changes
				if (!Change)
					aux += c;

			}
			i++;

			if (i != cadena.length()) {// If something fails, maybe is this XD
				j = i;
				i = SkipSpaces(cadena, i);
				if (j != i) {
					i--;
				}
			}
			// Save previous character
			if (c != Tab)
				pc = c;
		}
		aux += ")";
		try {
			if (Equalsign) {
				String[] variables = DiffAndEvaluator.getVariables(aux);
				// Saves the variable only if it is not already in the List
				// and if it is not an special function like: cos, sin, pi, etc.
				for (String s : variables) {
					Change = true;
					if (IsFunction(s) | s.equalsIgnoreCase("e") | s.equalsIgnoreCase("Pi")
							| s.equalsIgnoreCase(""))
						Change = false;

					if (Change)
						for (String S : CheckString.CaseVariables)
							if (s.equalsIgnoreCase(S))
								Change = false;

					if (Change)
						CheckString.CaseVariables.add(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method clean up all the variables used in iteration this is necessary because matheclipse
	 * remembers everything so if a variable was defined like a constant then in the next use of it it
	 * will still be a constant, so no derivatives will be possible
	 */
	public static void PurgeAll() {

		DiffAndEvaluator.PurgeVar();

		Functions.clear();

		CaseVariables.clear();

		FunctionsSolved.clear();

		Var.Variables.clear();

		OneEquationVar.clear();

		ResidualWorkAround.clear();

		// This is to erase the stack that checks and translates the parenthesis
		// and clasps
		Pila.ErasePila();

		// Clear Gradient, Hessian, Jacobian...whatever
		PrepareUncmin.clear();

		OdeProblems.clear(); // Clear ODE definitions as well

		// Restart Global variables
		Config.ErrorFound = false;
		evaluation.DiffAndEvaluator.TimeLimitExceeded = false;
		SolverGUI.ResidualsHigh = false;

	}

	// --- File: src/String2ME/CheckString.java ---

	/**
	 * Attempts to parse the line as an ODE definition using the SolveODE syntax. If successful, adds
	 * the definition to OdeProblems and returns true. Otherwise, returns false.
	 *
	 * @param originalLine The original input line (case preserved).
	 * @param processedLine The line processed for initial checks (lowercase, no spaces).
	 * @return true if parsed as ODE, false otherwise.
	 */
	private boolean parseAsODE(String originalLine, String processedLine) {
		// Quick check for potential ODE: presence of "=" and "solveode("
		int equalsPos = originalLine.indexOf('=');
		int solveOdePos = processedLine.indexOf("solveode("); // Use lowercase for keyword check

		if (equalsPos <= 0 || solveOdePos <= equalsPos) {
			return false; // Not in the format y = SolveODE(...)
		}

		// Extract target variable (preserving case)
		String targetVar = originalLine.substring(0, equalsPos).trim();
		if (targetVar.isEmpty() || !Character.isLetter(targetVar.charAt(0))) {
			return false; // Invalid target variable
		}
		// Basic validation - can enhance later
		if (!checkVariable(targetVar.toLowerCase())) { // Check lowercase version against rules
			// Consider returning a specific error if needed, for now just fail parsing
			return false;
		}

		// Extract the content within SolveODE(...)
		int openParen = processedLine.indexOf('(', solveOdePos);
		int closeParen = processedLine.lastIndexOf(')');
		if (openParen == -1 || closeParen <= openParen) {
			return false; // Malformed SolveODE call
		}

		String argsString = processedLine.substring(openParen + 1, closeParen);

		// --- Argument Parsing (Simplified - Assumes no commas within derivative expression) ---
		// We need to handle potential quoted strings for the derivative
		List<String> arguments = new ArrayList<>();
		StringBuilder currentArg = new StringBuilder();
		boolean inQuotes = false;
		int parenLevel = 0; // To handle nested parentheses within arguments if not quoted

		for (int i = 0; i < argsString.length(); i++) {
			char c = argsString.charAt(i);

			if (c == '"') {
				inQuotes = !inQuotes;
				currentArg.append(c); // Keep quotes for now, will trim later
			} else if (c == ',' && !inQuotes && parenLevel == 0) {
				arguments.add(currentArg.toString().trim());
				currentArg.setLength(0); // Reset for next argument
			} else {
				if (!inQuotes) { // Only track parentheses outside quotes
					if (c == '(' || c == '[')
						parenLevel++;
					else if (c == ')' || c == ']')
						parenLevel--;
				}
				currentArg.append(c);
			}
		}
		arguments.add(currentArg.toString().trim()); // Add the last argument

		if (arguments.size() != 5) {
			// Specific error could be raised here if needed
			return false; // Expected 5 arguments
		}

		// Trim quotes from the derivative expression if present
		String derivativeExpr = arguments.get(0);
		if (derivativeExpr.startsWith("\"") && derivativeExpr.endsWith("\"")
				&& derivativeExpr.length() >= 2) {
			derivativeExpr = derivativeExpr.substring(1, derivativeExpr.length() - 1);
		} else if (derivativeExpr.contains("\"")) {
			// Mismatched quotes or quotes inside - treat as error for now
			return false;
		}
		// --- End Argument Parsing ---


		String integrationVar = arguments.get(1);
		String startTimeExpr = arguments.get(2);
		String endTimeExpr = arguments.get(3);
		String initialValueExpr = arguments.get(4);

		// Basic validation of argument names (can be enhanced)
		if (!checkVariable(integrationVar.toLowerCase()) ||
		// Allow numbers or variables for time/initial value for now
				!isValidExpressionArgument(startTimeExpr) || !isValidExpressionArgument(endTimeExpr)
				|| !isValidExpressionArgument(initialValueExpr)) {
			return false;
		}

		// Store the definition
		ODEProblemDefinition odeDef = new ODEProblemDefinition(targetVar, derivativeExpr,
				integrationVar, startTimeExpr, endTimeExpr, initialValueExpr, originalLine // Store original
																																										// line too
		);
		OdeProblems.add(odeDef);

		// Add variables to tracking lists (preserving case from original line/args)
		// Target variable
		Var.addCountVar(targetVar.toLowerCase(), 1); // Track lowercase for solver internals
		addCaseVariableIfNotPresent(targetVar); // Track original case

		// Integration variable
		Var.addCountVar(integrationVar.toLowerCase(), 1);
		addCaseVariableIfNotPresent(integrationVar);

		// Also parse variables used *within* the expressions
		getVariablesFromExpression(derivativeExpr);
		getVariablesFromExpression(startTimeExpr);
		getVariablesFromExpression(endTimeExpr);
		getVariablesFromExpression(initialValueExpr);

		System.out.println("DEBUG: Successfully parsed ODE: " + odeDef); // Debug output
		return true; // Indicate successful ODE parsing
	}

	// Helper to check if an argument is a simple number or a valid variable name
	private boolean isValidExpressionArgument(String arg) {
		if (arg == null || arg.isEmpty())
			return false;
		// Try parsing as double
		try {
			Double.parseDouble(arg.replace(',', '.')); // Allow comma decimal temporarily
			return true;
		} catch (NumberFormatException e) {
			// Not a number, check if it's a valid variable name
			if (!Character.isLetter(arg.charAt(0)))
				return false; // Must start with letter
			return checkVariable(arg.toLowerCase()); // Check against variable rules
		}
	}

	// Helper to add original case variable if not already tracked
	private void addCaseVariableIfNotPresent(String varName) {
		boolean found = false;
		for (String existing : CaseVariables) {
			if (existing.equalsIgnoreCase(varName)) {
				found = true;
				break;
			}
		}
		if (!found) {
			CaseVariables.add(varName);
		}
	}

	// Helper to extract and track variables from expression strings (like derivative, times, etc.)
	// This is a simplified version based on the existing getVariables logic
	private void getVariablesFromExpression(String expression) {
		if (expression == null || expression.isEmpty())
			return;
		// Skip if it's just a number
		try {
			Double.parseDouble(expression.replace(',', '.'));
			return;
		} catch (NumberFormatException e) {
			// It's not a simple number, proceed to parse as potential expression
		}

		// Use a simplified tokenizer approach similar to getVariables
		String processedExpr = expression.toLowerCase(); // Work with lowercase
		String token;
		StringTokenizer tokenizer = new StringTokenizer(processedExpr, "+/*-()[]{} ^=!", true); // Include
																																														// space
																																														// as
																																														// delimiter

		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			if (token.equals(" "))
				continue; // Skip spaces

			// Check if it looks like a variable (starts with letter, follows rules)
			if (token.length() > 0 && Character.isLetter(token.charAt(0)) && checkVariable(token)) {
				// Check it's not a function or constant
				if (!IsFunction(token) && !token.equals("e") && !token.equals("pi")) {
					Var.addCountVar(token, 1); // Add lowercase to main var list
					// Find original case version (best effort) and add if needed
					addCaseVariableIfNotPresent(findOriginalCase(token, expression));
				}
			}
		}
	}

	// Helper to find original case (simple search in original expression)
	private String findOriginalCase(String lowerCaseToken, String originalExpression) {
		int index = originalExpression.toLowerCase().indexOf(lowerCaseToken);
		if (index != -1) {
			// Found a potential match, extract the substring with original casing
			// This is imperfect if the token appears multiple times with different casing
			return originalExpression.substring(index, index + lowerCaseToken.length());
		}
		return lowerCaseToken; // Fallback to lowercase if not found (should ideally not happen)
	}

}
