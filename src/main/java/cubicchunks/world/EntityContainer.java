/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.google.common.base.Predicate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ClassInheritanceMultiMap;

public class EntityContainer {
	
	private ClassInheritanceMultiMap entities;
	private boolean hasActiveEntities;
	private long lastSaveTime;
	
	public EntityContainer() {
		this.entities = new ClassInheritanceMultiMap(Entity.class);
		this.hasActiveEntities = false;
		this.lastSaveTime = 0;
	}
	
	public ClassInheritanceMultiMap getEntitySet() {
		return this.entities;
	}
	
	public boolean hasActiveEntities() {
		return this.hasActiveEntities;
	}
	
	public void setHasActiveEntities(boolean val) {
		this.hasActiveEntities = val;
	}
	
	public void add(Entity entity) {
		this.entities.add(entity);
		this.hasActiveEntities = true;
	}
	
	public boolean remove(Entity entity) {
		return this.entities.remove(entity);
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
	
	public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		//func_180215_b --> getIterableForType
		for (T entity : (Iterable<T>) this.entities.func_180215_b(entityType)) {
			if (entityType.isAssignableFrom(entity.getClass()) && entity.getBoundingBox().intersectsWith(queryBox) && (predicate == null || predicate.apply(entity))) {
				out.add(entity);
			}
		}
	}
	
	public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		
		for (Entity entity : (Iterable<Entity>) this.entities) {
			
			// handle entity exclusion
			if (entity == excludedEntity) {
				continue;
			}
			
			if (entity.getBoundingBox().intersectsWith(queryBox) && (predicate == null || predicate.apply(entity))) {
				out.add(entity);
				
				// also check entity parts
				if (entity.getParts() != null) {
					for (Entity part : entity.getParts()) {
						if (part != excludedEntity && part.getBoundingBox().intersectsWith(queryBox) && (predicate == null || predicate.apply(part))) {
							out.add(part);
						}
					}
				}
			}
		}
	}
	
	public boolean needsSaving(long time) {
		return this.hasActiveEntities && time >= this.lastSaveTime + 600;
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
		for (Entity entity : (Iterable<Entity>) this.entities) {
			
			NBTTagCompound nbtEntity = new NBTTagCompound();
			entity.writeToNBT(nbtEntity);
			this.hasActiveEntities = true;
			nbtEntities.appendTag(nbtEntity);
			
			if (listener != null) {
				listener.onEntity(entity);
			}
		}
	}
	
	public void readFromNbt(NBTTagCompound nbt, String name, World world, IEntityActionListener listener) {
		NBTTagList nbtEntities = nbt.getTagList(name, 10);
		if (nbtEntities == null) {
			return;
		}
		
		for (int i = 0; i < nbtEntities.tagCount(); i++) {
			NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt(i);
			
			// create the entity
			//Note: This probably isn't the forge EntityRegistry
			Entity entity = EntityRegistry.createEntity(nbtEntity, world);
			if (entity == null) {
				continue;
			}
			
			add(entity);
			
			if (listener != null) {
				listener.onEntity(entity);
			}
			
			// deal with riding
			while (nbtEntity.hasKey("Riding")) {
				
				// create the ridden entity
				NBTTagCompound nbtRiddenEntity = nbtEntity.getCompoundTag("Riding");
				Entity riddenEntity = EntityRegistry.createEntity(nbtRiddenEntity, world);
				if (riddenEntity == null) {
					break;
				}
				
				// RIDE THE PIG!!
				add(riddenEntity);
				entity.mountEntity(riddenEntity);
				
				// point to the ridden entity and iterate
				entity = riddenEntity;
				nbtEntity = nbtRiddenEntity;
			}
		}
	}
}
