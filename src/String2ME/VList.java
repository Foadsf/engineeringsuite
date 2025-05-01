package String2ME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A list to store a variable and count the times it appears
 * 
 * @author Pablo Salinas
 * 
 */
public class VList {

	public List<VString> Variables = new ArrayList<VString>();

	/**
	 * At first we check if the variable is already in the List if is in the List the we will count
	 * one if not we well add it
	 * 
	 * @param cadena
	 */
	public void AddVar(String cadena) {
		boolean IsInIt = false;
		Iterator<VString> it = Variables.listIterator();
		VString aux;
		// We search for it
		while ((it.hasNext()) & (!IsInIt)) {
			aux = it.next();
			if (aux.getVar().equals(cadena)) {
				IsInIt = true;
				aux.CountUp();
			}
		}
		if (!IsInIt) {
			aux = new VString(cadena);
			Variables.add(aux);
		}
	}

	/**
	 * This method subtract one count of the count variable of a variable if this count reach the zero
	 * then the variable will be erase
	 * 
	 * @param cadena
	 * @return boolean. If the variable is not in the List then returns false
	 */
	public boolean Erase(String cadena) {
		boolean IsInIt = false;
		Iterator<VString> it = Variables.listIterator();
		VString aux;
		// We search for it
		while ((it.hasNext()) & (!IsInIt)) {
			aux = it.next(); // Maybe NullPointerException Error
			if (aux.getVar().equals(cadena)) {
				IsInIt = true;
				aux.CountDown();
				if (aux.getCount() == 0) {
					it.remove();
				}
			}
		}

		return IsInIt;
	}

	/**
	 * 
	 * @param i
	 * @return The variable at the i position
	 */
	public String getVar(int i) {
		return Variables.get(i).getVar();

	}

	/**
	 * 
	 * @param i
	 * @return the times a variable appears
	 */
	public int getCount(int i) {
		return Variables.get(i).getCount();
	}

	/**
	 * 
	 * @param String , the variable
	 * @return the times that that variable appears
	 */
	public int getCount(String cadena) {
		boolean IsInIt = false;
		Iterator<VString> it = Variables.listIterator();
		VString aux;
		while ((it.hasNext()) & (!IsInIt)) {
			aux = it.next();
			if (aux.getVar().equals(cadena)) {
				IsInIt = true;
				return aux.getCount();
			}
		}

		return -1;

	}

	/**
	 * 
	 * @return the size of the List
	 */
	public int getSize() {
		return Variables.size();
	}

	/**
	 * Adds the variable 'cadena' to the list or increments its count. Expects 'cadena' to be in the
	 * internal format (lowercase, Gg).
	 * 
	 * @param cadena The internal variable name.
	 * @param counts The number of times this variable was found (usually 1).
	 */
	public void addCountVar(String cadena, int counts) {
		// ---- ADD DEBUG LINE ----
		System.out.println("DEBUG: VList.addCountVar called with: '" + cadena + "' count: " + counts);
		// ---- END DEBUG LINE ----

		boolean IsInIt = false;
		Iterator<VString> it = Variables.listIterator();
		VString aux = null; // Initialize aux

		// Search for it using case-sensitive equals (should match internal format)
		while (it.hasNext()) { // Removed redundant !IsInIt check from condition
			aux = it.next();
			// Ensure case-insensitive comparison JUST IN CASE, although input should be lowercase
			if (aux.getVar().equalsIgnoreCase(cadena)) { // Using equalsIgnoreCase for safety
				IsInIt = true;
				aux.addCount(counts);
				break; // Found it, stop searching
			}
		}

		// If not found, add it
		if (!IsInIt) {
			// Create VString with the provided internal name 'cadena'
			aux = new VString(cadena); // Constructor sets count to 1
			Variables.add(aux);
			// Apply the *rest* of the counts if counts > 1
			if (counts > 1) {
				aux.addCount(counts - 1);
			}
		}
	}

	/**
	 * This method eliminate one variable and it's count
	 * 
	 * @param cadena
	 */
	public void Exterminate(String cadena) {

		boolean IsInIt = false;
		Iterator<VString> it = Variables.listIterator();
		VString aux;
		int i = 0;
		// We search for it
		while ((it.hasNext()) & (!IsInIt)) {
			aux = it.next();
			i++;
			if (aux.getVar().equalsIgnoreCase(cadena)) {
				IsInIt = true;
				it.remove();
			}

		}
	}

}
