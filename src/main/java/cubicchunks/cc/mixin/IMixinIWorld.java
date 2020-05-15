package cubicchunks.cc.mixin;

import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(IWorldReader.class)
public interface IMixinIWorld {
    @Invoker("chunkExists") boolean chunkExists(int x, int z);

    /**
     * @author Barteks2x
     */
    @Deprecated
    @Overwrite
    default boolean isAreaLoaded(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (toY >= 0 && fromY < 512) {
            fromX = fromX >> 4;
            fromZ = fromZ >> 4;
            toX = toX >> 4;
            toZ = toZ >> 4;

            for(int i = fromX; i <= toX; ++i) {
                for(int j = fromZ; j <= toZ; ++j) {
                    if (!this.chunkExists(i, j)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
