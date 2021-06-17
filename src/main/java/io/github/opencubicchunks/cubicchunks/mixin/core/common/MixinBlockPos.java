package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockPos.class)
public class MixinBlockPos {

    //TODO: This is a temporary fix to achieve taller worlds(by stopping the light engine from NPEing. Will be removed in the future.
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 30000000))
    private static int getMaxWorldSizeXZ(int size) {
        int defaultValue = 1000000;
        try (FileReader reader = new FileReader(CubicChunks.CONFIG_PATH.resolve("blockpos.properties").toFile())) {
            Properties p = new Properties();
            p.load(reader);

            try {
                String xzsize = p.getProperty("xzsize");
                CubicChunks.LOGGER.info("BlockPos XZ size: " + xzsize);
                return Integer.parseInt(xzsize);
            } catch (NumberFormatException e) {
            }
        } catch (IOException e) {
        }

        CubicChunks.LOGGER.info("BlockPos XZ size: " + defaultValue);
        return defaultValue;
    }
}
