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
import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * A class to visualize 2D regions. 
 * <p>
 * Background is always 0, regions can be drawn in different ways.
 * The following kinds of region images can be created or drawn to an 
 * existing image:
 * <ul>
 * <li>LABEL_IMAGE:<br>
 * 	draw gray value image where regions are labeled by their index in 
 * 	the set plus one
 * <li>ID_IMAGE:<br>
 * 	draw gray value image where regions are labeled by their region id 
 * 	(see {@link MTBRegion2D} for details)
 * <li>MASK_IMAGE:<br>
 * 	draw a mask image with equal gray values for all regions
 * <li>COLOR_IMAGE:<br>
 * 	draw a mask with colored regions
 * <li>TRANSPARENT_IMAGE:<br>
 * 	draw a mask with colored regions, but the regions are transparent 
 * 	to show the underlying intensity structure of the given target 
 * 	image
 * <li>CONTOURS:<br>
 * 	draw only contours of the regions in a given colour
 * </ul>
 * <p>
 * Constructors take the kind of image and the regions to be drawn as 
 * well as an eventual target image. Default configurations are set by 
 * the constructors. Use the different get/set methods to specify
 * non default parameters like color, gray value etc.
 * <p>
 * Be sure to set the xMin, xMax, yMin, yMax values of the input 
 * {@link MTBRegion2DSet} correctly, because these values are
 * used to determine the output image size if no target image is specified
 * nor dimensions are given explicitly!
 * 
 * @author Oliver Gress
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class DrawRegion2DSet extends MTBOperator {
	
	/**
	 * Set of regions to draw.
	 */
	@Parameter( label= "Input Regions", required = true, 
			direction = Direction.IN, dataIOOrder = 0, 
			mode = ExpertMode.STANDARD, description = "Regions to draw.")
	private transient MTBRegion2DSet inputRegions = null;

	/**
	 * Type of image to be created.
	 */
	@Parameter( label= "Draw Type", required = true, 
			direction = Direction.IN, dataIOOrder = 1, 
			mode = ExpertMode.STANDARD, paramModificationMode = 
				ParameterModificationMode.MODIFIES_INTERFACE,
			callback = "updateImageAndColorSettings",
			description = "The type of image to be drawn.")
	private DrawType drawType = DrawType.LABEL_IMAGE;

	/**
	 * Desired type of output image.
	 */
	@Parameter( label= "Type of Output Image", required = true,  
			direction = Direction.IN, dataIOOrder = 2,
			mode = ExpertMode.STANDARD,
			description = "The data type of the output image.")
	private MTBImageType imageType = null;

	/**
	 * Gray value used, e.g., for mask or contour images.
	 */
	@Parameter( label= "Gray Value", required = false, 
			direction = Direction.IN, dataIOOrder = 0, 
			mode = ExpertMode.STANDARD, 
			description = "Gray value used to draw all regions or contours.")
	private Double grayValue = null;

	/**
	 * Color value used, e.g., for color and transparent images.
	 */
	@Parameter( label= "Color Value", required = false,  
			direction = Direction.IN, dataIOOrder = 1,
			mode = ExpertMode.STANDARD, paramModificationMode = 
				ParameterModificationMode.MODIFIES_INTERFACE,
			callback = "synchronizeRandomOptions",
			description = "A color used to paint all regions or contours.")
	private Color color = null;

	/**
	 * Random generator for color sampling.
	 */
	@Parameter( label= "Random Generator", required = false, 
			direction = Direction.IN, dataIOOrder = 2, 
			mode = ExpertMode.STANDARD, paramModificationMode = 
				ParameterModificationMode.MODIFIES_INTERFACE,
			callback = "synchronizeColorOptions",
			description = "Random generator for sampling pseudo-colors.")
	private Random random = null;

	/**
	 * Optional target image.
	 */
	@Parameter( label = "Target Image", required = false, 
			direction = Direction.IN, dataIOOrder = 3, 
			mode = ExpertMode.STANDARD, paramModificationMode = 
				ParameterModificationMode.MODIFIES_INTERFACE,
			callback = "updateTargetImageDependencies",
			description = "Image where to draw the regions into.")
	private transient MTBImage targetImage = null;

	/**
	 * Flag to enable/disable cloning of target image.
	 */
	@Parameter( label="   - Clone Target Image?", required = false, 
			direction = Direction.IN, dataIOOrder = 4,
			mode = ExpertMode.STANDARD,
			description = "Select if the target image should be cloned.")
	private boolean cloneTargetImage = true;
	
	/**
	 * Desired width of result image.
	 * <p>
	 * If this parameter and {@link #resultImgHeight} are set to something 
	 * different from -1 they will overwrite all other specifications.
	 */
	@Parameter( label = "Result Image Width", required = false, 
		direction = Direction.IN, dataIOOrder = 5, 
		mode = ExpertMode.ADVANCED, description = "Width of target image.")
	private int resultImgWidth = -1;
	
	/**
	 * Desired height of result image.
	 * <p>
	 * If this parameter and {@link #resultImgWidth} are set to something 
	 * different from -1 they will overwrite all other specifications.
	 */
	@Parameter( label = "Result Image Height", required = false, 
		direction = Direction.IN, dataIOOrder = 6, 
		mode = ExpertMode.ADVANCED, description = "Height of target image.")
	private int resultImgHeight = -1;
	
	/**
	 * Desired offset in x of result image.
	 * <p>
	 * If {@link #resultImgHeight} and {@link #resultImgWidth} are set to values
	 * different from -1 this parameter will be used and overwrite all other 
	 * specifications.
	 */
	@Parameter( label = "Result Image Offset X", required = false, 
		direction = Direction.IN, dataIOOrder = 7, 
		mode = ExpertMode.ADVANCED, description = "Offset in x of result image.")
	private int resultImgOffsetX = 0;

	/**
	 * Desired offset in y of result image.
	 * <p>
	 * If {@link #resultImgHeight} and {@link #resultImgWidth} are set to values
	 * different from -1 this parameter will be used and overwrite all other 
	 * specifications.
	 */
	@Parameter( label = "Result Image Offset Y", required = false, 
		direction = Direction.IN, dataIOOrder = 8, 
		mode = ExpertMode.ADVANCED, description = "Offset in y of result image.")
	private int resultImgOffsetY = 0;

	/**
	 * Output image.
	 */
	@Parameter( label= "Result Image", direction = Direction.OUT, 
			mode = ExpertMode.STANDARD, dataIOOrder = 0,
			description = "Result image.")
	private transient MTBImage resultImage = null;
	
	/**
	 * Type of image to be drawn.
	 */
	public enum DrawType {
		/**
		 * Draw gray value image where regions are labeled by their index 
		 * in the given set plus one.
		 * 
		 */
		LABEL_IMAGE, 
		/**
		 * Draw gray value image where regions are labeled by their region 
		 * ID (see {@link MTBRegion2D}).
		 * 
		 */
		ID_IMAGE, 
		/**
		 * Draw a mask image with identical gray values for all regions.
		 */
		MASK_IMAGE, 
		/**
		 * Draw a mask with colored regions.
		 * 
		 */
		COLOR_IMAGE, 
		/**
		 * Draw a mask with colored regions, but the regions are 
		 * transparently overlaid over the target image to show the 
		 * underlying intensity structure.
		 * 
		 */
		TRANSPARENT_IMAGE, 
		/**
		 * Draw contours of the regions in a given colour or gray value.
		 */
		CONTOURS
	}
	
	/**
	 * Default constructor where no parameters are set. 
	 * <p>
	 * Don't use this for region image creation.
	 * @throws ALDOperatorException Thrown if instantiation fails.
	 */
	public DrawRegion2DSet() throws ALDOperatorException {
		this.removeParameter("cloneTargetImage");
		this.initOperator();
	}
	
	/**
	 * Simple constructor to create the most common region image types.
	 * <p>
	 * See {@link #drawType} for details.
	 * 
	 * @param dtype 	Drawing type to be used.
	 * @param regions Set of regions to be drawn.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public DrawRegion2DSet(DrawType dtype, MTBRegion2DSet regions) 
			throws ALDOperatorException {
		this.removeParameter("cloneTargetImage");
		this.setInputRegions(regions);
		this.setDrawType(dtype);
		this.initOperator();
	}
	
	/**
	 * Simple constructor to draw the most common region image types to 
	 * a given image. 
	 * 
	 * @param dtype 			Drawing type to apply.
	 * @param regions 		Set of regions to draw.
	 * @param tImage 			Target image to draw the regions to.
	 * @param cloneTImage	Flag to clone the target image.
	 * @throws ALDOperatorException Thrown in case of processing failure.
	 */
	public DrawRegion2DSet(DrawType dtype, MTBRegion2DSet regions, 
			MTBImage tImage, boolean cloneTImage) throws ALDOperatorException {
		if (tImage != null)
			this.removeParameter("cloneTargetImage");
		this.setInputRegions(regions);
		this.setDrawType(dtype);
		this.setTargetImage(tImage);
		this.setCloneTargetImage(cloneTImage);
		this.initOperator();
	}

	/**
	 * Initial setip of the operator.
	 */
	private void initOperator() {
		if (   this.drawType == DrawType.COLOR_IMAGE 
				|| this.drawType == DrawType.TRANSPARENT_IMAGE) {
			this.setImageType(MTBImageType.MTB_RGB);
			this.setRandom(new Random(0));
		}
		else if (this.drawType == DrawType.MASK_IMAGE) {
			this.setImageType(MTBImageType.MTB_BYTE);
			this.setGrayValue(new Double(255.0));
		}
		else if (this.drawType == DrawType.CONTOURS) {
			if (this.targetImage != null) {
				this.setImageType(this.targetImage.getType());
			}
			else {
				this.setImageType(MTBImageType.MTB_INT);
			}
			this.setGrayValue(new Double(255.0));
		} else {
			this.setImageType(MTBImageType.MTB_INT);
		}
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		
		if (this.getDrawType() == DrawType.LABEL_IMAGE) {
			if (this.getImageType() == MTBImageType.MTB_RGB) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
								"DrawRegion2DSet.validateCustom(): Label image cannot be drawn to a RGB image.");
			}
			
		}
		else if (this.getDrawType() == DrawType.ID_IMAGE) {
			if (this.getImageType() == MTBImageType.MTB_RGB) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
								"DrawRegion2DSet.validateCustom(): ID image cannot be drawn to a RGB image.");
			}
			else if (this.getImageType() == MTBImageType.MTB_BYTE
					|| this.getImageType() == MTBImageType.MTB_SHORT) {
				System.out.println("Warning: DrawRegion2DSet.validateCustom(): ID image of regions is drawn to " +
						" a MTB_BYTE or MTB_SHORT image. IDs are integer values, possible loss of information.");
			}	
		}
		else if (this.getDrawType() == DrawType.COLOR_IMAGE) {
			if (this.getImageType() == MTBImageType.MTB_RGB) {

				if (!(this.getColor() == null ^ this.getRandom() == null))
					throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
								"DrawRegion2DSet.validateCustom(): For color region images, a color or a" +
								" a random number generator (random colors) must be specified (exclusively).");
			}
			else {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						"DrawRegion2DSet.validateCustom(): For color region image, the output type (probably the target) image must be MTB_RGB.");
			}	
		}
		else if (this.getDrawType() == DrawType.TRANSPARENT_IMAGE) {
			if (this.getTargetImage() == null) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"DrawRegion2DSet.validateCustom(): For transparent color region image, you must specify a RGB target image");	
			}
			
			if (this.getImageType() == MTBImageType.MTB_RGB) {

				if (!(this.getColor() == null ^ this.getRandom() == null))
					throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
								"DrawRegion2DSet.validateCustom(): For transparent color region images, a color or a" +
								" a random number generator (random colors) must be specified (exclusively).");
			}
			else {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						"DrawRegion2DSet.validateCustom(): For transparent color region image, the output type (probably the target) image must be MTB_RGB.");
			}	
		}
		else if (this.getDrawType() == DrawType.MASK_IMAGE) {
			if (this.getGrayValue() == null) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						"DrawRegion2DSet.validateCustom(): A gray value must be specified to draw a mask image.");
			}
		}
		else if (this.getDrawType() == DrawType.CONTOURS) {
			// TODO
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"DrawRegion2DSet.validateCustom(): Unknown DrawType: " + this.getDrawType());
		}
		
	}
	
	/**
	 * Set the input regions to be drawn.
	 * @param regions Regions to draw.
	 */
	public void setInputRegions(MTBRegion2DSet regions) {
		try {
	    this.setParameter("inputRegions", regions);
    } catch (ALDOperatorException e) {
    	this.inputRegions = regions;
    }
	}
	
	/**
	 * Get the regions that have to be drawn
	 * @return set of regions
	 */
	public MTBRegion2DSet getInputRegions() {
		return this.inputRegions;
	}	
	
	/**
	 * Specify a target image to which the regions are drawn.
	 * <p>
	 * If image is <code>null</code> a new image is created.
	 * @param targetimage Target image.
	 */
	public void setTargetImage(MTBImage targetimage) {
		try {
	    this.setParameter("targetImage", targetimage);
    } catch (ALDOperatorException e) {
  		this.targetImage = targetimage;
  		if (this.targetImage != null)
  			this.imageType = targetimage.getType();
    }
	}
	
	/**
	 * Get the target image to which the regions are drawn, if one was specified. 
	 * @return target image or null, if none was specified and the operator creates a new image
	 */
	public MTBImage getTargetImage() {
		return this.targetImage;
	}
	
	/**
	 * Enable or disable cloning of target image.
	 * <p>
	 * Enabling cloning of the image requires a target image to be 
	 * available.
	 * 
	 * @param flag	If true cloning of target image is enabled.
	 */
	public void setCloneTargetImage(boolean flag) {
		this.cloneTargetImage = flag;		
	}
	
	/**
	 * Set the kind of region image to be drawn
	 * @param dtype see DrawType
	 */
	public void setDrawType(DrawType dtype) {
		try {
	    this.setParameter("drawType", dtype);
    } catch (ALDOperatorException e) {
  		this.drawType = dtype;
    }
	}
	
	/**
	 * Get the kind of region image drawn by the operator
	 * @return Draw type currently set.
	 */
	public DrawType getDrawType() {
		return this.drawType;
	}	
	
	/**
	 * Set the resulting image's datatype. 
	 * <p>
	 * If a target image was specified and the given type differs from the 
	 * target image's type, the target image is set to null.
	 * In this case a new image is created.
	 * @param type Desired type of result image.
	 */
	public void setImageType(MTBImageType type) {
		if (   this.getTargetImage() != null 
				&& this.getTargetImage().getType() != type) {
			try {
	      this.setParameter("targetImage", null);
      } catch (ALDOperatorException e) {
      	this.targetImage = null;
      }
		}
		try {
	    this.setParameter("imageType", type);
    } catch (ALDOperatorException e) {
    	this.imageType = type;
    }
	}
	
	/**
	 * Get the datatype of the resulting image.
	 * @return Data type of result image.
	 */
	public MTBImageType getImageType() {
		return this.imageType;
	}	
	
	/**
	 * Set the gray value of the regions in a mask image.
	 * <p>
	 * This setting is only active if a mask image is to be created.
	 * Be aware of the resulting image's datatype!
	 * @param value Gray value to use.
	 */
	public void setGrayValue(Double value) {
		this.grayValue = value;
	}
	
	/**
	 * Get the gray value of the regions in a mask image (MASK_IMAGE only).
	 * @return Gray value or null, if none was specified
	 */
	public Double getGrayValue() {
		return this.grayValue;
	}
	
	/**
	 * Set the uniform color of the regions in a color image.
	 * <p>
	 * This setting is only active if a color image is to be created.
	 * @param c Color to be applied.
	 */
	public void setColor(Color c) {
		if (c == null)
			return;
		try {
	    this.setParameter("color", c);
    } catch (ALDOperatorException e) {
  		this.color = c;
    }
	}
	
	/**
	 * Get the uniform color of the regions in a color image (COLOR_IMAGE only).
	 * @return Color or null, if none was specified.
	 */
	public Color getColor() {
		return this.color;
	}
	
	/**
	 * Set a random number generator to draw each region in a random color.
	 * <p>
	 * This setting is only active if a color image is to be created.
	 * @param r	Random generator to be applied. 
	 */
	public void setRandom(Random r) {
		if (r == null)
			return;
		try {
	    this.setParameter("random", r);
    } catch (ALDOperatorException e) {
  		this.random = r;
    }
	}
	
	/**
	 * Get the random number generator which is responsable to draw each region in a random color (COLOR_IMAGE only).
	 * @return Random number generator or null, if none was specified
	 */
	public Random getRandom() {
		return this.random;
	}
	
	/**
	 * Set the resulting image.
	 * @param image Result image.
	 */
	private void setResultImage(MTBImage image) {
		this.resultImage = image;
	}
	
	/**
	 * Get the resulting region image. 
	 * <p>
	 * If a target image ({@link #targetImage}) was specified, this is the 
	 * same object if cloning was not selected.
	 * @return Result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImage;
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {

		MTBRegion2DSet regs = this.getInputRegions();
		
		DrawType dtype = this.getDrawType();
		MTBImage outImg = null;
		int x,y;
		int sizeX, sizeY;
		
		// offset of the output image coordinate system in x, i.e.,
		// x-coordinate value of the first pixel 
		int offsetX = 0;
		// offset of the output image coordinate system in y, i.e.,
		// y-coordinate value of the first pixel 
		int offsetY = 0;
		
		if (this.resultImgWidth > -1 && this.resultImgHeight > -1) {
			sizeX = this.resultImgWidth;
			sizeY = this.resultImgHeight;
			offsetX = this.resultImgOffsetX;
			offsetY = this.resultImgOffsetY;
			outImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, 
					this.getImageType());
		}
		else if (this.getTargetImage() != null) {
			sizeX = this.getTargetImage().getSizeX();
			sizeY = this.getTargetImage().getSizeY();
			if (this.cloneTargetImage)
				if ( this.getTargetImage().getType() == this.getImageType() )
				outImg = this.getTargetImage().duplicate();
				else 
					outImg = this.getTargetImage().convertType(this.getImageType(), 
							true);
			else
				outImg = this.getTargetImage();

		} else {
			sizeX = (int)Math.ceil(regs.getXmax() - regs.getXmin()) + 1;
			sizeY = (int)Math.ceil(regs.getYmax() - regs.getYmin()) + 1;
			offsetX = (int)Math.floor(regs.getXmin());
			offsetY = (int)Math.floor(regs.getYmin());
			outImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, 
					this.getImageType());
		}
		
		if (dtype == DrawType.COLOR_IMAGE) {
		
			Color col = this.getColor();
			Random rand = this.getRandom();

			int c = 255;
			// if output has one color for all regions
			if (col != null)
				c = this.color2int(col);
				
			for (int i = 0; i < regs.size(); i++) {
				Vector<Point2D.Double> pts = regs.get(i).getPoints();
				
				// if output has random colors for the different regions
				if (rand != null) {
					c = this.randomColor(rand);
				}
				
				for (int j = 0; j < pts.size(); j++) {
					x = (int)pts.get(j).getX() - offsetX;
					y = (int)pts.get(j).getY() - offsetY;
					
					if (x >= 0 && x < sizeX && y >= 0 && y < sizeY) {
						
						outImg.putValueInt(x, y, c);
					}
				}	
			}
		} else if (dtype == DrawType.TRANSPARENT_IMAGE) {
			
			Color col = this.getColor();
			Random rand = this.getRandom();
			Color color2;
			
			int c = 255;
			double val;
			// if output has one color for all regions
				
			for (int i = 0; i < regs.size(); i++) {
				Vector<Point2D.Double> pts = regs.get(i).getPoints();
				
				// if output has random colors for the different regions
				if (rand != null) {
					col = this.int2Color(this.randomColor(rand));
				}
				
				for (int j = 0; j < pts.size(); j++) {
					x = (int)pts.get(j).getX() - offsetX;
					y = (int)pts.get(j).getY() - offsetY;
					
					if (x >= 0 && x < sizeX && y >= 0 && y < sizeY) {
						color2 = this.int2Color(outImg.getValueInt(x, y));
						val = (color2.getRed() + color2.getGreen() + color2.getBlue())/(3.0*255.0);
						c = this.color2int(new Color(
								(int)Math.round(col.getRed()*val),
								(int)Math.round(col.getGreen()*val),
								(int)Math.round(col.getBlue()*val)));
						outImg.putValueInt(x, y, c);
					}
				}	
			}
		} else if ( dtype == DrawType.CONTOURS ) {
			double ccolor;
			for (int i = 0; i < regs.size(); i++) {
				MTBContour2D contour = regs.get(i).getContour();
				// set color of contour
				if (   outImg.getType() == MTBImageType.MTB_RGB 
						&& this.getColor() != null) {
					ccolor = this.color2int( this.getColor());
				} else if (   outImg.getType() == MTBImageType.MTB_RGB 
						       && this.getRandom() != null) {
					ccolor = this.randomColor(this.getRandom());
				}	else if ( this.getGrayValue() != null){
					ccolor = this.getGrayValue().doubleValue();
				} else {
					ccolor = 255;
				}
				for ( Point2D.Double p : contour.getPoints() ) {
					x = (int)p.getX() - offsetX;
					y = (int)p.getY() - offsetY;
					if (x >= 0 && x < sizeX && y >= 0 && y < sizeY) {
						outImg.putValueDouble(x, y, ccolor);
					}
				}
					
			}
			
		} else {

			double v = 0.0;
			// if output has one color for all regions
			if (this.getGrayValue() != null)
				v = this.getGrayValue().doubleValue();
			
			for (int i = 0; i < regs.size(); i++) {
				Vector<Point2D.Double> pts = regs.get(i).getPoints();
				
				// if output has random colors for the different regions
				if (dtype == DrawType.LABEL_IMAGE) {
					v += 1.0;
					if (v > outImg.getTypeMax()) {
						v = 1.0;
						System.err.println("Warning: Region label exceeds maximum supported pixel value. Going on resetting label to 1.");
					}
				}
				else if (dtype == DrawType.ID_IMAGE) {
					v = regs.get(i).getID();
					if (v == 0.0) {
						System.err.println("Warning: Drawing region with ID 0 as ID_IMAGE. Same pixel value as default background.");
					}
				}
				
				for (int j = 0; j < pts.size(); j++) {
					x = (int)pts.get(j).getX() - offsetX;
					y = (int)pts.get(j).getY() - offsetY;
					
					if (x >= 0 && x < sizeX && y >= 0 && y < sizeY) {
						outImg.putValueDouble(x, y, v);
					}
				}	
			}
		}
		
		this.setResultImage(outImg);
	}

	/**
	 * Generates the next random color.
	 * @param r	Random generator to be used.
	 * @return	Integer value of new color.
	 */
	protected int randomColor(Random r) {
		return (((((int)(r.nextDouble()*200.0 + 55)) & 0xff) << 16) 
				+ ((((int)(r.nextDouble()*200.0 + 55)) & 0xff) << 8) 
				+ (((int)(r.nextDouble()*200.0 + 55)) & 0xff));
	}
	
	/**
	 * Transforms color to integer value.
	 * @param c	Color to transform.
	 * @return	Resulting integer value.
	 */
	protected int color2int(Color c) {
		return (((c.getRed() & 0xff) << 16) 
				+ ((c.getGreen() & 0xff) << 8) + (c.getBlue() & 0xff));
	}

	/**
	 * Transforms integer value to {@link Color} object.
	 * @param cInt	Input integer value.
	 * @return	Resulting color object.
	 */
	protected Color int2Color(int cInt) {
		return new Color(((cInt & 0xff0000)>>16),
				((cInt & 0xff00)>>8), (cInt & 0xff));
	}
	
	/**
	 * Updates output image type and gray values according to chosen draw 
	 * type.
	 */
	@SuppressWarnings("unused")
  private void updateImageAndColorSettings() {
		if (this.drawType == DrawType.COLOR_IMAGE) {
			this.imageType = MTBImageType.MTB_RGB; 
			if (this.color == null && this.random == null)
				// choose red as color default
	      try {
	        this.setParameter("color", Color.RED);
        } catch (ALDOperatorException e) {
  				this.color = Color.RED;
        }
		}
		else if (this.drawType == DrawType.TRANSPARENT_IMAGE) {
			this.imageType = MTBImageType.MTB_RGB; 
			if (this.color == null && this.random == null)
				// choose red as color default
	      try {
	        this.setParameter("color", Color.RED);
        } catch (ALDOperatorException e) {
  				this.color = Color.RED;
        }
		}
		else if (this.drawType == DrawType.MASK_IMAGE) {
			this.imageType = MTBImageType.MTB_BYTE;
			if (this.grayValue == null) {
				// choose white as default gray value for regions and contours
				this.grayValue = new Double(255.0);
			}
		}
	}
	
	/**
	 * Updates parameters depending on the target image.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	@SuppressWarnings("unused")
  private void updateTargetImageDependencies() 
  		throws ALDOperatorException {
		if (this.targetImage != null) {
			if (!this.hasParameter("cloneTargetImage"))
				this.addParameter("cloneTargetImage");
			this.imageType = this.targetImage.getType();
		}
		else {
			if (this.hasParameter("cloneTargetImage"))
				this.removeParameter("cloneTargetImage");
		}
	}
	
	/**
	 * Updates random parameter if a color is set.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	@SuppressWarnings("unused")
  private void synchronizeRandomOptions() throws ALDOperatorException {
		if (this.color != null) {
			// Attention! Currently we cannot set color to null in the GUI,
			// so never remove the 'random' parameter. There is no chance
			// to get it back...
//			if (this.hasParameter("random"))
//				this.removeParameter("random");
		}
		else {
			if (!this.hasParameter("random"))
				this.addParameter("random");			
		}
	}

	/**
	 * Updates color parameter if a random generator is given.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	@SuppressWarnings("unused")
  private void synchronizeColorOptions() throws ALDOperatorException {
		if (this.random != null) {
			if (this.hasParameter("color")) {
				this.color = null;
				this.removeParameter("color");
			}
		}
		else {
			if (!this.hasParameter("color"))
				this.addParameter("color");			
		}
	}
}
