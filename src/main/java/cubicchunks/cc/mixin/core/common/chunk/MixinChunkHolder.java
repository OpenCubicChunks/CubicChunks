package cubicchunks.cc.mixin.core.common.chunk;

import cubicchunks.cc.chunk.ICubeHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements ICubeHolder {

    @Shadow public abstract ChunkPos getPosition();

    private SectionPos sectionPos;

    @Override
    public void setYPos(int yPos) { //Whenever ChunkHolder is instantiated this should be called to finish the construction of the object
        this.sectionPos = SectionPos.of(getPosition().x, yPos, getPosition().z);
    }

    @Override
    public int getYPos()
    {
        return this.sectionPos.getY();
    }

    @Override
    public SectionPos getSectionPos() {
        return sectionPos;
    }
}
