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
import java.io.IOException;
import java.util.Collection;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import de.jstacs.algorithms.optimization.ConstantStartDistance;
import de.jstacs.algorithms.optimization.DifferentiableFunction;
import de.jstacs.algorithms.optimization.DimensionException;
import de.jstacs.algorithms.optimization.EvaluationException;
import de.jstacs.algorithms.optimization.Optimizer;
import de.jstacs.algorithms.optimization.TerminationException;
import de.jstacs.algorithms.optimization.termination.SmallDifferenceOfFunctionEvaluationsCondition;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.math.fitting.FitEllipseToPointSet;

/**
 * General quadratic curve.
 * <p>
 * A curve is defined by the following implicit equation:
 *	
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * a \\cdot x^2 + 2 b \\cdot x \\cdot y + c \\cdot y^2 + 2 d \\cdot x 
 * 	+ 2 e \\cdot y + f = 0
 * \\end{equation*}}
 * <p>
 * The type of the curve is derived based on the following matrices:<br>
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * A = \\left[ \\begin{array}{ccc}
 *         a & b & d \\\\
 *         b & c & e \\\\
 *         d & e & f \\\\
 * 				 \\end{array} 
 * \\right] 
 * \\end{equation*}}
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * S = \\left[ \\begin{array}{cc}
 *         a & b \\\\
 *         b & c 
 * 				 \\end{array} 
 * \\right] 
 * \\end{equation*}}
 * For further details about how different types of curves are 
 * identified refer to the documentation of {@link CurveType}. 
 * <p>
 * If the curve is an ellipse, but not a circle, i.e.
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{equation*} 
 * a \\neq c 
 * \\end{equation*}}
 * additional parameters can be calculated: 
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{eqnarray*} 
 * x_0 &=& \\frac{cd-be}{b^2-ac} \\\\
 * y_0 &=& \\frac{ae-bd}{b^2-ac} \\\\
 * A &=& \\sqrt{\\frac{2(ae^2+cd^2+fb^2-2bde-acf)}{(b^2-ac)
 * 	\\left( \\sqrt{(a-c)^2+4b^2} - (a+c) \\right)}} \\\\
 * B &=& \\sqrt{\\frac{2(ae^2+cd^2+fb^2-2bde-acf)}{(b^2-ac)
 * 	\\left( - \\sqrt{(a-c)^2+4b^2} - (a+c) \\right)}} \\\\
 * \\phi &=& \\begin{cases}
 *  	 		          0, & \\text{for}\\; b=0 \\;\\text{and}\\;a<c \\\\
 *    \\frac{\\pi}{2}, & \\text{for}\\; b=0 \\;\\text{and}\\;a>c \\\\
 *  	\\frac{1}{2}cot^{-1}(\\frac{a-c}{2b}), & 
 *  			\\text{for}\\; b\\neq 0 \\;\\text{and}\\;a<c \\\\
 *  	\\frac{\\pi}{2} + \\frac{1}{2}cot^{-1}(\\frac{a-c}{2b}), & 
 *  			\\text{for}\\; b\\neq 0 \\;\\text{and}\\;a>c 
 *           \\end{cases}
 * \\end{eqnarray*}}
 * where the first two define the center of the ellipse, and A and B 
 * refer to the semi-major and semi-minor axes lengths. Note that it is 
 * not strictly defined which axis A or B defines the major axis and 
 * which one the minor. The fifth parameter refers to the orientation 
 * angle of the ellipse in counter-clockwise direction from the x-axis 
 * to the major axis.
 * <p>
 * For details about the definition of various types of curves refer 
 * to<br>
 * Koecher/Krieg, <i>Ebene Geometrie</i>, 3. Auflage, 
 * Springer Verlage, 2007. 
 * 
 * @see FitQuadraticCurveToPointSet
 * @see FitEllipseToPointSet
 * @see <a href="http://mathworld.wolfram.com/Ellipse.html">
 * 	Ellipses on WolframMathWorld</a>
 * 
 * @author Birgit Moeller
 */
@SuppressWarnings("javadoc")
@ALDParametrizedClass
public class MTBQuadraticCurve2D {

	/**
	 * Possible types of curve.
	 */
	public static enum CurveType {
		/**
		 * Single point.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) > 0 \\wedge det(A) = 0 
		 * \\end{equation*}}
		 */
		CT_POINT,
		/**
		 * Ellipse.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) > 0 \\wedge trace(S) \\cdot det(A) < 0 
		 * \\end{equation*}}
		 */
		CT_ELLIPSE,
		/**
		 * Hyperbola.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) < 0 \\wedge det(A) \\neq 0 
		 * \\end{equation*}}
		 */
		CT_HYPERBOLA,
		/**
		 * Pair of intersecting lines.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) < 0 \\wedge det(A) = 0 
		 * \\end{equation*}}
		 */
		CT_INTERSECTING_LINES,
		/**
		 * Parabola. 
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) = 0 \\wedge det(A) \\neq 0 
		 * \\end{equation*}}
		 */
		CT_PARABOLA,
		/**
		 * Double line.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) = 0 \\wedge rank(A) = 1 
		 * \\end{equation*}}
		 */
		CT_DOUBLE_LINE,
		/**
		 * Two parallel lines.  
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) = 0 \\wedge rank(A) = 2 \\wedge A \\;\\text{is indefinit}
		 * \\end{equation*}}
		 */
		CT_TWO_PARALLEL_LINES,
		/**
		 * Implicit equation without solutions.
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) > 0 \\wedge trace(A) \\cdot det(A) > 0 
		 * \\end{equation*}}
		 * or
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
		 * \\begin{equation*} 
		 * det(S) = 0 \\wedge rank(A) = 2 \\wedge A 
		 * 							\\;\\text{is semi-definit}
		 * \\end{equation*}}
		 */
		CT_EMPTY_CURVE
	}

	/**
	 * Local accuracy for comparisons of values against zero.
	 */
	private static final double accuracy = 10e-10;
	
	/**
	 * Implicit curve parameter a.
	 */
	protected double a;
	
	/**
	 * Implicit curve parameter b.
	 */
	protected double b;
	
	/**
	 * Implicit curve parameter c.
	 */
	protected double c;
	
	/**
	 * Implicit curve parameter d.
	 */
	protected double d;
	
	/**
	 * Implicit curve parameter e.
	 */
	protected double e;
	
	/**
	 * Implicit curve parameter f.
	 */
	protected double f;

	/**
	 * Ellipse center in x-direction.
	 */
	@ALDClassParameter(label = "Center in x")
	protected double centerX;
	
	/**
	 * Ellipse center in x-direction.
	 */
	@ALDClassParameter(label = "Center in y")
	protected double centerY;
	
	/**
	 * Half-length of axis a.
	 */
	@ALDClassParameter(label = "Semi-length of axis a.")
	protected double semiAxisLengthA;
	
	/**
	 * Half-length of axis b.
	 */
	@ALDClassParameter(label = "Semi-length of axis b.")
	protected double semiAxisLengthB;
	
	/**
	 * Ellipse orientation in degrees.
	 * <p>
	 * The orientation specifies the angle between x-axis of the 
	 * coordinate system and the major axis of the ellipse with regard
	 * to an upper-left coordinate system.
	 */
	@ALDClassParameter(label = "Orientation")
	protected double orientation;

	/**
	 * Type of curve.
	 */
	protected CurveType curveType;
	
	/**
	 * Default constructor.
	 */
	public MTBQuadraticCurve2D() {
		this.curveType = CurveType.CT_EMPTY_CURVE;
	}
	
	/**
	 * Constructor.
	 * <p>
	 * If an implicit curve is to be initialized, the parameters in the
	 * given array are interpreted as the parameters a, b, c, d, e and f
	 * of an implicit equation. If an explicit ellipse is to be
	 * initialized the parameters are interpreted as center in x and y,
	 * half-length of major and minor axes and ellipse orientation.
	 * <p>
	 * Note that explicit ellipse parameters are not automatically 
	 * transformed into implicit ones, however, if implicit parameters
	 * are provided internally an explicit representation is also 
	 * initialized.
	 *
	 * @param parameters	Set of curve parameters.
	 * @param implicit		If true, parameters define an implicit curve.
	 */
	public MTBQuadraticCurve2D(double[] parameters, boolean implicit) {
		if (implicit) {
			this.a = parameters[0];
			this.b = parameters[1];
			this.c = parameters[2];
			this.d = parameters[3];
			this.e = parameters[4];
			this.f = parameters[5];
			this.curveType = this.determineTypeOfCurve();
			this.calcEllipseParameters();
		}
		else {
			this.centerX = parameters[0];
			this.centerY = parameters[1];
			this.semiAxisLengthA = parameters[2];
			this.semiAxisLengthB = parameters[3];
			this.orientation = parameters[4];
			this.curveType = CurveType.CT_ELLIPSE;
			this.a = Double.NaN;
			this.b = Double.NaN;
			this.c = Double.NaN;
			this.d = Double.NaN;
			this.e = Double.NaN;
			this.f = Double.NaN;
		}
	}
	
	/**
	 * Figure out which kind of curve we have.
	 */
	protected CurveType determineTypeOfCurve() {
		// pre-calculate some important curve characteristics
		double detA = this.a*this.c + this.b*this.e*this.d 
				+ this.b*this.c*this.d - this.c*this.d*this.d  
				- this.b*this.b - this.a*this.e*this.e;
		
		double detS = this.a*this.c - this.b*this.b;

		// detS > 0 && detA == 0 
		if (detS > accuracy && Math.abs(detA) < accuracy) 
			return CurveType.CT_POINT;
		
		double traceS = this.a + this.c;

		// detS > 0 && traceS * detA < 0
		if (detS > accuracy && traceS * detA < -accuracy)
			return CurveType.CT_ELLIPSE;
		
		// detS < 0 && detA != 0
		if (detS < -accuracy && Math.abs(detA) > accuracy)
			return CurveType.CT_HYPERBOLA;
		
		// detS < 0 && detA == 0
		if (detS < -accuracy && Math.abs(detA) < accuracy)
			return CurveType.CT_INTERSECTING_LINES;

		// detS == 0 && detA != 0
		if (Math.abs(detS) < accuracy && Math.abs(detA) > accuracy)
			return CurveType.CT_PARABOLA;
		
		Matrix A = new Matrix(3, 3);
		A.set(0, 0, this.a);
		A.set(0, 1, this.b);
		A.set(0, 2, this.d);
		A.set(1, 0, this.b);
		A.set(1, 1, this.c);
		A.set(1, 2, this.e);
		A.set(2, 0, this.d);
		A.set(2, 1, this.e);
		A.set(2, 2, 1.0);
		int rankA = A.rank();
		
		// detS == 0 && rankA == 1
		if (Math.abs(detS) < accuracy && rankA == 1)
			return CurveType.CT_DOUBLE_LINE;
		
		EigenvalueDecomposition eig = A.eig();
		double[] eigenVals = eig.getRealEigenvalues();
		// If a matrix has positive and negative eigenvalues, 
		// it is indefinit.
		boolean pos = false;
		boolean neg = false;
		for (int i=0; i<eigenVals.length; ++i)
			if (eigenVals[i] > 0)
				pos = true;
			else if (eigenVals[i] <0)
				neg = true;
		
		// detS == 0 && rankA == 2 && pos && neg
		if (Math.abs(detS) < accuracy && rankA == 2 && pos && neg)
			return CurveType.CT_TWO_PARALLEL_LINES;

		// check for singularity/degeneracy: detS > 0 && traceS * detA > 0
		if (detS > accuracy && (traceS * detA) > accuracy)
			return CurveType.CT_EMPTY_CURVE;

		// A matrix is semi-definite if all eigenvalues are smaller or equal 
		// than zero or larger or equal than zero.
		
		// check for positive semi-definit
		pos = false;
		for (int i=0; i<eigenVals.length; ++i) {
			if (eigenVals[i] >= 0)
				pos = true;
			else {
				pos = false;
				break;
			}
		}
		// otherwise check for negative semi-definit
		if (!pos) {
			neg = false;
			for (int i=0; i<eigenVals.length; ++i) {
				if (eigenVals[i] <= 0)
					neg = true;
				else {
					neg = false;
					break;
				}
			}
		}
		
		// detS == 0 && rankA == 2 && (pos || neg)
		if (Math.abs(detS) < accuracy && rankA == 2 && (pos || neg))
			return CurveType.CT_EMPTY_CURVE;
		
		// default
		return CurveType.CT_EMPTY_CURVE;
	}

	/**
	 * Calculate concrete ellipse parameters from implicit equation.
	 * <p>
	 * Note that calculation of ellipse parameters is only possible for
	 * non-degenerate ellipses.
	 */
	protected void calcEllipseParameters() {
		double divisor = this.b*this.b - this.a*this.c;
		this.centerX = (this.c*this.d - this.b*this.e)/divisor;
		this.centerY = (this.a*this.e - this.b*this.d)/divisor;

		double enumerator = 2.0 * (this.a*this.e*this.e 
				+ this.c*this.d*this.d + this.f*this.b*this.b 
				- 2.0*this.b*this.d*this.e - this.a*this.c*this.f);
		double denominatorMajor = (this.b*this.b - this.a*this.c)
				* ( Math.sqrt(    (this.a-this.c)*(this.a-this.c) 
						+ 4*this.b*this.b )
						- (this.a + this.c) );
		this.semiAxisLengthA = Math.sqrt(enumerator/denominatorMajor);
		double denominatorMinor = (this.b*this.b - this.a*this.c)
				* (-Math.sqrt(    (this.a-this.c)*(this.a-this.c) 
						+ 4*this.b*this.b )
						- (this.a + this.c) );
		this.semiAxisLengthB = Math.sqrt(enumerator/denominatorMinor);
		
		// determine orientation
		if (Math.abs(this.b)<accuracy && this.a < this.c) {
			this.orientation = 0;
		}
		else if (Math.abs(this.b)<accuracy && this.a > this.c) {
			this.orientation = Math.PI/2.0;
		}
		else if (Math.abs(this.b)>accuracy && this.a < this.c) {
			this.orientation = 0.5 * Math.atan(2*this.b/(this.a-this.c));
		}
		else if (Math.abs(this.b)>accuracy && this.a > this.c) {
			this.orientation = Math.PI/2.0 
					+ 0.5 * Math.atan(2*this.b/(this.a-this.c));				
		}
		else {
			this.orientation = Double.NaN;
		}
		// convert from radiant to degrees
		this.orientation = 180.0 / Math.PI * this.orientation;
	}
	
	/**
	 * Normalizes the implicit parameters so that f=1.0.
	 * <p>
	 * Such a normalization might be beneficial to handle numerical
	 * issues if the parameter values are rather large.<br>
	 * Note that the normalization does not change the parameters if the 
	 * absolute value of f is smaller than one.
	 */
	public void normalizeImplicitParameters() {
		if (Math.abs(this.f) < 1.0)
			return;
		this.a /= this.f;
		this.b /= this.f;
		this.c /= this.f;
		this.d /= this.f;
		this.e /= this.f;
	}
	
	/**
	 * Prints parameters of curve to standard output.
	 */
	public void print() {
		System.out.println("Curve parameters:");
		System.out.println("=================");
		System.out.println("- type: " + this.curveType);
		System.out.println("- implicit: a = " + this.a 
				+ " , b = " + this.b + " , c = " + this.c
				+ " , d = " + this.d + " , e = " + this.e + " , f = " + this.f);
		System.out.println("- explicit: xc = " + this.centerX 
				+ " , yc = " + this.centerY + " , A/2 = " + this.semiAxisLengthA
				+ " , B/2 = " + this.semiAxisLengthB 
				+ " , theta = " + this.orientation + "\n");
	}
	
	/**
	 * Returns the type of the curve.
	 * @return	Type of curve.
	 */
	public CurveType getType() {
		return this.curveType;
	}
	
	/**
	 * Get value of implicit parameter a.
	 * @return	Value of a.
	 */
	public double getParameterA() {
		return this.a;
	}

	/**
	 * Get value of implicit parameter b.
	 * @return	Value of b.
	 */
	public double getParameterB() {
		return this.b;
	}

	/**
	 * Get value of implicit parameter c.
	 * @return	Value of c.
	 */
	public double getParameterC() {
		return this.c;
	}

	/**
	 * Get value of implicit parameter d.
	 * @return	Value of d.
	 */
	public double getParameterD() {
		return this.d;
	}

	/**
	 * Get value of implicit parameter e.
	 * @return	Value of e.
	 */
	public double getParameterE() {
		return this.e;
	}
	
	/**
	 * Get value of implicit parameter f.
	 * @return	Value of f.
	 */
	public double getParameterF() {
		return this.f;
	}

	/**
	 * Get coordinate of center in x.
	 * @return	Center x-coordinate.
	 */
	public double getCenterX() {
		return this.centerX;
	}
	
	/**
	 * Get coordinate of center in y.
	 * @return	Center y-coordinate.
	 */
	public double getCenterY() {
		return this.centerY;
	}
	
	/**
	 * Get half-length of axis A.
	 * @return	Half-length of axis A.
	 */
	public double getSemiLengthAxisA() {
		return this.semiAxisLengthA;
	}
	
	/**
	 * Get half-length of axis B.
	 * @return	Half-length of axis B.
	 */
	public double getSemiLengthAxisB() {
		return this.semiAxisLengthB;
	}
	
	/**
	 * Get orientation.
	 * @return	Orientation in degrees.
	 */
	public double getOrientation() {
		return this.orientation;
	}
	
	/**
	 * Returns the algebraic distance of the given point set.
	 * <p>
	 * The algebraic distance is defined as follows:
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
	 * \\begin{equation*} 
	 * d = \\sum_{i=1}^N \\left(
	 * 		a \\cdot p_{i,x}^2 + 2 b \\cdot p_{i,x} \\cdot p_{i,y} 
	 * 	+ c \\cdot p_{i,y}^2 + 2 d \\cdot p_{i,x}	+ 2 e \\cdot p_{i,y} + f
	 * 		\\right)^2
	 * \\end{equation*}}
	 * Note that the implicit parameters of the curve must have been
	 * provided before, if only the explicit parameters are given the 
	 * result is undefined.
	 * 
	 * @return Algebraic distance of the points.
	 */
	public double getDistanceAlgebraic(Collection<Point2D.Double> ps) {
		double algDist = 0;
		double dist = 0;
		for (Point2D.Double p: ps) {
			dist = this.a*p.x*p.x + 2.0*this.b*p.x*p.y + this.c*p.x*p.y
					+ 2.0*this.d*p.x + 2.0*this.e*p.y + this.f;
			algDist += dist*dist;
		}
		return algDist;
	}	
	
	/**
	 * Returns the point on ellipse closest to given point.
	 * <p>
	 * The closest point on the ellipse contour is searched for by using 
	 * Newton's method. The target point is the point on the ellipse 
	 * which tangent vector is perpendicular to the vector between the 
	 * given point and the closest point on the ellipse we are looking 
	 * for. For details refer to the documentation
	 * of {@link DistanceTargetFunction}.
	 * <p>
	 * Note that if the curve is not of type {@link CurveType.CT_ELLIPSE}
	 * null is returned.
	 * 
	 * @see Robert Nuernberg, Imperial College London, 2006, 
	 * 			<a href="http://www.ma.ic.ac.uk/~rn/distance2ellipse.pdf">
	 * 			Distance from a Point to an Ellipse</a>
	 * 
	 * @return Euclidean distance of the point to the ellipse.
	 * @throws Exception 
	 * @throws EvaluationException 
	 * @throws IOException 
	 * @throws TerminationException 
	 * @throws DimensionException 
	 */
	public Point2D.Double getClosestPointOnEllipse(Point2D.Double p) 
			throws DimensionException, TerminationException, IOException, 
				EvaluationException, Exception {
		if (this.curveType != CurveType.CT_ELLIPSE)
			return null;

		// shift and rotate point relative to default ellipse at origin
		double sx = p.x - MTBQuadraticCurve2D.this.centerX; 
		double sy = p.y - MTBQuadraticCurve2D.this.centerY;
		double theta = -MTBQuadraticCurve2D.this.orientation;
		double thetaRad = theta/180.0 * Math.PI;
		double srx = sx*Math.cos(thetaRad) - sy*Math.sin(thetaRad); 
		double sry = sx*Math.sin(thetaRad) + sy*Math.cos(thetaRad); 
		Point2D.Double psr = new Point2D.Double(srx, sry);

		// extract initial guess
		double initialGuess = Math.atan2(
				MTBQuadraticCurve2D.this.semiAxisLengthA*psr.x,
				MTBQuadraticCurve2D.this.semiAxisLengthB*psr.y);
		double[] valueArray = new double[]{initialGuess};
		Optimizer.optimize(Optimizer.QUASI_NEWTON_BFGS, 
				new DistanceTargetFunction(psr), valueArray,
				new SmallDifferenceOfFunctionEvaluationsCondition(1.0e-10), 
				10e-3, 
				new ConstantStartDistance(0.01), null);
		double bestAngle = valueArray[0];
		
		// calculate closest point on ellipse using Newton's method
		double epx = 
				MTBQuadraticCurve2D.this.semiAxisLengthA*Math.cos(bestAngle);
		double epy = 
				MTBQuadraticCurve2D.this.semiAxisLengthB*Math.sin(bestAngle);
		
		// rotate and shift closest point backwards
		double ox = epx*Math.cos(-thetaRad) - epy*Math.sin(-thetaRad);
		double oy = epx*Math.sin(-thetaRad) + epy*Math.cos(-thetaRad); 
		ox += MTBQuadraticCurve2D.this.centerX;
		oy += MTBQuadraticCurve2D.this.centerY;
		return new Point2D.Double(ox, oy);
	}	

	/**
	 * Returns the Euclidean distance of a point to an ellipse.
	 * <p>
	 * The Euclidean distance of a point to an ellipse is defined as the
	 * point's distance to the closest point on the ellipse.<br> 
	 * The distance is calculated by using Newton's method to search for 
	 * the point on the ellipse which tangent vector is perpendicular to 
	 * the vector between the given point and the closest point on the 
	 * ellipse we are looking for. For details refer to the documentation
	 * of {@link DistanceTargetFunction}.
	 * <p>
	 * Note that if the curve is not of type {@link CurveType.CT_ELLIPSE}
	 * the value {@link Double.NaN} is returned.
	 * 
	 * @see Robert Nuernberg, Imperial College London, 2006, 
	 * 			<a href="http://www.ma.ic.ac.uk/~rn/distance2ellipse.pdf">
	 * 			Distance from a Point to an Ellipse</a>
	 * 
	 * @return Euclidean distance of the point to the ellipse.
	 * @throws Exception 
	 * @throws EvaluationException 
	 * @throws IOException 
	 * @throws TerminationException 
	 * @throws DimensionException 
	 */
	public double getDistanceEuclideanPointToEllipse(Point2D.Double p) 
			throws DimensionException, TerminationException, IOException, 
				EvaluationException, Exception {
		if (this.curveType != CurveType.CT_ELLIPSE)
			return Double.NaN;

		// check if point is equal to ellipse origin, if so distance can be 
		// directly calculated from the semi-axes lengths
		if (   Math.abs(p.x-MTBQuadraticCurve2D.this.centerX) < 1.0e-20
				&& Math.abs(p.y-MTBQuadraticCurve2D.this.centerY) < 1.0e-20) {
			return (  MTBQuadraticCurve2D.this.semiAxisLengthA 
					    < MTBQuadraticCurve2D.this.semiAxisLengthB) ?
					    		MTBQuadraticCurve2D.this.semiAxisLengthA :	
					    			MTBQuadraticCurve2D.this.semiAxisLengthB;
		}
		
		// shift and rotate point relative to default ellipse at origin
//		double sx = p.x - MTBQuadraticCurve2D.this.centerX; 
//		double sy = p.y - MTBQuadraticCurve2D.this.centerY;
//		double theta = -MTBQuadraticCurve2D.this.orientation;
//		double thetaRad = theta/180.0 * Math.PI;
//		double srx = sx*Math.cos(thetaRad) - sy*Math.sin(thetaRad); 
//		double sry = sx*Math.sin(thetaRad) + sy*Math.cos(thetaRad); 
		Point2D.Double psr = new Point2D.Double(p.x, p.y);

		// extract closest point on ellipse contour
		Point2D.Double closestPoint = this.getClosestPointOnEllipse(p);

		// shift and rotate point relative to default ellipse at origin
		double epx = closestPoint.x;
		double epy = closestPoint.y;
//		sx = cpx - MTBQuadraticCurve2D.this.centerX; 
//		sy = cpy - MTBQuadraticCurve2D.this.centerY;
//		double epx = sx*Math.cos(thetaRad) - sy*Math.sin(thetaRad); 
//		double epy = sx*Math.sin(thetaRad) + sy*Math.cos(thetaRad); 

		// calculate Euclidean distance
		return Math.sqrt((psr.x-epx)*(psr.x-epx) + (psr.y-epy)*(psr.y-epy));
	}	
	
	/**
	 * Distance function for a point's distance to an ellipse.
	 * <p>
	 * This class implements the following target function:
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
	 * \\begin{equation*} 
	 * f(\\theta) = \\left( 
	 * 			(\\alpha^2 - \\beta^2) \\cos(\\theta) \\sin(\\theta) 
	 * 		- x \\alpha \\sin(\\theta) + y \\beta \\cos(\\theta)
	 * 							\\right)^2
	 * \\end{equation*}}
	 * and its derivative
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
	 * \\begin{equation*} 
	 * f^\\prime(\\theta) = 2.0 \\cdot f(\\theta) \\cdot \\left( 
	 * 			(\\alpha^2 - \\beta^2) (\\cos(\\theta)^2 - \\sin(\\theta)^2) 
	 * 		- x \\alpha \\cos(\\theta) - y \\beta \\sin(\\theta)
	 * 							\\right)^2
	 * \\end{equation*}}
	 * 
	 * @author moeller
	 */
	private class DistanceTargetFunction extends DifferentiableFunction {

		/**
		 * Point for which to calculate the distance to the ellipse.
		 */
		private Point2D.Double point;
		
		/**
		 * Default constructor.
		 * @param p	Point to consider.
		 */
		public DistanceTargetFunction(Point2D.Double p) {
			this.point = p;
    }
		
		@Override
    public double evaluateFunction(double[] angle) {
			double alpha = MTBQuadraticCurve2D.this.semiAxisLengthA;
			double beta = MTBQuadraticCurve2D.this.semiAxisLengthB;
			double fx = (alpha*alpha - beta*beta)
	    		* Math.cos(angle[0])*Math.sin(angle[0]) 
	    		- this.point.x*alpha*Math.sin(angle[0])
	    		+ this.point.y*beta*Math.cos(angle[0]);
			return fx*fx;
    }

		@Override
    public int getDimensionOfScope() {
	    return 1;
    }

		@Override
    public double[] evaluateGradientOfFunction(double[] angle) {
			double alpha = MTBQuadraticCurve2D.this.semiAxisLengthA;
			double beta = MTBQuadraticCurve2D.this.semiAxisLengthB;
			double fx = (alpha*alpha - beta*beta)
	    		* Math.cos(angle[0])*Math.sin(angle[0]) 
	    		- this.point.x*alpha*Math.sin(angle[0])
	    		+ this.point.y*beta*Math.cos(angle[0]);
			double dfx = (alpha*alpha - beta*beta)
	    		* (   Math.cos(angle[0])*Math.cos(angle[0]) 
	    				- Math.sin(angle[0])*Math.sin(angle[0])) 
	    		- this.point.x*alpha*Math.cos(angle[0])
	    		- this.point.y*beta*Math.sin(angle[0]);
			return new double[]{2 * dfx * fx};
		}
	}
}
