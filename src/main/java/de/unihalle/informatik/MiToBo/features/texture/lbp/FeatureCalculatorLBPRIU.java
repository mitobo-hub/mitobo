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
 * Implements rotation invariant uniform local binary patterns (LBP-RIU).
 * <p>
 * The operator is based on the JFeatureLib implementation of LBPs 
 * and MiToBo extensions for supporting rotation invariance and 
 * uniform pattern. The theory can be found in
 * <p> 
 * <i>Ojala et al, Multiresolution Gray-Scale and Rotation Invariant
 * Texture Classification with Local Binary Patterns, PAMI, vol. 24,
 * no. 7, pp. 971-987, July 2002.</i>
 * <p>
 * For more information refer to the documentation of the JFeatureLib:
 * <a href="https://github.com/locked-fg/JFeatureLib">
 * JFeatureLib at Github</a>
 * 
 * @see JFeatureLibLBPAdapterRIU
 * 
 * @author Birgit Moeller
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.SWING, 
	level = Level.STANDARD, allowBatchMode = false)
@ALDDerivedClass
public class FeatureCalculatorLBPRIU extends FeatureCalculatorLBPJFeatureLib { 	
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorLBPRIU() throws ALDOperatorException {
		// nothing to do here
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (   this.numberNeighbors != 8
				&& this.numberNeighbors != 12
				&& this.numberNeighbors != 16
				&& this.numberNeighbors != 24)
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED, 
					"[FeatureCalculatorLBPRIU] " 
							+ "only 8, 12, 16 or 24 neighbors are allowed!");
	}
	
	@Override
	protected JFeatureLibLBPAdapter getFeatureOp() {
		JFeatureLibLBPAdapterRIU fOp = 
				new JFeatureLibLBPAdapterRIU(this.numberNeighbors);

		// for rotation invariant LBP codes the number of histogram bins
		// should be equal to the number of possible codes, i.e., no
		// further quantization is required
		fOp.setNumberOfHistogramBins(this.numberNeighbors+2);
		
		fOp.setNumPoints(this.numberNeighbors);
		fOp.setRadius(this.radius);
		fOp.setConstant(this.baselineConstant);
		return fOp;
	}
}
