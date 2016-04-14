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

import java.awt.geom.Point2D;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;

/**
 * Calculate Champfer distance transform including precursor infos.
 * <p>
 * For details about the transform: see {@link DistanceTransform}
 * 
 * This class offers as additional information a map of the precursor
 * pixels for each position, i.e. the pixel which lead to the 
 * actual distance value, and a map containing for each background
 * pixel the object pixel being closest to the pixel according to 
 * the distances calculated during the transformation. 
 * <p>
 * The distance transform implementation in this class is slightly 
 * less efficient than the one in {@link DistanceTransform} since 
 * the two additional maps are filled during the calculations and 
 * more memory is required for them.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.NONE)
public class DistanceTransformPrecursorInfos extends MTBOperator {
	
	/**
	 * Binary input image.
	 */
	@Parameter(label = "Binary Input Image", required = true, 
			direction = Parameter.Direction.IN,  dataIOOrder = 0,
			description = "Binary input image.", mode = ExpertMode.STANDARD)
	private transient MTBImageByte inputImage = null;

	/**
	 * Distance metric to apply.
	 */
	@Parameter(label = "Distance Metric", required = true,
			direction = Parameter.Direction.IN, dataIOOrder = 1,
			description = "Distance metric.", mode = ExpertMode.STANDARD)
	private DistanceMetric distanceMetric = DistanceMetric.EUCLIDEAN;

	/**
	 * Foreground color of the foreground object.
	 */
	@Parameter(label = "Foreground Color", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 2,
			description = "Color of foreground.", mode = ExpertMode.STANDARD)
	private ForegroundColor foreground = ForegroundColor.FG_WHITE;

	/**
	 * The distance field as MTBImage.
	 */
	@Parameter(label = "Distance Map Image", 
			direction = Parameter.Direction.OUT, 
			description = "Image of the distance map.")
	private transient MTBImage distanceImg = null;

	/**
	 * The distance field as two-dimensional double array. Organized as
	 * double[y-dimension][x-dimension].
	 */
	@Parameter(label = "Distance Map", 
			direction = Parameter.Direction.OUT, 
			description = "2D distance map array.")
	private double[][] distanceMap = null;

	/**
	 * Map of precursor positions for each pixel in the background.
	 */
	@Parameter(label = "Precursor Map", 
			direction = Parameter.Direction.OUT, 
			description = "2D map of precursor pixels for each position.")
	private Point2D.Double[][] precursorMap = null;

	/**
	 * Map of closest object pixels for each background location.
	 */
	@Parameter(label = "Closest Object Pixel Map", 
			direction = Parameter.Direction.OUT, 
			description = "Map of closest object pixels for each position.")
	private Point2D.Double[][] closestObjectPixelMap = null;

	/**
	 * Width of the given image.
	 */
	private int width;

	/**
	 * Height of the given image.
	 */
	private int height;

	/**
	 * Standard constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public DistanceTransformPrecursorInfos() 
			throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor to create a new DistanceTransform object.
	 * 
	 * @param image	Input image to calculate the distance map from.
	 * @param dm		Distance metric.
	 * @param fg		Foreground color, i.e. white or black.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public DistanceTransformPrecursorInfos(MTBImageByte image, 
			DistanceMetric dm, ForegroundColor fg) 
					throws ALDOperatorException {
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
	 * @param inImg	Input image.
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
	 * @param metric	Distance metric to apply.
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
	 * @param fColor	Foreground color.
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
	 * Get the calculated distance map.
	 * <p>
	 * First dimension specifies the y-coordinate of the field, 
	 * and the second dimension specifies the x-coordinate.
	 * 
	 * @return Distance map.
	 */
	public double[][] getDistanceMap() {
		return this.distanceMap;
	}

	/**
	 * Get the calculated precursor map.
	 * <p>
	 * First dimension specifies the y-coordinate of the field, 
	 * and the second dimension specifies the x-coordinate.
	 * 
	 * @return Precursor map.
	 */
	public Point2D.Double[][] getPrecursorMap() {
		return this.precursorMap;
	}

	/**
	 * Get the calculated map of closest object pixels.
	 * <p>
	 * First dimension specifies the y-coordinate of the field, 
	 * and the second dimension specifies the x-coordinate.
	 * 
	 * @return Closest object pixel map.
	 */
	public Point2D.Double[][] getClosestObjectPixelMap() {
		return this.closestObjectPixelMap;
	}

	/**
	 * Get image width.
	 * @return Width of the input image.
	 */
	public int getWidth() {
		return this.width;
	}

	/**
	 * Get image height.
	 * @return Height of the input image.
	 */
	public int getHeight() {
		return this.height;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {
		this.width = this.inputImage.getSizeX();
		this.height = this.inputImage.getSizeY();
		this.precursorMap = 
				new Point2D.Double[this.height][this.width];
		this.closestObjectPixelMap = 
				new Point2D.Double[this.height][this.width];

		// initially each points is its own precursor and closest
		// object pixel
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				this.precursorMap[y][x] = new Point2D.Double(x, y);
				this.closestObjectPixelMap[y][x] = new Point2D.Double(x, y);
			}
		}

		// calculate distance map
		this.calcDM();
		// create distance image
		this.distanceImg = MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_DOUBLE);
		this.distanceImg.setTitle("DistanceTransformation-Result");
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				this.distanceImg.putValueDouble(x, y, this.distanceMap[y][x]);
			}
		}
	}

	/**
	 * Method to calculate the distance map of the given binary image 
	 * using the Chamfer-algorithm.
	 * <p> 
	 * A specified distance metric and the fore- and background
	 * of the binary image can be chosen. A neighborhood of 8 is used 
	 * for the distance calculations.
	 */
	private void calcDM() {
		/*
		 * Create the distance mask. The mask is defined like:
		 * 
		 * |b|a|b|
		 * |a|x|a|
		 * |b|a|b|
		 * 
		 * using a 3x3 mask with neighborhood 8.
		 * 
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
		 * Copy input data from binary image in a field with 0 as background 
		 * and 1 as foreground. Fore- and background of the input binary 
		 * image is defined by the user via the background variable.
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
		 *  Forward calculation using the forward mask:
		 *  
		 * |b|a|b|
		 * |a|x| |
		 * | | | |
		 * 
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
					double min = Double.MAX_VALUE;
					if (x > 0) {
						d1 = a + this.distanceMap[y][x - 1];
						if (d1 < min) {
							min = d1;
							this.precursorMap[y][x].x = x-1; 	
							this.precursorMap[y][x].y = y; 	
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y][x-1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y][x-1].y; 
						}
					}
					if (x > 0 && y > 0) {
						d2 = b + this.distanceMap[y - 1][x - 1];
						if (d2 < min) {
							min = d2;
							this.precursorMap[y][x].x = x-1; 
							this.precursorMap[y][x].y = y-1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y-1][x-1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y-1][x-1].y; 	
						}
					}
					if (y > 0) {
						d3 = a + this.distanceMap[y - 1][x];
						if (d3 < min) {
							min = d3;
							this.precursorMap[y][x].x = x; 
							this.precursorMap[y][x].y = y-1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y-1][x].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y-1][x].y; 	
						}
					}
					if (x < this.width - 1 && y > 0) {
						d4 = b + this.distanceMap[y - 1][x + 1];
						if (d4 < min) {
							min = d4;
							this.precursorMap[y][x].x = x+1; 
							this.precursorMap[y][x].y = y-1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y-1][x+1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y-1][x+1].y; 	
						}
					}
					this.distanceMap[y][x] = min;
				}
			}
		}
		/*
		 *  Backward calculation using the backward mask:
		 *  
		 * | | | |
		 * | |x|a|
		 * |b|a|b|
		 * 
		 */
		for (int y = this.height - 1; y >= 0; --y) { // bottom -> top
			for (int x = this.width - 1; x >= 0; --x) { // right -> left
				if (this.distanceMap[y][x] > MTBConstants.epsilon) {
					double d1 = Double.MAX_VALUE;
					double d2 = Double.MAX_VALUE;
					double d3 = Double.MAX_VALUE;
					double d4 = Double.MAX_VALUE;
					double min = this.distanceMap[y][x];
					if (x < this.width - 1) {
						d1 = a + this.distanceMap[y][x + 1];
						if (d1 < min) {
							min = d1;
							this.precursorMap[y][x].x = x+1; 	
							this.precursorMap[y][x].y = y; 	
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y][x+1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y][x+1].y; 	
						}
					}
					if (x < this.width - 1 && y < this.height - 1) {
						d2 = b + this.distanceMap[y + 1][x + 1];
						if (d2 < min) {
							min = d2;
							this.precursorMap[y][x].x = x+1; 
							this.precursorMap[y][x].y = y+1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y+1][x+1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y+1][x+1].y; 	
						}
					}
					if (y < this.height - 1) {
						d3 = a + this.distanceMap[y + 1][x];
						if (d3 < min) {
							min = d3;
							this.precursorMap[y][x].x = x; 
							this.precursorMap[y][x].y = y+1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y+1][x].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y+1][x].y; 	
						}
					}
					if (x > 0 && y < this.height - 1) {
						d4 = b + this.distanceMap[y + 1][x - 1];
						if (d4 < min) {
							min = d4;
							this.precursorMap[y][x].x = x-1; 
							this.precursorMap[y][x].y = y+1; 
							this.closestObjectPixelMap[y][x].x = 
									this.closestObjectPixelMap[y+1][x-1].x; 	
							this.closestObjectPixelMap[y][x].y = 
									this.closestObjectPixelMap[y+1][x-1].y; 	
						}
					}
					this.distanceMap[y][x] = min;
				}
			}
		}
	}
}
