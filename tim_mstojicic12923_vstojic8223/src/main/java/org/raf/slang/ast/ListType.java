package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListType {
    private VariableType variableType;
    public ListType(VariableType variableType) {
        super();
        this.variableType = variableType;
    }




    public String userReadableName() {
        /* number[] for instance.  */
        return "number array" + "[]";
    }
}
