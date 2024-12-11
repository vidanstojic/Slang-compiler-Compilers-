package org.raf.slang.ast;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.raf.slang.Slang;
import slang.parser.SlangParser;
import slang.parser.SlangVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class CSTtoASTConverter extends AbstractParseTreeVisitor<Tree> implements SlangVisitor<Tree> {
    private Slang slang;


    public CSTtoASTConverter(Slang slang) {
        this.slang = slang;
        /* Open the global scope.  */
        openBlock();
    }

    /* A stack of environments.  */
    private List<Map<String, Statement>> environments = new ArrayList<>();
    /** Open a new scope. */
    private void openBlock() {
        environments.add(new HashMap<>());
    }

    /** Removes the last scope. */
    private void closeBlock() {
        environments.removeLast();
    }

    /** Saves a declaration into the current environment, diagnosing
     redeclaration. */
    private void pushStatement(String name, Statement statement) {
        /* Intentionally overwriting the old variable as error recovery.  */
        // Ovde je potrebno da proverimo od cega je instanca statement i da ako je print i scan da to preskocimo i ne stavimo na stek
//        if (environments.getLast().containsKey(name)) {
//            slang.error(statement.getLocation(), "Variable '%s' already declared in this scope", name);
//        }
        Statement oldDecl = null;
        if(statement instanceof SimpleStatement) {
            oldDecl = environments.getLast().put(name, statement);
        }
        else if(statement instanceof FunctionDefinition){
            oldDecl = environments.getLast().put(name, statement);
        }
        else if(statement instanceof IfStatement){
            oldDecl = environments.getLast().put(name, statement);
        }
        else if(statement instanceof ElseStatement){
            oldDecl = environments.getLast().put(name, statement);
        }
        else if(statement instanceof LoopStatement){
            oldDecl = environments.getLast().put(name, statement);
        }

        if (oldDecl != null) {
            slang.error(statement.getLocation(), "");
        }
    }
    /** Tries to find a declaration in any scope parent to this one..  */
    private Optional<Statement> lookup(Location loc, String name) {
        /* Walk through the scope, starting at the top one, ... */
        for (var block : environments.reversed()) {
            /* ... for each of them, try to find the name we're looking for in
               the environment... */
            var decl = block.get(name);
            if (decl != null) {
                /* ... and if it is found, return it....  */
                return Optional.of(decl);
            }
        }
        /* ... otherwise, we fell through and found nothing.  Diagnose and
           continue.  */
        slang.error(loc, "failed to find variable '%s' in current scope", name);
        return Optional.empty();
    }

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
        Expr value = null;
        if (ctx.expr(0) != null) {
            value = (Expr) visit(ctx.expr(0));
        }
        VariableType type = null;
        if (ctx.variableType() != null) {
            type = (VariableType) visit(ctx.variableType());
        }
        var simpleStatement = new SimpleStatement(getLocation(ctx), name, value, type);

        pushStatement(name, simpleStatement);

        return simpleStatement;
    }

    @Override
    public Tree visitVariableType(SlangParser.VariableTypeContext ctx) {
        // Pretpostavljamo da ctx.getText() vraća naziv tipa kao string
        String typeName = ctx.getText();
        return new VariableType(getLocation(ctx), typeName);
    }


    @Override
    public Tree visitIfStatement(SlangParser.IfStatementContext ctx) {
        openBlock();

        var exprList = ctx.expr()
                .stream()
                .map(this::visit)
                .map(x -> (Expr) x)
                .toList();

        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();

        var ifStatement = new IfStatement(getLocation(ctx), exprList, statementList);


        closeBlock();
        return ifStatement;
    }
    @Override
    public Tree visitElseStatement(SlangParser.ElseStatementContext ctx) {
        openBlock();
       // ctx.statement().forEach(this::visit);
        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();


        var elseStatement = new ElseStatement(getLocation(ctx), statementList);

        closeBlock();
        return elseStatement;
    }

    @Override
    public Tree visitLoopStatement(SlangParser.LoopStatementContext ctx) {
        openBlock();
        var exprList = ctx.expr()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (Expr) x)
                /* ... and put them into a list.  */
                .toList();

        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();

        var loopStatement = new LoopStatement(getLocation(ctx), exprList, statementList);
        closeBlock();
        return loopStatement;
    }

    @Override
    public Tree visitFunctionDefinition(SlangParser.FunctionDefinitionContext ctx) {
        openBlock();
        var name = ctx.ID().getText();
        var parameterList = ctx.functionParameter()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (FunctionParameter) x)
                /* ... and put them into a list.  */
                .toList();
        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();
        var function = new FunctionDefinition(getLocation(ctx), name, parameterList, statementList);

        closeBlock();
        return function;
    }

    @Override
    public Tree visitFunctionParameter(SlangParser.FunctionParameterContext ctx) {
        // Dobijanje imena parametra
        String paramName = ctx.ID().getText();
        System.out.println("ULAAAZI ODJEEEE");
        // Dobijanje tipa ako postoji
        VariableType type = null;
        if (ctx.variableType() != null) {
            type = (VariableType) visit(ctx.variableType());
        }

        return new FunctionParameter(getLocation(ctx), paramName, type);
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
        var subexpr = (Expr) visit(ctx.getChild(0));
        Expr.Operation exprOp = null;
        if (ctx.getParent().children.toString().contains("!")) {
            exprOp = Expr.Operation.BANG;
            var loc = subexpr.getLocation().span(subexpr.getLocation());
            return new Expr(loc, subexpr, exprOp);
        }
        var left = (Expr) visit(ctx.relationalOperands());


        var loc = left.getLocation().span(subexpr.getLocation());
        return subexpr;
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
        if(Objects.equals(operatorText, ">"))
            exprOp = Expr.Operation.GREATERTHAN;
        else if(Objects.equals(operatorText, "<"))
            exprOp = Expr.Operation.LESSTHAN;
        else if (Objects.equals(operatorText, ">="))
            exprOp = Expr.Operation.GREATERTHANOREQ;
        else if(Objects.equals(operatorText, "<="))
            exprOp = Expr.Operation.LESSTHANOREQ;
        else if(Objects.equals(operatorText, "=="))
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
        }else {
            var loc = getLocation(ctx);

            String variableName = ctx.ID().getText();
            return lookup(loc, variableName)
                    .map(simpleStatement -> {
                        if (simpleStatement instanceof SimpleStatement variable) {
                            if (!variable.hasType()) {
                                slang.error(variable.getLocation(), "variable does not have a type");
                            }
                            return (Tree) new VariableRef(loc, variable);
                        }
                        throw new RuntimeException("Invalid reference to non-simple statement: " + variableName);
                    })
                    .orElseGet(() -> new ErrorExpr(loc));




        }
    }

    @Override
    public Tree visitBlock(SlangParser.BlockContext ctx) {
        openBlock();

        var stmts = ctx.statement()
                /* Take all the parsed statements, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to statements (because 'start: statement*',
                   so they can't be anything else), ...  */
                .map(x -> (Statement) x)
                /* ... and put them into a list.  */
                .collect(Collectors.toCollection(ArrayList::new));

        /* Close the one opened above.  */
        closeBlock();
        return new StatementList(getLocation(ctx), stmts);
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
