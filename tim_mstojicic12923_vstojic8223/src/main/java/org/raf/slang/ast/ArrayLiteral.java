package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class ArrayLiteral extends Expr{

    private String name;
    private List<Expr> elements;
    public ArrayLiteral(Location location,String name,List<Expr> elements) {
        super(location);
        this.name = name;
        this.elements = elements;
    }

    @Override
    public void nodePrint(ASTNodePrinter arrayForPrint) {
        arrayForPrint.node("array", () -> elements.forEach(x -> x.nodePrint(arrayForPrint)));// u ovom lambda izrazu se poziva nodePrint za svaki element liste pojedinacno
    }
}
