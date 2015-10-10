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

import com.google.common.base.Throwables;
import cubicchunks.asm.transformer.*;
import cubicchunks.util.ReflectionUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static cubicchunks.asm.Mappings.*;

public class CubicChunksTransformer implements IClassTransformer{

	private List<Transformer> transformers = new ArrayList<>();

	public CubicChunksTransformer() {
		add(WorldHeightCheckReplacement.class, WORLD, WORLD_IS_VALID);
		add(WorldHeightCheckReplacement.class, WORLD, WORLD_GET_LIGHT);
		add(WorldHeightCheckReplacement.class, WORLD, WORLD_GET_LIGHT_CHECK);
		add(WorldHeightCheckReplacementSpecial.class, WORLD, WORLD_GET_LIGHT_FOR);
		add(WorldHeightCheckReplacementSpecial.class, WORLD, WORLD_GET_LIGHT_FROM_NEIGHBORS_FOR);

		add(ViewFrustumSetCountChunks.class, VIEW_FRUSTUM, VIEW_FRUSTUM_SET_COUNT_CHUNKS);
		add(ViewFrustumGetRenderChunk.class, VIEW_FRUSTUM, VIEW_FRUSTUM_GET_RENDER_CHUNK);
		add(ViewFrustumUpdateChunkPositions.class, VIEW_FRUSTUM, VIEW_FRUSTUM_UPDATE_CHUNK_POSITIONS);
		add(ChunkCacheGetBlockState.class, CHUNK_CACHE, CHUNK_CACHE_GET_BLOCK_STATE);
		add(RenderGlobalGetRenderChunkOffset.class, RENDER_GLOBAL, RENDER_GLOBAL_GET_RENDER_CHUNK_OFFSET);
		add(RegionRenderCacheGetBlockStateRaw.class, REGION_RENDER_CACHE, REGION_RENDER_CACHE_GET_BLOCK_STATE_RAW);
		add(RenderGlobalRenderEntities.class, RENDER_GLOBAL, RENDER_GLOBAL_RENDER_ENTITIES);
	}

	private void add(Class<? extends MethodVisitor> methodTransformer, String jvmClassName, String methodName) {
		MethodHandle methodVisitorConstr = ReflectionUtil.getConstructorMethodHandle(methodTransformer, MethodVisitor.class);

		MethodHandle classVisitor = MethodHandles.insertArguments(MethodClassVisitor.HANDLE, 1, methodVisitorConstr, methodName);

		this.transformers.add(new Transformer(classVisitor, jvmClassName));
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(bytes == null) return bytes;

		transformedName = transformedName.replace(".", "/");

		ClassReader reader = null;
		ClassVisitor visitor = null;

		ClassWriter writer = null;
		for(Transformer t : transformers) {
			if(t.shouldTransform(transformedName)) {
				if(reader == null) {
					assert visitor == null && writer == null;
					reader = new ClassReader(bytes);
					writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
					visitor = writer;
				}
				visitor = t.newVisitor(visitor);
			}
		}
		if(visitor != null) {
			reader.accept(visitor, 0);
			return writer.toByteArray();
		}
		return bytes;
	}

	private static final class Transformer {

		private final MethodHandle constr;
		private final String name;

		private Transformer(MethodHandle visitorConstr, String name) {
			this.constr = visitorConstr;
			this.name = name;
		}

		private ClassVisitor newVisitor(ClassVisitor p) {
			try {
				return (ClassVisitor) constr.invoke(p);
			} catch (Throwable throwable) {
				throw Throwables.propagate(throwable);
			}
		}

		private boolean shouldTransform(String name) {
			return this.name.equals(name);
		}
	}

	public static final class MethodClassVisitor extends ClassVisitor {

		private static final MethodHandle HANDLE = ReflectionUtil.getConstructorMethodHandle(MethodClassVisitor.class, ClassVisitor.class, MethodHandle.class, String.class);

		private MethodHandle methodVisitorConstr;
		private String methodName;

		public MethodClassVisitor(ClassVisitor cv, MethodHandle methodVisitorConstr, String methodName) {
			super(Opcodes.ASM4, cv);

			this.methodVisitorConstr = methodVisitorConstr;
			this.methodName = methodName;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

			if (name.equals(this.methodName)) {
				try {
					MethodVisitor newMV = (MethodVisitor) methodVisitorConstr.invoke(mv);
					return newMV;
				} catch (Throwable throwable) {
					throw Throwables.propagate(throwable);
				}
			} else {
				return mv;
			}
		}
	}
}
