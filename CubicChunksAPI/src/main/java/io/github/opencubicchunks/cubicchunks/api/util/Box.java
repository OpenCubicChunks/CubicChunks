/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.util;

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Box {

    protected int x1, y1, z1;
    protected int x2, y2, z2;

    public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    public void forEachPoint(XYZFunction function) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    function.apply(x, y, z);
                }
            }
        }
    }

    public boolean allMatch(XYZPredicate predicate) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (!predicate.test(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Box add(Box o) {
        return new Box(x1 + o.x1, y1 + o.y1, z1 + o.z1,
                x2 + o.x2, y2 + o.y2, z2 + o.z2);
    }

    public Box.Mutable asMutable() {
        return new Mutable(x1, y1, z1, x2, y2, z2);
    }

    @FunctionalInterface
    public interface XYZFunction {

        void apply(int x, int y, int z);
    }

    @FunctionalInterface
    public interface XYZPredicate {

        boolean test(int x, int y, int z);
    }

    public static class Mutable extends Box{

        public Mutable(int x1, int y1, int z1, int x2, int y2, int z2) {
            super(x1, y1, z1, x2, y2, z2);
        }


        public int getX1() {
            return x1;
        }

        public void setX1(int x1) {
            this.x1 = x1;
        }

        public int getY1() {
            return y1;
        }

        public void setY1(int y1) {
            this.y1 = y1;
        }

        public int getZ1() {
            return z1;
        }

        public void setZ1(int z1) {
            this.z1 = z1;
        }

        public int getX2() {
            return x2;
        }

        public void setX2(int x2) {
            this.x2 = x2;
        }

        public int getY2() {
            return y2;
        }

        public void setY2(int y2) {
            this.y2 = y2;
        }

        public int getZ2() {
            return z2;
        }

        public void setZ2(int z2) {
            this.z2 = z2;
        }

        public Box.Mutable expand(Box box) {
            this.x1 = Math.min(box.x1, x1);
            this.y1 = Math.min(box.y1, y1);
            this.z1 = Math.min(box.z1, z1);
            this.x2 = Math.max(box.x2, x2);
            this.y2 = Math.max(box.y2, y2);
            this.z2 = Math.max(box.z2, z2);
            return this;
        }

        public Box.Mutable add(int dx, int dy, int dz) {
            this.x1 += dx;
            this.x2 += dx;
            this.y1 += dy;
            this.y2 += dy;
            this.z1 += dz;
            this.z2 += dz;
            return this;
        }

    }
}
