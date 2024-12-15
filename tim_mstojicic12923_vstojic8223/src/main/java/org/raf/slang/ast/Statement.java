package org.raf.slang.ast;


/** Base class for all statements.  */
public sealed abstract class Statement extends Tree
        permits SimpleStatement,IfStatement,ElseStatement,LoopStatement,PrintStatement,ScanStatement,FunctionDefinition
        ,FunctionCallStatement, ArrayStatement, StatementList {
    public Statement(Location location) {
        super(location);
    }
}
