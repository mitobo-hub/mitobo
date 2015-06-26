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

package de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link MTBSnakePoint2D}.
 * 
 * @author moeller
 */
public class TestMTBSnakePoint2D {

	private static final String IDS = "[MTBSnakePoint2D]";
	
	private static final double accuracy = 10e-10;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here...
	}
	
	/**
	 * Test methods.
	 */
	@Test
	public void testMTBPoint2D() {

		MTBSnakePoint2D p = new MTBSnakePoint2D();
		assertTrue(IDS + " new point should have an old ID of -1,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == -1);
		assertTrue(IDS + " new point should have a x-coordinate of 0," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX()) < accuracy);
		assertTrue(IDS + " new point should have a y-coordinate of 0,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY()) < accuracy);
		this.testSetters(p);
		this.testClone(p);
		
		p = new MTBSnakePoint2D(3.333, -0.75);
		assertTrue(IDS + " new point should have an old ID of -1,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == -1);
		assertTrue(IDS + " new point should have a x-coordinate of 3.333," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 3.333) < accuracy);
		assertTrue(IDS + " new point should have a y-coordinate of -0.75,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() - -0.75) < accuracy);
		this.testSetters(p);
		this.testClone(p);
		
		p = new MTBSnakePoint2D(new Point2D.Double(1.0/3.0, -100.0/3.0));
		assertTrue(IDS + " new point should have an old ID of -1,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == -1);
		assertTrue(IDS + " new point should have a x-coordinate of 1/3," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 1.0/3.0) < accuracy);
		assertTrue(IDS + " new point should have a y-coordinate of -100/3,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() + 100.0/3.0) < accuracy);
		this.testSetters(p);
		this.testClone(p);
	}
	
	/**
	 * Test setters for given point.
	 * @param p	Point to test.
	 */
	private void testSetters(MTBSnakePoint2D p) {
		p.setOldId(25);
		assertTrue(IDS + " after set ID should be 25,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == 25);
		p.setLocation(0.6666, 1.0/7.0);
		assertTrue(IDS + " after set ID should be 25,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == 25);
		assertTrue(IDS + " after set x-coordinate should be 0.6666," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 0.6666) < accuracy);
		assertTrue(IDS + " after set y-coordinate should be 1/7,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() - 1.0/7.0) < accuracy);
		p.setLocation(new Point2D.Double(12.5, 4));
		assertTrue(IDS + " after set ID should be 25,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == 25);
		assertTrue(IDS + " after set x-coordinate should be 12.5," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 12.5) < accuracy);
		assertTrue(IDS + " after set y-coordinate should be 4,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() - 4) < accuracy);
		p.setLocation(new Point2D.Double(0.0003, -0.0001), -5);
		assertTrue(IDS + " after set ID should be -5,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == -5);
		assertTrue(IDS + " after set x-coordinate should be 0.0003," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 0.0003) < accuracy);
		assertTrue(IDS + " after set y-coordinate should be -0.0001,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() - -0.0001) < accuracy);
		p.setLocation(10e-15, 10e3, (int)-10000.5);
		assertTrue(IDS + " after set ID should be -10000,"
			+ "it's " + p.getOldId() + "...!", p.getOldId() == -10000);
		assertTrue(IDS + " after set x-coordinate should be 10e-15," 
			+ "it's " + p.getX() + "...!", Math.abs(p.getX() - 10e-15) < accuracy);
		assertTrue(IDS + " after set y-coordinate should be 10e3,"
			+ "it's " + p.getY() + "...!", Math.abs(p.getY() - 10e3) < accuracy);
	}
	
	/**
	 * Test clone routine for given point.
	 * @param p	Point to test.
	 */
	private void testClone(MTBSnakePoint2D p) {
		MTBSnakePoint2D q = p.clone();
		assertTrue(IDS + " cloned point should have the same ID, but we have " 
			+ p.getOldId() + " vs. " + q.getOldId() + "...",
				q.getOldId() == p.getOldId());
		assertTrue(IDS + " cloned point should have the x-coordinate, but we have "
			+ p.getX() + " vs. " + q.getX() + "...",
				Math.abs(q.getX() - p.getX()) < accuracy);
		assertTrue(IDS + " cloned point should have the y-coordinate, but we have "
			+ p.getY() + " vs. " + q.getY() + "...",
				Math.abs(q.getY() - p.getY()) < accuracy);
	}
}