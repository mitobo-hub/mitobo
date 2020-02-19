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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.apps.datatypes.cellImages;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

/**
 * Cell boundary segmentation result from single-layer image.
 * <p>
 * Objects of this class subsume cell boundaries, number of cells, average
 * size and other figures relevant for cell image analysis.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class SegResult_Cytoplasm 
	implements SegResult_Interface {

	/**
	 * Name of the analyzed image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -10)
	protected String image_name;

	/**
	 * Set of detected cell boundaries.
	 */
	@ALDClassParameter(label="Detected contours", dataIOOrder = -8)
	protected MTBPolygon2DSet cellContours = null;

	/**
	 * Label mask.
	 */
	@ALDClassParameter(label="Label segmentation mask", dataIOOrder = -7)
	protected MTBImageByte labelImage = null;
	
	/**
	 * Count of detected cells.
	 */
	@ALDClassParameter(label="Cell count", dataIOOrder = -9)
	protected int number_cells = 0;

	/**
	 * Array indexed with snake IDs containing region sizes of snake interiors.
	 */
	@ALDClassParameter(label="Array of cell sizes", dataIOOrder = -6)
	protected int [] cellSizes = null;
	
	/**
	 * Average size of cells.
	 */
	@ALDClassParameter(label="Average cell size", dataIOOrder = -5)
	protected double avgsize_cells = 0;

	/**
	 * Unit in which measurements are provided.
	 */
	@ALDClassParameter(label="Measurement unit", dataIOOrder = -4)
	protected MeasureUnit units = MeasureUnit.pixels;
	
	/**
	 * Input image with snake contours overlayed (optional).
	 */
	@ALDClassParameter(label="Optional result overlay")
	protected MTBImageRGB resultCellImg= null;

	/**
	 * Default constructor.
	 * 
	 * @param imgName		Name of processed image.
	 * @param conts			Cell boundary contours.
	 * @param labels		Label image of segmentation result.
	 * @param count			Number of detected cells.
	 * @param avgsize		Average size of cells.
	 */
	@Deprecated
	public SegResult_Cytoplasm(String imgName, MTBPolygon2DSet conts, 
			MTBImageByte labels, int count, double avgsize) {
		this.image_name = imgName;
		this.labelImage = labels;
		this.number_cells = count;
		this.avgsize_cells = avgsize;
		this.cellContours = conts;
	}

	/**
	 * Default constructor.
	 * 
	 * @param imgName		Name of processed image.
	 * @param conts			Cell boundary contours.
	 * @param labels		Label image of segmentation result.
	 * @param count			Number of detected cells.
	 * @param sizes			Array with sizes of cell interiors.
	 * @param avgsize		Average size of cells.
	 */
	public SegResult_Cytoplasm(String imgName, MTBPolygon2DSet conts, 
			MTBImageByte labels, int count, int [] sizes, double avgsize) {
		this.image_name = imgName;
		this.labelImage = labels;
		this.number_cells = count;
		this.cellSizes = sizes;
		this.avgsize_cells = avgsize;
		this.cellContours = conts;
	}
	
	@Override
  public String getImageName() {
		return this.image_name;
  }
	
	/**
	 * Returns extracted contours.
	 * @return Set of contours.
	 */
	public MTBPolygon2DSet getContours() {
  	return this.cellContours;
  }

	/**
	 * Returns label mask with segmentation result.
	 * @return Label mask of segmentation.
	 */
	public MTBImageByte getLabelImage() {
		return this.labelImage;
	}
	
	/**
	 * Returns number of detected cells.
	 * @return	Number of detected cells.
	 */
	public int getCellCount() {
		return this.number_cells;
	}

	/**
	 * Returns sizes of snake interiors.
	 * @return	Array with cell sizes.
	 */
	public int [] getCellSizes() {
		return this.cellSizes;
	}
	
	/**
	 * Returns average size of detected cells.
	 * @return	Average size of cells.
	 */
	public double getCellAvgSize() {
		return this.avgsize_cells;
	}

	@Override
  public MeasureUnit getMeasurementUnit() {
	  return this.units;
  }
	
	/**
	 * Specify (optional) result image with cell contours.
	 * @param	Color overlay of result contours.
	 */
	public void setResultCellImg(MTBImageRGB img) {
		this.resultCellImg = img;
	}

	/**
	 * Returns (optional) result image with cell contours.
	 * @return	Color overlay image of result contours.
	 */
	public MTBImageRGB getResultCellImg() {
		return this.resultCellImg;
	}
}
