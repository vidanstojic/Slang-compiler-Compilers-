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
                var newValue = typecheck(stmt.getValue());
                stmt.setValue(tryAndConvert(stmt.getType(),stmt.getValue()));
                //stmt.setValue(newValue);
                stmt.setType(newValue.getResultType());
            }
            case IfStatement stmt -> {
                var exprList = stmt.getExprList();
                var variable = exprList.get(0);
                for (Expr expr: exprList){
                    typecheck(expr);
                    System.out.println(expr);
                }
                Expr expr = tryAndConvert(exprList.get(0).getResultType(), exprList.get(1));
                Expr.Operation op;

                List<Statement> listOfStatements = stmt.getStatementList();
                StatementList statementList = new StatementList(stmt.getLocation(),listOfStatements);
                typecheck(statementList);
                System.out.println("kkk");
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
                System.out.println("kk");
            }
            case FunctionDefinition stmt -> {}
            case FunctionCallStatement stmt -> {}
            case ScanStatement stmt -> {
                System.out.println("k");
            }

            /* Statement list logic is above.*/
            case StatementList stmt -> typecheck(stmt);
        }
    }




    private Expr typecheck(Expr expr_) {
        /* A few expressions are subclasses.  Check those separately.  */
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
                var eltType = expr.getElements().getFirst().getResultType();
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
            /* Checked below.  */
            }
            }

            /* We have a regular expression here.  */
            switch (expr_.getOperation()) {
                case ADD, DIV, MUL, CARET, SUB -> {// dodati ovde i ostale operacije
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
        }

        return expr;
    }

    }
/*
* numero i; grabmsg(i); numero j = 5; grabmsg(j); yeahNah k = true; grabmsg(k);
* */
