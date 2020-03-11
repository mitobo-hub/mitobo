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
 * This class implements 3 detection errors for contour detection evaluation.
 * <p>
 * In detail the measures are:
 * <ul>
 * <li> oversegmentation error (ODE)
 * <li> undersegmentation errro (UDE)
 * <li> localization error (LE)
 * </ul>
 * 
 * @author knispel, moeller
 */
public class Measure_DetectionErrors extends EvaluationMeasureContours {

	/**
	 * Default constructor.
	 * @throws ALDOperatorException		Thrown in case of failure.
	 */
	public Measure_DetectionErrors() throws ALDOperatorException {
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
	 * @param mmatrix					Assignment matrix.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public Measure_DetectionErrors(MTBImageShort segImg, MTBImageShort gtImg, 
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
		this.evalData = this.ComputeDetectionErrors();
		this.resultImg = this.showContours(this.gtLabelImage, this.segContours);
	}
	
	/**
	 * This method is the implementation of the Detection Errors.
	 * @return Evaluation data object.
	 */
	private MTBGroundtruthEvaluationData ComputeDetectionErrors () {
		HashMap<Integer, Double> mapODE = new HashMap<Integer,Double>();
		HashMap<Integer, Double> mapUDE = new HashMap<Integer,Double>();
		HashMap<Integer, Double> mapLE = new HashMap<Integer,Double>();
		int i;
		for (int j = 0; j < this.gtRegionLabels.size(); j++) {
			for (i = 0; i < this.segRegionLabels.size(); i++) {
				if (this.matchingMatrix[j][i] == 1) {
					mapODE.put(this.gtRegionLabels.get(j), new Double(this.ODE(j, i)));
					mapUDE.put(this.gtRegionLabels.get(j), new Double(this.UDE(j, i)));
					mapLE.put(this.gtRegionLabels.get(j), new Double(this.LE(j, i)));
					break;
				}
			}
			// region not matched
			if (i == this.segRegionLabels.size()) {
				mapODE.put(this.gtRegionLabels.get(j), Double.NaN);
				mapUDE.put(this.gtRegionLabels.get(j), Double.NaN);
				mapLE.put(this.gtRegionLabels.get(j), Double.NaN);
			} 
		}

		if(this.segRegionLabels.size() == 0)
		{
			mapODE.put(this.gtRegionLabels.get(0), new Double(0));
			mapUDE.put(this.gtRegionLabels.get(0), new Double(1));
			mapLE.put(this.gtRegionLabels.get(0), new Double(1));
		}

		HashMap< String, HashMap<Integer, Double> > resultMap =
				new HashMap<String, HashMap<Integer,Double>>();
		resultMap.put("ODE", mapODE);
		resultMap.put("UDE", mapUDE);
		resultMap.put("LE", mapLE);
		MTBGroundtruthEvaluationData resultData = 
				new MTBGroundtruthEvaluationData(resultMap);
		return resultData;
	}
	
	/**
	 * This method computes the overdetection error.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Overdetection error.
	 */
	private double ODE (int gtIndex, int segIndex) {
		int count = 0;

		for (Point point: this.segContours.get(segIndex)) {
			if (!this.gtContours.get(gtIndex).contains(point))
				count++;
		}
		// old implementation
		//return count / 	( (double)this.segLabels.getSizeX()
		//		            * (double)this.segLabels.getSizeY()
		//		            -          this.gtContours.get(gtIndex).size());
		if((double)this.segContours.get(segIndex).size() == 0)
			return 1;
		return (double)count / (double)this.segContours.get(segIndex).size();
	}
	
	/**
	 * This method computes the underdetection error.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Underdetection error.
	 */
	private double UDE (int gtIndex, int segIndex) {
		int count = 0;

		for (Point point: this.gtContours.get(gtIndex)) {
			if (!this.segContours.get(segIndex).contains(point))
				count++;
		}
		if((double)this.gtContours.get(gtIndex).size() == 0)
			return 1;
		return (double)count / (double)this.gtContours.get(gtIndex).size();
	}
	
	/**
	 * This method implements the localization error.
	 * 
	 * @param gtIndex		Index of groundtruth region.
	 * @param segIndex	Index of matched segmented region.
	 * @return	Localization error.
	 */
	private double LE (int gtIndex, int segIndex) {
		int count1 = 0;
		for (Point point: this.segContours.get(segIndex)) {
			if (!this.gtContours.get(gtIndex).contains(point))
				count1++;
		}

		int count2 = 0;
		for (Point point: this.gtContours.get(gtIndex)) {
			if (!this.segContours.get(segIndex).contains(point))
				count2++;
		}
		// old implementation
		//return ((double)count1 + (double)count2) /
		//		 ((double)this.segLabels.getSizeX()
		//				 * (double)this.segLabels.getSizeY());
		if(		(double)this.gtContours.get(gtIndex).size() 
				+ (double)this.segContours.get(segIndex).size() == 0)
			return 1;
		return ((double)count1 + (double)count2) /
				(			(double)this.gtContours.get(gtIndex).size() 
						+ (double)this.segContours.get(segIndex).size());
	}
}
