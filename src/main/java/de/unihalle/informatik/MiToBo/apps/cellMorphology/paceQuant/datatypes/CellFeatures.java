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

package de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.datatypes;

import java.util.HashMap;

/**
 * Stores general properties of cells of different types.
 * @author Benjamin Schwede
 */
public class CellFeatures {
	public static int MAJOR_AXIS_C1_X = 0;
	public static int MAJOR_AXIS_C1_Y = 1;
	public static int MAJOR_AXIS_C2_X = 2;
	public static int MAJOR_AXIS_C2_Y = 3;
	public static int MAJOR_AXIS_LENGTH = 4;
	public static int MAJOR_AXIS_ANGLE = 5;
	public static int MINOR_AXIS_C1_X = 6;
	public static int MINOR_AXIS_C1_Y = 7;
	public static int MINOR_AXIS_C2_X = 8;
	public static int MINOR_AXIS_C2_Y = 9;
	public static int MINOR_AXIS_LENGTH = 10;
	public static int MINOR_AXIS_ANGLE = 11;
	public static int COMPACTNESS = 12;
	public static int ELONGATION = 13;
	public static int ECCENTICITY = 14;
	public static int ROUNDNESS = 15;
	public static int SPHERECITY = 16;
	public static int ASPECT_RATIO = 17;
	public static int CURL = 18;
	public static int SOLIDITY = 19;
	public static int CONVEXITY = 20;
	public static int RECTANGULARITY = 21;
	public static int CENTROID_X = 22;
	public static int CENTROID_Y = 23;
	
	public String[] featureNames = {
			"major axis c1 (x)",
			"major axis c1 (y)",
			"major axis c2 (x)",
			"major axis c2 (y)",
			"major axis length",
			"major axis angle",
			"minor axis c1 (x)",
			"minor axis c1 (y)",
			"minor axis c2 (x)",
			"minor axis c2 (y)",
			"minor axis length",
			"minor axis angle",
			"compactness",
			"elongation",
			"eccenticity",
			"roundness",
			"spherecity",
			"aspect ratio",
			"curl",
			"solidity",
			"convexity",
			"rectangularity",
			"centroid (x)",
			"centroid (y)"
	};
	
	HashMap<Integer, Double> features = new HashMap<>();
	
	public CellFeatures()
	{
		for(int n = 0; n < featureNames.length; n++)
		{
			features.put(n,  Double.NaN);
		}
	}
	
	public void addFeature(int feature, double value)
	{
		features.put(feature, value);
	}
	
	public double getFeature(int feature)
	{
		return features.get(feature);
	}
	
	public String getFeatureName(int feature)
	{
		return featureNames[feature];
	}
	
	public String printFeatureNames()
	{
		String result = "";
		for(int n = 0; n < featureNames.length; n++)
		{
			result = result + "\t" + featureNames[n];
		}
		return result;
	}

	@Override
	public String toString()
	{
		String result = "";
		for(int n = 0; n < featureNames.length; n++)
		{
			result = result + "\t" + /*featureNames[n] + ": " + */features.get(n);
		}
		return result;
	}
}
