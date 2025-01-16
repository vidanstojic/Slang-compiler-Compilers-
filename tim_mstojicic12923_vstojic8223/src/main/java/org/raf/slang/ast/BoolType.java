package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoolType extends VariableType{

    public BoolType(Location loc, String typeName) {
        super(loc, typeName);
    }
    private boolean bool;


    public String userReadableName() {
        return "boolean";
    }
}
