package projet.hanouti.AIachat.tools;

import java.util.Map;
import java.util.Set;

/**
 * Detection result for one user message.
 */
public class RefinementIntent {

    private final Set<RefinementType> types;
    private final Map<RefinementType, String> parameters;

    public RefinementIntent(Set<RefinementType> types, Map<RefinementType, String> parameters) {
        this.types = types;
        this.parameters = parameters;
    }

    public boolean has(RefinementType type) {
        return types.contains(type);
    }

    public String getParameter(RefinementType type) {
        return parameters.getOrDefault(type, null);
    }

    public Set<RefinementType> getTypes() {
        return types;
    }

    public Map<RefinementType, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "RefinementIntent{types=" + types + ", params=" + parameters + "}";
    }
}


