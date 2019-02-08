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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.segmentation;

import java.util.Map;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperatorCollectionElement;
import ij.ImagePlus;

/**
 * 
 * @author schreck
 */
public abstract class RootSegmentationOperator 
	extends ALDOperatorCollectionElement 
{
   /**
    * Input image.
    */
   protected ImagePlus image;
	
   /**
   	* Result data structure: list of point pairs
   	*/
   @Parameter(label = "result data structure", 
  		required = true, 
  		direction = Parameter.Direction.OUT,
  		dataIOOrder = 1, 
  		mode = ExpertMode.STANDARD,
  		description = "Result list of point pairs")
    protected Map<Integer, Map<Integer, Node>> resultLineMap;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public RootSegmentationOperator() throws ALDOperatorException 
	{
		super();
	}

	/**
	 * Setter for the input image.
	 * @param img - ImagePlus
	 */
	public void setImage(ImagePlus img)
	{
		this.image = img;
	}
	
	/**
	 * Getter for the input image.
	 * @return the image as an ImagePlus
	 */
	public ImagePlus getImage()
	{
		return this.image;
	}
	
}
