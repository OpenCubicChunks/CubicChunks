package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.block.BlockBeacon$1")
public class MixinBlockBeaconAsyncUpdate {

    // synthetic fields
    @Shadow(remap = false, aliases = "field_180358_a") @Final World val$worldIn;
    @Shadow(remap = false, aliases = "field_180357_b") @Final BlockPos val$glassPos;

    @Inject(method = "run", at = @At("HEAD"))
    private void runCubicChunks(CallbackInfo ci) {
        if (!((ICubicWorld) val$worldIn).isCubicWorld()) {
            return;
        }
        ci.cancel();

        final int blockX = val$glassPos.getX();
        final int blockZ = val$glassPos.getZ();
        int blockY = val$glassPos.getY();

        final int cubeX = Coords.blockToCube(blockX);
        final int cubeZ = Coords.blockToCube(blockZ);
        int cubeY = Coords.blockToCube(val$glassPos.getY());

        ICubeProviderServer cubeProvider = ((ICubicWorldServer) val$worldIn).getCubeCache();
        // get cached is relatively safe to do off thread
        // still not good but nothing can be done about it
        ICube cube = cubeProvider.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.GET_CACHED);
        // keep going until we hit unloaded cube or surface
        while (cube != null) {
            final BlockPos blockpos = new BlockPos(blockX, blockY, blockZ);

            if (!cube.getColumn().canSeeSky(blockpos)) {
                break;
            }

            IBlockState block = cube.getBlockState(blockpos);

            if (block.getBlock() == Blocks.BEACON) {
                ((WorldServer) val$worldIn).addScheduledTask(() -> {
                    TileEntity tileentity = val$worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof TileEntityBeacon) {
                        ((TileEntityBeacon) tileentity).updateBeacon();
                        val$worldIn.addBlockEvent(blockpos, Blocks.BEACON, 1, 0);
                    }
                });
            }

            blockY--;
            cubeY = Coords.blockToCube(blockY);
            cube = cubeProvider.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.GET_CACHED);
        }
    }
}
