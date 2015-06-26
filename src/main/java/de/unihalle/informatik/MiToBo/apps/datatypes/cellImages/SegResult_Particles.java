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

import java.util.HashMap;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;

/**
 * Particle detection result for a single channel of an image.
 * <p>
 * Particles are defined as sub-cellular structures inside cells which are 
 * fluorescently labeled. The result of a particle detection comprises data
 * like number and average size of particles, or the set of corresponding 
 * regions. In addition, binary masks of the particles are provided.
 * 
 * @author moeller
 */
@ALDParametrizedClass
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class SegResult_Particles 
	implements SegResult_Interface {

	/**
	 * Name of the analyzed image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -10)
	protected String image_name;
	
	/**
	 * Channel of the image that was processed.
	 */
	@ALDClassParameter(label="Analyzed channel", dataIOOrder = -9)
	protected int image_channel;
	
	/**
	 * Binary segmentation mask.
	 */
	@ALDClassParameter(label="Binary mask")
	protected MTBImageByte binMask = null;
	
	/**
	 * List of detected regions.
	 */
	@ALDClassParameter(label="Set of regions", dataIOOrder = -8)
	protected MTBRegion2DSet particleRegions;
	
	/**
	 * Count of detected particles.
	 */
	@ALDClassParameter(label="Number of particles", dataIOOrder = -7)
	protected int number_particles = 0;

	/**
	 * Average size of particles.
	 */
	@ALDClassParameter(label="Average size", dataIOOrder = -6)
	protected double avgsize_particles = 0;

	/**
	 * Unit in which measurements are provided.
	 */
	@ALDClassParameter(label="Measurement unit", dataIOOrder = -5)
	protected MeasureUnit units = MeasureUnit.pixels;

	/**
	 * Particle counts per cell (optional).
	 */
	@ALDClassParameter(label="Particle counts per cell", dataIOOrder = -4)
	protected HashMap<Integer, Integer> perCellCount = null;

	/**
	 * Average particle size per cell (optional).
	 */
	@ALDClassParameter(label="Average particle size per cell", dataIOOrder = -3)
	protected HashMap<Integer, Double> perCellAvgSize = null;

	/**
	 * Default constructor.
	 * 
	 * @param imgName		Name of analyzed image.
	 * @param ch				Processed channel.
	 * @param regions		Set of detected regions.
	 * @param mask			Binary mask with segmentation result.
	 * @param count			Number of detected particles.
	 * @param avgsize		Average size of nuclei.
	 */
	public SegResult_Particles(String imgName, int ch, 
			MTBRegion2DSet regions,	MTBImageByte mask, int count, double avgsize) {
		if (imgName != null)
			this.image_name = imgName;
		if (mask != null)
			this.binMask = mask;
		if (regions != null)
			this.particleRegions = regions;
		this.image_channel = ch;
		this.number_particles = count;
		this.avgsize_particles = avgsize;
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.apps.helpers.MTBCellImgSegResult_Interface#getImageName()
	 */
	/**
	 * Note: Returned string might be null if the result is associated with a
	 * 			 single cell rather than a complete image!
	 */
	@Override
  public String getImageName() {
		return this.image_name;
  }
	
	/**
	 * Get the processed channel.
	 * @return Channel index.
	 */
	public int getProcessedChannel() {
		return this.image_channel;
	}
	
	/**
	 * Returns binary mask with segmentation result.
	 * <p>
	 * Note: Returned image might be null if the result is associated with a
	 * 			 single cell rather than a complete image!
	 * @return Binary image.
	 */
	public MTBImageByte getMask() {
		return this.binMask;
	}
	
	/**
	 * Returns number of detected particles.
	 * @return Number of detected particles.
	 */
	public int getParticleCount() {
		return this.number_particles;
	}
	
	/**
	 * Returns average size of detected particles.
	 * @return Average size of particles.
	 */
	public double getParticleAvgSize() {
		return this.avgsize_particles;
	}
	
	/**
	 * Sets particle counts per cell.
	 * @param map	Map with index-count pairs.
	 */
	public void setPerCellCount(HashMap<Integer, Integer> map) {
		this.perCellCount = map;
	}
	
	/**
	 * Returns detected particles per cell.
	 * @return	Map with index-count pairs.
	 */
	public HashMap<Integer, Integer> getPerCellCount() {
		return this.perCellCount;
	}

	/**
	 * Sets particle average sizes per cell.
	 * @param map	Map with index-size pairs.
	 */
	public void setPerCellAvgSize(HashMap<Integer, Double> map) {
		this.perCellAvgSize = map;
	}

	/**
	 * Returns detected particle average size per cell.
	 * @return	Map with index-size pairs.
	 */
	public HashMap<Integer, Double> getPerCellAvgSize() {
		return this.perCellAvgSize;
	}

	@Override
  public MeasureUnit getMeasurementUnit() {
	  return this.units;
  }
}
