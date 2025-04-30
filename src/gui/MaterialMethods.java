package gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList; // Ensure this is imported

/**
 * Methods to work with the substances database
 *
 * @author Pablo Salinas
 */
// Ensure class is public
public class MaterialMethods {
	/**
	 * Name of the file
	 */
	public static final String ThermodynamicalFile = Config.AbsolutePath
			+ "ThermodynamicalProperties.txt";

	private final Character cube = (char) 127;

	// Keep list protected or private
	protected LinkedList<MaterialList> Materials;

	// Make constructor public if called from outside package, otherwise keep default/protected
	public MaterialMethods() {
		Materials = new LinkedList<MaterialList>();
		getTree();
		// Sorting might cause issues if compareTo isn't robust, handle nulls
        try {
		    Collections.sort(this.Materials);
        } catch (NullPointerException e) {
             System.err.println("Warning: Null encountered during material sorting.");
        }
	}

    // --- ADDED / ENSURED PUBLIC GETTER ---
     /**
      * Provides access to the list of materials and their properties.
      * @return The list of MaterialList objects.
      */
     public LinkedList<MaterialList> getMaterialLists() {
         // Ensure list is initialized
         if (this.Materials == null) {
              this.Materials = new LinkedList<MaterialList>();
              // Optionally load data if needed, or return empty list
              // getTree(); // Be careful calling this here if it relies on other state
         }
         return this.Materials;
     }
     // --- END PUBLIC GETTER ---

	/**
	 * @deprecated Use saveTreeAndMaterial and saveTree instead.
	 * @param Material
	 * @param Property
	 * @param formula
	 * @param variable
	 * @param Note
	 */
	@Deprecated
	protected void SaveMaterial(String Material, String Property,
			String formula, String variable, String Note) {
		// ... (implementation as before) ...
		try {
			String text = this.getString();
			text += "Material: " + Material + ";" + Config.JumpLine;
			text += "Property: " + Property + ";" + Config.JumpLine;
			text += "Formula: " + formula + ";" + Config.JumpLine;
			text += "Variable: " + variable + ";" + Config.JumpLine;
			text += "Note:" + Note + Config.JumpLine + cube;
			FileWriter w;
			w = new FileWriter(ThermodynamicalFile);
			BufferedWriter o = new BufferedWriter(w);
			PrintWriter p = new PrintWriter(o);
			p.append(text);
			text = "";
			p.close();
			o.close();
			w.close();
		} catch (IOException e) {
			SolverGUI.PopUpError(Translation.Language.get(350));
		}
	}

	/**
	 * Adds the input information to the Material List in memory.
	 * Call saveTree() separately to persist changes to the file.
	 *
	 * @param Material
	 * @param Property
	 * @param formula
	 * @param variable
	 * @param Note
	 */
    // Make public if called from outside package, e.g., MaterialGUI
	public void saveTreeAndMaterial(String Material, String Property,
			String formula, String variable, String Note) {
		this.AddMaterial(Material, new MaterialStore(Property, formula,
				variable, Note));
        // Removed saveTree() call - should be called explicitly after adding is done.
	}

	/**
	 * Saves the current Material List to the file.
	 */
    // Make public if called from outside package
	public void saveTree() {
		// ... (implementation as before) ...
        try {
            // Ensure list is initialized
            if (this.Materials == null) {
                 this.Materials = new LinkedList<>();
            }
			// Order the list before saving
            try {
			    Collections.sort(this.Materials);
            } catch (NullPointerException e) {
                 System.err.println("Warning: Null encountered during material sorting before save.");
            }
			// save the List in the file
			FileWriter w;
			w = new FileWriter(ThermodynamicalFile);
			BufferedWriter o = new BufferedWriter(w);
			PrintWriter p = new PrintWriter(o);
			// Use the public getter now within the loop
			for (MaterialList m : this.getMaterialLists()) { // Use getter
                 // Add null check for safety
                 if (m == null || m.getPropertyList() == null) continue;
				 for (MaterialStore MS : m.getPropertyList()) { // Use getter
                      if (MS == null) continue; // Add null check
                      // Use public getters
					  p.println("Material: " + m.getMaterial() + ";");
					  p.println("Property: " + MS.getProperty() + ";");
					  p.println("Formula: " + MS.getFormula() + ";");
					  p.println("Variable: " + MS.getVariables() + ";");
					  p.println("Note:" + MS.getNote());
					  p.println(cube);
				 }
            }

			p.close();
			o.close();
			w.close();
		} catch (IOException e) {
            System.err.println("Error saving thermodynamic properties file: " + e.getMessage());
			// Avoid GUI call in non-GUI context if possible
            // SolverGUI.PopUpError(Translation.Language.get(350));
		}
	}

	/**
	 * Returns the content of the file ThermodynamicalFile as a String
	 *
	 * @return File content or empty string on error.
	 */
	private String getString() {
		// ... (implementation as before) ...
        StringBuilder text = new StringBuilder();
		try (FileReader r = new FileReader(ThermodynamicalFile);
             BufferedReader b = new BufferedReader(r))
        {
			String s;
			while ((s = b.readLine()) != null) {
				text.append(s).append(Config.JumpLine);
            }
		} catch (IOException e) {
            System.err.println("Error reading thermodynamic properties file: " + e.getMessage());
			// Avoid GUI call
            // SolverGUI.PopUpError(Translation.Language.get(350));
			return "";
		}
        return text.toString();
	}

	/**
	 * Reads the file and populates the internal Materials list.
	 */
    // Keep protected/private
	protected void getTree() {
		// ... (implementation mostly as before, ensure AddMaterial is called) ...
        try {
			String fileContent = this.getString();
            if (fileContent.isEmpty()) return; // Don't process if file read failed

			String[] tree = fileContent.split(java.util.regex.Pattern.quote(Character.toString(cube)));
			fileContent = null; // Release memory

			String Material = ""; String Property = ""; String Formula = "";
            String Variables = ""; String Nota = "";
			MaterialStore Ms;
			int start; int end; int count;

			for (String s : tree) {
                 if (s == null || s.trim().length() < 40) continue; // Skip potentially empty splits

                 String currentSegment = s; // Work on a copy
                 count = 0;

				 // Use indexOf with starting position for safety
                 int matPos = currentSegment.indexOf("Material: ");
                 int propPos = currentSegment.indexOf("Property: ");
                 int formPos = currentSegment.indexOf("Formula: ");
                 int varPos = currentSegment.indexOf("Variable: ");
                 int notePos = currentSegment.indexOf("Note:");

                 // Basic validation that all parts seem present
                 if (matPos == -1 || propPos == -1 || formPos == -1 || varPos == -1 || notePos == -1) {
                      System.err.println("Warning: Skipping malformed segment in properties file:\n" + s);
                      continue;
                 }

                 try {
                     Material = currentSegment.substring(matPos + 10, currentSegment.indexOf(";", matPos)).trim();
                     Property = currentSegment.substring(propPos + 10, currentSegment.indexOf(";", propPos)).trim();
                     Formula = currentSegment.substring(formPos + 9, currentSegment.indexOf(";", formPos)).trim();
                     Variables = currentSegment.substring(varPos + 10, currentSegment.indexOf(";", varPos)).trim();
                     // Note ends at the end of the segment (before the cube split char)
                     Nota = currentSegment.substring(notePos + 5).trim();

                    // Ensure no empty strings which might indicate parsing error
                    if (Material.isEmpty() || Property.isEmpty() || Formula.isEmpty() || Variables.isEmpty()) {
                         System.err.println("Warning: Parsed empty field in segment:\n" + s);
                         continue;
                    }

                     Ms = new MaterialStore(Property, Formula, Variables, Nota);
                     this.AddMaterial(Material, Ms);

                 } catch (IndexOutOfBoundsException | NullPointerException parseEx) {
                     System.err.println("Warning: Error parsing segment in properties file:\n" + s + "\nError: " + parseEx.getMessage());
                 }
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
            System.err.println("Error processing thermodynamic properties tree.");
			// Avoid GUI call
            // SolverGUI.PopUpError(Translation.Language.get(351));
		}
	}

	/**
	 * If the Material is already in the list then the property will be added.
	 * If not the new material with the property will be added to the list
	 * (Internal helper method).
	 * @param Material
	 * @param store
	 */
    // Keep private/protected
	private void AddMaterial(String Material, MaterialStore store) {
		// ... (implementation as before, uses public methods of MaterialList/Store now) ...
        boolean found = false;
        // Ensure list is initialized
         if (this.Materials == null) {
              this.Materials = new LinkedList<MaterialList>();
         }
		 for (MaterialList m : this.Materials) {
              // Add null check and use public getter
              if (m != null && m.getMaterial() != null && m.getMaterial().equalsIgnoreCase(Material)) {
                   m.addProperty(store); // addProperty should handle null _Lista internally
                   found = true;
                   break;
              }
         }
		 if (!found) {
              LinkedList<MaterialStore> aux = new LinkedList<MaterialStore>();
              aux.add(store);
              this.Materials.add(new MaterialList(Material, aux));
         }
	}

	/**
	 * @return A list with the substance names.
	 */
    // Keep public
	public LinkedList<String> getMaterials() {
		LinkedList<String> aux = new LinkedList<String>();
         // Use the getter which ensures initialization
		 for (MaterialList m : this.getMaterialLists()) {
              if (m != null) { // Add null check
                 aux.add(m.getMaterial());
              }
         }
		 return aux;
	}

	/**
     * Gets the list of property names for a given substance.
	 * @return A list with the property names, or null if substance not found.
	 */
    // Keep public
	public LinkedList<String> getProperties(String material) {
         // Use the getter which ensures initialization
		 for (MaterialList m : this.getMaterialLists()) {
              if (m != null && m.getMaterial() != null && m.getMaterial().equalsIgnoreCase(material))
                  return m.getProperties(); // Uses public getter
         }
		 return null;
	}

	/**
	 * Gets the list of MaterialStore objects for a given substance.
	 * @param material The substance name.
	 * @return A List with all the properties of the material, or null if not found.
	 */
    // Keep public
	public LinkedList<MaterialStore> getMaterial(String material) {
		 // Use the getter which ensures initialization
		 for (MaterialList m : this.getMaterialLists()) {
              if (m != null && m.getMaterial() != null && m.getMaterial().equalsIgnoreCase(material))
                  return m.getPropertyList(); // Uses public getter
         }
		 return null;
	}

	/**
	 * Erase the content of the Materials List in memory.
	 */
    // Keep public if needed externally
	public void clear() {
        if (this.Materials != null) {
		    this.Materials.clear();
        }
	}

} // End MaterialMethods class