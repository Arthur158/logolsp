package logo.analysis;

import logo.parser.LogoBaseVisitor;
import logo.parser.LogoParser;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class SymbolTableBuilder extends LogoBaseVisitor<Void> {

    private final SymbolTable symbols;

    public SymbolTableBuilder(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public Void visitProcedureDef(LogoParser.ProcedureDefContext ctx) {
        String name = ctx.name.getText();
        symbols.addProcedure(name, toRange(ctx.name));
        return visitChildren(ctx);
    }

    @Override
    public Void visitProcedureCall(LogoParser.ProcedureCallContext ctx) {
        String name = ctx.name.getText();
        symbols.addProcedureRef(name, toRange(ctx.name));
        return visitChildren(ctx);
    }

    @Override
    public Void visitVariable(LogoParser.VariableContext ctx) {
        String name = ctx.IDENT().getText();
        symbols.addVariableRef(name, toRange(ctx.IDENT().getSymbol()));
        return visitChildren(ctx);
    }

    @Override
    public Void visitMakeStmt(LogoParser.MakeStmtContext ctx) {
        // MAKE "varname — strip the leading "
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()));
        return visitChildren(ctx);
    }

    @Override
    public Void visitLocalMakeStmt(LogoParser.LocalMakeStmtContext ctx) {
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()));
        return visitChildren(ctx);
    }

    @Override
    public Void visitParam(LogoParser.ParamContext ctx) {
        // procedure parameters like :size are also declarations
        String name = ctx.IDENT().getText();
        symbols.addVariable(name, toRange(ctx.IDENT().getSymbol()));
        return visitChildren(ctx);
    }

    private Range toRange(Token token) {
        int line = token.getLine() - 1; // LSP is 0-indexed
        int col  = token.getCharPositionInLine();
        int len  = token.getText().length();
        return new Range(
            new Position(line, col),
            new Position(line, col + len)
        );
    }
}
