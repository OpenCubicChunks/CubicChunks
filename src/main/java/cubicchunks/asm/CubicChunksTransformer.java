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

import javax.annotation.Resource;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cubicchunks.asm.Mappings.*;

@Resource
public class CubicChunksTransformer implements IClassTransformer{

	private List<Transformer> transformers = new ArrayList<>();

	public CubicChunksTransformer() {
		//This transformation makes the World see blocks outside of 0..255 height
		//add(WorldHeightCheckReplacement.class, WORLD, WORLD_IS_VALID);
		//these 4 transformations allow the internals of Minecraft code to see non-default light values outside of 0..255 height
		//add(WorldHeightCheckReplacement.class, WORLD, WORLD_GET_LIGHT, WORLD_GET_LIGHT_DESC);
		//add(WorldHeightCheckReplacement.class, WORLD, WORLD_GET_LIGHT_CHECK, WORLD_GET_LIGHT_CHECK_DESC);
		//add(WorldHeightCheckReplacementSpecial.class, WORLD, WORLD_GET_LIGHT_FOR);
		//add(WorldHeightCheckReplacementSpecial.class, WORLD, WORLD_GET_LIGHT_FROM_NEIGHBORS_FOR);
		//World.isAreaLoaded is used to check if some things can be updated (like light). If it returns false - update doesn't happen. This fixes it
		//Note: there are some methods that use it incorrectly ie. by checking it at some constant height (usually 0).
		//add(WorldIsAreaLoadedReplace.class, WORLD, WORLD_IS_AREA_LOADED_IIIIIIZ, WORLD_IS_AREA_LOADED_IIIIIIZ_DESC);
		//updateEntityWithOptionalForce uses isAreaLoaded with height of 0. Change it to use entity position.
		//add(WorldEntityUpdateFix.class, WORLD, WORLD_UPDATE_ENTITY_WITH_OPTIONAL_FORCE);

		//ChunkCache is used by some AI code and (as subclass of ChunkCache) - rendering code.
		//getBlockState is used only in AI code but the same transformation as for other 2 methods works for it
		//add(ChunkCacheHeightCheckReplacement.class, CHUNK_CACHE, CHUNK_CACHE_GET_BLOCK_STATE);
		//these 2 methods are actually used by rendering code. Moving hardcoded limits in these methods
		//makes it possible to actually see non-default light values outside of 0..255
		//add(ChunkCacheHeightCheckReplacement.class, CHUNK_CACHE, CHUNK_CACHE_GET_LIGHT_FOR_EXT);
		//add(ChunkCacheHeightCheckReplacement.class, CHUNK_CACHE, CHUNK_CACHE_GET_LIGHT_FOR);

		//this transformation makes render distance a cube (height dependent on render distance)
		add(ViewFrustumSetCountChunks.class, VIEW_FRUSTUM, VIEW_FRUSTUM_SET_COUNT_CHUNKS);
		//inserts call to modified getRenderChunk method that works with cubic chunks before the actual method
		//without this transformation player won't see any blocks outside of 0..255 height
		add(ViewFrustumGetRenderChunk.class, VIEW_FRUSTUM, VIEW_FRUSTUM_GET_RENDER_CHUNK);
		//inserts call to modified method before the actual code.
		//loads renderers above as player moves up and loads renderers below as player moves down
		//without this transformation only blocks 0..renderDistance*16 are rendered (or 0..255 if SetCountChunks transformation is disabled)
		add(ViewFrustumUpdateChunkPositions.class, VIEW_FRUSTUM, VIEW_FRUSTUM_UPDATE_CHUNK_POSITIONS);
		//this transformation removes hardcoded limits from method that returns neighbor renderer (misleading name)
		//without this transformation cubes outside of 0..255 have fully opaque black borders
		add(RenderGlobalGetRenderChunkOffset.class, RENDER_GLOBAL, RENDER_GLOBAL_GET_RENDER_CHUNK_OFFSET);
		//removes hardcoded limits from subclass of ChunkCache. Allows renderers to "see" blocks outside of 0..255 height
		add(RegionRenderCacheGetBlockStateRaw.class, REGION_RENDER_CACHE, REGION_RENDER_CACHE_GET_BLOCK_STATE_RAW);
		//fixes crash when rendering entities when renderers have chunk position outside 0..15 (inclusive) range
		//replaces usage of entityStorage array with method call that returns correct entit list
		//add(RenderGlobalRenderEntities.class, RENDER_GLOBAL, RENDER_GLOBAL_RENDER_ENTITIES);
		//fixes entities dying below y=-64
		add(EntityChangeKillHeight.class, ENTITY, ENTITY_ON_ENTITY_UPDATE);

		//changes Integrated server build limit.
		addConstr(IntegratedServerHeightReplacement.class, INTEGRATED_SERVER, CONSTR_INTEGRATED_SERVER);
	}

	private void add(Class<? extends AbstractMethodTransformer> methodTransformer, String jvmClassName, String methodName) {
		MethodHandle methodVisitorConstr = ReflectionUtil.getConstructorMethodHandle(methodTransformer, MethodVisitor.class);

		MethodHandle classVisitor = MethodHandles.insertArguments(MethodClassVisitor.HANDLE, 1, methodVisitorConstr, methodName, null);

		this.transformers.add(new Transformer(classVisitor, jvmClassName));
	}

	private void addConstr(Class<? extends AbstractMethodTransformer> methodTransformer, String jvmClassName, String desc) {
		add(methodTransformer, jvmClassName, "<init>", desc);
	}

	private void add(Class<? extends AbstractMethodTransformer> methodTransformer, String jvmClassName, String methodName, String desc) {
		MethodHandle methodVisitorConstr = ReflectionUtil.getConstructorMethodHandle(methodTransformer, MethodVisitor.class);

		MethodHandle classVisitor = MethodHandles.insertArguments(MethodClassVisitor.HANDLE, 1, methodVisitorConstr, methodName, desc);

		this.transformers.add(new Transformer(classVisitor, jvmClassName));
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		//if(true) return bytes;
		if(bytes == null) return bytes;

		transformedName = transformedName.replace(".", "/");

		ClassReader reader = null;
		ClassVisitor visitor = null;

		ClassWriter writer = null;
		Set<MethodClassVisitor> visitors = new HashSet<>();
		for(Transformer t : transformers) {
			if(t.shouldTransform(transformedName)) {
				if(reader == null) {
					assert visitor == null && writer == null;
					reader = new ClassReader(bytes);
					writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
					visitor = writer;
				}
				visitor = t.newVisitor(visitor);
				if(visitor instanceof MethodClassVisitor) {
					visitors.add((MethodClassVisitor) visitor);
				}
			}
		}
		if(visitor != null) {
			reader.accept(visitor, 0);
			for(MethodClassVisitor mcv : visitors) {
				if(!mcv.isSuccessful()) {
					throw new RuntimeException("Transformation failed for class " + transformedName + " for method: " + mcv);
				}
			}
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

		private static final MethodHandle HANDLE = ReflectionUtil.getConstructorMethodHandle(MethodClassVisitor.class, ClassVisitor.class, MethodHandle.class, String.class, String.class);

		private MethodHandle methodVisitorConstr;
		private String methodName;
		private String methodDescriptor;
		private AbstractMethodTransformer newMV;

		public MethodClassVisitor(ClassVisitor cv, MethodHandle methodVisitorConstr, String methodName, String methodDescriptor) {
			super(Opcodes.ASM4, cv);

			this.methodVisitorConstr = methodVisitorConstr;
			this.methodName = methodName;
			this.methodDescriptor = methodDescriptor;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

			if (name.equals(this.methodName) && (methodDescriptor == null || desc.equals(methodDescriptor))) {
				try {
					this.newMV = (AbstractMethodTransformer) methodVisitorConstr.invoke(mv);
					return newMV;
				} catch (Throwable throwable) {
					throw Throwables.propagate(throwable);
				}
			} else {
				return mv;
			}
		}

		public boolean isSuccessful() {
			if(newMV == null) {
				return false;
			}
			return this.newMV.isSuccessful();
		}

		@Override
		public String toString() {
			return String.format("%s: %s", methodName, methodDescriptor);
		}
	}
}
