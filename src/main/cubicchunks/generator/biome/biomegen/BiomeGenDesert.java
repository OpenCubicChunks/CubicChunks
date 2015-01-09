/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package cubicchunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenDesertWellsCube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;

public class BiomeGenDesert extends CubeBiomeGenBase
{
	public BiomeGenDesert( int par1 )
	{
		super( par1 );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.fillerBlock = Blocks.sand;
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		cfg.treesPerColumn( DISABLE );
		cfg.deadBushPerColumn( 2 );
		cfg.reedsPerColumn( 50 );
		cfg.cactiPerColumn( 10 );
		this.spawnableCreatureList.clear();
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		super.decorate( world, rand, x, y, z );
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		gen.generateAtSurface( new WorldGenDesertWellsCube(), 1, 0.001 );
	}
}
