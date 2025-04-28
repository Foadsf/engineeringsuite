# EES Replication: Symbolic/Numerical Integration

*This tutorial covers functionality similar to EES "Integration and Differential Equations" using the Mathematics tab for symbolic integration and numerical integration of definite integrals.*

## Objective

Perform symbolic (indefinite) and numerical (definite) integration of a function using Engineering Suite's Mathematics capabilities.

## EES Approach

EES primarily focuses on solving systems of algebraic and differential equations numerically.
*   **Differential Equations:** EES has a built-in Ordinary Differential Equation (ODE) solver accessible via the `Integral` function within the Equations window or through the Integral Table for solving initial value problems. This is primarily numerical.
*   **Definite Integrals:** EES can calculate definite integrals numerically using the `Integral` function or Integral Table.
*   **Symbolic Integration:** EES has limited symbolic capabilities; complex symbolic integration usually isn't its primary strength.

Example EES usage for definite integral:
```EES
Result = integral(x^2, x, 0, 2) // Calculates integral of x^2 from x=0 to x=2
```

## Engineering Suite Approach

Engineering Suite leverages its symbolic backend (Matheclipse/symja) via the "eSuite Mathematics" tab to perform both symbolic (indefinite) and numerical (definite) integration. It does **not** have a dedicated GUI for solving ODEs like EES.

**1. Symbolic (Indefinite) Integration:**

1.  **Switch Tab:** Go to the "eSuite Mathematics" tab.
2.  **Enter Command:** In the "Command >>" input field, use the `Integrate` command with the function and the variable of integration.
    *Example:* To find the indefinite integral of `x^2 * Sin[x]` with respect to `x`:
    ```
    Integrate[x^2 * Sin[x], x]
    ```
    *(Syntax Note: Use capital `Integrate`, square brackets `[]`, and ensure function/variable names match symbolic engine conventions).*
3.  **Evaluate:** Press Enter (or click "Symbolic").
4.  **Result:** The console will display the symbolic result of the integration (e.g., `2*x*Sin[x] - (x^2-2)*Cos[x]`, though the exact form might vary).

**2. Numerical (Definite) Integration:**

1.  **Switch Tab:** Go to the "eSuite Mathematics" tab.
2.  **Enter Command:** Use the `NIntegrate` command, specifying the function, the variable with its lower and upper bounds.
    *Example:* To calculate the definite integral of `x^2 * Sin[x]` from `x=0` to `x=Pi`:
    ```
    NIntegrate[x^2 * Sin[x], {x, 0, Pi}]
    ```
    *(Syntax Note: Use capital `NIntegrate`, variable and bounds are enclosed in curly braces `{x, lower, upper}`).*
3.  **Evaluate:** Press Ctrl+Enter (or click "Numerical"). Using Enter (Symbolic) might also work if the engine can evaluate it symbolically first and then substitute, but `NIntegrate` is specifically for numerical results.
4.  **Result:** The console will display the numerical result of the definite integral (approximately `5.8696`).

## Key Differences from EES (in this context)

*   **ODE Solving:** EES has a dedicated numerical ODE solver integrated with its equation set. Engineering Suite **lacks** a comparable built-in feature accessible from the Solver or a dedicated GUI; ODEs would need to be solved externally or potentially via advanced symbolic commands in the Mathematics tab if supported by the backend engine (often complex).
*   **Symbolic Power:** Engineering Suite's "Mathematics" tab provides direct access to a more powerful symbolic engine (Matheclipse/symja) capable of complex indefinite integration, simplification, differentiation, etc., which typically exceeds EES's native symbolic abilities.
*   **Integration:** EES focuses on numerical definite integration and ODE solving via its `Integral` function/table. Engineering Suite offers both symbolic (`Integrate`) and numerical (`NIntegrate`) integration commands within its symbolic console.
*   **Workflow:** Integrating results from the symbolic engine (like a complex integral result) back into the main eSuite Solver requires manual copy/paste and potential syntax adjustment, whereas EES integration is seamless within its environment.

In summary, while EES excels at numerically solving coupled DAEs (Differential Algebraic Equations) including integrals representing ODEs, Engineering Suite provides stronger *symbolic* integration capabilities via its Mathematics tab and can compute definite integrals numerically there as well.