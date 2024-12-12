package org.raf.slang.ast;

import java.util.List;

public final class IfStatement extends Statement{

    private List<Expr>exprList;
    private List<Statement> statementList;
    public IfStatement(Location location, List<Expr>exprList, List<Statement> statementList) {
        super(location);
        this.exprList = exprList;
        this.statementList = statementList;
    }

    @Override
    public void nodePrint(ASTNodePrinter printIfStatement) {
        printIfStatement.node("if", () -> {
            exprList.forEach(x -> x.nodePrint(printIfStatement));
            statementList.forEach(statement -> statement.nodePrint(printIfStatement));
        });
    }
}
