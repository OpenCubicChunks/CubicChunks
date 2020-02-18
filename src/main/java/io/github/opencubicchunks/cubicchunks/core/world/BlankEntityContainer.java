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
package io.github.opencubicchunks.cubicchunks.core.world;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

public class BlankEntityContainer extends EntityContainer {

    public BlankEntityContainer() {
        this.entities = EntityContainer.EMPTY_ARR[0];
    }

    @Override
    public void addEntity(Entity entity) {
        int i = 0;
        // no-op
    }

    @Override
    public boolean remove(Entity entity) {
        // no-op
        return false;
    }

    @Override
    public void clear() {
        // no-op
    }

    @Override
    public Collection<Entity> getEntities() {
        return Collections.emptyList();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean needsSaving(boolean flag, long time, boolean isModified) {
        return false;
    }

    @Override
    public void markSaved(long time) {
    }

    @Override
    public void writeToNbt(NBTTagCompound nbt, String name, Consumer<Entity> listener) {
    }

    @Override
    public void readFromNbt(NBTTagCompound nbt, String name, World world, Consumer<Entity> listener) {
    }

    public static final class BlankEntityMap extends ClassInheritanceMultiMap<Entity> {

        public BlankEntityMap() {
            super(Entity.class);
        }

        @Override
        public boolean add(Entity e) {
            new Throwable().printStackTrace();
            // no-op
            return false;
        }

        @Override
        public boolean remove(Object o) {
            // no-op
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public <S> Iterable<S> getByClass(final Class<S> cl) {
            return Collections.emptyList();
        }

        @Override
        public Iterator<Entity> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

    }
}
