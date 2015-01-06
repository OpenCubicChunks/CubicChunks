package main.java.cubicchunks.generator.noise;

import java.util.Random;

public class NoiseGeneratorSimplex
{
    private static int[][] gradTable = new int[][] {{1, 1, 0}, { -1, 1, 0}, {1, -1, 0}, { -1, -1, 0}, {1, 0, 1}, { -1, 0, 1}, {1, 0, -1}, { -1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    public static final double sqrt3 = Math.sqrt(3.0D);
    private int[] perm;
    public double xCoord;
    public double zCoord;
    private static final double F3 = 0.5D * (sqrt3 - 1.0D);
    private static final double G3 = (3.0D - sqrt3) / 6.0D;

    public NoiseGeneratorSimplex()
    {
        this(new Random());
    }

    public NoiseGeneratorSimplex(Random rand)
    {
        this.perm = new int[512];
        this.xCoord = rand.nextDouble() * 256.0D;
        this.zCoord = rand.nextDouble() * 256.0D;
        int i;

        for (i = 0; i < 256; this.perm[i] = i++)
        {
            ;
        }

        for (i = 0; i < 256; ++i)
        {
            int var3 = rand.nextInt(256 - i) + i;
            int var4 = this.perm[i];
            this.perm[i] = this.perm[var3];
            this.perm[var3] = var4;
            this.perm[i + 256] = this.perm[i];
        }
    }

    private static int fastfloor(double val)
    {
        return val > 0.0D ? (int)val : (int)val - 1;
    }

    private static double func_151604_a(int[] p_151604_0_, double p_151604_1_, double p_151604_3_)
    {
        return (double)p_151604_0_[0] * p_151604_1_ + (double)p_151604_0_[1] * p_151604_3_;
    }

    public double noise2D(double p_151605_1_, double p_151605_3_)
    {
        double var11 = 0.5D * (sqrt3 - 1.0D);
        double var13 = (p_151605_1_ + p_151605_3_) * var11;
        int var15 = fastfloor(p_151605_1_ + var13);
        int var16 = fastfloor(p_151605_3_ + var13);
        double var17 = (3.0D - sqrt3) / 6.0D;
        double var19 = (double)(var15 + var16) * var17;
        double var21 = (double)var15 - var19;
        double var23 = (double)var16 - var19;
        double var25 = p_151605_1_ - var21;
        double var27 = p_151605_3_ - var23;
        byte var29;
        byte var30;

        if (var25 > var27)
        {
            var29 = 1;
            var30 = 0;
        }
        else
        {
            var29 = 0;
            var30 = 1;
        }

        double var31 = var25 - (double)var29 + var17;
        double var33 = var27 - (double)var30 + var17;
        double var35 = var25 - 1.0D + 2.0D * var17;
        double var37 = var27 - 1.0D + 2.0D * var17;
        int var39 = var15 & 255;
        int var40 = var16 & 255;
        int var41 = this.perm[var39 + this.perm[var40]] % 12;
        int var42 = this.perm[var39 + var29 + this.perm[var40 + var30]] % 12;
        int var43 = this.perm[var39 + 1 + this.perm[var40 + 1]] % 12;
        double var44 = 0.5D - var25 * var25 - var27 * var27;
        double var5;

        if (var44 < 0.0D)
        {
            var5 = 0.0D;
        }
        else
        {
            var44 *= var44;
            var5 = var44 * var44 * func_151604_a(gradTable[var41], var25, var27);
        }

        double var46 = 0.5D - var31 * var31 - var33 * var33;
        double var7;

        if (var46 < 0.0D)
        {
            var7 = 0.0D;
        }
        else
        {
            var46 *= var46;
            var7 = var46 * var46 * func_151604_a(gradTable[var42], var31, var33);
        }

        double var48 = 0.5D - var35 * var35 - var37 * var37;
        double var9;

        if (var48 < 0.0D)
        {
            var9 = 0.0D;
        }
        else
        {
            var48 *= var48;
            var9 = var48 * var48 * func_151604_a(gradTable[var43], var35, var37);
        }

        return 70.0D * (var5 + var7 + var9);
    }

    public void arrayNoise2D(double[] noiseArray, double xOffset, double zOffset, int xSize, int zSize, double xScale, double zScale, double scale)
    {
        int var14 = 0;

        for (int var15 = 0; var15 < zSize; ++var15)
        {
            double var16 = (zOffset + (double)var15) * zScale + this.zCoord;

            for (int var18 = 0; var18 < xSize; ++var18)
            {
                double var19 = (xOffset + (double)var18) * xScale + this.xCoord;
                
                double var27 = (var19 + var16) * F3;
                int var29 = fastfloor(var19 + var27);
                int var30 = fastfloor(var16 + var27);
                double var31 = (double)(var29 + var30) * G3;
                double var33 = (double)var29 - var31;
                double var35 = (double)var30 - var31;
                double var37 = var19 - var33;
                double var39 = var16 - var35;
                byte var41;
                byte var42;

                if (var37 > var39)
                {
                    var41 = 1;
                    var42 = 0;
                }
                else
                {
                    var41 = 0;
                    var42 = 1;
                }

                double var43 = var37 - (double)var41 + G3;
                double var45 = var39 - (double)var42 + G3;
                double var47 = var37 - 1.0D + 2.0D * G3;
                double var49 = var39 - 1.0D + 2.0D * G3;
                int var51 = var29 & 255;
                int var52 = var30 & 255;
                int var53 = this.perm[var51 + this.perm[var52]] % 12;
                int var54 = this.perm[var51 + var41 + this.perm[var52 + var42]] % 12;
                int var55 = this.perm[var51 + 1 + this.perm[var52 + 1]] % 12;
                double var56 = 0.5D - var37 * var37 - var39 * var39;
                double var21;

                if (var56 < 0.0D)
                {
                    var21 = 0.0D;
                }
                else
                {
                    var56 *= var56;
                    var21 = var56 * var56 * func_151604_a(gradTable[var53], var37, var39);
                }

                double var58 = 0.5D - var43 * var43 - var45 * var45;
                double var23;

                if (var58 < 0.0D)
                {
                    var23 = 0.0D;
                }
                else
                {
                    var58 *= var58;
                    var23 = var58 * var58 * func_151604_a(gradTable[var54], var43, var45);
                }

                double var60 = 0.5D - var47 * var47 - var49 * var49;
                double var25;

                if (var60 < 0.0D)
                {
                    var25 = 0.0D;
                }
                else
                {
                    var60 *= var60;
                    var25 = var60 * var60 * func_151604_a(gradTable[var55], var47, var49);
                }

                int var10001 = var14++;
                noiseArray[var10001] += 70.0D * (var21 + var23 + var25) * scale;
            }
        }
    }
}
