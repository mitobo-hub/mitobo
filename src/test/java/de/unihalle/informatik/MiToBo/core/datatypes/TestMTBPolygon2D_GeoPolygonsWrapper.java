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

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import uk.co.geolib.geolib.C2DPoint;
import uk.co.geolib.geopolygons.C2DPolygon;

/**
 * JUnit test class for {@link MTBPolygon2D_GeoPolygonsWrapper}.
 * 
 * @author moeller
 */
public class TestMTBPolygon2D_GeoPolygonsWrapper {

	/**
	 * Simple polygon for testing.
	 */
	MTBPolygon2D testPolySimple = null;
	
	/**
	 * Non-simple polygon for testing.
	 */
	MTBPolygon2D testPolyNonSimple = null;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// test polygon without self-overlaps
		Point2D.Double p01 = new Point2D.Double(2, 1);
		Point2D.Double p02 = new Point2D.Double(3, 1);
		Point2D.Double p03 = new Point2D.Double(4, 1);
		Point2D.Double p04 = new Point2D.Double(5, 2);
		Point2D.Double p05 = new Point2D.Double(5, 3);
		Point2D.Double p06 = new Point2D.Double(5, 4);
		Point2D.Double p07 = new Point2D.Double(4, 5);
		Point2D.Double p08 = new Point2D.Double(3, 5);
		Point2D.Double p09 = new Point2D.Double(2, 5);
		Point2D.Double p10 = new Point2D.Double(1, 4);
		Point2D.Double p11 = new Point2D.Double(1, 3);
		Point2D.Double p12 = new Point2D.Double(1, 2);

		Vector<Point2D.Double> pVec = new Vector<Point2D.Double>();
		pVec.add(p01); pVec.add(p02);	pVec.add(p03); pVec.add(p04);
		pVec.add(p05); pVec.add(p06); pVec.add(p07); pVec.add(p08);
		pVec.add(p09); pVec.add(p10);	pVec.add(p11); pVec.add(p12);
		
		this.testPolySimple = new MTBPolygon2D(pVec, true);
		
		// test polygon with self-overlaps
		p01 = new Point2D.Double(2, 1);
		p02 = new Point2D.Double(3, 1);
		p03 = new Point2D.Double(4, 1);
		p04 = new Point2D.Double(5, 2);
		p05 = new Point2D.Double(2, 3);
		p06 = new Point2D.Double(5, 4);
		p07 = new Point2D.Double(4, 5);
		p08 = new Point2D.Double(3, 5);
		p09 = new Point2D.Double(2, 5);
		p10 = new Point2D.Double(1, 4);
		p11 = new Point2D.Double(4, 3);
		p12 = new Point2D.Double(1, 2);

		pVec = new Vector<Point2D.Double>();
		pVec.add(p01); pVec.add(p02);	pVec.add(p03); pVec.add(p04);
		pVec.add(p05); pVec.add(p06); pVec.add(p07); pVec.add(p08);
		pVec.add(p09); pVec.add(p10);	pVec.add(p11); pVec.add(p12);
		
		this.testPolyNonSimple = new MTBPolygon2D(pVec, true);
	}
	
	@Test
	public void testConvertMethods() {
		
		/*
		 * Simple polygon.
		 */
		
		C2DPolygon cPoly = 
			MTBPolygon2D_GeoPolygonsWrapper.MTBtoC2D(this.testPolySimple);
		// check center of mass
		C2DPoint center = cPoly.GetCentroid();
		assertTrue("Expected center in x is 3, got " + center.x + "...",
			center.x == 3.0);
		assertTrue("Expected center in y is 3, got " + center.y + "...",
			center.y == 3.0);
		// check number of points
		ArrayList<C2DPoint> pList = new ArrayList<C2DPoint>();
		cPoly.GetPointsCopy(pList);
		assertTrue("Polygon is expected to have 12 points, found " 
			+ pList.size() + "...", pList.size() == 12);
		// C2DPolygons are always ordered clockwise after creation!
		assertTrue("Polygon is to be ordered clockwise, " 
			+ "but it is not!",	cPoly.IsClockwise());
		// polygon is to be simple
		assertTrue("Polygon is to be simple, but it is not!", 
			!cPoly.HasCrossingLines());
		
		MTBPolygon2D mPoly =
			MTBPolygon2D_GeoPolygonsWrapper.C2DtoMTB(cPoly);
		assertTrue("Polygon is expected to have 12 points, found " 
			+ mPoly.getPointNum() + "...", mPoly.getPointNum() == 12);
		// check if points are identical
		int i = 0;
		for (C2DPoint p: pList) {
			assertTrue("Point " + i + " does not match!" +
				p.toString() + " versus " + mPoly.getPoints().get(i).toString(),
					   mPoly.getPoints().get(i).x == p.x 
					&& mPoly.getPoints().get(i).y == p.y);
			++i;
		}
		
		/*
		 * Non-simple polygon.
		 */

		cPoly =	MTBPolygon2D_GeoPolygonsWrapper.MTBtoC2D(this.testPolyNonSimple);
		// check center of mass
		center = cPoly.GetCentroid();
		assertTrue("Expected center in x is 3, got " + center.x + "...",
			center.x == 3.0);
		assertTrue("Expected center in y is 3, got " + center.y + "...",
			center.y == 3.0);
		// check number of points
		pList = new ArrayList<C2DPoint>();
		cPoly.GetPointsCopy(pList);
		assertTrue("Polygon is expected to have 12 points, found " 
			+ pList.size() + "...", pList.size() == 12);
		// C2DPolygons are always ordered clockwise after creation!
		assertTrue("Polygon is to be ordered clockwise," 
			+ "but it is not!",	cPoly.IsClockwise());
		// polygon is not to be simple
		assertTrue("Polygon is not simple, but class assumes that!", 
			cPoly.HasCrossingLines());
		
		mPoly = MTBPolygon2D_GeoPolygonsWrapper.C2DtoMTB(cPoly);
		assertTrue("Polygon is expected to have 12 points, found " 
			+ mPoly.getPointNum() + "...", mPoly.getPointNum() == 12);
		// check if points are identical
		i = 0;
		for (C2DPoint p: pList) {
			assertTrue("Point " + i + " does not match!" +
				p.toString() + " versus " + mPoly.getPoints().get(i).toString(),
		   			 mPoly.getPoints().get(i).x == p.x 
					&& mPoly.getPoints().get(i).y == p.y);
			++i;
		}
	}
	
	@Test
	public void testOrdering() {
		assertTrue("Polygon is not convex, but class thinks that it is!",
			!MTBPolygon2D_GeoPolygonsWrapper.isConvex(this.testPolySimple));
		assertTrue("Polygon is simple, but class thinks that it is not!",
			MTBPolygon2D_GeoPolygonsWrapper.isSimple(this.testPolySimple));
		assertTrue("Polygon is to be ordered counter-clockwise, " 
			+ "but class thinks that it is not!",
				MTBPolygon2D_GeoPolygonsWrapper.isCounterClockwiseOriented(
					this.testPolySimple));
		assertTrue("Polygon is to be ordered counter-clockwise, " 
			+ "but class thinks that it is not!",
				!MTBPolygon2D_GeoPolygonsWrapper.isClockwiseOriented(
					this.testPolySimple));
		assertTrue("Polygon is not convex, but class thinks that it is!",
			!MTBPolygon2D_GeoPolygonsWrapper.isConvex(this.testPolyNonSimple));
		assertTrue("Polygon is not simple, but class thinks that it is!",
			!MTBPolygon2D_GeoPolygonsWrapper.isSimple(this.testPolyNonSimple));
		assertTrue("Polygon is to be ordered counter-clockwise, " 
			+ "but class thinks that it is not!",
				MTBPolygon2D_GeoPolygonsWrapper.isCounterClockwiseOriented(
					this.testPolyNonSimple));
		assertTrue("Polygon is to be ordered counter-clockwise, " 
			+ "but class thinks that it is not!",
				!MTBPolygon2D_GeoPolygonsWrapper.isClockwiseOriented(
					this.testPolyNonSimple));
	}
	
	@Test
	public void testSimplification() {
		MTBPolygon2D simplePoly =
			MTBPolygon2D_GeoPolygonsWrapper.makePolySimple(this.testPolyNonSimple);
		assertTrue("Polygon is not convex, but class thinks that it is!",
			!MTBPolygon2D_GeoPolygonsWrapper.isConvex(simplePoly));
		assertTrue("Polygon is simple, but class thinks that it is not!",
			MTBPolygon2D_GeoPolygonsWrapper.isSimple(simplePoly));
		assertTrue("Polygon is to be ordered counter-clockwise, " 
			+ "but class thinks that it is not!",
				MTBPolygon2D_GeoPolygonsWrapper.isCounterClockwiseOriented(simplePoly));
	}
	
	@Test
	public void testContainsPoint() {
		assertTrue("Point (3,3) is inside of polygon, method says no!",
			MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this.testPolySimple, 3, 3));
		assertTrue("Point (0,0) is not inside of polygon, method says yes!",
			!MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this.testPolySimple, 0, 0));
		assertTrue("Point (5,1) is not inside of polygon, method says yes!",
			!MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this.testPolySimple, 5, 1));
		assertTrue("Point (2,1) is on the polygon, i.e. inside, " 
			+ "method says no!",
				MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this.testPolySimple, 2, 1));
		assertTrue("Point (1.9999999999,1) is not inside, method says yes!",
			MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this.testPolySimple, 
				1.99999999999999, 1));
	}
}