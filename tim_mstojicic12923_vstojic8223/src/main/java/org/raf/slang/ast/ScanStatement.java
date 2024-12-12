package org.raf.slang.ast;

import java.util.List;
import java.util.Objects;

public final class ScanStatement extends Statement {

    //private List<Expr> arguments;
    private String name;

    public ScanStatement(Location location, String name) {
        super(location);
        this.name = name;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("scan ", () -> pp.terminal(Objects.toString(name)));
    }
}