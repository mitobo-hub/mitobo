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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * Interface for representing 2D and 3D segmentations of image data.
 * <p>
 * A segmentation representation stores for each position within its domain  
 * a class label. Labels are assumed to be of positive value or at least equal 
 * to zero. It is possible to declare certain positions 'invisible', 
 * for example for the purpose of ignoring them in statistical calculations.
 * In addition, each pixel position within the domain of the segmentation 
 * can be assigned an individual weight.
 *
 * @author Birgit Moeller
 */
public interface MTBSegmentationInterface {
	
	/**
	 * Segmentation dimension datatype.
	 */
	public enum SegmentationDimension {
		/**
		 * 2D segmentation data.
		 */
		dim_2,
		/**
		 * 3D segmentation data.
		 */
		dim_3
	}

	/**
	 * Returns the dimension of the segmentation.
	 */
	public abstract SegmentationDimension getDimension();
	
	/**
	 * Returns the size of the segmentation domain in x direction.
	 */
	public abstract int getSizeX();
	
	/**
	 * Returns the size of the segmentation domain in y direction.
	 */
	public abstract int getSizeY();
	
	/**
	 * Returns the size of the segmentation domain in z direction.
	 */
	public abstract int getSizeZ();
	
	/**
	 * Returns the number of classes represented by the segmentation.
	 */
	public abstract int getNumberOfClasses();
	
	/**
	 * Returns the maximal label used in the segmentation representation.
	 * <p>
	 * Note that the maximal label is not required to coincide with the 
	 * number of classes represented by the segmentation. 
	 * Labels should usually, but do not necessarily need to be positive.
	 */
	public abstract int getMaxLabel();
	
	/**
	 * True, if position (x,y) is visible.
	 */
	public abstract boolean isVisible(int x, int y);

	/**
	 * True, if position (x,y,z) is visible.
	 */
	public abstract boolean isVisible(int x, int y, int z);
	
	/**
	 * Set position (x,y) visible.
	 */
	public abstract void setVisible(int x, int y);

	/**
	 * Set position (x,y,z) visible.
	 */
	public abstract void setVisible(int x, int y, int z);

	/**
	 * Set position (x,y) invisible.
	 */
	public abstract void setInvisible(int x, int y);

	/**
	 * Set position (x,y,z) invisible.
	 */
	public abstract void setInvisible(int x, int y, int z); 

	/**
	 * Returns the class label of position (x,y).
	 */
	public abstract int getClass(int x, int y); 
	
	/**
	 * Returns the class label of position (x,y,z).
	 */
	public abstract int getClass(int x, int y, int z); 
	
	/**
	 * Set the label of position (x,y) to c.
	 */
	public abstract void setClass(int x, int y, int c);
	
	/**
	 * Set the label of position (x,y,z) to c.
	 */
	public abstract void setClass(int x, int y, int z, int c);

	/**
	 * Returns the pixel weight of position (x,y).
	 */
	public abstract double getWeight(int x, int y); 
	
	/**
	 * Returns the pixel weight of position (x,y,z).
	 */
	public abstract double getWeight(int x, int y, int z); 
	
	/**
	 * Sets the pixel weight of position (x,y) to c.
	 */
	public abstract void setWeight(int x, int y, double c);
	
	/**
	 * Sets the pixel weight of position (x,y,z) to c.
	 */
	public abstract void setWeight(int x, int y, int z, double c);

    /**
     * Gets the mask of the specified class (0 Background, 255 Foreground)
     */
    public abstract MTBImage getMask(int class_);
}
