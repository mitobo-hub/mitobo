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
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;

/**
 * External snake energy based on local intensity gradients.  
 * <p>
 * The energy for a snake C is defined as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{equation*}
 *      	E(C) = \\int_0^1 - | \\nabla I(C(s)) |^2 ds 
 *      \\end{equation*}}
 * Applying this energy a snake tends to move towards image locations with 
 * large gradient magnitudes.
 * 
 * @see GradientFieldCalculator2D
 * 
 * @author Danny Misiak
 */

@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCDIB_Gradient extends MTBSnakeEnergyCDImageBased {

		/**
		 * The given input image to calculate the external energy from it.
		 */
		@ALDClassParameter(label = "Input Image")
		private MTBImage image;

		/**
		 * Normalized version of input image used in calculations.
		 */
		// TODO Birgit: optimizer contains normalized image already...
		private MTBImageDouble imageNormalized;

		private MTBVectorField2D gradField;

		/**
		 * Constructor to create a new SnakeExternalEnergyGradient object.
		 * 
		 * @param img
		 *          input image
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public MTBSnakeEnergyCDIB_Gradient() {
				// nothing to do here
		}

		/**
		 * Constructor to create a new SnakeExternalEnergyGradient object.
		 * 
		 * @param img
		 *          input image
		 */
		public MTBSnakeEnergyCDIB_Gradient(MTBImage img) {
				this.image = img;
		}

		@Override
		public boolean initEnergy(SnakeOptimizerSingle o) {
				if (this.image == null) {
						this.image = o.getWorkingImage();
				}
				this.width = this.image.getSizeX();
				this.height = this.image.getSizeY();
				this.normMode = o.getNormalizationMode();
				// initialization of super class
				boolean superInitSuccessful = super.initEnergy(o);
				if (!superInitSuccessful)
					return superInitSuccessful;
				// calculate the gradient of the image
				GradientFieldCalculator2D IG;
				try {
						IG = new GradientFieldCalculator2D(this.imageNormalized,
						    GradientFieldCalculator2D.GradientMode.PARTIAL_DIFF);
						IG.runOp(null);
						this.gradField = IG.getVectorField();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				return true;
		}

		/**
		 * Returns the negative absolute gradient value to the power of 2 (the
		 * external energy) at the given position.
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return Absolute value of external energy.
		 */
		@Override
		public double getValue(double x, double y) {
				return ((-1) * Math.pow(this.gradField.getMagnitude(x, y), 2));
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
					grad_x = this.getDerivativeX_norm(xx, yy) * 1.0/4.0;
					grad_y = this.getDerivativeY_norm(xx, yy) * 1.0/4.0;
					break;
				case NORM_NONE:
				default:
					grad_x = this.getDerivativeX_norm(xx, yy);
					grad_y = this.getDerivativeY_norm(xx, yy);
					break;
				}
				B.set(i, 0, grad_x);
				B.set(i + snakePointNum, 0, grad_y);
			}
			return B;
		}

		/**
		 * Get x-derivative of negative absolute gradient value to the power of two
		 * (external energy) at given position using central differences.
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
				double dx1 = 0.0;
				double dx2 = 0.0;
				if ((px - 1) < 0)
						dx1 = this.gradField.getMagnitude(x, y);
				else
						dx1 = this.gradField.getMagnitude(x - 1, y);
				if ((px + 1) >= this.width)
						dx2 = this.gradField.getMagnitude(x, y);
				else
						dx2 = this.gradField.getMagnitude(x + 1, y);
				return (Math.pow(dx2, 2) - Math.pow(dx1, 2));
		}

		/**
		 * Get y-derivative of negative absolute gradient value to the power of two
		 * (external energy) at given position using central differences.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return y-derivative value at given position.
		 */
		@Override
		public double getDerivativeY(double x, double y) {
				int py = (int) Math.round(y);
				double dy1 = 0.0;
				double dy2 = 0.0;
				if ((py - 1) < 0)
						dy1 = this.gradField.getMagnitude(x, y);
				else
						dy1 = this.gradField.getMagnitude(x, y - 1);

				if ((py + 1) >= this.width)
						dy2 = this.gradField.getMagnitude(x, y);
				else
						dy2 = this.gradField.getMagnitude(x, y + 1);
				return (Math.pow(dy2, 2) - Math.pow(dy1, 2));
		}

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
				return new String("SnakeExtEnergy-Gradient");
		}
}
