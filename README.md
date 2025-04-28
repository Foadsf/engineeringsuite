# Engineering Suite

Version 1.02

## About

Open source and multiplatform equation solver written in Java. This project allows users to solve systems of non-linear equations, particularly engineering-type equations. It includes a database of thermodynamical properties for various substances including steam, air, nitrogen, argon, butane, propane, and more.

This is a mirror of the original project hosted on Google Code: https://code.google.com/archive/p/engineeringsuite/

**Important Compatibility Note:** Due to old dependencies (jEuclid, Batik) used for rendering mathematical equations, this application **requires a Java 8 Development Kit (JDK) or Java 8 Runtime Environment (JRE)** to run fully without errors, especially in the "eSuite Mathematics" tab. It will likely fail or experience rendering errors on Java 9 or newer.

## Features

- Solves systems of non-linear algebraic equations in an easy way
- Includes trigonometric, hyperbolic, logarithmic, and exponential functions
- Thermodynamic property database for many substances (User-extendable)
- Complete GUI with undo/redo, save/load, and export to PDF
- Four globally convergent algorithms for solving equation systems (Line Search, Dogleg, More-Hebdon, Levenberg-Marquardt)
- Equation system decomposition using Tarjan's algorithm
- Mathematical symbolic program based on Matheclipse (requires Java 8 for MathML rendering)
- Available in English and Spanish

## Prerequisites (Windows)

- **Java 8 Development Kit (JDK 8):** You **must** have a JDK 8 (e.g., Adoptium Temurin 8, Oracle JDK 8, Amazon Corretto 8) installed. Newer JDK versions will cause errors in the Mathematics tab rendering. Ensure the JDK 8 `bin` directory is in your system PATH or you use the full path to `java.exe`/`javac.exe`.
    - **Installation via Chocolatey:** `choco install adoptopenjdk8 -y`
- **Command Line:** Windows `cmd` or PowerShell.

Verify your JDK 8 installation by opening `cmd` and running `java -version` (it should report version 1.8.x) and `javac -version` (should report `javac 1.8.x`). If you have multiple JDKs, ensure JDK 8 is the active one in your PATH or use full paths in the commands below.

## Building (Windows Command Line with JDK 8)

1.  **Navigate to Project Root:** Open `cmd` or PowerShell in the project's root directory.
    ```cmd
    cd path\to\engineeringsuite-git
    ```
2.  **Create Output Directory:**
    ```cmd
    mkdir bin
    ```
3.  **List Source Files:**
    ```cmd
    dir /s /b src\*.java > sources.txt
    ```
4.  **Compile:** Use the JDK 8 `javac` and specify UTF-8 encoding.
    ```cmd
    rem Ensure javac from JDK 8 is used (check with javac -version)
    rem Or use the full path, e.g.:
    rem "C:\Program Files\Eclipse Adoptium\jdk-8.0.xxx.yy-hotspot\bin\javac.exe" -encoding UTF-8 -d bin -cp "Dependencies\*" @sources.txt

    javac -encoding UTF-8 -d bin -cp "Dependencies\*" @sources.txt
    ```
    *   `-encoding UTF-8`: Handles potential special characters in source file comments.
    *   `-d bin`: Output compiled files to the `bin` directory.
    *   `-cp "Dependencies\*"`: Include library JARs.
    *   `@sources.txt`: Read source files list.

    **Note:** You may see warnings about "unchecked or unsafe operations". These are expected with older code and can be ignored if compilation succeeds without errors.

## Running (Windows Command Line with JDK 8)

1.  **Navigate to Project Root:** Ensure your command prompt is in the project's root directory.
2.  **Run the Application:** Execute using the JDK 8 `java` command.
    ```cmd
    rem Ensure java from JDK 8 is used (check with java -version)
    rem Or use the full path, e.g.:
    rem "C:\Program Files\Eclipse Adoptium\jdk-8.0.xxx.yy-hotspot\bin\java.exe" -cp ".;bin;Dependencies\*" gui.Principal

    java -cp ".;bin;Dependencies\*" gui.Principal
    ```
    *   `-cp ".;bin;Dependencies\*"`: Sets the runtime classpath.
        *   `.`: Current directory (for resources like `config.txt`, `icons`, `examples`).
        *   `bin`: Your compiled code.
        *   `Dependencies\*`: The library JARs.
    *   `gui.Principal`: The main class.

3.  **Language:** The application might start in Spanish. Edit `config.txt` and change `Language: Español` to `Language: English`, then restart.

## Troubleshooting

- **`javac` or `java` not found / Wrong Version:** Ensure the JDK 8 `bin` directory is correctly configured in your system's PATH environment variable, or use the full explicit path to the executables.
- **`error: unmappable character ... for encoding windows-1252`:** Add `-encoding UTF-8` to your `javac` command.
- **`NoClassDefFoundError: org/w3c/dom/events/CustomEvent` (or similar Batik/jEuclid error):** You are likely **not** running with Java 8. Switch to a JDK 8 / JRE 8 environment.
- **Resource Errors (Cannot find `config.txt`, icons, etc.):** Ensure `.` is part of the runtime classpath (`-cp ".;...`) and you are running the `java` command from the project's root directory.
- **Solver Errors:** Check initial values, ensure equation/variable counts match (or ignore the initial warning if inputs are defined later), and consider trying different solver methods via Edit -> Preferences.

## Examples

Examples can be found in the `/examples` directory. See `examples/README.md` for details.

## License

This project is licensed under the GNU Lesser GPL (see `lgpl.txt`).

## Acknowledgments

- This project was originally developed by **naguillo@gmail.com**.
- It utilizes several third-party libraries included in the `Dependencies` folder, such as Matheclipse (symja), iText, Apache Commons, jEuclid, RSyntaxTextArea, SwingX, and others.