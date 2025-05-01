package gui;

/**
 * Class that store two string. Used to store a formula in a String and a note about that formula;
 * For example the units of the formula;
 *
 * @author Pablo Salinas
 *
 */
public class MaterialStore {
	private String _Property;
	private String _Formula;
	private String _Note;
	private String _Variables;

	// Constructor can remain default/package-private unless needed elsewhere
	public MaterialStore(String Property, String formula, String Variables, String note) {
		this._Formula = formula;
		this._Note = note;
		this._Variables = Variables;
		this._Property = Property;
	}

	// Constructor can remain default/package-private unless needed elsewhere
	public MaterialStore(String Property, String formula) {
		this._Formula = formula;
		this._Note = "";
		this._Variables = "";
		this._Property = Property; // Corrected: Assign Property here too
	}

	// ---- CHANGE TO PUBLIC ----
	public String getFormula() {
		return this._Formula;
	}

	// ---- CHANGE TO PUBLIC ---- (Added for completeness, might be useful)
	public String getNote() {
		return this._Note;
	}

	// ---- CHANGE TO PUBLIC ----
	public String getVariables() {
		return this._Variables;
	}

	// ---- CHANGE TO PUBLIC ----
	public String getProperty() {
		return this._Property;
	}

	// Setters can remain protected/default unless needed externally
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
