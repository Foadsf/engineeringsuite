# EES Replication: Using Thermodynamic Properties

*This tutorial corresponds to parts of the EES "Property Functions" section, specifically using built-in thermophysical properties.*

## Objective

Calculate a thermodynamic property (like enthalpy, specific heat, etc.) of a substance at a given state using Engineering Suite's built-in database.

## EES Approach

EES has a comprehensive library of built-in property functions for various substances (real fluids, ideal gases, psychrometrics, brines, incompressible). The syntax typically involves specifying the substance, the desired property, and the independent variables with their units.

Example EES function call:
```EES
h_air = enthalpy(Air, T=T_K)  // Calculates enthalpy of Air given temperature T_K
Cp_butane = CP(Butane, T=T_K) // Calculates specific heat of Butane given temperature T_K
T_sat = T_sat(Steam, P=P_kPa) // Calculates saturation temperature of Steam given pressure P_kPa
```
EES handles unit consistency automatically based on the system settings.

## Engineering Suite Approach

Engineering Suite includes a database of property correlations stored in `ThermodynamicalProperties.txt`. These are accessed within the Solver using a specific syntax.

1.  **Syntax:** The general format for calling a property function is:
    ```
    SubstanceName.PropertyName(OutputVar1, OutputVar2, ..., InputVar1, InputVar2, ...)
    ```
    *   `SubstanceName`: The name of the substance as listed in the database (e.g., `Air`, `Butane`, `SteamSaturated`).
    *   `PropertyName`: The name of the property function (e.g., `Enthalpy`, `Cp`, `SaturationTemp_1`).
    *   `OutputVar...`: The variable(s) where the calculated property value(s) will be stored.
    *   `InputVar...`: The variable(s) providing the state information (e.g., Temperature, Pressure).
    *   **Crucially:** The *order* of the output and input variables must exactly match the order specified in the `Variable:` line for that specific property in `ThermodynamicalProperties.txt` or as shown in the Thermodynamic Formulas window (Edit -> Thermodynamic formulas).

2.  **Finding Available Properties:**
    *   Open Engineering Suite.
    *   Go to the menu: **Edit -> Thermodynamic formulas** (or press F5).
    *   This window lists available substances. Selecting a substance shows its available properties.
    *   Selecting a property shows its required input/output variables, their order, and any notes (like units or validity range).

3.  **Example `.ris` File (Calculating Cp of Air):**
    Let's replicate calculating the specific heat (Cp) of air at a given temperature.

    ```text
    /* EES Replication Example: Air Cp Calculation */

    /** Goal: Find Cp of air at 500 K **/

    Target_Temperature = 500 /* Kelvin */

    /** Call the property function **/
    /* From database: Air.Cp(Cp, Temperature) */
    /* Note the order: Output Cp comes first, Input Temperature second */
    /* We use our variable names: Result_Cp and Target_Temperature */

    Air.Cp(Result_Cp, Target_Temperature)

    @$@%@EndOfEquationData@$@%@
    /* No initial guess needed as Result_Cp is explicitly calculated */
    @$@%@EndOfInitialVariableValueData@$@%@
    ```

4.  **Solve:**
    *   Save the above content as `examples\11_AirCpExample.ris`.
    *   Open the file in Engineering Suite (File -> Open).
    *   Click the "Play" button (or press F3).

5.  **Check Results:**
    *   Go to the "Results" tab.
    *   You should see a value calculated for `Result_Cp`. Based on typical air properties, it should be slightly above 1.0 kJ/kg-K (around 1.02-1.03 kJ/kg-K at 500K).

## Key Differences from EES (in this context)

*   **Syntax:** Engineering Suite uses `Substance.Property(Out, ..., In, ...)` whereas EES uses `Prop = PropertyFunction(Substance, In=Val, ...)`. The order of variables in eSuite is critical and defined by the database entry.
*   **Units:** Engineering Suite **does not** perform unit checking or conversion for property functions. The user *must* provide inputs in the units expected by the correlation (usually specified in the notes, often SI units like K, MPa, kJ/kg) and interpret the output accordingly. EES handles units based on its settings.
*   **Function Discovery:** In eSuite, you primarily discover functions via the "Edit -> Thermodynamic formulas" window or by inspecting `ThermodynamicalProperties.txt`. EES uses its Function Information dialog.
*   **Database Scope:** The included database in Engineering Suite (`ThermodynamicalProperties.txt`) is much smaller and less comprehensive than the standard EES property library (which often includes REFPROP data, incompressible substances, etc.). However, eSuite allows users to add their own correlations by editing the text file or using the GUI.
*   **Error Handling:** If you provide inputs outside the validity range mentioned in the function's notes, eSuite might produce inaccurate results without warning, whereas EES often provides warnings or errors for out-of-range inputs.

Engineering Suite demonstrates the *capability* to integrate property correlations directly into the equation set for solving, similar to EES, but requires more manual setup and vigilance regarding units and variable order.