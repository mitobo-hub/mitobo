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

package de.unihalle.informatik.MiToBo.math;

import java.awt.geom.Point2D;

/**
 * Math class with helper functions for basic geometric operations.
 * 
 * @author Birgit Moeller
 */
public class MathXGeom {

	/**
	 * Rotate a 2D point around the given angle.
	 * 
	 * @param p			Point to rotate.
	 * @param rad		Rotation angle in radiant.
	 * @return	Resulting point.
	 */
	public static Point2D.Double rotatePoint2D(Point2D.Double p, double rad) {
		return new Point2D.Double(Math.cos(rad)*p.x - Math.sin(rad)*p.y,
				Math.sin(rad)*p.x + Math.cos(rad)*p.y);
	}

	/**
	 * Rotate a 2D point around the given angle.
	 * 
	 * @param x			x-coordinate of the point.
	 * @param y			y-coordinate of the point.
	 * @param rad		Rotation angle in radiant.
	 * @return	Resulting point.
	 */
	public static Point2D.Double rotatePoint2D(double x, double y, double rad) {
		return rotatePoint2D(new Point2D.Double(x,y), rad);
	}

}
