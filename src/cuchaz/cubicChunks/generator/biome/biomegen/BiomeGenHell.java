package cuchaz.cubicChunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;

public class BiomeGenHell extends CubeBiomeGenBase
{
    public BiomeGenHell(int par1)
    {
        super(par1);
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.spawnableMonsterList.add(new CubeBiomeGenBase.SpawnListEntry(EntityGhast.class, 50, 4, 4));
        this.spawnableMonsterList.add(new CubeBiomeGenBase.SpawnListEntry(EntityPigZombie.class, 100, 4, 4));
        this.spawnableMonsterList.add(new CubeBiomeGenBase.SpawnListEntry(EntityMagmaCube.class, 1, 4, 4));
    }
}
