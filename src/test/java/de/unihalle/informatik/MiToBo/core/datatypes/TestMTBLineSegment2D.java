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

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;

/**
 * JUnit test class for {@link MTBLineSegment2D}.
 * 
 * @author moeller
 */
public class TestMTBLineSegment2D {

	/*
	 * Some test objects.
	 */
	
	private MTBLineSegment2D line= null;
	private MTBLineSegment2D lineA= null;
	private MTBLineSegment2D lineB= null;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	@Test
	public void testContainsPoint() {
		this.line= new MTBLineSegment2D(0,0,4,0);
		assertTrue("Point is on the segment!!!",
			this.line.containsPoint(2, 0));
		assertTrue("Point is on the segment!!!",
			this.line.containsPoint(0, 0));
		assertTrue("Point is on the segment!!!",
			this.line.containsPoint(4, 0));
		assertFalse("Point is not on the segment!!!",
			this.line.containsPoint(5, 0));
		assertFalse("Point is not on the segment!!!",
			this.line.containsPoint(5, 1));
		assertFalse("Point is not on the segment!!!",
			this.line.containsPoint(2, 2));

		this.line= new MTBLineSegment2D(0,0,4,0);
		assertTrue("Point is located left of segment, not right...",
			this.line.getOrientation(2, 2)>0);
		assertTrue("Point is located right of segment, not left...",
			this.line.getOrientation(2,-2)<0);
		assertTrue("Point is located on the segment...",
			Math.abs(this.line.getOrientation(2,0))<MTBConstants.epsilon);
		assertTrue("Point is located on the line to which the segment belongs...",
			Math.abs(this.line.getOrientation(5,0))<MTBConstants.epsilon);
	}
	
	@Test
	public void testIntersections() {
		MTBLineSegment2D seg_a= new MTBLineSegment2D(0, 0, 4, 4);
		MTBLineSegment2D seg_b= new MTBLineSegment2D(0, 4, 4, 0);
		assertTrue("LineSegments do intersect!",seg_a.intersectsLine(seg_b)); 
		seg_b= new MTBLineSegment2D(1, 0, 4, 0);
		assertTrue("LineSegments do not intersect!",!seg_a.intersectsLine(seg_b)); 
		seg_b= new MTBLineSegment2D(0, 0, 4, 0);
		assertTrue("LineSegments do intersect!",seg_a.intersectsLine(seg_b)); 

		Point2D.Double intersect= null;
		this.lineA= new MTBLineSegment2D(0,0,4,4);
		this.lineB= new MTBLineSegment2D(0,4,4,0);
		intersect= this.lineA.getIntersection(this.lineB);
		assertTrue("Intersection should be (2,2)",
			intersect.equals(new Point2D.Double(2,2)));
		this.lineA= new MTBLineSegment2D(0,0,4,4);
		this.lineB= new MTBLineSegment2D(0,1,4,1);
		intersect= this.lineA.getIntersection(this.lineB);
		assertTrue("Intersection should be (1,1)",
			intersect.equals(new Point2D.Double(1,1)));
		this.lineA= new MTBLineSegment2D(0,1,4,1);
		this.lineB= new MTBLineSegment2D(1,0,1,4);
		intersect= this.lineA.getIntersection(this.lineB);
		assertTrue("Intersection should be (1,1)",
			intersect.equals(new Point2D.Double(1,1)));
	}
}