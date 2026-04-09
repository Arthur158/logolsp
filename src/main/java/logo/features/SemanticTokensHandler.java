package logo.features;

import logo.analysis.DocumentState;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.SemanticTokens;
import logo.parser.LogoLexer;

import java.util.ArrayList;
import java.util.List;

public class SemanticTokensHandler {

    // Token type index 0 = "keyword" (declared in the legend)
    private static final int TYPE_KEYWORD = 0;

    private static int getTokenType(Token token) {
        int type = token.getType();

        // These constants come from your generated LogoLexer
        if (type == LogoLexer.TO || type == LogoLexer.END ||
            type == LogoLexer.REPEAT || type == LogoLexer.IF ||
            type == LogoLexer.WHILE || type == LogoLexer.FOREVER ||
            type == LogoLexer.MAKE || type == LogoLexer.OUTPUT ||
            type == LogoLexer.STOP || type == LogoLexer.FOR) {
            return 0; // keyword
        }
        if (type == LogoLexer.NUMBER) {
            return 3; // number
        }
        if (type == LogoLexer.QUOTED_WORD) {
            return 4; // string
        }
        if (type == LogoLexer.IDENT) {
            return 1; // function — procedure name / built-in command
        }
        // COLON IDENT = variable, but COLON is a separate token
        // so check if the previous token was COLON
        return 0; // default to keyword
    }

    public static SemanticTokens computeTokens(DocumentState doc) {
        List<Integer> data = new ArrayList<>();

        int prevLine = 0;
        int prevCol  = 0;

        // getAllTokens() requires the stream to be filled first
        doc.tokens.fill();

        Token prev = null;
        for (Token token : doc.tokens.getTokens()) {
            if (token.getType() == Token.EOF) continue;

            int line   = token.getLine() - 1;
            int col    = token.getCharPositionInLine();
            int length = token.getText().length();

            int deltaLine = line - prevLine;
            int deltaCol  = (deltaLine == 0) ? col - prevCol : col;

            int tokenType;
            if (prev != null && prev.getType() == LogoLexer.COLON) {
                tokenType = 2; // variable
            } else {
                tokenType = getTokenType(token);
            }

            data.add(deltaLine);
            data.add(deltaCol);
            data.add(length);
            data.add(tokenType); // was hardcoded TYPE_KEYWORD before
            data.add(0);         // modifiers

            prevLine = line;
            prevCol  = col;
            prev = token;
        }

        return new SemanticTokens(data);
    }
}
