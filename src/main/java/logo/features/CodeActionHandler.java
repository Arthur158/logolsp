package logo.features;

import logo.analysis.DocumentState;
import logo.analysis.EditDistance;
import logo.analysis.Logger;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.*;
import java.util.stream.Stream;

public class CodeActionHandler {

    private static final List<String> BUILTINS = List.of(
        "forward", "fd", "back", "bk", "right", "rt", "left", "lt",
        "penup", "pu", "pendown", "pd", "clearscreen", "cs", "home",
        "setpencolor", "setpc", "print", "show", "make", "repeat",
        "if", "stop", "output", "op", "wait", "hideturtle", "showturtle"
    );


    public static List<Either<Command, CodeAction>> compute(
            DocumentState doc, CodeActionParams params) {

        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        for (Diagnostic diagnostic : params.getContext().getDiagnostics()) {
            String message = diagnostic.getMessage();

            if (message.startsWith("Variable not in scope: ")) {
                String badName = message.substring("Variable not in scope: ".length());
                String closest = EditDistance.findClosest(
                    badName, doc.symbols.getAllVariableNames()
                );
                if (closest != null) {
                    actions.add(Either.forRight(
                        buildFix(diagnostic, badName, closest,
                                 "Change to :" + closest, doc.uri)
                    ));
                }
            }

            if (message.startsWith("Undefined procedure: ")) {
                String badName = message.substring("Undefined procedure: ".length());
                String closest = EditDistance.findClosest(
                    badName,
                    Stream.concat(
                        doc.symbols.getAllProcedureNames().stream(),
                        BUILTINS.stream()
                    ).toList()
                );
                if (closest != null) {
                    actions.add(Either.forRight(
                        buildFix(diagnostic, badName, closest,
                                 "Change to " + closest, doc.uri)
                    ));
                }
            }
        }

        return actions;
    }

    private static CodeAction buildFix(Diagnostic diagnostic,
                                       String oldText, String newText,
                                       String title, String uri) {
        // the text edit replaces the bad token range with the correct name
        TextEdit edit = new TextEdit(diagnostic.getRange(), newText);

        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        workspaceEdit.setChanges(Map.of(uri, List.of(edit)));

        CodeAction action = new CodeAction(title);
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        action.setEdit(workspaceEdit);

        return action;
    }
}
