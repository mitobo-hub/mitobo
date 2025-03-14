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

import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;

/**
 * Operator that fits an ellipse to a given set of 2D points.
 * <p>
 * An ellipse is a special case of a general quadratic curve defined by 
 * the following implicit equation:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * a \\cdot x^2 + 2 b \\cdot x \\cdot y + c \\cdot y^2 + 2 d \\cdot x 
 * 	+ 2 e \\cdot y + f = 0
 * \\end{equation*}}
 * <p>
 * The curve equation yields always an elliptical solution if
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * b^2 - 4ac = \\vec p^{\\;T} \\cdot
 * \\left[\\begin{array}{cccccc}
 * 	0 & 0 & -2 & 0 & 0 & 0 \\\\
 * 	0 & 1 & 0 & 0 & 0 & 0 \\\\
 * 	-2 & 0 & 0 & 0 & 0 & 0 \\\\
 * 	0 & 0 & 0 & 0 & 0 & 0 \\\\
 * 	0 & 0 & 0 & 0 & 0 & 0 \\\\
 * 	0 & 0 & 0 & 0 & 0 & 0 
 * \\end{array}\\right] \\cdot \\vec{p} = 
 * 		\\vec{p}^{\\;T} \\cdot C \\cdot \\vec{p} < 0
 * \\end{equation*}}
 * This operator estimates the parameters of a conic in a least square 
 * sense. By including the above constraint, however, it guarantees to 
 * always return an ellipse as solution. The ellipse fits a given set
 * of points in a least-squares sense.
 * <p>
 * The algorithm basically solves an eigenvalue problem:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * D^TD\\vec{p} = S \\vec p = \\lambda C \\vec p
 * \\end{equation*}}
 * with matrix D containing the data
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * D = 
 * \\left[\\begin{array}{cccccc}
 * 	x_1^2 & x_1 \\cdot y_1 & y_1^2 & x_1 & y_1 & 1 \\\\
 * 	\\vdots & \\vdots & \\vdots & \\vdots & \\vdots & \\vdots \\\\
 * 	x_1^N & x_N \\cdot y_N & y_N^2 & x_N & y_N & 1
 * \\end{array}\\right]
 * \\end{equation*}}
 * and the parameter vector
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * \\vec p = 
 * \\left[\\begin{array}{c}
 * 	a \\\\ b \\\\ c \\\\ d \\\\ e \\\\ f
 * \\end{array}\\right]
 * \\end{equation*}} 
 * This general eigenvalue problem can be transformed into an ordinary
 * eigenvalue problem as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{eqnarray*} 
 * & D^TD\\vec{p} = S \\vec p &= \\lambda C \\vec p \\\\
 * \\Rightarrow& S^{-1} S \\vec p &= \\lambda S^{-1} C \\vec p \\\\
 * \\Rightarrow& \\frac{1}{\\lambda} \\vec p &= S^{-1} C \\vec p
 * \\end{eqnarray*}}
 * The solution vector is given by the (denormalized) eigenvector 
 * corresponding to the only negative eigenvalue calculated.
 * <p>
 * To ensure that the scatter matrix S is invertible, the algorithm 
 * expects a point set containing at least 6 points in general position,
 * i.e. matrix S is required to have full rank. Note that this 
 * condition is also not fulfilled if the given points are all lying
 * exactly (!) on a single ellipse, i.e., if the algebraic error is
 * zero. In that case the operator will return no result, but the
 * estimated curve will be null.
 * <p>
 * The result is returned as an object of class 
 * {@link MTBQuadraticCurve2D} with type ellipse.
 * <p>
 * Details concerning the algorithm can be found in<br> Pilu et al., 
 * <i>Ellipse-specific Direct Least-Square Fitting</i>, Proc. of 
 * IEEE International Conference on Image Processing, Lausanne, 
 * September 1996 
 * (<a href="http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/PILU1/demo.html">Webpage</a>).
 * 
 * @see MTBQuadraticCurve2D
 * @see FitQuadraticCurveToPointSet
 * @see <a href="http://mathworld.wolfram.com/Ellipse.html">
 * 	Ellipses on WolframMathWorld</a>
 * @see <a href="http://ejml.org">Efficient Java Matrix Library</a> 
 * 
 * @author Birgit Moeller
 */
@SuppressWarnings("javadoc")
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE,
		level=Level.STANDARD)
public class FitEllipseToPointSet extends MTBOperator {
	
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
	public FitEllipseToPointSet() throws ALDOperatorException {
		// nothing to do here
	}		
	
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.points.size() < 6) 
			throw new ALDOperatorException(
					OperatorExceptionType.VALIDATION_FAILED, 
					"[FitEllipseToPointSet] less than 6 points provided, " 
							+ "cannot do anything!");
	}
	 
	@Override
	protected void operate() throws ALDOperatorException {

		// normalize data
		double mx= 0, my= 0;
		double minX= Double.MAX_VALUE, minY= Double.MAX_VALUE;
		double maxX= -Double.MAX_VALUE, maxY= -Double.MAX_VALUE;
		for (int i=0; i<this.points.size(); ++i) {
			Point2D.Double p = this.points.get(i);
			mx += p.x;
			my += p.y;
			if (p.x < minX)
				minX= p.x;
			if (p.x > maxX)
				maxX= p.x; 
			if (p.y < minY)
				minY= p.y;
			if (p.y > maxY)
				maxY= p.y; 
		}
		mx /= this.points.size();
		my /= this.points.size();
		double sx= (maxX - minX)/2.0; 
		double sy= (maxY - minY)/2.0;
		
		double[] normPointX = new double[this.points.size()];
		double[] normPointY = new double[this.points.size()];
		for (int i=0; i<this.points.size(); ++i) {
			Point2D.Double p = this.points.get(i);
			normPointX[i] = (p.x - mx)/sx;
			normPointY[i] = (p.y - my)/sy;
		}
		
		// init data matrix
		double[][] matrixDataD = new double[this.points.size()][6];
		double px, py;
		for (int i=0; i<this.points.size(); ++i) {
			px = normPointX[i];
			py = normPointY[i];
			matrixDataD[i][0] = px*px;
			matrixDataD[i][1] = px*py;
			matrixDataD[i][2] = py*py;
			matrixDataD[i][3] = px;
			matrixDataD[i][4] = py;
			matrixDataD[i][5] = 1.0;
		}
		
		// use Efficient Java Matrix Library (EJML) for eigenvalue problem
		SimpleMatrix matD = new SimpleMatrix(matrixDataD);
		SimpleMatrix matDt = matD.transpose();
		SimpleMatrix matS = matDt.mult(matD);
		
		// check if matrix S has full rank
		if (MatrixFeatures_DDRM.rank(matS.getMatrix(), 1e-200) < 6) {
			throw new ALDOperatorException(
					OperatorExceptionType.OPERATE_FAILED, 
					"[FitEllipseToPointSet] scatter matrix not invertible!");
		}
		
		// constraint matrix
		double[][] matrixDataC = new double[6][6];
		for (int i=0; i<6; ++i) {
			for (int j=0; j<6; ++j) {
				matrixDataD[i][j] = 0.0;
			}
		}
		matrixDataC[0][2] = -2.0;
		matrixDataC[2][0] = -2.0;
		matrixDataC[1][1] =  1.0;
		SimpleMatrix matC = new SimpleMatrix(matrixDataC);
		
		SimpleMatrix invS = matS.invert();
		SimpleMatrix matSinvC = invS.mult(matC);
		
		// solve eigenvalue problem and get estimated parameters
		try {
			@SuppressWarnings("unchecked")
			SimpleEVD<SimpleMatrix> evd = matSinvC.eig();

			int negID = -1;
			for (int i=0; i<6; ++i) {
				if (   evd.getEigenvalue(i).isReal()
						&& !Double.isNaN(evd.getEigenvalue(i).real)
						&& !Double.isInfinite(evd.getEigenvalue(i).real)
						&& evd.getEigenvalue(i).real < 0) {
					if (negID != -1)
						throw new ALDOperatorException(
								OperatorExceptionType.OPERATE_FAILED, 
								"[FitEllipseToPointSet] something went wrong, " 
										+ "more than one negative eigenvalue!");
					negID = i;
				}
			}

			if (negID == -1)
				throw new ALDOperatorException(
						OperatorExceptionType.OPERATE_FAILED, 
						"[FitEllipseToPointSet] no negative eigenvalue found!");

			SimpleMatrix solution = evd.getEigenVector(negID);
			double a1 = solution.get(0,0);
			double a2 = solution.get(1,0);
			double a3 = solution.get(2,0);
			double a4 = solution.get(3,0);
			double a5 = solution.get(4,0);
			double a6 = solution.get(5,0);

			// denormalize and copy parameters
			// (details of denormalization can be found in the Matlab sample 
			//  script available from the webpage of the paper)
			double[] params = new double[6];
			params[0] =      a1*sy*sy;
			params[1] =      a2*sx*sy;
			params[2] =      a3*sx*sx;
			params[3] = -2.0*a1*sy*sy*mx -     a2*sx*sy*my + a4*sx*sy*sy;
			params[4] =     -a2*sx*sy*mx - 2.0*a3*sx*sx*my + a5*sx*sx*sy;
			params[5] =   a1*sy*sy*mx*mx + a2*sx*sy*mx*my + a3*sx*sx*my*my 
					- a4*sx*sy*sy*mx - a5*sx*sx*sy*my + a6*sx*sx*sy*sy;	
			
			// correct the parameters according to our curve definition; in
			// our datatype the parameters b, d and e are scaled by 2.0
			params[1] /= 2.0;
			params[3] /= 2.0;
			params[4] /= 2.0;
			
			// init result object
			this.curve = new MTBQuadraticCurve2D(params, true);
		} catch(Exception e) {
			throw new ALDOperatorException(
					OperatorExceptionType.OPERATE_FAILED, 
					"[FitEllipseToPointSet] eigen decomposition failed!");
		}
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

