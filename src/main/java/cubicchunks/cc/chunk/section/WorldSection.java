package cubicchunks.cc.chunk.section;

import cubicchunks.cc.chunk.Coords;
import cubicchunks.cc.chunk.biome.SectionBiomeContainer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;

import java.util.HashMap;

public class WorldSection extends ChunkSection {
    private final SectionPos sectionPos;

    private final HashMap<BlockPos, TileEntity> tileEntities = new HashMap<>();
    private final ClassInheritanceMultiMap<Entity> entities = new ClassInheritanceMultiMap<>(Entity.class);

    private SectionBiomeContainer sectionBiomeContainer;

    public WorldSection(SectionPos pos) {
        super(Coords.cubeToMinBlock(pos.getY()), (short)0, (short)0, (short)0);
        this.sectionPos = pos;
    }

    public void setSectionBiomeContainer(SectionBiomeContainer biomes)
    {
        this.sectionBiomeContainer = biomes;
    }
}
