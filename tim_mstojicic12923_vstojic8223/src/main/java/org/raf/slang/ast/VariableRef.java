package org.raf.slang.ast;


import java.util.Objects;

public class VariableRef extends Expr{
    private SimpleStatement variable;

    public VariableRef(Location location,SimpleStatement variable) {
        super(location);
        this.variable = variable;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("var", () -> {
            pp.terminal(variable.getName());
            pp.terminal("identity: %s"
                    /* toIdentityString is the default toString, so it
                       should return a string based on object
                       identity, i.e. a different one for each decl  */
                    .formatted(Objects.toIdentityString(variable)));
        });
    }
}
