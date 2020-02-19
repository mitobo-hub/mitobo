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

package de.unihalle.informatik.MiToBo.apps.datatypes.cellImages;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

/**
 * Complete segmentation result from multi-channel image.
 * <p>
 * This data structure subsumes cell boundary, nuclei and structure
 * (particles, granules, etc.) segmentation results.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class SegResult_Complete implements SegResult_Interface {

	/**
	 * Indicator for segmentation mode.
	 * 
	 * @author moeller
	 */
	public static enum SegmentationMode {
		/**
		 * No individual cells are considered.
		 */
		IMAGE_COMPLETE,
		/**
		 * Individual cells are considered. 
		 */
		INDIVIDUAL_CELLS
	}
	
	/**
	 * Name of the image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -20)
	protected String image_name;
	
	/**
	 * Number of channels in the image.
	 */
	@ALDClassParameter(label="Number of channels in image", dataIOOrder = -19)
	protected int number_channels = 0;

	/**
	 * Mode how the channel of the image was treated during segmentation.
	 */
	@ALDClassParameter(label="Segmentation mode", dataIOOrder = -18)
	protected SegmentationMode segMode;
	
	/**
	 * Result for nucleus channel.
	 */
	@ALDClassParameter(label="Nuclei segmentation result", dataIOOrder = -17)
	protected SegResult_Nuclei nucleusResult;
	
	/**
	 * Result for cell/cytoplasm channel.
	 */
	@ALDClassParameter(label="Cytoplasm segmentation result", dataIOOrder = -16)
	protected SegResult_Cytoplasm cellsResult;
		
	/**
	 * Detection results for particles. 
	 * <p>
	 * The map size is equal to the number of channels the image has 
	 * and is indexed with the channel ID starting with 0.
	 * The contents of the detection results depend on the mode:
	 * - IMAGE_COMPLETE: only global data
	 * - INDIVIDUAL_CELLS: in addition particles per cell statistics
	 */
	@ALDClassParameter(label="Particle segmentation results", dataIOOrder = -15)
	protected HashMap<Integer,SegResult_Particles> particleResults;

	/**
	 * (Optional) stack of result images with segmentation masks/overlays.
	 */
	@ALDClassParameter(label="Result image stack")
	protected MTBImageRGB resultImageStack = null;
	
	/**
	 * Unit in which measurements are provided.
	 */
	@ALDClassParameter(label="Measurement units", dataIOOrder = -10)
	protected MeasureUnit units = MeasureUnit.pixels;

	/**
	 * Default constructor.
	 * 
	 * @param iname			Name of the corresponding image.
	 * @param channels	Number of channels of the processed image.
	 * @param smode			Segmentation mode.
	 * @param cells			Cell contour segmentation result.
	 */
	public SegResult_Complete(String iname, int channels,
			SegmentationMode smode, SegResult_Cytoplasm cells) {
		this.image_name = iname;
		this.number_channels = channels;
		this.segMode = smode;
		this.cellsResult = cells;
		this.particleResults = new HashMap<Integer,SegResult_Particles>();
	}
	
	@Override
  public String getImageName() {
		return this.image_name;
	}
	
	/**
	 * Returns the number of image channels available.
	 */
	public int getNumberChannels() {
		return this.number_channels;
	}
	
	/**
	 * Returns reference to cytoplasm/cell boundary detection result.
	 */
	public SegResult_Cytoplasm getCytoplasmResult() {
		return this.cellsResult;
	}
	
	/**
	 * Set nuclei detection result.
	 */
	public void setNucleiResult(SegResult_Nuclei nr) {
		this.nucleusResult= nr;
	}

	/**
	 * Returns nuclei detection result.
	 */
	public SegResult_Nuclei getNucleiResult() {
		return this.nucleusResult;
	}

	/**
	 * Set particle counts per channel.
	 * <p>
	 * Note that channel IDs start with 0! Prior results are replaced.
	 */
	public void setParticleResult(
			SegResult_Particles res, int channel) {
		this.particleResults.put(new Integer(channel),res);
	}

	/**
	 * Returns number of detected particles per channel.
	 */
	public SegResult_Particles getParticleResult(int channel) {
		return this.particleResults.get(new Integer(channel));
	}
	
	/**
	 * Set stack with result segmentation images.
	 */
	public void setResultImageStack(MTBImageRGB stack) {
		this.resultImageStack = stack;
	}

	/**
	 * Returns stack with result segmentation images.
	 */
	public MTBImageRGB getResultImageStack() {
		return this.resultImageStack;
	}
	
	/**
	 * Returns mode activated during segmentation.
	 */
	public SegmentationMode getSegmentationMode() {
		return this.segMode;
	}
	
	@Override
  public MeasureUnit getMeasurementUnit() {
	  return this.units;
  }
}
