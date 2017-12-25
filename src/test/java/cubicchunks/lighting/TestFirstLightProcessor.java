package cubicchunks.lighting;

import static cubicchunks.testutil.LightingMatchers.pos;
import static cubicchunks.testutil.LightingMatchers.posRange;
import static cubicchunks.testutil.TestLightBlockAccessImpl.lightAccess;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import cubicchunks.testutil.TestLightBlockAccessImpl;
import cubicchunks.util.CubePos;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TestFirstLightProcessor {

    @Captor
    private ArgumentCaptor<Iterable<BlockPos>> captor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEmptyCubeNoBlocksUpdateAll() {
        CubePos cubePos = new CubePos(0, 0, 0);

        Set<BlockPos> expected = new HashSet<>();
        BlockPos.getAllInBox(cubePos.getMinBlockPos(), cubePos.getMaxBlockPos()).forEach(expected::add);

        LightPropagator propagator = mock(LightPropagator.class);

        FirstLightProcessor flp = makeProcessor(new TestLightBlockAccessImpl(20), propagator);
        flp.updateSkylightFor(cubePos);

        verify(propagator).propagateLight(any(), captor.capture(), any(), eq(EnumSkyBlock.SKY), any());

        Iterable<BlockPos> actualValues = captor.getAllValues().stream().flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .collect(Collectors.toList());
        assertThat(actualValues, containsInAnyOrder(expected.toArray(new BlockPos[0])));
    }

    @Test
    public void testEmptyCubeNoBlocksDarkUpdateNone() {
        CubePos cubePos = new CubePos(0, 0, 0);

        LightPropagator propagator = mock(LightPropagator.class);

        FirstLightProcessor flp = makeProcessor(
                lightAccess(20)
                        .withOpaque(posRange(
                                pos(cubePos.getMinBlockX() - 1, cubePos.getMaxBlockY() + 1, cubePos.getMinBlockZ() - 1),
                                pos(cubePos.getMaxBlockX() + 1, cubePos.getMaxBlockY() + 1, cubePos.getMaxBlockX() + 1)
                        )).make(),
                propagator);
        flp.updateSkylightFor(cubePos);

        verifyZeroInteractions(propagator);
    }

    private FirstLightProcessor makeProcessor(ILightBlockAccess access, LightPropagator propagator) {
        return new FirstLightProcessor(new ILightChunkAccess() {
            @Override public ILightBlockAccess getLightBlockAccess(CubePos pos) {
                return access;
            }

            @Override public Iterable<CubePos> getCubesToUpdate(ChunkPos pos, int minBlockY, int maxBlockY) {
                return Collections.emptyList();
            }
        }, mock(LightUpdateTracker.class), propagator, EnumSet.allOf(EnumSkyBlock.class));
    }
}
