package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public final class ArrayStatement extends Statement{

    private VariableType typeOfArray;
    private String name;
    private ArrayLiteral elements;
    private int sizeOfArray;
    public ArrayStatement(Location location, String name, ArrayLiteral elements, VariableType typeOfArray) {
        super(location);
        this.name = name;
        this.elements = elements;
        this.typeOfArray = typeOfArray;
    }

    @Override
    public void nodePrint(ASTNodePrinter arrayForPrint) {
        arrayForPrint.node("array", () -> elements.nodePrint(arrayForPrint));// u ovom lambda izrazu se poziva nodePrint za svaki element liste pojedinacno
    }
}
