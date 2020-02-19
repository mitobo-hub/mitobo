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

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Common super class for all nucleus separators.
 * <p>
 * This class is mainly used for enabling Alida/MiToBo operators to handle 
 * different nucleus separator algorithms in a generic fashion.
 * 
 * @author moeller
 */
public abstract class NucleusSeparator2DAlgos extends MTBOperator {

	/**
	 * Input image to be processed.
	 */
	@Parameter( label= "Greyscale Input Image", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder= -101, 
			mode= ExpertMode.STANDARD, 
      description = "Greyscale input image to be analyzed.")	
	protected transient MTBImage inputImg= null;
	
	/**
	 * Input image to process.
	 */
	@Parameter( label= "Label input image", required = true, dataIOOrder = -100,
			direction= Direction.IN, description = "Label input image of nuclei.")
	protected transient MTBImage labelImg = null;

	/**
	 * Result label image of separated nuclei.
	 */
	@Parameter( label= "Result image", direction= Direction.OUT, 
      description = "Label image of separated nuclei.")
	protected transient MTBImageShort resultImg = null;

	/** Get value of resultImg.
	  * Explanation: Label image of seprated nuclei.
	  * @return value of resultImg
	  */
	public MTBImageShort getResultImg(){
		return this.resultImg;
	}

	/** Set value of resultImg.
	  * Explanation: Label image of seprated nuclei.
	  * @param value New value of resultImg
	  */
	public void setResultImg(MTBImageShort value){
		this.resultImg = value;
	}

	/**
	 * Specify label input image.
	 */
	public void setInputLabelImage(MTBImage img) {
		this.labelImg = img;
	}
	
	/**
	 * Specify input gray-scale image.
	 */
	public void setInputGrayScaleImage(MTBImage img) {
		this.inputImg = img;
	}

	/**
	 * Empty constructor.
	 */
	public NucleusSeparator2DAlgos() throws ALDOperatorException {
		// nothing to be done here
	}

  @Override
  protected abstract void operate() 
		throws ALDOperatorException, ALDProcessingDAGException;
}
