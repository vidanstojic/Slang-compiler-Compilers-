package org.raf.slang.ast;

import lombok.Getter;

import java.util.List;
@Getter
public final class LoopStatement extends Statement{

    private List<Expr> exprList;
    private List<Statement> statementList;
    private SimpleStatement iterator;

    public LoopStatement(Location location, List<Expr> exprList, List<Statement> statementList, SimpleStatement iterator) {
        super(location);
        this.exprList = exprList;
        this.statementList = statementList;
        this.iterator = iterator;
    }

    @Override
    public void nodePrint(ASTNodePrinter printLoopStatement) {
        printLoopStatement.node("loop", () -> {
            exprList.forEach(x -> x.nodePrint(printLoopStatement));
            statementList.forEach(statement -> statement.nodePrint(printLoopStatement));
        });

    }
}
