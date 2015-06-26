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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;

/**
 * <pre>
 * 
 * Class for external energy from a distance transform (distance map / field).
 * The distance field is stored in a two-dimensional double array, where the
 * first dimension includes the y-coordinates of the field and the
 * second dimension hold the x-coordinates.
 * For the distance field the user can specify the distance metric
 * (Euclidean, Chessboard, Cityblock)
 * and can set the foreground of the given binary image.
 * 
 * So the external energy is defined as: extEnergy(x,y) = distField[y][x].
 * 
 * !!!ATTENTION!!!
 * If the distance map should be used as external energy, the map should be
 * calculated on the foreground. Normal distance map is calculated in the
 * object, so we want to set the foreground as object.
 * That means, on a black image with white objects, the foreground for the
 * distance map must be set to white and vice versa!
 * 
 * </pre>
 * 
 * @see DistanceTransform
 * 
 * 
 * @author Danny Misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCDIB_Distance extends MTBSnakeEnergyCDImageBased {

		/**
		 * The used distance metric for the distance map calculation.
		 */
		@ALDClassParameter(label = "Distance Metric")
		private DistanceMetric distMetric = DistanceMetric.EUCLIDEAN;

		@ALDClassParameter(label = "Foreground Color")
		private ForegroundColor colorFG = ForegroundColor.FG_WHITE;

		@ALDClassParameter(label = "Input Image")
		private MTBImageByte inImage;

		/**
		 * The calculated 2D vector field from the distance map.
		 */
		private double[][] distField;

		public MTBSnakeEnergyCDIB_Distance() {
				// nothing to do here
		}

		/**
		 * Constructor to create a new SnakeExternalEnergyDistance object. The
		 * distance map can be normalized between [0,1] using the normalizeEnergy()
		 * method.
		 * 
		 * @param image
		 *          the given input image to calculate the external energy
		 * @param d
		 *          distance metric (euclidean, cityblock, ...)
		 * @param f
		 *          foreground f the image (white or black)
		 */
		public MTBSnakeEnergyCDIB_Distance(MTBImageByte image, DistanceMetric d,
		    ForegroundColor f) {
				this.inImage = image;
				this.distMetric = d;
				this.colorFG = f;
		}

		@Override
		public boolean initEnergy(SnakeOptimizerSingle o) {
				if (this.inImage == null) {
						this.inImage = (MTBImageByte) o.getWorkingImage().convertType(
						    MTBImageType.MTB_BYTE, true);
				}
				this.width = this.inImage.getSizeX();
				this.height = this.inImage.getSizeY();

				/*
				 * Calculate the distance field from the given binary image with the
				 * specified parameters.
				 */
				DistanceTransform DT;
				try {
						DT = new DistanceTransform(this.inImage, this.distMetric, this.colorFG);
						DT.runOp(null);
						this.distField = DT.getDistanceMap();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				return super.initEnergy(o);
		}

		/**
		 * Returns the distance value from the distance map at the given position.
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return Absolute value of external energy.
		 */
		@Override
		public double getValue(double x, double y) {
				// return (this.distField.getValue((int) Math.round(x), (int)
				// Math.round(y)));
				return (this.distField[(int) Math.round(y)][(int) Math.round(x)]);
		}

		/**
		 * Get x-derivative of the distance map at given position using central
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
						// dx1 = this.distField.getValue(px, py);
						dx1 = this.distField[py][px];
				else
						// dx1 = this.distField.getValue(px - 1, py);
						dx1 = this.distField[py][px - 1];

				if ((px + 1) >= this.width)
						// dx2 = this.distField.getValue(px, py);
						dx2 = this.distField[py][px];
				else
						// dx2 = this.distField.getValue(px + 1, py);
						dx2 = this.distField[py][px + 1];
				return (dx2 - dx1);
		}

		/**
		 * Get y-derivative of the distance map at given position using central
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
						// dy1 = this.distField.getValue(px, py);
						dy1 = this.distField[py][px];
				else
						// dy1 = this.distField.getValue(px, py - 1);
						dy1 = this.distField[py - 1][px];

				if ((py + 1) >= this.height)
						// dy2 = this.distField.getValue(px, py);
						dy2 = this.distField[py][px];
				else
						// dy2 = this.distField.getValue(px, py + 1);
						dy2 = this.distField[py + 1][px];
				return (dy2 - dy1);
		}

		/**
		 * Normalize the external energy in a range [0.0, 1.0].
		 */
		@Override
		public void normalizeEnergy() {
				double maxB = 1.0;
				double minB = 0.0;
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				// get minimum and maximum value
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								if (this.distField[y][x] < min)
										min = this.distField[y][x];
								if (this.distField[y][x] > max)
										max = this.distField[y][x];
						}
				}
				// normalize the values in the energy
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								this.distField[y][x] = (this.distField[y][x] - min)
								    * ((maxB - minB) / (max - min)) + minB;
						}
				}
		}

		@Override
		public String toString() {
				return new String("SnakeExtEnergy-DistanceMap (metric: "
				    + this.distMetric.toString() + ")");
		}
}
