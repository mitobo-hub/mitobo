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

package de.unihalle.informatik.MiToBo.features.statistical;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics.StatValue;

/**
 * Calculates statistical features on images tile-wise, 
 * based on {@link FeatureCalculator}.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD, allowBatchMode=false,
	shortDescription="Calculates statistical features tile-wise on images.")
@ALDDerivedClass
public class FeatureCalculatorIntensityStats extends FeatureCalculator
{
	
	/**
	 * Statistical measure to be calculated.
	 */
	@Parameter( label= "Statistical measure", required = true,
			direction = Parameter.Direction.IN, dataIOOrder=1,  
			mode=ExpertMode.STANDARD, description = "Desired statistical measure.")
	private StatValue statMeasure = StatValue.INTENSITY_MEAN;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorIntensityStats() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Specify feature measure to extract.
	 * @param s	Measure to apply.
	 */
	public void setMeasure(StatValue s) {
		this.statMeasure = s;
	}

	@Override
	protected FeatureCalculatorIntensityStatsResult getResultDataObjectInvalid(
			int dim) {
		return new FeatureCalculatorIntensityStatsResult(StatValue.INTENSITY_MEAN, 
				Double.NaN);
	}

	@Override
  public void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		ImageStatistics statOp= new ImageStatistics(this.inImg, this.statMeasure);
		statOp.runOp(HidingMode.VISIBLE);
		double resultVal = statOp.getResultValue();
		this.resultObj = 
				new FeatureCalculatorIntensityStatsResult(this.statMeasure, resultVal);
  }
	
	@Override
	public String getDocumentation() {
		return "This operator calculates statistical indicators on the given image.\r\n" + 
				" \r\n" + 
				"<ul>\r\n" + 
				"<li><p><b>input:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Input image</i>: the (gray-scale) image to analyze</p></li>\r\n" + 
				"<li>\r\n" + 
				"<p><i>Statistical measure</i>: the indicator to calculate, available are\r\n" + 
				"<ul>\r\n" + 
				"<li> INTENSITY_MEAN: average intensity in the image\r\n" + 
				"<li> INTENSITY_MAX: maximal intensity in the image\r\n" + 
				"<li> INTENSITY_MIN: minimal intensity in the image\r\n" + 
				"<li> INTENSITY_VARIANCE: intensity variance in the image\r\n" + 
				"<li> INTENSITY_STDDEV: standard deviation of the image intensity\r\n" + 
				"<li> INTENSITY_ENTROPY: entropy of image intensities\r\n" + 
				"<li> HISTO_UNIFORMITY: uniformity of intensity\r\n" + 
				"<li> HISTO_SKEWNESS: skewness of intensity distribution, third moment\r\n" + 
				"</ul>\r\n" + 
				"</li>\r\n" + 
				"</ul>\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"<li><p><b>output:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Value</i>: calculated value of selected indicator</p></li>\r\n" + 
				"<li><p><i>Statistical indicator</i>: name of indicator</p></li>\r\n" + 
				"</ul>\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"</ul>\r\n";
	}

}
