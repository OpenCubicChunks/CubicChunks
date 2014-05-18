package cuchaz.cubicChunks.gen.biome.biomegen;

import java.util.Random;

import cuchaz.cubicChunks.gen.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;

public class BiomeGenPlains extends CubeBiomeGenBase
{
    protected boolean field_150628_aC;

    protected BiomeGenPlains(int biomeID)
    {
        super(biomeID);
        this.setTemperatureAndRainfall(0.8F, 0.4F);
        this.setHeightRange(field_150593_e);
        this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityHorse.class, 5, 2, 6));
        this.theBiomeDecorator.treesPerChunk = -999;
        this.theBiomeDecorator.flowersPerChunk = 4;
        this.theBiomeDecorator.grassPerChunk = 10;
    }

    public String spawnFlower(Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_)
    {
        double var5 = field_150606_ad.func_151601_a((double)p_150572_2_ / 200.0D, (double)p_150572_4_ / 200.0D);
        int var7;

        if (var5 < -0.8D)
        {
            var7 = p_150572_1_.nextInt(4);
            return BlockFlower.field_149859_a[4 + var7];
        }
        else if (p_150572_1_.nextInt(3) > 0)
        {
            var7 = p_150572_1_.nextInt(3);
            return var7 == 0 ? BlockFlower.field_149859_a[0] : (var7 == 1 ? BlockFlower.field_149859_a[3] : BlockFlower.field_149859_a[8]);
        }
        else
        {
            return BlockFlower.field_149858_b[0];
        }
    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        double var5 = field_150606_ad.func_151601_a((double)(par3 + 8) / 200.0D, (double)(par4 + 8) / 200.0D);
        int var7;
        int var8;
        int var9;
        int var10;

        if (var5 < -0.8D)
        {
            this.theBiomeDecorator.flowersPerChunk = 15;
            this.theBiomeDecorator.grassPerChunk = 5;
        }
        else
        {
            this.theBiomeDecorator.flowersPerChunk = 4;
            this.theBiomeDecorator.grassPerChunk = 10;
            field_150610_ae.func_150548_a(2);

            for (var7 = 0; var7 < 7; ++var7)
            {
                var8 = par3 + par2Random.nextInt(16) + 8;
                var9 = par4 + par2Random.nextInt(16) + 8;
                var10 = par2Random.nextInt(par1World.getHeightValue(var8, var9) + 32);
                field_150610_ae.generate(par1World, par2Random, var8, var10, var9);
            }
        }

        if (this.field_150628_aC)
        {
            field_150610_ae.func_150548_a(0);

            for (var7 = 0; var7 < 10; ++var7)
            {
                var8 = par3 + par2Random.nextInt(16) + 8;
                var9 = par4 + par2Random.nextInt(16) + 8;
                var10 = par2Random.nextInt(par1World.getHeightValue(var8, var9) + 32);
                field_150610_ae.generate(par1World, par2Random, var8, var10, var9);
            }
        }

        super.decorate(par1World, par2Random, par3, par4);
    }

    protected CubeBiomeGenBase func_150566_k()
    {
        BiomeGenPlains var1 = new BiomeGenPlains(this.biomeID + 128);
        var1.setBiomeName("Sunflower Plains");
        var1.field_150628_aC = true;
        var1.setColor(9286496);
        var1.field_150609_ah = 14273354;
        return var1;
    }
}
