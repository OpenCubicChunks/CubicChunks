/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

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
	
	public List<Entity> entities( )
	{
		return m_entities;
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
	
	public void readFromNbt( NBTTagCompound nbt, String name, World world )
	{
		readFromNbt( nbt, name, world, null );
	}
	
	public void readFromNbt( NBTTagCompound nbt, String name, World world, EntityActionListener listener )
	{
		NBTTagList nbtEntities = nbt.getTagList( name, 10 );
		if( nbtEntities != null )
		{
			for( int i=0; i<nbtEntities.tagCount(); i++ )
			{
				NBTTagCompound nbtEntity = nbtEntities.getCompoundTagAt( i );
				Entity entity = EntityList.createEntityFromNBT( nbtEntity, world );
				if( entity != null )
				{
					add( entity );
					
					if( listener != null )
					{
						listener.onEntity( entity );
					}
					
					// deal with riding
					Entity topEntity = entity;
					for( NBTTagCompound nbtRiddenEntity = nbtEntity; nbtRiddenEntity.func_150297_b( "Riding", 10 ); nbtRiddenEntity = nbtRiddenEntity.getCompoundTag( "Riding" ) )
					{
						Entity riddenEntity = EntityList.createEntityFromNBT( nbtRiddenEntity.getCompoundTag( "Riding" ), world );
						if( riddenEntity != null )
						{
							add( riddenEntity );
							topEntity.mountEntity( riddenEntity );
						}
						topEntity = riddenEntity;
					}
				}
			}
		}
	}
}
