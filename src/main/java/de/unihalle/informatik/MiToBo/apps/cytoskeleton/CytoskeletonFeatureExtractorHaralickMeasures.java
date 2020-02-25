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

package de.unihalle.informatik.MiToBo.apps.cytoskeleton;

import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures.HaralickDirection;

/**
 * Operator for extracting Haralick texture measures from 
 * co-occurrence matrices as features for the {@link ActinAnalyzer2D}.
 * 
 * @see FeatureCalculatorHaralickMeasures 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING,
	level=Level.STANDARD, allowBatchMode=false,
	shortDescription="Extracts Haralick texture measures from " + 
		"co-occurrence matrices as features for the ActinAnalyzer2D.")
@ALDDerivedClass
public class CytoskeletonFeatureExtractorHaralickMeasures 
	extends CytoskeletonFeatureExtractorTiles {

	/**
	 * Default directions for Haralick features.
	 */
	private static Vector<HaralickDirection> defaultDirections;
	
	// this initialization block is necessary to set default directions
	static {
		defaultDirections = 
			new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
		defaultDirections.add(HaralickDirection.EAST);
		defaultDirections.add(HaralickDirection.NORTH_EAST);
		defaultDirections.add(HaralickDirection.NORTH);
		defaultDirections.add(HaralickDirection.NORTH_WEST);		
	}

	/**
	 * Distance.
	 */
	@Parameter( label= "Haralick distance", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=1,  
		mode=ExpertMode.STANDARD, description = "Desired distance.")
	protected int distance= 4;

	/**
	 * Directions for calculating cooccurrence matrices.
	 */
	@Parameter(label = "Haralick directions", required = true, 
		dataIOOrder = 2, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, description = "Directions for matrices.")
	protected Vector<HaralickDirection> directions = defaultDirections;

	/**
	 * Flag for isotropic calculations.
	 * <p>
	 * If flag is true the average of each feature for the four directions 
	 * EAST, NORTH_EAST, NORTH and NORTH_WEST is taken as result. 
	 * But, note that this causes the computation time to be increased by 
	 * a factor of four as well.
	 */
	@Parameter( label= "Isotropic calculations", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=3,  
		description = "Flag to enable isotropic calculations.",
		mode=ExpertMode.ADVANCED)
	protected boolean isotropicCalcs = false;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure. 
	 */
	public CytoskeletonFeatureExtractorHaralickMeasures() 
			throws ALDOperatorException {
		this.operatorID = "[CytoskeletonFeatureExtractorHaralickMeasures]";
	}
	
	/**
	 * Specify distance for calculating cooccurrence matrices.
	 * @param d		Pixel distance to apply.
	 */
	public void setDistance(int d) {
		this.distance = d;
	}
	
	/**
	 * Set directions for which to calculate cooccurence matrices.
	 * @param dirs	List of directions.
	 */
	public void setHaralickDirections(Vector<HaralickDirection> dirs) {
		this.directions = dirs;
	}

	/**
	 * Enable/disable isotropic calculations.
	 * @param flag	If true, isotropic calculations are enabled.
	 */
	public void setFlagIsotropicCalculations(boolean flag) {
		this.isotropicCalcs = flag;
	}
	
	@Override
	protected Vector<FeatureCalculator> getFeatureOps() 
			throws ALDOperatorException {
		
		// check if vector of directions is null or empty, 
		// if so init with default
		if (this.directions == null || this.directions.isEmpty()) {
			this.directions = new 
				Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
			this.directions.add(HaralickDirection.EAST);
		}
		
		// initialize the feature calculator
		Vector<FeatureCalculator> featureOps = 
				new Vector<FeatureCalculator>();
		FeatureCalculatorHaralickMeasures fOp = 
				new FeatureCalculatorHaralickMeasures();
		fOp.doIsotropicCalcutations(this.isotropicCalcs);
		fOp.setDistance(this.distance);
		fOp.setDirections(this.directions);
		fOp.setFlagThrinkMatrix(true);
		featureOps.add(fOp);
		
		return featureOps;
	}
	
	@Override
	public String getDocumentation() {
		return "This operator calculates a set of texture features from the images,\n" + 
				"particularly Haralick texture measures.\n" + 
				"<ul>\n" + 
				"<li><p><b>input:</b>\n" + 
				"<ul>\n" + 
				"<li><p><i>Image directory</i>:<br> directory where the images are read \n" + 
				"  from, all image files are considered;<br> please refer to the webpage \n" + 
				"  for further information on how the file names should be \n" + 
				"  formatted </p></li>\n" + 
				"<li><p><i>Mask directory</i>:<br> directory where the segmentation \n" + 
				"	information for the images is read from; the directory can be identical \n" + 
				"	to the image directory</p></li>\n" + 
				"<li><p><i>Mask format</i>:<br> expected format of the segmentation data \n" + 
				"  files\n" + 
				"	<ul>\n" + 
				"	<li>LABEL_IMAGE:<br> a gray-scale image is expected where the area of \n" + 
				"	  each cell is marked with a single unique gray-scale value;<br>\n" + 
				"		the files should share the names of the input image files and have \n" + 
				"		the	ending \"-mask.tif\"\n" + 
				"	<li>IJ_ROIS:<br> an ImageJ 1.x file of ROI manager regions is \n" + 
				"		expected;<br> the files should share the names of the input image \n" + 
				"		files and have the ending \"-mask.zip\" or \"-mask.roi\"\n" + 
				"	</ul>\n" + 
				"<li><p><i>Output and working directory</i>:<br> directory for \n" + 
				"  intermediate and final results\n" + 
				"</ul>\n" + 
				"</ul>\n" + 
				"\n" + 
				"<p>\n" + 
				"For more details about the operator and the corresponding \n" + 
				"ActinAnalyzer2D refer to its webpage: \n" + 
				"<a href=\"http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D\">\n" + 
				"http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D</a>.\n"; 
	}
}

