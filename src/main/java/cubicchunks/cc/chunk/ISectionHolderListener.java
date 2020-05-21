package cubicchunks.cc.chunk;

import net.minecraft.util.math.SectionPos;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public interface ISectionHolderListener {
    void onUpdateSectionLevel(SectionPos pos, IntSupplier p_219066_2_, int p_219066_3_, IntConsumer p_219066_4_);
}
