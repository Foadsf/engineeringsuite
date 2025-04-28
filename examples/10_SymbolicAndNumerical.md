# Example 10: Symbolic Derivative for Sensitivity Analysis

This example shows how to use the "eSuite Mathematics" tab to find an analytical derivative and then potentially use that result in the "eSuite Solver".

**Scenario:** Imagine you have a model where an output `Y` depends on a parameter `P` through a complex relationship, e.g., `Y = P*Sin[P] + Exp[-P^2]`, and you want to know the sensitivity `dY/dP`.

**Step 1: Find the Symbolic Derivative (Mathematics Tab)**

1.  Go to the "eSuite Mathematics" tab.
2.  In the "Command >>" input field, type the command to differentiate the expression with respect to `P`:
    ```
    Deriv[P*Sin[P] + Exp[-P^2], P]
    ```
    *(Note: `Deriv` might be the command based on `Help`. Case and brackets are likely important. Use `P` as the variable name).*
3.  Press Enter (or click "Symbolic").
4.  The console should display the analytical derivative. It will likely be something equivalent to:
    `Cos[P]*P + Sin[P] - 2*E^(-1*P^2)*P`
    *(The exact output format might vary slightly).*

**Step 2: Use the Derivative in the Solver (Solver Tab)**

1.  **Copy** the resulting derivative expression from the Mathematics console. Be careful to adjust syntax if necessary (e.g., ensure function names match Solver's expectations like `Exp`, `Sin`, `Cos`, and variables are lowercase if needed by your main model). Let's assume the Solver uses `exp`, `sin`, `cos`, and lowercase `p`. The copied expression might become:
    `cos[p]*p + sin[p] - 2*exp[-p^2]*p`
2.  Go to the "eSuite Solver" tab and click the "Equations" sub-tab.
3.  You can now add this derivative to your main set of equations. For example, if your main model calculates `y` and `p` based on other inputs, you could add:
    ```text
    /* Main model equations */
    y = p*sin[p] + exp[-p^2]
    /* ... other equations determining p ... */
    p = 0.5 /* Example value */

    /* Sensitivity Calculation */
    sensitivity_dy_dp = cos[p]*p + sin[p] - 2*exp[-p^2]*p
    ```
4.  Add `sensitivity_dy_dp` to your list of variables if needed (though here it's directly calculated).
5.  Add initial guesses if required by your main model.
6.  Click "Play".

**Outcome:**

*   The "Results" tab will show the calculated values for `y`, `p`, *and* the analytically derived sensitivity `sensitivity_dy_dp` evaluated at the solution point (`p=0.5` in this simple case).

**Comparison to EES:**

*   EES typically calculates sensitivity numerically within its parametric tables by perturbing inputs.
*   eSuite allows obtaining the *exact symbolic derivative* first using its integrated symbolic engine, which can then be evaluated numerically within the solver. This can be more accurate and provide more insight into the sensitivity relationship. The downside is the manual copy/paste and syntax adjustment step between the two tabs.