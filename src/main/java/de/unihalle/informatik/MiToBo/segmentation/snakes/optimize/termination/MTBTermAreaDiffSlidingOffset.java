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

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination;

import java.util.LinkedList;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer.Snake_status;

/**
 * Check upon snake termination by area change within the snake.
 * 
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBTermAreaDiffSlidingOffset extends MTBTermination {

	private int timeOffset = 10;
	
	private int window = 11;
	
	private LinkedList<Integer> areas = new LinkedList<Integer>();

	private LinkedList<Double> meanAreas = new LinkedList<Double>();

	private int elemCount = 0;
	
	/**
	 * Area fraction factor.
	 */
	@ALDClassParameter(label="Area Fraction")
	private double areaFraction;

	/**
	 * Default constructor.
	 */
	public MTBTermAreaDiffSlidingOffset() {
		// nothing to do here...
	}
	
		/**
		 */
		public MTBTermAreaDiffSlidingOffset(double _areaFraction) {
				this.areaFraction = _areaFraction;
		}

		@Override
		public boolean init(SnakeOptimizerSingle opt) {
				this.optimizer = opt;
				return true;
		}

		/**
		 * Termination is done when the area difference of the former and the current
		 * snake is below a given fraction factor or if a maximum number of iterations
		 * is reached.
		 */
		@Override
		public Snake_status terminate() {
			
			int width = this.optimizer.getWorkingImage().getSizeX();
      int height = this.optimizer.getWorkingImage().getSizeY();
			int[][] maskNew = this.optimizer.getCurrentSnake().getBinaryMask(width, height);
			int snakeSize_new = 0;
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
						if (maskNew[y][x] > 0)
								snakeSize_new++;
				}
			}
			// add new element
    	this.areas.add(new Integer(snakeSize_new));
    	this.elemCount++;
      if (this.elemCount < this.window ) {
				return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
      }
      if (this.elemCount > this.window ) {
      	this.areas.pollFirst();
      	--this.elemCount;
      }
//      System.out.println("Elements: " + this.elemCount);
      
      // calc mean of elements
      double sum=0;
      for (int i=0;i<this.window;++i)
      	sum += this.areas.get(i).doubleValue();
      this.meanAreas.add(new Double(sum/this.window));

      if (this.meanAreas.size() < this.timeOffset+1)
				return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
      if (this.meanAreas.size() > this.timeOffset+1)
      	this.meanAreas.pollFirst();
      // check criterium
      double hSize= 
      	this.meanAreas.get(this.meanAreas.size()-1-this.timeOffset).doubleValue();
      double nSize=
      	this.meanAreas.get(this.meanAreas.size()-1).doubleValue();
      if (Math.abs(nSize - hSize)/hSize < 0.001)
      	return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
      return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;      	
		}

		/**
		 * Get area fraction.
		 * 
		 * @return Area Fraction.
		 */
		public double getAreaFraction() {
				return this.areaFraction;
		}

		@Override
		public String toString() {
				return ("MTBTermAreaDiffSlidingOffset [areaFraction=" + 
																										this.areaFraction +"]");
		}
		
		/**
		 * Clones this object.
		 * <p>
		 * Note that the {@link SnakeOptimizerSingle} object is left null! 
		 * You have to call {@link SnakeOptimizer.initOptimizer()} before 
		 * using the gamma updater.
		 */
	  /* (non-Javadoc)
	   * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.MTBGammaUpdate#clone()
	   */
	  @Override
		@SuppressWarnings("unused")
	  public MTBTermAreaDiffSlidingOffset clone() throws CloneNotSupportedException {
	  	MTBTermAreaDiffSlidingOffset newObj = 
	  		new MTBTermAreaDiffSlidingOffset(this.areaFraction);
	  	newObj.status = this.status;
	  	newObj.optimizer = null;
	  	newObj.timeOffset = this.timeOffset;
	  	newObj.window = this.window;
	  	return newObj;
		}
}
