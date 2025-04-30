package gui;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import evaluation.DiffAndEvaluator;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import edu.jas.kern.ComputerThreads;

import String2ME.CheckString; // Needed for cleanComments logic if adapted
import String2ME.InitVal;

/**
 * Handles saving and loading of .ris files.
 * @author Pablo Salinas (Original), Modified
 */
// Ensure class is public
public class SaveLoad {

	/**
	 * The icon for the windows
	 */
	public static final Image Icon = (Toolkit.getDefaultToolkit()
			.getImage(Config.AbsolutePath + "icons/logo.png"));

	/**
	 * Open dialog for the GUI. Loads equations and initial values.
	 *
	 * @param The
	 *            TextArea(RSyntaxTextArea) you want to write the archive
	 */
	public void Open(RSyntaxTextArea TextArea) {
		JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"eSuite document RIS (.ris)", "ris");
		fc.addChoosableFileFilter(filter);
		fc.setFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(false);
		try {
			int respuesta = fc.showOpenDialog(Principal.frame); // Use frame as parent
			if (respuesta == JFileChooser.APPROVE_OPTION) {

				File file = fc.getSelectedFile();

				String archivo = file.getAbsolutePath();
				if (archivo != null && archivo.length() > 0) {
                    // First i clean everything
                    CleanAll(); // Use the static helper method
                    Principal.SaveFile = archivo; // Update the global save file path

                    // --- Load File Content ---
                    StringBuilder fileContent = new StringBuilder();
                    try (FileReader r = new FileReader(archivo);
                         BufferedReader b = new BufferedReader(r)) {
                          String line;
                          while ((line = b.readLine()) != null) {
                               fileContent.append(line).append(System.lineSeparator());
                          }
                    } // try-with-resources ensures files are closed

                    // --- Extract and Set Equations ---
                    String fullContent = fileContent.toString();
                    int markerPos = fullContent.indexOf("@$@%@EndOfEquationData@$@%@");
                    String equationText;
                     if (markerPos != -1) {
                         equationText = fullContent.substring(0, markerPos);
                     } else {
                          System.err.println("WARNING: EndOfEquationData marker not found in " + archivo + ". Treating whole file as equations.");
                          equationText = fullContent; // Load everything if marker missing
                     }
                    // Remove trailing newline added by StringBuilder if present
                    if(equationText.endsWith(System.lineSeparator())){
                        equationText = equationText.substring(0, equationText.length() - System.lineSeparator().length());
                    }
                    TextArea.setText(equationText);
                    TextArea.setCaretPosition(0); // Move cursor to start
                    TextArea.discardAllEdits(); // Clear undo history

                    // --- Load Initial Values using the robust method ---
                    if (!loadInitialValuesFromRis(archivo)) {
                         System.err.println("Warning: Potential issues encountered while loading initial values section from " + archivo);
                         // Optionally show a GUI warning?
                         // SolverGUI.PopUpWarning("Could not fully load initial values. Check file format.");
                    }
                }
			} else {/* User cancelled */
			     System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err); // Print stack trace to console
			System.err.println("Open File Error, or no file selected");
            SolverGUI.PopUpError("Error opening file: " + e.getMessage()); // Show error to user
		} finally {
             // Ensure cursor is reset even on error (though setText usually handles it)
			 Principal.TextArea.TextArea.setCaretPosition(0);
             // Discard edits is good practice after loading
             Principal.TextArea.TextArea.discardAllEdits();
		}
	} // End Open method

	/**
	 * Loads only the initial values from a .ris file.
	 * Reports errors to System.err. Used by CLI.
	 * @param filePath Path to the .ris file
	 * @return true if loading completed without parsing errors, false otherwise.
	 */
	public boolean loadInitialValuesFromRis(String filePath) {
	    boolean success = true;
	    Config.InitValue.clear(); // Clear previous values

	    try (FileReader r = new FileReader(filePath);
	         BufferedReader b = new BufferedReader(r)) {

	        String s;
	        boolean foundDataSection = false;
	        boolean foundValueSection = false;

	        // Find the end of the equation data
	        while ((s = b.readLine()) != null) {
	            if (s.trim().equals("@$@%@EndOfEquationData@$@%@")) {
	                foundDataSection = true;
	                break;
	            }
	        }

	        if (!foundDataSection) {
	            System.err.println("WARNING: EndOfEquationData marker not found in " + filePath);
	            // Continue assuming the rest might be initial values
	        }

	        // Now read initial values until the next marker
	        while ((s = b.readLine()) != null) {
	            s = s.trim();
	            if (s.equals("@$@%@EndOfInitialVariableValueData@$@%@")) {
	                foundValueSection = true;
	                break; // Found the end marker
	            }
	            if (s.isEmpty() || s.startsWith("/*") || s.startsWith("/**")) {
	                continue; // Skip empty lines/comments starting at beginning
	            }

	            int k = s.indexOf('=');
	            if (k <= 0 || k == s.length() - 1) {
	                System.err.println("WARNING: Skipping invalid initial value line (bad format): " + s);
	                continue;
	            }

	            try {
	                String varName = s.substring(0, k).trim();
	                String varValueStr = s.substring(k + 1).trim();
	                if (varName.isEmpty()) { // Check for empty variable name
                         System.err.println("WARNING: Skipping invalid initial value line (empty variable name): " + s);
                         continue;
                    }

	                // Handle potential inline comments AFTER the value
	                int commentStart = varValueStr.indexOf("/*");
	                if (commentStart != -1) {
	                     varValueStr = varValueStr.substring(0, commentStart).trim();
	                }
	                // Consider single line comment markers too if needed (e.g. // or #)
	                // commentStart = varValueStr.indexOf("//");
	                // if (commentStart != -1) {
	                //     varValueStr = varValueStr.substring(0, commentStart).trim();
	                // }

	                if (varValueStr.isEmpty()) { // Check again after stripping comment
	                     System.err.println("WARNING: Skipping initial value line (value became empty after comment removal): " + s);
	                     continue;
	                }

	                varValueStr = varValueStr.replace(',', '.'); // Replace comma with dot
	                Config.InitValue.add(new InitVal(Double.parseDouble(varValueStr), varName));
	                System.out.println("  Loaded initial value: " + varName + " = " + varValueStr); // Debug output

	            } catch (NumberFormatException e) {
	                 System.err.println("WARNING: Could not parse number from initial value line: \"" + s + "\" - Error: " + e.getMessage());
	                 success = false; // Mark as potential issue, but continue loading others
	            } catch (Exception e) {
	                 System.err.println("WARNING: Error processing initial value line: \"" + s + "\" - Error: " + e.getMessage());
	                 success = false;
	            }
	        } // end while reading values

	        if (!foundValueSection) {
	             System.err.println("WARNING: EndOfInitialVariableValueData marker not found in " + filePath);
	             // This might be okay if there were no initial values intended
	        }

	    } catch (IOException e) {
	        System.err.println("ERROR: Failed to open or read file '" + filePath + "': " + e.getMessage());
	        return false; // Indicate failure
	    }
	    System.out.println("Finished loading initial values. Success status: " + success);
	    return success;
	}


	/**
	 * Save dialog
	 *
	 * @param The
	 *            TextArea(RSyntaxTextArea) you want to save
	 * @param SaveAs Forces the "Save As" dialog even if a file is already associated.
	 */
	protected void Save(RSyntaxTextArea TextArea, boolean SaveAs) {
		JFileChooser fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"eSuite document RIS (.ris)", "ris");
		fc.addChoosableFileFilter(filter);
		fc.setFileFilter(filter);

		String archivo = null;

		try {
			// Determine if we need to show the Save As dialog
			if (SaveAs || Principal.SaveFile == null || Principal.SaveFile.equals("NoFileSelected") || Principal.SaveFile.isEmpty()) {
				// Set current directory for chooser if a file was previously saved/opened
                if (Principal.SaveFile != null && !Principal.SaveFile.equals("NoFileSelected") && !Principal.SaveFile.isEmpty()) {
                    File currentFile = new File(Principal.SaveFile);
                    fc.setCurrentDirectory(currentFile.getParentFile());
                    fc.setSelectedFile(new File(currentFile.getName())); // Suggest current name
                }

				int respuesta = fc.showSaveDialog(Principal.frame); // Use frame as parent
				if (respuesta == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					archivo = file.getAbsolutePath();
					// Ensure .ris extension
					if (!archivo.toLowerCase().endsWith(".ris")) {
						archivo += ".ris";
					}
					Principal.SaveFile = archivo; // Update the current save file path
				} else {
					System.out.println("Save command cancelled by user.");
					return; // Exit method if user cancelled
				}
			} else {
				// Use the existing save file path
				archivo = Principal.SaveFile;
			}

            System.out.println("Saving to file: " + archivo);

			/* Use try-with-resources for automatic closing */
            try (FileWriter w = new FileWriter(archivo);
                 BufferedWriter o = new BufferedWriter(w);
                 PrintWriter p = new PrintWriter(o);
                 StringReader J = new StringReader(TextArea.getText()); // Read directly from TextArea
                 BufferedReader BufJ = new BufferedReader(J))
            {

                /* 1º Save equations method */
                String s;
                while ((s = BufJ.readLine()) != null) {
                    p.println(s);
                }
                p.println("@$@%@EndOfEquationData@$@%@");

                /* 2º Save initial values */
                if (Config.InitValue != null) {
                    for (InitVal iv : Config.InitValue) {
                        // Ensure variable name doesn't contain '='
                        String varName = iv.getVariable().replace("=", "");
                        if (!varName.isEmpty()) {
                           p.println(varName + " = " + iv.getValue());
                        }
                    }
                }
                p.println("@$@%@EndOfInitialVariableValueData@$@%@");

                 System.out.println("File saved successfully.");

            } // Files are closed automatically here

		} catch (NullPointerException NE) {
			System.err.println("Save Error: Null pointer encountered (Possibly related to file chooser).");
            NE.printStackTrace(System.err);
            SolverGUI.PopUpError("Error saving file: Null object encountered.");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Save File Error");
            SolverGUI.PopUpError("Error saving file: " + e.getMessage());
		}
	}

	/**
	 * Erase current document after a Confirm dialog
	 */
	protected void NewFile() {

		int n = JOptionPane.showOptionDialog(Principal.frame, // Parent component
				Translation.Language.get(117), // Message
				Translation.Language.get(41), // Title
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				new ImageIcon(Config.AbsolutePath + "icons/help-browser.png"), // Icon
				null, // Options (use default Yes/No)
				null); // Initial value
		// Yes = 0 ; No = 1

		if (n == JOptionPane.YES_OPTION) {
			CleanAll();
			Principal.SaveFile = "NoFileSelected"; // Reset save file path
             System.out.println("New document created.");
		} else {
             System.out.println("New document cancelled by user.");
        }

	}

	/**
	 * Checks if the document is saved and prompts the user if necessary before exiting.
	 */
	protected static void Salir() {
		Principal.frame.setEnabled(true); // Re-enable frame if disabled elsewhere
		boolean changesMade = false;
		String currentText = Principal.TextArea.TextArea.getText();
		String savedText = "";

		try {
			// Check if there is a file associated and if its content matches current text
			if (Principal.SaveFile != null && !Principal.SaveFile.equals("NoFileSelected") && !Principal.SaveFile.isEmpty()) {
                File file = new File(Principal.SaveFile);
                if (file.exists() && file.isFile()) {
                     StringBuilder content = new StringBuilder();
                     try (FileReader r = new FileReader(file);
                          BufferedReader b = new BufferedReader(r)) {
                           String line;
                           while ((line = b.readLine()) != null) {
                                if (line.trim().equals("@$@%@EndOfEquationData@$@%@")) {
                                    break; // Stop reading at the marker
                                }
                                content.append(line).append(System.lineSeparator());
                           }
                           savedText = content.toString();
                           // Remove trailing newline if present from StringBuilder append
                           if(savedText.endsWith(System.lineSeparator())){
                               savedText = savedText.substring(0, savedText.length() - System.lineSeparator().length());
                           }
                     } catch (IOException ioe) {
                           System.err.println("Error reading saved file for comparison: " + ioe.getMessage());
                           // Assume changes if we can't read the saved file
                           changesMade = true;
                     }
                      // Compare current text (handle potential trailing newline)
                      String currentTextTrimmed = currentText;
                      if(currentTextTrimmed.endsWith(System.lineSeparator())){
                          currentTextTrimmed = currentTextTrimmed.substring(0, currentTextTrimmed.length() - System.lineSeparator().length());
                      }
                     if (!savedText.equals(currentTextTrimmed)) {
                         changesMade = true;
                     }

                } else {
                    // File path exists but file doesn't - treat as unsaved changes if text area not empty
                    if (currentText != null && !currentText.isEmpty()) {
                        changesMade = true;
                    }
                }
			} else {
				// No file associated, check if text area has content
				if (currentText != null && !currentText.isEmpty()) {
					changesMade = true;
				}
			}
		} catch (Exception e) {
			System.err.println("Error checking for unsaved changes: " + e.getMessage());
			e.printStackTrace(System.err);
			changesMade = true; // Be safe, assume changes on error
		}

        // Prompt user based on whether changes were detected
		String exitMessage = Translation.Language.get(118); // "Are you sure you want to quit?"
		String iconPath = Config.AbsolutePath + "icons/help-browser.png";
		int messageType = JOptionPane.QUESTION_MESSAGE;

		if (changesMade) {
			exitMessage = Translation.Language.get(349); // "Document not saved. Quit anyway?"
			iconPath = Config.AbsolutePath + "icons/dialog-warning.png";
            messageType = JOptionPane.WARNING_MESSAGE;
		}

		int n = JOptionPane.showOptionDialog(Principal.frame,
				exitMessage,
				Translation.Language.get(40), // Title "Exit"
				JOptionPane.YES_NO_OPTION,
				messageType, // Use appropriate message type
				new ImageIcon(iconPath),
				null,
				null);

		if (n == JOptionPane.YES_OPTION) {
             System.out.println("Exiting application.");
			 ComputerThreads.terminate(); // Ensure background threads are stopped if applicable
			 System.exit(0); // Clean exit
		} else {
             System.out.println("Exit cancelled by user.");
        }
	}

	/**
	 * Cleans up all text areas and resets relevant state variables.
	 */
	private static void CleanAll() {
        System.out.println("Cleaning workspace...");
		Principal.TextArea.TextArea.setText("");      // Clean the Equation TextArea
		Principal.ResultArea.TextArea.setText("");    // Clean the Result Area
		Principal.LogArea.TextArea.setText("");       // Clean the Log area
		Principal.Rendered.setText("");               // Clean the Rendered area
		Config.InitValue.clear();                     // Clean the initial values list
		CheckString.PurgeAll();                       // Clean internal parser state
		Principal.TextArea.TextArea.discardAllEdits();// Clean undo/redo history

        // Reset flags
        Config.ErrorFound = false;
        DiffAndEvaluator.TimeLimitExceeded = false;
        SolverGUI.ResidualsHigh = false;
        DiffAndEvaluator.StringErrorEvaluating = null;
        DiffAndEvaluator.IrrealEvaluation = false;

        System.gc(); // Suggest garbage collection
	}

} // End SaveLoad Class