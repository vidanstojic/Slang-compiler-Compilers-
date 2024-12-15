package org.raf.slang;

import org.raf.slang.ast.*;

import java.util.List;

public class TypeCheck {
    private Slang slang;

    public TypeCheck(Slang slang) {
        this.slang = slang;
    }


    public void typecheck(StatementList block) {
        /* Typecheck all statements.  */
        block.getListOfStatements().forEach(this::typecheck);
    }

    private void typecheck(Statement stmt_) {
        switch (stmt_) {
            case PrintStatement stmt -> {
                /* Prints can print anything, so all are okay.*/
            }
            case SimpleStatement stmt -> {
            /*The type of the left-hand side of a 'let' statement is the same
            as the type of the right-hand side.  This is type deduction.*/
                var type = stmt.getType();
                //
                Expr newValue;
                if (typecheck(stmt.getValue()) != null){
                    newValue = stmt.getValue();
                    stmt.setValue(tryAndConvert(stmt.getType(),newValue));
                    stmt.setType(newValue.getResultType());//proveriti da li je ovo tacno
                }


                //stmt.setValue(newValue);

            }
            case IfStatement stmt -> {
                var exprList = stmt.getExprList();
                for (Expr expr2: exprList){
                    typecheck(expr2);
                   // typecheck(expr2);
              //      System.out.println(expr);
                }
                Expr expr = tryAndConvert(exprList.get(0).getResultType(), exprList.get(0));
                int j = 0;
                for(int i = 0; i < exprList.size(); i++){
                    if(!tryAndConvert(exprList.get(j).getResultType(), exprList.get(i)).getResultType().getTypeName().equals(expr.getResultType().getTypeName())){
                        slang.error(stmt.getLocation(), "GRESKAA, NISU ISTI TIPOVII");
                    }

                }
                //Expr expr = tryAndConvert(exprList.get(0).getResultType(), exprList.get(0));
                //Expr.Operation op;//treba ispraviti hardkodovano

                List<Statement> listOfStatements = stmt.getStatementList();
                StatementList statementList = new StatementList(stmt.getLocation(),listOfStatements);
                typecheck(statementList);
            }
            case ElseStatement stmt -> {
                List<Statement> listOfStatements = stmt.getStatementList();
                StatementList statementList = new StatementList(stmt.getLocation(),listOfStatements);
                typecheck(statementList);
            }
            case LoopStatement stmt -> {
                var exprList = stmt.getExprList();
                for (Expr expr: exprList){
                    typecheck(expr);
                    System.out.println(expr);
                }
                List<Statement> listOfStatements = stmt.getStatementList();
                StatementList statementList = new StatementList(stmt.getLocation(),listOfStatements);
                typecheck(statementList);
            }
            case FunctionDefinition stmt -> {
                var functioReturnType = stmt.getFunctionReturnType();
                if(!functioReturnType.equals(stmt.getTypeOfReturnData())){
                    slang.error(stmt.getLocation(), "type of returned data is not correct");

                }
                List<Statement> listOfStatements = stmt.getStatementList();
                StatementList statementList = new StatementList(stmt.getLocation(),listOfStatements);
                typecheck(statementList);
            }
            case FunctionCallStatement stmt -> {
                if(stmt.getArguments().size() == stmt.getDefinition().getParameters().size()){
                    for(int i = 0; i < stmt.getArguments().size(); i++){
                        var newValue = typecheck(stmt.getArguments().get(i));
                        tryAndConvert(stmt.getDefinition().getParameters().get(i).getType(),newValue);
                    }
                }
            }
            case ScanStatement stmt -> {
               // ne ide nista za scan, mozda samo neke provere da l je pravilno napisao ime promenljive ili slicno
            }
            case ArrayStatement stmt -> {
                typecheck(stmt.getElements());
            }

            /* Statement list logic is above.*/
            case StatementList stmt -> typecheck(stmt);
        }
    }




    private Expr typecheck(Expr expr_) {
        /* A few expressions are subclasses.  Check those separately.  */
        if(expr_.getOperation().equals(Expr.Operation.BANG)){
            expr_.setResultType(slang.getBoolType());
            expr_ = expr_.getLhs();
        }

        switch (expr_) {
            case ErrorExpr expr -> {
                /* Something went wrong.  Make up a result.  */
                expr.setResultType(slang.getNumberType());//nelogicno je da bude numbertype za error
                return expr;
            }
            case NumberLiteral expr -> {
                /* Axiomatically a number.  */
                expr.setResultType(slang.getNumberType());
                return expr;
            }
            case VariableRef expr -> {
                /* Whatever the type of the variable we're looking at is.  */
                expr.setResultType(expr.getVariable().getType());
                return expr;
            }
            case ArrayLiteral expr -> {
                /* We need to type check each of the arguments.  */
                for (int i = 0; i < expr.getElements().size(); i++) {
                    /* Type-check and apply any conversions.  */
                    var newElem = typecheck(expr.getElements().get(i));
                    expr.getElements().set(i, newElem);
                }

            /* This language is entirely powered by type deduction.  To deduce
               a type of a list, we'd need the least upper bound of all the
               element types, and then to convert each element to that least
               upper bound, as that will be the element type.

               However, our type system only does numbers and lists, which do
               not have a LUB ever, nor do two distinct list types, so we can
               take the type of the first element to be our LUB.

               If it is empty, eh, make it a list of numbers.  Whatever.  */

                if (expr.getElements().isEmpty()) {
                    expr.setResultType(slang.listOfType(slang.getNumberType()));
                    return expr;
                }
                /* Element type.  */
                var eltType = expr.getTypeOfArray();
                var vectorType = slang.listOfType(eltType);
                expr.setResultType(vectorType);

                /* Now we need to try to make the elements fit the vectors.  */
                for (int i = 0; i < expr.getElements().size(); i++) {
                    var newElem = tryAndConvert(eltType, expr.getElements().get(i));
                    expr.getElements().set(i, newElem);
                }

                /* Done.  */
                return expr;
            }
            case BoolLiteral expr -> {
                expr.setResultType(slang.getBoolType());
                return expr;
            }
            case Expr expr -> {
                //ovde dodati za bang 
            /* Checked below.  */
            }
            }
            /* We have a regular expression here.  */
            switch (expr_.getOperation()) {
                case ADD, DIV, MUL, CARET, SUB,
                        MOD, BITAND, BITOR, OR, AND, NOTEQUAL,
                        EQUALTO, GREATERTHAN, GREATERTHANOREQ,
                        LESSTHAN, LESSTHANOREQ-> {// dodati ovde i ostale operacije

            /* Binary number expressions.  */
            expr_.setLhs(typecheck(expr_.getLhs()));
            expr_.setRhs(typecheck(expr_.getRhs()));

            /* They both must be numbers.  */
            expr_.setLhs(tryAndConvert(slang.getNumberType(), expr_.getLhs()));
            expr_.setRhs(tryAndConvert(slang.getNumberType(), expr_.getRhs()));

            /* The result is always a number.  */
            expr_.setResultType(slang.getNumberType());
            return expr_;
            }
                    case VALUE ->
            /* Shouldn't be possible.  */
            throw new IllegalStateException();
                case BANG -> {
                    if (!expr_.getResultType().getTypeName().equals("yeahNah")){
                        slang.error(expr_.getLocation(),
                                "cannot use a value of type '%s' where type bool is needed",
                                expr_.getResultType().userReadableName());
                        //treba dodati sta u ovom slucaju da bude expr
                    }else{
                        expr_.setResultType(slang.getBoolType());
                    }

                    return expr_;
                }
            }

                    throw new IllegalStateException();
        }
    /** Attempts to make the expression EXPR fit into a place of type TO.  */
    private Expr tryAndConvert(VariableType to, Expr expr) {
        /* We expect the EXPR to be typechecked.  */
        assert expr.getResultType() != null : "expr " + expr + " not checked";

        /* Here we could add any conversions we deem necessary.  */
        if (!expr.getResultType().getTypeName().equals(to.getTypeName())) {
            slang.error(expr.getLocation(),
                    "cannot use a value of type '%s' where type '%s' is needed",
                    expr.getResultType().userReadableName(),
                    to.userReadableName());
            expr = new ErrorExpr(null);
            expr.setResultType(new VariableType(expr.getLocation(), "ErrorType") {
                @Override
                public String userReadableName() {
                    return "Error";
                }
            });
        }

        return expr;
    }

    }
/*
* numero i; grabmsg(i); numero j = 5; grabmsg(j); yeahNah k = true; grabmsg(k);
* */
