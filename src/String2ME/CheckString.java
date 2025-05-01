package String2ME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
	 * Performs grammatical and syntactical checks on an equation string.
	 *
	 * @param originalCadena The original line string with original casing and spaces.
	 * @param processedCadena The line string processed for parsing (lowercase, no spaces, thermo subs
	 *        done, no tabs).
	 * @return A GramErr object indicating success (0) or the type of error.
	 */
	public GramErr GramCheck(String originalCadena, String processedCadena) {

		// --- START ODE CHECK ---
		// Perform ODE check BEFORE modifying '=' or '_' for algebraic processing.
		// We still use the lowercase, space-removed 'cadena' input for the check itself,
		// but we need an unmodified version for context/target var extraction if it IS an ODE.
		// This assumes the CALLER (CommandLineRunner.parseEquations) passes the correct
		// processed string (lowercase, no spaces, thermo sub done).
		// We need the original line mainly for 'addCaseVariableIfNotPresent'. Let's assume
		// the caller can provide it or we can reconstruct it somewhat.
		// For now, we pass 'cadena' as the context placeholder. A better solution
		// might involve passing the original line into GramCheck.
		if (parseAsODE(cadena, cadena)) { // Use processed 'cadena' for both args for now
			// Successfully parsed as an ODE, handled elsewhere.
			// System.out.println("DEBUG: Line treated as ODE, skipping algebraic parse: " + cadena);
			return new GramErr((byte) 0);
		}
		// System.out.println("DEBUG: Line not an ODE, proceeding with algebraic parse: " + cadena);
		// --- END ODE CHECK ---


		// --- Proceed with Algebraic Equation Processing ONLY IF NOT an ODE ---
		String aux = new String(""); // String to build the internal MathEclipse representation
		char c = Espacio;
		char pc = Espacio; // Previous character
		int i = 0;
		boolean Change;
		boolean Equalsign = false;

		// Reset the static Pila stack before processing this algebraic line
		Pila.ErasePila();

		// Build the internal 'aux' string: '=' -> "-1*(", '_' -> "Gg"
		// Input 'cadena' is assumed lowercase, no spaces, no tabs here.
		while (i != cadena.length()) {
			Change = false;
			c = cadena.charAt(i);

			// Substitute equals sign
			if (c == Igual) {
				if (Equalsign) {
					Pila.ErasePila();
					return (new GramErr((byte) 2, c));
				}
				aux += SubsEqual;
				Equalsign = true;
				Change = true;
			}

			// Substitute underscore
			if (c == Barra) {
				aux += "Gg";
				Change = true;
			}

			// --- Basic Syntax Checks on the fly ---
			// Check for empty parenthesis/brackets using previous char 'pc'
			if ((c == CloseP || c == CloseC) && (pc == OpenP || pc == OpenC)) {
				Pila.ErasePila();
				return (new GramErr((byte) 7, c));
			}
			// Check dot validity
			if (c == Dot && !IsNumber(pc)) {
				if (!(pc == CloseP || pc == CloseC || IsLetter(pc) || IsNumber(pc))) {
					Pila.ErasePila();
					return (new GramErr((byte) 2, c));
				}
			}
			// Check double operators
			if (CheckOperator(c) && CheckOperator(pc)) {
				if (!((pc == Elevado || pc == Por || pc == Slash || pc == OpenP || pc == OpenC
						|| pc == Igual || pc == Menos || pc == Plus) && (c == Plus || c == Menos))) {
					Pila.ErasePila();
					return (new GramErr((byte) 3, c));
				}
			}
			// Check for operator followed by closing parenthesis/bracket (simple check)
			if (CheckOperator(pc) && !IsNumber(c) && !IsLetter(c) && (c == CloseP || c == CloseC)) {
				if (pc != OpenP && pc != OpenC) { // Allow f(-)
					Pila.ErasePila();
					return (new GramErr((byte) 9, pc));
				}
			}
			// Check for dot after a letter
			if (IsLetter(pc) && (c == Dot)) {
				// Allow object.method or number like var.1 for now
			}
			// Comma should not be present
			if (c == Comma) {
				System.err
						.println("DEBUG: GramCheck loop 1 - Comma found unexpectedly in processed string.");
				Pila.ErasePila();
				return new GramErr((byte) 1, c);
			}
			// --- End Basic Syntax Checks ---


			// --- Parenthesis/Bracket Handling using P2C ---
			if ((c == OpenP) || (c == OpenC) || (c == CloseP) || (c == CloseC)) {
				try {
					String translatedBracket = P2C(Character.toString(c), Character.toString(pc));
					aux += translatedBracket;
					Change = true;
				} catch (Exception e) {
					Pila.ErasePila();
					return new GramErr((byte) 6);
				}
			}
			// --- End Parenthesis/Bracket Handling ---


			// Append character if no substitution/change happened
			if (!Change) {
				aux += c;
			}

			pc = c;
			i++;
		} // End first while loop building 'aux'

		// Check for trailing operator
		if (CheckOperator(c)) {
			Pila.ErasePila();
			return (new GramErr((byte) 9, c));
		}

		// Final parenthesis balance check (should be done before tokenizing)
		if (Pila.GetSize() != -1) {
			Pila.ErasePila();
			return (new GramErr((byte) 6));
		}
		Pila.ErasePila(); // Clear stack after successful balance check

		// Proceed only if an equals sign was found
		if (Equalsign) {
			aux += ")"; // Append closing parenthesis for the "-1*(" substitution

			// Tokenize and process the internal 'aux' string
			VarThisEquation = new VList();
			String aux2 = new String(""); // Final ME-compatible expression string
			String aux3 = new String(""); // Token buffer
			String PrevToken = new String(" ");
			StringTokenizer lector = new StringTokenizer(aux, "+/*-()[]{} ^=!", true);

			while (lector.hasMoreTokens()) {
				aux3 = lector.nextToken();

				// --- Token Validation and Processing ---
				if (IsFunction(aux3)) {
					// Function
				} else if ((aux3.equals("pi")) || (aux3.equals("e"))) {
					// Constant
				} else if (aux3.length() > 0 && IsNumber(aux3.charAt(0))) {
					if (!IsNumber(aux3) && !aux3.contains("e") && !aux3.contains("E")) {
						return (new GramErr((byte) 8, aux3)); // Number followed by letter
					}
					// Number
				} else if ((aux3.equals("+")) || (aux3.equals("-")) || (aux3.equals("/"))
						|| (aux3.equals("*")) || (aux3.equals(" ")) || (aux3.equals("^"))
						|| (aux3.equals("!"))) {
					if (aux3.equals(" "))
						continue; // Skip spaces
					// Operator
				} else if ((aux3.equals("(")) || (aux3.equals(")")) || (aux3.equals("["))
						|| (aux3.equals("]"))) {
					// Parenthesis/Bracket - just append
				} else {
					// Assume Variable
					if (aux3.length() > 0) {
						if (!checkVariableInternal(aux3)) { // Validate internal Gg format
							return (new GramErr((byte) 10, aux3));
						}
						// Check number directly followed by variable
						if (PrevToken.length() > 0 && IsNumber(PrevToken.charAt(PrevToken.length() - 1))
								&& Character.isLetter(aux3.charAt(0))) {
							return (new GramErr((byte) 8, PrevToken + aux3));
						}
						// Check invalid char after opening function bracket '['
						if (PrevToken.equals("[")
								&& (aux3.equals("/") || aux3.equals("*") || aux3.equals("^") || aux3.equals("!"))) {
							return new GramErr((byte) 2, aux3.charAt(0));
						}
						VarThisEquation.AddVar(aux3); // Add internal Gg name to local list
					} else {
						continue; // Skip empty tokens
					}
				}
				// --- End Token Validation ---

				// Translate known functions/constants to MathEclipse format (TitleCase)
				if ((IsFunction(aux3)) || (aux3.equals("e")) || (aux3.equals("pi"))) {
					aux3 = f2F(aux3);
				}
				aux2 += aux3; // Append processed token to final expression string
				if (!aux3.equals(" ")) {
					PrevToken = aux3;
				}

			} // End token processing loop

			// Add variables from local list to GLOBAL Var list
			String n, internalName;
			for (int m = 0; m < VarThisEquation.getSize(); m++) {
				n = VarThisEquation.getVar(m);
				internalName = n.toLowerCase(Locale.ENGLISH); // Should already be lowercase+Gg
				Var.addCountVar(internalName, 1);
			}

			Functions.add(new EqStorer(aux2, VarThisEquation)); // Add processed equation

			return (new GramErr((byte) 0)); // Success

		} else { // No equals sign found
			if (emptyString(cadena)) // Check original cadena for emptiness
				return (new GramErr((byte) 0)); // Empty line is OK
			else {
				return (new GramErr((byte) 5)); // Equal sign missing error
			}
		}
	} // End of GramCheck method


	// --- Add Helper checkVariableInternal if needed ---
	/**
	 * Checks if a variable name in internal format (lowercase, Gg for underscore) is valid. Starts
	 * with letter, contains only letters, digits, or 'G','g'.
	 * 
	 * @param internalVarName Variable name like "tggstart".
	 * @return true if valid internal syntax.
	 */
	private boolean checkVariableInternal(String internalVarName) {
		if (internalVarName == null || internalVarName.isEmpty()) {
			return false;
		}
		// Check if starts with a letter
		if (!Character.isLetter(internalVarName.charAt(0))) {
			return false;
		}
		// Check subsequent characters
		for (int i = 1; i < internalVarName.length(); i++) {
			char ch = internalVarName.charAt(i);
			// Allow letters, digits, or G/g
			if (!Character.isLetterOrDigit(ch)) {
				if (Character.toLowerCase(ch) != 'g') { // Allow only G/g besides letters/digits
					return false;
				}
			}
		}
		return true;
	}

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
	 * Checks that the variable name is valid (starts with letter, contains only letters, numbers, or
	 * underscore). Case-insensitive for the check itself, but preserves original case.
	 * 
	 * @param varName The variable name to check.
	 * @return true if the name syntax is valid.
	 */
	public boolean checkVariable(String varName) {
		if (varName == null || varName.isEmpty()) {
			return false;
		}
		// Check if starts with a letter
		if (!Character.isLetter(varName.charAt(0))) {
			return false;
		}
		// Prevent starting with Gg literally (internal representation check)
		if (varName.length() >= 2 && varName.substring(0, 2).equalsIgnoreCase("Gg")) {
			return false;
		}

		// Check subsequent characters
		for (int i = 1; i < varName.length(); i++) {
			char ch = varName.charAt(i);
			// Allow letters, digits, or underscores
			if (!Character.isLetterOrDigit(ch) && ch != '_') { // Explicitly allow '_'
				return false; // Disallow anything else
			}
		}
		// Prevent purely internal 'Gg' from being a valid variable
		if (varName.equalsIgnoreCase("Gg")) {
			return false;
		}
		return true; // Passed all checks
	}

	/**
	 * If the input string (lowercase) is a known function like Cos, Sin, Tan, Exp,etc. Returns true.
	 */
	public boolean IsFunction(String auxLower) { // Renamed parameter for clarity
		// Assume auxLower is already lowercase
		if ((auxLower.equals("cos")) || (auxLower.equals("sin")) || (auxLower.equals("tan"))
				|| (auxLower.equals("exp")) || (auxLower.equals("log")) // handles ln via earlier
																																// substitution
				|| (auxLower.equals("sinh")) || (auxLower.equals("cosh")) || (auxLower.equals("tanh"))
				|| (auxLower.equals("arcsin")) || (auxLower.equals("arccos")) || (auxLower.equals("arctan"))
		// Add other known functions if any
		)
			return true;
		else
			return false;
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

	// --- COMPLETE Method getVariables(String) ---
	/**
	 * Populates the static CaseVariables list with potential variable names found in the input
	 * equation string, preserving their original case. This method only identifies potential names
	 * based on syntax and does NOT interact with the main CheckString.Var list used by the solver
	 * internals, nor does it perform Gg substitutions. Ignores known function names, constants (e,
	 * pi), numbers, and operators.
	 *
	 * @param cadena The input equation string.
	 */
	public void getVariables(String cadena) {
		// Basic check to avoid processing null, empty, or comment lines
		if (cadena == null)
			return;
		String trimmed = cadena.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("/*") || trimmed.startsWith("/**")) {
			return;
		}

		// Tokenize the original string to identify potential variable names.
		// Delimiters include common operators, parentheses, brackets, equals, comma, space, tab.
		// Underscore is NOT a delimiter here. Dot is complex, omitting for this stage.
		String delimiters = "+/*-()[]{} ^=!,\t"; // Removed underscore
		StringTokenizer tokenizer = new StringTokenizer(cadena, delimiters, false); // Don't return
																																								// delimiters

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			// Further split tokens that might contain dots (like Substance.Property)
			// This is a simple split, assumes dots are separators if not part of a number.
			StringTokenizer dotTokenizer = new StringTokenizer(token, ".", false);
			while (dotTokenizer.hasMoreTokens()) {
				String subToken = dotTokenizer.nextToken();

				// Validate the token/subToken as a potential variable
				// 1. Must not be empty
				// 2. Must start with a letter
				// 3. Must contain only valid characters (using checkVariable)
				// 4. Must not be a known function name (case-insensitive)
				// 5. Must not be a known constant (e, pi) (case-insensitive)
				if (subToken.length() > 0 && Character.isLetter(subToken.charAt(0))
						&& checkVariable(subToken)) {
					String lowerToken = subToken.toLowerCase(Locale.ENGLISH); // Use Locale for consistency
					if (!IsFunction(lowerToken) && !lowerToken.equals("e") && !lowerToken.equals("pi")) {
						// If all checks pass, add its ORIGINAL case to CaseVariables
						addCaseVariableIfNotPresent(subToken);
					}
				}
				// We ignore tokens that are purely numbers or operators here.
			}
		}
	}
	// --- End of Method getVariables(String) ---

	// --- COMPLETE Method getVariables(String, MaterialMethods, CheckString) ---
	/**
	 * Populates CaseVariables list, potentially after thermodynamic function substitution. This
	 * overload first attempts to substitute thermodynamic function calls in the input string before
	 * extracting variables. Otherwise, it behaves like the simpler getVariables(String) method. It
	 * only populates CaseVariables with original-case names.
	 *
	 * @param cadena The input equation string.
	 * @param Materiales Material methods instance for thermodynamic lookup.
	 * @param ch CheckString instance (needed for thermo lookup).
	 */
	public void getVariables(String cadena, MaterialMethods Materiales, CheckString ch) {
		// Basic check
		if (cadena == null)
			return;
		String trimmed = cadena.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("/*") || trimmed.startsWith("/**")) {
			return;
		}

		// --- Attempt Thermodynamic Function Substitution FIRST ---
		// This modifies the string *before* variable extraction if a thermo call is found.
		SolverGUI SGUI = new SolverGUI(); // Consider making SolverGUI methods static if possible
		GramErr thermoResult = SGUI.searchThermodynamicFunction(cadena, Materiales, ch);
		String stringToParse;
		if (thermoResult.GetTypeError() != 0) {
			// Error during thermo lookup OR it wasn't a thermo function call.
			// Proceed to parse the original string for variables.
			// System.err.println("Warning: Error/NoSubst in thermo func during var extract: " + cadena);
			stringToParse = cadena;
		} else {
			// Thermo lookup successful, use the substituted formula string for parsing.
			// System.out.println("DEBUG: Using substituted string for var extract: " +
			// thermoResult.getString());
			stringToParse = thermoResult.getString();
		}

		// --- Variable Extraction (from the potentially substituted string 'stringToParse') ---
		String delimiters = "+/*-()[]{} ^=!,\t";
		StringTokenizer tokenizer = new StringTokenizer(stringToParse, delimiters, false);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			// Handle potential dot notation within the token
			StringTokenizer dotTokenizer = new StringTokenizer(token, ".", false);
			while (dotTokenizer.hasMoreTokens()) {
				String subToken = dotTokenizer.nextToken();

				// Validate the token/subToken as a potential variable
				if (subToken.length() > 0 && Character.isLetter(subToken.charAt(0))
						&& checkVariable(subToken)) {
					String lowerToken = subToken.toLowerCase(Locale.ENGLISH);
					if (!IsFunction(lowerToken) && !lowerToken.equals("e") && !lowerToken.equals("pi")) {
						addCaseVariableIfNotPresent(subToken);
					}
				}
			}
		}
	}
	// --- End of Method getVariables(String, MaterialMethods, CheckString) ---

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

	/**
	 * Attempts to parse the line as an ODE definition using the SolveODE syntax. If successful, adds
	 * the definition to OdeProblems, tracks variables in CaseVariables, and returns true. Otherwise,
	 * returns false.
	 *
	 * @param originalLine The original input line (case preserved).
	 * @param processedLine The line processed for initial checks (lowercase, no spaces).
	 * @return true if parsed as ODE, false otherwise.
	 */
	private boolean parseAsODE(String originalLine, String processedLine) {
		// Quick check for potential ODE: presence of "=" and "solveode("
		int equalsPos = originalLine.indexOf('=');
		// Use lowercase for keyword check and ensure it appears *after* the equals
		int solveOdePos = processedLine.indexOf("solveode(");

		// Ensure SolveODE exists and appears after the equals sign
		if (equalsPos <= 0 || solveOdePos <= equalsPos) {
			return false; // Not in the format y = SolveODE(...)
		}

		// Extract target variable (preserving case from original line)
		String targetVar = originalLine.substring(0, equalsPos).trim();
		if (targetVar.isEmpty()) {
			return false; // No target variable
		}
		// --- Use the corrected checkVariable ---
		// Validate target variable name syntax (using the method that allows underscores)
		if (!checkVariable(targetVar)) { // Check original case name syntax
			System.err.println("Warning: Invalid target variable name syntax for ODE: " + targetVar);
			return false;
		}

		// Extract the content within SolveODE(...) from the processed (lowercase, no space) string
		int openParen = processedLine.indexOf('(', solveOdePos);
		int closeParen = processedLine.lastIndexOf(')');
		// Check for valid parenthesis pair for SolveODE arguments
		if (openParen == -1 || closeParen <= openParen) {
			System.err.println("Warning: Malformed SolveODE parenthesis in line: " + originalLine);
			return false; // Malformed SolveODE call
		}

		String argsString = processedLine.substring(openParen + 1, closeParen);

		// --- Argument Parsing (Handles potential quoted strings for the derivative) ---
		List<String> arguments = new ArrayList<>();
		StringBuilder currentArg = new StringBuilder();
		boolean inQuotes = false;
		int parenLevel = 0; // To handle nested parentheses within non-quoted arguments

		for (int i = 0; i < argsString.length(); i++) {
			char c = argsString.charAt(i);

			if (c == '"') {
				inQuotes = !inQuotes;
				currentArg.append(c); // Keep quotes for now, will trim later
			} else if (c == ',' && !inQuotes && parenLevel == 0) {
				// Found top-level comma outside quotes, store previous argument
				arguments.add(currentArg.toString().trim());
				currentArg.setLength(0); // Reset for next argument
			} else {
				// Track parentheses nesting only outside quotes
				if (!inQuotes) {
					if (c == '(' || c == '[')
						parenLevel++;
					else if (c == ')' || c == ']') {
						parenLevel--;
						if (parenLevel < 0) { // Mismatched closing parenthesis
							System.err.println(
									"Warning: Mismatched parenthesis inside SolveODE arguments: " + argsString);
							return false;
						}
					}
				}
				currentArg.append(c); // Append character to current argument
			}
		}
		// Add the last argument after the loop finishes
		arguments.add(currentArg.toString().trim());

		// Check if the correct number of arguments was found
		if (arguments.size() != 5) {
			System.err.println("Warning: Incorrect number of arguments for SolveODE (expected 5, found "
					+ arguments.size() + ") in line: " + originalLine);
			return false; // Expected 5 arguments
		}
		// Check for unbalanced parentheses in the last argument (or overall)
		if (parenLevel != 0) {
			System.err
					.println("Warning: Unbalanced parenthesis inside SolveODE arguments: " + argsString);
			return false;
		}


		// Trim quotes from the derivative expression if present
		String derivativeExpr = arguments.get(0);
		if (derivativeExpr.startsWith("\"") && derivativeExpr.endsWith("\"")
				&& derivativeExpr.length() >= 2) {
			derivativeExpr = derivativeExpr.substring(1, derivativeExpr.length() - 1);
		} else if (derivativeExpr.contains("\"")) {
			// Mismatched quotes or quotes inside - treat as error
			System.err.println(
					"Warning: Invalid quoting in derivative expression for SolveODE: " + arguments.get(0));
			return false;
		}
		// --- End Argument Parsing ---

		// Extract other arguments (they are already trimmed)
		String integrationVar = arguments.get(1);
		String startTimeExpr = arguments.get(2);
		String endTimeExpr = arguments.get(3);
		String initialValueExpr = arguments.get(4);

		// --- Use corrected checkVariable and isValidExpressionArgument for validation ---
		// Integration variable must be a valid variable name
		if (!checkVariable(integrationVar)) { // Use checkVariable which allows '_'
			System.err
					.println("Warning: Invalid integration variable name syntax for ODE: " + integrationVar);
			return false;
		}
		// Time/initial value expressions must be valid expressions (number or variable name)
		if (!isValidExpressionArgument(startTimeExpr)) { // isValidExpressionArgument now uses
																											// checkVariable
			System.err.println("Warning: Invalid start time expression for ODE: " + startTimeExpr);
			return false;
		}
		if (!isValidExpressionArgument(endTimeExpr)) {
			System.err.println("Warning: Invalid end time expression for ODE: " + endTimeExpr);
			return false;
		}
		if (!isValidExpressionArgument(initialValueExpr)) {
			System.err.println("Warning: Invalid initial value expression for ODE: " + initialValueExpr);
			return false;
		}
		// Derivative expression validation is still minimal here.


		// --- Store the ODE definition ---
		ODEProblemDefinition odeDef = new ODEProblemDefinition(targetVar, derivativeExpr,
				integrationVar, startTimeExpr, endTimeExpr, initialValueExpr, originalLine // Store original
																																										// line too
		);
		// Ensure OdeProblems list is initialized before adding
		if (OdeProblems == null) {
			OdeProblems = new ArrayList<>();
		}
		OdeProblems.add(odeDef); // Add to the static list

		// --- Variable Tracking (Populate CaseVariables ONLY) ---
		// Ensure CaseVariables list is initialized
		if (CaseVariables == null) {
			CaseVariables = new LinkedList<>();
		}
		addCaseVariableIfNotPresent(targetVar); // Add original case of target

		// Find original case of integration var from the original line for accuracy
		addCaseVariableIfNotPresent(findOriginalCase(integrationVar, originalLine));

		// Parse variables used *within* the expressions, using original full line as context
		getVariablesFromExpression(derivativeExpr, originalLine);
		getVariablesFromExpression(startTimeExpr, originalLine);
		getVariablesFromExpression(endTimeExpr, originalLine);
		getVariablesFromExpression(initialValueExpr, originalLine);
		// --- End Variable Tracking ---

		System.out.println("DEBUG: Successfully parsed ODE: " + odeDef); // Debug output
		return true; // Indicate successful ODE parsing
	}
	// --- End of Method parseAsODE ---

	// Helper method JUST to extract potential variable names from an expression string
	private Set<String> extractVarsOnly(String expression) {
		Set<String> foundVars = new HashSet<>();
		if (expression == null || expression.isEmpty())
			return foundVars;
		try {
			Double.parseDouble(expression.replace(',', '.')); // Ignore if just a number
			return foundVars;
		} catch (NumberFormatException e) {
		}

		String processedExpr = expression.toLowerCase();
		String token;
		StringTokenizer tokenizer = new StringTokenizer(processedExpr, "+/*-()[]{} ^=!", true);
		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken().trim();
			if (token.isEmpty())
				continue;
			if (token.length() > 0 && Character.isLetter(token.charAt(0)) && checkVariable(token)) {
				if (!IsFunction(token) && !token.equals("e") && !token.equals("pi")
						&& !token.equalsIgnoreCase("gg")) {
					foundVars.add(token);
				}
			}
		}
		return foundVars;
	}

	// Helper to check if an argument is a simple number or a valid variable name
	private boolean isValidExpressionArgument(String arg) {
		if (arg == null || arg.isEmpty())
			return false;
		try {
			Double.parseDouble(arg.replace(',', '.'));
			return true;
		} catch (NumberFormatException e) {
			// Use the corrected checkVariable
			return checkVariable(arg); // Check the argument directly
		}
	}

	/**
	 * Helper to add original case variable if not already tracked. Performs case-insensitive check
	 * before adding.
	 * 
	 * @param varName The variable name with its original casing.
	 */
	private void addCaseVariableIfNotPresent(String varName) {
		if (varName == null || varName.trim().isEmpty())
			return;
		String trimmedVarName = varName.trim();

		boolean found = false;
		if (CaseVariables == null)
			CaseVariables = new LinkedList<>(); // Initialize if needed
		for (String existing : CaseVariables) {
			if (existing.equalsIgnoreCase(trimmedVarName)) {
				found = true;
				break;
			}
		}
		if (!found) {
			// Use corrected checkVariable before adding
			if (checkVariable(trimmedVarName)) {
				CaseVariables.add(trimmedVarName);
			} else {
				// System.err.println("Warning: Prevented adding invalid name to CaseVariables: " +
				// trimmedVarName);
			}
		}
	}

	// Helper to extract and track variables from expression strings (like derivative, times, etc.)
	// This is a simplified version based on the existing getVariables logic
	private void getVariablesFromExpression(String expression, String originalContext) { // Added
																																												// originalContext
		if (expression == null || expression.isEmpty())
			return;
		// Skip if it's just a number
		try {
			// Use Locale.US to ensure dot is decimal separator regardless of system locale
			Double.parseDouble(expression.replace(',', '.'));
			return;
		} catch (NumberFormatException e) {
			// It's not a simple number, proceed to parse as potential expression
		}

		// Use a simplified tokenizer approach similar to getVariables
		String processedExpr = expression.toLowerCase(); // Work with lowercase
		String token;
		// Added '=' to delimiters just in case, though unlikely in these expression args
		StringTokenizer tokenizer = new StringTokenizer(processedExpr, "+/*-()[]{} ^=!", true);

		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			token = token.trim(); // Trim spaces from token
			if (token.isEmpty())
				continue;

			// Check if it looks like a variable (starts with letter, follows rules)
			// Use the MODIFIED checkVariable that allows underscores
			if (token.length() > 0 && Character.isLetter(token.charAt(0)) && checkVariable(token)) {
				// Check it's not a function or constant
				if (!IsFunction(token) && !token.equals("e") && !token.equals("pi")
						&& !token.equalsIgnoreCase("gg")) {
					Var.addCountVar(token, 1); // Add lowercase ONLY to main var list
					// Find original case version using the original expression/line as context
					addCaseVariableIfNotPresent(findOriginalCase(token, originalContext));
				}
			}
		}
	}

	// Helper to find original case (simple search in original expression)
	private String findOriginalCase(String lowerCaseToken, String originalExpression) {
		// Use regex word boundaries (\b) to find the token as a whole word, case-insensitive
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
				"\\b" + java.util.regex.Pattern.quote(lowerCaseToken) + "\\b",
				java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher matcher = pattern.matcher(originalExpression);
		if (matcher.find()) {
			// Found a match, return the exact substring from the original expression
			return matcher.group(0);
		}
		// Fallback if regex fails (should be rare)
		return lowerCaseToken;
	}

}
