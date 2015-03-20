/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class EntityContainer
{
	private List<Entity> m_entities;
	private boolean m_hasActiveEntities;
	private long m_lastSaveTime;
	
	public EntityContainer( )
	{
		m_entities = new ArrayList<Entity>();
		m_hasActiveEntities = false;
		m_lastSaveTime = 0;
	}
	
	public boolean hasActiveEntities( )
	{
		return m_hasActiveEntities;
	}
	public void setHasActiveEntities( boolean val )
	{
		m_hasActiveEntities = val;
	}
	
	public void add( Entity entity )
	{
		m_entities.add( entity );
		m_hasActiveEntities = true;
	}
	
	public boolean remove( Entity entity )
	{
		return m_entities.remove( entity );
	}
	
	public void clear( )
	{
		m_entities.clear();
	}
	
	public List<Entity> entities( )
	{
		return m_entities;
	}
	
	public int size( )
	{
		return m_entities.size();
	}
	
	public void getEntities( List<Entity> out, Class<?> c, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		for( Entity entity : m_entities )
		{
			if( c.isAssignableFrom( entity.getClass() ) && entity.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( entity ) ) )
			{
				out.add( entity );
			}
		}
	}
	
	public void getEntitiesExcept( List<Entity> out, Entity excludedEntity, AxisAlignedBB queryBox, IEntitySelector selector )
	{
		for( Entity entity : m_entities )
		{
			// handle entity exclusion
			if( entity == excludedEntity )
			{
				continue;
			}
			
			if( entity.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( entity ) ) )
			{
				out.add( entity );
				
				// also check entity parts
				if( entity.getParts() != null )
				{
					for( Entity part : entity.getParts() )
					{
						if( part != excludedEntity && part.boundingBox.intersectsWith( queryBox ) && ( selector == null || selector.isEntityApplicable( part ) ) )
						{
							out.add( part );
						}
					}
				}
			}
		}
	}
	
	public boolean needsSaving( long time )
	{
		return m_hasActiveEntities && time >= m_lastSaveTime + 600;
	}
	
	public void markSaved( long time )
	{
		m_lastSaveTime = time;
	}
	
	public void writeToNbt( NBTTagCompound nbt, String name )
	{
		writeToNbt( nbt, name, null );
	}
	
	public void writeToNbt( NBTTagCompound nbt, String name, EntityActionListener listener )
	{
		m_hasActiveEntities = false;
		NBTTagList nbtEntities = new NBTTagList();
		nbt.setTag( name, nbtEntities );
		for( Entity entity : m_entities )
		{
			NBTTagCompound nbtEntity = new NBTTagCompound();
			if( entity.writeToNBTOptional( nbtEntity ) )
			{
				m_hasActiveEntities = true;
				nbtEntities.appendTag( nbtEntity );
				
				if( listener != null )
				{
					listener.onEntity( entity );
				}
			}
		}
	}
	
	public void readFromNbt( NBTTagCompound nbt, String name, World world, EntityActionListener listener )
	{
		NBTTagList nbtEntities = nbt.getTagList( name, 10 );
		if( nbtEntities == null )
		{
			return;
		}
		
		for( int i=0; i<nbtEntities.tagCount(); i++ )
		{
			NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt( i );
			
			// create the entity
			Entity entity = EntityList.createEntityFromNBT( nbtEntity, world );
			if( entity == null )
			{
				continue;
			}
			
			add( entity );
			
			if( listener != null )
			{
				listener.onEntity( entity );
			}
			
			// deal with riding
			while( nbtEntity.func_150297_b( "Riding", 10 ) )
			{
				// create the ridden entity
				NBTTagCompound nbtRiddenEntity = nbtEntity.getCompoundTag( "Riding" );
				Entity riddenEntity = EntityList.createEntityFromNBT( nbtRiddenEntity, world );
				if( riddenEntity == null )
				{
					break;
				}
				
				// RIDE THE PIG!!
				add( riddenEntity );
				entity.mountEntity( riddenEntity );
				
				// point to the ridden entity and iterate
				entity = riddenEntity;
				nbtEntity = nbtRiddenEntity;
			}
		}
	}
}
