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

<<<<<<< HEAD:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenMushroomIsland.java
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenMushroomIsland.java
=======
import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenMushroomIsland.java
=======
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
>>>>>>> 69175bb... - Refactored package structure again to remove /java/:src/main/cubicchunks/generator/biome/biomegen/BiomeGenMushroomIsland.java
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.init.Blocks;

public class BiomeGenMushroomIsland extends CubeBiomeGenBase
{
	@SuppressWarnings("unchecked")
	public BiomeGenMushroomIsland( int par1 )
	{
		super( par1 );

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn( -100 );
		cfg.flowersPerColumn( -100 );
		cfg.grassPerColumn( -100 );
		cfg.mushroomsPerColumn( 1 );
		cfg.bigMushroomsPerColumn( 1 );
		
		this.topBlock = Blocks.mycelium;
		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityMooshroom.class, 8, 4, 8 ) );
	}
}
