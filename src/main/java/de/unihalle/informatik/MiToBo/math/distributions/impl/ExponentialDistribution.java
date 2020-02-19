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
package de.unihalle.informatik.MiToBo.math.distributions.impl;

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IntegrableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;

/**
 * Exponential distribution.
 * 
 * @author Oliver Gress
 *
 */
public class ExponentialDistribution implements IntegrableDistribution<Double>, FirstOrderMoment<Double>,
												SecondOrderCentralMoment<Double> {

	protected double lambda;
	
	/**
	 * Constructor for exponential distribution lambda*exp(-lambda*x) if x >= 0.
	 * @param lambda
	 */
	public ExponentialDistribution(double lambda) {
		this.lambda = lambda;
	}
	
	@Override
	public double P(Double x) {
		
		if (x <= 0.0)
			return 0;
		else
			return - Math.expm1(-this.lambda * x);
	}

	@Override
	public Double getMean() {
		return 1.0/this.lambda;
	}

	@Override
	public Double getCovariance() {
		return 1.0/(this.lambda*this.lambda);
	}
	
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}
	
	public double getLambda() {
		return this.lambda;
	}

}
