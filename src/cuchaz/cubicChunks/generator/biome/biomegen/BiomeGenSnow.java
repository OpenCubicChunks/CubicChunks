/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.generator.biome.biomegen;

import java.util.Random;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.Height;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenIcePath;
import net.minecraft.world.gen.feature.WorldGenIceSpike;
import net.minecraft.world.gen.feature.WorldGenTaiga2;

public class BiomeGenSnow extends CubeBiomeGenBase
{
    private boolean field_150615_aC;
    private WorldGenIceSpike field_150616_aD = new WorldGenIceSpike();
    private WorldGenIcePath field_150617_aE = new WorldGenIcePath(4);
    private static final String __OBFID = "CL_00000174";

    public BiomeGenSnow(int p_i45378_1_, boolean p_i45378_2_)
    {
        super(p_i45378_1_);
        this.field_150615_aC = p_i45378_2_;

        if (p_i45378_2_)
        {
            this.topBlock = Blocks.snow;
        }

        this.spawnableCreatureList.clear();
    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        if (this.field_150615_aC)
        {
            int var5;
            int var6;
            int var7;

            for (var5 = 0; var5 < 3; ++var5)
            {
                var6 = par3 + par2Random.nextInt(16) + 8;
                var7 = par4 + par2Random.nextInt(16) + 8;
                this.field_150616_aD.generate(par1World, par2Random, var6, par1World.getHeightValue(var6, var7), var7);
            }

            for (var5 = 0; var5 < 2; ++var5)
            {
                var6 = par3 + par2Random.nextInt(16) + 8;
                var7 = par4 + par2Random.nextInt(16) + 8;
                this.field_150617_aE.generate(par1World, par2Random, var6, par1World.getHeightValue(var6, var7), var7);
            }
        }

        super.decorate(par1World, par2Random, par3, par4);
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return new WorldGenTaiga2(false);
    }

    protected CubeBiomeGenBase func_150566_k()
    {
        CubeBiomeGenBase var1 = (new BiomeGenSnow(this.biomeID + 128, true)).func_150557_a(13828095, true).setBiomeName(this.biomeName + " Spikes").setEnableSnow().setTemperatureAndRainfall(0.0F, 0.5F).setHeightRange(new CubeBiomeGenBase.Height(this.minHeight + 0.1F, this.maxHeight + 0.1F));
        var1.minHeight = this.minHeight + 0.3F;
        var1.maxHeight = this.maxHeight + 0.4F;
        return var1;
    }
}
