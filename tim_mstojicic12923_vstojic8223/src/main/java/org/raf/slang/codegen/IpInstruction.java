package org.raf.slang.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IpInstruction {
    private int indexInStack;
    private Instruction instruction;


}
