package cubicchunks.cc.chunk.ticket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ChunkManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CubeTaskPriorityQueue<T> {
   public static final int field_219419_a = ChunkManager.MAX_LOADED_LEVEL + 2;
   private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> field_219420_b = IntStream.range(0, field_219419_a).mapToObj((p_219415_0_) -> new Long2ObjectLinkedOpenHashMap<List<Optional<T>>>()).collect(Collectors.toList());
   private volatile int field_219421_c = field_219419_a;
   private final String field_219422_d;
   private final LongSet field_219423_e = new LongOpenHashSet();
   private final int field_219424_f;

   public CubeTaskPriorityQueue(String p_i50714_1_, int p_i50714_2_) {
      this.field_219422_d = p_i50714_1_;
      this.field_219424_f = p_i50714_2_;
   }

   protected void func_219407_a(int p_219407_1_, SectionPos pos, int p_219407_3_) {
      if (p_219407_1_ < field_219419_a) {
         Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.field_219420_b.get(p_219407_1_);
         List<Optional<T>> list = long2objectlinkedopenhashmap.remove(pos.asLong());
         if (p_219407_1_ == this.field_219421_c) {
            while (this.field_219421_c < field_219419_a && this.field_219420_b.get(this.field_219421_c).isEmpty()) {
               ++this.field_219421_c;
            }
         }

         if (list != null && !list.isEmpty()) {
            this.field_219420_b.get(p_219407_3_).computeIfAbsent(pos.asLong(), (p_219411_0_) -> {
               return Lists.newArrayList();
            }).addAll(list);
            this.field_219421_c = Math.min(this.field_219421_c, p_219407_3_);
         }

      }
   }

   protected void func_219412_a(Optional<T> p_219412_1_, long p_219412_2_, int p_219412_4_) {
      this.field_219420_b.get(p_219412_4_).computeIfAbsent(p_219412_2_, (p_219410_0_) -> {
         return Lists.newArrayList();
      }).add(p_219412_1_);
      this.field_219421_c = Math.min(this.field_219421_c, p_219412_4_);
   }

   protected void func_219416_a(long p_219416_1_, boolean p_219416_3_) {
      for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap : this.field_219420_b) {
         List<Optional<T>> list = long2objectlinkedopenhashmap.get(p_219416_1_);
         if (list != null) {
            if (p_219416_3_) {
               list.clear();
            } else {
               list.removeIf((p_219413_0_) -> !p_219413_0_.isPresent());
            }

            if (list.isEmpty()) {
               long2objectlinkedopenhashmap.remove(p_219416_1_);
            }
         }
      }

      while (this.field_219421_c < field_219419_a && this.field_219420_b.get(this.field_219421_c).isEmpty()) {
         ++this.field_219421_c;
      }

      this.field_219423_e.remove(p_219416_1_);
   }

   private Runnable func_219418_a(long p_219418_1_) {
      return () -> {
         this.field_219423_e.add(p_219418_1_);
      };
   }

   @Nullable
   public Stream<Either<T, Runnable>> func_219417_a() {
      if (this.field_219423_e.size() >= this.field_219424_f) {
         return null;
      } else if (this.field_219421_c >= field_219419_a) {
         return null;
      } else {
         int i = this.field_219421_c;
         Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.field_219420_b.get(i);
         long j = long2objectlinkedopenhashmap.firstLongKey();

         List<Optional<T>> list;
         for (list = long2objectlinkedopenhashmap.removeFirst(); this.field_219421_c < field_219419_a && this.field_219420_b.get(this.field_219421_c).isEmpty(); ++this.field_219421_c) {
            ;
         }

         return list.stream().map((p_219408_3_) -> {
            return p_219408_3_.<Either<T, Runnable>>map(Either::left).orElseGet(() -> {
               return Either.right(this.func_219418_a(j));
            });
         });
      }
   }

   public String toString() {
      return this.field_219422_d + " " + this.field_219421_c + "...";
   }

   @VisibleForTesting
   LongSet func_225414_b() {
      return new LongOpenHashSet(this.field_219423_e);
   }
}
