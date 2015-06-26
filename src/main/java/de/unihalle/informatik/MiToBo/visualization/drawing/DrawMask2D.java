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
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Operator for drawing a mask into an image.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
	level = Level.APPLICATION)
public class DrawMask2D extends MTBOperator {

	/**
	 * Input image.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Direction.IN, description = "Input image.")
	private transient MTBImage inImg = null;

	/**
	 * Mask.
	 */
	@Parameter( label= "Mask", required = true, dataIOOrder = 1,
			direction = Direction.IN, description = "Mask.")
	private transient MTBImageByte mask = null;

	/**
	 * Color for mask, default is white.
	 * <p>
	 * To specify the color you have to provide an integer value where the R
	 * value is encoded	in the first 8 bits, the G value in the second 8 bits and
	 * the B value in the last 8 bits. 
	 */
	@Parameter( label= "Color", required= false, dataIOOrder = 2,
			direction = Direction.IN, description = "Color to be used.")
	private int color = ((255&0xff) << 16) + ((0&0xff) << 8) + (0&0xff);                        
		
	/**
	 * Flag to request a RGB image in any case.
	 */
	@Parameter( label= "Request RGB image", required= false, dataIOOrder = 3,
			direction = Direction.IN, description = "Flag to request RGB image.")
	private boolean getRGBImage = false;
	
	/**
	 * Result image.
	 */
	@Parameter( label= "Result Image",
			direction = Direction.OUT, description = "Resulting (color) image.")
	private transient MTBImage resultImg = null;
	
	/**
	 * Default constructor.
	 */
	public DrawMask2D() throws ALDOperatorException {
		// nothing to be done here
	}
	
	/**
	 * Constructor.
	 * 
	 * @param img		Input image.
	 * @param msk		Structure mask.
	 * @throws ALDOperatorException 
	 */
	public DrawMask2D(MTBImage img, MTBImageByte msk) 	
		throws ALDOperatorException {
		this.inImg = img;
		this.mask = msk;
	}
	
	/**
	 * Specify color as integer value, right 24 bits are interpreted as R,G,B.
	 */
	public void setColor(int c) {
		this.color = c;
	}
	
	/**
	 * Returns the result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
	
	@Override
  protected void operate() {
		
		int sizeX= this.inImg.getSizeX();
		int sizeY= this.inImg.getSizeY();
		
		// extract color components
		int red= (this.color & 0xff0000)>>16;
		int green= (this.color & 0x00ff00)>>8;
		int blue= (this.color & 0x0000ff);
				
		// if input image has more than 8-bit depth, init multi-channel image,
		// otherwise return RGB image
		MTBImage.MTBImageType inType= this.inImg.getType();
		if ((   inType == MTBImage.MTBImageType.MTB_DOUBLE
				 || inType == MTBImage.MTBImageType.MTB_FLOAT
				 || inType == MTBImage.MTBImageType.MTB_SHORT)
				 && !this.getRGBImage) {
			this.resultImg= MTBImage.createMTBImage(sizeX,sizeY,1,1,3,inType);
			switch(this.inImg.getSizeC())
			{
			case 2:
			{
				if (this.verbose.booleanValue())
					System.out.println("DrawStructures - 2 channel image, ignoring " +
							"second one...");
			}
			//$FALL-THROUGH$
			case 1:
			{
				for (int y=0;y<sizeY;++y) {
					for (int x=0;x<sizeX;++x) {
						if (this.mask.getValueInt(x, y) == 0) {
							this.resultImg.putValueInt(x,y,0,0,0,
									this.inImg.getValueInt(x,y));
							this.resultImg.putValueInt(x,y,0,0,1,
									this.inImg.getValueInt(x,y));
							this.resultImg.putValueInt(x,y,0,0,2,
									this.inImg.getValueInt(x,y));
						}
						else {
							this.resultImg.putValueInt(x,y,0,0,0,red);
							this.resultImg.putValueInt(x,y,0,0,1,green);
							this.resultImg.putValueInt(x,y,0,0,2,blue);
						}
					}	
				}
				break;
			}
			case 3:
			{
				for (int y=0;y<sizeY;++y) {
					for (int x=0;x<sizeX;++x) {
						if (this.mask.getValueInt(x, y) == 0) {
							this.resultImg.putValueInt(x,y,0,0,0,
									this.inImg.getValueInt(x,y,0,0,0));
							this.resultImg.putValueInt(x,y,0,0,1,
									this.inImg.getValueInt(x,y,0,0,1));
							this.resultImg.putValueInt(x,y,0,0,2,
									this.inImg.getValueInt(x,y,0,0,2));
						}
						else {
							this.resultImg.putValueInt(x,y,0,0,0,red);
							this.resultImg.putValueInt(x,y,0,0,1,green);
							this.resultImg.putValueInt(x,y,0,0,2,blue);
						}
					}	
				}
				break;
			}
			}
		}
		// RGB image requested, convert input image accordingly
		else if (   inType == MTBImage.MTBImageType.MTB_DOUBLE
				 			|| inType == MTBImage.MTBImageType.MTB_FLOAT
				 			|| inType == MTBImage.MTBImageType.MTB_SHORT) {
			MTBImageRGB outImg= (MTBImageRGB)MTBImage.createMTBImage(
					sizeX,sizeY,1,1,1, MTBImageType.MTB_RGB);
			int maxVal = this.inImg.getMinMaxInt()[1];
			for (int y=0;y<this.inImg.getSizeY();++y) {
				for (int x=0;x<this.inImg.getSizeX();++x) {
					if (this.mask.getValueInt(x, y) > 0) {
						outImg.putValueR(x, y, red);
						outImg.putValueG(x, y, green);
						outImg.putValueB(x, y, blue	);
					}
					else {
						int value = this.inImg.getValueInt(x, y);
						outImg.putValueR(x, y, (int)(255.0/maxVal*value));
						outImg.putValueG(x, y, (int)(255.0/maxVal*value));
						outImg.putValueB(x, y, (int)(255.0/maxVal*value));
					}							
				}
			}
			this.resultImg = outImg;
		}
		// input image is of type byte or long
		else if (inType != MTBImage.MTBImageType.MTB_RGB) {
			// init RGB image
			if (this.verbose.booleanValue())
				System.out.println("Initializing RGB image...");
			this.resultImg= MTBImage.createMTBImage(sizeX,sizeY,1,1,1,
					MTBImageType.MTB_RGB);
			MTBImageRGB resultRef = (MTBImageRGB)this.resultImg;
			// initialize result image as a copy of the input image
			for (int y=0;y<sizeY;++y) {
				for (int x=0;x<sizeX;++x) {
					if (this.mask.getValueInt(x, y) == 0) {
						resultRef.putValueR(x, y, this.inImg.getValueInt(x, y));
						resultRef.putValueG(x, y, this.inImg.getValueInt(x, y));
						resultRef.putValueB(x, y, this.inImg.getValueInt(x, y));
					}
					else {
						resultRef.putValueR(x, y, red);
						resultRef.putValueG(x, y, green);
						resultRef.putValueB(x, y, blue);
					}
				}
			}
		}
		// input image is already RGB
		else {
			this.resultImg = this.inImg.duplicate(this);
			MTBImageRGB resultRef = (MTBImageRGB)this.resultImg;
			for (int y=0;y<sizeY;++y) {
				for (int x=0;x<sizeX;++x) {
					if (this.mask.getValueInt(x, y) != 0) {
						resultRef.putValueR(x, y, red);
						resultRef.putValueG(x, y, green);
						resultRef.putValueB(x, y, blue);
					}
				}
			}
		}
	}	
}
