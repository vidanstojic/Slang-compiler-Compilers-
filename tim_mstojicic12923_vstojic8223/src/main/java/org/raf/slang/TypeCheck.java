package org.raf.slang;

import org.raf.slang.ast.*;

import java.util.List;

public class TypeCheck {
    private Slang slang;

    public TypeCheck(Slang slang) {
        this.slang = slang;
    }

    public void typecheck(StatementList block) {
        block.getListOfStatements().forEach(this::typecheck);
    }

    private void typecheck(Statement stmt_) {
        switch (stmt_) {
            case PrintStatement stmt -> {}
            case SimpleStatement stmt -> {
                Expr newValue;
                if (stmt.getValue() != null){
                    if (typecheck(stmt.getValue()) != null){
                        newValue = stmt.getValue();
                        stmt.setValue(tryAndConvert(stmt.getType(), newValue));
                        stmt.setType(newValue.getResultType());
                    }
                }
            }
            case IfStatement stmt -> {
                for (Expr expr : stmt.getExprList()) {
                    typecheck(expr);
                }
                StatementList statementList = new StatementList(stmt.getLocation(), stmt.getStatementList());
                typecheck(statementList);
            }
            case ElseStatement stmt -> {
                StatementList statementList = new StatementList(stmt.getLocation(), stmt.getStatementList());
                typecheck(statementList);
            }
            case LoopStatement stmt -> {
                for (Expr expr : stmt.getExprList()) {
                    typecheck(expr);
                }
                StatementList statementList = new StatementList(stmt.getLocation(), stmt.getStatementList());
                typecheck(statementList);
            }
            case FunctionDefinition stmt -> {
                if(!stmt.getFunctionReturnType().equals(stmt.getTypeOfReturnData())){
                    slang.error(stmt.getLocation(), "type of returned data is not correct");
                }
                StatementList statementList = new StatementList(stmt.getLocation(), stmt.getStatementList());
                typecheck(statementList);
            }
            case FunctionCallStatement stmt -> {
                if(stmt.getArguments().size() == stmt.getDefinition().getParameters().size()){
                    for(int i = 0; i < stmt.getArguments().size(); i++){
                        var newValue = typecheck(stmt.getArguments().get(i));
                        tryAndConvert(stmt.getDefinition().getParameters().get(i).getType(), newValue);
                    }
                }
            }
            case ScanStatement stmt -> {}
            case ArrayStatement stmt -> {
                typecheck(stmt.getElements());
            }
            case StatementList stmt -> typecheck(stmt);
        }
    }

    private Expr typecheck(Expr expr_) {
        if (expr_.getOperation() != null && expr_.getOperation().equals(Expr.Operation.BANG)) {
            expr_.setResultType(slang.getBoolType());
            return expr_.getOperands().get(0);
        }

        switch (expr_) {
            case ErrorExpr expr -> {
                expr.setResultType(slang.getNumberType());
                return expr;
            }
            case NumberLiteral expr -> {
                expr.setResultType(slang.getNumberType());
                return expr;
            }
            case VariableRef expr -> {
                expr.setResultType(expr.getVariable().getType());
                return expr;
            }
            case ArrayLiteral expr -> {
                if(expr.getSizeOfArray() != -1 && expr.getSizeOfArray() < expr.getElements().size()) {
                    slang.error(expr.getLocation(), "the array has exceeded its defined size");
                }
                for (int i = 0; i < expr.getElements().size(); i++) {
                    var newElem = typecheck(expr.getElements().get(i));
                    expr.getElements().set(i, newElem);
                }
                if (expr.getElements().isEmpty()) {
                    expr.setResultType(slang.listOfType(slang.getNumberType()));
                    return expr;
                }
                var eltType = expr.getTypeOfArray();
                var vectorType = slang.listOfType(eltType);
                expr.setResultType(vectorType);
                for (int i = 0; i < expr.getElements().size(); i++) {
                    var newElem = tryAndConvert(eltType, expr.getElements().get(i));
                    expr.getElements().set(i, newElem);
                }
                return expr;
            }
            case BoolLiteral expr -> {
                expr.setResultType(slang.getBoolType());
                return expr;
            }
            case Expr expr -> {}
        }

        switch (expr_.getOperation()) {
            case AND, OR -> {
                for (Expr operand : expr_.getOperands()) {
                    operand = typecheck(operand);
                    operand = tryAndConvert(slang.getBoolType(), operand);
                }
                expr_.setResultType(slang.getBoolType());
                return expr_;
            }
            case ADD, DIV, MUL, CARET, SUB, MOD -> {
                for (Expr operand : expr_.getOperands()) {
                    operand = typecheck(operand);
                    operand = tryAndConvert(slang.getNumberType(), operand);
                }
                expr_.setResultType(slang.getNumberType());
                return expr_;
            }
            case NOT_EQUALS, EQUALTO, GREATERTHAN, GREATERTHANOREQ, LESSTHAN, LESSTHANOREQ ->{
                Expr expresion = null;
                for (Expr operand : expr_.getOperands()) {
                    operand = typecheck(operand);
                    if (expresion == null)
                        expresion = operand;
                    else
                        operand = tryAndConvert(expresion.getResultType(), operand);
                }
                expr_.setResultType(slang.getNumberType());
                return expr_;
            }
            case VALUE -> throw new IllegalStateException();
            case BANG -> {
                if (!expr_.getResultType().getTypeName().equals("yeahNah")) {
                    slang.error(expr_.getLocation(), "cannot use a value of type '%s' where type bool is needed",
                            expr_.getResultType().userReadableName());
                    expr_.setResultType(new VariableType(expr_.getLocation(), "ErrorType") {
                        @Override
                        public String userReadableName() {
                            return "Error";
                        }
                    });
                } else {
                    expr_.setResultType(slang.getBoolType());
                }
                return expr_;
            }
        }

        throw new IllegalStateException();
    }

    private Expr tryAndConvert(VariableType to, Expr expr) {
        assert expr.getResultType() != null : "expr " + expr + " not checked";
        if (!expr.getResultType().getTypeName().equals(to.getTypeName())) {
            slang.error(expr.getLocation(), "cannot use a value of type '%s' where type '%s' is needed",
                    expr.getResultType().userReadableName(), to.userReadableName());
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
