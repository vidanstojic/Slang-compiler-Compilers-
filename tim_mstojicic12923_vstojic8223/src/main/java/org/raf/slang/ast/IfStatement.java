package org.raf.slang.ast;

import java.util.List;

public class IfStatement extends Statement{

    private List<Expr>exprList;
    public IfStatement(Location location, List<Expr>exprList) {
        super(location);
        this.exprList = exprList;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {

    }
}
