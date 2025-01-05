package org.raf.slang.vm;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@RequiredArgsConstructor
public class BytecodeContainerInvocation {
    private final List<Value> operandStack = new ArrayList<>();
    private final BytecodeContainer bytecodeContainer;
    private final Value[] upvalues;
    private final Value[] locals;
    private final int prevIp;

    /** Construct a blob invocation for the outermost blob.  It has no locals,
     nor upvalues (duh - there's no up).  */
    public BytecodeContainerInvocation(BytecodeContainer bytecodeContainer) {
        this(bytecodeContainer, null, null, -1);
    }
}
