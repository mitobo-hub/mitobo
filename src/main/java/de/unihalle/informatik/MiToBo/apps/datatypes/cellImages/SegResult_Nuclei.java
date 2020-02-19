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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;

/**
 * Nuclei segmentation result from a single image channel.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class SegResult_Nuclei 
	implements SegResult_Interface {

	/**
	 * Name of the analyzed image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -10)
	protected String image_name;
	
	/**
	 * Binary segmentation mask.
	 */
	@ALDClassParameter(label="Binary mask")
	protected MTBImageByte binMask = null;
	
	/**
	 * Gray-scale label image of nuclei regions.
	 */
	@ALDClassParameter(label="Labeled nuclei regions")
	protected MTBImage labelImage = null;
	
	/**
	 * Set of detected regions.
	 */
	@ALDClassParameter(label="Set of nuclei regions", dataIOOrder = -8)
	protected MTBRegion2DSet nucleiRegions = null;
	
	/**
	 * Index of the nucleus channel, if -1 it is unknown or image has only one.
	 */
	@ALDClassParameter(label="Nucleus channel", dataIOOrder = -9)
	protected int nucleusChannelID = -1;
	
	/**
	 * Count of detected nuclei.
	 */
	@ALDClassParameter(label="Number of nuclei", dataIOOrder = -7)
	protected int number_nuclei = 0;

	/**
	 * Average size of nuclei.
	 */
	@ALDClassParameter(label="Average size", dataIOOrder = -6)
	protected double avgsize_nuclei = 0;

	/**
	 * Unit in which measurements are provided.
	 */
	@ALDClassParameter(label="Measurement unit", dataIOOrder = -5)
	protected MeasureUnit units = MeasureUnit.pixels;
	
	/**
	 * Default constructor.
	 * 
	 * @param imgName		Name of processed image.
	 * @param channel		Index of nucleus channel.
	 * @param mask			Binary mask with segmentation result.
	 * @param count			Number of detected nuclei.
	 * @param avgsize		Average size of nuclei.
	 * @param chan			Nucleus channel ID.
	 */
	public SegResult_Nuclei(String imgName, int channel,
			MTBImageByte mask, MTBImage labels, MTBRegion2DSet regs,
			int count, double avgsize) {
		this.image_name = imgName;
		this.nucleusChannelID = channel;
		this.binMask = mask;
		this.labelImage = labels;
		this.nucleiRegions = regs;
		this.number_nuclei = count;
		this.avgsize_nuclei = avgsize;
		this.nucleusChannelID = -1;
	}
	
	/**
	 * Specify image channel containing nuclei (just for reference).
	 * @param c		Nucleus channel ID.
	 */
	public void setNucleusChannel(int c) {
		this.nucleusChannelID = c;
	}
	
	@Override
  public String getImageName() {
		return this.image_name;
  }

	/**
	 * Specify binary mask of nuclei.
	 * @param m	Binary mask of detected nuclei.
	 */
	public void setMask(MTBImageByte m) {
		this.binMask = m;
	}
	
	/**
	 * Returns binary mask with segmentation result.
	 * @return Binary mask of segmented nuclei.
	 */
	public MTBImageByte getMask() {
		return this.binMask;
	}
	
	/**
	 * Set label image of detected nuclei.
	 * @param limg	Label image with detected nuclei.
	 */
	public void setLabelImage(MTBImage limg) {
		this.labelImage = limg;
	}

	/**
	 * Returns label image of detected nuclei.
	 * @return Label image of segmentation result.
	 */
	public MTBImage getLabelImage() {
		return this.labelImage;
	}
	
	/**
	 * Specify set of regions.
	 * @param	Set of detected nucleus regions.
	 */
	public void setNucleiRegions(MTBRegion2DSet regs) {
		this.nucleiRegions = regs;		
	}
	
	/**
	 * Returns set of detected regions.
	 * @return	Set of nucleus regions.
	 */
	public MTBRegion2DSet getNucleiRegions() {
		return this.nucleiRegions;
	}
	
	/**
	 * Specify number of segmented nuclei regions.
	 * @param	c Number of detected nuclei.
	 */
	public void setNucleiCount(int c) {
		this.number_nuclei = c;
	}
	
	/**
	 * Returns number of detected nuclei.
	 * @return	Count of detected nuclei.
	 */
	public int getNucleiCount() {
		return this.number_nuclei;
	}

	/**
	 * Specify average size of nuclei.
	 * @param avg	Average size of detected nuclei.
	 */
	public void setNucleiAvgSize(double avg) {
		this.avgsize_nuclei = avg;
	}
	
	/**
	 * Returns average size of detected nuclei.
	 * @return Average size of detected nuclei.
	 */
	public double getNucleiAvgSize() {
		return this.avgsize_nuclei;
	}

	@Override
  public MeasureUnit getMeasurementUnit() {
	  return this.units;
  }
	
	/**
	 * Specify unit for measurements.
	 * @param	u	Unit used for area and length measurements. 
	 */
	public void setMeasurementUnit(MeasureUnit u) {
		this.units = u;
	}
}
