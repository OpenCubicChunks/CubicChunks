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
package cubicchunks.asm.mixin.core.client;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

/**
 * Fixes renderEntities crashing when rendering cubes
 * that are not at existing array index in chunk.getEntityLists(),
 * <p>
 * Allows to render cubes outside of 0..256 height range.
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

	private BlockPos position;

	@Shadow private int renderDistanceChunks;

	@Shadow private ViewFrustum viewFrustum;

	/**
	 * This allows to get the Y position by injecting itself directly before call to chunk.getEntityLists
	 */
	@Group(name = "renderEntitiesFix", max = 3)
	@Inject(
			method = "renderEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/multiplayer/WorldClient;getChunkFromBlockCoords(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"
			),
			locals = LocalCapture.CAPTURE_FAILHARD,
			require = 1
	)
	public void onGetPosition(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci,
	                          int pass, double d0, double d1, double d2,
	                          Entity entity, double d3, double d4, double d5,
	                          List list, List list1, List list2, Iterator var21,
	                          RenderGlobal.ContainerLocalRenderInformation info) {
		ICubicWorld world = (ICubicWorld) info.renderChunk.getWorld();
		if (world.isCubicWorld()) {
			this.position = info.renderChunk.getPosition();
		} else {
			this.position = null;
		}
	}

	/**
	 * After chunk.getEntityLists() renderGlobal needs to get correct element of the arrsy
	 * The array element number is calculated using renderChunk.getPosition().getY() / 16.
	 * getY() is redirected to this method to always return 0.
	 * <p>
	 * Then chunk.getEntityLists is redirected to a method that returns a 1-element array.
	 */
	@Group(name = "renderEntitiesFix")
	@Redirect(
			method = "renderEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/math/BlockPos;getY()I"
			),
			require = 1
	)
	public int getRenderChunkYPos(BlockPos pos) {
		if (this.position != null) {
			return 0;
		}
		return pos.getY();
	}

	/**
	 * Return a 1-element array for Cubic Chunks code,
	 * or original chunk.getEntityLists if not cubic chunks world.
	 */
	@Group(name = "renderEntitiesFix")
	@Redirect(
			method = "renderEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/chunk/Chunk;getEntityLists()[Lnet/minecraft/util/ClassInheritanceMultiMap;"
			),
			require = 1
	)
	public ClassInheritanceMultiMap<Entity>[] getEntityList(Chunk chunk) {
		if (position == null) {
			return chunk.getEntityLists();
		}
		Column column = (Column) chunk;
		int cubeY = Coords.blockToCube(position.getY());

		Cube cube = column.getCube(cubeY);
		if (cube == null) {
			return new ClassInheritanceMultiMap[]{
					new ClassInheritanceMultiMap(Entity.class)
			};
		}
		return new ClassInheritanceMultiMap[]{
				cube.getEntityContainer().getEntitySet()
		};
	}

	@Overwrite
	private RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
		BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);
		return MathHelper.abs_int(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks*16 ? null :
				MathHelper.abs_int(playerPos.getY() - blockpos.getY()) > this.renderDistanceChunks*16 ? null :
						MathHelper.abs_int(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks*16 ? null :
								this.viewFrustum.getRenderChunk(blockpos);
	}
}
