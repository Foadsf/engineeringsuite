package gui;

import java.util.LinkedList;

public class MaterialList implements Comparable<MaterialList> {
	private String _Material;
	private LinkedList<MaterialStore> _Lista;

	// Keep constructors protected or change to public if needed elsewhere
	protected MaterialList() {
		this._Material = new String();
		this._Lista = new LinkedList<MaterialStore>();
	}

	protected MaterialList(String name, LinkedList<MaterialStore> List) {
		this._Material = name;
		this._Lista = List;
	}

	// Keep setter protected unless needed externally
	protected void setMaterial(String name) {
		this._Material = name;
	}

	// ---- CHANGE TO PUBLIC ----
	public String getMaterial() {
		return this._Material;
	}

	// ---- CHANGE TO PUBLIC ----
	/**
	 *
	 * @return A MaterialStore List of the material
	 */
	public LinkedList<MaterialStore> getPropertyList() {
		return this._Lista;
	}

	/**
	 *
	 * @return A linkedList with the names of all the properties
	 */
	// ---- CHANGE TO PUBLIC ---- (Also added null check for safety)
	public LinkedList<String> getProperties() {
		LinkedList<String> Lista = new LinkedList<String>();
		if (this._Lista != null) { // Add null check
			for (MaterialStore m : this._Lista)
				Lista.add(m.getProperty());
		}
		return Lista;
	}

	// Keep adder protected unless needed externally
	protected void addProperty(MaterialStore Store) {
		if (this._Lista == null) { // Initialize if null
			this._Lista = new LinkedList<>();
		}
		this._Lista.add(Store);
	}

	@Override
	public int compareTo(MaterialList input) {
		// Add null checks for robustness
		if (input == null || input._Material == null)
			return 1; // Treat null as greater
		if (this._Material == null)
			return -1; // Treat this null as smaller
		// Original comparison
		if (this._Material.isEmpty() && input._Material.isEmpty())
			return 0;
		if (this._Material.isEmpty())
			return -1;
		if (input._Material.isEmpty())
			return 1;
		return this._Material.charAt(0) - input._Material.charAt(0);
	}

}
