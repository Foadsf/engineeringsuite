# Engineering Suite - Plotting Examples

These examples demonstrate how to use the 2D plotting functionality available in the "eSuite Mathematics" tab. This feature allows you to visualize functions of the form `y = f(x)`.

**(See `Imagenes/Mathematics.png` for a visual reference of the Mathematics tab and Plot window)**

## Instructions:

1.  **Switch Tab:** Open Engineering Suite and click on the "eSuite Mathematics" tab.
2.  **Open Plot Window:** Click the "2D Plot" button located below the console area. A new "Plot" window will appear.
3.  **Enter Function(s):** In the "Plot" window, you'll see fields labeled `y(x) =`. Enter the mathematical expression for the function you want to plot in terms of `x`. You can add more functions by clicking the "Add Function" button.
    *   Use standard mathematical notation (e.g., `+`, `-`, `*`, `/`, `^` for power).
    *   Use functions like `Sin[x]`, `Cos[x]`, `Exp[x]`, `Log[x]`, `Sqrt[x]`, etc. (Note: The symbolic engine likely requires functions to start with a capital letter and use square brackets `[]` for arguments, similar to Mathematica/Matheclipse).
    *   Use `Pi` and `E` for the constants.
4.  **Set Ranges (Optional):** Adjust the "X min", "X max", "Y min", and "Y max" values to define the viewable area of the plot. The default is often -10 to 10 for both axes.
5.  **Plot:** Click the "Update" button in the "Plot" window. The graph(s) should appear in the plot area.

---

## Example Functions to Plot:

### Example 7.1: Simple Polynomials

*   Function 1: `x^2` (A simple parabola)
*   Function 2: `x^3 - 2*x + 1` (A cubic polynomial)
    *   *Instructions:* Enter `x^2` in the first `y(x) =` field. Click "Add Function". Enter `x^3 - 2*x + 1` in the second field. Click "Update". Adjust ranges if needed to see the interesting parts.

### Example 7.2: Trigonometric Functions

*   Function 1: `Sin[x]`
*   Function 2: `Cos[2*x]`
    *   *Instructions:* Enter `Sin[x]` and `Cos[2*x]` in separate fields. You might want to set X min to `-2*Pi` and X max to `2*Pi` to see a couple of cycles. Set Y min to `-1.5` and Y max to `1.5`. Click "Update".

### Example 7.3: Exponential and Logarithmic Functions

*   Function 1: `Exp[x]`
*   Function 2: `Log[x]` (Natural Logarithm)
    *   *Instructions:* Enter `Exp[x]` and `Log[x]` in separate fields. Note that `Log[x]` is only defined for `x > 0`. You might set X min to `0.1` (to avoid `Log[0]`) and X max to `5`. Adjust Y min/max accordingly (e.g., -5 to 50 to see the exponential growth). Click "Update".

### Example 7.4: Function with Asymptote

*   Function 1: `1/x`
    *   *Instructions:* Enter `1/x`. Set X min/max (e.g., -5 to 5) and Y min/max (e.g., -5 to 5). Click "Update". Observe the behavior near `x=0`.

### Example 7.5: Combined Functions

*   Function 1: `Sin[x] / x`
    *   *Instructions:* Enter `Sin[x]/x`. Plot from, for example, X min `-4*Pi` to X max `4*Pi`. Adjust Y range (e.g., -1 to 1.5). Click "Update".

---

**Note on Other Visualizations:**

*   The images in the `Imagenes` directory related to **Tarjan's algorithm** (`1Sinordenar.jpg` to `5Sistema2.jpg`) illustrate the *internal logic* the solver uses to break down equation systems. The eSuite GUI does not directly visualize these matrix steps.
*   The images related to **trust-region optimization** (`function.jpg`, `Image1.jpg` to `Image7.jpg`) visualize how numerical optimization algorithms find a minimum. While eSuite *uses* these algorithms (Dogleg, More-Hebdon), the GUI does not provide this specific type of contour plot visualization of the iteration steps. You can observe the *numerical* progress in the "Log" tab when solving equations using these methods.