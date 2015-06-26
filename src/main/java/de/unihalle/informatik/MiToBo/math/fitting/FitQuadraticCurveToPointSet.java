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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations .ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;
import de.unihalle.informatik.MiToBo.core.operator.*;

import java.awt.geom.Point2D;
import java.util.Vector;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

/**
 * Operator that fits a quadratic curve or conic to a given set of 2D points.
 * <p>
 * A general quadratic curve is defined by the following implicit equation:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * a \\cdot x^2 + 2 b \\cdot x \\cdot y + c \\cdot y^2 + 2 d \\cdot x 
 * 	+ 2 e \\cdot y + f = 0
 * \\end{equation*}}
 * <p>
 * This operator estimates the parameters of a conic in a least square sense
 * from a given set of 2D points. The point set is required to contain 
 * at least 5 points which is the minimal number of points necessary to 
 * uniquely estimate the parameters.
 * <p>
 * The result is given by an object of type {@link MTBQuadraticCurve2D} which
 * can be asked for the concrete type of the estimated conic, e.g., if it is
 * an ellipse, hyperbola or parabola.
 * 
 * @see MTBQuadraticCurve2D
 * @see <a href="http://mathworld.wolfram.com/Ellipse.html">
 * 	Ellipses on WolframMathWorld</a>
 * @see <a href="http://ejml.org">Efficient Java Matrix Library</a> 
 * 
 * @author Birgit Moeller
 */
@SuppressWarnings("javadoc")
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE,
		level=Level.STANDARD)
public class FitQuadraticCurveToPointSet extends MTBOperator {
	
	/**
	 * Input set of points.
	 */
	@Parameter(label = "Point Set", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Point Set.")
	private Vector<Point2D.Double> points = null;
	
	/**
	 * Estimated curve.
	 */
	@Parameter(label = "Curve", dataIOOrder = 1,
			direction = Parameter.Direction.OUT, 
			description = "Estimated curve.")
	private MTBQuadraticCurve2D curve = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of operate failure.
	 */
	public FitQuadraticCurveToPointSet() throws ALDOperatorException {
		// nothing to do here
	}		
	
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.points.size() < 5) 
			throw new ALDOperatorException(
					OperatorExceptionType.VALIDATION_FAILED, 
					"[FitQuadraticCurveToPointSet] less than 5 points provided, " 
							+ "cannot do anything!");
	}
	 
	@Override
	protected void operate() {

		// allocate matrices
		double[][] matrixData = new double[this.points.size()][6];

		// copy input points to matrices
		for (int i=0; i<this.points.size(); ++i) {
			Point2D.Double p = this.points.get(i);
			matrixData[i][0] =     p.x*p.x;
			matrixData[i][1] = 2.0*p.x*p.y;
			matrixData[i][2] =     p.y*p.y;
			matrixData[i][3] = 2.0*p.x;
			matrixData[i][4] = 2.0*p.y;
			matrixData[i][5] = 1.0;
		}
		
		// use Efficient Java Matrix Library (EJML) for SVD
		// (Jama and Apache Commons Math do not handle matrices with m<n correctly)
		SimpleMatrix matA = new SimpleMatrix(matrixData);
		@SuppressWarnings("unchecked")
    SimpleSVD<SimpleMatrix> s = matA.svd();
		
		// get solution as last column of V matrix and copy to parameter vector
		SimpleMatrix V=s.getV();
		double[] params = new double[6];
		for (int j=0; j<6; ++j) {
			params[j] = V.get(j, 5);
		}
		
		// init result object
		this.curve = new MTBQuadraticCurve2D(params, true);
	}
	
	/**
	 * Set input set of 2D points.
	 * @param p	Set of points to process.
	 */
	public void setPointSet(Vector<Point2D.Double> p) {
		this.points = p;
	}
	
	/**
	 * Get estimated curve.
	 * @return	Estimated curve.
	 */
	public MTBQuadraticCurve2D getEstimatedCurve() {
		return this.curve;
	}
	
}

