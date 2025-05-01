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

## 6. Parser Limitation: Scientific E-Notation

*   **Observation:** The `CheckString.GramCheck` parser fails to interpret standard scientific E-notation (e.g., `5.67E-8`). It incorrectly identifies the `E` as the start of a variable name following a number, leading to a "missing operator" error. This can also cause cascading errors like spurious "parenthesis mismatch" reports in subsequent lines.
*   **Error Message:** `Number followed directly by variable/letter (missing operator?) near <...e>`
*   **Workaround:** Express scientific notation using explicit multiplication and exponentiation syntax understood by the parser.
    *   **Replace:** `variable = 1.23E-4`
    *   **With:** `variable = 1.23 * (10 ^ (-4))`
*   **Status:** Workaround confirmed effective in `15_SimpleRadiation.ris` example.

*(This file should be updated as new significant findings or workarounds are discovered.)*