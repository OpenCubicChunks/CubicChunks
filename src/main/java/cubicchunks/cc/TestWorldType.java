package cubicchunks.cc;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProviderSettings;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.ChunkGeneratorType;
import net.minecraft.world.gen.OverworldGenSettings;

public class TestWorldType extends WorldType {
    public TestWorldType() {
        super("cubic");
    }
    public static final ChunkGeneratorType<OverworldGenSettings, CCOverworldChunkGenerator> SURFACE = new ChunkGeneratorType<>(CCOverworldChunkGenerator::new, true ,OverworldGenSettings::new);

    @Override
    public ChunkGenerator<?> createChunkGenerator(World world) {
        ChunkGeneratorType<OverworldGenSettings, CCOverworldChunkGenerator> chunkgeneratortype4 = SURFACE;
        if(world.dimension.getType() == DimensionType.OVERWORLD) {
            return SURFACE.create(world, new OverworldBiomeProvider(new OverworldBiomeProviderSettings(world.getWorldInfo())), new OverworldGenSettings());
        }
        return super.createChunkGenerator(world);
    }
}
