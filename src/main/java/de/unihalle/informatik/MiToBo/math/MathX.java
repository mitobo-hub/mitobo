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

package de.unihalle.informatik.MiToBo.math;

/**
 * Math class with functions that are not provided by Java's Math class.
 * 
 * @author Oliver Gress
 */
public class MathX {
	
	/**
	 * Natural logarithm of the sum of two values P1 and P2 when only their natural logarithms log(P1) and log(P2) are given. 
	 * @param logP1 natural logarithm of P1: log(P1)
	 * @param logP2 natural logarithm of P2: log(P2)
	 * @return log( P1 + P2 )
	 */
	public static double logSumP(double logP1, double logP2) {
		if (logP1 == 0.0)
			return logP2;
		if (logP2 == 0.0)
			return logP1;
		if (logP1 < logP2)
			return logP2 + Math.log1p(Math.exp(logP1 - logP2));
		return logP1 + Math.log1p(Math.exp(logP2 - logP1));
	}
	
  /**
   * Calculates the factorial of the given natural number.
   * <p>
   * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
	 *      \\begin{equation*} 
	 *         n! = \\prod_{i=1}^n i
	 *      \\end{equation*}}
	 *      
   * @param n		Number to calculate the factorial for, must be positive.
   * @return	Factorial of n.
   */
  public static int factorial(int n) {
  	if (n == 0)
  		return 1;
  	int fac = 1;
  	for (int i=1;i<=n;++i)
  		fac *= i;
  	return fac;
  }

	/**
	 * Binomial coefficient of n over k
	 * @param n
	 * @param k
	 * @return
	 */
	public static double binomial(int n, int k) {
		if (k < 0 || n < 0 || n < k)
			throw new IllegalArgumentException("MathX.binomial(int n, int k): Following properties must be satisfied: n >= 0, k >= 0, k <= n.");
		
		if (k == 0)
			return 1;
		else if (k == 1)
			return n;
		else if (k == n)
			return 1;
		else {
			int k_ = k;
			
			if (k_ > n - k)
				k_ = n - k;
			
			double b1 = 1.0;
			double b2 = 1.0;
			for (int i = 0; i < k_; i++) {
				b1 *= (n-i);
				b2 *= (i+1);
			}
			
			return b1/b2;
		}	
	}
	
}
