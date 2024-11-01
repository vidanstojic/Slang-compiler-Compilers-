package org.raf.slang.ast;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import slang.parser.SlangParser;
import slang.parser.SlangVisitor;

public class CSTtoASTConverter extends AbstractParseTreeVisitor<Tree> implements SlangVisitor<Tree> {
    @Override
    public Tree visitStart(SlangParser.StartContext ctx) {
        var stmts = ctx.statement()
                /* Take all the parsed statements, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to statements (because 'start: statement*',
                   so they can't be anything else), ...  */
                .map(x -> (Statement) x)
                /* ... and put them into a list.  */
                .toList();
        return new StatementList(getLocation(ctx), stmts);
    }

    @Override
    public Tree visitStatement(SlangParser.StatementContext ctx) {
         /* A statement just contains a child.  Visit it instead.  It has to be
           a statement, so we check for that by casting.

           Note that we assume here that statement is defined as an OR of many
           different rules, with its first child being whatever the statement
           actually is, and the rest, if any, are unimportant.  */
        var substatement = visit(ctx.getChild(0));

        return (Statement) substatement;
    }

    @Override
    public Tree visitSimpleStatement(SlangParser.SimpleStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitIfStatement(SlangParser.IfStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitElseStatement(SlangParser.ElseStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitLoopStatement(SlangParser.LoopStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitFunctionDefinition(SlangParser.FunctionDefinitionContext ctx) {
        return null;
    }

    @Override
    public Tree visitFunctionCallStatement(SlangParser.FunctionCallStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitFunctionArgumentList(SlangParser.FunctionArgumentListContext ctx) {
        return null;
    }

    @Override
    public Tree visitPrintStatement(SlangParser.PrintStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitScanStatement(SlangParser.ScanStatementContext ctx) {
        return null;
    }

    @Override
    public Tree visitExpr(SlangParser.ExprContext ctx) {
        return null;
    }

    @Override
    public Tree visitRelationalOperands(SlangParser.RelationalOperandsContext ctx) {
        return null;
    }

    @Override
    public Tree visitAddSubOperands(SlangParser.AddSubOperandsContext ctx) {
        return null;
    }

    @Override
    public Tree visitMulDivOperands(SlangParser.MulDivOperandsContext ctx) {
        return null;
    }

    @Override
    public Tree visitCore(SlangParser.CoreContext ctx) {
        return null;
    }
    /// FUNKCIJE ZA LOKACIJU


    /** Returns the range that this subtree is in.  */
    private static Location getLocation(ParserRuleContext context) {
        return getLocation(context.getStart())
                .span(getLocation(context.getStop ()));
    }

    /** Returns the location this terminal is in.  */
    private static Location getLocation(TerminalNode term) {
        return getLocation(term.getSymbol());
    }

    /** Returns the location this token is in.  */
    private static Location getLocation(Token token) {
        /* The token starts at the position ANTLR provides us.  */
        var start = new Position(token.getLine(), token.getCharPositionInLine());

        /* But it does not provide a convenient way to get where it ends, so we
           have to calculate it based on length.  */
        assert !token.getText ().contains ("\n")
                : "CSTtoASTConverter assumes single-line tokens";
        var length = token.getText ().length ();
        assert length > 0;

        /* And then put it together.  */
        var end = new Position (start.line (), start.column () + length - 1);
        return new Location (start, end);
    }
}
