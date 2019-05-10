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

package de.unihalle.informatik.MiToBo.visualization.drawing;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import java.awt.Color;
import java.util.Random;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * A class to visualize 2D contours. 
 * <p>
 * If no input image is provided, a new image is generated with the background 
 * set to zero (black). The default drawing color is red. 
 *  
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class DrawContour2DSet extends MTBOperator {
	
	/**
	 * Set of contours of type {@link MTBContour2D} to draw.
	 */
	@Parameter( label= "inputContours", required = true, 
			direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Input contours.")
	private MTBContour2DSet inputConts = null;

	/**
	 * Input image into which the contours are to be drawn.
	 */
	@Parameter( label= "inputImage", required = false,
			direction=Direction.IN, dataIOOrder=2, mode=ExpertMode.STANDARD,
			description = "Optional input image.")
	private MTBImageRGB inputImage = null;

	/**
	 * Color to be used.
	 */
	@Parameter( label= "color", required = false,
			direction=Direction.IN, dataIOOrder=3, mode=ExpertMode.STANDARD,
			description = "Optional color.")
	private Color color = Color.red;
	
	/**
	 * Result image.
	 */
	@Parameter( label= "resultImage", required = true,
			direction=Direction.OUT, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Result image")
	private MTBImage resultImage = null;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawContour2DSet() throws ALDOperatorException {
		// nothing happens here
	}
	
	/**
	 * Simple constructor with contour set.
	 * @param cs 	Set of contours to draw.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawContour2DSet(MTBContour2DSet cs) throws ALDOperatorException {
		this.inputConts = cs;
	}
	
	/**
	 * Constructor with contour set and image.
	 * @param cs 	Set of contours to draw.
	 * @param img Target image to which the contours are to be drawn.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawContour2DSet(MTBContour2DSet cs, MTBImageRGB img) 
		throws ALDOperatorException {
		this.inputImage = img;
		this.inputConts = cs;
	}
	
	/**
	 * Set input image into which to draw the contours.
	 * @param img 	Input image.
	 */
	public void setInputImage(MTBImageRGB img) {
		this.inputImage = img;
	}

	/**
	 * Specify color in which to draw the contours.
	 * <p>
	 * If null, random colors are used.
	 * 
	 * @param c	Color to use. 
	 */
	public void setColor(Color c) {
		this.color = c;
	}

	/**
	 * Get the resulting region image.
	 * @return Result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImage;
	}
	
	@Override
	protected void operate() {
		MTBImageRGB dispImg= null; 
		if (this.inputImage == null) {
			dispImg= 
				(MTBImageRGB) MTBImage.createMTBImage((int)this.inputConts.getXMax()+1,
						(int)this.inputConts.getYMax()+1,	1, 1, 1, MTBImageType.MTB_RGB);
		}
		else
			dispImg = this.inputImage;

		Random rand = new Random();
		int red, green, blue;
		for (MTBContour2D c: this.inputConts) {
			// check for color
			Color col = this.color;
			if (col == null) {
				red = rand.nextInt(256);
				green = rand.nextInt(256);
				blue = rand.nextInt(256);
				col = new Color(red, green, blue);
			}
			c.drawContour(dispImg, col);
		}
		this.resultImage = dispImg;
	}
}
