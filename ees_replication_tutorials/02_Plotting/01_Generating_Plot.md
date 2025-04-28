# EES Replication: Generating a Plot

*This tutorial corresponds to the EES "Generating a Plot".*

## Objective

Plot a simple mathematical function `y = f(x)` using Engineering Suite.

## EES Approach

EES typically generates plots from data in Parametric Tables or Lookup Tables, or directly using its Plot commands after solving equations. It offers various plot types (X-Y, Bar, Polar, 3D).

## Engineering Suite Approach

Engineering Suite provides basic 2D function plotting (`y=f(x)`) via the "eSuite Mathematics" tab, which uses a symbolic backend.

1.  **Switch Tab:** Open Engineering Suite and click the "eSuite Mathematics" tab.

2.  **Open Plot Window:** Click the "2D Plot" button located below the console input area. A separate "Plot" window will open.

3.  **Enter Function:** In the "Plot" window, locate the field labeled `y(x) =`. Enter the function you want to plot, using `x` as the independent variable.
    *Example:* To plot `y = x^2 * Sin[x]`, enter:
    ```
    x^2 * Sin[x]
    ```
    *Note on Syntax:*
    *   Use standard operators: `+`, `-`, `*`, `/`, `^` (power).
    *   Use built-in functions with **capital letters** and **square brackets**: `Sin[x]`, `Cos[x]`, `Exp[x]`, `Log[x]` (natural log), `Sqrt[x]`.
    *   Use constants `Pi` and `E`.

4.  **Set Plot Ranges (Important!):** Adjust the **X min**, **X max**, **Y min**, and **Y max** values to define the viewing window for your plot. For `x^2 * Sin[x]`:
    *   Set X min: `-10`
    *   Set X max: `10`
    *   Set Y min: `-60` (Estimate based on x^2 growth)
    *   Set Y max: `60`

5.  **Update Plot:** Click the "Update" button in the "Plot" window. The graph of the function should appear in the plot area.

6.  **Adding More Functions:** Click the "Add Function" button in the "Plot" window to get another `y(x) =` field and plot multiple functions on the same axes. Click "Update" after adding/modifying functions.

## Key Differences from EES (in this context)

*   **Plotting Source:** eSuite plots functions directly entered into the plot window using its symbolic engine syntax. EES plotting is often tightly integrated with solved variables from Parametric/Lookup tables.
*   **Plot Types:** eSuite currently offers basic 2D function plotting. EES offers a wider range including parametric plots, 3D plots, etc.
*   **Syntax:** Function syntax differs (e.g., `Sin[x]` in eSuite vs. `sin(x)` in EES).
