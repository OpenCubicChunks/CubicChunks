package io.github.opencubicchunks.cubicchunks;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.Test;
import org.objectweb.asm.Type;

public class TestInheritanceObfuscationCollisions {
    @Test
    public void tesChunkCube() {
        verifyNoCollision(CubeAccess.class, ChunkAccess.class);
    }

    private void verifyNoCollision(Class<?> ccClass, Class<?> mcClass) {
        Set<String> ccMethods = Arrays.stream(ccClass.getDeclaredMethods())
            .map(this::methodString).collect(Collectors.toSet());
        Set<String> mcMethods = Arrays.stream(mcClass.getDeclaredMethods())
            .map(this::methodString).collect(Collectors.toSet());


        ccMethods.retainAll(mcMethods);
        assertThat(ccMethods, is(empty()));
    }

    private String methodString(Method m) {
        return m.getName() + Type.getMethodDescriptor(m);
    }
}
