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

import java.util.Iterator;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link MTBSnake}.
 * 
 * @author moeller
 */
public class TestMTBSnake {

	private static final String IDS = "[MTBSnake]";
	
	private static final double accuracy = 10e-10;
	
	private MTBSnake testSnake;
	
	/**
	 * Original points of test snake.
	 */
	private Vector<MTBSnakePoint2D> spoints;
	
	/**
	 * Scaled points of test snake.
	 */
	private Vector<MTBSnakePoint2D> scpoints;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		this.spoints = new Vector<MTBSnakePoint2D>();
		this.spoints.add(new MTBSnakePoint2D(2.0, 1.0));
		this.spoints.add(new MTBSnakePoint2D(3.0, 1.0));
		this.spoints.add(new MTBSnakePoint2D(4.0, 2.0));
		this.spoints.add(new MTBSnakePoint2D(4.0, 3.0));
		this.spoints.add(new MTBSnakePoint2D(3.0, 4.0));
		this.spoints.add(new MTBSnakePoint2D(2.0, 4.0));
		this.spoints.add(new MTBSnakePoint2D(1.0, 3.0));
		this.spoints.add(new MTBSnakePoint2D(1.0, 2.0));
		int index = 0;
		for (MTBSnakePoint2D p: this.spoints) {
			p.setOldId(index);
			++index;
		}
		this.testSnake = new MTBSnake(this.spoints, true);

		// points scaled by 4
		double scale = 4.0;
		this.scpoints = new Vector<MTBSnakePoint2D>();
		this.scpoints.add(new MTBSnakePoint2D(2.0/scale, 1.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(3.0/scale, 1.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(4.0/scale, 2.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(4.0/scale, 3.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(3.0/scale, 4.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(2.0/scale, 4.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(1.0/scale, 3.0/scale));
		this.scpoints.add(new MTBSnakePoint2D(1.0/scale, 2.0/scale));
	}
	
	/**
	 * Test methods.
	 */
	@Test
	public void testMTBSnake() {

		// test empty constructor
		MTBSnake s = new MTBSnake();
		assertFalse(IDS + " new snake should never be closed...", s.isClosed());
		assertTrue(IDS + " new snake has no points...", s.getPointNum() == 0);
		assertTrue(IDS + " new snake has no points...", 
			s.getSnakePoints().isEmpty());
		assertTrue(IDS + " scale factor of new snake should be 1.0...", 
			Math.abs(s.getScaleFactor() - 1.0) < accuracy);

		// run tests on test snake
		assertTrue(IDS + " snake should be closed...", this.testSnake.isClosed());
		assertTrue(IDS + " snake has 8 points...", this.testSnake.getPointNum()==8);
		assertTrue(IDS + " point vector should contain 8 points...", 
			this.testSnake.getSnakePoints().size() == 8);
		assertTrue(IDS + " scale factor of the snake should be 1.0...", 
			Math.abs(this.testSnake.getScaleFactor() - 1.0) < accuracy);
		this.testDomain(this.testSnake);
		this.testNormalization(this.testSnake);
		this.testDerivatives(this.testSnake);
	}
	
	/**
	 * Test if calculation of domain and COM is correct.
	 * @param s	Snake to test.
	 */
	private void testDomain(MTBSnake s) {
		/*
		 * Remember the border of 5 pixel width added to the snake's 
		 * bounding box...
		 */
		assertTrue(IDS + " minimal x-coordinate should be -4, it's " 
			+ s.getDomainXMinCoordinate(), 
				Math.abs(s.getDomainXMinCoordinate() - -4) < accuracy);
		assertTrue(IDS + " maximal x-coordinate should be 9, it's " 
			+ s.getDomainXMaxCoordinate(), 
				Math.abs(s.getDomainXMaxCoordinate() - 9) < accuracy);
		assertTrue(IDS + " minimal y-coordinate should be -4, it's " 
			+ s.getDomainYMinCoordinate(), 
				Math.abs(s.getDomainYMinCoordinate() - -4) < accuracy);
		assertTrue(IDS + " maximal y-coordinate should be 9, it's " 
			+ s.getDomainYMaxCoordinate(), 
				Math.abs(s.getDomainYMaxCoordinate() - 9) < accuracy);
		assertTrue(IDS + " x size should be 14, it's " + s.getSizeX(), 
			s.getSizeX() == 14);
		assertTrue(IDS + " y size should be 14, it's " + s.getSizeY(), 
			s.getSizeY() == 14);
		assertTrue(IDS + " z size should be 1, it's " + s.getSizeZ(), 
			s.getSizeZ() == 1);
		assertTrue(IDS + " minimal visiblity map x-coordinate should be -4, it's " 
			+ s.getVisibilityMapXMinCoordinate(), 
				Math.abs(s.getVisibilityMapXMinCoordinate() - -4) < accuracy);
		assertTrue(IDS + " maximal visiblity map x-coordinate should be 9, it's " 
			+ s.getVisibilityMapXMaxCoordinate(), 
				Math.abs(s.getVisibilityMapXMaxCoordinate() - 9) < accuracy);
		assertTrue(IDS + " minimal visiblity map y-coordinate should be -4, it's " 
			+ s.getVisibilityMapYMinCoordinate(), 
				Math.abs(s.getVisibilityMapYMinCoordinate() - -4) < accuracy);
		assertTrue(IDS + " maximal visiblity map y-coordinate should be 9, it's " 
			+ s.getVisibilityMapYMaxCoordinate(), 
				Math.abs(s.getVisibilityMapYMaxCoordinate() - 9) < accuracy);
		assertTrue(IDS + " visiblity map x size should be 14, it's " 
			+ s.getVisibilityMapWidth(), s.getVisibilityMapWidth() == 14);
		assertTrue(IDS + " visiblity map y size should be 14, it's " 
			+ s.getVisibilityMapHeight(), s.getVisibilityMapHeight() == 14);
		
		// test center of mass
		assertTrue(IDS + " center of mass in x should be 2.5, it's " 
			+ s.getCOMx(), Math.abs(s.getCOMx() - 2.5) < accuracy);
		assertTrue(IDS + " center of mass in y should be 2.5, it's " 
			+ s.getCOMy(), Math.abs(s.getCOMy() - 2.5) < accuracy);
	}
	
	/**
	 * Test if normalization works correctly.
	 * @param s	Snake to test.
	 */
	private void testNormalization(MTBSnake s) {
		
		// normalize
		s.normalize(4.0);
		assertTrue(IDS + " scale factor after normalization should be 4.0...", 
			Math.abs(s.getScaleFactor() - 4.0) < accuracy);
		Vector<MTBSnakePoint2D> points = s.getSnakePoints();
		int index = 0;
		Iterator<MTBSnakePoint2D> it = this.scpoints.iterator();
		for (MTBSnakePoint2D p: points) {
			MTBSnakePoint2D target = it.next();
			assertTrue(IDS + "Scaled point with ID " 	
				+ index + " is different from target in x-coordinate!",
					Math.abs(p.x - target.x) < accuracy);
			assertTrue(IDS + "Scaled point with ID " 	
				+ index + " is different from target in y-coordinate!",
					Math.abs(p.y - target.y) < accuracy);
			++index;
		}
		
		// denormalize
		s.denormalize();
		points = s.getSnakePoints();
		index = 0;
		it = this.spoints.iterator();
		for (MTBSnakePoint2D p: points) {
			MTBSnakePoint2D target = it.next();
			assertTrue(IDS + "Denormalized point with ID " 	
				+ index + " is different from target in x-coordinate!",
					Math.abs(p.x - target.x) < accuracy);
			assertTrue(IDS + "Denormalized point with ID " 	
				+ index + " is different from target in y-coordinate!",
					Math.abs(p.y - target.y) < accuracy);
			++index;
		}
	}

	/**
	 * Test if calculation of derivatives is correct.
	 * @param s	Snake to test.
	 */
	private void testDerivatives(MTBSnake s) {
		// first order derivatives
		assertTrue(IDS + " dx at position 3 should be -1, it's " 
			+ s.getPartialDiffX(3), Math.abs(s.getPartialDiffX(3) - -1) < accuracy);
		assertTrue(IDS + " dx at position 0 should be 1, it's " 
			+ s.getPartialDiffX(0), Math.abs(s.getPartialDiffX(0) - 1) < accuracy);
		assertTrue(IDS + " dx at position 7 should be 1, it's " 
			+ s.getPartialDiffX(7), Math.abs(s.getPartialDiffX(7) - 1) < accuracy);
		assertTrue(IDS + " dy at position 3 should be 1, it's " 
			+ s.getPartialDiffY(3), Math.abs(s.getPartialDiffY(3) - 1) < accuracy);
		assertTrue(IDS + " dy at position 0 should be 0, it's " 
			+ s.getPartialDiffY(0), Math.abs(s.getPartialDiffY(0)) < accuracy);
		assertTrue(IDS + " dy at position 7 should be -1, it's " 
			+ s.getPartialDiffY(7), Math.abs(s.getPartialDiffY(7) - -1) < accuracy);
		// second order derivatives
		assertTrue(IDS + " dxdx at position 3 should be -1, it's " 
			+ s.getSndPartialDiffX(3), 
				Math.abs(s.getSndPartialDiffX(3) - -1) < accuracy);
		assertTrue(IDS + " dxdx at position 0 should be 0, it's " 
			+ s.getSndPartialDiffX(0), 
				Math.abs(s.getSndPartialDiffX(0)) < accuracy);
		assertTrue(IDS + " dxdx at position 7 should be 1, it's " 
			+ s.getSndPartialDiffX(7), 
				Math.abs(s.getSndPartialDiffX(7) - 1) < accuracy);
		assertTrue(IDS + " dydy at position 3 should be 0, it's " 
			+ s.getSndPartialDiffY(3), 
				Math.abs(s.getSndPartialDiffY(3)) < accuracy);
		assertTrue(IDS + " dydy at position 0 should be 1, it's " 
			+ s.getSndPartialDiffY(0), 
				Math.abs(s.getSndPartialDiffY(0) - 1) < accuracy);
		assertTrue(IDS + " dydy at position 7 should be 0, it's " 
			+ s.getSndPartialDiffY(7), 
				Math.abs(s.getSndPartialDiffY(7)) < accuracy);
		
		/*
		 * normalize the snake
		 */
		s.normalize(4.0);
		// first order derivatives
		assertTrue(IDS + " dx at position 3 should be -0.25, it's " 
			+ s.getPartialDiffX(3), Math.abs(s.getPartialDiffX(3) + 0.25) < accuracy);
		assertTrue(IDS + " dx at position 0 should be 0.25, it's " 
			+ s.getPartialDiffX(0), Math.abs(s.getPartialDiffX(0) - 0.25) < accuracy);
		assertTrue(IDS + " dx at position 7 should be 0.25, it's " 
			+ s.getPartialDiffX(7), Math.abs(s.getPartialDiffX(7) - 0.25) < accuracy);
		assertTrue(IDS + " dy at position 3 should be 0.25, it's " 
			+ s.getPartialDiffY(3), Math.abs(s.getPartialDiffY(3) - 0.25) < accuracy);
		assertTrue(IDS + " dy at position 0 should be 0, it's " 
			+ s.getPartialDiffY(0), Math.abs(s.getPartialDiffY(0)) < accuracy);
		assertTrue(IDS + " dy at position 7 should be -0.25, it's " 
			+ s.getPartialDiffY(7), Math.abs(s.getPartialDiffY(7) + 0.25) < accuracy);
		// second order derivatives
		assertTrue(IDS + " dxdx at position 3 should be -0.25, it's " 
			+ s.getSndPartialDiffX(3), 
				Math.abs(s.getSndPartialDiffX(3) - -0.25) < accuracy);
		assertTrue(IDS + " dxdx at position 0 should be 0, it's " 
			+ s.getSndPartialDiffX(0), 
				Math.abs(s.getSndPartialDiffX(0)) < accuracy);
		assertTrue(IDS + " dxdx at position 7 should be 0.25, it's " 
			+ s.getSndPartialDiffX(7), 
				Math.abs(s.getSndPartialDiffX(7) - 0.25) < accuracy);
		assertTrue(IDS + " dydy at position 3 should be 0, it's " 
			+ s.getSndPartialDiffY(3), 
				Math.abs(s.getSndPartialDiffY(3)) < accuracy);
		assertTrue(IDS + " dydy at position 0 should be 0.25, it's " 
			+ s.getSndPartialDiffY(0), 
				Math.abs(s.getSndPartialDiffY(0) - 0.25) < accuracy);
		assertTrue(IDS + " dydy at position 7 should be 0.25, it's " 
			+ s.getSndPartialDiffY(7), 
				Math.abs(s.getSndPartialDiffY(7)) < accuracy);
	}
}