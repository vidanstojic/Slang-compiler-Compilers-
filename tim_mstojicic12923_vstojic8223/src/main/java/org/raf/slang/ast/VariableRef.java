package org.raf.slang.ast;


import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
@Getter
@Setter
public class VariableRef extends Expr{
    private SimpleStatement variable;

    public VariableRef(Location location,SimpleStatement variable) {
        super(location);
        this.variable = variable;

    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("variable", () -> {
            pp.terminal(variable.toString());
        });
    }
}
/**
 *             pp.terminal("identity: %s"
 *                     /* toIdentityString is the default toString, so it
 *                        should return a string based on object
 *                        identity, i.e. a different one for each decl
 *
 *   Ovaj deo koda je stajao u nodeprint ali nam trenutno nije potreban, ostavio sam ga ovde u slucaju da nam kasnije ponovo treba.
 *
 */
