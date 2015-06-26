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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core;

/**
 * Class calculating approximations to the Dirac impulse function.
 * <p>
 * The approximations used here are implemented according to
 * <p>
 * <i>Chan and Vese, "An Active Contour Model without Edges", Scale-Space '99,
 * LNCS 1682, pp. 141-151, Springer 1999.</i>
 * <p>
 * Refer to {@link DeltaApproxHelper.ApproxVersion} for details about the
 * available approximations.
 * 
 * @author Birgit Moeller
 */
public class DeltaApproxHelper {
	/**
	 * Available approximations for the Heaviside function and its derivatives.
	 */
	public static enum ApproxVersion {
		/** 
		 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
		 * \\begin{eqnarray*}
		 *   H_{1,\\epsilon}(x) = \\hspace*{4cm}
		 *     	1, & \\text{if} \\; x > \\epsilon \\\\
		 *     	0, & \\text{if} \\; x < -\\epsilon \\\\
		 *     	\\frac{1}{2} \\cdot \\left[1 + \\frac{x}{\\epsilon} + 
		 * 	\\frac{1}{\\pi}\\sin\\left(\\frac{\\pi x}{\\epsilon}\\right)\\right] , & 
		 * 			\\text{if} \\; |x| \\leq \\epsilon \\
		 * \\end{eqnarray*}
		 * \\begin{eqnarray*}
		 *   \\delta_{1,\\epsilon}(x) = H^\\prime_{1,\\epsilon}(x) = \\hspace*{4cm}
		 *     	0, & \\text{if} \\; |x| > \\epsilon \\\\
		 *     	\\frac{1}{2\\epsilon} \\cdot \\left[1 + \\cos\\left(\\frac{\\pi x}{\\epsilon}\\right)\\right] , & 
		 * 			\\text{if} \\; |x| \\leq \\epsilon \\
		 * \\end{eqnarray*}}
		 */
		VERSION_1,
		/** 
		 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
		 * \\begin{eqnarray*}
		 *   H_{2,\\epsilon}(x) = 
		 *   \\frac{1}{2} \\cdot \\left( 1 + \\frac{2}{\\pi} 
		 *   		\\text{atan} \\left( \\frac{x}{\\epsilon} \\right) \\right)
		 * \\end{eqnarray*}
		 * \\begin{eqnarray*}
		 *   \\delta_{2,\\epsilon}(x) = H^\\prime_{2,\\epsilon}(x) =
		 *   \\frac{1}{\\pi} \\cdot 
		 *   			\\left( \\frac{\\epsilon}{\\epsilon^2+x^2} \\right)
		 * \\end{eqnarray*}
		 * }
		 */
		VERSION_2
	}
	
	/**
	 * Epsilon value to configure scale to use.
	 */
	private double epsilon; 
	
	/**
	 * Type of approximating function to use.
	 */
	private ApproxVersion version;
	
	/**
	 * Default constructor.
	 * @param eps		Value for approximation scale parameter.
	 * @param v			Function version to be used for approximation.
	 */
	public DeltaApproxHelper(double eps, ApproxVersion v) {
		this.epsilon = eps;
		this.version = v;
	}
	
	/**
	 * Calculate approximated value for given parameter value.
	 * @param z		Value for which to calculate function value.
	 * @return Calculated function value.
	 */
	public double getValue(double z) {
		switch(this.version)
		{
		case VERSION_1:
			return this.dirac_v1(z);
		case VERSION_2:
			return this.dirac_v2(z);
		}
		return 0.0;
	}
	
	/**
	 * Implements version 1 of approximation.
	 * @param z		Value for which to calculate function value.
	 * @return	Calculated function value.
	 */
	private double dirac_v1 (double z) {
		if (Math.abs(z) > this.epsilon) 
			return 0;
		return (1 + Math.cos (Math.PI * z / this.epsilon)) / (2.0 * this.epsilon);
	}
	
	/**
	 * Implements version 2 of approximation.
	 * @param z		Value for which to calculate function value.
	 * @return	Calculated function value.
	 */
	private double dirac_v2 (double z) {
		return this.epsilon / (Math.PI * (this.epsilon * this.epsilon + z * z));
	}
}

