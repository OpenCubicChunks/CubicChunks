package cubicchunks;

import com.google.common.collect.ImmutableMap;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.region.ShadowPagingRegion;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author DaPorkchop_
 */
public class TestShadowPagingRegion {
    private static SaveSection2D createShadowPagingSaveSection2d(Path directory) {
        //copied from RegionCubeStorage.saveForPath()
        return new SaveSection2D(
                new SharedCachedRegionProvider<>(
                        new SimpleRegionProvider<>(new EntryLocation2D.Provider(), directory, (keyProv, r) ->
                                ShadowPagingRegion.<EntryLocation2D>builder()
                                        .setDirectory(directory)
                                        .setRegionKey(r)
                                        .setKeyProvider(keyProv)
                                        .setSectorSize(512)
                                        .build(),
                                (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
                        )
                ),
                new SharedCachedRegionProvider<>(
                        new SimpleRegionProvider<>(new EntryLocation2D.Provider(), directory,
                                (keyProvider, regionKey) -> new ExtRegion<>(directory, Collections.emptyList(), keyProvider, regionKey),
                                (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
                        )
                ));
    }

    @Test
    public void testWriteTooLargeOverflowsToExtRegion() throws IOException {
        Path tempDir = Files.createTempDirectory("CubicChunks-TestShadowPagingRegion");
        try (SaveSection2D saveSection = createShadowPagingSaveSection2d(tempDir)) {
            EntryLocation2D loc0 = new EntryLocation2D(0, 0);
            EntryLocation2D loc1 = new EntryLocation2D(0, 1);
            EntryLocation2D loc2 = new EntryLocation2D(0, 2);

            Assert.assertEquals(loc0.getRegionKey(), loc1.getRegionKey());
            Assert.assertEquals(loc0.getRegionKey(), loc2.getRegionKey());

            RegionKey regionKey = loc0.getRegionKey();

            //write some small values and make sure they don't overflow into an ext region
            saveSection.save(ImmutableMap.of(
                    loc0, ByteBuffer.allocate(10),
                    loc1, ByteBuffer.allocate(4096)
            ));
            saveSection.flush();

            Assert.assertEquals(Optional.of(ByteBuffer.allocate(10)), saveSection.load(loc0, false));
            Assert.assertEquals(Optional.of(ByteBuffer.allocate(4096)), saveSection.load(loc1, false));

            Assert.assertTrue(Files.isRegularFile(tempDir.resolve(regionKey.getName())));
            Assert.assertFalse(Files.isDirectory(tempDir.resolve(regionKey.getName() + ".ext")));

            //write different small values as well as a single big value which will overflow into an external region
            saveSection.save(ImmutableMap.of(
                    loc0, ByteBuffer.allocate(8193),
                    loc1, ByteBuffer.allocate(1 << 20),
                    loc2, ByteBuffer.allocate(1337)
            ));
            saveSection.flush();

            Assert.assertEquals(Optional.of(ByteBuffer.allocate(8193)), saveSection.load(loc0, false));
            Assert.assertEquals(Optional.of(ByteBuffer.allocate(1 << 20)), saveSection.load(loc1, false));
            Assert.assertEquals(Optional.of(ByteBuffer.allocate(1337)), saveSection.load(loc2, false));

            Assert.assertTrue(Files.isRegularFile(tempDir.resolve(regionKey.getName())));
            Assert.assertTrue(Files.isDirectory(tempDir.resolve(regionKey.getName() + ".ext")));
            Assert.assertFalse(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc0.getId()))));
            Assert.assertTrue(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc1.getId()))));
            Assert.assertFalse(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc2.getId()))));

            //write different small values again, allowing loc1 to be stored without an ext entry again
            Map<EntryLocation2D, ByteBuffer> entries = new LinkedHashMap<>(); //not using the ImmutableMap syntax here because we want a null value
            entries.put(loc0, null);
            entries.put(loc1, ByteBuffer.allocate(69)); //nice
            entries.put(loc2, ByteBuffer.allocate(9001));
            saveSection.save(entries);
            saveSection.flush();

            Assert.assertEquals(Optional.empty(), saveSection.load(loc0, false));
            Assert.assertEquals(Optional.of(ByteBuffer.allocate(69)), saveSection.load(loc1, false));
            Assert.assertEquals(Optional.of(ByteBuffer.allocate(9001)), saveSection.load(loc2, false));

            Assert.assertTrue(Files.isRegularFile(tempDir.resolve(regionKey.getName())));
            Assert.assertTrue(Files.isDirectory(tempDir.resolve(regionKey.getName() + ".ext")));
            Assert.assertFalse(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc0.getId()))));
            Assert.assertFalse(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc1.getId()))));
            Assert.assertFalse(Files.isRegularFile(tempDir.resolve(regionKey.getName() + ".ext").resolve(String.valueOf(loc2.getId()))));
        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}
