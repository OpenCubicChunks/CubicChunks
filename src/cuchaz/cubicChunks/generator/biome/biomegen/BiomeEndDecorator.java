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

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenSpikes;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeEndDecorator extends CubeBiomeDecorator
{
    protected WorldGenerator spikeGen;

    public BiomeEndDecorator()
    {
        this.spikeGen = new WorldGenSpikes(Blocks.end_stone);
    }

    protected void func_150513_a(CubeBiomeGenBase p_150513_1_)
    {
        this.generateOres();

        /*if (this.randomGenerator.nextInt(5) == 0)
        {
            int var2 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            int var3 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            int var4 = this.currentWorld.getTopSolidOrLiquidBlock(var2, var3);
            this.spikeGen.generate(this.currentWorld, this.randomGenerator, var2, var4, var3);
        }

        if (this.cubeX == 0 && this.cubeZ == 0)
        {
            EntityDragon var5 = new EntityDragon(this.currentWorld);
            var5.setLocationAndAngles(0.0D, 128.0D, 0.0D, this.randomGenerator.nextFloat() * 360.0F, 0.0F);
            this.currentWorld.spawnEntityInWorld(var5);
        }*/
    }
}
