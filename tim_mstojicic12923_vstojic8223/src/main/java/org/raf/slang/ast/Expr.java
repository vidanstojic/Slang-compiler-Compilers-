package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Expr extends Tree{
    public Expr(Location location) {
        super(location);
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {

    }
}
