/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

@SuppressWarnings("unused")
public class CubicChunksWorldEditTransformer implements IClassTransformer {

    @Override public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"com.sk89q.worldedit.forge.ForgeWorld".equals(transformedName)) {
            return basicClass;
        }

        ClassReader cr = new ClassReader(basicClass);

        ClassNode node = new ClassNode();
        cr.accept(node, 0);

        for (MethodNode method : node.methods) {
            if (method.name.equals("getMinY") && method.desc.equals("()I")) {
                transformGetMinY(method);
            }
        }

        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        return cw.toByteArray();
    }

    private byte[] unrelocate(byte[] basicClass) {
        Remapper remapper = new Remapper() {
            @Override
            public String map(String typeName) {
                return typeName.replace("io/github/opencubicchunks/cubicchunks/cubicgen/blue/endless", "blue/endless");
            }
        };
        ClassWriter cw = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(cw, remapper);
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classRemapper, 0);
        return cw.toByteArray();
    }

    private void transformGetMinY(MethodNode getMinY) {

        /*
         This:
           public World getWorld() {
               throw new Error();
           }

           public int getMinY() {
               return ((ICubicWorld) getWorld()).getMinHeight();
           }
         Gives the following bytecode:
           // access flags 0x1
           public getMinY()I
           L0
            LINENUMBER 43 L0
            ALOAD 0
            INVOKEVIRTUAL io/github/opencubicchunks/cubicchunks/core/asm/transformer/CubicChunksWorldEditTransformer.getWorld ()Lnet/minecraft/world/World;
            CHECKCAST io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld
            INVOKEINTERFACE io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld.getMinHeight ()I (itf)
            IRETURN
           L1
            LOCALVARIABLE this Lio/github/opencubicchunks/cubicchunks/core/asm/transformer/CubicChunksWorldEditTransformer; L0 L1 0
            MAXSTACK = 1
            MAXLOCALS = 1
        */
        InsnList list = getMinY.instructions;
        list.clear();

        LabelNode start = new LabelNode(new Label());
        LabelNode end = new LabelNode(new Label());

        list.add(start);
        list.add(new LineNumberNode(10000, start));
        list.add(new IntInsnNode(Opcodes.ALOAD, 0));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "com/sk89q/worldedit/forge/ForgeWorld", "getWorld", "()Lnet/minecraft/world/World;", false));
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, "io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld"));
        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld", "getMinHeight", "()I", true));
        list.add(new InsnNode(Opcodes.IRETURN));
        list.add(end);

        getMinY.localVariables.clear();
        getMinY.localVariables.add(new LocalVariableNode(
                "this", "Lcom/sk89q/worldedit/forge/ForgeWorld;", null, start, end, 0));
        getMinY.maxLocals = 1;
        getMinY.maxStack = 1;
    }

    public int getMinY() {
        return 0;
    }
}
