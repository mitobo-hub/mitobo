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

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator to draw an ellipse to a given image.
 * <p>
 * Note that this is a really brute-force implementation. Ellipses are
 * sampled only with 360 steps and no Bresenham algorithm or something
 * similar is applied. Also there are no special efforts to make the
 * drawing process efficient.
 * 
 * @author Birgit Moller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION)
public class DrawEllipse extends MTBOperator {
	
	/**
	 * Input image where to draw the ellipse into.
	 */
	@Parameter(label = "Input Image", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 0,
			description = "Input image where to draw ellipse.")
	private transient MTBImage inImg = null;
	
	/**
	 * Ellipse to draw.
	 */
	@Parameter(label = "Ellipse", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 1,
			description = "Ellipse to draw.")
	private MTBQuadraticCurve2D ellipse = null;
	
	/**
	 * Color or gray value to use.
	 */
	@Parameter(label = "Color", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 2,
			description = "Color or gray value to use.")
	private Color color = Color.WHITE;

	/**
	 * Result image.
	 * <p>
	 * This image is of the same type than the input image.
	 */
	@Parameter(label = "Result Image",  
			direction = Parameter.Direction.OUT, dataIOOrder = 0,
			description = "Output image where to draw ellipse.")
	private transient MTBImage outImg = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException 
	 * 		Thrown in case of initialization problems.
	 */
	public DrawEllipse() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Specify input image.
	 * @param img	Image where to draw into.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Specify ellipse to draw.
	 * <p>
	 * Note that you have to make sure that the quadratic curve refers to
	 * a valid, non-degenerate ellipse.
	 * 
	 * @param e	Ellipse to draw.
	 */
	public void setEllipse(MTBQuadraticCurve2D e) {
		this.ellipse = e;
	}
	
	/**
	 * Specify color to use.
	 * @param c	Color to use for drawing.
	 */
	public void setColor(Color c) {
		this.color = c;
	}
	
	/**
	 * Get result image.
	 * @return	Image with drawn ellipse.
	 */
	public MTBImage getResultImage() {
		return this.outImg;
	}
	
	@Override
	public void operate() {
		this.outImg = this.inImg.duplicate();
		this.drawEllipse();
	}
	
	/**
	 * Draw ellipse to image.
	 */
	private void drawEllipse() {
		// get ellipse parameters
		double major = this.ellipse.getSemiLengthAxisA();
		double minor = this.ellipse.getSemiLengthAxisB();
		double xCenter = this.ellipse.getCenterX();
		double yCenter = this.ellipse.getCenterY();
		double theta = this.ellipse.getOrientation();

		int iRed = this.color.getRed();
		int iGreen = this.color.getGreen();
		int iBlue = this.color.getBlue();

		int iColor = 
				((iRed & 0xff)<<16) + ((iGreen & 0xff)<<8) + ((iBlue & 0xff));

		// convert angle from degrees to radiant
		double trad = Math.PI/180.0*theta;
		for (int i=0;i<360; ++i) {
			double rad = Math.PI/180.0*i;
			double x = major * Math.cos(rad);
			double y = minor * Math.sin(rad);
			// rotate ellipse
			int rx = (int)(Math.cos(trad)*x - Math.sin(trad)*y + xCenter);
			int ry = (int)(Math.sin(trad)*x + Math.cos(trad)*y + yCenter);
			if (   rx>=0 && rx<this.outImg.getSizeX() 
					&& ry>=0 && ry<this.outImg.getSizeY())
			this.outImg.putValueInt(rx, ry, iColor);
		}
	}
}
