package cubicchunks.worldgen.generator.custom.structure.feature;

import cubicchunks.util.CubePos;
import cubicchunks.util.XYZAddressable;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import net.minecraft.world.World;

public interface ICubicStructureStart extends XYZAddressable {
    int getChunkPosY();

    // internal use instead of constructor argument
    void initCubic(World world, CustomGeneratorSettings conf, int cubeY);

    CubePos getCubePos();
}
