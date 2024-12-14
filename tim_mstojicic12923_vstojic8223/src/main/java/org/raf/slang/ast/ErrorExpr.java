package org.raf.slang.ast;


/** A special node indicating an error occurred.  */
public final class ErrorExpr extends Expr {
    private String message;
    public ErrorExpr(Location location) {
        super(location);
    }

    protected ErrorExpr(Location location, String message){
        super(location);
        this.message = message;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("error", () -> {
            if (message != null) {
                pp.terminal(message);
            }
        });
    }
}
