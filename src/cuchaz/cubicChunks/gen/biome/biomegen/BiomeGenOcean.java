package cuchaz.cubicChunks.gen.biome.biomegen;

import java.util.Random;

import cuchaz.cubicChunks.gen.CubeBlocks;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BiomeGenOcean extends CubeBiomeGenBase
{
    public BiomeGenOcean(int par1)
    {
        super(par1);
        this.spawnableCreatureList.clear();
    }

    public CubeBiomeGenBase.TempCategory func_150561_m()
    {
        return CubeBiomeGenBase.TempCategory.OCEAN;
    }

    public void modifyBlocks_pre(World world, Random rand, CubeBlocks cubeBlocks, int xAbs, int yAbs, int zAbs, double var)
    {
        super.modifyBlocks_pre(world, rand, cubeBlocks, xAbs, yAbs, zAbs, var);
    }
}
