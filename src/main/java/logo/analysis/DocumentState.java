package logo.analysis;

import logo.parser.LogoLexer;
import logo.parser.LogoParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import java.util.ArrayList;
import java.util.List;

public class DocumentState {

    public final String uri;
    public final CommonTokenStream tokens;
    public final LogoParser.ProgramContext tree;
    public final SymbolTable symbols;
    public final String text;

    public final List<Diagnostic> parseErrors;

    private DocumentState(String uri, CommonTokenStream tokens,
                          LogoParser.ProgramContext tree,
                          SymbolTable symbols,
                          List<Diagnostic> parseErrors,
                          String text) {
        this.uri         = uri;
        this.tokens      = tokens;
        this.tree        = tree;
        this.symbols     = symbols;
        this.parseErrors = parseErrors;
        this.text        = text;
    }

    public static DocumentState parse(String uri, String text) {
        CharStream input         = CharStreams.fromString(text);
        LogoLexer lexer          = new LogoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LogoParser parser        = new LogoParser(tokens);

        // remove default console error listener
        parser.removeErrorListeners();
        lexer.removeErrorListeners();

        // collect parse errors as diagnostics
        List<Diagnostic> parseErrors = new ArrayList<>();
        ANTLRErrorListener errorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                Range range = new Range(
                    new Position(line - 1, charPositionInLine),
                    new Position(line - 1, charPositionInLine + 1)
                );
                parseErrors.add(new Diagnostic(
                    range,
                    "Syntax error: " + msg,
                    DiagnosticSeverity.Error,
                    "logo-lsp"
                ));
            }
        };

        parser.addErrorListener(errorListener);
        lexer.addErrorListener(errorListener);

        LogoParser.ProgramContext tree = parser.program();

        SymbolTable symbols = new SymbolTable();
        new SymbolTableBuilder(symbols).visit(tree);

        return new DocumentState(uri, tokens, tree, symbols, parseErrors, text);
    }

    public String printTree() {
        return tree.toStringTree(new LogoParser(null));
    }

    public static void log(String message) {
    try {
        java.nio.file.Files.writeString(
            java.nio.file.Path.of("/tmp/logolsp-debug.log"),
            message + "\n",
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND
        );
    } catch (Exception e) {
        // ignore
    }
}
}
