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

package de.unihalle.informatik.MiToBo.segmentation.evaluation.measures;

import java.util.HashMap;
import java.util.TreeSet;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.*;

/**
 * Base class for evaluation measures comparing a segmentation result 
 * against a groundtruth segmentation.
 * 
 * @author Birgit Moeller
 */
public abstract class EvaluationMeasure extends MTBOperator {

	/**
	 * Label image of segmentation result.
	 */
	@Parameter( label= "Segmentation label image", required = true, 
			direction=Parameter.Direction.IN, dataIOOrder = 1,
      description = "Segmentation result label image.")
	protected transient MTBImageShort segLabelImage = null;

	/**
	 * Groundtruth labeling.
	 */
	@Parameter( label= "Groundtruth label image", required = true, 
			direction=Parameter.Direction.IN, dataIOOrder = 2,
			description = "Groundtruth label image.")
	protected transient MTBImageShort gtLabelImage = null;

	/**
	 * Set of available groundtruth labels.
	 */
	protected TreeSet<Integer> labelsetGT = null;

	/**
	 * Set of available segmentation labels.
	 */
	protected TreeSet<Integer> labelsetSG = null;

	/**
	 * Match list of set entries to original groundtruth labels.
	 */
	protected int [] gtIDs;

	/**
	 * Match list of set entries to original segmentation labels.
	 */
	protected int [] sgIDs;

	/**
	 * Array of sizes of groundtruth regions.
	 */
	protected HashMap<Integer, Integer> sizesGT = null;
	
	/**
	 * Array of sizes of segmented regions.
	 */
	protected HashMap<Integer, Integer> sizesSG	= null;

	/**
	 * Minimum number of regions in groundtruth or segmented region set.
	 */
	protected int minRegionCount = 0;

	/**
	 * Maximum number of regions in groundtruth or segmented region set.
	 */
	protected int maxRegionCount = 0;

	/**
	 * Score matrix with pairwise overlaps.
	 */
	protected double [][] scoreMatrix = null;

	/**
	 * Matching matrix.
	 */
	protected byte [][] matchingMatrix = null;

	/*
	 * some internal helpers
	 */

	/**
	 * Width of label images.
	 */
	protected int width;

	/**
	 * Height of label images.
	 */
	protected int height;

	/**
	 * Result data.
	 */
	@Parameter( label= "Evaluation data", required = true, 
			direction=Parameter.Direction.OUT, 
      description = "Evaluation data.")
	protected MTBGroundtruthEvaluationData evalData = null;

	/**
	 * Constructor.
	 * @param sLabels Image with segmentation labels.
	 * @param gLabels Image with groundtruth labels.
	 * 
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public EvaluationMeasure(MTBImageShort sLabels, MTBImageShort gLabels) 
			throws ALDOperatorException {
		this.segLabelImage = sLabels;
		this.gtLabelImage = gLabels;
	}

	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public EvaluationMeasure() throws ALDOperatorException {
		// nothing to do here
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// initialize some member variables
		this.width = this.segLabelImage.getSizeX();
		this.height = this.segLabelImage.getSizeY();

		// calculate statistics and quality measures
		this.doEvaluation();
	}

	/**
	 * Method to overwrite that does actual calculations.
	 * @throws ALDOperatorException				Thrown in case of failure. 
	 * @throws ALDProcessingDAGException 	Thrown in case of failure.
	 */
	protected abstract void doEvaluation()
			throws ALDOperatorException, ALDProcessingDAGException;
	
	/**
	 * Returns evaluation result data.
	 * @return Result data object.
	 */
	public MTBGroundtruthEvaluationData getResult() {
		return this.evalData;
	}
}
