package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class SimpleStatement extends Statement{

    private String name;
    private Expr value;
    private VariableType type;

    public SimpleStatement(Location location, String name, Expr value, VariableType type) {
        super(location);
        this.name = name;
        this.value = value;
        this.type = type;
    }
    public boolean hasType() {
        return type != null && !type.getTypeName().isEmpty();
    }
    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("declaration of %s".formatted(name),
                () -> {
                    if (type != null) {
                        pp.terminal("type: %s".formatted(type.toString()));
                    } else {
                        pp.terminal("type: undefined");
                    }
                    if (value != null) {
                        value.nodePrint(pp);
                    }
                });
    }
}
