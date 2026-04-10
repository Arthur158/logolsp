package logo.analysis;

import logo.parser.LogoBaseVisitor;
import logo.parser.LogoParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class SymbolTableBuilder extends LogoBaseVisitor<Void> {

    private final SymbolTable symbols;
    private String currentScope = null; // null = global
    private int counter = 0;           // global ordering counter

    public SymbolTableBuilder(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public Void visitProcedureDef(LogoParser.ProcedureDefContext ctx) {
        String name = ctx.name.getText();
        symbols.addProcedure(name, toRange(ctx.name));
        String prev = currentScope;
        currentScope = name.toLowerCase();
        visitChildren(ctx);
        currentScope = prev;
        return null;
    }

    @Override
    public Void visitParam(LogoParser.ParamContext ctx) {
        String name = ctx.IDENT().getText();
        symbols.addVariable(name, toRange(ctx.IDENT().getSymbol()), currentScope, counter++);
        return visitChildren(ctx);
    }

    @Override
    public Void visitMakeStmt(LogoParser.MakeStmtContext ctx) {
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()), currentScope, counter++);
        return visitChildren(ctx);
    }

    @Override
    public Void visitLocalMakeStmt(LogoParser.LocalMakeStmtContext ctx) {
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()), currentScope, counter++);
        return visitChildren(ctx);
    }

    @Override
    public Void visitProcedureCall(LogoParser.ProcedureCallContext ctx) {
        symbols.addProcedureRef(ctx.name.getText(), toRange(ctx.name), currentScope, counter++);
        return visitChildren(ctx);
    }

    @Override
    public Void visitVariable(LogoParser.VariableContext ctx) {
        String name = ctx.IDENT().getText();
        symbols.addVariableRef(name, toRange(ctx.IDENT().getSymbol()), currentScope, counter++);
        return visitChildren(ctx);
    }

    private Range toRange(Token token) {
        int line = token.getLine() - 1;
        int col  = token.getCharPositionInLine();
        return new Range(
            new Position(line, col),
            new Position(line, col + token.getText().length())
        );
    }
}
