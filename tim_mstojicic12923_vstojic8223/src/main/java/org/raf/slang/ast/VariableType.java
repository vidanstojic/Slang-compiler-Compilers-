package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public abstract class VariableType extends Tree{

    private final String typeName;
    private VariableType variableType;

    public VariableType (Location loc, String typeName) {
        super(loc);
        this.typeName = typeName;
    }


    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.terminal("variable type: "+typeName);
    }

    @Override
    public String toString() {
        return "(" +
                "typeName='" + typeName + '\'' +
                ')';
    }

    public abstract String userReadableName();
}
// action proba(numero broj1, numero broj2){getback empty;}