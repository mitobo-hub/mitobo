/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;


@ALDMetaInfo(export=ExportPolicy.MANDATORY)
public interface MTBImageManipulator {

	/**
	 * Get title string
	 * @return
	 */
	public String getTitle();
	
	/** 
	 * Get data type
	 * @return data type ID 
	 */
	public MTBImageType getType();
	
	/**
	 * Get size of x-dimension
	 * @return size of x-dimension
	 */
	public int getSizeX();
	
	/**
	 * Get size of y-dimension
	 * @return size of y-dimension
	 */	
	public int getSizeY();
	
	/**
	 * Get size of z-dimension
	 * @return size of z-dimension
	 */		
	public int getSizeZ();

	/**
	 * Get size of t(ime)-dimension
	 * @return size of t(ime)-dimension
	 */		
	public int getSizeT();

	/**
	 * Get size of c(hannel)-dimension
	 * @return size of c(hannel)-dimension
	 */		
	public int getSizeC();
	
	/**
	 * Get the value of the 5D image at coordinate (x,y,z,t,c) as an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return voxel value
	 */
	public int getValueInt(int x, int y, int z, int t, int c);
	
	/**
	 * Get the value of the 5D image at coordinate (x,y,z,t,c) as a Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return voxel value
	 */
	public double getValueDouble(int x, int y, int z, int t, int c);		
	
	/**
	 * Set the value of the 5D image at coordinate (x,y,z,t,c) using an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param value to set the voxel to 
	 */	
	public void putValueInt(int x, int y, int z, int t, int c, int value);
	
	/**
	 * Set the value of the 5D image at coordinate (x,y,z,t,c) using a Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param value to set the voxel to 
	 */	
	public void putValueDouble(int x, int y, int z, int t, int c, double value);
	
	/**
	 * Get the physical size of a voxel (stepsize) in x-dimension 
	 * @return physical voxel size in x-dimension
	 */
	public double getStepsizeX();

	/**
	 * Get the physical size of a voxel (stepsize) in y-dimension 
	 * @return physical voxel size in y-dimension
	 */
	public double getStepsizeY();

	/**
	 * Get the physical size of a voxel (stepsize) in z-dimension 
	 * @return physical voxel size in z-dimension
	 */
	public double getStepsizeZ();

	/**
	 * Get the stepsize in t-dimension (timestep)
	 * @return time stepsize
	 */
	public double getStepsizeT();
	
	/**
	 * Get the unit of the x-dimension
	 * @return String of x-dimension's unit
	 */
	public String getUnitX();
	
	/**
	 * Get the unit of the y-dimension
	 * @return String of y-dimension's unit
	 */
	public String getUnitY();
	
	/**
	 * Get the unit of the z-dimension
	 * @return String of z-dimension's unit
	 */
	public String getUnitZ();
	
	/**
	 * Get the unit of the t-dimension
	 * @return String of t-dimension's unit
	 */
	public String getUnitT();
}
