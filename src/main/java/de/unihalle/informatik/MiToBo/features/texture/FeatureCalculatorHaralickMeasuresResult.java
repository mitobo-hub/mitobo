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

package de.unihalle.informatik.MiToBo.features.texture;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.features.FeatureCalculatorResult;

/**
 * Class representing the result of statistical feature calculations.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class FeatureCalculatorHaralickMeasuresResult 
	implements FeatureCalculatorResult 
{
	/**
	 * Result values.
	 */
	@ALDClassParameter(label = "Haralick measures", dataIOOrder = 1)
	private double[] haralickMeasures;
	
	/**
	 * Default constructor.
	 * @param value		Statistical measure calculated.
	 */
	public FeatureCalculatorHaralickMeasuresResult(double value[]) {
		this.haralickMeasures = value;
	}
	
	@Override
  public String getTableEntry(int dim) {
		return new Double(this.haralickMeasures[dim]).toString();
	}
	
	@Override
  public Object getResult() {
		return this.haralickMeasures;
	}

	@Override
  public String getOpIdentifier() {
		return "Haralick";
  }

	@Override
  public boolean isConvertableToNumericalData() {
	  return true;
  }

	@Override
  public double getNumericalValue(int dim) {
	  return this.haralickMeasures[dim];
  }

	@Override
  public int getDimensionality() {
	  return this.haralickMeasures.length;
  }

	@Override
  public String getResultIdentifier(int dim) {
		int id = dim%10;
		switch(id)
		{
		case 0: return "Homogenity";
		case 1: return "Contrast";
		case 2: return "Local_Homogenity";
		case 3: return "Entropy";
		case 4: return "Correlation";
		case 5: return "Auto_Correlation";
		case 6: return "Dissimilarity";
		case 7: return "Cluster_Shade";
		case 8: return "Cluster_Prominence";
		case 9: return "Maximum_Probability";
		}
	  return "Not implemented!";
	}
}
