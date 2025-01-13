package org.raf.slang.codegen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.raf.slang.ast.SimpleStatement;
import org.raf.slang.vm.BytecodeContainer;
import org.raf.slang.vm.UpvalueMapEntry;

import java.util.IdentityHashMap;
@RequiredArgsConstructor
@Getter
public class InTranslationBytecodeContainer {
    private final BytecodeContainer code;
    private final IdentityHashMap<SimpleStatement, Integer> localSlots;

    public record UpvalSlotInfo(int slotNr, UpvalueMapEntry entry) {}
    private final IdentityHashMap<SimpleStatement, UpvalSlotInfo> upvalSlots;
    private final InTranslationBytecodeContainer previousBlob;

    private int localDepth = 0;
    private int maxLocalDepth = 0;

    public void setLocalDepth(int localDepth) {
        if ((this.localDepth = localDepth) > maxLocalDepth)
            maxLocalDepth = localDepth;
    }
}
