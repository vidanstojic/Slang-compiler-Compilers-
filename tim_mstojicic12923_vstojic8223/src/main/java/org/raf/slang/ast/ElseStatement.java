package org.raf.slang.ast;

import java.util.List;
import java.util.Objects;

public class ElseStatement extends Statement{

    private List<Statement> statementList;

    public ElseStatement(Location location, List<Statement> statementList) {
        super(location);
        this.statementList = statementList;
    }

    @Override
    public void nodePrint(ASTNodePrinter printElseStatement) {
        printElseStatement.node("else", () -> statementList.forEach(statement -> statement.nodePrint(printElseStatement)));
    }
}
