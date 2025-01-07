package org.raf.slang.ast;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
@Getter
@Setter
public final class ScanStatement extends Statement {

    //private List<Expr> arguments;
    private String name;
    private VariableType variableType;

    public ScanStatement(Location location, String name) {
        super(location);
        this.name = name;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("scan ", () -> pp.terminal(Objects.toString(name)));
    }
}