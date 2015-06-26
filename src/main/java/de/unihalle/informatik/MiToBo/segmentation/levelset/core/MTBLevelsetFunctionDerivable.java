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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core;

import de.unihalle.informatik.MiToBo.segmentation.basics.*;

/**
 * Level set function supporting optimization based on variational calculus.
 * 
 * @author Michael Schneider
 * @author Birgit Moeller
 */
public abstract class MTBLevelsetFunctionDerivable 
	implements MTBSegmentationInterface {

	/**
	 * Get the function value at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Value of level set function at given position.
	 */
	public abstract double get(int x, int y, int z);

	/**
	 * Partial derivative in x direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeX(int x, int y, int z);

	/**
	 * Partial derivative in y direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeY(int x, int y, int z);

	/**
	 * Partial derivative in z direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeZ(int x, int y, int z);

	/**
	 * Second partial derivative in x direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeXX(int x, int y, int z);

	/**
	 * Second partial derivative in y direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeYY(int x, int y, int z);

	/**
	 * Second partial derivative in z direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeZZ(int x, int y, int z);

	/**
	 * Second partial derivative in x-y direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeXY(int x, int y, int z);

	/**
	 * Second partial derivative in x-z direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeXZ(int x, int y, int z);

	/**
	 * Second partial derivative in y-z direction at position (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Derivative value.
	 */
	public abstract double getDerivativeYZ(int x, int y, int z);

	/**
	 * Curvature at point (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	Curvature value.
	 */
	public abstract double getCurvature(int x, int y, int z);

	/**
	 * Validity of point (x,y,z).
	 * @param x		Coordinate in x.
	 * @param y		Coordinate in y.
	 * @param z		Coordinate in z.
	 * @return	If true, point is valid and will be considered in calculations.
	 */
	public abstract boolean valid(int x, int y, int z);
	
	/**
	 * Checks if a pixel belongs to the zero level.
	 * @param x x coordinate of point.
	 * @param y y coordinate of point.
	 * @param z z coordinate of point.
	 * @return True, if point belongs to zero-level, false otherwise.
	 */
	public boolean nearZero(int x, int y, int z) {
		if (   x > 0 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x-1,y,z))) {
			return true;
		}
		if (   x < this.getSizeX() - 1 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x+1,y,z))) {
			return true;
		}
		if (   y > 0 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x,y-1,z))) {
			return true;
		}
		if (   y < this.getSizeY() - 1 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x,y+1,z))) {
			return true;
		}
		if (   z > 0 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x,y,z-1))) {
			return true;
		}
		if (   z < this.getSizeZ() - 1 
				&& sgn(this.get(x,y,z)) != sgn(this.get(x,y,z+1))) {
			return true;
		}
		return false;
	}
	
	/**
	 * Signum function.
	 * @param val		Value to check.
	 * @return 1, if value is greater zero, otherwise -1.
	 */
	protected int sgn(double val) {
		if (val > 0) {
			return 1;
		}
		if (val < 0)
			return -1;
		return 0;
	}
}

