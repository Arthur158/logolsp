package logo.analysis;

import logo.parser.LogoBaseVisitor;
import logo.parser.LogoParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.*;


public class SymbolTableBuilder extends LogoBaseVisitor<Void> {

    private final SymbolTable symbols;
    private String currentScope = null; // null = global

    public SymbolTableBuilder(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public Void visitProcedureDef(LogoParser.ProcedureDefContext ctx) {
        Range funcRange = new Range(
                new Position(ctx.statement().getFirst().getStart().getLine(),ctx.statement().getFirst().getStart().getCharPositionInLine()),
                new Position(ctx.statement().getLast().getStop().getLine(),ctx.statement().getLast().getStop().getCharPositionInLine())
        );
        String name = ctx.name.getText();

        // collect parameter names in order
        List<String> params = ctx.param().stream()
            .map(p -> p.IDENT().getText())
            .toList();

        symbols.addProcedure(name, toRange(ctx.name), params, funcRange);

        String prev = currentScope;
        currentScope = name.toLowerCase();
        visitChildren(ctx);
        currentScope = prev;
        return null;
    }

    @Override
    public Void visitProcedureCall(LogoParser.ProcedureCallContext ctx) {
        String name = ctx.name.getText();
        symbols.addProcedureRef(name, toRange(ctx.name), currentScope, ctx.getStart().getLine());

        // collect argument ranges
        List<Range> argRanges = ctx.expr().stream()
            .map(e -> new Range(
                new Position(e.getStart().getLine() - 1,
                             e.getStart().getCharPositionInLine()),
                new Position(e.getStop().getLine() - 1,
                             e.getStop().getCharPositionInLine()
                             + e.getStop().getText().length())
            ))
            .toList();

        // full range of the call — from proc name to last arg
        Range fullRange = toRange(ctx.name);
        if (!argRanges.isEmpty()) {
            fullRange = new Range(fullRange.getStart(),
                                  argRanges.get(argRanges.size() - 1).getEnd());
        }

        symbols.addCallSite(name, fullRange, argRanges);
        return visitChildren(ctx);
    }

    @Override
    public Void visitParam(LogoParser.ParamContext ctx) {
        String name = ctx.IDENT().getText();
        symbols.addVariable(name, toRange(ctx.IDENT().getSymbol()), currentScope, ctx.getStart().getLine());
        return visitChildren(ctx);
    }

    @Override
    public Void visitMakeStmt(LogoParser.MakeStmtContext ctx) {
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()), currentScope, ctx.getStart().getLine());
        return visitChildren(ctx);
    }

    @Override
    public Void visitLocalMakeStmt(LogoParser.LocalMakeStmtContext ctx) {
        String name = ctx.QUOTED_WORD().getText().substring(1);
        symbols.addVariable(name, toRange(ctx.QUOTED_WORD().getSymbol()), currentScope, ctx.getStart().getLine());
        return visitChildren(ctx);
    }


    @Override
    public Void visitVariable(LogoParser.VariableContext ctx) {
        String name = ctx.IDENT().getText();
        symbols.addVariableRef(name, toRange(ctx.IDENT().getSymbol()), currentScope, ctx.getStart().getLine());
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
