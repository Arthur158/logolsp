package logo.analysis;

import logo.parser.LogoLexer;
import logo.parser.LogoParser;
import org.antlr.v4.runtime.*;

public class DocumentState {

    public final String uri;
    public final CommonTokenStream tokens;
    public final LogoParser.ProgramContext tree;
    public final SymbolTable symbols;

    private DocumentState(String uri, CommonTokenStream tokens,
                          LogoParser.ProgramContext tree, SymbolTable symbols) {
        this.uri     = uri;
        this.tokens  = tokens;
        this.tree    = tree;
        this.symbols = symbols;
    }

    public static DocumentState parse(String uri, String text) {
        CharStream input         = CharStreams.fromString(text);
        LogoLexer lexer          = new LogoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LogoParser parser        = new LogoParser(tokens);
        LogoParser.ProgramContext tree = parser.program();

        // build symbol table
        SymbolTable symbols = new SymbolTable();
        new SymbolTableBuilder(symbols).visit(tree);

        return new DocumentState(uri, tokens, tree, symbols);
    }
}
