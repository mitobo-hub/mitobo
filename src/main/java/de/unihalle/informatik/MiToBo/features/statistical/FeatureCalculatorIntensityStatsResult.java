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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.features.FeatureCalculatorResult;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics.StatValue;

/**
 * Class representing the result of statistical feature calculations.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class FeatureCalculatorIntensityStatsResult 
	implements FeatureCalculatorResult 
{
	/**
	 * Result value, i.e. calculated statistical measure.
	 */
	@ALDClassParameter(label = "Value", dataIOOrder = 2)
	private double statValue;
	
	/**
	 * Statistical measure to be calculated.
	 */
	@ALDClassParameter(label = "Statistical indicator", dataIOOrder = 1)
	private StatValue statMeasure;
	
	/**
	 * Default constructor.
	 * @param value		Statistical measure calculated.
	 */
	public FeatureCalculatorIntensityStatsResult(StatValue mod, double value) {
		this.statValue = value;
		this.statMeasure = mod;
	}
	
	@Override
  public String getTableEntry(int dim) {
		return new Double(this.statValue).toString();
	}
	
	@Override
  public Object getResult() {
		return new Double(this.statValue);
	}
	
	@Override
  public String getOpIdentifier() {
		return "IntensityStats";
  }

	@Override
  public boolean isConvertableToNumericalData() {
	  return true;
  }

	@Override
  public double getNumericalValue(int dim) {
	  return this.statValue;
  }

	@Override
  public int getDimensionality() {
	  return 1;
  }

	@Override
  public String getResultIdentifier(int dim) {
		switch(this.statMeasure)
		{
		case INTENSITY_MIN:
			return "Min";
		case INTENSITY_MAX:
			return "Max";
		case INTENSITY_MEAN:
			return "Mean";
		case INTENSITY_ENTROPY:
			return "Entropy";
		case INTENSITY_VARIANCE:
			return "Variance";
		case INTENSITY_STDDEV:
			return "Std. Dev.";
		case HISTO_UNIFORMITY:
			return "Histo Uniformity";
		case HISTO_SKEWNESS:
			return "Histo Skewness";
		}
		return "";
  }
}
