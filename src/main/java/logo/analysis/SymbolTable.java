package logo.analysis;

import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    public record SymbolReference(String name, Range range) {}

    private final Map<String, Range> procedures = new HashMap<>();
    private final Map<String, Range> variables  = new HashMap<>();

    private final List<SymbolReference> procedureRefs = new ArrayList<>();
    private final List<SymbolReference> variableRefs  = new ArrayList<>();

    public void addProcedure(String name, Range range) {
        procedures.put(name.toLowerCase(), range);
    }

    public void addVariable(String name, Range range) {
        variables.put(name.toLowerCase(), range);
    }

    public void addProcedureRef(String name, Range range) {
        procedureRefs.add(new SymbolReference(name, range));
    }

    public void addVariableRef(String name, Range range) {
        variableRefs.add(new SymbolReference(name, range));
    }

    public Range findProcedure(String name) {
        return procedures.get(name.toLowerCase());
    }

    public Range findVariable(String name) {
        return variables.get(name.toLowerCase());
    }

    public List<SymbolReference> getProcedureRefs() {
        return procedureRefs;
    }

    public List<SymbolReference> getVariableRefs() {
        return variableRefs;
    }
}
