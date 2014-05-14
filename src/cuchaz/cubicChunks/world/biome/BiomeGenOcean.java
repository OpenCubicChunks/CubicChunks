package cuchaz.cubicChunks.world.biome;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BiomeGenOcean extends CubeBiomeGenBase
{
    private static final String __OBFID = "CL_00000179";

    public BiomeGenOcean(int par1)
    {
        super(par1);
        this.spawnableCreatureList.clear();
    }

    public CubeBiomeGenBase.TempCategory func_150561_m()
    {
        return CubeBiomeGenBase.TempCategory.OCEAN;
    }

    public void modifyBlocks_pre(World world, Random rand, Block[] blocks, byte[] meta, int xAbs, int yAbs, int zAbs, double var)
    {
        super.modifyBlocks_pre(world, rand, blocks, meta, xAbs, yAbs, zAbs, var);
    }
}
