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

package de.unihalle.informatik.MiToBo.math.arrays.filter;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;

/**
 * This class implements a Gaussian filter for 1D arrays. 
 * <p>
 * The size of the filter kernel mask is chosen as two times the given
 * standard deviation plus one, truncated to the next full integer
 * value.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class GaussFilterDouble1D extends ArrayFilterDouble1D {

	/**
	 * Standard deviation of the Gaussian kernel to be applied.
	 */
	@Parameter( label= "Standard Deviation \u03A3", required = true, 
			dataIOOrder = 2, direction = Parameter.Direction.IN, 
			description = "Standard deviation \u03A3 of Gaussian kernel.")
	private double sigma = 1.0;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException 
	 * 		Thrown in case of problems during execution.
	 */
	public GaussFilterDouble1D() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Set standard deviation of Gaussian kernel.
	 * @param s	Standard deviation to apply.
	 */
	public void setSigma(double s) {
		this.sigma = s;
	}
	
	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {
		// generate Gaussian kernel
		double[] kernel = this.getGaussKernel();
		int kernelSize = kernel.length;
		int kernelWidth = (kernelSize-1)/2;
		
		// convolve the input array
		this.outputArray = new double[this.inputArray.length];
		double arrayVal = 0;
		for (int x=0; x<this.inputArray.length; ++x) {
			double sum = 0;			
			for (int dx=-kernelWidth; dx<=kernelWidth; ++dx) {
				if (x+dx < 0) {
					if (this.dataIsPeriodic)
						arrayVal = this.inputArray[this.inputArray.length+(x+dx)];
					else
						arrayVal = 0;
				}
				else if (x+dx >= this.inputArray.length) {
					if (this.dataIsPeriodic)
						arrayVal = this.inputArray[0-(this.inputArray.length-x-dx)];
					else
						arrayVal = 0;					
				}
				else
					arrayVal = this.inputArray[x+dx];
				sum += kernel[dx+kernelWidth] * arrayVal;
			}
			this.outputArray[x] = sum;
		}
	}

	/**
	 * Get normalized Gaussian kernel for given parameters.
	 * @return Gaussian kernel.
	 */
	protected double[] getGaussKernel() {

		// calculate kernel size
		int kSize = (int)(this.sigma*2.0) + 1;
		int kAnchor = (kSize-1)/2;

		double[] kernel = new double[kSize];
		double kernelSum = 0;
		for (int x = 0; x < kSize; ++x) {
			kernel[x] = Math.exp(-0.5*(x-kAnchor)*(x-kAnchor)
					/(this.sigma*this.sigma));
			kernelSum += kernel[x];
		}
		for (int x = 0; x < kSize; ++x) {
			kernel[x] /= kernelSum;
		}
		return kernel;
	}

}
