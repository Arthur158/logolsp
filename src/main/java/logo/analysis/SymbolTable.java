package logo.analysis;

import org.eclipse.lsp4j.Range;

import java.util.*;

public class SymbolTable {

    public record SymbolReference(String name, Range range, String scope, int line) {}
    public record VariableDeclaration(Range range, int line) {}
    public record CallSite(String procName, Range fullRange, List<Range> argRanges) {}

    private final Map<String, Map<String, List<VariableDeclaration>>> scopedVariables = new HashMap<>();

    private final List<SymbolReference> procedureRefs = new ArrayList<>();
    private final List<SymbolReference> variableRefs  = new ArrayList<>();
    private final List<Map.Entry<Range, String>> procedureRanges = new ArrayList<>();


    public record ProcedureSignature(Range declarationRange, List<String> params) {}

    private final Map<String, ProcedureSignature> procedures = new HashMap<>();

    public final List<CallSite> callSites = new ArrayList<>();

    public void addCallSite(String procName, Range fullRange, List<Range> argRanges) {
        callSites.add(new CallSite(procName.toLowerCase(), fullRange, argRanges));
    }

    public List<CallSite> getCallSites(String procName) {
        return callSites.stream()
            .filter(c -> c.procName().equals(procName.toLowerCase()))
            .toList();
    }

    public void addProcedure(String name, Range range, List<String> params, Range funcRange) {
        procedures.put(name.toLowerCase(), new ProcedureSignature(range, params));
        Logger.log("adding provedure:");
        Logger.log(name);
        Logger.log(range.toString());
    }

    public ProcedureSignature getProcedure(String name) {
        return procedures.get(name.toLowerCase());
    }

    public Range findProcedure(String name) {
        ProcedureSignature sig = procedures.get(name.toLowerCase());
        return sig == null ? null : sig.declarationRange();
    }

    public Set<Map.Entry<String, ProcedureSignature>> getAllProcedureEntries() {
        return procedures.entrySet();
    }

    public void addVariable(String name, Range range, String scope, int line) {
        scopedVariables
            .computeIfAbsent(scope, k -> new HashMap<>())
            .computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>())
            .add(new VariableDeclaration(range, line));
    }

    public void addProcedureRef(String name, Range range, String scope, int line) {
        procedureRefs.add(new SymbolReference(name, range, scope, line));
    }

    public void addVariableRef(String name, Range range, String scope, int line) {
        variableRefs.add(new SymbolReference(name, range, scope, line));
    }

    // public Range findVariable(String name, Position pos) {

    // }

    // find declaration range for goto-definition
    public Range findVariable(String name, String scope, int line) {
        String key = name.toLowerCase();

        VariableDeclaration currBest = null;
        // check local scope first
        List<VariableDeclaration> localList = getFromScope(key, scope);
        for (VariableDeclaration local : localList) {
            if (local != null && local.line() < line && currBest.line < local.line()) currBest = local;
        }
        List<VariableDeclaration> globalList = getFromScope(key, scope);
        for (VariableDeclaration global : globalList) {
            if (global != null && global.line() < line && currBest.line < global.line()) currBest = global;
        }
        return currBest.range();
    }

    public boolean isVariableValidAt(String name, String scope, int line) {
        return findVariable(name, scope, line) != null;
    }

    private List<VariableDeclaration> getFromScope(String name, String scope) {
        Map<String, List<VariableDeclaration>> vars = scopedVariables.get(scope);
        if (vars == null) return null;
        return vars.get(name);
    }

    public Range findProcedureRef(String name) {
        return procedures.get(name.toLowerCase()).declarationRange();
    }

    public List<SymbolReference> getProcedureRefs() { return procedureRefs; }
    public List<SymbolReference> getVariableRefs()  { return variableRefs; }
    public Set<String> getAllVariableNames() {
        Set<String> all = new HashSet<>();
        for (Map<String, List<VariableDeclaration>> scope : scopedVariables.values()) {
            all.addAll(scope.keySet());
        }
        return all;
    }

    // public Map<String, VariableDeclaration> getVariablesInScope(String scope) {
    //     Map<String, List<VariableDeclaration>> result = new HashMap<>();
    //
    //     // global first
    //     Map<String, List<VariableDeclaration>> global = scopedVariables.get(null);
    //     if (global != null) {
    //         result.putAll(global);
    //     }
    //
    //     // then local overrides
    //     Map<String, VariableDeclaration> local = scopedVariables.get(scope);
    //     if (local != null) {
    //         result.putAll(local);
    //     }
    //
    //     return result;
    // }
    public Set<String> getAllProcedureNames() { return procedures.keySet(); }
}
