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

package de.unihalle.informatik.MiToBo.features;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;


/**
 * Super class for operators calculating features on images.
 * <p>
 * Operators extending this class are supposed to extract features from a 
 * given image. As result they should return an object of type 
 * {@link FeatureCalculatorResult}. 
 * 
 * @author moeller
 */
public abstract class FeatureCalculator extends MTBOperator
{
	/**
	 * Input image to calculate features for.
	 */
	@Parameter(label = "Input image", required = true, 
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "Input image to analyze.", dataIOOrder = -10)
	protected transient MTBImage inImg = null;
	
	/**
	 * Result data object, e.g., an image, a histogram, a vector, ....
	 */
	@Parameter(label = "Result data",	direction = Parameter.Direction.OUT, 
			description = "Result of feature calculation.")
	protected transient FeatureCalculatorResult resultObj = null;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected FeatureCalculator() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Sets the input image to work on.
	 * @param input		Image to work on.
	 */
	public void setInputImage(MTBImage input) {
		this.inImg = input;
	}
	
	/**
	 * Returns result data object.
	 * @return Result data.
	 */
	public FeatureCalculatorResult getResultData() {
		return this.resultObj;
	}
}
