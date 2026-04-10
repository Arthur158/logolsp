package logo.features;

import logo.analysis.DocumentState;
import logo.analysis.SymbolTable.SymbolReference;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsHandler {

    // built-in commands we should not flag as undefined
    private static final Set<String> BUILTINS = Set.of(
        "forward", "fd", "back", "bk", "right", "rt", "left", "lt",
        "penup", "pu", "pendown", "pd", "clearscreen", "cs", "home",
        "setpencolor", "setpc", "print", "show", "make", "repeat",
        "if", "stop", "output", "op", "wait", "hideturtle", "showturtle"
    );

    public static List<Diagnostic> compute(DocumentState doc) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // check procedure references
        for (SymbolReference ref : doc.symbols.getProcedureRefs()) {
            String name = ref.name().toLowerCase();
            if (!BUILTINS.contains(name) && doc.symbols.findProcedure(name) == null) {
                diagnostics.add(new Diagnostic(
                    ref.range(),
                    "Undefined procedure: " + ref.name(),
                    DiagnosticSeverity.Error,
                    "logo-lsp"
                ));
            }
        }

        // check variable references
        // for (SymbolReference ref : doc.symbols.getVariableRefs()) {
        //     String name = ref.name().toLowerCase();
        //     if (doc.symbols.findVariable(name) == null) {
        //         diagnostics.add(new Diagnostic(
        //             ref.range(),
        //             "Undefined variable: " + ref.name(),
        //             DiagnosticSeverity.Error,
        //             "logo-lsp"
        //         ));
        //     }
        // }
        for (SymbolReference ref : doc.symbols.getVariableRefs()) {
            if (!doc.symbols.isVariableValidAt(ref.name(), ref.scope(), ref.line())) {
                diagnostics.add(new Diagnostic(
                    ref.range(),
                    "Variable not in scope: " + ref.name(),
                    DiagnosticSeverity.Error,
                    "logo-lsp"
                ));
            }
        }

        return diagnostics;
    }
}
