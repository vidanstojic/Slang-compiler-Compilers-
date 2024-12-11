package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class VariableType extends Tree{

    private final String typeName;

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
}
// action proba(numero broj1, numero broj2){getback empty;}