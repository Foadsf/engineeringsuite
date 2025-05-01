// --- File: src/String2ME/Pila.java ---
package String2ME;

import java.util.*;

/**
 * This Class is to convert parenthesis into clasps because in matheclipse functions like sin must
 * be followed by a clasp. Uses a static list to maintain state across calls within a single
 * GramCheck.
 *
 * @author Pablo Salinas
 *
 */
class Pila {
	/**
	 * The list is a list of bytes, the meanings are: 0 = Is a parenthesis '(' 1 = Is a clasp '[' not
	 * requiring degree conversion 2 = Is a clasp '[' requiring degree conversion insertion '))' later
	 */
	private static List<Byte> Pila = new LinkedList<Byte>(); // Make the list static

	/**
	 * Clears the static Pila stack.
	 */
	// ---- ADD static ----
	protected static void ErasePila() {
		// Correct way to clear a List
		Pila.clear();
	}

	/**
	 * Add a byte to the static stack.
	 * 
	 * @param c The byte representing the bracket type (0, 1, or 2).
	 */
	// ---- ADD static ----
	public static void AddTerm(byte c) {
		Pila.add(c);
	}

	/**
	 * Returns the last value from the static stack and removes it. Throws NoSuchElementException if
	 * the stack is empty.
	 * 
	 * @return a byte representing the last opened bracket type.
	 * @throws NoSuchElementException if the stack is empty.
	 */
	// ---- ADD static ----
	public static byte GetTerm() throws NoSuchElementException {
		if (Pila.isEmpty()) {
			throw new NoSuchElementException("Attempted to get term from empty Pila stack.");
		}
		// LinkedList doesn't have efficient random access, use removeLast
		return Pila.remove(Pila.size() - 1); // removeLast() returns the element
	}

	/**
	 * Returns the current size of the static stack minus 1. Returns -1 if the stack is empty (as per
	 * original logic's check).
	 * 
	 * @return the size of the stack minus 1, or -1 if empty.
	 */
	// ---- ADD static ----
	public static int GetSize() {
		// The original code checked against -1, implying an empty stack
		// should result in -1 being returned from GetSize().
		// Let's maintain that logic. size() returns 0 when empty.
		return Pila.size() - 1;
	}
}
