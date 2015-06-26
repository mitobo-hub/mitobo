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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer;

/**
 * Generic super class for level set solvers.
 * 
 * @author Birgit Moeller
 */
public abstract class LevelsetSolver extends MTBOperator {

  /**
   * Image to segment.
   */
  @Parameter(label = "Input Image", required = true, dataIOOrder = -10, 
  		direction = Parameter.Direction.IN, description = "Image to segment.")
  protected transient MTBImage inputImg = null;

  /**
   * Operator to generate initialization for the level set function.
   */
  @Parameter(label = "Initializer", required = true, dataIOOrder = -9,
  		direction = Parameter.Direction.IN, 
  		description = "Segmentation initializer.")
  protected SegmentationInitializer initGenerator = null;
	
  /**
   * Initial segmentation for initializing level set function.
   */
  protected MTBSegmentationInterface initialSegmentation;
  
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected LevelsetSolver() throws ALDOperatorException {
	  super();
  }
	
	/**
	 * Runs initializer to calculate initial segmentation.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	protected void initSegmentation() 
			throws ALDOperatorException, ALDProcessingDAGException {
		this.initGenerator.runOp(HidingMode.HIDDEN);
		this.initialSegmentation = this.initGenerator.getSegmentation();
	}
	
  /**
   * Get input image.
   * @return Input image.
   */
	public MTBImage getInputImg() {
		return this.inputImg;
	}
	
	/**
	 * Get initial segmentation.
	 * @return	Initial segmentation object, might be null.
	 */
	public MTBSegmentationInterface getInitialSegmentation() {
		return this.initialSegmentation;
	}
}
