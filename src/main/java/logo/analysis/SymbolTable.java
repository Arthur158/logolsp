package logo.analysis;

import org.eclipse.lsp4j.Range;

import java.util.*;

public class SymbolTable {

    public record SymbolReference(String name, Range range, String scope, int order) {}

    // procedure name → declaration range
    private final Map<String, Range> procedures = new HashMap<>();

    // scope → variable name → (declaration range, declaration order)
    // null scope = global
    private final Map<String, Map<String, VariableDeclaration>> scopedVariables = new HashMap<>();

    private final List<SymbolReference> procedureRefs = new ArrayList<>();
    private final List<SymbolReference> variableRefs  = new ArrayList<>();

    public record VariableDeclaration(Range range, int order) {}

    public void addProcedure(String name, Range range) {
        procedures.put(name.toLowerCase(), range);
    }

    // scope = null means global
    public void addVariable(String name, Range range, String scope, int order) {
        scopedVariables
            .computeIfAbsent(scope, k -> new HashMap<>())
            .put(name.toLowerCase(), new VariableDeclaration(range, order));
    }

    public void addProcedureRef(String name, Range range, String scope, int order) {
        procedureRefs.add(new SymbolReference(name, range, scope, order));
    }

    public void addVariableRef(String name, Range range, String scope, int order) {
        variableRefs.add(new SymbolReference(name, range, scope, order));
    }

    public Range findProcedure(String name) {
        return procedures.get(name.toLowerCase());
    }

    // find declaration range for goto-definition
    public Range findVariable(String name, String scope, int usageOrder) {
        String key = name.toLowerCase();

        // check local scope first
        VariableDeclaration local = getFromScope(key, scope);
        if (local != null && local.order() < usageOrder) return local.range();

        // fall back to global
        VariableDeclaration global = getFromScope(key, null);
        if (global != null && global.order() < usageOrder) return global.range();

        return null;
    }

    // for code actions where we don't have position context
    public Range findVariable(String name) {
        String key = name.toLowerCase();
        for (Map<String, VariableDeclaration> scope : scopedVariables.values()) {
            if (scope.containsKey(key)) return scope.get(key).range();
        }
        return null;
    }

    public boolean isVariableValidAt(String name, String scope, int usageOrder) {
        String key = name.toLowerCase();

        // check local scope — declared before usage
        VariableDeclaration local = getFromScope(key, scope);
        if (local != null && local.order() < usageOrder) return true;

        // check global scope — also must be declared before usage
        VariableDeclaration global = getFromScope(key, null);
        if (global != null && global.order() < usageOrder) return true;

        return false;
    }

    private VariableDeclaration getFromScope(String name, String scope) {
        Map<String, VariableDeclaration> vars = scopedVariables.get(scope);
        if (vars == null) return null;
        return vars.get(name);
    }

    public Range findProcedureRef(String name) {
        return procedures.get(name.toLowerCase());
    }

    public List<SymbolReference> getProcedureRefs() { return procedureRefs; }
    public List<SymbolReference> getVariableRefs()  { return variableRefs; }
    public Set<String> getAllVariableNames() {
        Set<String> all = new HashSet<>();
        for (Map<String, VariableDeclaration> scope : scopedVariables.values()) {
            all.addAll(scope.keySet());
        }
        return all;
    }
    public Set<String> getAllProcedureNames() { return procedures.keySet(); }
}
