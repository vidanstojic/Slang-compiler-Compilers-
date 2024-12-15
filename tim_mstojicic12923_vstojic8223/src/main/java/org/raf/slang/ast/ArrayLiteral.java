package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ArrayLiteral extends Expr {
    private VariableType typeOfArray;
    private List<Expr> elements;

    protected ArrayLiteral(Location location, List<Expr> elements) {
        super(location);
        this.elements = elements;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("array element", () -> elements.forEach(x -> x.nodePrint(pp)));
    }
}
