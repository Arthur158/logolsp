package logo.features;

import logo.analysis.DocumentState;
import logo.analysis.SymbolTable.CallSite;
import logo.analysis.SymbolTable.ProcedureSignature;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.*;

public class ReorderParamsHandler {

    public static List<Either<Command, CodeAction>> compute(
            DocumentState doc, CodeActionParams params, String text) {

        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        // check if cursor is on a procedure definition
        Position cursor = params.getRange().getStart();

        for (Map.Entry<String, ?> entry : doc.symbols.getAllProcedureEntries()) {
            String procName = entry.getKey();
            ProcedureSignature sig = doc.symbols.getProcedure(procName);

            if (sig == null || sig.params().size() < 2) continue;

            // is cursor on this procedure's declaration?
            if (!containsPosition(sig.declarationRange(), cursor)) continue;

            List<String> params2 = sig.params();

            // offer one action per non-first parameter
            for (int i = 1; i < params2.size(); i++) {
                String paramToPromote = params2.get(i);
                List<String> newOrder = new ArrayList<>(params2);
                newOrder.remove(i);
                newOrder.add(0, paramToPromote);

                CodeAction action = buildReorderAction(
                    doc, text, procName, sig, params2, newOrder,
                    "Make :" + paramToPromote + " the first parameter"
                );
                actions.add(Either.forRight(action));
            }
        }

        return actions;
    }

    private static CodeAction buildReorderAction(
            DocumentState doc, String text,
            String procName, ProcedureSignature sig,
            List<String> oldOrder, List<String> newOrder,
            String title) {

        List<TextEdit> edits = new ArrayList<>();

        // 1. rewrite the procedure definition parameter list
        // find the text of the old param list and replace it
        String oldParams = oldOrder.stream()
            .map(p -> ":" + p)
            .reduce((a, b) -> a + " " + b).orElse("");
        String newParams = newOrder.stream()
            .map(p -> ":" + p)
            .reduce((a, b) -> a + " " + b).orElse("");

        // the param list starts right after the procedure name
        Range defRange = sig.declarationRange();
        // find the range of the full param list in the source
        Range paramListRange = findParamListRange(text, defRange, oldParams);
        if (paramListRange != null) {
            edits.add(new TextEdit(paramListRange, newParams));
        }

        // 2. rewrite every call site
        for (CallSite call : doc.symbols.getCallSites(procName)) {
            if (call.argRanges().size() != oldOrder.size()) continue;

            // read current arg texts
            List<String> argTexts = call.argRanges().stream()
                .map(r -> extractText(text, r))
                .toList();

            // reorder arg texts to match new param order
            // newOrder[i] was at oldOrder.indexOf(newOrder[i]) in the old order
            List<String> reorderedArgs = newOrder.stream()
                .map(param -> argTexts.get(oldOrder.indexOf(param)))
                .toList();

            // emit one edit per argument range
            for (int i = 0; i < call.argRanges().size(); i++) {
                edits.add(new TextEdit(call.argRanges().get(i), reorderedArgs.get(i)));
            }
        }

        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        workspaceEdit.setChanges(Map.of(doc.uri, edits));

        CodeAction action = new CodeAction(title);
        action.setKind(CodeActionKind.RefactorRewrite);
        action.setEdit(workspaceEdit);
        return action;
    }

    // find the range of the param list text inside the source
    private static Range findParamListRange(String text, Range afterRange, String paramList) {
        String[] lines = text.split("\n", -1);
        int line = afterRange.getStart().getLine();
        String sourceLine = lines[line];
        int idx = sourceLine.indexOf(paramList);
        if (idx == -1) return null;
        return new Range(
            new Position(line, idx),
            new Position(line, idx + paramList.length())
        );
    }

    private static String extractText(String text, Range range) {
        String[] lines = text.split("\n", -1);
        // single line args only for now
        String line = lines[range.getStart().getLine()];
        return line.substring(
            range.getStart().getCharacter(),
            range.getEnd().getCharacter()
        );
    }

    private static boolean containsPosition(Range range, Position pos) {
        return pos.getLine() >= range.getStart().getLine()
            && pos.getLine() <= range.getEnd().getLine();
    }
}
