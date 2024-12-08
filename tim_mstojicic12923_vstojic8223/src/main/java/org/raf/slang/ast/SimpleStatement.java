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

    public SimpleStatement(Location location, String name, Expr value) {
        super(location);
        this.name = name;
        this.value = value;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("declaration of %s".formatted(name),
                () -> {
                    if (value != null) {
                        value.nodePrint(pp);
                    }
                });
    }
}
