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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import uk.co.geolib.geolib.C2DPoint;
import uk.co.geolib.geopolygons.C2DPolygon;

/**
 * Wrapper class for C2DPolygon functions from GeoPolygons library.
 * <p>
 * Webpage: <a href="http://www.geolib.co.uk/index.htm">
 * 	GeoLib Homepage</a>
 * 
 * @author moeller
 */
public class MTBPolygon2D_GeoPolygonsWrapper {

	/**
	 * Checks if a polygon is simple.
	 * 
	 * @param p		Input polygon.
	 * @return True, if polygon is simple.
	 */
	protected static boolean isSimple(MTBPolygon2D p) {
		return !MTBtoC2D(p).HasCrossingLines();
	}

	/**
	 * Checks if a polygon is convex.
	 * 
	 * @param p		Input polygon.
	 * @return True, if polygon is convex.
	 */
	protected static boolean isConvex(MTBPolygon2D p) {
		return MTBtoC2D(p).IsConvex();
	}

	/**
	 * Checks if polygon is oriented counter-clockwise.
	 * 
	 * @param p		Input polygon.
	 * @return True, if polygon is oriented counter-clockwise.
	 */
	protected static boolean isCounterClockwiseOriented(MTBPolygon2D p) {
		C2DPolygon cPoly = MTBtoC2D(p); 
		// C2DPolygon is always oriented clockwise, so we have to check if 
		// point order was changed during conversion
		if (pointOrderWasChanged(p, cPoly))
			return true;
		return false;
	}

	/**
	 * Checks if polygon is oriented counter-clockwise.
	 * 
	 * @param p		Input polygon.
	 * @return True, if polygon is oriented clockwise.
	 */
	protected static boolean isClockwiseOriented(MTBPolygon2D p) {
		C2DPolygon cPoly = MTBtoC2D(p); 
		// C2DPolygon is always oriented clockwise, so we have to check if 
		// point order was changed during conversion
		if (pointOrderWasChanged(p, cPoly))
			return false;
		return true;
	}

	/**
	 * Simplifies the given polygon.
	 * 
	 * @param p		Input polygon.
	 * @return Simplified polygon without crossings.
	 */
	protected static MTBPolygon2D makePolySimple(MTBPolygon2D p) {
		C2DPolygon cPoly = MTBtoC2D(p);
		
		// access private function of C2DPolygon by reflections
		Method method;
    try {
	    method = 
	    	cPoly.getClass().getDeclaredMethod("EliminateCrossingLines");
	    method.setAccessible(true);
			Object returnVal = method.invoke(cPoly);
			if (((Boolean)returnVal).booleanValue()) {
				ArrayList<C2DPoint> pList = new ArrayList<C2DPoint>();
				cPoly.GetPointsCopy(pList);
				MTBPolygon2D pRev = C2DtoMTB(cPoly);
				// make sure that point ordering is not changed according to 
				// simplification procedure
				if (isCounterClockwiseOriented(p))
					pRev.reversePolypoints();
				return pRev;
			}
    } catch (Exception e) {
    	System.err.println("[Polygon2D_GeoPolygons] could not simplify "
    			+ "polygon, eliminination of crossing lines failed!");
	    e.printStackTrace();
    }
		return p;
	}

	/**
	 * Checks if a point lies inside of the polygon.
	 * <p>
	 * Note that the boundary does not belong to the interior.
	 * 
	 * @param p		Input polygon.
	 * @param x		x coordinate of point to check.
	 * @param y		y coordinate of point to check.
	 * @return		True if point is inside of the polygon.
	 */
	protected static boolean containsPoint(MTBPolygon2D p, 
			double x, double y) {
		C2DPoint cp = new C2DPoint(x, y);
		C2DPolygon cPoly = MTBtoC2D(p); 
		return cPoly.Contains(cp);
	}
	
	/**
	 * Converts a MiToBo polygon to C2D data type.
	 * <p>
	 * Note that the result polygon is always in clockwise ordering, 
	 * independent of how the input polygon was sorted.
	 * 
	 * @param mPoly	Input MiToBo polygon.
	 * @return	Resulting C2D polygon.
	 */
	protected static C2DPolygon MTBtoC2D(MTBPolygon2D mPoly) {
		ArrayList<C2DPoint> pList = new ArrayList<C2DPoint>();
		for (Point2D.Double p : mPoly.points) {
			pList.add(new C2DPoint(p.x, p.y));
		}
		return new C2DPolygon(pList, false);
	}

	/**
	 * Converts a C2D polygon to the MiToBo polygon data type.
	 * <p>
	 * Note that the result polygon is always in clockwise ordering.
	 * 
	 * @param mPoly	Input C2D polygon.
	 * @return	Resulting MiToBo polygon, always sorted clockwise.
	 */
	protected static MTBPolygon2D C2DtoMTB(C2DPolygon cPoly) {
		Vector<Point2D.Double> pList = new Vector<Point2D.Double>();
		ArrayList<C2DPoint> cPoints = new ArrayList<C2DPoint>(); 	
		cPoly.GetPointsCopy(cPoints);
		for (int i=0; i<cPoints.size(); ++i) {
			pList.add(new Point2D.Double(cPoints.get(i).x, cPoints.get(i).y));
		}
		return new MTBPolygon2D(pList, true);
	}
	
	/**
	 * Checks if order of points was changed during initialization.
	 * <p>
	 * C2DPolygons are always ordered clockwise. Consequently, if the 
	 * point order was changed during initialization the input polygon
	 * has a counter-clockwise ordering, otherwise it is ordered 
	 * clockwise.
	 * 
	 * @param mPoly		Input MiToBo polygon.
	 * @param cPoly		Input C2DPolygon.
	 * @return	True, if point ordering was changed.
	 */
	private static boolean pointOrderWasChanged(
			MTBPolygon2D mPoly, C2DPolygon cPoly) {
		ArrayList<C2DPoint> pList = new ArrayList<C2DPoint>();
		cPoly.GetPointsCopy(pList);
		if (   mPoly.getPoints().get(1).x == pList.get(1).x 
				&& mPoly.getPoints().get(1).y == pList.get(1).y)
			return false;
		return true;
	}
}
