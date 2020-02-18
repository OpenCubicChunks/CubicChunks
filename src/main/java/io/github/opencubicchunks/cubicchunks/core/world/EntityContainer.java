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

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: Have xcube review this class... I dont trust it
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EntityContainer {
    @SuppressWarnings("unchecked")
    public static final ClassInheritanceMultiMap<Entity>[] EMPTY_ARR = new ClassInheritanceMultiMap[]{new BlankEntityContainer.BlankEntityMap()};

    @Nonnull protected ClassInheritanceMultiMap<Entity> entities;
    protected boolean hasActiveEntities; //TODO: hasActiveEntitys is like an isModifyed right?
    protected long lastSaveTime;

    public EntityContainer() {
        this.entities = new ClassInheritanceMultiMap<>(Entity.class);
        this.hasActiveEntities = false;
        this.lastSaveTime = 0;
    }

    //=======================================
    //========Methods for Chunk/Cube=========
    //=======================================

    public void addEntity(Entity entity) {
        this.entities.add(entity);
        this.hasActiveEntities = true;
    }

    public boolean remove(Entity entity) {
        return this.entities.remove(entity);
    }

    public ClassInheritanceMultiMap<Entity> getEntitySet() {
        return this.entities;
    }

    public void clear() {
        this.entities.clear();
    }

    public Collection<Entity> getEntities() {
        return Collections.unmodifiableCollection(this.entities);
    }

    public int size() {
        return this.entities.size();
    }


    public boolean needsSaving(boolean flag, long time, boolean isModified) {
        if (flag) {
            if (this.hasActiveEntities && time != lastSaveTime || isModified) {
                return true;
            }
        } else if (this.hasActiveEntities && time >= this.lastSaveTime + 600) {
            return true;
        }
        return isModified;
    }

    public void markSaved(long time) {
        this.lastSaveTime = time;
    }

    public void writeToNbt(NBTTagCompound nbt, String name, Consumer<Entity> listener) {
        this.hasActiveEntities = false;
        NBTTagList nbtEntities = new NBTTagList();
        nbt.setTag(name, nbtEntities);
        for (Entity entity : this.entities) {

            NBTTagCompound nbtEntity = new NBTTagCompound();
            if (entity.writeToNBTOptional(nbtEntity)) {
                this.hasActiveEntities = true;
                nbtEntities.appendTag(nbtEntity);

                listener.accept(entity);
            }
        }
    }

    //listener is passed from CubeIO to set chunk position
    public void readFromNbt(NBTTagCompound nbt, String name, World world, Consumer<Entity> listener) {
        NBTTagList nbtEntities = nbt.getTagList(name, 10);

        for (int i = 0; i < nbtEntities.tagCount(); i++) {
            NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt(i);
            readEntity(nbtEntity, world, listener);
        }
    }

    private Entity readEntity(NBTTagCompound nbtEntity, World world, Consumer<Entity> listener) {

        // create the entity
        Entity entity = EntityList.createEntityFromNBT(nbtEntity, (World) world);
        if (entity == null) {
            return null;
        }
        if (entity instanceof EntityPlayerMP) {
            CubicChunks.LOGGER.error("EntityPlayerMP is serialized in save file! Reading the entity would break world ticking, skipping");
            return null;
        }
        addEntity(entity);

        listener.accept(entity);
        // deal with riding
        if (nbtEntity.hasKey("Passengers", Constants.NBT.TAG_LIST)) {

            NBTTagList nbttaglist = nbtEntity.getTagList("Passengers", 10);

            for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                Entity entity1 = readEntity(nbttaglist.getCompoundTagAt(i), world, listener);

                if (entity1 != null) {
                    entity1.startRiding(entity, true);
                }
            }
        }
        return entity;
    }
}
