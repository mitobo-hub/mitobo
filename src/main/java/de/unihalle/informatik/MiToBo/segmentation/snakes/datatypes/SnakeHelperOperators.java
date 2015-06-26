/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2011
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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.geom.*;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Helper class with internal operators for snakes.
 *
 * @author misiak, moeller
 */
class SnakeHelperOperators {

	/**
	 * Operator class to generate 2D array mask for snake interior.
	 * <p>
	 * The mask will have size (width x height) and the area enclosed by the 
	 * polygon will be filled in white. The background will be filled in black.
	 * The orientation of the polygon is not considered here. Note that 
	 * undefined behavior will result if applied to non-closed snakes.
	 * 
	 * @author moeller
	 */
	protected class MaskMaker extends MTBOperator {

		/**
		 * Input snake.
		 */
		@Parameter(label = "inSnake", direction = Direction.IN, 
				required = true, description = "Input snake.")
		private MTBSnake inSnake = null;

		/**
		 * Generated mask.
		 */
		@Parameter(label = "outMask", direction = Direction.OUT, 
				description = "Output mask.")
		private int[][] outMask = null;

		/**
		 * Width of the mask to be generated.
		 */
		@Parameter(label = "maskWidth", direction = Direction.IN, 
				required = true, description = "Width of the mask.")
		private int maskWidth = 0;

		/**
		 * Height of the mask to be generated.
		 */
		@Parameter(label = "maskHeight", direction = Direction.IN, 
				required = true, description = "Height of the mask.")
		private int maskHeight = 0;

		/**
		 * x-offset of the mask coordinate system.
		 */
		@Parameter(label = "xOffset", direction = Direction.IN, 
				required = false, description = "Origin offset in x.")
		private int xOffset = 0;

		/**
		 * y-offset of the mask coordinate system.
		 */
		@Parameter(label = "yOffset", direction = Direction.IN, 
				required = false, description = "Origin offset in y.")
		private int yOffset = 0;                   

		/**
		 * Default constructor with zero offsets.
		 * 
		 * @param input	Snake for which the mask is to be generated.
		 * @param w	Width of mask array.
		 * @param h Height of mask array.
		 * @throws ALDOperatorException
		 */
		MaskMaker(MTBSnake input, int w, int h) throws ALDOperatorException {
			this.inSnake = input;
			this.maskWidth = w;
			this.maskHeight = h;
		}

		/**
		 * Constructor with non-zero offsets.
		 * 
		 * @param input	Snake for which the mask is to be generated.
		 * @param xoff Offset in x direction, 1st column will have this coordinate.
		 * @param yoff Offset in y direction, 1st row will have this coordinate.
		 * @param w	Width of mask array.
		 * @param h Height of mask array.
		 * @throws ALDOperatorException
		 */
		MaskMaker(MTBSnake input, int xoff, int yoff, int w, int h) 
		throws ALDOperatorException {
			this.inSnake = input;
			this.maskWidth = w;
			this.maskHeight = h;
			this.xOffset = xoff;
			this.yOffset = yoff;
		}

		@Override
		protected void operate() {
			// rescale the polygon points,
			// if scaling factor is 1.0, use Polygon2D method
			int[] xps = new int[this.inSnake.getPointNum()];
			int[] yps = new int[this.inSnake.getPointNum()];
			int n = 0;
			double scaleFactor = this.inSnake.getScaleFactor();
			Vector<Point2D.Double> points= this.inSnake.getPoints();
			for (Point2D.Double p : points) {
				xps[n] = (int) (p.x * scaleFactor + 0.5);
				yps[n] = (int) (p.y * scaleFactor + 0.5);
				n++;
			}
			Polygon awtPoly = new Polygon(xps, yps, this.inSnake.getPointNum());

			// plot filled polygon in black on white background
			ImagePlus img = 
				NewImage.createByteImage("", this.maskWidth + this.xOffset, 
						this.maskHeight + this.yOffset, 1, 
						NewImage.FILL_WHITE);
			ImageProcessor ip = img.getProcessor();
			ip.fillPolygon(awtPoly);

			// copy image data to mask
			this.outMask = new int[this.maskHeight][this.maskWidth];
			for (int y = 0; y < this.maskHeight; ++y) {
				for (int x = 0; x < this.maskWidth; ++x) {
					if (ip.getPixel(x+this.xOffset, y+this.yOffset) == 0)
						// polygon inner area
						this.outMask[y][x] = 1;
				}
			}
		}

		/**
		 * Returns generated mask.
		 */
		protected int[][] getResultMask() {
			return this.outMask;
		}
	}

	/**
	 * Operator class to draw snake to an image.
	 * 
	 * @author moeller
	 */
	protected class ImageMaker extends MTBOperator {

		/**
		 * Input snake.
		 */
		@Parameter(label = "inSnake", direction = Direction.IN, 
				required = true, description = "Input snake.")
		private MTBSnake inSnake = null;

		/**
		 * Output image.
		 */
		@Parameter(label = "outImage", direction = Direction.OUT, 
				description = "Output image.")
		private MTBImage outImage = null;

		/**
		 * Width of the image to be generated.
		 */
		@Parameter(label = "imWidth", direction = Direction.IN, 
				required = true, description = "Width of the image.")
		private int iWidth = 0;

		/**
		 * Height of the image to be generated.
		 */
		@Parameter(label = "imHeight", direction = Direction.IN, 
				required = true, description = "Height of the image.")
		private int iHeight = 0;

		/**
		 * Type of the image to be generated.
		 */
		@Parameter(label = "imType", direction = Direction.IN, 
				required = false, description = "Type of the image.")
		private int imType = 0;

		/**
		 * File name for optionally saving the image.
		 */
		@Parameter(label = "filename", direction = Direction.IN, 
				required = false, description = "File where to save the image.")
		private String file = null;

		/**
		 * Optional input image to be used for drawing.
		 */
		@Parameter(label = "inImage", direction = Direction.IN, 
				required = false, description = "Optional input image.")
		private MTBImage inImage = null;

		/**
		 * Default constructor.
		 * 
		 * @param input		Input snake.
		 * @param imgType	Type of output image.
		 * @param w				Width of image.
		 * @param h				Height of image.
		 * @param f				Optional filename for saving.
		 * @param img			Image where to draw into.
		 * @throws ALDOperatorException
		 */
		ImageMaker(MTBSnake input, int imgType, int w, int h, String f,
				MTBImage img) throws ALDOperatorException {
			this.inSnake = input;
			this.imType = imgType;
			this.iWidth = w;
			this.iHeight = h;
			this.file = f;
			this.inImage = img;
		}

		@Override
		protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {

			Vector<Point2D.Double> points = this.inSnake.getPoints();
			double scaleFactor = this.inSnake.getScaleFactor();

			// MTBImageByte
			if (this.imType == 1) {
				MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(
						this.iWidth, this.iHeight, 1, 1, 1, MTBImageType.MTB_BYTE);
				int end = this.inSnake.getPoints().size();
				if (this.inSnake.isClosed()) {
					end = this.inSnake.getPoints().size() - 1;
				}
				for (int i = 0; i < end; i++) {
					int x1 = (int) Math.round(points.elementAt(i).getX()
							* scaleFactor);
					int y1 = (int) Math.round(points.elementAt(i).getY()
							* scaleFactor);
					int x2 = (int) Math.round(points.elementAt(i + 1).getX()
							* scaleFactor);
					int y2 = (int) Math.round(points.elementAt(i + 1).getY()
							* scaleFactor);
					imageToDraw.drawLine2D(x1, y1, x2, y2, 255);
				}
				if (this.inSnake.isClosed()) {
					int x1 = (int) Math.round(points.elementAt(end).getX()
							* scaleFactor);
					int y1 = (int) Math.round(points.elementAt(end).getY()
							* scaleFactor);
					int x2 = (int) Math.round(points.elementAt(0).getX()
							* scaleFactor);
					int y2 = (int) Math.round(points.elementAt(0).getY()
							* scaleFactor);
					imageToDraw.drawLine2D(x1, y1, x2, y2, 255);
				}
				// if no image should be saved on disk, file has to be null.
				if (this.file != null) {
					//									ImageSave(file, imageToDraw, true, true);
					ImageWriterMTB WI = new ImageWriterMTB(imageToDraw, this.file);
					WI.setVerbose(new Boolean(true));
					WI.setOverwrite(true);
					WI.runOp(true);
				}
				this.outImage = imageToDraw;
			}
			// MTBImage
			else if (this.imType == 2) {
				MTBImage imageToDraw = this.inImage.duplicate();
				int end = points.size();
				if (this.inSnake.isClosed()) {
					end = points.size() - 1;
				}
				for (int i = 0; i < end; i++) {
					int x1 = (int) Math.round(points.elementAt(i).getX()
							* scaleFactor);
					int y1 = (int) Math.round(points.elementAt(i).getY()
							* scaleFactor);
					int x2 = (int) Math.round(points.elementAt(i + 1).getX()
							* scaleFactor);
					int y2 = (int) Math.round(points.elementAt(i + 1).getY()
							* scaleFactor);
					imageToDraw
					.drawLine2D(x1, y1, x2, y2, (int) imageToDraw.getTypeMax());
				}
				if (this.inSnake.isClosed()) {
					int x1 = (int) Math.round(points.elementAt(end).getX()
							* scaleFactor);
					int y1 = (int) Math.round(points.elementAt(end).getY()
							* scaleFactor);
					int x2 = (int) Math.round(points.elementAt(0).getX()
							* scaleFactor);
					int y2 = (int) Math.round(points.elementAt(0).getY()
							* scaleFactor);
					imageToDraw
					.drawLine2D(x1, y1, x2, y2, (int) imageToDraw.getTypeMax());
				}
				// if no image should be saved on disk, file has to be null.
				if (this.file != null) {
					//										ImageSave IS = new ImageSave(file, imageToDraw, true, true);
					ImageWriterMTB WI = new ImageWriterMTB(imageToDraw,this.file);
					WI.setVerbose(new Boolean(true));
					WI.setOverwrite(true);
					WI.runOp(true);
				}
				this.outImage = imageToDraw;
			}
		}

		/**
		 * Returns the generated image.
		 */
		protected MTBImage getResultImage() {
			return this.outImage;
		}
	}

	/**
	 * Operator class to convert a contour into a snake.
	 * 
	 * @author moeller
	 */
	protected class ContourConverter extends MTBOperator {

		/**
		 * Input contour.
		 */
		@Parameter(label = "inContour", direction = Direction.IN, 
				required = true, description = "Input contour.")
		private MTBContour2D inContour = null;

		/**
		 * Result snake.
		 */
		@Parameter(label = "outSnake", direction = Direction.OUT, 
				description = "Output snake.")
		private MTBSnake outSnake = null;

		/**
		 * Default constructor.
		 * 
		 * @param cont	Incoming contour.
		 * @throws ALDOperatorException
		 */
		ContourConverter(MTBContour2D cont) throws ALDOperatorException {
			this.inContour = cont;
		}

		/**
		 * Returns resulting snake object.
		 */
		protected MTBSnake getResultSnake() {
			return this.outSnake;
		}

		@Override
		protected void operate() {
			int cPntNum = this.inContour.getPointNum();
			Vector<MTBSnakePoint2D> snakePoints = 
				new Vector<MTBSnakePoint2D>(cPntNum);
			for (int i = 0; i < cPntNum; i++) {
				MTBSnakePoint2D tmpPoint = 
					new MTBSnakePoint2D(this.inContour.getPointAt(i));
				snakePoints.addElement(tmpPoint);
			}
			this.outSnake = new MTBSnake(snakePoints,true,1.0,true);
		}
	}

	/**
	 * Operator class to convert a region into a snake.
	 * 
	 * @author moeller
	 */
	protected class RegionConverter extends MTBOperator {

		/**
		 * Input region.
		 */
		@Parameter(label = "inRegion", direction = Direction.IN, 
				required = true, description = "Input region.")
		private MTBRegion2D inRegion = null;

		/**
		 * Resulting snake.
		 */
		@Parameter(label = "outSnake", direction = Direction.OUT, 
				description = "Output snake.")
		private MTBSnake outSnake = null;

		/**
		 * Default constructor.
		 * 
		 * @param reg	Incoming region.
		 * @throws ALDOperatorException
		 */
		RegionConverter(MTBRegion2D reg) throws ALDOperatorException {
			this.inRegion = reg;
		}

		/**
		 * Returns resulting snake object.
		 */
		protected MTBSnake getResultSnake() {
			return this.outSnake;
		}

		@Override
		protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
			// extract the contour
			MTBContour2D inContour = this.inRegion.getContour();
			this.outSnake = MTBSnake.convertContourToSnake(inContour);
		}
	}

	/**
	 * Operator class to extract snakes from a region image.
	 * 
	 * @author moeller
	 */
	protected class RegionImageConverter extends MTBOperator {

		/**
		 * Input image.
		 */
		@Parameter(label = "inImg", direction = Direction.IN, 
				required = true, description = "Input image.")
		private MTBImage inImg = null;

		/**
		 * Result snakes.
		 */
		@Parameter(label = "outSnakes", direction = Direction.OUT, 
				description = "Output snakes.")
		private MTBPolygon2DSet outSnakes = null;

		/**
		 * Result image displaying the snakes as overlay.
		 */
		@Parameter( label= "resultImg", direction = Direction.OUT,
				description = "(Optional) result image")
		private MTBImageRGB resultImg = null;

		/**
		 * Default constructor with parameters.
		 * 
		 * @param img	Input image.
		 * @throws ALDOperatorException
		 */
		RegionImageConverter(MTBImage img) 
		throws ALDOperatorException {
			this.inImg = img;
		}

		/**
		 * Returns resulting snake objects.
		 */
		protected MTBPolygon2DSet getResultSnakes() {
			return this.outSnakes;
		}

		/**
		 * Returns image with plotted snakes.
		 */
		protected MTBImageRGB getResultImage() {
			return this.resultImg;
		}

		@Override
		protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {

			// create a binary mask from the image
			MTBImageByte mask= 
				(MTBImageByte)MTBImage.createMTBImage(this.inImg.getSizeX(),
						this.inImg.getSizeY(),1,1,1,MTBImageType.MTB_BYTE);
			for (int y=0; y<this.inImg.getSizeY(); ++y) {
				for (int x=0; x<this.inImg.getSizeX(); ++x) {
					if (this.inImg.getValueInt(x, y)>0)
						mask.putValueInt(x, y, 255);
					else
						mask.putValueInt(x, y, 0);
				}
			}

			// label connected components
			LabelComponentsSequential regLabler=
				new LabelComponentsSequential(mask, true);
			regLabler.runOp(true);
			MTBRegion2DSet regions= regLabler.getResultingRegions();

			// extract outer contours of all labeled components, i.e. regions
			ContourOnLabeledComponents labler= 
				new ContourOnLabeledComponents(mask,regions,ContourType.OUTER_CONTOUR,0);
			labler.runOp(true);
			MTBContour2DSet contours= labler.getResultContours();

			this.outSnakes= new MTBPolygon2DSet(
					0, 0, this.inImg.getSizeX()-1, this.inImg.getSizeY()-1);
			for (int i=0; i<contours.size(); ++i) {
				MTBContour2D c= contours.elementAt(i); 
				this.outSnakes.add(MTBSnake.convertContourToSnake(c));
			}
		}
	}
}
