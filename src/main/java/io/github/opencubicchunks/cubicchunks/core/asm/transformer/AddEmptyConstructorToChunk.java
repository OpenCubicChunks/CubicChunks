/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.transformer;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AddEmptyConstructorToChunk implements IClassTransformer {

    public static Logger LOGGER = LogManager.getLogger("AddEmptyConstructorToChunk");

    @Override public byte[] transform(String name, String transformedName, byte[] basicClass) {
        try {
            if (!"net.minecraft.world.chunk.Chunk".equals(transformedName)) {
                return basicClass;
            }

            ClassReader cr = new ClassReader(basicClass);

            ClassNode node = new ClassNode();
            cr.accept(node, 0);
            MethodNode constructor = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
                    "<init>",
                    // take the CubicChunks object as arg to make sure the method is unique
                    "(Lio/github/opencubicchunks/cubicchunks/core/CubicChunks;)V",
                    null, null);
            constructor.maxLocals = 0;
            constructor.maxStack = 1;
            constructor.instructions.add(new IntInsnNode(Opcodes.ALOAD, 0));
            constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            constructor.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(constructor);

            ClassWriter cw = new ClassWriter(0);
            node.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            LOGGER.catching(t);
            throw t;
        }
    }

}
