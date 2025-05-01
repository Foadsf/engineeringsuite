// --- File: src/solver/ODEProblemDefinition.java ---
package solver;

/**
 * Stores the definition of an Ordinary Differential Equation
 * Initial Value Problem parsed from the input.
 */
public class ODEProblemDefinition {

    private final String targetVariable;
    private final String derivativeExpression;
    private final String integrationVariable;
    private final String startTimeExpression;
    private final String endTimeExpression;
    private final String initialValueExpression;
    private final String originalEquation; // Store the original line for reference/debugging

    public ODEProblemDefinition(String targetVariable, String derivativeExpression,
                                String integrationVariable, String startTimeExpression,
                                String endTimeExpression, String initialValueExpression,
                                String originalEquation) {
        this.targetVariable = targetVariable;
        this.derivativeExpression = derivativeExpression;
        this.integrationVariable = integrationVariable;
        this.startTimeExpression = startTimeExpression;
        this.endTimeExpression = endTimeExpression;
        this.initialValueExpression = initialValueExpression;
        this.originalEquation = originalEquation;
    }

    // --- Getters ---
    public String getTargetVariable() {
        return targetVariable;
    }

    public String getDerivativeExpression() {
        return derivativeExpression;
    }

    public String getIntegrationVariable() {
        return integrationVariable;
    }

    public String getStartTimeExpression() {
        return startTimeExpression;
    }

    public String getEndTimeExpression() {
        return endTimeExpression;
    }

    public String getInitialValueExpression() {
        return initialValueExpression;
    }

    public String getOriginalEquation() {
        return originalEquation;
    }

    @Override
    public String toString() {
        return "ODEProblemDefinition{" +
               "target='" + targetVariable + '\'' +
               ", derivative='" + derivativeExpression + '\'' +
               ", integrateVar='" + integrationVariable + '\'' +
               ", t0='" + startTimeExpression + '\'' +
               ", tFinal='" + endTimeExpression + '\'' +
               ", y0='" + initialValueExpression + '\'' +
               '}';
    }
}