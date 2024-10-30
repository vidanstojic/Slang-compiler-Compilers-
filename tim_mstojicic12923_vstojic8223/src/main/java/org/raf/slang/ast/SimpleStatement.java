package org.raf.slang.ast;

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
                    value.nodePrint(pp);
                });
    }
}
