package org.raf.slang.vm;

import org.raf.slang.ast.FunctionDefinition;

public class Function {
    private BytecodeContainer code;
    /** Given {@code upvalueMap[i] = x}, upvalue in slot {@code i} will be
     loaded with upvalue in {@code CLOSURE} context slot {@code x.slot()} if
     {@code x.loc()} is {@code UPVALUE}, or local variable in local slot
     {@code x.slot()}.

     In essence, we initialize upvalue {@code i} with either the upvalue or
     local variable {@code x.slot()} at time when we construct the closure,
     propagating it into the closure.  */
    private UpvalueMapEntry[] upvalueMap;
    /** Number of local variables in this function.  */
    private int localCount = -1;
    private FunctionDefinition funcDef;
}
