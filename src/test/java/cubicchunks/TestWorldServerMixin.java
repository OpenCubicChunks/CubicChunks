/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cubicchunks.testutil.MinecraftEnvironment;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.type.FlatCubicWorldType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.spongepowered.lwts.runner.LaunchWrapperTestRunner;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RunWith(LaunchWrapperTestRunner.class)
public class TestWorldServerMixin {

    @Nonnull @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ICubicWorldServer world;

    @Before
    public void setUp() throws IOException {
        //Note: this setup code will probably break between minecraft versions

        MinecraftEnvironment.init();
        MinecraftServer server = MinecraftEnvironment.createFakeServer();

        ISaveHandler mockSaveHandler =
                new AnvilSaveHandler(folder.newFolder("save"), "world", false, new DataFixer(512));
        WorldType cubicChunksType = new FlatCubicWorldType();
        WorldSettings settings = new WorldSettings(0, GameType.SURVIVAL, false, false, cubicChunksType);
        WorldInfo worldInfo = new WorldInfo(settings, "test");
        this.world = (ICubicWorldServer) new WorldServer(server, mockSaveHandler, worldInfo, 0, new Profiler());
    }

    @Test
    public void testWorldMinHeightVanillaCompatibility() {
        assertEquals("Invalid min world height for vanilla world", 0, this.world.getMinHeight());
    }

    @Test
    public void testWorldMaxHeightVanillaCompatibility() {
        assertEquals("Invalid max world height for vanilla world", 256, this.world.getMaxHeight());
    }

    @Test
    public void testInitCubicWorldIsCubic() {
        this.world.initCubicWorld(-4096,4096);
        assertTrue(this.world.isCubicWorld());
    }

    @Test
    public void testCubicWorldMinHeight() {
        this.world.initCubicWorld(-4096,4096);
        assertThat(this.world.getMinHeight(), is(lessThan(0)));
    }

    @Test
    public void testCubicWorldMaxHeight() {
        //System.err.println(((ICubicChunksWorldType)world.getWorldInfo().getTerrainType()).getMinimumPossibleHeight());
        this.world.initCubicWorld(-4096,4096);
        assertThat(this.world.getMaxHeight(), is(greaterThan(256)));
    }
}
