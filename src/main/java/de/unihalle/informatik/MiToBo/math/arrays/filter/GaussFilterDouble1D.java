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
 * <p>
 * If a kernel array is provided, that one is used. Otherwise 
 * a kernel is automatically created from the given standard deviation.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.STANDARD)
public class GaussFilterDouble1D extends ArrayFilterDouble1D {

	/**
	 * Standard deviation of the Gaussian kernel to be applied.
	 */
	@Parameter( label= "Standard Deviation \u03A3", required = true, 
			dataIOOrder = 2, direction = Parameter.Direction.IN, 
			description = "Standard deviation \u03A3 of Gaussian kernel.")
	private double sigma = 1.0;

	/**
	 * Kernel array, either provided externally or filled internally based on
	 * given standard deviation. 
	 */
	private double[] kernel = null;
	
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
		this.kernel = null;
	}
	
	/**
	 * Set Gaussian kernel.
	 * <p>
	 * If provided, sigma is ignored. Note that no checks are included,
	 * thus, the user has to take care of correct values and scaling.
	 * <p>
	 * This function is especially useful if the same kernel has to be
	 * applied many, many times to different data. Then it needs to be
	 * generated only once and can be reused.
	 * 
	 * @param k	Gaussian kernel array to be used.
	 */
	public void setKernel(double[] k) {
		this.kernel = k;
	}

	/**
	 * Get the kernel which is currently in use.
	 * @return	Active kernel.
	 */
	public double[] getKernel() {
		return this.kernel;
	}
	
	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {
		
		// generate Gaussian kernel if none given
		if (this.kernel == null)
			this.kernel = this.getGaussKernel();
		int kernelSize = this.kernel.length;
		int kernelWidth = (kernelSize-1)/2;
		
		// convolve the input array
		this.outputArray = new double[this.inputArray.length];
		
		double arrayVal = 0, sum = 0;
		for (int x=0; x<this.inputArray.length; ++x) {
			sum = 0;			
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
				sum += this.kernel[dx+kernelWidth] * arrayVal;
			}
			this.outputArray[x] = sum;
		}

	}

	/**
	 * Get normalized Gaussian kernel for given parameters.
	 * @return Gaussian kernel.
	 */
	protected double[] getGaussKernel() {
		return GaussFilterDouble1D.getGaussKernel(this.sigma);
	}

	/**
	 * Get normalized Gaussian kernel for given parameters.
	 * @return Gaussian kernel.
	 */
	public static double[] getGaussKernel(double sigma) {

		// calculate kernel size
		int kSize = (int)(sigma*2.0) + 1;
		int kAnchor = (kSize-1)/2;

		double[] kernel = new double[kSize];
		double kernelSum = 0;
		for (int x = 0; x < kSize; ++x) {
			kernel[x] = Math.exp(-0.5*(x-kAnchor)*(x-kAnchor)
					/(sigma*sigma));
			kernelSum += kernel[x];
		}
		for (int x = 0; x < kSize; ++x) {
			kernel[x] /= kernelSum;
		}
		return kernel;
	}

}
