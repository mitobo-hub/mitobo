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
 * Interface for distributions that can be evaluated for each realization x of its random variable X. Evaluation means P(X=x)
 * @author Oliver Gress
 *
 * @param <T> type of the random variable
 */
public interface EvaluatableDistribution<T> {

	/**
	 * Evaluate p(X) at location x. P(X=x)
	 * @param x realization of random variable X
	 * @return value of p(X) at x
	 */
	double p(T x);
	
}
