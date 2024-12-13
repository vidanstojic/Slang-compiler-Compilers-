package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrayType extends VariableType{


    public ArrayType(Location loc, String typeName) {
        super(loc, typeName);
    }


    @Override
    public String userReadableName() {
        return "number array";
    }
}
