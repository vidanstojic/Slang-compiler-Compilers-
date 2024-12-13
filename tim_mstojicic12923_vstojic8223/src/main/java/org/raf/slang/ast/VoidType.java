package org.raf.slang.ast;

public class VoidType extends VariableType{
    public VoidType(Location loc, String typeName) {
        super(loc, typeName);
    }

    public String userReadableName() {
        return "void";
    }


}
