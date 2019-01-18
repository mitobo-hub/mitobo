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

package de.unihalle.informatik.MiToBo.features.texture.lbp;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;

/**
 * Implements an extended version of local binary patterns (LBPs).
 * <p>
 * The operator is based on the JFeatureLib which implements the LBPs 
 * according to
 * <p> 
 * <i>M. Heikkilae, M. Pietikaeinen, "A texture-based method for modeling 
 * the background and detecting moving objects", IEEE Transactions on 
 * Pattern Analysis and Machine Intelligence, Vol. 28, No. 4, 
 * pp. 657-662, 2006.</i>
 * and
 * <p>
 * <i>Ojala et al, "Multiresolution Gray-Scale and Rotation Invariant
 * Texture Classification with Local Binary Patterns", IEEE Transactions 
 * on Pattern Analysis and Machine Intelligence, Vol. 24, No. 7, 
 * pp. 971-987, 2002.</i>
 * <p>
 * Tha basic idea of the extension is to consider not only direct 
 * neighbors, but rather neighbors which are at a specified distance of 
 * the center pixel. To this end a circular neighborhood around each 
 * pixel is defined, and a specific number of sample pixels on the 
 * border of this neighborhood is considered for code generation. 
 * In addition, the size of the region used for generating the code 
 * histogram for each pixel can be configured freely.<br>
 * Finally, the implementation targets at a larger robustness by 
 * introducing a baseline for the differences between the center and a 
 * pixel along the border which decreases the algorithm's sensitivity 
 * to small intensity changes. 
 * <p>
 * As result the operator either returns a vector of the LBP code 
 * histograms of all pixels in the input image, appended row-by-row from 
 * top-left to bottom-right. Alternatively a single histogram for the
 * complete image is returned which is extracted by setting the 
 * histogram region to the complete image and returning the histogram
 * of the center pixel (with possibly rounded coordinates).
 * <p>
 * For more information refer to the documentation of the JFeatureLib at
 * <a href="https://github.com/locked-fg/JFeatureLib">Github</a>
 * 
 * @author Alexander Weiss
 * @author Birgit Moeller
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.STANDARD, allowBatchMode = false)
@ALDDerivedClass
public class FeatureCalculatorLBP extends FeatureCalculatorLBPJFeatureLib { 	
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorLBP() throws ALDOperatorException {
		// nothing to do here
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.numberNeighbors > 30)
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED,
				"[FeatureCalculatorLBP] cannot handle more than 30 neighbors!");
		if (this.radius <= 0.0)
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED,
				"[FeatureCalculatorLBP] radius must be > 0!");
	}

	/**
	 * Get instance of actual feature calculator.
	 * @return	Feature calculator object.
	 */
	@Override
	protected JFeatureLibLBPAdapter getFeatureOp() {
		JFeatureLibLBPAdapter l = new JFeatureLibLBPAdapter();
		l.setNumberOfHistogramBins(this.histBins);
		l.setNumPoints(this.numberNeighbors);
		l.setRadius(this.radius);
		l.setConstant(this.baselineConstant);
		return l;
	}
}
