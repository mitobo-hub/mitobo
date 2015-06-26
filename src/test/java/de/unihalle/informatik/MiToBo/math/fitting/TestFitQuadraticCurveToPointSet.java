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

package de.unihalle.informatik.MiToBo.math.fitting;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D.CurveType;

/**
 * JUnit test class for {@link FitQuadraticCurveToPointSet}.
 * 
 * @author moeller
 */
public class TestFitQuadraticCurveToPointSet {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	@Test
	public void testQuadraticCurve2D() {		
		
		this.testParameterEstimation();
		
	}
	
	/**
	 * Test of fitting routine.
	 */
	protected void testParameterEstimation() {

		Vector<Point2D.Double> points = new Vector<Point2D.Double>();

		/*
		 * Tests are performed based on examples from   
		 * Rutter, <i>Geometry of Curves</i>, Chapman and Hall/CRC, 2000.
		 */
		
		// page 51, curve: x^2 + 4xy + 4y^2 - 4 sqrt(5) x - 3 sqrt(5) y = 0 
		points.add(new Point2D.Double(0.0, 3.0*Math.sqrt(5.0)/4.0));
		points.add(new Point2D.Double(1.0, 
			1.0/8.0*(Math.sqrt(8.0*Math.pow(5.0,1.5)+45.0)+3.0*Math.sqrt(5.0)-4.0)));
		points.add(new Point2D.Double(2.0,
		 -1.0/8.0*(Math.sqrt(16.0*Math.pow(5.0,1.5)+45.0)-3.0*Math.sqrt(5.0)+8.0)));
		points.add(new Point2D.Double(3.0, 
			1.0/8.0*(Math.sqrt(3.0)*Math.sqrt(8.0*Math.pow(5.0,1.5)+15.0)
					+ 3.0*Math.sqrt(5.0)-12.0)));
		
		// only four points, validation will fail
		boolean exceptionThrown = false;
		FitQuadraticCurveToPointSet fitter;
		try {
			fitter = new FitQuadraticCurveToPointSet();
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				exceptionThrown = true;
			}
			assertTrue("Validation expected to fail, but passed..?!", 
					exceptionThrown);

			// add fifth point and try again
			points.add(new Point2D.Double(4.0, 
				-1.0/8.0*(Math.sqrt(32.0*Math.pow(5.0,1.5)+45.0)
						- 3.0*Math.sqrt(5.0)+16.0)));
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				ex.printStackTrace();
				exceptionThrown = true;
			}
			assertFalse("Caught an unexpected exception...!?", exceptionThrown);
			MTBQuadraticCurve2D conic = fitter.getEstimatedCurve();
			assertTrue("Expected a parabola, got " + conic.getType(),
					conic.getType() == CurveType.CT_PARABOLA);
		
			// page 49, curve: 3x^2 + 2xy + 3y^2 - 6x +14y -101 = 0 
			points.clear();
			points.add(new Point2D.Double(-2.0, -7.0));
			points.add(new Point2D.Double(-1.0,  (2.0*Math.sqrt(78.0)-6.0)/3.0));
			points.add(new Point2D.Double( 0.0, -(4.0*Math.sqrt(22.0)+7.0)/3.0));
			points.add(new Point2D.Double( 1.0,  (2.0*Math.sqrt(94.0)-8.0)/3.0));
			points.add(new Point2D.Double( 2.0, -(8.0*Math.sqrt(6.0)+9.0)/3.0));
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				ex.printStackTrace();
				exceptionThrown = true;
			}
			assertFalse("Caught an unexpected exception...!?", exceptionThrown);
			conic = fitter.getEstimatedCurve();
			assertTrue("Expected an ellipse, got " + conic.getType(),
					conic.getType() == CurveType.CT_ELLIPSE);

			// page 54, curve: 6x^2 - 4xy + 3y^2 + 20x - 16y - 198 = 0 
			points.clear();
			points.add(new Point2D.Double(-2.0,  (1.0*Math.sqrt(658.0)+4.0)/3.0));
			points.add(new Point2D.Double(-1.0,  (4.0*Math.sqrt(42.0)+6.0)/3.0));
			points.add(new Point2D.Double( 0.0, -(1.0*Math.sqrt(658.0)-8.0)/3.0));
			points.add(new Point2D.Double( 1.0,  (2.0*Math.sqrt(154.0)+10.0)/3.0));
			points.add(new Point2D.Double( 2.0, -(1.0*Math.sqrt(546.0)-12.0)/3.0));
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				ex.printStackTrace();
				exceptionThrown = true;
			}
			assertFalse("Caught an unexpected exception...!?", exceptionThrown);
			conic = fitter.getEstimatedCurve();
			assertTrue("Expected an ellipse, got " + conic.getType(),
					conic.getType() == CurveType.CT_ELLIPSE);

			// page 54, curve: 3x^2 + 10xy + 3y^2 + 46x + 34y + 93 = 0 
			points.clear();
			points.add(new Point2D.Double(-2.0,  (2.0*Math.sqrt(10.0)-2.0)/3.0));
			points.add(new Point2D.Double(-1.0, -(2.0*Math.sqrt(6.0)-3.0)/3.0));
			points.add(new Point2D.Double( 0.0,  (2.0*Math.sqrt(10.0)+8.0)/3.0));
			points.add(new Point2D.Double( 1.0, -(2.0*Math.sqrt(22.0)-13.0)/3.0));
			points.add(new Point2D.Double( 2.0,  (2.0*Math.sqrt(42.0)+18.0)/3.0));
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				ex.printStackTrace();
				exceptionThrown = true;
			}
			assertFalse("Caught an unexpected exception...!?", exceptionThrown);
			conic = fitter.getEstimatedCurve();
			assertTrue("Expected an ellipse, got " + conic.getType(),
					conic.getType() == CurveType.CT_HYPERBOLA);

			// page 55, curve: 4x^2 - 4xy + y^2 + 8 sqrt(5)x + 6 sqrt(5)y - 15 = 0 
			points.clear();
			points.add(new Point2D.Double(-3.0, -26.64248104507279));
			points.add(new Point2D.Double(-2.0,  1.516472713530644));
			points.add(new Point2D.Double(-1.0, -18.94154940453322));
			points.add(new Point2D.Double( 0.0,  1.037762759915464));
			points.add(new Point2D.Double( 1.0, -8.616994084196463));
			fitter.setPointSet(points);
			exceptionThrown = false;
			try {
				fitter.runOp();
			} catch (Exception ex) {
				ex.printStackTrace();
				exceptionThrown = true;
			}
			assertFalse("Caught an unexpected exception...!?", exceptionThrown);
			conic = fitter.getEstimatedCurve();
			assertTrue("Expected a parabola, got " + conic.getType(),
					conic.getType() == CurveType.CT_PARABOLA);

		} catch (ALDOperatorException ex) {
			exceptionThrown = true;
		}
		assertFalse("Caught an exception on execution...!?", exceptionThrown);
	}

}