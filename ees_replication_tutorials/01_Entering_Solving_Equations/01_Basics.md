# EES Replication: Entering and Solving - Basics

*This tutorial corresponds to the EES "Entering and Solving Equations - the Basics".*

## Objective

Solve a simple system of linear equations using Engineering Suite's Solver tab.

## EES Approach

In EES, you would directly type the equations into the Equations window, check syntax, and hit Solve. Units might be handled automatically or manually.

Example EES equations:
```
X + Y = 10
X - Y = 4
```

## Engineering Suite Approach

Engineering Suite uses a similar concept but requires saving equations in a `.ris` file format or typing them directly into the GUI.

1.  **Create the Equations:**
    Open Engineering Suite, go to the "eSuite Solver" tab, and select the "Equations" sub-tab. Enter the equations, one per line:
    ```text
    X + Y = 10
    X - Y = 4
    ```
    *Note: Comments can be added using `/* comment */` or `/** comment */`.*

2.  **Add Initial Values (Optional but Recommended):**
    While not strictly necessary for this simple linear system, providing initial guesses helps the solver converge, especially for non-linear problems. Go to Edit -> Initial Values. Click "Add".
    *   Enter `X` for Variable, `1` for Value, click Add.
    *   Enter `Y` for Variable, `1` for Value, click Add.
    *   Click Close.
    *(Alternatively, save the file as a `.ris` and add the values after the `@$@%@EndOfEquationData@$@%@` marker).*

3.  **Solve:**
    Click the "Play" button (triangle icon) or press F3.

4.  **Check Results:**
    Switch to the "Results" sub-tab. You should see the calculated values:
    ```
    >> X = 7.0
    >> Y = 3.0
    ```

5.  **Check Log:**
    Switch to the "Log" sub-tab. It will show which variables were found and potentially details about the solver's iterations and convergence status. For this simple system, it should solve very quickly.

## Key Differences from EES (in this context)

*   **Units:** Engineering Suite does not have built-in unit checking or conversion. You must ensure all terms in your equations have consistent units manually.
*   **File Format:** Equations are typically managed within `.ris` files or directly in the GUI, rather than EES's proprietary format.
*   **Initial Values:** While EES often finds solutions without explicit guesses for simple systems, providing initial values is generally more critical in Engineering Suite, especially for non-linear systems.
