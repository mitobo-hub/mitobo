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

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Crop an image part.
 * 
 * @author Stefan Posch
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class CropImage extends MTBOperator {

	@Parameter( label= "InputImage", required = true,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
      description = "Input image")
	private transient MTBImage inputImg = null;
	
	@Parameter( label= "ResultingImage", required = true,
			direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
			description = "Resulting image")
	private transient MTBImage resultImg = null;
	
	@Parameter( label= "startX", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=2,
			description = "First x coordinate of crop")
	private Integer startX = 0;
	
	@Parameter( label= "sizeX", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=3,
			description = "Size in x direction")
	private Integer sizeX = 1;
	
	@Parameter( label= "startY", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=4,
			description = "First y coordinate of crop")
	private Integer startY = 0;
	
	@Parameter( label= "sizeY", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=5,
			description = "Size in y direction")
	private Integer sizeY = 1;
	
	@Parameter( label= "startZ", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=6,
			description = "First z coordinate of crop")
	private Integer startZ = 0;
	
	@Parameter( label= "sizeZ", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=7,
			description = "Size in z direction")
	private Integer sizeZ = 1;
	
	@Parameter( label= "startT", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=6,
			description = "First t coordinate of crop")
	private Integer startT = 0;
	
	@Parameter( label= "sizeT", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=7,
			description = "Size in t direction")
	private Integer sizeT = 1;
	
	@Parameter( label= "startC", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=6,
			description = "First c coordinate of crop")
	private Integer startC= 0;
	
	@Parameter( label= "sizeC", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=7,
			description = "Size in c direction")
	private Integer sizeC = 1;
	
	/**
	 * Constructor. Use set-functions to specify parameters
	 */
	public CropImage() throws ALDOperatorException {
		super();
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
	}
	
	@Override
	protected void operate() {
		resultImg = inputImg.getImagePart( startX, startY, startZ, startT, startC, 
				sizeX, sizeY, sizeZ, sizeT, sizeC);
	}

}
