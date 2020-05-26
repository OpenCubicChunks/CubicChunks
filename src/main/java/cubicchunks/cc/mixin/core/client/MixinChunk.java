package cubicchunks.cc.mixin.core.client;

import static cubicchunks.cc.utils.Coords.blockToCube;

import com.google.common.collect.Sets;
import cubicchunks.cc.CubicChunks;
import cubicchunks.cc.chunk.IColumn;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

import javax.annotation.Nullable;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IColumn {

    @Final @Shadow private final ChunkSection[] sections = new ChunkSection[Math.round((float) CubicChunks.worldMAXHeight / 16)];

    @Shadow @Final private Map<BlockPos, TileEntity> tileEntities;

    @Shadow @Final private World world;

    @Shadow @Final public static ChunkSection EMPTY_SECTION;

    @Shadow private BiomeContainer blockBiomeArray;

    @Shadow public abstract void setHeightmap(Heightmap.Type type, long[] data);

    @Override
    public void readSection(int sectionY, @Nullable BiomeContainer biomeContainerIn, PacketBuffer packetBufferIn, CompoundNBT nbtIn,
            boolean sectionExists) {
        Sets.newHashSet(this.tileEntities.keySet()).stream().filter(p -> blockToCube(p.getY()) == sectionY)
                .forEach(this.world::removeTileEntity);

        for (TileEntity tileEntity : tileEntities.values()) {
            tileEntity.updateContainingBlockInfo();
            tileEntity.getBlockState();
        }

        ChunkSection section = this.sections[sectionY];
        if (section == EMPTY_SECTION) {
            section = new ChunkSection(sectionY << 4);
            this.sections[sectionY] = section;
        }
        if (sectionExists) {
            section.read(packetBufferIn);
        }

        if (biomeContainerIn != null) {
            this.blockBiomeArray = biomeContainerIn;
        }

        for (Heightmap.Type type : Heightmap.Type.values()) {
            String typeId = type.getId();
            if (nbtIn.contains(typeId, Constants.NBT.TAG_LONG_ARRAY)) {
                this.setHeightmap(type, nbtIn.getLongArray(typeId));
            }
        }

        for (TileEntity tileentity : this.tileEntities.values()) {
            tileentity.updateContainingBlockInfo();
        }
    }
}
