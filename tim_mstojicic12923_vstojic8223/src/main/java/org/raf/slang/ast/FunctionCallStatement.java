package org.raf.slang.ast;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public final class FunctionCallStatement extends Statement{

    private String name;
    private List<Expr> arguments;
    private FunctionDefinition definition;


    public FunctionCallStatement(Location location, String name, List<Expr> arguments) {
        super(location);
        this.name = name;
        this.arguments = arguments;

    }

    @Override
    public void nodePrint(ASTNodePrinter functionCallPrint) {
        functionCallPrint.node("function call " + name, () -> arguments.forEach(x -> x.nodePrint(functionCallPrint)));
    }
}
