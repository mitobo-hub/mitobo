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

import java.awt.Color;
import java.awt.geom.Point2D;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Operator to draw a given string into a given image.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD)
public class DrawStringToImage extends MTBOperator
{
	/**
	 * Input image to which the string is to be drawn.
	 */
	@Parameter(label = "Input Image", required = true, 
		direction = Parameter.Direction.IN, 
		description = "Input image.", dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	/**
	 * String to draw into the image.
	 */
	@Parameter(label = "String", required = true, 
		direction = Parameter.Direction.IN, 
		description = "String to draw.", dataIOOrder = 1)
	private String text = null;

	/**
	 * Position where to draw the string.
	 */
	@Parameter(label = "Position", required = true, 
		direction = Parameter.Direction.IN, 
		description = "Position where to draw the string.", dataIOOrder = 2)
	private Point2D.Double position = new Point2D.Double(0,0);
	
	/**
	 * Color to use.
	 */
	@Parameter(label = "Color", required = false, 
		direction = Parameter.Direction.IN, 
		description = "Optional color for text.", dataIOOrder = 0)
	private Color color = Color.white;
	
	/**
	 * Result image.
	 */
	@Parameter(label = "Output Image", direction = Parameter.Direction.OUT, 
		description = "Result image.", dataIOOrder = 0)
	private transient MTBImage resultImg = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public DrawStringToImage() throws ALDOperatorException {
		// nothing to do here
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (   this.position.x < 0 || this.position.x >= this.inImg.getSizeX()
				|| this.position.y < 0 || this.position.y >= this.inImg.getSizeY()) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[DrawStringToImage] text position outside of image domain!");
		}
	}
	
	@Override
	public void operate() {
		// extract ImageJ ImagePlus
		ImageProcessor ip= this.inImg.getImagePlus().getProcessor();
		ip.moveTo((int)(this.position.x + 0.5), (int)(this.position.y + 0.5));
		ip.setColor(this.color);
		ip.drawString(this.text);
		// create result image
		ImagePlus resultImage = 
			new ImagePlus("Image <" + this.inImg.getTitle() + "> with string", ip);
		this.resultImg = MTBImage.createMTBImage(resultImage);
		this.resultImg.updateAndRepaintWindow();
	}
	
	/**
	 * Specify input image.
	 * @param img	Input image.
	 */
	public void setInImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Specify string.
	 * @param t	String to draw.
	 */
	public void setString(String t) {
		this.text = t;
	}
	
	/**
	 * Specify color.
	 * @param c	Color to use.
	 */
	public void setColor(Color c) {
		this.color = c;
	}
	
}
