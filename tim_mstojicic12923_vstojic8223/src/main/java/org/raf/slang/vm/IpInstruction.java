package org.raf.slang.vm;

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
