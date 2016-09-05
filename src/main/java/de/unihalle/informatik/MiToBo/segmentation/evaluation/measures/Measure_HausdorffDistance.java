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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.MTBGroundtruthEvaluationData;

/**
 * The class implements the Hausdorff Distance for contour detection evaluation.
 * 
 * @author knispel, moeller
 */
public class Measure_HausdorffDistance extends EvaluationMeasureContours {

	/**
	 * Default constructor.
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public Measure_HausdorffDistance() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with arguments.
	 * @param segImg				Segmentation result.
	 * @param gtImg					Groundtruth label image.
	 * @param _segContours		Segmented contours.
	 * @param _gtContours		Groundtruth contours.
	 * @param _segRegions		Labels of segmented regions.
	 * @param _gtRegions			Labels of groundtruth regions.
	 * @param mmatrix			Assignment matrix.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public Measure_HausdorffDistance(MTBImageShort segImg, MTBImageShort gtImg, 
			ArrayList<ArrayList<Point>> _segContours,
			ArrayList<ArrayList<Point>> _gtContours, 
			ArrayList<Integer> _segRegions, ArrayList<Integer> _gtRegions,
			byte[][] mmatrix) throws ALDOperatorException {
		this.segLabelImage = segImg;
		this.gtLabelImage = gtImg;
		this.segContours = _segContours;
		this.gtContours = _gtContours;
		this.segRegionLabels = _segRegions;
		this.gtRegionLabels = _gtRegions;
		this.matchingMatrix = mmatrix;
	}
	
	@Override
	@SuppressWarnings("unused")
	protected void doEvaluation() 
			throws ALDOperatorException, ALDProcessingDAGException {
		this.evalData = this.HAU();
		this.resultImg = this.showContours(this.gtLabelImage, this.segContours);
	}
	
	/**
	 * This method is the implementation of the Hausdorff Distance
	 * @return Evaluation data object.
	 */
	private MTBGroundtruthEvaluationData HAU () {
		HashMap<Integer, Double> mapHausdorff = new HashMap<Integer,Double>();
		for (int j = 0; j < this.gtRegionLabels.size(); j++) {
			// search for corresponding segmented region
			for (int i = 0; i < this.segRegionLabels.size(); i++) {
				if (this.matchingMatrix[i][j] == 1) {
					mapHausdorff.put(this.gtRegionLabels.get(j), 
							new Double(Math.max(
								h(this.segContours.get(i), this.gtContours.get(j)), 
									h(this.gtContours.get(j),	this.segContours.get(i)))));
					// leave the loop
					i = this.segRegionLabels.size();
				}
			}
		}
		HashMap< String, HashMap<Integer, Double> > resultMap =
				new HashMap<String, HashMap<Integer,Double>>();
		resultMap.put("Hausdorff", mapHausdorff);
		MTBGroundtruthEvaluationData resultData = 
				new MTBGroundtruthEvaluationData(resultMap);
		return resultData;
	}
	
	/**
	 * This methods computes for all points in a contour the maximum minimal 
	 * distance to an other contour.
	 * 
	 * PRE:
	 * > requires two lists of points of two contours
	 * 
	 * POST:
	 * > outputs the maximum minimal distance from a contour (list1) to an other 
	 * 		contour (list2)
	 * 
	 * @param list1	Point list of first contour.
	 * @param list2	Point list of second contour.
	 * @return	Hausdorff distance.
	 */
	protected double h (ArrayList<Point> list1, ArrayList<Point> list2) {
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (Point point1: list1) {
			for (Point point2: list2) {
				min = Math.min(min, Math.sqrt(
															Math.pow(point1.x - point2.x, 2) 
														+ Math.pow(point1.y - point2.y, 2)));
			}
			max = Math.max(max, min);
			min = Double.MAX_VALUE;
		}
		return max;
	}
}
