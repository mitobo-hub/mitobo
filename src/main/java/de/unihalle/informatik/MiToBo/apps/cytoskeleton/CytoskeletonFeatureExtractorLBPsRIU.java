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
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.features.texture.lbp.FeatureCalculatorLBPRIU;
import de.unihalle.informatik.MiToBo.features.texture.lbp.FeatureCalculatorLBPJFeatureLib.FeatureType;

/**
 * Operator for extracting LBP RIU features for the {@link ActinAnalyzer2D}.
 * <p>
 * The operator calculates rotation invariant uniform LBP features in a 
 * multi-resolution fashion, extracting a concatenation of LBP code histograms 
 * for different radii R and numbers of neighborhood pixels P for each tile:
 * <ul>
 * <li> R = 1.0, P = 8
 * <li> R = 1.5, P = 12
 * <li> R = 2.0, P = 16
 * <li> R = 3.0, P = 24
 * </ul>
 * 
 * @see FeatureCalculatorLBPRIU 
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING,
	level=Level.STANDARD, allowBatchMode=false)
@ALDDerivedClass
public class CytoskeletonFeatureExtractorLBPsRIU 
	extends CytoskeletonFeatureExtractorTiles {

	/**
	 * Number of histogram bins.
	 */
	@Parameter(label = "Number of Histograms Bins", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Number of Histogram Bins.", dataIOOrder = 5)
	private int histBins = 8;
	
	/**
	 * Scale factor for radius.
	 * <p>
	 * By default the operator extracts four histograms of LBPs for 
	 * radii of 1, 1.5, 2 and 3 with pixel counts of 8, 12, 16 and 24,
	 * respectively. By changing the scale parameter these radii can
	 * be adapted. E.g., setting the scale to 2.0 results in LBP codes
	 * for radii of 2, 3, 4 and 6. 
	 */
	@Parameter(label = "Radius Scale", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Scale for default radius values.", dataIOOrder = 6)
	private double radiusScale = 1.0;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure. 
	 */
	public CytoskeletonFeatureExtractorLBPsRIU() 
			throws ALDOperatorException {
		this.operatorID = "[CytoskeletonFeatureExtractorLBPs]";
	}
	
	/**
	 * Specify number of histogram bins.
	 * @param n	Number of bins to use.
	 */
	public void setHistBins(int n) {
		this.histBins = n;
	}
	
	@Override
	protected Vector<FeatureCalculator> getFeatureOps()
		throws ALDOperatorException {
		
		// initialize the feature calculators
		Vector<FeatureCalculator> featureOps = 
				new Vector<FeatureCalculator>();
		FeatureCalculatorLBPRIU fOp; 
		
		// R = 1.0, P = 8
		fOp = new FeatureCalculatorLBPRIU();
		fOp.setFeatureType(FeatureType.IMAGE_HISTO);
		fOp.setHistBins(8);
		fOp.setRadius(1.0 * this.radiusScale);
		fOp.setNumberNeighbors(8);
		featureOps.add(fOp);

		// R = 1.5, P = 12
		fOp =	new FeatureCalculatorLBPRIU();
		fOp.setFeatureType(FeatureType.IMAGE_HISTO);
		fOp.setHistBins(8);
		fOp.setRadius(1.5 * this.radiusScale);
		fOp.setNumberNeighbors(12);
		featureOps.add(fOp);

		// R = 2.0, P = 16
		fOp =	new FeatureCalculatorLBPRIU();
		fOp.setFeatureType(FeatureType.IMAGE_HISTO);
		fOp.setHistBins(8);
		fOp.setRadius(2.0 * this.radiusScale);
		fOp.setNumberNeighbors(16);
		featureOps.add(fOp);

		// R = 3.0, P = 24
		fOp = new FeatureCalculatorLBPRIU();
		fOp.setFeatureType(FeatureType.IMAGE_HISTO);
		fOp.setHistBins(8);
		fOp.setRadius(3.0 * this.radiusScale);
		fOp.setNumberNeighbors(24);
		featureOps.add(fOp);
		
		return featureOps;
	}
}

/*BEGIN_MITOBO_ONLINE_HELP

This operator calculates a set of texture features from the images,
i.e., local binary pattern code histograms. By default 4 histograms are
calculated for each tile which are then concatenated to form the final
feature vector. In detail, histograms for radii of 1.0, 1.5, 2.0 and 3.0 
are extracted with numbers of neighborhood pixels of 8, 12, 16 and 24,
respectively.
<ul>
<li><p><b>input:</b>
<ul>
<li><p><i>Image directory</i>:<br> directory where the images are read 
  from, all image files are considered;<br> please refer to the webpage 
  for further information on how the file names should be 
  formatted </p></li>
<li><p><i>Mask directory</i>:<br> directory where the segmentation 
	information for the images is read from; the directory can be identical 
	to the image directory</p></li>
<li><p><i>Mask format</i>:<br> expected format of the segmentation data 
  files
	<ul>
	<li>LABEL_IMAGE:<br> a gray-scale image is expected where the area of 
	  each cell is marked with a single unique gray-scale value;<br>
		the files should share the names of the input image files and have 
		the	ending "-mask.tif"
	<li>IJ_ROIS:<br> an ImageJ 1.x file of ROI manager regions is 
		expected;<br> the files should share the names of the input image 
		files and have the ending "-mask.zip" or "-mask.roi"
	</ul>
<li><p><i>Output and working directory</i>:<br> directory for 
  intermediate and final results
</ul>
</ul>

<p>
For more details about the operator and the corresponding 
ActinAnalyzer2D refer to its webpage: 
<a href="http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D">
http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D</a>.

END_MITOBO_ONLINE_HELP*/
