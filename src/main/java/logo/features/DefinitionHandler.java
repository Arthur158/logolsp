package logo.features;

import logo.analysis.DocumentState;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class DefinitionHandler {

    public static Location findDefinition(DocumentState doc, Position cursor) {
        doc.tokens.fill();

        Token target    = null;
        Token prevToken = null;

        for (Token token : doc.tokens.getTokens()) {
            if (contains(token, cursor)) {
                target = token;
                break;
            }
            prevToken = token;
        }

        if (target == null) return null;

        String name = target.getText();
        Range decl  = null;

        // check if it's a variable reference (:name)
        if (prevToken != null && prevToken.getText().equals(":")) {
            decl = doc.symbols.findVariable(name);
        } else {
            // try procedure first, then variable (for "name in MAKE)
            decl = doc.symbols.findProcedure(name);
            if (decl == null) {
                // strip leading " for quoted words
                decl = doc.symbols.findVariable(name.startsWith("\"")
                    ? name.substring(1) : name);
            }
        }

        if (decl == null) return null;
        return new Location(doc.uri, decl);
    }

    private static boolean contains(Token token, Position cursor) {
        int line = token.getLine() - 1;
        int col  = token.getCharPositionInLine();
        int end  = col + token.getText().length();
        return line == cursor.getLine()
            && col <= cursor.getCharacter()
            && cursor.getCharacter() <= end;
    }
}
