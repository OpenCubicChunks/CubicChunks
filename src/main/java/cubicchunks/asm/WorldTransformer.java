package cubicchunks.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

/**
 * Replaces hardcoded height checks in World class with getWorldHeight method
 */
public class WorldTransformer implements IClassTransformer{
	private static final String WORLD_CLASS_NAME = "net.minecraft.world.World";

	private static final MethodInfo WORLD_IS_VALID = new MethodInfo("func_175701_a", "isValid");
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(!transformedName.equals(WORLD_CLASS_NAME)) {
			return bytes;
		}
		ClassReader cr = new ClassReader(bytes);
		ClassNode cn = new ClassNode(Opcodes.ASM4);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

		cr.accept(cn, 0);

		transformIsValid(findMethod(cn, WORLD_IS_VALID));

		cn.accept(cw);
		return cw.toByteArray();
	}

	private void transformIsValid(MethodNode cn) {
		InsnList insns = cn.instructions;
		//the y < 0 is the only IFLT Opcode in this method
		AbstractInsnNode ifltInsn = findInsn(insns, JumpInsnNode.class, Opcodes.IFLT, 0);
		//the label we want to jump to is the second label
		LabelNode label = findInsn(insns, LabelNode.class, -1, 1);
		insns.insertBefore(ifltInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(ifltInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMinHeight", "(Lnet/minecraft/world/World;)I", false));
		insns.set(ifltInsn, new JumpInsnNode(Opcodes.IF_ICMPLT, label));

		AbstractInsnNode sipushInsn = findInsn(insns, IntInsnNode.class, Opcodes.SIPUSH, 0);
		insns.insertBefore(sipushInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.set(sipushInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMaxHeight", "(Lnet/minecraft/world/World;)I", false));
	}

	private <T extends AbstractInsnNode> T findInsn(InsnList insns, Class<T> toFind, int opcode, int occurrence) {
		Iterator<AbstractInsnNode> it = insns.iterator();
		T found = null;
		int count = 0;
		while(it.hasNext()) {
			AbstractInsnNode node = it.next();
			System.out.println(node.getClass() + ", " + node.getOpcode() + ", " + count + ", " + occurrence);
			if(node.getClass() == toFind && node.getOpcode() == opcode) {
				if(count == occurrence) {
					found = (T) node;
					break;
				}
				count++;
			}
		}
		System.out.println(found);
		return found;
	}

	private MethodNode findMethod(ClassNode cn, MethodInfo mi) {
		MethodNode found = null;
		System.out.println("Finding methods");
		for(MethodNode meth : cn.methods) {
			System.out.println("Method: " + meth.name);
			if(WORLD_IS_VALID.sameAs(meth.name)) {
				found = meth;
				break;
			}
		}
		return found;
	}
}
