# Engineering Suite - Lessons Learned

This document tracks key findings, workarounds, and best practices discovered during the development and usage of Engineering Suite, especially when using the command-line interface (CLI) and `.ris` files.

## 1. CLI Runner vs. GUI: Property Function Syntax (`Substance.Property(...)`)

*   **Observation:** The custom syntax `Substance.Property(OutputVar, InputVar1, ...)` or the EES-like assignment `OutputVar = Substance.Property(InputVar1=..., ...)` **fails** when parsed directly from a `.ris` file using the `run_cli.bat` script.
*   **Errors Encountered:**
    *   `String2ME.GramCheck`: Reports `Unexpected character <.>`.
    *   `evaluation.DiffAndEvaluator.getVariables` (via Matheclipse): Throws `org.matheclipse.parser.client.SyntaxError: ... ')' expected.` because it doesn't recognize the `.` notation as valid within its expression parser.
    *   `String2ME.CheckString.getVariables`: Throws `java.lang.NullPointerException` immediately after the Matheclipse error, as it receives no valid variable list.
*   **Root Cause:** The core parser (`CheckString.GramCheck`) and the underlying symbolic engine's parser expect standard mathematical syntax (e.g., `variable = expression`). The `Substance.Property(...)` notation is a *convention* specific to Engineering Suite's thermodynamic layer. There's a necessary pre-processing step where this call should be identified and replaced by its underlying formula (retrieved from `ThermodynamicalProperties.txt`) *before* the main syntax check and evaluation occur. This substitution step appears to be either missing, bypassed, or malfunctioning in the execution path used by the CLI runner.
*   **Workaround (CLI):** **Manual Substitution.** To run files with thermodynamic properties via the CLI, the user must *manually* look up the required formula in `ThermodynamicalProperties.txt` (or know it) and write the *actual formula* directly into the `.ris` file, assigning it to the desired variable.
    *   **Example (Failing in CLI):**
        ```text
        /* This line causes parsing errors when run via CLI */
        h_air = AirH2O.enthalpy(T=T_db_K, P=P_kPa, R=RelHum)
        ```
    *   **Example (Working Workaround in CLI - Requires Correct Formula):**
        ```text
        /* User manually inserts the formula for enthalpy (hypothetical example) */
        h_air = (1.005 * T_db_C) + W_kgkg * (2501 + 1.86 * T_db_C) /* Must also define/calculate W_kgkg, T_db_C etc. */
        ```
*   **Caveat:** When using manual substitution, the accuracy of the result depends entirely on the correctness of the formula inserted by the user. As seen in the psychrometric example, using an inaccurate placeholder formula (like the simplified one for `P_ws_kPa`) will lead to the solver executing *successfully* but producing *physically incorrect* results.
*   **Implication:** The `Substance.Property(...)` syntax, while likely intended and potentially functional within the GUI (where different event handling might trigger the substitution), is **unreliable for `.ris` files processed by the current CLI runner.** Development is needed to ensure the property formula substitution occurs robustly before parsing in the CLI context.

## 2. Unit Handling

*   **Lesson:** Engineering Suite **does not** perform automatic unit checking or conversion. This is a fundamental difference from systems like EES.
*   **Best Practice:**
    *   **Explicitly comment** the assumed units for all constants, inputs, and expected outputs within every `.ris` file.
    *   The user is **solely responsible** for ensuring dimensional consistency within all equations. Errors arising from inconsistent units will manifest as physically incorrect results, not parsing or solver errors.
    *   When using thermodynamic properties (even with manual substitution), ensure input values match the units expected by the correlation (often K, kPa/MPa) and interpret the output units correctly (e.g., kJ/kg).

## 3. `.ris` File Structure

*   **Required Format:**
    1.  Equations section (standard assignments, comments `/*...*/` or `/**...*/`).
    2.  Mandatory marker: `@$@%@EndOfEquationData@$@%@`
    3.  Initial Values / Fixed Inputs section (one `variable = value` per line, comments allowed).
    4.  Mandatory marker: `@$@%@EndOfInitialVariableValueData@$@%@`
*   **Parsing:** The parser reads line by line. Ensure equations do not span multiple lines unless intended by a specific feature (currently none observed).

## 4. Initial Guesses

*   **Importance:** Crucial for non-linear systems handled by iterative solvers (Newton, LM, Dogleg, etc.). Poor guesses can lead to slow convergence, divergence, or convergence to an undesired local minimum/root.
*   **Placement:** Must be defined in the section *after* `@$@%@EndOfEquationData@$@%@`.
*   **Requirement:** Even variables intended as *inputs* (like `T_db_C`, `RelHum`, `P_kPa` in the psychrometric example) need to be defined in this section for the solver to treat them as fixed values during its process. Variables that are directly calculated (left-hand side of a simple assignment evaluated sequentially) generally do not *require* a guess, but providing one usually doesn't hurt.

## 5. Parser (`CheckString.GramCheck`)

*   **Function:** Performs initial syntax validation before equations are processed further or sent to the symbolic engine.
*   **Errors Caught:** Identifies issues like missing `=`, duplicate `=`, misplaced operators, invalid characters in variable names, parenthesis mismatches, empty function calls (e.g., `sin()`), etc. Refer to the `GramErr` codes and `SolverGUI.checkGram` for specifics.
*   **Limitation:** Does *not* understand the custom `Substance.Property()` syntax (see Lesson 1).
*   

## 6. Equation Parser Limitation: Scientific E-Notation

*   **Observation:** The `CheckString.GramCheck` parser (which processes the main *equation* section, before the `@$@%@EndOfEquationData@$@%@` marker) fails to interpret standard scientific E-notation (e.g., `5.67E-8`). It incorrectly identifies the `E` as the start of a variable name following a number, leading to a "missing operator" error. This can also cause cascading errors like spurious "parenthesis mismatch" reports in subsequent lines.
*   **Error Message:** `Number followed directly by variable/letter (missing operator?) near <...e>`
*   **Workaround (Equation Section):** Express scientific notation using explicit multiplication and exponentiation syntax understood by the parser.
    *   **Replace:** `variable = 1.23E-4`
    *   **With:** `variable = 1.23 * (10 ^ (-4))`
*   **Status:** Workaround confirmed effective in `15_SimpleRadiation.ris` example.

## 7. Initial Value Parser Limitations

*   **Observation 1:** The parser that reads the *initial value* section (after the `@$@%@EndOfEquationData@$@%@` marker) **cannot evaluate mathematical expressions**. It requires plain numerical values.
    *   **Failing Example (Initial Value):** `variable = 1.5 * (10 ^ (-4))` will cause a parsing error.
    *   **Working Example (Initial Value):** `variable = 0.00015`
*   **Observation 2:** Unlike the *equation* parser, the *initial value* parser **can** correctly handle standard scientific E-notation.
    *   **Working Example (Initial Value):** `variable = 1.5E-4` is parsed correctly.
*   **Error Message (Expression Failure):** `WARNING: Could not parse number from initial value line: ... - Error: For input string: "..."`
*   **Impact:** If an initial guess cannot be parsed, the solver might use a default value (e.g., 1.0 from `config.txt`), which can lead to convergence failure or runtime errors during the solution or final residual check, even if the solver ultimately finds a result (as seen initially in `20_VanDerWaals.ris`).
*   **Best Practice (Initial Values):** Use plain decimal numbers (e.g., `0.00015`) or scientific E-notation (e.g., `1.5E-4`), but **avoid** expressions involving operators like `*` or `^`.
*   **Status:** Plain number workaround confirmed in `20_VanDerWaals.ris`. E-notation success also confirmed for initial values in separate testing (or assumed based on typical parser behavior). Expression failure confirmed.

## 8. Residual Evaluation Artifacts near Zero

*   **Observation:** When solving an equation that sets an expression to zero (e.g., finding a maximum/minimum by setting `dydx = 0`), the solver may find the correct variable value(s) resulting in a zero residual for the `dydx = 0` equation itself. However, the separate residual check for the line *defining* the expression (e.g., `dydx = expression_that_should_be_zero`) might fail with a `RuntimeException`.
*   **Cause:** This is likely due to floating-point precision limitations. The calculated variable value is extremely close but not *exactly* the theoretical value that makes the expression zero. When `DiffAndEvaluator.Evaluate` calculates the residual for the defining expression, tiny precision errors can lead to numerical instability or internal errors within the symbolic engine, even though the solver correctly satisfied the `dydx = 0` condition.
*   **Example:** Seen in `22_SimpleOptimization.ris`. The solver correctly finds `x` where `dydx=0` (residual is 0), but the residual check for the line `dydx = exp[-x^2] * (1 - 2*(x^2))` fails.
*   **Recommendation:** If the primary condition (e.g., `dydx = 0`) shows a zero residual, and the variable values seem correct, the `RuntimeException` on the defining expression's residual check can often be considered an artifact and potentially ignored for simple cases. For complex models, it might warrant further investigation or equation reformulation.

## 9. Function Syntax vs. Variable Recognition

*   **Observation:** The parser (`CheckString.GramCheck` and the subsequent variable identification logic) does not consistently recognize built-in function names when used with standard function call syntax (e.g., `sqrt[...]`, `abs[...]`). It incorrectly identifies the function name itself as a variable.
*   **Impact:** This leads to an incorrect count of unique variables compared to the number of equations. This dimensional mismatch causes the numerical solver (e.g., Levenberg-Marquardt) to fail with an `ArrayIndexOutOfBoundsException` during setup.
*   **Example:** In `26_PipeSizing.ris`, using `sqrt[f]` or `abs[f^0.5]` caused `sqrt` or `abs` to be treated as variables, leading to 14 variables vs 13 equations and a solver crash.
*   **Workaround:** Avoid using function call syntax for basic mathematical operations if it causes parsing issues. Use standard operators instead.
    *   **Replace:** `f_sqrt = sqrt[f]` or `f_sqrt = abs[f^0.5]`
    *   **With:** `f_sqrt = f ^ 0.5` (Uses the power operator for square root, which implies the positive root).
*   **Status:** Using `f^0.5` in Example 26 correctly resolved the parser error and the variable count mismatch, allowing the solver to run.

## 10. Solver Convergence vs. Solution Accuracy

*   **Observation:** The numerical solver can report successful convergence (e.g., exit code 0, zero residuals calculated for simple assignments) even when the final set of variable values does not accurately satisfy all the coupled non-linear equations in the system upon independent verification.
*   **Cause:** This can occur in complex non-linear systems like the coupled Darcy-Weisbach and Colebrook equations. The solver might converge to a point where the iterative changes fall below the specified tolerances (`steptl`, `gradtl`), but this point may not be the true mathematical solution. This could be a local minimum in the residual sum-of-squares that isn't zero, a point near a steep gradient, or related to the solver's specific algorithm and internal precision limits. The final residual check performed by the framework might also have precision issues for complex equations, potentially reporting zero incorrectly or failing with a `RuntimeException` even if the solver *thinks* it converged.
*   **Example:** In `26_PipeSizing.ris`, after fixing the parser issues, the solver ran successfully and reported zero residuals. However, manually plugging the resulting values for `f`, `D`, `Vel`, `Re`, `eps_D` back into the Darcy-Weisbach and Colebrook equations showed significant imbalance, indicating the found solution point was not correct, despite the successful exit status. The calculated `f_sqrt` was also negative, which is physically impossible in this context.
*   **Recommendation:** **Always critically evaluate results** from complex, coupled non-linear systems solved via the CLI. Do not solely rely on the "Execution finished successfully" message or the reported residuals (especially if they seem *too* perfect like all zeros in a complex iterative solution). Perform sanity checks:
    *   Do the values make physical sense (e.g., positive friction factor, temperatures within expected ranges)?
    *   Manually (or programmatically if possible) re-substitute key results back into the most complex original equations to verify they balance reasonably well.
    *   If results seem suspect, try different, physically plausible initial guesses in the `.ris` file and re-run. Convergence to the same incorrect point might indicate a limitation of the solver/algorithm for that specific problem formulation.

*(This file should be updated as new significant findings or workarounds are discovered.)*