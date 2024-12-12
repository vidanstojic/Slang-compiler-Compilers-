package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public final class StatementList extends Statement{

    private List<Statement> listOfStatements;

    public StatementList(Location location, List<Statement> listOfStatements){
        super(location);
        this.listOfStatements = listOfStatements;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node("statements", () -> listOfStatements.forEach (x -> x.nodePrint(pp)));
    }
}
