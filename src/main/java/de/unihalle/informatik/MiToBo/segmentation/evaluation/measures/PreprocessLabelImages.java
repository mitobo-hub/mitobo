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
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Helper class to preprocess label images for segmentation evaluation.
 * <p>
 * This class extracts the intensity values of regions in label images as 
 * well as the regions' contours.
 * 
 * @author Birgit Moeller
 * @author Felix Knispel
 */
public class PreprocessLabelImages extends MTBOperator {

	/**
	 * Label image of segmentation result.
	 */
	@Parameter( label= "Segmentation label image", required = true, 
		direction=Parameter.Direction.IN, 
    description = "Segmentation result label image.")
	protected transient MTBImage segImage = null;

	/**
	 * Groundtruth labeling.
	 */
	@Parameter( label= "Groundtruth label image", required = true, 
		direction=Parameter.Direction.IN, description = "Groundtruth label image.")
	protected transient MTBImage gtImage = null;

	/**
	 * List of labels of segmented regions.
	 */
	@Parameter( label= "Segmented Region Labels", 
		direction=Parameter.Direction.IN, description = "Segmented Region Labels.")
	protected transient ArrayList<Integer> segLabels = null;
	
	/**
	 * List of labels of groundtruth regions.
	 */
	@Parameter( label= "Groundtruth Region Labels", 
		direction=Parameter.Direction.IN, description = "Groundtruth Labels.")
	protected transient ArrayList<Integer> gtLabels = null;
	
	/**
	 * List of contours of segmented objects.
	 */
	@Parameter( label= "Segmented contours.", 
		direction=Parameter.Direction.OUT, description = "Segmented contours.")
	protected ArrayList<ArrayList<Point>> segContours= null;
	
	/**
	 * List of contours of groudtruth objects.
	 */
	@Parameter( label= "Groundtruth contours.",
		direction=Parameter.Direction.OUT, description = "Groundtruth contours.")
	protected ArrayList<ArrayList<Point>> gtContours= null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public PreprocessLabelImages() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Constructor.
	 * @param gtImg Groundtruth image.
	 * @param segImg Segmentation image.
	 * @param sLabels List of labels in segmentation image.
	 * @param gLabels List of labels in groundtruth image.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public PreprocessLabelImages(MTBImage gtImg, MTBImage segImg,
			ArrayList<Integer> sLabels, ArrayList<Integer> gLabels) 
			throws ALDOperatorException {
		this.gtImage = gtImg;
		this.segImage = segImg;
		this.gtLabels = gLabels;
		this.segLabels = sLabels;
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (   this.gtImage.getSizeX() != this.segImage.getSizeX() 
				|| this.gtImage.getSizeY() != this.segImage.getSizeY()) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[PreprocessLabelImages] groundtruth and segmentation images " 
							+ "must have the same size!"); 
	}
	
	/**
	 * This method initializes all required components.
	 */
	@Override
  protected void operate() {
		this.segContours = new ArrayList<ArrayList<Point>>();
		this.gtContours = new ArrayList<ArrayList<Point>>();
		this.CollectContoursStageTwo(
				this.gtImage.duplicate(), this.gtLabels, this.gtContours, 
				this.CollectContoursStageOne(this.gtImage, this.gtLabels));
		this.CollectContoursStageTwo(
				this.segImage.duplicate(), this.segLabels, this.segContours, 
				this.CollectContoursStageOne(this.segImage, this.segLabels));
	}
	
	/**
	 * This method collects all contour pixels for a region in a given MTBImage.
	 * 
	 * PRE:
	 * > requires the region image
	 * > requires a list of Integer, where these Integer is the color value 
	 * 		of a region in the image
	 * 
	 * POST:
	 * > outputs an boolean array where 'true' means a pixel on this coordinates 
	 * 		is a contour pixel, and false not
	 * 
	 * @param img			Region image.
	 * @param regions	Label set.
	 * @return Boolean array.
	 */
	private boolean[][] CollectContoursStageOne(MTBImage img, 
			ArrayList<Integer> regions) {
		boolean[][] result = new boolean[img.getSizeX()][img.getSizeY()];
		//for each region of the image
		for (int i = 0; i < regions.size(); i++) {
			/*
			 * searching for a pixel that is arranged in the region
			 * if this pixel has a neighbor with an other value 
			 * (black or other region) this pixel has to be a contour pixel
			 */
			for (int y = 0; y < img.getSizeY(); y++) {
				for (int x = 0; x < img.getSizeX(); x++) {
					if (img.getValueInt(x, y) == regions.get(i).intValue()) {
						try { 
							if (img.getValueInt(x - 1, y - 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x - 1, y) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x - 1, y + 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try {
							if (img.getValueInt(x, y - 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x, y + 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x + 1, y - 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x + 1, y) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (img.getValueInt(x + 1, y + 1) != regions.get(i).intValue()) 
								result[x][y] = true; 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * This method implements the final stage of the contour detection. 
	 * <p>
	 * It is needed for region images with unclean borders.
	 * 
	 * PRE:
	 * > requires the region image where all contours are white (255)
	 * > requires a list of Integer, where these Integer is the color value 
	 * 		of a region in the image
	 * > requires a list, where the contours have to be stored 
	 * 		(will be overwritten)
	 * 
	 * POST:
	 * > outputs the new and final contour-list for all regions in the image
	 * 
	 * @param img				Region image with white contours.
	 * @param regions		Labels of regions.
	 * @param contours	Data structure for result contours.
	 * @param isContour Array where contour pixels are marked.
	 */
	private void CollectContoursStageTwo(MTBImage img, 
			ArrayList<Integer> regions, ArrayList<ArrayList<Point>> contours, 
			boolean[][] isContour) {
		contours.clear();
		//for each region of the image
		for (int i = 0; i < regions.size(); i++) {
			contours.add(new ArrayList<Point>());
			/*
			 * searching for a pixel that is arranged in the region
			 * if this pixel has a true-flagged neighbor this neighbor has to be 
			 * a contour-pixel for the correspondent region
			 */
			for (int y = 0; y < img.getSizeY(); y++) {
				for (int x = 0; x < img.getSizeX(); x++) {
					if (    img.getValueInt(x, y) == regions.get(i).intValue() 
							&& !isContour[x][y]) {
						try { 
							if (    isContour[x - 1][y - 1] 
									&& !contours.get(i).contains(new Point(x - 1, y - 1))) 
								contours.get(i).add(new Point(x - 1, y - 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x - 1][y] 
									&& !contours.get(i).contains(new Point(x - 1, y))) 
								contours.get(i).add(new Point(x - 1, y)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x - 1][y + 1] 
									&& !contours.get(i).contains(new Point(x - 1, y + 1))) 
								contours.get(i).add(new Point(x - 1, y + 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x][y - 1] 
									&& !contours.get(i).contains(new Point(x, y - 1))) 
								contours.get(i).add(new Point(x, y - 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x][y + 1] 
									&& !contours.get(i).contains(new Point(x, y + 1))) 
								contours.get(i).add(new Point(x, y + 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x + 1][y - 1] 
									&& !contours.get(i).contains(new Point(x + 1, y - 1))) 
								contours.get(i).add(new Point(x + 1, y - 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x + 1][y] 
									&& !contours.get(i).contains(new Point(x + 1, y))) 
								contours.get(i).add(new Point(x + 1, y)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
						try { 
							if (    isContour[x + 1][y + 1] 
									&& !contours.get(i).contains(new Point(x + 1, y + 1))) 
								contours.get(i).add(new Point(x + 1, y + 1)); 
							} catch(ArrayIndexOutOfBoundsException e) { /* just ignore... */ }
					}
				}
			}
		}
	}
	
	/**
	 * Get list of segmentation contours.
	 * @return Segmented contours.
	 */
	public ArrayList<ArrayList<Point>> getSegContours() {
		return this.segContours;
	}

	/**
	 * Get list of groundtruth contours.
	 * @return Groundtruth contours.
	 */
	public ArrayList<ArrayList<Point>> getGtContours() {
		return this.gtContours;
	}
}
