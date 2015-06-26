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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;

/**
 * <pre>
 * Mock object for testing energies.
 * </pre>
 * Note: this class does not have any reasonable functionality, it is solely
 * dedicated to simulate a snake optimizer object for unit testing.
 * <p> 
 * Never use it outside of the JUnit testing framework in MiToBo!
 * 
 * @author moeller
 */
public class MockSnakeOptimizerSingleVarCalc 
	extends SnakeOptimizerSingleVarCalc {

	/********************************************************************/
  /*** Constructors and clone()-methods, validation routines.       ***/
  /********************************************************************/

	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException
	 */
	public MockSnakeOptimizerSingleVarCalc() throws ALDOperatorException {
		// nothing to do here
	}
	
  @Override
	protected void initOptimizer() throws MTBSnakeException {
  	
		// convert polygon to snake, if necessary...
		MTBSnake initS = null;
		if (this.initialSnakes.elementAt(0) instanceof MTBSnake)
			initS = (MTBSnake)this.initialSnakes.elementAt(0);
		else {
			Vector<Point2D.Double> pps = this.initialSnakes.elementAt(0).getPoints();
      Vector<MTBSnakePoint2D> sps = new Vector<MTBSnakePoint2D>();
      for (Point2D.Double pp: pps) {
      	sps.addElement(new MTBSnakePoint2D(pp.x, pp.y));
      }
      initS = new MTBSnake(sps, true);
		}
		
		// normalize the snake
		this.scaleFactor = 1.0;
		this.snake = new MTBSnake(initS.getSnakePoints(), initS.isClosed(),
				this.scaleFactor, false);
		this.snakeNum = 1;
		Vector<MTBActiveContourEnergy> eVec = this.energySet.getGenericEnergyList();
		for (MTBActiveContourEnergy ener : eVec) {
			if (ener instanceof MTBSnakeEnergyDerivable)
			((MTBSnakeEnergyDerivable)ener).setScaleFactor(this.scaleFactor);
		}
		
		// ask energies for additional data they require
		this.counterClockwiseSnakePointOrderRequested = false;
		for (MTBActiveContourEnergy ener : eVec) {
			if (!(ener instanceof MTBSnakeEnergyDerivable))
				continue;
			this.counterClockwiseSnakePointOrderRequested = 
				   this.counterClockwiseSnakePointOrderRequested 
				|| ((MTBSnakeEnergyDerivable)ener).
								requiresCounterClockwiseContourSorting();
		}

		// check if it is sorted correctly
		if (    this.snake.getPointNum() > 5
				&&  this.counterClockwiseSnakePointOrderRequested
				&& !this.snake.isOrderedCounterClockwise())
			this.snake.reversePolypoints();

		// initialize energies
		for (MTBActiveContourEnergy ener : eVec) {
			if (!(ener instanceof MTBSnakeEnergyDerivable))
				continue;
			if (!((MTBSnakeEnergyDerivable)ener).initEnergy(this))
				throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
						"Energy could not be initialized! +" + ener.toString());
		}
  }

	@Override
  protected boolean hasEnergies() {
		return true;
  }

	@Override
  public void setEnergySet(MTBSet_ActiveContourEnergy eSet) {
		if (eSet instanceof MTBSet_SnakeEnergyDerivable)
			this.energySet = (MTBSet_SnakeEnergyDerivable)eSet;
  }

	@SuppressWarnings("unused")
  @Override
  protected Snake_status doIteration() throws MTBException {
	  return Snake_status.SNAKE_SUCCESS;
  }
	  
}
