package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class BoolLiteral extends Expr{

    private boolean bool;
    public BoolLiteral(Location location, boolean bool) {
        super(location);
        this.bool = bool;
    }
    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("bool", () -> pp.terminal(Objects.toString(bool)));
    }
}
