package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;

public class UserFunction {

    public static final Codec<UserFunction> CODEC = RecordCodecBuilder.create((instance) ->
        instance.group(
            Codec.list(Entry.CODEC).fieldOf("values").forGetter((UserFunction config) -> Arrays.asList(config.values))
        ).apply(instance, UserFunction::new));

    private final Entry[] values;

    public UserFunction(List<Entry> entry) {
        values = entry.toArray(new Entry[0]);
    }


    public UserFunction(Map<Float, Float> funcMap) {
        values = funcMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new Entry(e.getKey(), e.getValue()))
            .toArray(Entry[]::new);
    }

    public float getValue(float y) {
        if (values.length == 0) {
            return 0;
        }
        if (values.length == 1) {
            return values[0].v;
        }
        Entry e1 = values[0];
        Entry e2 = values[1];

        // TODO: binary search? do we want to support functions complex enough for it to be needed? Will it improve performance?
        for (int i = 2; i < values.length; i++) {
            if (values[i - 1].y < y) {
                e1 = e2;
                e2 = values[i];
            }
        }
        float yFract = MathUtil.unlerp(y, e1.y, e2.y);
        return MathUtil.lerp(yFract, e1.v, e2.v);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Map<Float, Float> map = new HashMap<>();

        public Builder point(float y, float v) {
            this.map.put(y, v);
            return this;
        }

        public UserFunction build() {
            return new UserFunction(this.map);
        }
    }


    public static class Entry {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(
                Codec.FLOAT.fieldOf("y").forGetter((Entry config) -> config.y),
                Codec.FLOAT.fieldOf("v").forGetter((Entry config) -> config.v)
            ).apply(instance, Entry::new));
        public float y;
        public float v;

        public Entry() {
        }

        public Entry(float key, float value) {
            this.y = key;
            this.v = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return Float.compare(entry.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(y);
        }

        @Override
        public String toString() {
            return "Entry{" +
                "y=" + y +
                ", v=" + v +
                '}';
        }
    }
}
