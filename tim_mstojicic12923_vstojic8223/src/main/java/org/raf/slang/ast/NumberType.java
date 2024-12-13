package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NumberType extends VariableType {


    public NumberType(Location loc, String typeName) {
        super(loc, typeName);
    }

    public String userReadableName() {
        return "number";
    }
}
