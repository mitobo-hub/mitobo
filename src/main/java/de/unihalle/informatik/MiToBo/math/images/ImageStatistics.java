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

package de.unihalle.informatik.MiToBo.math.images;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBImageHistogram;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Class offering statistical calculations on objects of type {@link MTBImage}.
 * <p>
 * Up to three image dimensions, i.e. x, y and z, are considered. The 
 * measures are common statistical measures. Uniformity and skewness are 
 * calculated according to 
 * 
 * "Gonzalez/Woods, Digital Image Processing using Matlab, chap. 11, page 466"
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD)
public class ImageStatistics extends MTBOperator {
	
	@Parameter( label= "Input image", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder=0, 
			mode=ExpertMode.STANDARD, description = "Input image.")
	private MTBImage inputImage = null;

	@Parameter( label= "Value to be calculated.", required = true,
			direction = Parameter.Direction.IN, dataIOOrder=1,  
			mode=ExpertMode.STANDARD, description = "Desired statistical value.")
	private StatValue calcMode = StatValue.INTENSITY_MEAN;

	@Parameter( label= "Result value", direction = Parameter.Direction.OUT,  
			mode=ExpertMode.STANDARD, description = "Result value.")
	private double resultValue;
	
	/**
	 * Statistical values that could be calculated.
	 */
	public static enum StatValue {
		/**
		 * Average image intensity.
		 */
		INTENSITY_MEAN,
		/**
		 * Maximal image intensity.
		 */
		INTENSITY_MAX,
		/**
		 * Minimal image intensity.
		 */
		INTENSITY_MIN,
		/**
		 * Image intensity variance.
		 */
		INTENSITY_VARIANCE,
		/**
		 * Image intensity standard deviation.
		 */
		INTENSITY_STDDEV,
		/**
		 * Image entropy.
		 */
		INTENSITY_ENTROPY,
		/**
		 * Uniformity of intensity.
		 * <p>
		 * Measure is maximal, if histogram is equally distributed.
		 */
		HISTO_UNIFORMITY,
		/**
		 * Histogram skewness as given by third image moment.
		 * <p>
		 * Measure is 0 for symmetric histograms, positive for histograms skewed 
		 * to the right, and negative for histograms skewed to the left.
		 */
		HISTO_SKEWNESS
	}
	
	/**
	 * Default constructor.
	 */
	public ImageStatistics() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with image argument.
	 * @param inimg		Image to process.
	 */
	public ImageStatistics(MTBImage inimg, StatValue mode) 
			throws ALDOperatorException {
		this.inputImage = inimg;
		this.calcMode = mode;
	}

	/**
	 * Get result value;
	 * @return	Calculated result value.
	 */
	public double getResultValue() {
		return this.resultValue;
	}
	
	@Override
	protected void operate() throws ALDOperatorException {

		MTBImageHistogram histo = this.getNormalizedImageHisto();
		switch(this.calcMode)
		{
		case INTENSITY_MIN:
			this.resultValue = this.inputImage.getMinMaxDouble()[0];
			break;
		case INTENSITY_MAX:
			this.resultValue = this.inputImage.getMinMaxDouble()[1];
			break;
		case INTENSITY_MEAN:
			this.resultValue = histo.getMean();
			break;
		case INTENSITY_ENTROPY:
		{
			int minBin = histo.getSmallestNonEmptyBin();
			int maxBin = histo.getLargestNonEmptyBin();
			double sum = 0.0;
			for (int binID = minBin; binID <= maxBin; ++binID) {
				if (histo.getBinValue(binID) > MTBConstants.epsilon)
					sum += 
						histo.getBinValue(binID) * Math.log(histo.getBinValue(binID));
			}
			this.resultValue = -sum;
			break;
		}
		case INTENSITY_VARIANCE:
			this.resultValue = histo.getVariance();
			break;
		case INTENSITY_STDDEV:
			this.resultValue = histo.getStdDev();
			break;
		case HISTO_UNIFORMITY:
		{
			int minBin = histo.getSmallestNonEmptyBin();
			int maxBin = histo.getLargestNonEmptyBin();
			double sum = 0.0;
			for (int binID = minBin; binID <= maxBin; ++binID) {
				sum += histo.getBinValue(binID) * histo.getBinValue(binID);
			}
			this.resultValue = sum;
			break;
		}
		case HISTO_SKEWNESS:
		{
			double mean = histo.getMean();
			int minBin = histo.getSmallestNonEmptyBin();
			int maxBin = histo.getLargestNonEmptyBin();
			double sum = 0.0, diff = 0.0;
			for (int binID = minBin; binID <= maxBin; ++binID) {
				diff = (histo.getBinMidpoint(binID) - mean);
				sum +=  diff * diff * diff * histo.getBinValue(binID);
			}
			this.resultValue = sum;
			break;
		}
		}
	}
	
	/**
	 * Extracts the normalized image histogram with 256 bins.
	 * @return	Image histogram.
	 */
	private MTBImageHistogram getNormalizedImageHisto() {
		MTBImageHistogram histo;
		int[] minmaxInt = this.inputImage.getMinMaxInt();
		double[] minmaxDouble = this.inputImage.getMinMaxDouble();
		histo = 
				new MTBImageHistogram(this.inputImage,	minmaxInt[1]-minmaxInt[0]+1,
						minmaxDouble[0], minmaxDouble[1]);
		histo.normalize();
		return histo;
	}
}
