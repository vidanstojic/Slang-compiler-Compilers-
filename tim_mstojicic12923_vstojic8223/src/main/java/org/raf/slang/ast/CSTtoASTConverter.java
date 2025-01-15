package org.raf.slang.ast;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.raf.slang.Slang;
import slang.parser.SlangLexer;
import slang.parser.SlangParser;
import slang.parser.SlangVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class CSTtoASTConverter extends AbstractParseTreeVisitor<Tree> implements SlangVisitor<Tree> {
    private Slang slang;


    public CSTtoASTConverter(Slang slang) {
        this.slang = slang;
        /* Open the global scope.  */
        //openBlock();
    }

    /* A stack of environments.  */
    private List<Map<String, Statement>> environments = new ArrayList<>();

    private List<FunctionDefinition> functionDefinitions = new ArrayList<>();
    private List<ArrayStatement> arrays = new ArrayList<>();

    /** Open a new scope. */
    private void openBlock() {
        environments.add(new HashMap<>());
    }

    /** Removes the last scope. */
    private void closeBlock() {
        for(Statement statement : environments.getLast().values()){
            if(statement instanceof ArrayStatement){
                ArrayStatement array = arrayDefined(((ArrayStatement) statement).getName());
                if(array != null) arrays.remove(array);
            }
        }
        environments.removeLast();
    }
    private boolean isNumber(String str) {
        try {
            Double.parseDouble(str); // Za decimalne brojeve
            return true;
        } catch (NumberFormatException e) {
            return false; // Nije broj
        }
    }
    public void firstOpen(){
        openBlock();
    }
    public void lastClose(){
        for(Statement statement : environments.getLast().values()){
            if(statement instanceof FunctionDefinition){
                FunctionDefinition function = functionDefined(((FunctionDefinition) statement).getName());
                if(function != null) functionDefinitions.remove(function);
            }
        }
        closeBlock();
    }

    private FunctionDefinition functionDefined(String functionName){
        for(FunctionDefinition functionFromList : functionDefinitions){
            if(functionFromList.getName().equals(functionName))
                return functionFromList;
        }
        return null;
    }

    private ArrayStatement arrayDefined(String arrayName){
        for(ArrayStatement arrayFromList : arrays){
            if(arrayFromList.getName().equals(arrayName))
                return arrayFromList;
        }
        return null;
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
        else if(statement instanceof ArrayStatement){
            oldDecl = environments.getLast().put(name, statement);
        }

        if (oldDecl != null) {
            slang.error(statement.getLocation(), "variable with this id is already defined");
        }
    }
    /**Prolazi kroz envirionments i trazi da li ima neki simpleStatement sa istim imenom*/
    public SimpleStatement findSimpleStatement(String name) {
        for (Map<String, Statement> environment : environments) {
            for (Map.Entry<String, Statement> entry : environment.entrySet()) {
                Statement statement = entry.getValue();
                if (statement instanceof SimpleStatement) {
                    SimpleStatement simpleStatement = (SimpleStatement) statement;
                    if (simpleStatement.getName().equals(name)) {
                        return simpleStatement;
                    }
                }
            }
        }
        return null;
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
        if (ctx.expr() != null) {// vratiti 0 u expr ako je potrebno
            value = (Expr) visit(ctx.expr());
        }
        VariableType type = null;
        if (ctx.variableType() != null) {
            type = (VariableType) visit(ctx.variableType());
        }
        var simpleStatement = new SimpleStatement(getLocation(ctx), name, value, type);
        if(type == null) {
            SimpleStatement foundSimpleStatement = findSimpleStatement(simpleStatement.getName());
            if(foundSimpleStatement == null) {
                slang.error(simpleStatement.getLocation(), "variable "+ simpleStatement.getName() + " does not have a type");
            }
            else{
                //moze da se ispravi da se ne azurira vrednost
                foundSimpleStatement.setValue(simpleStatement.getValue());
                simpleStatement = foundSimpleStatement;
            }
        }
        else
            pushStatement(name, simpleStatement);

        return simpleStatement;
    }

    @Override
    public Tree visitVariableType(SlangParser.VariableTypeContext ctx) {
        // Pretpostavljamo da ctx.getText() vraća naziv tipa kao string
        String typeName = ctx.getText();
        if(typeName.equals("empty")){
            return new VoidType(getLocation(ctx), typeName);
        }
        else if(typeName.equals("numero")){
            return new NumberType(getLocation(ctx), typeName);
        }
        else if(typeName.equals("yeahNah")){
            return new BoolType(getLocation(ctx), typeName);
        }
        else{
            slang.error(getLocation(ctx), "type is not valid");
            return new ErrorExpr(getLocation(ctx));
        }

    }

    @Override
    public Tree visitArray(SlangParser.ArrayContext ctx) {
        var name = ctx.ID().getText();
        List<Expr> elementsOfArray = new ArrayList<>();
        int i = 0;
        while(ctx.expr(i) != null) {
            Expr value = (Expr) visit(ctx.expr(i));
            elementsOfArray.add(value);
            i++;
        }
        ArrayLiteral arrayLiteral = new ArrayLiteral(getLocation(ctx), elementsOfArray);
        VariableType type = null;
        if (ctx.variableType() != null) {
            type = (VariableType) visit(ctx.variableType());
        }
        arrayLiteral.setTypeOfArray(type);
        int sizeOfArray = -1;
        if(ctx.NUMBER_LITERAL() != null){
            if(isNumber(ctx.NUMBER_LITERAL().getText())){
                sizeOfArray = Integer.parseInt(ctx.NUMBER_LITERAL().getText());
            }
        }
        ArrayStatement array = new ArrayStatement(getLocation(ctx), name, arrayLiteral, type);
        if(sizeOfArray != -1){
            array.setSizeOfArray(sizeOfArray);
            arrayLiteral.setSizeOfArray(sizeOfArray);
        }
        else{
            array.setSizeOfArray(array.getElements().getElements().size());
            arrayLiteral.setSizeOfArray(array.getElements().getElements().size());
        }

        if(sizeOfArray > array.getElements().getElements().size()){
            slang.error(getLocation(ctx), "the array size must match the specified number of elements.");
        }
        this.arrays.add(array);
        pushStatement(name, array);
        return array;
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
        Expr value = null;
        SimpleStatement iterator = null;
        if (ctx.expr(0) != null) {
            value = (Expr) visit(ctx.expr(0));
        }
      //  VariableType type = new VariableType(getLocation(ctx), "null");
        VariableType type = null;
        if (ctx.children.toString().contains("numero") || ctx.children.toString().contains("yeahNah")){
            var name = ctx.ID().getFirst().getText();
            type = new NumberType(getLocation(ctx), ctx.children.get(2).toString());
            if (value instanceof NumberLiteral)
                type = new NumberType(getLocation(ctx), slang.getNumberType().getTypeName());
            else if (value instanceof BoolLiteral)
                type = new BoolType(getLocation(ctx), slang.getBoolType().getTypeName());
            iterator = new SimpleStatement(getLocation(ctx), name, value, type);
            pushStatement(name, iterator);
        }
        var exprList = ctx.expr()
                .stream()
                .map(this::visit)
                .map(x -> (Expr) x)
                .toList();

        /*if (ctx.children.get(2).toString().equals("numero")){
            lookup(getLocation(ctx), ctx.children.get(7).toString());
        }else if (ctx.children.toString().contains("replay"))
            lookup(getLocation(ctx), ctx.children.get(2).toString());
        else
            lookup(getLocation(ctx), ctx.children.get(3).toString());
*/
        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();

        var loopStatement = new LoopStatement(getLocation(ctx), exprList, statementList, iterator);
        closeBlock();
        return loopStatement;
    }

    @Override
    public Tree visitFunctionDefinition(SlangParser.FunctionDefinitionContext ctx) {
        openBlock();
        var name = ctx.ID().getFirst().getText();
        var parameterList = ctx.functionParameter()
                /* Take all the parsed arguments, ... */
                .stream()
                /* ... visit them using this visitor, ... */
                .map(this::visit)
                /* ... then cast them to expressions, ...  */
                .map(x -> (FunctionParameter) x)
                /* ... and put them into a list.  */
                .toList();
        // Dodavanje parametara u trenutni opseg
        for (var param : parameterList) {
            var paramName = param.getParamName();
            var paramStatement = new SimpleStatement(
                    param.getLocation(), paramName, null, param.getType()
            );
            pushStatement(paramName, paramStatement);
        }
        var statementList = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x)
                .toList();

        var dataForReturn = ctx.ID().getLast().getText();
        SimpleStatement foundSimpleStatement = findSimpleStatement(dataForReturn);
        Expr savedValueOfReturnedData = null;
        if(dataForReturn != null && (dataForReturn != "true" && dataForReturn != "false" && !isNumber(dataForReturn))  && !dataForReturn.equals(name)   ){
            if(foundSimpleStatement == null){
                slang.error(getLocation(ctx), "returned variable doesn not exist");
            }
            else{
                savedValueOfReturnedData = findSimpleStatement(dataForReturn).getValue();
            }

        }

        var functionReturnType = ctx.variableType();
        String textReturnType = "";
        if(functionReturnType == null){
            var emptyReturn = ctx.VOID_KEYWORD().getFirst();
            if(emptyReturn == null){
                slang.error(getLocation(ctx), "function does not have return type");
            }
            else textReturnType = emptyReturn.getText();
        }
        else{
            textReturnType = functionReturnType.getText();
        }
        var function = new FunctionDefinition(getLocation(ctx), name,textReturnType ,parameterList, statementList);
        if(savedValueOfReturnedData != null)function.setValueOfReturnData(savedValueOfReturnedData);
        if(foundSimpleStatement != null) function.setTypeOfReturnData(foundSimpleStatement.getType().getTypeName());
        else if(ctx.NUMBER_LITERAL() != null){
            if(isNumber(ctx.NUMBER_LITERAL().getText())){
                function.setTypeOfReturnData("numero");
            }
        }
        else if(ctx.BOOLEAN_LITERAL() != null){
            function.setTypeOfReturnData("yeahNah");
        }
        else if(ctx.VOID_KEYWORD() != null){
            function.setTypeOfReturnData("empty");
        }
        if(functionDefined(function.getName()) != null)
            slang.error(getLocation(ctx), "function with this id already exist");
        else{
            functionDefinitions.add(function);
           // pushStatement(name, function);
            environments.getFirst().put(name, function);
        }
        closeBlock();
        return function;
    }


    @Override
    public Tree visitFunctionParameter(SlangParser.FunctionParameterContext ctx) {
        // Dobijanje imena parametra
        String paramName = ctx.ID().getText();
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

        var functionCall = new FunctionCallStatement(getLocation(ctx), name,exprList);
        if(functionDefined(name) == null)
            slang.error(getLocation(ctx), "function with this id does not exist");
        else{
            // provera da li je poslat odgovarajuci broj argumenata(ovde samo proveravamo broj, a u tipizaciji cemo proveriti da li su dobri tipovi argumenata)
            boolean flagForArgSize = false;
            for(FunctionDefinition functionDefinition : functionDefinitions){
                if(functionDefinition.getName().equals(name)){
                    if(functionDefinition.getParameters().size() != functionCall.getArguments().size()){
                        slang.error(getLocation(ctx), "function does not have correct number of arguments");
                        flagForArgSize = true;
                    }
                    functionCall.setDefinition(functionDefinition);
                }
            }
            if(!flagForArgSize){
                int i = 0;
                for(FunctionParameter functionParameterEl : functionCall.getDefinition().getParameters()){
                    functionParameterEl.setValueOfParameter(functionCall.getArguments().get(i));
                    i++;
                }
            }
        }

        return functionCall;
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
        var arguments = ctx.expr().getText();
        lookup(getLocation(ctx), arguments);
        ScanStatement scanStatement = new ScanStatement(getLocation(ctx), arguments);
        SimpleStatement foundSimpleStatement = findSimpleStatement(arguments);
        if(foundSimpleStatement != null)  scanStatement.setVariableType(foundSimpleStatement.getType());
        return new ScanStatement(getLocation(ctx), arguments);
    }

//    @Override
//    public Tree visitExpr(SlangParser.ExprContext ctx) {
//        var subexpr = (Expr) visit(ctx.getChild(0));
//        Expr.Operation exprOp = null;
//        if (ctx.getParent().children.toString().contains("!")) {
//            exprOp = Expr.Operation.BANG;
//            var loc = subexpr.getLocation().span(subexpr.getLocation());
//            //subexpr.setOperation(exprOp);
//            return new Expr(loc, subexpr, exprOp);
//        }
//
//        if (exprOp == null){
//
//            return subexpr;
//        }else {
//            var left = (Expr) visit(ctx.relationalOperands());
//
//            var loc = left.getLocation().span(subexpr.getLocation());
//        }
//        return subexpr;
//    }

    @Override
    public Tree visitExpr(SlangParser.ExprContext ctx) {
        return (Expr) visit(ctx.orExpression());
    }

    @Override
    public Tree visitOrExpression(SlangParser.OrExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            Expr.Operation exprOp;
            if (op.getType() == SlangLexer.OR) {
                exprOp = Expr.Operation.OR;
            } else {
                throw new IllegalArgumentException("unhandled expr op " + op);
            }
            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitAndExpression(SlangParser.AndExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            Expr.Operation exprOp;
            if (op.getType() == SlangLexer.AND) {
                exprOp = Expr.Operation.AND;
            } else {
                throw new IllegalArgumentException("unhandled expr op " + op);
            }
            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitCompareExpression(SlangParser.CompareExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case SlangLexer.EQUALTO -> Expr.Operation.EQUALTO;
                case SlangLexer.NOTEQUAL -> Expr.Operation.NOT_EQUALS;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitRelationalExpression(SlangParser.RelationalExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case SlangLexer.LESSTHAN -> Expr.Operation.LESSTHAN;
                case SlangLexer.LESSTHANOREQ -> Expr.Operation.LESSTHANOREQ;
                case SlangLexer.GREATERTHAN -> Expr.Operation.GREATERTHAN;
                case SlangLexer.GREATERTHANOREQ -> Expr.Operation.GREATERTHANOREQ;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitAdditionExpression(SlangParser.AdditionExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case SlangLexer.ADD -> Expr.Operation.ADD;
                case SlangLexer.SUB -> Expr.Operation.SUB;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitMultiplicationExpression(SlangParser.MultiplicationExpressionContext ctx) {
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case SlangLexer.MUL -> Expr.Operation.MUL;
                case SlangLexer.DIV -> Expr.Operation.DIV;
                case SlangLexer.MOD -> Expr.Operation.MOD;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitUnaryExpression(SlangParser.UnaryExpressionContext ctx) {
        // Get the operator and the value expression
        var operator = ctx.unaryOp;
        var value = (Expr) visit(ctx.core());

        if (operator != null) {
            // Map the operator to its corresponding operation
            var operation = switch (operator.getType()) {
                case SlangLexer.SUB -> Expr.Operation.NEGATE;
                case SlangLexer.BANG -> Expr.Operation.BANG;
                default -> throw new AssertionError("Unknown unary operator: " + operator.getText());
            };

            // Return the unary expression
            return new Expr(
                    getLocation(ctx).span(getLocation(operator)),
                    operation,
                    List.of(value)
            );
        }

        // Return the value directly if no operator
        return value;
    }

    @Override
    public Tree visitCore(SlangParser.CoreContext ctx) {
        if(ctx.NUMBER_LITERAL()!=null && ctx.getText().equals(ctx.NUMBER_LITERAL().toString())){
            return new NumberLiteral(getLocation(ctx), Double.parseDouble(ctx.getText()));
        }else if(ctx.BOOLEAN_LITERAL()!=null && ctx.getText().equals(ctx.BOOLEAN_LITERAL().toString())){
            return new BoolLiteral(getLocation(ctx), Boolean.parseBoolean(ctx.getText()));
        }
        else if(ctx.ID() != null && ctx.getText().contains("[") && ctx.getText().contains("]") && ctx.NUMBER_LITERAL() != null){
            var arrayName = ctx.ID().getText();
            var wantedIndex = Integer.parseInt(ctx.NUMBER_LITERAL().getText());
            ArrayStatement array = arrayDefined(arrayName);
            if(array != null){
                if(array.getSizeOfArray() - 1 >= wantedIndex){
                    return array.getElements().getElements().get(wantedIndex);
                }
                else{
                    slang.error(getLocation(ctx), "index out of bounds");
                    return new ErrorExpr(getLocation(ctx));
                }
            }
            else{
                slang.error(getLocation(ctx), "array is not defined");
                return new ErrorExpr(getLocation(ctx));
            }
        }
        else {
            var loc = getLocation(ctx);

            String variableName = ctx.ID().getText();

            return lookup(loc, variableName)
                    .map(simpleStatement -> {
                        if (simpleStatement instanceof SimpleStatement variable) {
                            //slang.error(variable.getLocation(), "variable does not have a type");
                            SimpleStatement foundSimpleStatement = findSimpleStatement(((SimpleStatement) simpleStatement).getName());
                            if (foundSimpleStatement == null || (foundSimpleStatement.getType() == null)) {
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
// action proba(numero broj1, numero broj2){ sisa = broj1; getback sisa;}