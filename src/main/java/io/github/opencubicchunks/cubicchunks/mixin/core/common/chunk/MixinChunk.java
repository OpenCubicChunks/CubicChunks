package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IChunk {

    @Shadow @Final private World world;

    @Shadow @Final private ChunkPos pos;

    @Shadow public abstract ChunkStatus getStatus();

    // getBlockState

    @Override public boolean isEmptyBetween(int startY, int endY) {
        return false;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;", "setBlockState"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=get"
            ))
    private ChunkSection getStorage(ChunkSection[] array, int y) {
        ICube cube = ((ICubeProvider) world.getChunkProvider()).getCube(
                Coords.sectionToCube(pos.x),
                Coords.sectionToCube(y),
                Coords.sectionToCube(pos.z), getStatus(), true);
        if (cube instanceof EmptyCube) {
            return null;
        }
        assert cube != null : "cube was null when requested loading!";
        ChunkSection[] cubeSections = cube.getCubeSections();
        return cubeSections[Coords.sectionToIndex(pos.x, y, pos.z)];
    }

    @ModifyConstant(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;"},
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinHeight(int _0) {
        return Integer.MIN_VALUE;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=length"
            ))
    private int getStorage(ChunkSection[] array) {
        return Integer.MAX_VALUE;
    }

    // setBlockState

    @Redirect(method = "setBlockState",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=set"
            ))
    private void setStorage(ChunkSection[] array, int y, ChunkSection newVal) {
        ICube cube = ((ICubeProvider) world.getChunkProvider()).getCube(
                Coords.sectionToCube(pos.x),
                Coords.sectionToCube(y),
                Coords.sectionToCube(pos.z), getStatus(), true);
        if (cube instanceof EmptyCube) {
            return;
        }
        assert cube != null : "cube was null when requested loading!";
        cube.getCubeSections()[Coords.sectionToIndex(pos.x, y, pos.z)] = newVal;
    }
}
