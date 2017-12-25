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

import static org.junit.Assert.assertEquals;

import com.flowpowered.noise.module.source.Perlin;
import cubicchunks.util.MathUtil;
import cubicchunks.util.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.math.MathHelper;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class TestPacketUtil {

    @Test
    public void testSignedVarInts() {
        testRange(-128, 128, 0);
        testRange(-128, 128, 6);
        testRange(-128, 128, 12);
        testRange(-128, 128, 18);
        testRange(-128, 128, 24);
        testRange(Integer.MIN_VALUE, Integer.MIN_VALUE + 128, 0);
        testRange(Integer.MAX_VALUE - 128, Integer.MAX_VALUE, 0);
    }

    private void testRange(int min, int max, int bitshift) {
        // 5* because max 5 bytes per int
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[5 * (max - min + 1)]);
        buf.writerIndex(0);
        for (long i = min; i <= max; i++) {
            int val = (int) i << bitshift;
            PacketUtils.writeSignedVarInt(buf, val);
        }
        buf.readerIndex(0);
        for (long i = min; i <= max; i++) {
            int val = (int) i << bitshift;
            assertEquals(val, PacketUtils.readSignedVarInt(buf));
        }
    }

    private Perlin perlin = new Perlin();

    {
        perlin.setFrequency(0.003);
        perlin.setOctaveCount(8);
    }

    int SIZE = 512;

    @Test
    public void t() throws IOException {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_USHORT_GRAY);

        DataBufferUShort buffer = (DataBufferUShort) img.getRaster().getDataBuffer();

        for (int x = 0; x < img.getWidth(); x++) {
            for (int z = 0; z < img.getHeight(); z++) {
                float height = getHeight(x, z);
                buffer.setElem(x + z * img.getWidth(), (int) ((height * 0.5 + 0.5) * Character.MAX_VALUE));
            }
        }

        ImageIO.write(img, "PNG", new File("/home/bartosz/GREG_HMAP.png"));
    }

    private float getHeight(int x, int z) {
        float xNorm = (x - SIZE / 2) / (SIZE * 0.5f);
        float zNorm = (z - SIZE / 2) / (SIZE * 0.5f);

        float base = getBaseHeight(xNorm, zNorm);
        float var = getHeightVariation(xNorm, zNorm);

        float perlin = (float) (this.perlin.getValue(xNorm * 2000, zNorm * 2000, 0.144352) / this.perlin.getMaxValue());
        perlin -= 0.5;
        perlin *= 2;

        perlin *= 1.5;//because flow noise bugs

        perlin *= var;
        perlin += base;

        return perlin;
    }

    private float getBaseHeight(float x, float z) {
        float minR = getMinRadius(x, z);
        float maxR = getMaxRadius(x, z);

        float flat = 0.1f;
        float topMountains = 0.6f;
        float ocean = -0.2f;
        float outside = 0.2f;
        float r = (float) Math.sqrt(x * x + z * z);
        if (r < 0.8 * minR) {
            return flat;
        } else if (r < 0.9 * minR) {
            float a = (float) MathUtil.unlerp(r / minR, 0.8, 0.9);
            return (float) (a * (topMountains - flat) + flat);
        } else if (r < minR) {
            float a = (float) MathUtil.unlerp(r / minR, 0.9, 1);
            return (1 - a) * topMountains;
        } else if (r < minR * 1.1) {
            float a = (float) MathUtil.unlerp(r / minR, 1, 1.1);
            return (float) (ocean * a);
        } else if (r < maxR * 0.9) {
            return ocean;
        } else if (r < maxR) {
            float a = (float) MathUtil.unlerp(r / maxR, 0.9, 1);
            return (float) ocean * (1 - a);
        } else if (r < maxR * 1.1) {
            float a = (float) MathUtil.unlerp(r / maxR, 1, 1.1);
            return a * outside;
        } else {
            return outside;
        }
    }

    private float getHeightVariation(float x, float z) {
        float minR = getMinRadius(x, z);
        float maxR = getMaxRadius(x, z);

        float r = (float) Math.sqrt(x * x + z * z);
        if (r < 0.8 * minR) {
            return 0.1f;
        } else if (r < 0.9 * minR) {
            float a = (float) MathUtil.unlerp(r / minR, 0.8, 0.9);
            return (float) (a * 0.9 + 0.1);
        } else if (r < minR) {
            float a = (float) MathUtil.unlerp(r / minR, 0.9, 1);
            return 1 - a;
        } else if (r < minR * 1.1) {
            float a = (float) MathUtil.unlerp(r / minR, 1, 1.1);
            return (float) (-0.2 * a);
        } else if (r < maxR * 0.9) {
            return -0.2f;
        } else if (r < maxR) {
            float a = (float) MathUtil.unlerp(r / maxR, 0.9, 1);
            return (float) 0.2 * (a - 1);
        } else if (r < maxR * 1.1) {
            float a = (float) MathUtil.unlerp(r / maxR, 1, 1.1);
            return a * 0.5f;
        } else {
            return 0.5f;
        }
    }

    private float getMaxRadius(float x, float z) {
        double dist = Math.sqrt(x * x + z * z);
        x /= dist;
        z /= dist;

        float sin = z;
        float cos = x;

        float squareDist = (float) Math.min(1.0 / Math.abs(sin), 1.0 / Math.abs(cos));

        return (squareDist + 1) * 0.5f - (float) (perlin.getValue(x * 400, z * 400, 2.47239) / perlin.getMaxValue()) * 0.3f;
    }

    private float getMinRadius(float x, float z) {
        double dist = Math.sqrt(x * x + z * z);
        x /= dist;
        z /= dist;

        return (float) ((perlin.getValue(x * 400, z * 400, 1.21234) / perlin.getMaxValue()) - 0.5f) * 3 + 0.5f;
    }
}
