package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FunctionParameter extends Tree {
    private final String paramName;
    private final VariableType type;


    public FunctionParameter(Location loc, String paramName, VariableType type) {
        super(loc);
        this.paramName = paramName;
        this.type = type;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.terminal("function parameter: "+this.type.toString() +this.paramName);
    }
}
