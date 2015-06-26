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

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link MTBPolygon2D}.
 * 
 * @author moeller
 */
public class TestMTBPolygon2D {

	/**
	 * Test object.
	 */
	MTBPolygon2D poly = null;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	@Test
	public void testPolygon2D() {		
		
		// test polygon: sorted counter-clockwise, area= 16 
		this.poly= new MTBPolygon2D();
		this.poly.addPoint(0, 0);
		this.poly.addPoint(4, 0);
		this.poly.addPoint(4, 4);
		this.poly.addPoint(0, 4);
		assertTrue("Area is 16, and nothing else!", 
			this.poly.getSignedArea() == 16);
		double [] box= this.poly.getBoundingBox();
		assertTrue("Xmin= 0!", box[0]==0);
		assertTrue("Ymin= 0!", box[1]==0);
		assertTrue("Xmax= 4!", box[2]==4);
		assertTrue("Ymax= 4!", box[3]==4);		

		// test polygon: sorted clockwise, area= -16 
		this.poly= new MTBPolygon2D();
		this.poly.addPoint(0, 0);
		this.poly.addPoint(0, 4);
		this.poly.addPoint(4, 4);
		this.poly.addPoint(4, 0);
		assertTrue("Area is -16, and nothing else!", 
			this.poly.getSignedArea()==-16);
		assertTrue("Polygon should be ordered clockwise...",
			this.poly.isOrderedClockwise());
		assertTrue("Polygon is convex!", this.poly.isConvex());

		// revert the polygon
		this.poly.reversePolypoints();
		assertTrue("Polygon should be ordered counter-clockwise...",
			this.poly.isOrderedCounterClockwise());

		// test polygon: sorted clockwise
		this.poly= new MTBPolygon2D();
		this.poly.addPoint(0, 0);
		this.poly.addPoint(0, 4);
		this.poly.addPoint(4, 4);
		this.poly.addPoint(2, 2);
		this.poly.addPoint(4, 0);
		assertTrue("Polygon should be ordered clockwise...",
			this.poly.isOrderedClockwise());
		assertFalse("Polygon is not convex!", this.poly.isConvex());

		// non-convex polygon: test routine that checks if points are inside
		this.poly= new MTBPolygon2D();
		this.poly.addPoint(0, 0);
		this.poly.addPoint(0, 4);
		this.poly.addPoint(4, 4);
		this.poly.addPoint(2, 2);
		this.poly.addPoint(4, 0);
		assertTrue("Polygon is simple!", this.poly.isSimple());
		assertFalse("Polygon is not convex!",	this.poly.isConvex());
		assertTrue("Point is located outside, not inside!",
			!this.poly.containsPoint(2,2));
		assertTrue("Point is located inside, not outside!",
			this.poly.containsPoint(3,1));
		assertTrue("Point is located inside, not outside!",
			this.poly.containsPoint(0,0));
		assertTrue("Point is located inside, not outside!",
			this.poly.containsPoint(0,2));
		assertFalse("Point is located outside, not inside!",
			this.poly.containsPoint(3,2));
		assertFalse("Point is located outside, not inside!",
			this.poly.containsPoint(2,6));
		assertFalse("Point is located outside, not inside!",
			this.poly.containsPoint(0,4.001));

		// test orientation: polygon is oriented clockwise
		assertTrue("Polygon should be ordered clockwise...",
			this.poly.isOrderedClockwise());
		assertTrue("Polygon is simple!", this.poly.isSimple());

		// revert polygon points, orientation of points changes as well
		this.poly.reversePolypoints();
		assertFalse("Polygon should be ordered counter-clockwise...",
			this.poly.isOrderedClockwise());
	}
}