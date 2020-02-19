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

package de.unihalle.informatik.MiToBo.math.distributions.interfaces;


/**
 * Interface for densities p(X) with independent variables in X,
 * that can be evaluated (natural logarithm) for each element in the realization x of its random variable X.
 * p(x) = p_1(x_1) * ... * p_n(x_n)
 * @author Oliver Gress
 *
 * @param <T> Random set
 */
public interface LogIndependentlyEvaluatableDistribution<T> {

	
	/**
	 * Evaluate log(p_i(X)) at x_i 
	 * @param x realization of random variable X
	 * @param i i-th element in x 
	 * @return value of log(p_i(X_i)) at x_i
	 */
	double log_p(T x, int i);
	
}
