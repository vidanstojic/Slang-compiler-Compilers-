package org.raf.slang.ast;

import java.util.List;

public final class LoopStatement extends Statement{

    private List<Expr> exprList;
    private List<Statement> statementList;

    public LoopStatement(Location location, List<Expr> exprList, List<Statement> statementList) {
        super(location);
        this.exprList = exprList;
        this.statementList = statementList;
    }

    @Override
    public void nodePrint(ASTNodePrinter printLoopStatement) {
        printLoopStatement.node("loop", () -> {
            exprList.forEach(x -> x.nodePrint(printLoopStatement));
            statementList.forEach(statement -> statement.nodePrint(printLoopStatement));
        });

    }
}
