package org.raf.slang.ast;

import java.util.List;

public class FunctionDefinition extends Statement{


    private String name;
    private String functionReturnType;
    private List<FunctionParameter> parameters;
    private List<Statement> statementList;


    public FunctionDefinition(Location location, String name,String functionReturnType,List<FunctionParameter> arguments,List<Statement> statementList ) {
        super(location);
        this.name = name;
        this.functionReturnType = functionReturnType;
        this.parameters = arguments;
        this.statementList = statementList;
    }

    @Override
    public void nodePrint(ASTNodePrinter functionDefinitionPrint) {
        functionDefinitionPrint.node("function definiton of " + this.functionReturnType +" " +this.name, () -> {
            parameters.forEach(x -> x.nodePrint(functionDefinitionPrint));
            statementList.forEach(statement -> statement.nodePrint(functionDefinitionPrint));
        });
    }
}
