/*******************************************************************************
 * Copyright (c) 2014 Nick Whitney.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Nick Whitney - initial implementation.
 ******************************************************************************/
package main.java.cubicchunks.generator.builder;

public abstract interface IBuilder 
{	
	public void setSeed(int seed);
	
	public void setSeaLevel(double seaLevel);
	
	public void build() throws IllegalArgumentException;
	
	public double getValue(double x, double y, double z);
}
