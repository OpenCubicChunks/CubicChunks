package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Chunk.class)
public class MixinChunk {

    @Shadow @Final public static ChunkSection EMPTY_SECTION;
    @Shadow @Final private ChunkPos pos;
    @Shadow @Final private ChunkSection[] sections;
    private Int2ObjectMap<Cube> cubeMap;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;[Lnet/minecraft/world/biome/Biome;"
        + "Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/ITickList;Lnet/minecraft/world/ITickList;"
        + "J[Lnet/minecraft/world/chunk/ChunkSection;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void afterConstruct(World world, ChunkPos pos, Biome[] biomes, UpgradeData upgradeData, ITickList<Block> toTick,
        ITickList<Fluid> toTickFluids, long p_i49946_7_, ChunkSection[] sections, Consumer<Chunk> p_i49946_10_, CallbackInfo ci) {
        cubeMap = new Int2ObjectOpenHashMap<>();

        try {
            if (sections != null) {
                for (int i = 0; i < sections.length; i++) {
                    this.setChunkSection(this.sections, i, sections[i]);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Redirect(method = {
        "func_217326_a",
        "setBlockState",
        "getFluidState(III)Lnet/minecraft/fluid/IFluidState;",
        "getBlockState"
    }, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=get"
    ))
    private ChunkSection getChunkSection(ChunkSection[] sections, int index) {
        try {
            Cube cube = cubeMap.get(index);
            return cube == null ? EMPTY_SECTION : cube.getSection();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Redirect(method = {"setBlockState", "func_217326_a"}, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=set"
    ))
    private void setChunkSection(ChunkSection[] sections, int index, ChunkSection newValue) {
        try {
            if (index >= 0 && index < sections.length) {
                sections[index] = newValue;
            }
            //System.out.println("setChunkSection!");
            cubeMap.computeIfAbsent(index, idx -> new Cube(this.pos.x, idx, this.pos.z)).setSection(newValue);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Redirect(method = {"getFluidState(III)Lnet/minecraft/fluid/IFluidState;", "getBlockState"}, at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
        args = "array=length"
    ))
    private int getChunkSectionCount(ChunkSection[] sections) {
        return Integer.MAX_VALUE / 32;
    }
}
