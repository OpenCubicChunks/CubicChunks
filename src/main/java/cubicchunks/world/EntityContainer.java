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
package cubicchunks.world;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cubicchunks.CubicChunks;

//TODO: Have xcube review this class... I dont trust it
public class EntityContainer {

	private ClassInheritanceMultiMap<Entity> entities;
	private boolean hasActiveEntities; //TODO: hasActiveEntitys is like an isModifyed right?
	private long lastSaveTime;

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

	private boolean canAddEntityExcluded(Entity toAdd, Entity excluded, AxisAlignedBB queryBox, Predicate<? super Entity> predicate) {
		return toAdd != excluded &&
			toAdd.getEntityBoundingBox().intersectsWith(queryBox) &&
			(predicate == null || predicate.apply(toAdd));
	}

	// CHECKED: 1.11-13.19.0.2148
	public void getEntitiesWithinAABBForEntity(Entity excluded, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		for (Entity entity : this.entities) {

			// handle entity exclusion
			if (canAddEntityExcluded(entity, excluded, queryBox, predicate)) {
				out.add(entity);
			}
			// also check entity parts
			if (entity.getParts() != null) {
				for (Entity part : entity.getParts()) {
					if (canAddEntityExcluded(part, excluded, queryBox, predicate)) {
						out.add(part);
					}
				}
			}
		}
	}

	// CHECKED: 1.11-13.19.0.2148
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		for (T entity : this.entities.getByClass(entityType)) {
			if (entity.getEntityBoundingBox().intersectsWith(queryBox) &&
				(predicate == null || predicate.apply(entity))) {
				out.add(entity);
			}
		}
	}


	public ClassInheritanceMultiMap<Entity> getEntitySet() {
		return this.entities;
	}

	public boolean hasActiveEntities() {
		return this.hasActiveEntities;
	}

	public void setHasActiveEntities(boolean val) {
		this.hasActiveEntities = val;
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

	public void writeToNbt(NBTTagCompound nbt, String name) {
		writeToNbt(nbt, name, null);
	}

	public void writeToNbt(NBTTagCompound nbt, String name, IEntityActionListener listener) {
		this.hasActiveEntities = false;
		NBTTagList nbtEntities = new NBTTagList();
		nbt.setTag(name, nbtEntities);
		for (Entity entity : this.entities) {

			NBTTagCompound nbtEntity = new NBTTagCompound();
			if (entity.writeToNBTOptional(nbtEntity)) {
				this.hasActiveEntities = true;
				nbtEntities.appendTag(nbtEntity);

				if (listener != null) {
					listener.onEntity(entity);
				}
			}
		}
	}

	//listener is passed from CubeIO to set chunk position
	public void readFromNbt(NBTTagCompound nbt, String name, ICubicWorld world, IEntityActionListener listener) {
		NBTTagList nbtEntities = nbt.getTagList(name, 10);
		if (nbtEntities == null) {
			return;
		}

		for (int i = 0; i < nbtEntities.tagCount(); i++) {
			NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt(i);
			readEntity(nbtEntity, world, listener);
		}
	}

	private Entity readEntity(NBTTagCompound nbtEntity, ICubicWorld world, IEntityActionListener listener) {

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

		if (listener != null) {
			listener.onEntity(entity);
		}
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
