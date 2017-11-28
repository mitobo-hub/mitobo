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

package de.unihalle.informatik.MiToBo.enhance;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Global contrast stretching.
 * <p>
 * This operator performs a linear global contrast stretching on the 
 * intensity histogram of the given image. Since outliers may seriously 
 * affect the stretching, fractions of outliers at the left and right 
 * margins of the histogram can be defined which are then ignored in 
 * determining the minimal and maximal intensity values in the image.
 * <p>
 * The new intensity value of a pixel is calculated by the following 
 * equation:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
 * 	\\begin{equation*}
 *  		I_{new}(x,y) = (I(x,y) - c) \\cdot \\frac{b-a}{d-c} + a
 *  \\end{equation*}}
 * where a and b are the minimal and maximal values of the target 
 * intensity range (e.g., usually 0 and 255 for 8-bit grayscale images), 
 * and c and d are the minimal and maximal intensity values in the given 
 * image. c and d are determined considering the outlier fractions.
 * <p>
 * Note that we always assume positive image intensities here, i.e. a
 * is always 0. In case of negative values being present in the input 
 * image the result is undefined.
 * <p>
 * Target values being larger than b or smaller than a are mapped to 
 * b and a, respectively.
 * <p>
 * More information can be found at 
 * <a href="http://homepages.inf.ed.ac.uk/rbf/HIPR2/stretch.htm#1">
 * Image Processing Learning Resources</a>.
 * 
 * @author Birgit Moeller
 */
@SuppressWarnings("javadoc")
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION)
public class GlobalContrastStretching extends MTBOperator {

	/**
	 * Input grayscale image.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Direction.IN, description = "Input image.")
	private transient MTBImage inImg = null;

	/**
	 * Fraction of outlier pixels at left histogram margin.
	 */
	@Parameter( label= "Expected Fraction of Left Outliers", 
			required = true, direction = Direction.IN, dataIOOrder = 1,
			description = "Lower Outlier Fraction.")
	private double leftOutlierFraction = 0.005;

	/**
	 * Fraction of outlier pixels at right histogram margin.
	 */
	@Parameter( label= "Expected Fraction of Right Outliers", 
			required = true, direction = Direction.IN, dataIOOrder = 2,
			description = "Right Outlier Fraction.")
	private double rightOutlierFraction = 0.005;

	/**
	 * Result image with stretched contrast.
	 */
	@Parameter( label= "Result Image", required = true,
			direction = Direction.OUT, description = "Result image.")
	private transient MTBImage resultImg = null;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public GlobalContrastStretching() throws ALDOperatorException	{
		// nothing to do here
	}
	
	/**
	 * Constructor with default image.
	 * @param img		Image to enhance.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public GlobalContrastStretching(MTBImage img) 
			throws ALDOperatorException {	
		this.inImg = img;	
	}
	
	/**
	 * Returns the result image.
	 * @return	Result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
		
	/**
	 * This method does the actual work. 
	 */
	@Override
	protected void operate() {
		MTBImageHistogram histo = new MTBImageHistogram(this.inImg);
		
		int totalPixCount = this.inImg.getSizeX()*this.inImg.getSizeY();
		
		int leftOutlierCount = 
			(int)(this.leftOutlierFraction * totalPixCount + 0.5);
		int rightOutlierCount = 
			(int)((1.0 - this.rightOutlierFraction) * totalPixCount + 0.5);
		
		int pixCount = 0;
		int c = -1, d = -1;
		int index = 0;
		for (index=0; index<histo.getSize(); ++index) {
			pixCount += histo.getBinValue(index);
			if (pixCount > leftOutlierCount) {
				c = index;
				break;
			}
		}
		for (++index; index<histo.getSize(); ++index) {
			pixCount += histo.getBinValue(index);
			if (pixCount > rightOutlierCount) {
				d = index;
				break;
			}
		}
		
		double minVal = histo.getBinMidpoint(c);
		double maxVal = histo.getBinMidpoint(d);
		double upperLimit = this.inImg.getTypeMax();
		double lowerLimit = 0;
		
		this.resultImg = this.inImg.duplicate();
		for (int y=0; y<this.inImg.getSizeY(); ++y) {
			for (int x=0; x<this.inImg.getSizeX(); ++x) {
				double newVal = (this.inImg.getValueDouble(x, y) - minVal) 
					* (upperLimit - lowerLimit) / (maxVal - minVal) + lowerLimit;
				if (newVal < 0)
					newVal = 0;
				else if (newVal > upperLimit)
					newVal = upperLimit;
				this.resultImg.putValueDouble(x, y, newVal);
			}
		}
	}
	
}
