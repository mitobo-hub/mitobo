/*
 * Copyright (C) 2010 - @YEAR@ by the MiToBo development team
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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;

/**
 * Class to represent the geometric shape of a single marker in the image.
 * <p>
 * In the MTB Cell Counter in addition to a position a marker also owns 
 * a shape, e.g., a region contour. 
 *
 * @author Birgit Moeller
 */
public abstract class CellCntrMarkerShape {
	
	/**
	 * Border of the object.
	 */
	protected MTBBorder2D mBorder = null;
	
	/**
	 * Average intensity of all shape pixels.
	 */
	protected double avgIntensity = -1;
	
	/**
	 * Get outline of shape.
	 * @return	Outline of shape, null if not available.
	 */
	public MTBBorder2D getOutline() {
		return this.mBorder;
	}
	
	/**
	 * Get area of the shape.
	 * @return	Size of shape area.
	 */
	public abstract double getArea();

	/**
	 * Set average region intensity.
	 * @param ai	Average intensity.
	 */
	public void setAvgIntensity(double ai) {
		this.avgIntensity = ai;
	}
	
	/**
	 * Get average intensity.
	 * @return	Average intensity, -1 if not available.
	 */
	public double getAvgIntensity() {
		return this.avgIntensity;
	}

}
