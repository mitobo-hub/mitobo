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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies;

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;

/**
 * External snake energy based on image intensities. 
 * <p>
 * The energy for a snake C is defined as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{equation*}
 *      	E(C) = \\int_0^1 I(C(s)) ds 
 *      \\end{equation*}}
 * Applying this energy a snake tends to move towards dark areas in an image,
 * i.e. moves in the opposite direction of the local intensity gradient.
 * <p> 
 * Note that this energy has only a small capture range of one or two pixels,
 * thus, your initialization is required to be very accurate already.
 * 
 * @author Danny Misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCDIB_Intensity extends MTBSnakeEnergyCDImageBased {

		/**
		 * The given input image to calculate the external energy from it.
		 */
		@ALDClassParameter(label = "Input Image")
		private MTBImage image;

		/**
		 * Normalized version of input image used in calculations.
		 */
		private MTBImageDouble imageNormalized;

		/**
		 * Default constructor.
		 */
		public MTBSnakeEnergyCDIB_Intensity() {
				// nothing to do here
		}

		/**
		 * Constructor to create a new SnakeExternalEnergyIntensity object.
		 * 
		 * @param _image
		 *          input image
		 */
		public MTBSnakeEnergyCDIB_Intensity(MTBImage _image) {
				this.image = _image;
		}

		@Override
		public boolean initEnergy(SnakeOptimizerSingle o) {
			if (this.image == null) {
				this.image = o.getWorkingImage();
				this.imageNormalized = 
					(MTBImageDouble)this.image.convertType(MTBImageType.MTB_DOUBLE,true);
			}
			else {
				// normalize image according to normalization mode of optimizer
				this.imageNormalized = 
					(MTBImageDouble)this.image.convertType(MTBImageType.MTB_DOUBLE,true);
				// do normalization according to chosen mode
				double sourceMin, sourceMax, targetMin, targetMax, maxAbsVal;
				switch(o.getIntensityNormalizationMode())
				{
				case INTENSITY_NORM_TRUE_RANGE:
					double minmax[] = this.image.getMinMaxDouble();
					// tell the user that image contains negative intensities...
					if (minmax[0] < 0) {
						System.err.println(
								"Your image contains negative intensities!\n" + 
								"We hope that you know what you are doing..."); 
					}
					maxAbsVal = (Math.abs(minmax[0]) > Math.abs(minmax[1]) ? 
							 Math.abs(minmax[0]) : Math.abs(minmax[1]));
					// only negative values in image
					if (minmax[1] < 0) {
						sourceMin = minmax[0];
						sourceMax = minmax[1];
						targetMin = -1;
						targetMax =  0;
					}
					// negative and positive values
					else if (minmax[0] < 0 && minmax[1] >= 0){
						sourceMin = -maxAbsVal;
						sourceMax =  maxAbsVal;
						targetMin = -1;
						targetMax =  1;
					}
					// only positive values in image
					else {
						sourceMin = minmax[0];
						sourceMax = minmax[1];
						targetMin = 0;
						targetMax = 1;				
					}
					// normalize the image
					this.imageNormalized = this.image.scaleValues(0, 0, 
							sourceMin, sourceMax, targetMin, targetMax);
					break;
				case INTENSITY_NORM_THEORETIC_RANGE:
					maxAbsVal = 
						(Math.abs(this.image.getTypeMin()) > 
						 Math.abs(this.image.getTypeMax()) ? 
								 Math.abs(this.image.getTypeMin()) :
									 Math.abs(this.image.getTypeMax()));
					// only negative values in image
					if (this.image.getTypeMax() < 0) {
						sourceMin = this.image.getTypeMin();
						sourceMax = this.image.getTypeMax();
						targetMin = -1;
						targetMax =  0;
					}
					// negative and positive values
					else if (    this.image.getTypeMin() < 0 
										&& this.image.getTypeMax() >= 0) {
						sourceMin = -maxAbsVal;
						sourceMax =  maxAbsVal;
						targetMin = -1;
						targetMax =  1;
					}
					// only positive values in image
					else {
						sourceMin = this.image.getTypeMin();
						sourceMax = this.image.getTypeMax();
						targetMin = 0;
						targetMax = 1;				
					}
					// normalize the image
					this.imageNormalized = this.image.scaleValues(0, 0, 
						sourceMin, sourceMax, targetMin, targetMax);
					break;
				case INTENSITY_NORM_NONE:
				default:
					// nothing to do here
					break;
				}
			}
			this.width = this.image.getSizeX();
			this.height = this.image.getSizeY();
			this.normMode = o.getNormalizationMode();
			return super.initEnergy(o);
		}

		/**
		 * Returns the value of the external energy at the given position.
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return Absolute value of external energy.
		 */
		@Override
		public double getValue(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return (this.imageNormalized.getValueDouble(px, py));
		}

		@Override
		public Matrix getDerivative_VectorPart(SnakeOptimizerSingleVarCalc opt) {
			MTBSnake snake = (MTBSnake)opt.getCurrentSnakes().elementAt(0);
			int snakePointNum = snake.getPointNum();
			Matrix B = new Matrix(snakePointNum * 2, 1);
			for (int i = 0; i < snakePointNum; i++) {
				double xx = snake.getPoints().elementAt(i).getX();
				double yy = snake.getPoints().elementAt(i).getY();
				
				// consider mode of normalization
				double grad_x = 0.0, grad_y = 0.0;
				switch(this.normMode) 
				{
				case NORM_BALANCED_DERIVATIVES:
					grad_x = this.getDerivativeX_norm(xx, yy);
					grad_y = this.getDerivativeY_norm(xx, yy);
					break;
				case NORM_NONE:
				default:
				}
				B.set(i, 0, grad_x);
				B.set(i + snakePointNum, 0, grad_y);
			}
			return B;
		}

		/**
		 * Get x-derivative of external snake energy at given position using central
		 * differences.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return x-derivative value at given position.
		 */
		@Override
		public double getDerivativeX(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				double dx1 = 0.0;
				double dx2 = 0.0;
				if ((px - 1) < 0)
						dx1 = this.imageNormalized.getValueDouble(px, py);
				else
						dx1 = this.imageNormalized.getValueDouble(px - 1, py);
				if ((px + 1) >= this.width)
						dx2 = this.imageNormalized.getValueDouble(px, py);
				else
						dx2 = this.imageNormalized.getValueDouble(px + 1, py);
				
				// switch between different normalization modes
				return (dx2 - dx1);
		}

		/**
		 * Get y-derivative of external snake energy at given position using central
		 * differences.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return y-derivative value at given position.
		 */
		@Override
		public double getDerivativeY(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				double dy1 = 0.0;
				double dy2 = 0.0;
				if ((py - 1) < 0)
						dy1 = this.imageNormalized.getValueDouble(px, py);
				else
						dy1 = this.imageNormalized.getValueDouble(px, py - 1);
				if ((py + 1) >= this.width)
						dy2 = this.imageNormalized.getValueDouble(px, py);
				else
						dy2 = this.imageNormalized.getValueDouble(px, py + 1);
				return (dy2 - dy1);
		}

		/**
		 * Normalize the external energy in a range [-1.0, 1.0].
		 */
		@Override
		public void normalizeEnergy() {
			this.imageNormalized = 
					(MTBImageDouble) MTBImage.createMTBImage(this.width, this.height, 
							1, 1, 1, MTBImageType.MTB_DOUBLE);
			double maxB = 1.0;
			double minB = 0.0;
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			// get minimum and maximum value
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.width; x++) {
					if (this.image.getValueDouble(x, y) < min)
						min = this.image.getValueDouble(x, y);
					if (this.image.getValueDouble(x, y) > max)
						max = this.image.getValueDouble(x, y);
				}
			}
			// normalize the values in the energy
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.width; x++) {
					double newValue = (this.image.getValueDouble(x, y) - min)
							* ((maxB - minB) / (max - min)) + minB;
					this.imageNormalized.putValueDouble(x, y, newValue);
				}
			}
		}

		@Override
		public String toString() {
				return new String("SnakeExtEnergy-Intensity");
		}
}
