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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.morphology;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * <pre>
 * 
 * Class to calculate a distance map / field from a binary image using the
 * Champfer-algorithm from
 * 
 * G. Borgefors, "Distance transformations in digital images", Computer
 * Vision, Graphics, and Image Processing, vol. 34, pp. 344–371, 1986.
 * 
 * Several distance metrics (Euclide, Chessboard, Cityblock) and the foreground
 * (0 for black and 1 for white) can be specified. A 8-way neighborhood is used.
 * 
 * NOTE!
 * The distance is calculated for each background pixel to the nearest
 * foreground pixel. For example, if the distance should be calculated inside a
 * white object, the black background should be set as foreground color.
 * 
 * The distance field is created as MTBImage or as a two dimensional double array.
 * First dimension specifies the y-coordinate of the field, and the
 * second dimension specifies the x-coordinate of the field.
 * 
 * </pre>
 * 
 * 
 * @author misiak
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION)
@ALDMetaInfo(export = ExportPolicy.MANDATORY)
public class DistanceTransform extends MTBOperator {
		/**
		 * Color of binary image foreground.
		 * 
		 * @author Danny Misiak
		 */
		public static enum ForegroundColor {
				FG_BLACK, FG_WHITE
		}

		/**
		 * Several distance metrics.
		 * 
		 * @author misiak
		 */
		public static enum DistanceMetric {
				/**
				 * Euclidean distance metric where dist = Math.sqrt(dx * dx + dy * dy).
				 */
				EUCLIDEAN,
				/**
				 * Cityblock distance metric where dist = Math.abs(dx) + Math.abs(dy).
				 */
				CITYBLOCK,
				/**
				 * Chessboard distance metric where dist = 1.
				 */
				CHESSBOARD
		}

		@Parameter(label = "Binary Input Image", required = true, direction = Parameter.Direction.IN, description = "Binary input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImageByte inputImage = null;

		@Parameter(label = "Distance Metric", direction = Parameter.Direction.IN, required = true, description = "Used distance metric", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private DistanceMetric distanceMetric = DistanceMetric.EUCLIDEAN;

		/**
		 * Foreground color of the foreground object.
		 */
		@Parameter(label = "Foreground Color", required = true, direction = Parameter.Direction.IN, description = "Color of foreground.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private ForegroundColor foreground = ForegroundColor.FG_WHITE;

		/**
		 * The distance field as MTBImage.
		 */
		@Parameter(label = "Distance Map Image", direction = Parameter.Direction.OUT, description = "Image of the distance map.")
		private transient MTBImage distanceImg = null;

		/**
		 * The distance field as two-dimensional double array. Organized as
		 * double[y-dimension][x-dimension].
		 */
		@Parameter(label = "Distance Map", direction = Parameter.Direction.OUT, description = "2D distance map array.")
		private double[][] distanceMap = null;

		/**
		 * Width and height of the given image.
		 */
		private int width, height;

		/**
		 * Standard constructor.
		 */
		public DistanceTransform() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new DistanceTransform object.
		 * 
		 * @param image
		 *          the input image to calculate the distance map on it
		 * @param dm
		 *          the distance metric
		 * @param fg
		 *          set foreground white or black
		 * @throws ALDOperatorException
		 */
		public DistanceTransform(MTBImageByte image, DistanceMetric dm,
		    ForegroundColor fg) throws ALDOperatorException {
				this.inputImage = image;
				this.distanceMetric = dm;
				this.foreground = fg;
		}

		/**
		 * Get the input image.
		 * 
		 * @return Input image.
		 */
		public MTBImageByte getInImg() {
				return this.inputImage;
		}

		/**
		 * Set the input image.
		 */
		public void setInImg(MTBImageByte inImg) {
				this.inputImage = inImg;
		}

		/**
		 * Get the used distance metric.
		 * 
		 * @return Distance metric.
		 */
		public DistanceMetric getDistMetric() {
				return this.distanceMetric;
		}

		/**
		 * Set the distance metric.
		 */
		public void setDistMetric(DistanceMetric metric) {
				this.distanceMetric = metric;
		}

		/**
		 * Get the used foreground color.
		 * 
		 * @return Foreground color.
		 */
		public ForegroundColor getForeground() {
				return this.foreground;
		}

		/**
		 * Set the foreground color.
		 */
		public void setForeground(ForegroundColor fColor) {
				this.foreground = fColor;
		}

		/**
		 * Get the calculated distance map image.
		 * 
		 * @return Distance map image.
		 */
		public MTBImage getDistanceImage() {
				return this.distanceImg;
		}

		/**
		 * Get the calculated distance map. First dimension specifies the y-coordinate
		 * of the field, and the second dimension specifies the x-coordinate of the
		 * field.
		 * 
		 * @return Distance map.
		 */
		public double[][] getDistanceMap() {
				return this.distanceMap;
		}

		/**
		 * Get image width.
		 */
		public int getWidth() {
				return width;
		}

		/**
		 * Set image width.
		 */
		public void setWidth(int width) {
				this.width = width;
		}

		/**
		 * Get image height.
		 */
		public int getHeight() {
				return height;
		}

		/**
		 * Set image height.
		 */
		public void setHeight(int height) {
				this.height = height;
		}

		@Override
		protected void operate() throws ALDOperatorException {
				this.width = this.inputImage.getSizeX();
				this.height = this.inputImage.getSizeY();
				// calculate distance map
				this.calcDM();
				// create distance image
				this.distanceImg = MTBImage.createMTBImage(this.width, this.height, 1, 1,
				    1, MTBImageType.MTB_DOUBLE);
				this.distanceImg.setTitle("DistanceTransformation-Result");
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								distanceImg.putValueDouble(x, y, this.distanceMap[y][x]);
						}
				}
		}

		/**
		 * Method to calculate the distance map of the given binary image using the
		 * Chamfer-algorithm. A specified distance metric and the fore- and background
		 * of the binary image can be chosen. A neighborhood of 8 is used for the
		 * distance calculation.
		 */
		private void calcDM() {
				/**
				 * <pre>
				 * Create the distance mask. The mask is defined like:
				 * 
				 * |b|a|b|
				 * |a|x|a|
				 * |b|a|b|
				 * 
				 * using a 3x3 mask with neighborhood 8.
				 * 
				 * </pre>
				 */
				double a = 0.0;
				double b = 0.0;
				switch (this.distanceMetric) {
				case EUCLIDEAN: // Euclidean Distance Metric
						a = 1.0;
						b = Math.sqrt(2.0);
						break;
				case CITYBLOCK: // City Block Distance Metric
						a = 1.0;
						b = Double.MAX_VALUE;
						break;
				case CHESSBOARD: // Chessboard Distance Metric
						a = 1.0;
						b = 1.0;
						break;
				}

				this.distanceMap = new double[this.height][this.width];
				/*
				 * Copy input data from binary image in a field with 0 as background and 1
				 * as foreground. Fore- and background of the input binary image is defined
				 * by the user via the background variable.
				 */
				double[][] binCopy = new double[this.height][this.width];
				switch (this.foreground) {
				case FG_WHITE:
						for (int y = 0; y < this.height; ++y) {
								for (int x = 0; x < this.width; ++x) {
										if (this.inputImage.getValueDouble(x, y) > 128.0) {
												this.distanceMap[y][x] = 1.0;
												binCopy[y][x] = 1.0;
										} else {
												this.distanceMap[y][x] = 0.0;
												binCopy[y][x] = 0.0;
										}
								}
						}
						break;
				case FG_BLACK:
						for (int y = 0; y < this.height; ++y) {
								for (int x = 0; x < this.width; ++x) {
										if (this.inputImage.getValueDouble(x, y) < 128.0) {
												this.distanceMap[y][x] = 1.0;
												binCopy[y][x] = 1.0;
										} else {
												this.distanceMap[y][x] = 0.0;
												binCopy[y][x] = 0.0;
										}
								}
						}
						break;
				}
				/*
				 * Calculate current distance map with given distance metric and
				 */

				/**
				 * <pre>
				 *  Forward calculation using the forward mask:
				 *  
				 * |b|a|b|
				 * |a|x| |
				 * | | | |
				 * 
				 * </pre>
				 */
				for (int y = 0; y < this.height; ++y) {
						for (int x = 0; x < this.width; ++x) {
								if (binCopy[y][x] == 1.0)
										this.distanceMap[y][x] = 0.0;
								else
										this.distanceMap[y][x] = Double.MAX_VALUE;
						}
				}
				for (int y = 0; y < this.height; ++y) {// top -> bottom
						for (int x = 0; x < this.width; ++x) {// left -> right
								if (this.distanceMap[y][x] > MTBConstants.epsilon) {
										double d1 = Double.MAX_VALUE;
										double d2 = Double.MAX_VALUE;
										double d3 = Double.MAX_VALUE;
										double d4 = Double.MAX_VALUE;
										if (x > 0)
												d1 = a + this.distanceMap[y][x - 1];
										if (x > 0 && y > 0)
												d2 = b + this.distanceMap[y - 1][x - 1];
										if (y > 0)
												d3 = a + this.distanceMap[y - 1][x];
										if (x < this.width - 1 && y > 0)
												d4 = b + this.distanceMap[y - 1][x + 1];
										double min = Math.min(Math.min(d1, d2), Math.min(d3, d4));
										this.distanceMap[y][x] = min;
								}
						}
				}
				/**
				 * <pre>
				 *  Backward calculation using the backward mask:
				 *  
				 * | | | |
				 * | |x|a|
				 * |b|a|b|
				 * 
				 * </pre>
				 */
				for (int y = this.height - 1; y >= 0; --y) { // bottom -> top
						for (int x = this.width - 1; x >= 0; --x) { // right -> left
								if (this.distanceMap[y][x] > MTBConstants.epsilon) {
										double d1 = Double.MAX_VALUE;
										double d2 = Double.MAX_VALUE;
										double d3 = Double.MAX_VALUE;
										double d4 = Double.MAX_VALUE;
										if (x < this.width - 1)
												d1 = a + this.distanceMap[y][x + 1];
										if (x < this.width - 1 && y < this.height - 1)
												d2 = b + this.distanceMap[y + 1][x + 1];
										if (y < this.height - 1)
												d3 = a + this.distanceMap[y + 1][x];
										if (x > 0 && y < this.height - 1)
												d4 = b + this.distanceMap[y + 1][x - 1];
										double min = Math.min(this.distanceMap[y][x], Math.min(Math.min(d1,
										    d2), Math.min(d3, d4)));
										this.distanceMap[y][x] = min;
								}
						}
				}
		}
}
