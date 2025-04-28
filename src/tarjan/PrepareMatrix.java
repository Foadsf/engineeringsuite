package tarjan;

import java.util.Collections; 
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import gui.Config;
import solver.LaunchOperations;
import String2ME.CheckString;
import String2ME.DerivEquation;
import String2ME.EqStorer;
import String2ME.VString;

/**
 * This class is to subdivide the equation system in various equations system.
 * 
 * @author pablo salinas
 * 
 */
public class PrepareMatrix {

	/**
	 * The variables(columns) are positive int; The functions(rows) are negative
	 * int;
	 */
	private AdjacencyList _relations;
	private LinkedList<NodoStorer> _Nodes = new LinkedList<NodoStorer>();
	private ArrayList<ArrayList<Node>> SCC;
	/* The nodes must be created first, so i will create two list of nodes */
	/** Variable nodes list */
	private LinkedList<Node> VarNodes = new LinkedList<Node>();
	/** Functions node list */
	private LinkedList<Node> FuncNodes = new LinkedList<Node>();

	public PrepareMatrix() {
        // Create the nodes
        VarNodes.clear(); // Ensure lists are clear if instance is reused
        FuncNodes.clear();
        for (int i = 0; i < CheckString.Var.getSize(); i++) { // Use actual size
            VarNodes.add(new Node(i + 1)); // Indices 1 to N
        }
         for (int i = 0; i < CheckString.Functions.size(); i++) { // Use actual size
             FuncNodes.add(new Node(-(i + 1))); // Indices -1 to -M
         }
    }

	/**
	 * At first i save the initial positions of the variables in the list, and
	 * then i call compareTo, because i want to know the count of the variables
	 * to call the tarjan method properly
	 */
	public void PreNewton() {

		createAdjacencyList();
         Collections.sort(this._Nodes); // Nodes sorted by variable count
         Tarjan T;
         ListMatrix Relations;
         LinkedList<PotentialRelationMatrix> PRM = new LinkedList<>();

         // --- Tarjan loop to find best SCC ordering (Keep this logic) ---
         // Determine the number of lowest count variables to check
          int lowestCount = (this._Nodes.isEmpty()) ? 0 : this._Nodes.getFirst().getCount();
          int nodesToCheck = 0;
          for (NodoStorer ns : this._Nodes) {
              if (ns.getCount() == lowestCount) {
                  nodesToCheck++;
              } else {
                  break;
              }
          }
          nodesToCheck = Math.min(nodesToCheck, 5); // Limit checks for performance


         // --- Store best result ---
         PotentialRelationMatrix bestPRM = null;

          System.out.println("INFO: Checking Tarjan starting from " + nodesToCheck + " variable(s) with lowest count (" + lowestCount + ")");
          for (int k = 0; k < nodesToCheck; k++) {
               NodoStorer startNodeStorer = this._Nodes.get(k);
                T = new Tarjan();
                this._relations.RestartNodes(); // Restart graph nodes
                // this.restartNodes(); // No longer needed if _Nodes only used for initial sort

                System.out.println("  Running Tarjan starting with node: " + startNodeStorer.getNode().getName());
                SCC = T.tarjan(startNodeStorer.getNode(), this._relations);

                if (SCC == null || SCC.isEmpty()) {
                     System.err.println("Warning: Tarjan returned empty SCC list for start node " + startNodeStorer.getNode().getName());
                     continue;
                }

                Relations = RelationMatrix(SCC);
                if (Relations == null || Relations.size() == 0) {
                     System.err.println("Warning: RelationMatrix is empty for start node " + startNodeStorer.getNode().getName());
                     continue;
                }
                PotentialRelationMatrix currentPRM = new PotentialRelationMatrix(Relations, SCC);
                 PRM.add(currentPRM); // Add even if diagonal not perfect, sort later

                 // Keep track of the best one found so far based on sorting criteria
                  if (bestPRM == null || currentPRM.compareTo(bestPRM) < 0) { // compareTo is reversed, lower value is better
                      bestPRM = currentPRM;
                  }
          }


         // --- Use the best SCC ordering found ---
          if (bestPRM == null) {
              System.err.println("ERROR: Could not determine a valid SCC ordering after Tarjan attempts.");
              // Fallback: Maybe try to solve as one big block? Requires changes.
              // For now, just exit.
              return;
          }

          System.out.println("INFO: Selected SCC ordering with " + bestPRM.ValuesDownDiagonal + " lower-triangular dependencies.");
          this.SCC = bestPRM.SCC; // Use the best SCC list
          updateTerms(this.SCC); // Update the list of remaining nodes (_Nodes)
          RelationMatrix2Newton(RelationMatrix(this.SCC), this.SCC); // Solve using the best ordering


    }

	/**
	 * After one tarjan iteration, this._Nodes must be updated, erasing the
	 * nodes that were used to create the Boolean Matrix
	 * 
	 * @param SCC
	 */
	private void updateTerms(ArrayList<ArrayList<Node>> SCC) {
		int aux;
		Iterator<NodoStorer> it;
		for (ArrayList<Node> Lista : SCC) {
			for (Node n : Lista) {
				aux = n.getName();
				if (aux > 0) {// Its a column and then a variable
					it = this._Nodes.listIterator();
					// Search in the list that variable, if founded, then
					// removes it
					while (it.hasNext()) {
						if (it.next().getNode().getName() == aux)
							it.remove();
					}
				}
			}
		}
	}

	/**
	 * @return The variable with less appearance count in
	 *         CheckString.Var.Variables
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private int getLowCountVariable() {
		Iterator<VString> it = CheckString.Var.Variables.listIterator();
		int n = Integer.MAX_VALUE;
		int i = 0;
		int varPosition = 0;
		VString aux;
		while (it.hasNext() & n != 1) {
			aux = it.next();
			if (aux.getCount() < n) {
				n = aux.getCount();
				varPosition = i;
			}
			i++;
		}
		return varPosition;
	}

	/**
	 * 
	 * @param SCC
	 * @return A Matrix with the relations between variables and functions; For
	 *         example the equations x = 2 ; x + y =4 would be this way: {{1 ,
	 *         0};{1 , 1}}
	 */
	private ListMatrix RelationMatrix(ArrayList<ArrayList<Node>> SCC) {

		// for(ArrayList<Node> An :SCC)
		// for(Node n :An)
		// System.out.println("Nodo: "+n.getName());

		LinkedList<String> var = new LinkedList<String>();
		LinkedList<Integer> func = new LinkedList<Integer>();
		int variable;
		/*
		 * This splits the SCC in functions(negatives values) and variables(the
		 * positives ones)
		 */
		for (int i = 0; i < SCC.size(); i++) {
			for (int j = 0; j < SCC.get(i).size(); j++) {
				variable = SCC.get(i).get(j).getName();
				if (variable > 0)
					var.add(CheckString.Var.getVar(variable - 1));// previously
																	// i added 1
				else {
					// if(variable != 0)
					func.add(Math.abs(variable) - 1);// previously i added 1
				}

			}
		}

		ListMatrix result = new ListMatrix(var.size());

		Iterator<Integer> it = func.listIterator();
		EqStorer function;
		int row = 0;
		while (it.hasNext()) {
			int position = it.next();
			function = CheckString.Functions.get(position);

			Iterator<String> it2 = var.listIterator();
			String nombre;
			int col = 0;
			while (it2.hasNext()) {
				nombre = it2.next();
				for (int j = 0; j < function.aux.size(); j++)
					if (function.aux.get(j).GetVar().equalsIgnoreCase(nombre))
						result.setValue(row, col, (byte) 1);

				col++;
			}

			row++;
		}

		return result;
	}

	/**
	 * 
	 * @param Lista
	 * @param name
	 * @return the node of the list that have the same name of the integer
	 *         introduced
	 */
	private Node NodeOfTheList(LinkedList<Node> Lista, int name) {

		Iterator<Node> it = Lista.listIterator();
		Node aux;
		while (it.hasNext()) {
			aux = it.next();
			if (name == aux.getName())
				return aux;
		}
		return new Node(0);

	}

	/**
	 * 
	 * @param var
	 * @return The position of that variable in CheckString.Var.Variables
	 */
	private NodoNameCount getVarPosition(String var) {
		Iterator<VString> it = CheckString.Var.Variables.listIterator();
		int i = 0;
		VString aux;
		while (it.hasNext()) {
			aux = it.next();
			if (aux.getVar().equalsIgnoreCase(var))
				return new NodoNameCount(i, aux.getCount());
			i++;
		}
		return new NodoNameCount(-1, -1);

	}

	/**
	 * Creates an adjacency List of all the variables to use in Tarjan
	 */
	private void createAdjacencyList() {
		this._relations = new AdjacencyList();
		// int fila=-1,col=1;
		int col = -1, fila;
		EqStorer Eqaux;
		Node Naux, nodo;
		Iterator<EqStorer> it2 = CheckString.Functions.iterator();
		NodoNameCount NodoAux;
		while (it2.hasNext()) {
			Eqaux = it2.next();
			Naux = NodeOfTheList(FuncNodes, col);
			if (Naux.name != 0) {
				Iterator<DerivEquation> it3 = Eqaux.aux.listIterator();
				fila = 0;
				while (it3.hasNext()) {
					NodoAux = getVarPosition(it3.next().GetVar());
					nodo = NodeOfTheList(VarNodes, NodoAux.getName() + 1);
					if (nodo.name != 0) {
						// Store the from node
						// First i check if the node is already in the list
						boolean found = false;
						for (int k = 0; k < this._Nodes.size(); k++) {
							if (this._Nodes.get(k).getNode().getName() == nodo
									.getName())
								found = true;
						}
						if (!found)// add node
							this._Nodes.add(new NodoStorer(nodo, NodoAux
									.getCount()));

						_relations.addEdge(Naux, nodo, 1);
						_relations.addEdge(nodo, Naux, 1);
						// System.out.println("Nodo uno que añado: "+Naux.getName()+" -> "+nodo.getName());
						// System.out.println("Nodo dos que añado: "+nodo.getName()+" -> "+Naux.getName());
						fila++;
					}
				}

				col--;
			}
		}
	}

	/**
	 * @return the number of variables that appears the same than the one with
	 *         lowest appearance
	 */
	@SuppressWarnings("unused")
	private int getLowRepeated() {
		Iterator<NodoStorer> itNode = this._Nodes.listIterator();
		int previous = this._Nodes.getFirst().getCount();
		int actual = previous;
		int k = -1;
		while (actual == previous) {
			previous = actual;
			if (itNode.hasNext())
				actual = itNode.next().getCount();
			else {
				actual = 1;
				previous = 0;
			}
			k++;
		}
		return k;
	}

	private void restartNodes() {

		for (NodoStorer n : this._Nodes)
			n.getNode().restart();
	}

	/**
	 * Makes Jacobians matrix from the relation matrix and calls a method for
	 * solving them
	 * 
	 * @param relations
	 */
	private void RelationMatrix2Newton(ListMatrix relations,
			ArrayList<ArrayList<Node>> scc) {
		ArrayList<Byte> aux;
		LinkedList<Integer> Variables = new LinkedList<Integer>();
		int k = 0;
		// At first i check the equations with one variable for solving them
		// boolean found = false;

		/*
		 * while(k < relations.size()){ Variables.clear(); found = false; aux =
		 * relations.getRow(k); k++; if(ListMatrix.numberVariables(aux)==1){ k =
		 * 0; // while(!found){ // if(aux.get(i)!=ListMatrix.cero) // found =
		 * true; // else i++; //System.out.println("Found "+i); // } Variables =
		 * ListMatrix.AnalizeRow(Variables, aux); //Variables.add(new
		 * Integer(i)); relations.refresh(Variables);//SE QUEDAN FILAS SIN
		 * BORRAR!! MakeJacobian(Variables , scc); } }
		 */
		// System.out.println("Purgada");
		// System.out.println(relations);

		// Now i analyze the equations

		while (relations.size() > 0) {
			Variables.clear();
			k = 0;

			aux = relations.getRow(0);
			Variables = ListMatrix.AnalizeRow(Variables, aux);

			// At first i check if we have a equation with one variable
			if (ListMatrix.numberVariables(aux) == 1) {
				MakeJacobian(Variables, scc);
				relations.refresh(Variables);
			} else {
				while (ListMatrix.numberVariables(aux) != k + 1) {
					k++;
					aux = relations.OperateRow(aux, relations.getRow(k));
					Variables = ListMatrix.AnalizeRow(Variables, aux);
				}

				MakeJacobian(Variables, scc);
				relations.refresh(Variables);
			}
		}

	}

	// We need a Vector of functions, a vector of variables and a analitic
	// Jacobian.
	/**
	 * 
	 * Makes two lists one with the functions and one with the variables. And
	 * then it calls to the solvers methods.
	 * 
	 * @param Variables
	 * @param scc
	 */
	private void MakeJacobian(LinkedList<Integer> VariablesIndices, ArrayList<ArrayList<Node>> scc) {
        // Sort indices for consistency (optional but good)
        Collections.sort(VariablesIndices);

        // Find the corresponding function indices from the SCC structure
        LinkedList<Integer> FunctionsIndices = new LinkedList<>();
        boolean[] varUsed = new boolean[CheckString.Var.getSize()]; // Track variables used in this subsystem
        boolean[] funcUsed = new boolean[CheckString.Functions.size()]; // Track functions used

        // Mark variables belonging to this subsystem
        for (Integer varIndex : VariablesIndices) {
            if (varIndex != null && varIndex >= 0 && varIndex < varUsed.length) {
                 varUsed[varIndex] = true;
            }
        }

        // Find functions that involve *only* variables from this subsystem
        // This requires iterating through the original GLOBAL CheckString lists
        for (int funcIndex = 0; funcIndex < CheckString.Functions.size(); funcIndex++) {
             EqStorer eq = CheckString.Functions.get(funcIndex);
             boolean belongsToSubsystem = true;
             boolean usesAnySubsystemVar = false;

             if (eq.aux == null) continue; // Skip if no variables

             for (DerivEquation de : eq.aux) {
                  int varIndex = findVarIndexGlobal(de.GetVar()); // Find index in GLOBAL list
                  if (varIndex != -1) {
                       if (varUsed[varIndex]) {
                            usesAnySubsystemVar = true; // This function uses a variable from our target list
                       } else {
                            belongsToSubsystem = false; // This function uses a variable NOT in our target list
                            break; // No need to check further variables for this function
                       }
                  } else {
                       // Variable from equation not found in global list - parsing error?
                       belongsToSubsystem = false;
                       break;
                  }
             }

             // Add function index if it belongs exclusively to this subsystem's variables
             // AND it hasn't already been assigned to another subsystem (though Tarjan should prevent overlap)
             if (belongsToSubsystem && usesAnySubsystemVar) {
                 FunctionsIndices.add(funcIndex);
                 funcUsed[funcIndex] = true; // Mark as used
             }
        }

        // Simple check - often fails if Tarjan groups strangely, but basic sanity check
        if (FunctionsIndices.size() != VariablesIndices.size()) {
             System.out.println("WARNING: Subsystem identified by Tarjan has mismatched function/variable count ("+FunctionsIndices.size()+" funcs, "+VariablesIndices.size()+" vars). Solver might fail.");
             System.out.println("  Functions Indices: " + FunctionsIndices);
             System.out.println("  Variables Indices: " + VariablesIndices);
        }
        if (FunctionsIndices.isEmpty() || VariablesIndices.isEmpty()) {
             System.out.println("INFO: Skipping empty subsystem identified by Tarjan.");
             return; // Nothing to solve
        }


        // --- CORRECTED CALL to LaunchOperations constructor ---
        solver.LaunchOperations LO = new solver.LaunchOperations(FunctionsIndices, VariablesIndices);
        // --- END CORRECTION ---

        solver.OperationCounter OC = new solver.OperationCounter();
        Thread N = new Thread(OC, "Clock");
        Thread m = new Thread(LO, "Operations");
        N.start();
        m.start();
        while (m.isAlive()) {
            if (!N.isAlive()) {
                m.interrupt(); // Use interrupt instead of stop
                System.err.println("WARNING: Solver thread interrupted due to timeout!");
                Config.ErrorFound = true; // Indicate timeout potentially led to incomplete solve
                break; // Exit wait loop
            }
             // Optional: Add a short sleep to prevent busy-waiting
             try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        // Wait briefly for thread to potentially finish after interrupt
        try { m.join(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

         // Remove solved functions and variables from global lists for next iteration
         // This requires careful index management as lists shrink
         removeSolvedItems(FunctionsIndices, VariablesIndices);

    }

	// --- Helper to find index in GLOBAL CheckString.Var.Variables ---
	private int findVarIndexGlobal(String variable) {
		for (int i = 0; i < CheckString.Var.Variables.size(); i++) {
			if (CheckString.Var.Variables.get(i).getVar().equalsIgnoreCase(variable)) {
				return i;
			}
		}
		return -1;
	}

	// --- Helper to remove solved items, managing indices ---
	private void removeSolvedItems(LinkedList<Integer> solvedFuncIndices, LinkedList<Integer> solvedVarIndices) {
		// Remove items from higher indices first to avoid shifting issues
		Collections.sort(solvedFuncIndices, Collections.reverseOrder());
		for (int index : solvedFuncIndices) {
			if (index >= 0 && index < CheckString.Functions.size()) {
				CheckString.Functions.remove(index);
			}
		}

		Collections.sort(solvedVarIndices, Collections.reverseOrder());
		for (int index : solvedVarIndices) {
			if (index >= 0 && index < CheckString.Var.Variables.size()) {
				// Move solved var to OneEquationVar list before removing from main list
				VString solvedVar = CheckString.Var.Variables.get(index);
				boolean alreadyMoved = false;
				for(VString vs : CheckString.OneEquationVar) {
					if(vs.getVar().equalsIgnoreCase(solvedVar.getVar())) {
						alreadyMoved = true; break;
					}
				}
				if (!alreadyMoved) CheckString.OneEquationVar.add(solvedVar);

				CheckString.Var.Variables.remove(index);
			}
		}

		// Also need to update the node lists used for Tarjan if PrepareMatrix is reused
		updateNodesAfterSolve(solvedFuncIndices, solvedVarIndices);
	}

	// --- Helper to update internal node lists ---
	private void updateNodesAfterSolve(LinkedList<Integer> solvedFuncIndices, LinkedList<Integer> solvedVarIndices) {
		// Remove corresponding nodes from VarNodes and FuncNodes
		LinkedList<Integer> funcNodeNames = new LinkedList<>();
		for(int i : solvedFuncIndices) funcNodeNames.add(-(i+1)); // Convert back to negative node name
		FuncNodes.removeIf(node -> funcNodeNames.contains(node.getName()));

		LinkedList<Integer> varNodeNames = new LinkedList<>();
		for(int i : solvedVarIndices) varNodeNames.add(i+1); // Convert back to positive node name
		VarNodes.removeIf(node -> varNodeNames.contains(node.getName()));

		// Also remove from _Nodes list used for initial sorting
			_Nodes.removeIf(nodoStorer -> varNodeNames.contains(nodoStorer.getNode().getName()));

	}

	/**
	 * Erase from the CheckString.Var the variables stored in
	 * CheckString.OneEquationVar. Erase the solved variables from the
	 * checkString.Functions. Solves the equations that now have one variable.
	 */

	public static void PreTarjan() {
		int pos = 0;
		while (pos != -1) {
			pos = searchOneVariableFunction();
			if (pos != -1) {
				String functionString = CheckString.Functions.get(pos).getEquation();
				String varString = CheckString.Functions.get(pos).aux.get(0).GetVar();
				System.out.println("INFO: Pre-solving single variable function (Index " + pos + ") for variable: " + varString);
				solver.LaunchOperations LO = new solver.LaunchOperations(functionString, varString);
				solver.OperationCounter OC = new solver.OperationCounter();
				Thread N = new Thread(OC, "Clock"); // N is defined HERE
				Thread m = new Thread(LO, "Operations");
				N.start();
				m.start();
				while (m.isAlive()) {
					if (!N.isAlive()) { // N is accessible here
						m.interrupt();
						System.err.println("WARNING: Pre-solver thread interrupted due to timeout for var " + varString + "!");
						Config.ErrorFound = true; // Access Config correctly
						break;
					}
					try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				}
				try { m.join(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

				if (!Config.ErrorFound) { // Access Config correctly
					// ... rest of removal logic ...
					// Find the corresponding VString in global Var list
					Iterator<VString> itVar = CheckString.Var.Variables.iterator();
					VString solvedVString = null;
					while(itVar.hasNext()){
							VString current = itVar.next();
							if(current.getVar().equalsIgnoreCase(varString)){
								solvedVString = current;
								itVar.remove(); // Remove from unsolved list
								break;
							}
					}
					// Add to solved list (if found)
					if(solvedVString != null) {
							boolean alreadyMoved = false;
							for(VString vs : CheckString.OneEquationVar) if(vs.getVar().equalsIgnoreCase(solvedVString.getVar())) alreadyMoved = true;
							if(!alreadyMoved) CheckString.OneEquationVar.add(solvedVString);
					} else {
							System.err.println("Warning: Could not find variable " + varString + " in global list after pre-solve.");
					}

					updateFunctions(varString);
					CheckString.FunctionsSolved.add(CheckString.Functions.get(pos));
					CheckString.Functions.remove(pos);
				} else {
					System.err.println("Skipping removal of function/variable " + varString + " due to solve error.");
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @return -1 means no function found
	 */
	public static int searchOneVariableFunction() {
		int k = 0;
		for (EqStorer eq : CheckString.Functions) {
			if (eq.aux.size() == 1)
				return k;
			k++;
		}

		return -1;
	}

	/**
	 * Erase from the Functions List the variables that belongs to one variable
	 * equations
	 */
	public static void cleanFunction() {
		/*------Erase from the Functions List the variables that belongs to one variable equations--*/
		Iterator<DerivEquation> it2;
		DerivEquation Dev;
		for (EqStorer Eq : CheckString.Functions) {
			it2 = Eq.aux.listIterator();
			while (it2.hasNext()) {
				Dev = it2.next();
				for (VString S : CheckString.OneEquationVar)
					if (Dev.GetVar().equalsIgnoreCase(S.getVar()))
						it2.remove();
			}
		}
	}

	/**
	 * Erase from the Var List the variables that belongs to one variable
	 * equations
	 */
	public static void cleanVar() {
		Iterator<VString> it;
		boolean found = false;
		VString auxV;
		for (VString S : CheckString.OneEquationVar) {
			it = CheckString.Var.Variables.listIterator();
			found = false;
			while (it.hasNext() & !found) {
				auxV = it.next();
				if (auxV.getVar().equalsIgnoreCase(S.getVar())) {
					S.setCount(auxV.getCount() + 1);
					found = true;
					it.remove();
				}
			}
		}
	}

	/**
	 * Removes the variable from the equations of Functions
	 * 
	 * @param aux
	 */
	public static void updateFunctions(String aux) {
		Iterator<DerivEquation> it;
		for (EqStorer eq : CheckString.Functions) {
			it = eq.aux.listIterator();
			while (it.hasNext())
				if (it.next().GetVar().equalsIgnoreCase(aux))
					it.remove();
		}

	}

}