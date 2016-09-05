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

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.*;

/**
 * This class implements Pratt's figure of merit for contour detection evaluation.
 * 
 * @author knispel, moeller
 */
public class Measure_PrattsFigureOfMerit extends EvaluationMeasureContours {

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public Measure_PrattsFigureOfMerit() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with arguments.
	 * @param segImg				Segmentation result.
	 * @param gtImg					Groundtruth label image.
	 * @param _segContours		Segmented contours.
	 * @param _gtContours		Groundtruth contours.
	 * @param _segRegions		Segmented regions (pixel list).
	 * @param _gtRegions			Groundtruth regions (pixel list).
	 * @param mmatrix				Assignment matrix.
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public Measure_PrattsFigureOfMerit(MTBImageShort segImg, MTBImageShort gtImg, 
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
	
	@SuppressWarnings("unused")
	@Override
	protected void doEvaluation() throws ALDOperatorException {
		this.evalData = this.PRA();
		this.resultImg = this.showContours(this.gtLabelImage, this.segContours);
	}
	
	/**
	 * This method is the implementation of Pratt's figure of merit.
	 * @return Evaluation data object.
	 */
	protected MTBGroundtruthEvaluationData PRA () {
		HashMap<Integer, Double> mapPratt = new HashMap<Integer,Double>();
		for (int j = 0; j < this.gtRegionLabels.size(); j++) {
			for (int i = 0; i < this.segRegionLabels.size(); i++) {
				if (this.matchingMatrix[i][j] == 1) {
					mapPratt.put(
						this.gtRegionLabels.get(j), new Double(this.compute(j, i)));
					break;
				}
			}
		}
		HashMap< String, HashMap<Integer, Double> > resultMap =
				new HashMap<String, HashMap<Integer,Double>>();
		resultMap.put("Pratt", mapPratt);
		MTBGroundtruthEvaluationData resultData = 
				new MTBGroundtruthEvaluationData(resultMap);
		return resultData;
	}
	
	/**
	 * This method implements the PRA itself for two given contours.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Pratt's error measure.
	 */
	protected double compute (int gtIndex, int segIndex) {
		double sum = 0;
		double min = Double.MAX_VALUE;

		/* 
		 * computes the sum formula of Pratt's figure of merit by calculating the 
		 * squared distance for each
		 * segmentation contour pixel to the nearest ground truth contour pixel
		 */
		for (Point point1: this.segContours.get(segIndex)) {
			for (Point point2: this.gtContours.get(gtIndex)) {
//				min = Math.min(min, 
//						Math.sqrt(Math.pow(point1.x - point2.x, 2) 
//												+ Math.pow(point1.y - point2.y, 2)));
				min = Math.min(min, 
								Math.pow(point1.x - point2.x, 2) 
									+ Math.pow(point1.y - point2.y, 2));
			}
			sum += (1.0 / (1.0 + min));
			min = Double.MAX_VALUE;
		}
		return sum * 1.0 / Math.max(
									(double)this.gtContours.get(gtIndex).size(), 
									(double)this.segContours.get(segIndex).size()); 
	}
}