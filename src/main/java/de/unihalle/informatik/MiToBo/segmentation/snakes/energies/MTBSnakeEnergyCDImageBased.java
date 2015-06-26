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

import java.awt.geom.Point2D;
import java.util.Vector;
import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * Interface class for snake energies that are purely image-based.
 * <p>
 * This class is used by the snake optimizer. Pure image-based energies are
 * energies that just evaluate the energy gradient at single image positions.
 * Examples are intensity gradient, GVFs and so on. On updating the snake
 * optimizer target functional they usually just modify the constant vector and
 * not the linear matrix. All energies of this type share the common property
 * that derivatives can be calculated quite easy and straight-forward which
 * discriminates them from more complex snake energies.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public abstract class MTBSnakeEnergyCDImageBased 
	implements MTBSnakeEnergyDerivable, MTBSnakeEnergyComputable {

		/**
		 * Width of the given image for the energy.
		 */
		protected int width = 0;
		/**
		 * Height of the given image for the energy.
		 */
		protected int height = 0;

		/**
		 * Scaling factor to rescale image coordinates in range [0,1] or several
		 * ranges to the original coordinates range like [1000, 1000] in a image of
		 * size 1000 x 1000. Default scaling factor is 1.
		 */
		protected double scaleFactor = 1.0;

		/*
		 * Private helper variables.
		 */

		/**
		 * Mode of normalization.
		 */
		protected EnergyNormalizationMode normMode = 
			EnergyNormalizationMode.NORM_BALANCED_DERIVATIVES;
		
		/**
		 * Normalization factor for scaling matrix entries. 
		 */
		protected double normalizationFactor = 1.0;

		/**
		 * Set the scaling factor.
		 * 
		 * @param s
		 *          new scaling factor.
		 */
		@Override
    public void setScaleFactor(double s) {
			this.scaleFactor = s;
		}

		/**
		 * Get scaling factor.
		 * 
		 * @return Scaling factor.
		 */
		@Override
    public double getScaleFactor() {
			return this.scaleFactor;
		}

		@Override
		public boolean initEnergy(SnakeOptimizerSingle o) {
			/*
			 * Normalize the energy value in a specific range, but values must 
			 * all be in range [-1.0, 1.0]
			 */
			this.normalizeEnergy();
			return true;
		}

		@Override
		public final Matrix getDerivative_MatrixPart(SnakeOptimizerSingleVarCalc opt) {
				return null;
		}

		@Override
		// TODO Birgit: deprecated... muss in die Subklassen ausgelagert werden
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
				case NORM_NONE:
				case NORM_BALANCED_DERIVATIVES:
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

		@Override
		public double calcEnergy(SnakeOptimizerSingle opt) {
			MTBSnake snake = opt.getCurrentSnake();
			double eEnergy = 0.0;
			Vector<Point2D.Double> snakePoints = snake.getPoints();
			for (Point2D.Double p : snakePoints) {
				eEnergy += this.getValue_norm(p.x, p.y);
			}
			return eEnergy;
		}

		/**
		 * Calculates energy at a certain snake point.
		 * @param opt				Snake optimizer.
		 * @param pointID		Point ID where to calculate local energy.
		 * @return	Energy value at location with given ID.
		 */
		public double calcEnergy(SnakeOptimizerSingle opt, int pointID) {
				MTBSnake s = (MTBSnake)opt.getCurrentSnakes().elementAt(0);
				Point2D.Double p = s.getPoints().get(pointID);
				return this.getValue_norm(p.x, p.y);
		}

		/**
		 * Returns the value of the external energy at the given position on a
		 * normalizes image coordinates in range [width*scale, height*scale].
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return Absolute value of external energy.
		 */
		public double getValue_norm(double x, double y) {
				double xNew = x * this.scaleFactor;
				double yNew = y * this.scaleFactor;
				return (getValue(xNew, yNew));
		}

		/**
		 * Get x-derivative of external snake energy at given position on a normalizes
		 * image coordinates in range [width*scale, height*scale].
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return x-derivative value at given scaled position.
		 */
		public double getDerivativeX_norm(double x, double y) {
				double xNew = x * this.scaleFactor;
				double yNew = y * this.scaleFactor;
				return (getDerivativeX(xNew, yNew));
		}

		/**
		 * Get y-derivative of external snake energy at given position on a normalizes
		 * image coordinates in range [width*scale, height*scale].
		 * 
		 * @param x
		 *          x-coordinate of position
		 * @param y
		 *          y-coordinate of position
		 * @return y-derivative value at given scaled position.
		 */
		public double getDerivativeY_norm(double x, double y) {
				double xNew = x * this.scaleFactor;
				double yNew = y * this.scaleFactor;
				return (getDerivativeY(xNew, yNew));
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
		public abstract double getValue(double x, double y);

		/**
		 * Get x-derivative of external snake energy at given position.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return x-derivative value at given position.
		 */
		public abstract double getDerivativeX(double x, double y);

		/**
		 * Get y-derivative of external snake energy at given position.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return y-derivative value at given position.
		 */
		public abstract double getDerivativeY(double x, double y);

		/**
		 * Normalize the external energy in a range [-1.0, 1.0].
		 * 
		 * The user implements how the energy is normalized. The range can also be
		 * [0.0, 1.0] or something else, but the range must be inside [-1.0, 1.0].
		 * 
		 * The normalization is called in the initEnergy method of the energy.
		 */
		protected abstract void normalizeEnergy();
		
		@Override
    public void updateStatus(SnakeOptimizerSingle o) {
	    // nothing to do here	    
    }
		
		@Override
    public boolean requiresCounterClockwiseContourSorting() {
			return false;
		}

		@Override
    public  boolean requiresOverlapMask() {
			return false;
		}
}
