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
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * A class to visualize 2D polygons. 
 * <p>
 * If no input image is provided, a new image is generated with the background 
 * set to zero (black). The default drawing color is red. 
 *  
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class DrawPolygon2DSet extends MTBOperator {
	
	/**
	 * Set of polygons of type {@link MTBPolygon2D} to draw.
	 */
	@Parameter( label= "inputPolygons", required = true, 
			direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Input polygons.")
	private MTBPolygon2DSet inputPolys = null;

	/**
	 * Input image into which the polygons are to be drawn.
	 */
	@Parameter( label= "inputImage", required = false,
			direction=Direction.IN, dataIOOrder=2, mode=ExpertMode.STANDARD,
			description = "Optional input image.")
	private MTBImageRGB inputImage = null;

	/**
	 * Color to be used, known colors are: red, green, blue, yellow, white
	 */
	@Parameter( label= "color", required = false,
			direction=Direction.IN, dataIOOrder=3, mode=ExpertMode.STANDARD,
			description = "Optional color.")
	private String color = "red";
	
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
	public DrawPolygon2DSet() throws ALDOperatorException {
		// nothing happens here
	}
	
	/**
	 * Simple constructor with polygon set.
	 * @param ps Set of polygons to draw.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawPolygon2DSet(MTBPolygon2DSet ps) throws ALDOperatorException {
		this.inputPolys = ps;
	}
	
	/**
	 * Constructor with polygon set and image.
	 * @param ps 	Set of polygons to draw.
	 * @param img Target image to which the polygons are to be drawn.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawPolygon2DSet(MTBPolygon2DSet ps, MTBImageRGB img) 
		throws ALDOperatorException {
		this.inputImage = img;
		this.inputPolys = ps;
	}
	
	/**
	 * Set input image into which to draw the polygons.
	 * @param img 	Input image.
	 */
	public void setInputImage(MTBImageRGB img) {
		this.inputImage = img;
	}

	/**
	 * Specify color in which to draw the snakes.
	 * <p>
	 * Known colors are: red, green, blue, yellow, white
	 * 
	 * @param c	Color to use. 
	 */
	public void setColor(String c) {
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
				(MTBImageRGB) MTBImage.createMTBImage((int)this.inputPolys.getXmax()+1,
						(int)this.inputPolys.getYmax()+1,	1, 1, 1, MTBImageType.MTB_RGB);
		}
		else
			dispImg = this.inputImage;
		// choose color, red is default
		int col = ((255&0xff)<<16) + ((0&0xff)<<8) + (0&0xff);
		if (this.color.equals("green"))
			col =  ((0&0xff)<<16) + ((255&0xff)<<8) + (0&0xff);
		if (this.color.equals("blue"))
			col =  ((0&0xff)<<16) + ((0&0xff)<<8) + (255&0xff);
		if (this.color.equals("yellow"))
			col =  ((255&0xff)<<16) + ((255&0xff)<<8) + (0&0xff);
		if (this.color.equals("white"))
			col =  ((255&0xff)<<16) + ((255&0xff)<<8) + (255&0xff);
		for (int i = 0; i< this.inputPolys.size(); ++i) {
			MTBPolygon2D poly = this.inputPolys.elementAt(i);
			poly.drawPolygon(dispImg,col);
		}
		this.resultImage = dispImg;
	}
}
