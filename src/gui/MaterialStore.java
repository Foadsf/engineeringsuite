package gui;

// NO import for LinkedList here
// NO getMaterialLists method here

/**
 * Class that store two string. Used to store a formula in a String and a note
 * about that formula; For example the units of the formula;
 *
 * @author Pablo Salinas
 *
 */
// Ensure class is public
public class MaterialStore {
	private String _Property;
	private String _Formula;
	private String _Note;
	private String _Variables;

	// Constructor remains protected or package-private if only created within gui package
	 MaterialStore(String Property, String formula, String Variables, String note) {
		this._Formula = formula;
		this._Note = note;
		this._Variables = Variables;
		this._Property = Property;
	}

	// Constructor remains protected or package-private
	 MaterialStore(String Property, String formula) {
		this._Formula = formula;
		this._Note = "";
		this._Variables = "";
		this._Property = Property; // Fixed typo _Variables = Property
	}

    // --- Make Getters PUBLIC ---
	public String getFormula() {
		return this._Formula;
	}

	public String getNote() {
		return this._Note;
	}

	public String getVariables() {
		return this._Variables;
	}

	public String getProperty() {
		return this._Property;
	}
    // --- End Public Getters ---

	// Setters can remain protected/package-private if only used within gui package
	protected void setFormula(String formula) {
		this._Formula = formula;
	}

	protected void setNote(String Note) {
		this._Note = Note;
	}

	protected void setVariables(String Variables) {
		this._Variables = Variables;
	}

	protected void setProperty(String Property) {
		this._Property = Property;
	}
}