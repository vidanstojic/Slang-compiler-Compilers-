package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
public class PrintStatement extends Statement{
    private List<Expr> arguments;
    public PrintStatement(Location location, List<Expr> arguments) {
        super(location);
        this.arguments = arguments;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {

    }
}
