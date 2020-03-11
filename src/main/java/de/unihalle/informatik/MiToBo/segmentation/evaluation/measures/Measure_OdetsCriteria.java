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

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Point;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.MTBGroundtruthEvaluationData;

/**
 * This class implements Odet's contour detection evaluation criteria for
 * oversegmentation (OCO) and undersegmentation (OCU).
 * 
 * The parameter n is a scale factor.
 * 
 * @author knispel, moeller
 */
public class Measure_OdetsCriteria extends EvaluationMeasureContours {

	/**
	 * Exponent for calculations.
	 */
	@Parameter(label= "Parameter n", required = true, dataIOOrder = 10, 
		direction = Parameter.Direction.IN, description = "Exponential Parameter n.")
	protected int n = 1;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public Measure_OdetsCriteria() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with arguments.
	 * @param segImg				Segmentation result.
	 * @param gtImg					Groundtruth label image.
	 * @param _segContours	Segmented contours.
	 * @param _gtContours		Groundtruth contours.
	 * @param _segRegions		Segmented regions (pixel list).
	 * @param _gtRegions		Groundtruth regions (pixel list).
	 * @param mmatrix				Assignment matrix.
	 * @param nValue				Parameter n.
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public Measure_OdetsCriteria(MTBImageShort segImg, MTBImageShort gtImg, 
			ArrayList<ArrayList<Point>> _segContours,
			ArrayList<ArrayList<Point>> _gtContours, 
			ArrayList<Integer> _segRegions, ArrayList<Integer> _gtRegions,
			byte[][] mmatrix, int nValue) throws ALDOperatorException {
		this.segLabelImage = segImg;
		this.gtLabelImage = gtImg;
		this.segContours = _segContours;
		this.gtContours = _gtContours;
		this.segRegionLabels = _segRegions;
		this.gtRegionLabels = _gtRegions;
		this.matchingMatrix = mmatrix;
		this.n = nValue;
	}
	
	/**
	 * This method does the actual work.
	 */
	@SuppressWarnings("unused")
	@Override
	protected void doEvaluation() throws ALDOperatorException {
		this.evalData = this.ComputeOdetsCriteria();
		this.resultImg = this.showContours(this.gtLabelImage, this.segContours);
	}
	
	/**
	 * This method is the implementation of Odet's criteria.
	 * @return Evaluation data object.
	 */
	private MTBGroundtruthEvaluationData ComputeOdetsCriteria () {
		HashMap<Integer, Double> mapOCO = new HashMap<Integer,Double>();
		HashMap<Integer, Double> mapOCU = new HashMap<Integer,Double>();
		int i;
		for (int j = 0; j < this.gtRegionLabels.size(); j++) {
			for (i = 0; i < this.segRegionLabels.size(); i++) {
				if (this.matchingMatrix[j][i] == 1) {
					mapOCO.put(this.gtRegionLabels.get(j), new Double(this.OCO(j, i)));
					mapOCU.put(this.gtRegionLabels.get(j), new Double(this.OCU(j, i)));
					break;
				}
			}
			// region not matched!
			if (i ==  this.segRegionLabels.size()) {
				mapOCO.put(this.gtRegionLabels.get(j), Double.NaN);
				mapOCU.put(this.gtRegionLabels.get(j), Double.NaN);
			}
		}
		HashMap< String, HashMap<Integer, Double> > resultMap =
				new HashMap<String, HashMap<Integer,Double>>();
		resultMap.put("OCO", mapOCO);
		resultMap.put("OCU", mapOCU);
		MTBGroundtruthEvaluationData resultData = 
				new MTBGroundtruthEvaluationData(resultMap);
		return resultData;
	}

	/**
	 * This method implements Odet's oversegmentation error.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Odet's oversegmentation error.
	 */
	protected double OCO (int gtIndex, int segIndex) {
		ArrayList<Point> oversegPixel = new ArrayList<Point>();
		double sum = 0;
		double max = 5.0;
		double min = Double.MAX_VALUE;

		// detection of all oversegmentated pixels
		for (Point point: this.segContours.get(segIndex)) {
			if (!this.gtContours.get(gtIndex).contains(point))
				oversegPixel.add(point);
		}

		// compute the sum formula of Odet's criterion
		for (Point point1: oversegPixel) {
			for (Point point2: this.gtContours.get(gtIndex)) {
				min = Math.min(min, Math.sqrt(Math.pow(point1.x - point2.x, 2) 
						+ Math.pow(point1.y - point2.y, 2)));
			}
			sum += Math.pow((min / max), this.n);
			min = Double.MAX_VALUE;
		}
		return sum / oversegPixel.size();
	}
	
	/**
	 * This method implements Odet's undersegmentation error.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Odet's undersegmentation error.
	 */
	protected double OCU (int gtIndex, int segIndex) {
		ArrayList<Point> undersegPixel = new ArrayList<Point>();
		double sum = 0;
		double max = 5.0;
		double min = Double.MAX_VALUE;

		// detection of all undersegmentated pixels
		for (Point point: this.gtContours.get(gtIndex)) {
			if (!this.segContours.get(segIndex).contains(point))
				undersegPixel.add(point);
		}

		// compute the sum formula of Odet's criterion
		for (Point point1: undersegPixel) {
			for (Point point2: this.segContours.get(segIndex)) {
				min = Math.min(min, Math.sqrt(Math.pow(point1.x - point2.x, 2)
						+ Math.pow(point1.y - point2.y, 2)));
			}
			sum += Math.pow(min / max, this.n);
			min = Double.MAX_VALUE;
		}
		return sum / undersegPixel.size();
	}
}