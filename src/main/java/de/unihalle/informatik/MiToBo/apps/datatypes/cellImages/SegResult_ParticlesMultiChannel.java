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
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;

/**
 * Particle detection result for a multi-channel image.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class SegResult_ParticlesMultiChannel 
	implements SegResult_Interface {

	/**
	 * Name of the analyzed image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -10)
	protected String image_name;

	/**
	 * Unit in which measurements are provided.
	 */
	@ALDClassParameter(label="Measurement unit", dataIOOrder = -5)
	protected MeasureUnit units = MeasureUnit.pixels;

	/**
	 * Name of the analyzed image.
	 */
	@ALDClassParameter(label="Processed image", dataIOOrder = -10)
	protected Vector<SegResult_Particles> resultVector;

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
	public SegResult_ParticlesMultiChannel(String imgName) {
		this.image_name = imgName;
		this.resultVector = new Vector<SegResult_Particles>();
	}
	
	public void addSegmentationResult(SegResult_Particles result) {
		this.resultVector.add(result);
	}
	
	public Vector<SegResult_Particles> getResultVec() {
		return this.resultVector;
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

	@Override
  public MeasureUnit getMeasurementUnit() {
	  return this.units;
  }
}
