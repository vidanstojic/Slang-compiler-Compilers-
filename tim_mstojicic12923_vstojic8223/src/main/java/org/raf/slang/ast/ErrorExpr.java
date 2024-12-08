package org.raf.slang.ast;


/** A special node indicating an error occurred.  */
public final class ErrorExpr extends Expr {
    protected ErrorExpr(Location location) {
        super(location);
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("error", () -> {});
    }
}
