package gui;

import java.util.LinkedList; // Ensure this is imported

// Ensure class is public
public class MaterialList implements Comparable<MaterialList> {
	private String _Material;
	private LinkedList<MaterialStore> _Lista; // Keep private or protected

	// Keep constructors protected/package-private
	protected MaterialList() {
		this._Material = new String();
		this._Lista = new LinkedList<MaterialStore>();
	}

	protected MaterialList(String name, LinkedList<MaterialStore> List) {
		this._Material = name;
		this._Lista = List;
	}

    // Keep setter protected/package-private
	protected void setMaterial(String name) {
		this._Material = name;
	}

    // --- Make Getters PUBLIC ---
	public String getMaterial() {
		return this._Material;
	}

	/**
	 *
	 * @return A MaterialStore List of the material
	 */
	public LinkedList<MaterialStore> getPropertyList() {
	    // Initialize if null
	    if (this._Lista == null) {
	         this._Lista = new LinkedList<MaterialStore>();
	    }
		return this._Lista;
	}

	/**
	 *
	 * @return A linkedList with the names of all the properties
	 */
	public LinkedList<String> getProperties() {
		LinkedList<String> propertiesList = new LinkedList<String>();
		if (this._Lista != null) { // Check for null before iterating
		    for (MaterialStore m : this._Lista) {
		        propertiesList.add(m.getProperty()); // Uses public getter now
		    }
		}
		return propertiesList;
	}
    // --- End Public Getters ---

    // Keep adder protected/package-private
	protected void addProperty(MaterialStore Store) {
	    if (this._Lista == null) {
	         this._Lista = new LinkedList<MaterialStore>();
	    }
		this._Lista.add(Store);
	}

	// Keep compareTo public as it implements interface method
	@Override
    public int compareTo(MaterialList input) {
        if (this._Material == null && input._Material == null) return 0;
        if (this._Material == null) return -1; // nulls first
        if (input._Material == null) return 1;
		// Use compareTo for proper sorting
        return this._Material.compareToIgnoreCase(input._Material);
	}

}