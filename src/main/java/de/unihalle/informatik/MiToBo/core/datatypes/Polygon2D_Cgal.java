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

package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * Java Native Interface wrapper class for 2D polygons to include CGAL library
 * methods.
 * 
 * 
 * @author moeller
 */
@ALDMetaInfo(export = ExportPolicy.ALLOWED)
@Deprecated
public class Polygon2D_Cgal {

		/**
		 * Checks if a polygon is simple.
		 * 
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return True, if polygon is simple.
		 */
		public native boolean cgal_isSimple(double[] xs, double[] ys);

		/**
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return True, if polygon is convex.
		 */
		public native boolean cgal_isConvex(double[] xs, double[] ys);

		/**
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @param pt
		 *          Point to check.
		 * @return -1, if point lies on negative side, 1, if on positive side, and 0
		 *         if on boundary
		 */
		public native int cgal_orientation(double[] xs, double[] ys, double[] pt);

		/**
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return True, if polygon is oriented counter-clockwise.
		 */
		public native boolean cgal_isCounterclockwiseOriented(double[] xs, double[] ys);

		/**
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return True, if polygon is oriented clockwise.
		 */
		public native boolean cgal_isClockwiseOriented(double[] xs, double[] ys);

		/**
		 * Calculates the signed area of a polygon.
		 * <p>
		 * The sign is positive for counter-clockwise polygons, negative for clockwise
		 * polygons. If the polygon is not simple, the area is not well defined
		 * 
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return Signed value of polygon area.
		 */
		public native double cgal_signedArea(double[] xs, double[] ys);

		/**
		 * Simplifies the given polygon.
		 * 
		 * @param xs
		 *          List of point x coordinates.
		 * @param ys
		 *          List of point y coordinates.
		 * @return New polygon points (first x coords, then y coords).
		 */
		public native double[] cgal_makePolySimple(double[] xs, double[] ys);

		/**
		 * Static library load routine, called at runtime.
		 */
		static {
				try {
						System.loadLibrary("JNI_Cgal_Polygon2D");
				} catch (UnsatisfiedLinkError e) {
						System.out.println("   " + System.mapLibraryName("JNI_Cgal_Polygon2D")
						    + " not found\n" + e.getMessage());
				}
		}
}
