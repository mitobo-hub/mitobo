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

import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D.CurveType;

/**
 * JUnit test class for {@link MTBQuadraticCurve2D}.
 * 
 * @author moeller
 */
public class TestMTBQuadraticCurve2D {

	/**
	 * Test object.
	 */
	MTBQuadraticCurve2D curve = null;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	@SuppressWarnings("javadoc")
  @Test
	public void testQuadraticCurve2D() {		
		
		this.testDetermineCurveType();
		this.testPointToEllipseDistance();
		
	}
	
	@SuppressWarnings("javadoc")
  public void testDetermineCurveType() {

		/*
		 * Tests are performed based on eexamples in 
		 * Koecher/Krieg, <i>Ebene Geometrie</i>, 3. Auflage, Springer, 2007.
		 * 
		 * Examples are to be found on page 222. 
		 */

		// curve: 7x^2 + 2xy -18x - 17 = 0 
		double[] params = new double[]{-7.0/17.0, -1.0/17.0, 0, 0, 9.0/17.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// curve: x^2 - 2xy + y^2 + 30x = 0 
		params = new double[]{1.0, -1.0, 1.0, 15.0, 0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a parabola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_PARABOLA);

		// curve: 4xy + 16x + 8y -3 = 0 
		params = new double[]{0, -2.0/3.0, 0, -8.0/3.0, -4.0/3.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// curve: x^2 + 8y^2 - 4x - 8 = 0 
		params = new double[]{-1.0/8.0, 0, -1.0/2.0, 1.0/4.0, 0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected an empty curve, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_ELLIPSE);

		/*
		 * Tests are performed based on examples in   
		 * Rutter, <i>Geometry of Curves</i>, Chapman and Hall/CRC, 2000.
		 */

		// page 46, curve: 3x^2 - 10xy + 3y^2 + 16x - 16y + 8 = 0 
		params = new double[]
				{3.0/8.0, -5.0/8.0, 3.0/8.0, 1.0, -1.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// page 49, curve: 3x^2 + 2xy + 3y^2 - 6x + 14y - 101 = 0 
		params = new double[]
				{-3.0/101.0, -1.0/101.0, -3.0/101.0, 3.0/101.0, -7.0/101.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected an ellipse, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_ELLIPSE);

		// page 51, curve: x^2 + 4xy + 4y^2 - 4 sqrt(5) x - 3 sqrt(5) y = 0 
		params = new double[]
				{1.0, 2.0, 4.0, -2*Math.sqrt(5.0), -1.5*Math.sqrt(5.0), 0.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a parabola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_PARABOLA);

		// page 54, top, curve: 3x^2 + 10xy + 3y^2 + 46x + 34y + 93 = 0 
		params = new double[]
				{3.0/93.0, 5.0/93.0, 3.0/93, 23.0/93.0, 17.0/93.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// page 54, top, curve: x^2 - 6xy - 7y^2 - 16x - 48y - 88 = 0 
		params = new double[]
				{-1.0/88.0, 3.0/88.0, 7.0/88.0, -8.0/88.0, 24.0/88.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);
		
		// page 54, top, curve: 4x^2 - 10xy + 4y^2 + 6x - 12y - 9 = 0 
		params = new double[]
				{-4.0/9.0, 5.0/9.0, -4.0/9.0, -3.0/9.0, 6.0/9.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// page 54, top, curve: 3x^2 - 10xy + 3y^2 + 8x - 24y - 8 = 0 
		params = new double[]
				{-3.0/8.0, 5.0/8.0, -3.0/8.0, -4.0/8.0, 12.0/8.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a hyperbola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_HYPERBOLA);

		// page 54, bottom, curve: 6x^2 - 4xy + 3y^2 + 20x - 16y - 198 = 0 
		params = new double[]
				{-6.0/198.0, 2.0/198.0, -3.0/198.0, -10.0/198.0, 8.0/198.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected an ellipse, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_ELLIPSE);

		// page 54, bottom, curve: 5x^2 - 4xy + 8y^2 - 18x + 36y - 279 = 0 
		params = new double[]
				{-5.0/279.0, 2.0/279.0, -8.0/279.0, 9.0/279.0, -18.0/279.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected an ellipse, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_ELLIPSE);

		// page 54, bottom, curve: 3x^2 + 2xy + 3y^2 + 14x + 20y - 183 = 0 
		params = new double[]
				{-3.0/183.0, -1.0/183.0, -3.0/183.0, -7.0/183.0, -10.0/183.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected an ellipse, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_ELLIPSE);

		// page 55, curve: 4x^2 - 4xy + y^2 + 8 sqrt(5) x + 6 sqrt(5) y - 15 = 0 
		params = new double[]
				{-4.0/15.0, 2.0/15.0, -1.0/15.0, 
					-4.0*Math.sqrt(5.0)/15.0, -3.0*Math.sqrt(5.0)/15.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a parabola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_PARABOLA);

		// page 55, curve: x^2 - 2xy + y^2 - 6 sqrt(2) x - 2 sqrt(2) y - 6 = 0 
		params = new double[]
				{-1.0/6.0, 1.0/6.0, -1.0/6.0, 
					3.0*Math.sqrt(2.0)/6.0, -1.0*Math.sqrt(2.0)/6.0, 1.0};
		this.curve = new MTBQuadraticCurve2D(params, true);
		assertTrue("Expected a parabola, got " + this.curve.getType(), 
				this.curve.getType() == CurveType.CT_PARABOLA);

	}
	
	@SuppressWarnings("javadoc")
  public void testPointToEllipseDistance() {
		boolean caughtException = false;
		try {
			
			Point2D.Double p;
			double distance;
			
			// ellipse centered at origin and default orientation
			double[] params= new double[]{0.0, 0.0, 2.0, 1.0, 0};
			this.curve = new MTBQuadraticCurve2D(params, false);

			p = new Point2D.Double(2.0, 0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be 0, but is " + distance, 
					Math.abs(distance-0)<1.0e-15);
			p = new Point2D.Double(3.0, 0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be 1.0, but is " + distance, 
					Math.abs(distance-1.0)<1.0e-15);
			p = new Point2D.Double(0.0, 0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be 1.0, but is " + distance, 
					Math.abs(distance-1.0)<1.0e-15);
			
			// ellipse centered at origin and rotated by 45 degrees
			params= new double[]{0.0, 0.0, 2.0, 1.0, 45.0};
			this.curve = new MTBQuadraticCurve2D(params, false);
			p = new Point2D.Double(5.0, 5.0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be sqrt(50)-2.0, but is " + distance, 
					Math.abs(distance-(Math.sqrt(50.0)-2.0))<1.0e-10);
			
			// ellipse centered at (-1,-1) and rotated by 45 degrees
			params= new double[]{-1.0, -1.0, 2.0, 1.0, 45.0};
			this.curve = new MTBQuadraticCurve2D(params, false);
			
			p = new Point2D.Double(5.0, 5.0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be sqrt(72)-2.0, but is " + distance, 
					Math.abs(distance-(Math.sqrt(72)-2.0))<1.0e-10);
			p = new Point2D.Double(-7.0, -7.0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be sqrt(72)-2.0, but is " + distance, 
					Math.abs(distance-(Math.sqrt(72)-2.0))<1.0e-10);
			p = new Point2D.Double(-1.0, -1.0);
			distance = this.curve.getDistanceEuclideanPointToEllipse(p);
			assertTrue("Distance should be 1.0, but is " + distance, 
					Math.abs(distance-1.0)<1.0e-10);

		} catch(Exception e) {
			caughtException = true;
		}
		assertFalse("Caught an exception during distance calculation!",
				caughtException);
	}
}