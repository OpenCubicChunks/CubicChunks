/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.asm;

import com.google.common.collect.Lists;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.List;

public class RenderGlobalTransformer extends AbstractClassTransformer {

	private static final MethodInfo RENDER_ENTITIES = new MethodInfo("func_180446_a", "renderEntities");

	private static final List<MethodInfo> TO_TRANSFORM = Lists.newArrayList(RENDER_ENTITIES);
	@Override
	protected String getTransformedClassName() {
		return "net.minecraft.client.renderer.RenderGlobal";
	}

	@Override
	protected Collection<MethodInfo> getMethodsToTransform() {
		return TO_TRANSFORM;
	}

	@Override
	protected void transformMethod(MethodInfo methodInfo, MethodNode node) {
		replaceChunkGetEntityListsUsage(node);
	}

	/**
	 * I have no idea how to do it using MethodVisitor...
	 */
	private void replaceChunkGetEntityListsUsage(MethodNode node) {
		/*
		 * It should replace chunk.getEntityLists[chunkY] with method call that will return ClassInheritanceMultiMap
		 * Then Minecraft calls iterator() on this object.
		 *
		 * Instead of replacing the whole method call and array access
		 * (a few bytecode instruction, and it involves inner classes)
		 * it will only replace array access with special method call.
		 * It means that when we call our method we have unnecessary ClassInheritanceMultiMap[] array on the stack
		 * so our method needs to take it as (completely ignored) argument.
		 * We also need to insert out code before the code will get Y position from BlockPos
		 * So that we know which chunk it's in.
		 * We also need to get access to the World object - we need to get the chunk somehow
		 */
		InsnList insns = node.instructions;

		/*
		 * find the method call that is used to get iterator,
		 * somewhere before that method we have INVOKEVIRTUAL opcode
		 * which calls BlockPos.getY
		 * in my dev environment it's the fourth instruction before invokevirtual,
		 * but it may be different in the actual Minecraft bytecode
		 * so will need to scan it backwards from getIterator instruction
		 */
		MethodInsnNode getIteratorNode = AsmUtils.findMethodCall(insns, "net/minecraft/util/ClassInheritanceMultiMap", new MethodInfo("iterator", "iterator"));

		MethodInsnNode invokeGetY = AsmUtils.findInsnBefore(insns, Opcodes.INVOKEVIRTUAL, getIteratorNode);

		//a few labels we will need
		LabelNode newCodeLabel = new LabelNode();
		LabelNode beforeGetIterLabel = new LabelNode();

		insns.add(newCodeLabel);
		insns.insertBefore(getIteratorNode, beforeGetIterLabel);

		//jump to our new code before executing invokeGetY
		insns.insertBefore(invokeGetY, new JumpInsnNode(Opcodes.GOTO, newCodeLabel));
		/*
		 * STACK at this point:
		 * ClassInheritanceMultiMap[] --> should be ignored. maybe pop it somehow?
		 * BlockPos --> we can use it to get chunk position and then to get the chunk
		 * we still need World object. The easiest way it to pass 'this' to our method
		 * and extract world object from RenderGlobal later
		 */
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); //ALOAD 0 (this)
		//now we can actually call our external static method
		insns.add(new MethodInsnNode(
				Opcodes.INVOKESTATIC,
				"cubicchunks/asm/RenderMethods",
				"getEntityList",
				"([Lnet/minecraft/util/ClassInheritanceMultiMap;Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/RenderGlobal;)Lnet/minecraft/util/ClassInheritanceMultiMap;",
				false));
		//and  then go back to Minecraft code
		insns.add(new JumpInsnNode(Opcodes.GOTO, beforeGetIterLabel));
	}
}
