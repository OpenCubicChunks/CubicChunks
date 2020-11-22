package io.github.opencubicchunks.cubicchunks.mixin;

import java.util.Collection;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode("CC_INSTANCEOF")
public class BeforeInstanceofInjectionPoint extends InjectionPoint {

    private final String target;
    private final int ordinal;

    public BeforeInstanceofInjectionPoint(InjectionPointData data) {
        this.target = data.get("class", null);
        this.ordinal = data.getOrdinal();
    }

    @Override public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> outputNodes) {
        int idx = 0;
        boolean foundAny = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.INSTANCEOF && target.equals(((TypeInsnNode) insn).desc)) {
                if (ordinal < 0 || idx == ordinal) {
                    outputNodes.add(insn);
                    foundAny = true;
                }
                idx++;
            }
        }
        return foundAny;
    }
}