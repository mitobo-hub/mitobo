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

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyComputable;

/**
 * <pre>
 * 
 * Image contour segmentation using parametric snakes with greedy optimization.
 * 
 * This class provides methods to segment contours in an image based on
 * parametric active contour models, i.e. snakes. The snake optimization
 * is done based on a greedy algorithm. Its main intention is for comparison
 * and demontration purposes. You should always keep in mind that greedy
 * snake algorithms are slow - particularly since for each point movement
 * excessive local energy calculations have to be done. 
 * 
 * </pre>
 * 
 * 
 * @author Birgit Möller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
@ALDDerivedClass
public class SnakeOptimizerSingleGreedy extends SnakeOptimizerSingle {

	@Parameter(label = "List of Energies", direction = Direction.IN,
		required = true, dataIOOrder = 3, description = "List of snake energies.")
	protected MTBSet_SnakeEnergyComputable energies= null;

  /************************************************************************/
  /*** Internal helpers for optimization procedure. ***/
  /************************************************************************/

	/**
	 * List of normalized energy weights.
	 */
	protected transient double [] energyWeightsNormed = null;

  /***********************************************************************/
  /*** Data Access. ***/
  /***********************************************************************/

  /***********************************************************************/
  /*** Optimizer initialization. ***/
  /***********************************************************************/

	public SnakeOptimizerSingleGreedy() throws ALDOperatorException{
		this.normMode = EnergyNormalizationMode.NORM_NONE;
		this.resampleSegLength = new Double(25.0);
	}
	
  /**
   * Initializes the optimizer.
   * <p>
   * Here internal member variables are initialized according to the given
   * parameters, and memory for intermediate results and debug data is
   * allocated.
   * @throws {@link IllegalArgumentException} 
   * @throws {@link MTBSnakeException} 
   */
  @Override
  protected void initOptimizer() 
  		throws MTBSnakeException, IllegalArgumentException {

		// do initialization of super class
		super.initOptimizer();

    // reset iterations counter, init local variables
    this.itCounter = 0;
    
		// convert polygon to snake, if necessary...
		if (this.initialSnakes.elementAt(0) instanceof MTBSnake)
			this.snake = (MTBSnake)this.initialSnakes.elementAt(0);
		else {
			Vector<Point2D.Double> pps = this.initialSnakes.elementAt(0).getPoints();
      Vector<MTBSnakePoint2D> sps = new Vector<MTBSnakePoint2D>();
      for (Point2D.Double pp: pps) {
      	sps.addElement(new MTBSnakePoint2D(pp.x, pp.y));
      }
      this.snake = new MTBSnake(sps, true);
		}
		this.snakeNum = 1;
		this.scaleFactor = 1.0;
 		Vector<MTBSnakeEnergyComputable> eVec = this.energies.getEnergyList();
		for (MTBSnakeEnergyComputable ener : eVec) {
			ener.setScaleFactor(this.scaleFactor);
		}

		// ask energies for additional data they require
		this.counterClockwiseSnakePointOrderRequested = false;
		for (MTBSnakeEnergyComputable ener : eVec) {
			this.counterClockwiseSnakePointOrderRequested = 
				   this.counterClockwiseSnakePointOrderRequested 
				|| ener.requiresCounterClockwiseContourSorting();
		}
		
    // check if it is sorted correctly
//    try {
      if (    this.snake.getPointNum() > 5 
      		&&  this.counterClockwiseSnakePointOrderRequested
//          && !this.snake.jni_isCounterClockwiseOriented())
      		&& !this.snake.isOrderedCounterClockwise())
        this.snake.reversePolypoints();
//    } catch (MTBPolygon2DException e) {
//      System.out.println(">>> " + e.getError());
//    }

    if (this.snake.getPointNum() > 5 && this.doResampling.booleanValue())
      this.resampleSnake();

    // initialize energies
		for (MTBSnakeEnergyComputable ener : eVec) {
			if (!ener.initEnergy(this))
				throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
						"Energy could not be initialized! +" + ener.toString());
		}

		// normalize energy weights
		this.energyWeightsNormed = new double[eVec.size()];
		double wSum = 0.0;
		for (int i=0;i<eVec.size();++i) {
			this.energyWeightsNormed[i] = this.energies.getWeight(i).doubleValue();
			wSum += this.energyWeightsNormed[i];
		}
		for (int i=0;i<eVec.size();++i) {
			this.energyWeightsNormed[i] /= wSum;
		}

    // display current snake energy
    if (this.verbose.booleanValue())
    	System.out.println("Initial snake energy: " + this.energy);

    // setup table for storing energy data if requested
		if (this.sampleEnergyData.booleanValue()) {
			this.setupEnergyTable();
		}

    if (this.verbose.booleanValue())
    	this.printParams();
  }

  /***********************************************************************/
  /*** Optimization. ***/
  /***********************************************************************/

  /**
   * Performs one single optimization step, i.e. iterates once over all points.
   * 
   * @return Status of the optimization process.
   * @throws ALDOperatorException 
   */
  @Override
  protected Snake_status doIteration() throws MTBSnakeException {

    // check if you have enough snake points, if not, stop
    if (this.snake.getPointNum() <= 5)
      return Snake_status.SNAKE_FAIL;

		// save current snake data for later reference
		this.previousSnake = this.snake.clone();

    // update energies
    Vector<MTBSnakeEnergyComputable> eVec = this.energies.getEnergyList();
    for (int i= 0; i<eVec.size(); ++i) {
    	MTBSnakeEnergyComputable ener = eVec.get(i);
    	ener.updateStatus(this);
    }

		// increase iteration counter
    this.itCounter++;

    // move each point to local minimum in neighborhood
    int pointsMoved= 0;
    int shiftDist = 1;
    int spointsNum = this.snake.getPoints().size();
    for (int i=0; i<spointsNum; ++i) {
      Vector<MTBSnakePoint2D> newpoints = new Vector<MTBSnakePoint2D>();
      Vector<Point2D.Double> scpoints = this.snake.getPoints();
      for (int j=0; j<scpoints.size(); ++j) {
      	newpoints.add(new MTBSnakePoint2D(scpoints.get(j)));
      }
    	// update energies
    	for (MTBSnakeEnergyComputable ener : eVec) {
  			ener.updateStatus(this);
  		}
    	double startE= this.calcSnakeEnergy();
    	double Emin= startE;
    	double nx, ny;
    	int minDx= 0; int minDy= 0;
    	for (int dy= -shiftDist; dy<=shiftDist; dy += shiftDist) {
    		for (int dx= -shiftDist; dx<=shiftDist; dx += shiftDist) {
    			nx = scpoints.get(i).x*this.scaleFactor + dx;
    			ny = scpoints.get(i).y*this.scaleFactor + dy;
    			// skip points that would be shifted outside the image domain
    			if (   nx < 0 || nx >= this.iWidth || ny < 0 || ny >= this.iHeight)
    				continue;
    			newpoints.get(i).x = nx;
    			newpoints.get(i).y = ny;
        	this.snake = new MTBSnake(newpoints, true, this.scaleFactor, true);
        	// update energies
        	for (MTBSnakeEnergyComputable ener : eVec) {
      			ener.updateStatus(this);
      		}
    			double E= this.calcSnakeEnergy();
    			if (E < Emin) {
    				Emin= E;
    				minDx= dx;
    				minDy= dy;
    			}
    		}
    	}
    	if (Emin < startE && (minDx != 0 || minDy != 0)) {
    		newpoints.get(i).x = scpoints.get(i).x*this.scaleFactor + minDx;
    		newpoints.get(i).y = scpoints.get(i).y*this.scaleFactor + minDy;
    		pointsMoved++;
    		if (this.verbose.booleanValue())
    			System.out.println("Energy decreases shifting point " + i 
    				+ " => new energy = " + Emin);
    	}
    	else {
    		newpoints.get(i).x = scpoints.get(i).x*this.scaleFactor;
    		newpoints.get(i).y = scpoints.get(i).y*this.scaleFactor;		
    	}
    	this.snake = new MTBSnake(newpoints, true, this.scaleFactor, true);
    }
//    this.snake.printPoints();
//    try {
//	    System.in.read();
//    } catch (IOException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//    }
		/*
		 * Update the snake: set new snake point coordinates and update the old
		 * snake point indices for every snake point. The "new" old index is set to
		 * the index of this point in the previous snake.
		 */
//		for (int i = 0; i < this.snake.getPoints().size(); ++i) {
//			this.snake.getPoints().get(i).
//			po.setOldId(i);
//			newpoints.add(po);
//		}

		/*
		 * Calculate energy value for current snake if needed.
		 */
		if (   this.sampleEnergyData.booleanValue() 
				|| this.energyCalculationRequested) {
			// calculate new energy
			this.energy = this.calcSnakeEnergy();
		}
		this.energy = this.calcSnakeEnergy();

		// resample the snake in each second iteration
		if (this.doResampling.booleanValue() && (this.itCounter % 2 == 0)) {
			if (this.verbose.booleanValue())
				System.out.println("Doing resampling...");
			this.resampleSnake();
		}

		// make current snakes available to the outside
		MTBPolygon2DSet set = 
			new MTBPolygon2DSet(0, 0, this.iWidth-1, this.iHeight-1);
		set.add(this.snake);
		this.outSnakes = set; 

    if (pointsMoved < 1) {
    	return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
    }
		if (this.snake.getPointNum() <= 5)
			return SnakeOptimizerSingle.Snake_status.SNAKE_FAIL;
    return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
  }

	@Override
  public SnakeOptimizerSingle clone(){
		SnakeOptimizerSingleGreedy newOpt;
		try {
			newOpt = new SnakeOptimizerSingleGreedy();
			newOpt.inImg = this.inImg;
			newOpt.initialSnakes = this.initialSnakes;
			newOpt.showIntermediateResults= this.showIntermediateResults;
			newOpt.saveIntermediateResults= this.saveIntermediateResults;
			newOpt.saveIntermediateResultsPath = this.saveIntermediateResultsPath;
			newOpt.resampleSegLength= this.resampleSegLength;
			newOpt.doResampling= this.doResampling;
			newOpt.energies= this.energies;
			return newOpt;
		} catch (ALDOperatorException e) {
			System.err.println("[SnakeOptimizerSingleGreedy] something went wrong "
				+ "during cloning the object... returning null!");
			e.printStackTrace();
			return null;
		}
  }

	@Override
  protected boolean hasEnergies() {
		return this.energies != null;
	}

	@Override
	public void setEnergySet(MTBSet_ActiveContourEnergy eSet) {
		if (eSet instanceof MTBSet_SnakeEnergyComputable)
			this.energies = (MTBSet_SnakeEnergyComputable)eSet;
	}

	@Override
  public MTBSet_SnakeEnergyComputable getEnergySet() {
		return this.energies;
	}
	
	/**
	 * Print important parameters to standard output stream.
	 */
	@Override
  public void printParams() {
		System.out.println("Snake number= " + this.snakeNum);
		System.out.println("Resample snake= " + this.doResampling);
		System.out.println("Resampling segment length= " + this.resampleSegLength);	
	}

  /***********************************************************************/
  /*** Other utilities. ***/
  /***********************************************************************/
	
	/**
	 * Calculates total snake energy.
	 * @return Value of local energy, -1 if error during calculation.
	 */
	@SuppressWarnings("null")
  protected double calcSnakeEnergy() {
		double sEnergy = 0.0;
		
		// process all internal energies
		if (this.energies == null)
			return -1.0;
		
		Object [] data = null; 
		if (this.sampleEnergyData.booleanValue()) {
			data = new Object[this.energies.getEnergyList().size()+3];
			data[0] = new Double(this.itCounter);
		}
		
		double energyVal = 0.0;
		int c=3;
		Vector<MTBSnakeEnergyComputable> eVec = this.energies.getEnergyList(); 
		Vector<Double> wVec = this.energies.getWeights();
		int i=0;
		for (MTBSnakeEnergyComputable ener : eVec) {
			energyVal = ener.calcEnergy(this);
			sEnergy += wVec.elementAt(i).doubleValue()*energyVal;
			if (this.sampleEnergyData.booleanValue()) {
				data[c]= new Double(energyVal);
			}
			++c;
			++i;
		}
		if (this.sampleEnergyData.booleanValue()) {
			data[1] = new Double(sEnergy);
			data[2] = new Double(sEnergy/this.snake.getPointNum());
			Vector<Object[]> datVec = new Vector<Object[]>();
			datVec.add(data);
			this.energyData.insertData(datVec);
		}
		return sEnergy;
	}
	
	/**
	 * Inits the table for sampled energy values.
	 */
	protected void setupEnergyTable() {
		this.energyData = 
			new MTBTableModel(1, this.energies.getEnergyList().size() + 3);
		this.energyData.setColumnName(0, "Iteration");
		this.energyData.setColumnName(1, "Sum");
		this.energyData.setColumnName(2, "per Point");
		int i=3;
		for (MTBSnakeEnergyComputable ener: this.energies.getEnergyList()) {
			this.energyData.setColumnName(i, ener.getClass().getSimpleName());
			++i;
		}
	}
}
