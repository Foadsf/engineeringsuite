# Engineering Suite

Version 1.02

## About

Open source and multiplatform equation solver written in Java. This project allows users to solve systems of non-linear equations, particularly engineering-type equations. It includes a database of thermodynamical properties for various substances including steam, air, nitrogen, argon, butane, propane, and more.

This is a mirror of the original project hosted on Google Code: https://code.google.com/archive/p/engineeringsuite/

## Features

- Solves systems of non-linear algebraic equations in an easy way
- Includes trigonometric, hyperbolic, logarithmic, and exponential functions
- Thermodynamic property database for many substances
- Complete GUI with undo/redo, save/load, and export to PDF
- Four globally convergent algorithms for solving equation systems
- Mathematical symbolic program based on Matheclipse
- Available in English and Spanish

## Example

For solving the second Newton law you only have to introduce the following code:

```
F = M * A /*It includes trigonometric, hiperbolic, logarithms and exponential functions*/
M = 2 * 2 + 1 - 1 + sin(Pi) + sinh(2.7)
A = 2^2*(exp(2/3)+log(1))
```

## Prerequisites (Windows)

- **Java Development Kit (JDK):** You need a JDK (version 8 or later recommended, tested with OpenJDK 22) installed and configured on your system PATH. You can install it using package managers:
    - Chocolatey: `choco install openjdk`
    - Winget: `winget install Microsoft.OpenJDK.21` (or `.17`, `.11`, etc.)
- **Command Line:** Windows `cmd` or PowerShell.

Verify your JDK installation by opening `cmd` and running `java -version` and `javac -version`.

## Building (Windows Command Line)

1.  **Navigate to Project Root:** Open `cmd` or PowerShell in the directory where you cloned or extracted the project (e.g., `C:\dev\Java\20250428\engineeringsuite-work\engineeringsuite-git`).
    ```cmd
    cd path\to\engineeringsuite-git
    ```
2.  **Create Output Directory:** Create a directory to store the compiled `.class` files.
    ```cmd
    mkdir bin
    ```
3.  **List Source Files:** Create a temporary file listing all `.java` source files.
    ```cmd
    dir /s /b src\*.java > sources.txt
    ```
4.  **Compile:** Compile the source code using the dependencies provided.
    ```cmd
    javac -d bin -cp "Dependencies\*" @sources.txt
    ```
    *   `-d bin`: Output compiled files to the `bin` directory.
    *   `-cp "Dependencies\*"`: Include all JAR files from the `Dependencies` folder in the classpath.
    *   `@sources.txt`: Read the list of source files from `sources.txt`.

    **Note:** You may see several deprecation warnings during compilation if using a modern JDK (like 17 or 21). This is expected as the code is older. As long as there are no *errors*, the compilation was successful.

## Running (Windows Command Line)

1.  **Navigate to Project Root:** Ensure your command prompt is still in the project's root directory.
2.  **Run the Application:** Execute the main class using the `java` command, including the compiled code, dependencies, and current directory in the classpath.
    ```cmd
    java -cp ".;bin;Dependencies\*" gui.Principal
    ```
    *   `-cp ".;bin;Dependencies\*"`: Sets the runtime classpath.
        *   `.`: Current directory (for resources like `config.txt`, `icons`, `Imagenes`).
        *   `bin`: Your compiled code.
        *   `Dependencies\*`: The library JARs.
    *   `gui.Principal`: The fully qualified name of the main class.

3.  **Language:** The application might start in Spanish by default. To change it to English:
    *   **Option 1 (Edit File):** Close the application. Open `config.txt` in the project root, change the line `Language: Español` to `Language: English`, save the file, and rerun the `java` command above.
    *   **Option 2 (GUI):** Use the menus (likely Edit -> Preferences or similar) within the application to change the language setting, apply, close, and restart.

## Troubleshooting

- **`javac` or `java` not found:** Ensure the JDK `bin` directory is in your system's PATH environment variable.
- **Compilation Errors (`Cannot find symbol`, `package does not exist`):** Usually a classpath (`-cp`) issue. Verify the `Dependencies` folder path and its contents. If errors persist with a modern JDK, try compiling with an older version like JDK 11 or 8.
- **Runtime Errors (`Could not find or load main class`, `NoClassDefFoundError`):** Usually a runtime classpath (`-cp`) issue. Double-check the `java` command, ensure `bin` and `Dependencies\*` are correct, and that you are running from the project root directory.
- **Resource Errors (Cannot find `config.txt`, icons, etc.):** Ensure `.` is part of the runtime classpath and you are running from the project root directory.

## License

This project is licensed under the GNU Lesser GPL (see `lgpl.txt`).

## Acknowledgments

- This project was originally developed by **naguillo@gmail.com**.
- It utilizes several third-party libraries included in the `Dependencies` folder, such as Matheclipse (symja), iText, Apache Commons, jEuclid, RSyntaxTextArea, SwingX, and others necessary for its functionality.
