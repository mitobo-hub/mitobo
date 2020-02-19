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
 * Interface for densities with independent variables, where components of a sample can be drawn independently.
 * Don't forget to give your implementation the possibility to specify a Random generator
 * if you want to reproduce results. The drawSample method lacks this possibility for time efficiency
 * reasons. 
 * @author Oliver Gress
 *
 * @param <T> type of the sample / random variable
 */
public interface IndependentSamplingDistribution<T> {

	/**
	 * Generate a new sample from this density by drawing only one independent variable for a given realization x. 
	 * This method should create a new object.
	 * @param i sample a new realization of the i-th element in x
	 * @param x realization of a random vector or finite set
	 * @return new sample object
	 */
	T drawSample(int i, T x);
}
