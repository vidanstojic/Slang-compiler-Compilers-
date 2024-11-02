package org.raf.slang.ast;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import slang.parser.SlangLexer;
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
        var name = ctx.ID().getText();
        var value = (Expr) visit(ctx.expr(0));
        return new SimpleStatement(getLocation(ctx), name, value);
    }

    @Override
    public Tree visitIfStatement(SlangParser.IfStatementContext ctx) {
        var exprList = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();
        return new IfStatement(getLocation(ctx), exprList);
    }

    @Override
    public Tree visitElseStatement(SlangParser.ElseStatementContext ctx) {
        ctx.statement().forEach(this::visit);//pokusati sa listom statmenta ako ovo ne radi
        return new ElseStatement(getLocation(ctx));
    }

    @Override
    public Tree visitLoopStatement(SlangParser.LoopStatementContext ctx) {
        var exprList = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();
        return new LoopStatement(getLocation(ctx), exprList);
    }

    @Override
    public Tree visitFunctionDefinition(SlangParser.FunctionDefinitionContext ctx) {
        var name = ctx.ID().getText();
        var exprList = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();
        return new FunctionDefinition(getLocation(ctx), name, exprList);
    }

    @Override
    public Tree visitFunctionCallStatement(SlangParser.FunctionCallStatementContext ctx) {
        var name = ctx.ID().getText();
        var exprList = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();
        return new FunctionCallStatement(getLocation(ctx), name,exprList);
    }

    @Override
    public Tree visitPrintStatement(SlangParser.PrintStatementContext ctx) {
        var arguments = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();
        return new PrintStatement(getLocation(ctx), arguments);
    }

    @Override
    public Tree visitScanStatement(SlangParser.ScanStatementContext ctx) {
        var arguments = ctx.ID().getText();
        return new ScanStatement(getLocation(ctx), arguments);
    }

    @Override
    public Tree visitExpr(SlangParser.ExprContext ctx) {
        var subexpr = visit(ctx.getChild(0));

        return (Expr) subexpr;
    }

    @Override
    public Tree visitRelationalOperands(SlangParser.RelationalOperandsContext ctx) {
        if (ctx.relationalOperands() == null) {
            return visit(ctx.addSubOperands());
        }

        var left = (Expr) visit(ctx.relationalOperands());
        var right = (Expr) visit(ctx.addSubOperands());

        var operatorText = ctx.getChild(1).getText();
        Expr.Operation exprOp;
        if(operatorText == ">")
            exprOp = Expr.Operation.GREATERTHAN;
        else if(operatorText == "<")
            exprOp = Expr.Operation.LESSTHAN;
        else if (operatorText == ">=")
            exprOp = Expr.Operation.GREATERTHANOREQ;
        else if(operatorText == "<=")
            exprOp = Expr.Operation.LESSTHANOREQ;
        else if(operatorText == "==")
            exprOp = Expr.Operation.EQUALTO;
        else
            throw new IllegalArgumentException("Nepoznati operator: " + operatorText);

        var loc = left.getLocation().span(right.getLocation());
        return new Expr(loc, exprOp, left, right);
    }

    @Override
    public Tree visitAddSubOperands(SlangParser.AddSubOperandsContext ctx) {
        if (ctx.addSubOperands() == null) {
            return visit(ctx.mulDivOperands());
        }

        var left = (Expr) visit(ctx.addSubOperands());
        var right = (Expr) visit(ctx.mulDivOperands());

        var operatorText = ctx.getChild(1).getText();
        Expr.Operation exprOp;
        if(operatorText == "+")
            exprOp = Expr.Operation.ADD;
        else if(operatorText == "-")
            exprOp = Expr.Operation.SUB;
        else
            throw new IllegalArgumentException("Nepoznati operator: " + operatorText);

        var loc = left.getLocation().span(right.getLocation());
        return new Expr(loc, exprOp, left, right);
    }

    @Override
    public Tree visitMulDivOperands(SlangParser.MulDivOperandsContext ctx) {
        if (ctx.mulDivOperands() == null) {
            return visit(ctx.core());
        }

        var left = (Expr) visit(ctx.mulDivOperands());
        var right = (Expr) visit(ctx.core());

        var operatorText = ctx.getChild(1).getText();
        Expr.Operation exprOp;
        if(operatorText == "*")
            exprOp = Expr.Operation.MUL;
        else if(operatorText == "/")
            exprOp = Expr.Operation.DIV;
        else
            throw new IllegalArgumentException("Nepoznati operator: " + operatorText);

        var loc = left.getLocation().span(right.getLocation());
        return new Expr(loc, exprOp, left, right);
    }

    @Override
    public Tree visitCore(SlangParser.CoreContext ctx) {
        if(ctx.NUMBER_LITERAL()!=null && ctx.getText().equals(ctx.NUMBER_LITERAL().toString())){
            return new NumberLiteral(getLocation(ctx), Double.parseDouble(ctx.getText()));
        }else if(ctx.BOOLEAN_LITERAL()!=null && ctx.getText().equals(ctx.BOOLEAN_LITERAL().toString())){
            return new BoolLiteral(getLocation(ctx), Boolean.parseBoolean(ctx.getText()));
        }else{
            return new VariableRef(getLocation(ctx), ctx.getText());
        }
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
