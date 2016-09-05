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
import java.awt.Point;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;

/**
 * Class implements the super class for contour detection evaluation operators.
 * <p>
 * These operators need a segmentation image and a ground truth image. 
 * They evaluate a contour segmentation in comparison to the ground truth 
 * segmentation by an expert.
 * Both images have to be region images and have to have the same size.
 * 
 * The class is able to detect all regions (which a later identified by their 
 * pixel value) and their contours. In addition the class is able to allocate 
 * regions, that we know which region in the segmentation image belongs to 
 * which region in the ground truth image.
 * 
 * This method just collects regions and region contours. If there are no 
 * regions in an image the accordingly lists (gtRegions, segRegions, 
 * gtContours, segContours) are not allocated.
 * 
 * @author knispel, moeller
 */
public abstract class EvaluationMeasureContours extends EvaluationMeasure {

	/**
	 * List of point-wise segmented contours.
	 */
	@Parameter( label= "Segmented Contours", required = true, 
		direction=Parameter.Direction.IN, dataIOOrder = 0,
		description = "List of segmented contours (point lists).")
	protected transient ArrayList<ArrayList<Point>> segContours= null;

	/**
	 * List of point-wise groundtruth contours.
	 */
	@Parameter( label= "Groundtruth Contours", required = true, 
		direction=Parameter.Direction.IN, dataIOOrder = 1,
		description = "List of groundtruth contours (point lists).")
	protected transient ArrayList<ArrayList<Point>> gtContours = null;

	/**
	 * List of labels of segmented regions.
	 */
	@Parameter( label= "Labels of Segmented Regions", required = true, 
		direction=Parameter.Direction.IN, dataIOOrder = 2,
		description = "Labels of segmented regions.")
	protected transient ArrayList<Integer> segRegionLabels= null;

	/**
	 * List of labels of groundtruth regions.
	 */
	@Parameter( label= "Labels of Groundtruth Regions", required = true, 
		direction=Parameter.Direction.IN, dataIOOrder = 3,
		description = "Labels of groundtruth regions.")
	protected transient ArrayList<Integer> gtRegionLabels= null;

	/**
	 * Result image showing the ground truth image and the segmented contours.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0,
		direction = Parameter.Direction.OUT, description = "Result image.")
	protected MTBImage resultImg = null;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public EvaluationMeasureContours() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * This method shows the contour pixel on a given image.
	 * 
	 * PRE:
	 * > requires the image
	 * > requires a list of lists of contour pixels for each region in the image
	 * 
	 * POST:
	 * > outputs a copy of the given image where each contour pixel is marked white
	 * 
	 * @param img				Image in which to plot the contours.
	 * @param contours	Contours to plot.
	 * @return	Image with contours.
	 */
	protected MTBImage showContours(MTBImage img, 
			ArrayList<ArrayList<Point>> contours) {
		MTBImage result = img.duplicate();
		Point point = new Point();

		for(int i = 0; i < contours.size(); i++) {
			for (int j = 0; j < contours.get(i).size(); j++) {
				point = contours.get(i).get(j);
				result.putValueInt(point.x, point.y, 255);
			}
		}
		return result;
	}
}
