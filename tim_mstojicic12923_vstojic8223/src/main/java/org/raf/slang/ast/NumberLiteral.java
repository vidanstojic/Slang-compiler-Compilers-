package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
public class NumberLiteral extends Expr{

    private double value;
    public NumberLiteral(Location location, double value) {
        super(location);
        this.value = value;
    }
    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("number", () -> pp.terminal(Objects.toString(value)));
    }

}
