package cubicchunks.asm.mixin.fixes.common;

import static cubicchunks.asm.JvmNames.CHUNK_GET_TOP_FILLED_SEGMENT;

import cubicchunks.asm.JvmNames;
import cubicchunks.world.column.IColumn;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(TileEntityEndGateway.class)
public class MixinTileEntityEndGateway {

    @Redirect(method = "findExitPortal", at = @At(value = "INVOKE", target = CHUNK_GET_TOP_FILLED_SEGMENT))
    private int getChunkTopFilledSegmentExitFromPortal(Chunk chunk) {
        int top = chunk.getTopFilledSegment();
        if (top < 0) {
            return 0;
        }
        return top;
    }

    @Redirect(method = "findSpawnpointInChunk", at = @At(value = "INVOKE", target = CHUNK_GET_TOP_FILLED_SEGMENT))
    private static int getChunkTopFilledSegmentFindSpawnpoint(Chunk chunk) {
        int top = chunk.getTopFilledSegment();
        if (top < 0) {
            return 0;
        }
        return top;
    }

    /**
     * @author Barteks2x
     * @reason Make it generate cubes with cubic chunks so that it's filled with blocks
     */
    @Overwrite
    private static Chunk getChunk(World world, Vec3d pos) {
        Chunk chunk = world.getChunkFromChunkCoords(MathHelper.floor(pos.xCoord / 16.0D), MathHelper.floor(pos.zCoord / 16.0D));
        if (((IColumn) chunk).getCubicWorld().isCubicWorld()) {
            for (int cubeY = 0; cubeY < 16; cubeY++) {
                ((IColumn) chunk).getCube(cubeY);// load the cube
            }
        }
        return chunk;
    }
}
