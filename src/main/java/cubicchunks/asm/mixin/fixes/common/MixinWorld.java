package cubicchunks.asm.mixin.fixes.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.util.Coords;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import static cubicchunks.asm.JvmNames.CHUNK_IS_POPULATED;
/**
 * Currently only fixes markAndNotifyBlock checking if chunk is populated instead of checking cubes.
 */
@Mixin(World.class)
public class MixinWorld {

	// note: markAndNotifyBlock has @Nullable on chunk, this will never be null here,
	// because this is the chunk on which isPopulated is called
	@Redirect(method = "markAndNotifyBlock", at = @At(value = "INVOKE", target = CHUNK_IS_POPULATED))
	public boolean markNotifyBlock_CubeCheck(Chunk _this,
	                                         BlockPos pos, Chunk chunk, IBlockState oldstate,
	                                         IBlockState newState, int flags) {
		if(!(chunk instanceof Column)) {
			// vanilla compatibility
			return chunk.isPopulated();
		}
		Column column = (Column) chunk;
		Cube cube = column.getCube(Coords.blockToCube(pos.getY()));
		return cube.isFullyPopulated();
	}
}
