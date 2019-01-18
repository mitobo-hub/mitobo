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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.features.FeatureCalculatorResult;

/**
 * Class representing the LBP features.
 * <p>
 * In detail results of 
 * <ul>
 * <li> {@link FeatureCalculatorLBPOriginal}
 * <li> {@link FeatureCalculatorLBP}
 * </ul>
 * are represented by objects of this class.
 * 
 * @author Alexander Weiss
 * @author Birgit Moeller
 */
@ALDParametrizedClass
public class FeatureCalculatorLBPResult 
	implements FeatureCalculatorResult 
{
	/**
	 * Result values.
	 */
	@ALDClassParameter(label = "LBP Code Histogram", dataIOOrder = 1)
	private double[] localBinaryPatternHist;
	
	/**
	 * Default constructor.
	 * @param features		Textual measure calculated.
	 */
	public FeatureCalculatorLBPResult(double[] features) {
		this.localBinaryPatternHist = features;
	}
	
	@Override
  public String getTableEntry(int dim) {
		return new Double(this.localBinaryPatternHist[dim]).toString();
	}
	
	@Override
  public Object getResult() {
		return this.localBinaryPatternHist;
	}

	@Override
  public String getOpIdentifier() {
		return "LocalBinaryPattern";
  }

	@Override
  public boolean isConvertableToNumericalData() {
	  return true;
  }

	@Override
  public double getNumericalValue(int dim) {
		return this.localBinaryPatternHist[dim];
  }

	@Override
  public int getDimensionality() {
		return this.localBinaryPatternHist.length;
  }

	@Override
  public String getResultIdentifier(int dim) {
		return new String(dim + ".LBP_Bin");
	}
}
