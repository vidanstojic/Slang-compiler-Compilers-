package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class ArrayLiteral extends Statement{

    private List<NumberLiteral> elements;
    public ArrayLiteral(Location location, List<NumberLiteral> elements) {
        super(location);
        this.elements = elements;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {

    }
}
