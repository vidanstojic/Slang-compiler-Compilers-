package org.raf.slang.vm;

import java.util.ArrayList;
import java.util.List;

public record  BytecodeContainer(List<Instruction> code,
                                 /** {@code PUSH_CONSTANT} instructions contain a reference
                                  by index into this table.  */
                                 List<Double> constantTable) {

    public BytecodeContainer() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public int addInsn(Instruction instruction) {
        var newInsnIp = code().size();
        code().add(instruction);
        return newInsnIp;
    }
}
