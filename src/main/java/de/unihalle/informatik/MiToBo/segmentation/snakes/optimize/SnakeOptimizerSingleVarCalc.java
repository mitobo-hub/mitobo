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
import java.util.*;

import Jama.Matrix;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBException;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.*;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination.*;
import de.unihalle.informatik.MiToBo.tools.system.UserTime;

/**
 * <pre>
 * 
 * Image contour segmentation using parametric snakes.
 * 
 * This class provides methods to segment contours in an image based on
 * parametric active contour models, i.e. snakes. Note that we assume here a
 * counter-clockwise ordering of the snake points. Pay attention if you deal
 * with upper-left coordinates!
 * <p>
 * In this class snake optimization is done by solving PDEs like in the 
 * original Kass et al. paper.
 * 
 * TODO correct handling of not closed snakes
 * TODO adaptation of gamma for every single point
 * TODO adaptation of segment length
 * 
 * </pre>
 * 
 * 
 * @author moeller, misiak
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level= Level.STANDARD,
		shortDescription="Image contour segmentation with parametric snakes.")
@ALDDerivedClass
public class SnakeOptimizerSingleVarCalc extends SnakeOptimizerSingle {

	/**
	 * List of snake energies to be used in segmentation.
	 */
	@Parameter(label="List of Energies", mode= ExpertMode.STANDARD,
			direction=Parameter.Direction.IN, required=true,
			description = "List of snake energies.", dataIOOrder = 3)
	protected MTBSet_SnakeEnergyDerivable energySet= null;

	/**
	 * Gamma update strategy.
	 */
	@Parameter(label="Gamma Update Strategy", mode= ExpertMode.ADVANCED,
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 22,
			description = "Gamma update object.")
	protected MTBGammaUpdate gammaUpdater= new MTBGammaNone();

	/**
	 * Termination criterion.
	 */
	@Parameter(label="Termination Criterion", mode= ExpertMode.ADVANCED,
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 20,
			description = "Termination criterion for optimization.")
	protected MTBTermination termCriterion= new MTBTermMaxIterations(100);
		    
	/**
	 * Initial step size in snake optimization.
	 */
	@Parameter(label="Initial Gamma Value", mode= ExpertMode.STANDARD,
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 21,
			description = "Initial step sizes.")
	protected Double initialGammas= new Double(0.5); 

	/************************************************************************/
	/*** Internal helpers for optimization procedure. ***/
	/************************************************************************/

	/**
	 * List of normalized energy weights.
	 */
	protected transient double [] energyWeightsNormed = null;

	/**
	 * Vector containing the current gamma values (step sizes).
	 * <p>
	 * Note that the array is two-dimensional, however, the number of columns is
	 * always one! This is mainly for compatibility reasons as we need to convert
	 * the double array to Jama matrices which require two-dimensional arrays.
	 */
	protected transient double[][] gammaAdaptive = null;

	/**
	 * Optimization matrix A, to be modified by snake energies in each iteration.
	 */
	private transient Matrix A = null;

	/**
	 * Optimization matrix with values of last calculations.
	 */
	private transient Matrix memA = null;

	/**
	 * External energy vector, to be modified by snake energies.
	 */
	private transient Matrix B = null;

//	protected double minEnergy = Double.MAX_VALUE;
//	protected double minEnergyOld = Double.MAX_VALUE;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public SnakeOptimizerSingleVarCalc() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor with parameters.
	 * 
	 * @param img
	 *          Gray-scale input image.
	 * @param initSnake
	 *          Array of initial snake polygon(s).
	 * @param energs
	 *          Vector of energy objects.
	 * @param gamUpdater
	 *          Object for gamma adaptation.
	 * @param gammas
	 *          Initial value(s) for step size.
	 * @param tc
	 *          Termination checker.
	 * @param opStepWise
	 *          Enables step-through optimization (requires GUI).
	 * @param stepsize
	 *          Number of iterations per step-through step.
	 * @param normalize
	 *          Flag for enabling snake/image normalization.
	 * @param resample
	 *          Flag for activating snake resampling.
	 * @param segLength
	 *          Desired length of segments are resampling.
	 * @throws ALDOperatorException
	 * 
	 * @throws SnakeAlgosRegionException
	 */
	public SnakeOptimizerSingleVarCalc(MTBImage img, MTBPolygon2DSet initSnake,
			MTBSet_SnakeEnergyDerivable energs, MTBGammaUpdate gamUpdater,
			Double gammas, MTBTermination tc, 
			Boolean resample, Double segLength)
	throws ALDOperatorException {
		this.inImg = img;
		this.initialSnakes = initSnake;
		this.energySet = energs;
		this.gammaUpdater = gamUpdater;
		this.initialGammas = gammas;
		this.termCriterion = tc;
		this.doResampling = resample;
		this.resampleSegLength = segLength;
	}

	@Override
  public SnakeOptimizerSingleVarCalc clone() {
		try {
			SnakeOptimizerSingleVarCalc newOpt = new SnakeOptimizerSingleVarCalc();
			newOpt.inImg = this.inImg;
			newOpt.initialSnakes = this.initialSnakes;
			newOpt.showIntermediateResults= this.showIntermediateResults;
			newOpt.saveIntermediateResults= this.saveIntermediateResults;
			newOpt.saveIntermediateResultsPath = this.saveIntermediateResultsPath;
			newOpt.resampleSegLength= this.resampleSegLength;
			newOpt.doResampling= this.doResampling;
//			newOpt.outSnakesStackWanted= this.outSnakesStackWanted;
			// TODO besser clonen?
			newOpt.energySet= this.energySet;
			try {
	      newOpt.gammaUpdater= this.gammaUpdater.clone();
      } catch (CloneNotSupportedException e) {
      	System.out.println("SnakeOptimizerSinglePDE::clone - " +
  			" Cannot clone MTBGammaUpdate object, returning!");
      	return null;
      }
			try {
	      newOpt.termCriterion= this.termCriterion.clone();
      } catch (CloneNotSupportedException e) {
      	System.out.println("SnakeOptimizerSinglePDE::clone - " +
  			" Cannot clone MTBTermination object, returning!");
      	return null;
      }
			newOpt.initialGammas= this.initialGammas; 
			return newOpt;
    } catch (ALDOperatorException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
    }
	}
	
  /***********************************************************************/
  /*** Helper method for object serialization.                         ***/
  /***********************************************************************/
	
	/**
	 * Function for proper initialization of deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being deserialized
	 * from file, prior to handing the instance over to the user. It takes care
	 * of a proper initialization of transient member variables as they are not
	 * initialized to the default values during deserialization. 
	 * @return
	 */
	@Override
  protected Object readResolve() {
		super.readResolve();
//		// super class members
//		this.snakeNum= 1;
//		this.itCounter = 0;
//		this.counterClockwiseSnakePointOrderRequested = false;
		// members of this class
		this.energyCalculationRequested = false;
		this.energy = -1.0;
		this.previousEnergy = -1.0;
		this.scaleFactor = 1.0;
		this.timer = new UserTime();
		return this;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		super.validateCustom();
		if (this.energySet == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[SnakeOptimizerSingleVarCalc] no energy set given!");
		if (this.termCriterion == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[SnakeOptimizerSingleVarCalc] no termination criterion given!");
		if (this.gammaUpdater == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[SnakeOptimizerSingleVarCalc] no gamma updater given!");
	}

	/***********************************************************************/
	/*** Utilities. ***/
	/***********************************************************************/

	@Override
  protected boolean hasEnergies() {
		return this.energySet != null;
	}
	
	/**
	 * Print important parameters to standard output stream.
	 * 
	 * @throws ALDOperatorException
	 */
	@Override
  public void printParams() {
		super.printParams();
		System.out.println("Snake number= " + this.snakeNum);
		System.out.println("\u03B3 = " + this.initialGammas);
		System.out.println("\u03B3-update= " + this.gammaUpdater.toString());
		System.out.println("Termination criterion= "+this.termCriterion.toString());
		Vector<MTBSnakeEnergyDerivable> eVec = this.energySet.getEnergyList();
		for (MTBSnakeEnergyDerivable ener : eVec) {
			System.out.println(ener.toString());
		}
		System.out.println("Resample snake= " + this.doResampling);
		System.out.println("Resampling segment length= " + this.resampleSegLength);
	}

  @Override
  public String toString() {
  	return new String("SnakeOptimizerSinglePDE - object");
  }

	/***********************************************************************/
	/*** Optimizer initialization. ***/
	/***********************************************************************/

  /**
	 * Initializes the optimizer.
	 * <p>
	 * Here internal member variables are initialized according to the given
	 * parameters, and memory for intermediate results and debug data is
	 * allocated.
	 */
  @Override
	protected void initOptimizer() throws MTBSnakeException {
		
		// do initialization of super class
		super.initOptimizer();
		
		// init excludeMask -> per default all pixels are included
		boolean [][] exMask = new boolean[this.iHeight][this.iWidth];
		for (int y=0;y<this.iHeight;++y) {
			for (int x=0;x<this.iWidth;++x) {
				exMask[y][x] = false;
			}
		}
		this.excludeMask = exMask;						

		// snakes are required and should not be null
//		if (this.initialSnakes == null) {
//			System.out.println("SnakeOptimizerSingle: error - snake list is null!");
//			return;
//		}

		// extract vector of energy objects
		Vector<MTBSnakeEnergyDerivable> eVec = this.energySet.getEnergyList();

		// reset iterations counter
		this.itCounter = 0;
		this.printParams();
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
		MTBSnake tmpSnake = initS.clone();
		tmpSnake.denormalize();
		if (this.verbose.booleanValue())
			System.out.println("Doing normalization...");
		this.scaleFactor = Math.max(this.iWidth, this.iHeight);
		this.snake = new MTBSnake(tmpSnake.getSnakePoints(), initS.isClosed(),
				this.scaleFactor, false);
		this.snakeNum = 1;
		for (MTBSnakeEnergyDerivable ener : eVec) {
			ener.setScaleFactor(this.scaleFactor);
		}

		// ask energies for additional data they require
		this.counterClockwiseSnakePointOrderRequested = false;
		for (MTBSnakeEnergyDerivable ener : eVec) {
			this.counterClockwiseSnakePointOrderRequested = 
				   this.counterClockwiseSnakePointOrderRequested 
				|| ener.requiresCounterClockwiseContourSorting();
		}

		// check if it is sorted correctly
//		try {
			if (    this.snake.getPointNum() > 5
					&&  this.counterClockwiseSnakePointOrderRequested
//					&& !this.snake.jni_isCounterClockwiseOriented())
					&& !this.snake.isOrderedCounterClockwise())
				this.snake.reversePolypoints();
//		} catch (MTBPolygon2DException e) {
//			System.out.println(">>> " + e.getError());
//		}

		if (this.snake.getPointNum() <= 5 || this.doResampling.booleanValue())
			this.resampleSnake();

		// initialize gamma vector
		this.gammaAdaptive = new double[this.snake.getPointNum() * 2][1];
		for (int i = 0; i < this.snake.getPointNum() * 2; ++i)
			this.gammaAdaptive[i][0] = this.initialGammas.doubleValue();

		// initialize energies
		for (MTBSnakeEnergyDerivable ener : eVec) {
			if (!ener.initEnergy(this))
				throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
					"Energy could not be initialized! +" + ener.toString());
		}

		// normalize energy weights
		this.energyWeightsNormed = new double[eVec.size()];
		double wSum = 0.0;
		for (int i=0;i<eVec.size();++i) {
			this.energyWeightsNormed[i] = this.energySet.getWeight(i).doubleValue();
			wSum += this.energyWeightsNormed[i];
		}
		for (int i=0;i<eVec.size();++i) {
			this.energyWeightsNormed[i] /= wSum;
		}

		// init the termination checker object
		if (!this.termCriterion.init(this))
			throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
					"Term-checker init failed!");

		// init the gamma update object
		if (!this.gammaUpdater.init(this))
			throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
					"GammaUpdater init failed!");

    // setup table for storing energy data if requested
		if (this.sampleEnergyData.booleanValue()) {
			// calculate energy
			this.setupEnergyTable();
		}
	}

	/***********************************************************************
	 * Optimization: 
	 ***********************************************************************/

	/***********************************************************************/
	/*** Operator routines. ***/
	/***********************************************************************/

	/**
	 * Performs one single optimization step.
	 * 
	 * @return Status of the optimization process.
	 */
	@SuppressWarnings("unused")
	@Override
	protected Snake_status doIteration() throws MTBException {

		// reset timer
		this.timer.reset();
		
		// check if you have enough snake points, if not, stop
		if (this.snake.getPointNum() <= 5)
			return Snake_status.SNAKE_FAIL;

		// increase iteration counter
		this.itCounter++;
		System.out.println("\n--- Iteration " + this.itCounter + " ---");

		// save current snake data for later reference
		this.previousSnake = this.snake.clone();

		// init matrices A (mostly int. energies) and B (mostly ext. energies)
		this.resizeMatrices();

		// process all energies
		//				if (eVec == null || eVec.isEmpty() || eVec.size() != eWeights.size())
		//						return Snake_status.SNAKE_FAIL;
		Vector<MTBSnakeEnergyDerivable> eVec = this.energySet.getEnergyList();
		double energyval = 0.0;
		for (int i= 0; i<eVec.size(); ++i) {
			MTBSnakeEnergyDerivable ener = eVec.get(i);
			ener.updateStatus(this);

			Matrix A_ener = ener.getDerivative_MatrixPart(this);
			if (A_ener != null)
				this.A = 
					this.A.plus(A_ener.times(this.energyWeightsNormed[i]));
			Matrix B_ener = ener.getDerivative_VectorPart(this);
			if (B_ener != null)
				this.B = 
					this.B.plus(B_ener.times(this.energyWeightsNormed[i]));
		}
		if (this.A == null || this.B == null)
			return Snake_status.SNAKE_FAIL;
	
		/*
		 * Calculate energy value for current snake if needed.
		 */
		if (   this.sampleEnergyData.booleanValue() 
				|| this.energyCalculationRequested) {
			// calculate new energy
			this.energy = this.calcSnakeEnergy();
		}
		/*
		 * Print console outputs.
		 */
		if (this.verbose.booleanValue()) {
			System.out.println("    Energy prior to optimizatio =  " + this.energy);
			System.out.println("    Pointnum = " + this.snake.getPointNum());
			System.out.println("    \u03B3 = " + this.gammaAdaptive[0][0]);
		}
		
		// calculate new snake points
		double[][] identMatrix = 
			new double[this.snake.getPointNum() * 2][this.snake.getPointNum() * 2];
		for (int i = 0; i < identMatrix.length; i++) {
			for (int j = 0; j < identMatrix.length; j++) {
				if (i == j)
					identMatrix[i][j] = 1;
				else
					identMatrix[i][j] = 0;
			}
		}
		Matrix I = new Matrix(identMatrix);

		// create gamma matrix
		double[][] oneMatrix = new double[1][2 * this.snake.getPointNum()];
		for (int i = 0; i < 2 * this.snake.getPointNum(); ++i)
			oneMatrix[0][i] = 1.0;
		Matrix Ones = new Matrix(oneMatrix);
		Matrix Gamma = (new Matrix(this.gammaAdaptive)).times(Ones);

		// calculate denominator
		Matrix H = I.plus(this.A.arrayTimes(Gamma));

		// create vector with x and y coordinates
		double[][] x_vec = new double[this.snake.getPointNum() * 2][1];

		int snakePointNum = this.snake.getPointNum();
		for (int i = 0; i < snakePointNum; i++) {
			double xx = this.snake.getPoints().elementAt(i).getX();
			double yy = this.snake.getPoints().elementAt(i).getY();
			x_vec[i][0] = xx;
			x_vec[i + snakePointNum][0] = yy;
		}
		// vector with old coordinates
		Matrix Kx = new Matrix(x_vec);

		// update external energy vector and add it: external energies
		Matrix Nx = null;
		// put gammas into a vector
		Matrix GammaVec = new Matrix(this.gammaAdaptive);
		Nx = Kx.minus(GammaVec.arrayTimes(this.B));
		
//		for (int i=0;i<50;++i)
//			System.out.println(0.5 * this.B.get(i, 0));
//		System.out.println("=======");
//		for (int i=0;i<50;++i)
//			System.out.println(Nx.get(i, 0));

		// calculate new points by matrix multiplication:
		// changes for speed-up by omitting inversion on Oct 21, 2011
		Matrix newPoints = H.solve(Nx);

//		System.out.println("=======");
//		for (int i=0;i<50;++i)
//			System.out.println(newPoints.get(i, 0));
//		try {
//	    System.in.read();
//    } catch (IOException e1) {
//	    // TODO Auto-generated catch block
//	    e1.printStackTrace();
//    }

		/*
		 * Update the snake: set new snake point coordinates and update the old
		 * snake point indices for every snake point. The "new" old index is set to
		 * the index of this point in the previous snake.
		 */
		Vector<MTBSnakePoint2D> newpoints = new Vector<MTBSnakePoint2D>();
		for (int i = 0; i < snakePointNum; ++i) {
			MTBSnakePoint2D po = new MTBSnakePoint2D(newPoints.get(i, 0), newPoints
					.get(i + snakePointNum, 0));
			po.setOldId(i);
			newpoints.add(po);
//			 System.out.println(i + ": (" +
//			 this.snake.getSnakePoints().elementAt(i).x
//			 + "," + this.snake.getSnakePoints().elementAt(i).y + ","
//			 + this.snake.getSnakePoints().elementAt(i).getOldId() + ") ; ("
//			 + po.x + "," + po.y + "," + po.getOldId() + ")");
		}
		// remember matrix A
		this.memA = (Matrix) this.A.clone();

		// set new snake
		MTBSnake newSnake = new MTBSnake(newpoints, true, this.scaleFactor, true);
		
		
		/* calc point shifts! */
//		double shiftSum = 0;
//		double shiftMax = 0;
//		for (int i = 0; i < this.snake.getPointNum(); ++i) {
//			MTBSnakePoint2D po = this.snake.getSnakePoints().elementAt(i);
//			MTBSnakePoint2D pn = newSnake.getSnakePoints().elementAt(i);
//			double shift = 
//				Math.sqrt((pn.x-po.x)*(pn.x-po.x) + (pn.y-po.y)*(pn.y-po.y));
//			shiftSum += shift;
//			if (shift > shiftMax) 
//				shiftMax = shift;
//		}		
//		double shiftMean = shiftSum/this.snake.getPointNum();
		/* up to here */
		

		this.snake = newSnake;

		// loop elimination, every 5 iterations
		// TODO check number of iterations
				if (this.itCounter % 1 == 0) {
//						if (!this.snake.jni_isSimple()) {
						if (!this.snake.isSimple()) {
								if (this.verbose.booleanValue())
										System.out.println("Snake is not simple: simplifying it...");
								this.snake.makeSimple();
						}
				}

		// check if snake is sorted correctly
		// -> only needed if requested by at least on energy
				if (this.verbose.booleanValue())
						System.out.println("Check correct order of points...");
				if (this.counterClockwiseSnakePointOrderRequested) {
//						try {
//								if (!this.snake.jni_isCounterClockwiseOriented()) {
								if (!this.snake.isOrderedCounterClockwise()) {
										if (this.verbose.booleanValue())
												System.out.println("After optimization: snake is in wrong order"
												    + ", reverting it...");
										this.snake.reversePolypoints();
								}
//						} catch (MTBPolygon2DException e) {
//								System.err.println(">>> " + e.getError());
//						}
				}

		// resample the snake in each second iteration
		if (this.verbose.booleanValue())
			System.out.println("Doing resampling...");
		if (this.doResampling.booleanValue() && (this.itCounter % 2 == 0)) {
			this.resampleSnake();
		}
		if (this.snake.getPointNum() <= 5)
			return SnakeOptimizerSingle.Snake_status.SNAKE_FAIL;

		// check if there are snake points outside of the image
		// => project them back to image border
		// ToDo @Danny: test for ID's, should be the same as before !!!
		newpoints = new Vector<MTBSnakePoint2D>();
		for (int i = 0; i < this.snake.getPointNum(); ++i) {
			MTBSnakePoint2D po = this.snake.getSnakePoints().elementAt(i);
			if (po.x < 0)
				po.x = 0;
			if (po.x * this.scaleFactor > this.iWidth - 1)
				po.x = (this.iWidth - 1) / this.scaleFactor;
			if (po.y < 0)
				po.y = 0;
			if (po.y * this.scaleFactor > this.iHeight - 1)
				po.y = (this.iHeight - 1) / this.scaleFactor;
			newpoints.addElement(po);

			// System.out.println(po.getOldId() +
			// this.snake.getPoints().get(i).getOldId);

		}
		newSnake = new MTBSnake(newpoints, true, this.scaleFactor, true);
		this.snake = newSnake;

		// boolean minEnergyChanged= false;
		// if (this.energy < this.minEnergy) {
		// this.minEnergyOld= this.minEnergy;
		// this.minEnergy= this.energy;
		// minEnergyChanged= true;
		// }

		// if (minEnergyChanged && this.minEnergyOld > this.minEnergy) {
		// double energyImprove= (this.minEnergyOld -
		// this.minEnergy)/this.minEnergyOld;
		// this.gammaAdaptive= this.gammaAdaptive*(1+2*energyImprove);
		// }

		// if (this.useInternalEnergies()) {
		// System.out.println("    length = " + this.calcEnergyInternal_lengthTerm()
		// + " , ");
		// System.out.println("    curv = " + this.calcEnergyInternal_curvTerm());
		// }
		// if (this.useExternalEnergies()) {
		// System.out.println("    ext = " + this.calcEnergyExternal());
		// this.energy = this.calcEnergyExternal();
		// }

		/*
		 * Use gamma adaptation.
		 */
		this.gammaAdaptive = this.gammaUpdater.adaptGamma();

		/*
		 * Use termination criteria.
		 */
		if (this.verbose.booleanValue())
			System.out.println("  Termination criterion: ");
		Snake_status termCheckStat = this.termCriterion.terminate();
		/*
		 * Store energy value of the snake before optimizing.
		 */
		if (this.energyCalculationRequested) {
			// store snake energy
			this.previousEnergy = this.energy;
		}
		if (this.verbose.booleanValue())
		System.out.println("  === End of loop === \n");
		
		// make current snakes available to the outside
		MTBPolygon2DSet set = 
			new MTBPolygon2DSet(0, 0, this.iWidth-1, this.iHeight-1);
		set.add(this.snake);
		this.outSnakes = set; 
		
		// stop timer and print time
		if (this.verbose.booleanValue())
		System.out.println("Iteration duration: " + this.timer.getElapsedTime());
		
//		System.out.println("Information: ");
//		System.out.println("Snake area = " + this.snake.getSignedArea());
//		System.out.println("Snake length = " + this.snake.getLength());
//		System.out.println("Max shift = " + shiftMax);
//		System.out.println("Total shift = " + shiftSum);
//		System.out.println("Average shift = " + shiftMean);
			
		if (   termCheckStat == Snake_status.SNAKE_DONE
				|| termCheckStat == Snake_status.SNAKE_FAIL) {
			if (   this.sampleEnergyData.booleanValue() 
					|| this.energyCalculationRequested) {
				// calculate new energy
				this.energy = this.calcSnakeEnergy();
			}
		}
		
		// return status value
		return termCheckStat;
	}

	/**
	 * Resize parameter matrices.
	 * <p>
	 * The size of the matrices depends on the number of points used to represent
	 * the snake. This methods resizes the snake, if the given point number does
	 * not coincide with the current size of the matrices. After resizing the
	 * matrices, entries are set to zero.
	 */
	public void resizeMatrices() {
		// TODO Muss noch raus, kürzere Snakes sollten kein Problem sein...
		if (this.snake.getPointNum() < 5)
			return;

		int snakePointNum = this.snake.getPointNum();
		if (this.A == null) {
			this.A = new Matrix(this.snake.getPointNum() * 2, this.snake
					.getPointNum() * 2);
		} else if (snakePointNum * 2 != this.A.getColumnDimension())
			this.A = new Matrix(snakePointNum * 2, snakePointNum * 2);
		for (int i = 0; i < snakePointNum * 2; ++i)
			for (int j = 0; j < snakePointNum * 2; ++j)
				this.A.set(i, j, 0);

		if (this.B == null) {
			this.B = new Matrix(this.snake.getPointNum() * 2, 1);
		} else if (snakePointNum * 2 != this.B.getRowDimension())
			this.B = new Matrix(snakePointNum * 2, 1);
		for (int i = 0; i < snakePointNum * 2; ++i) {
			this.B.set(i, 0, 0.0);
		}

		for (int i = 0; i < snakePointNum * 2; ++i) {
			if (this.B.get(i, 0) != 0)
				System.out.println("Error im resize, unequal to zero!");
		}
	}

	/**
	 * Inits the table for sampled energy values.
	 */
	protected void setupEnergyTable() {
		this.energyData = 
				new MTBTableModel(1, this.energySet.getEnergyList().size() + 3);
		this.energyData.setColumnName(0, "Iteration");
		this.energyData.setColumnName(1, "Sum");
		this.energyData.setColumnName(2, "per Point");
		int i=3;
		for (MTBSnakeEnergyDerivable ener: this.energySet.getEnergyList()) {
			this.energyData.setColumnName(i, ener.getClass().getSimpleName());
			++i;
		}
	}
	
	/************************************************************************/
	/*** Energy calculation. ***/
	/************************************************************************/

	/**
	 * Calculates total snake energy.
	 * 
	 * @return Value of local energy, -1 if error during calculation.
	 */
	@SuppressWarnings("null")
  protected double calcSnakeEnergy() {
		double sEnergy = 0.0;

		// process all internal energies
		if (this.energySet == null)
			return -1.0;
		
		Object [] data = null; 
		if (this.sampleEnergyData.booleanValue()) {
			data = new Object[this.energySet.getEnergyList().size()+3];
			data[0] = new Double(this.itCounter);
		}
		
		double energyVal = 0.0;
		int c=3;
		Vector<MTBSnakeEnergyDerivable> eVec = this.energySet.getEnergyList(); 
		Vector<Double> wVec = this.energySet.getWeights();
		int i=0;
		for (MTBSnakeEnergyDerivable ener : eVec) {
			energyVal = Double.NaN;
			if (ener instanceof MTBSnakeEnergyComputable) {
				energyVal = ((MTBSnakeEnergyComputable)ener).calcEnergy(this);
				sEnergy += wVec.elementAt(i).doubleValue()*energyVal;
			}
			if (this.sampleEnergyData.booleanValue()) {
				data[c]= new Double(energyVal);
			}
			++c;
			++i;
		}
		data[1] = new Double(sEnergy);
		data[2] = new Double(sEnergy/this.snake.getPointNum());
		Vector<Object[]> datVec = new Vector<Object[]>();
		datVec.add(data);
		this.energyData.insertData(datVec);
		return sEnergy;
	}

	/**
	 * Calculates snake energy at a certain snake point.
	 * 
	 * @param position
	 *          Index of snake point.
	 * @return Value of local energy, -1 if error during calculation.
	 */
//	protected double calcSnakeEnergy(int position) {
//		double sEnergy = 0.0;
//
//		// process all internal energies
//		Vector<MTBSnakeEnergyDerivable> eVec = this.energies.getEnergyList(); 
//		for (MTBSnakeEnergyDerivable ener : eVec) {
//			if (ener instanceof MTBSnakeEnergyComputable)
//				sEnergy += ((MTBSnakeEnergyComputable)ener).calcEnergy(this, position);
//		}
//		return sEnergy;
//	}

	/***********************************************************************/
	/*** Data Access. ***/
	/***********************************************************************/
	
	@Override
	public void setEnergySet(MTBSet_ActiveContourEnergy eSet) {
		if (eSet instanceof MTBSet_SnakeEnergyDerivable)
			this.energySet = (MTBSet_SnakeEnergyDerivable)eSet;
	}

	@Override
  public MTBSet_SnakeEnergyDerivable getEnergySet() {
		return this.energySet;
	}
	
	/**
	 * Returns a reference to the list of energies.
	 */
	public Vector<MTBSnakeEnergyDerivable> getEnergies() {
		return this.energySet.getEnergyList();
	}
	
	/**
	 * Returns current gamma vector.
	 */
	public double[][] getCurGamma() {
		return this.gammaAdaptive;
	}

	/**
	 * Returns matrix carrying always the values of the last iteration, or null
	 * before first run.
	 */
	public Matrix getMemorizedMatrixA() {
		return this.memA;
	}
	
	@Override
	public String getDocumentation() {
		return "<p>Parametric activce contours represent contours as sets of discrete 2D points. Based on application specific energy functionals image segmentation is accomplished by minimizing the functionals with common methods from numerical mathmatics. This optimizer for snake-based image segmentation applies methods from variational calculus to solve the optimization problem, i.e. uses Euler-Lagrange equations and gradient descent techniques. It evolves a single snake in a given image.</p>\n" + 
				"\n" + 
				"<p><b>Reference</b>:</p>\n" + 
				"\n" + 
				"<p>Michael Kass, Andrew Witkin and Demetri Terzopoulos,<br>\n" + 
				"\"Snakes: Active contour models\",<br>\n" + 
				"Int. Journal of Computer Vision, vol. 1, no. 4, pp. 321-331, 1988.</p>\n" + 
				"<h3>Required input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Input image</b>:</p>\n" + 
				"\n" + 
				"<p>Image to be processed, should be a one-layer gray-scale image.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Initial Snake(s)</b>:</p>\n" + 
				"\n" + 
				"<p>Set of initial snakes to be optimized. This optimizer is able to handle one snake  at a time, hence, if the list of polygons contains more than one object, all but the first one are ignored.<br>\n" + 
				"Initial snake contours can be loaded from different sources:\n" + 
				"<ul><li>\n" + 
				"<p>ROI Manager - import ImageJ selections as initial snake polygons</p>\n" + 
				"</li><li>\n" + 
				"<p>MTB&nbsp;XML - load polygons from a file in MiToBo XML format (see manual for details)</p>\n" + 
				"</li><li>\n" + 
				"<p>SERIAL&nbsp;XML - load polygons from a file in XStream serialized XML format</p>\n" + 
				"</li><li>\n" + 
				"<p>MANUAL - opens a window for manual input of polygon points<br>\n" + 
				"<br>\n" + 
				"Selecting the option \"ROI Manager\" and pressing the \"View\" button allows to export loaded snake contours to the ImageJ ROI manager for visualization. The edit button allows for manually modifying loaded contours, e.g. adding or erasing points.<br>\n" + 
				"The icon on the right of the buttons indicates whether contours have already been loaded or not.</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><b>List of Energies</b>:</p>\n" + 
				"\n" + 
				"<p>Snake-based image segmentation relies on a set of energies encoding specific characteristics of the target object that guide the segmentation.<br>\n" + 
				"<br>\n" + 
				"By clicking on the button \"Configure Energies...\" a window pops-up that allows to select a set of energies to be applied in the segmentation. Select an energy in the box on top and press \"Add selected energy...\". The selected energy will be added to the table at the bottom. Note that each energy can only be once added to the table, if it is selected and added a second time, nothing happens.<br>\n" + 
				"<br>\n" + 
				"The overall snake functional results from the sum of all selected energies. In addition, each energy can be individually weighted by assigning a weight to it in the table. The higher an energy's weight, the more influence the energy will have.<br>\n" + 
				"<br>\n" + 
				"Most energies offer configuration parameters. For setting these parameters select an energy from the table at the bottom and press the button \"Configure\". Another configuration window for the selected energy will pop-up which allows you to set its configuration parameters.<br>\n" + 
				"<br>\n" + 
				"To remove an energy from the table, select the corresponding row in the table and press the button \"Remove\".<br>\n" + 
				"</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Optional input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Image intensity normalization mode</b>:</p>\n" + 
				"\n" + 
				"<p>Image intensities are optionally normalized to a range of [0,1] or [-1,1] (if negative values are present in the image) prior to segmentation for simplifying parameter adjustment. The mode specifies how this normalization is done:\n" + 
				"<ul><li>\n" + 
				"<p>INTENSITY&nbsp;NORM&nbsp;NONE: no normalization is done</p>\n" + 
				"</li><li>\n" + 
				"<p>INTENSITY&nbsp;NORM&nbsp;THEORETIC&nbsp;RANGE: scale the image according to the theoretic range of possible values, i.e. for an image containing unsigned byte values the range of [0,255] is mapped to [0,1]</p>\n" + 
				"</li><li>\n" + 
				"<p>INTENSITY&nbsp;NORM&nbsp;TRUE&nbsp;RANGE: scale the image according to the true range of intensities, i.e. for an image with a minimal intensity of 25 and a maximum intensity of 221, the range of [25,221] is mapped to [0,1]</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Energy normalization mode</b>:</p>\n" + 
				"\n" + 
				"<p>The values different energies yield usually vary significantly. To account for numerical issues arising from this fact the optimizer offers to balance different ranges of possible values by energy normalization.\n" + 
				"<ul><li>\n" + 
				"<p>NORM&nbsp;NONE: no normalization is done</p>\n" + 
				"</li><li>\n" + 
				"<p>NORM&nbsp;BALANCED&nbsp;DERIVATIVES: scales the pixel-wise derivatives of the energy terms to yield values in a range of [-1,1]</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Do Resampling</b>:</p>\n" + 
				"\n" + 
				"<p>Turns on snake resampling. In each iteration the snake is then resampled to ensure approximately equally spaced sampling points along the contour. Turning on this option is highly recommended!</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Resample Segment Length</b>:</p>\n" + 
				"\n" + 
				"<p>Desired spacing between two snake points along the polygon. During resampling points are removed and added as to yield segments of this length.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Termination Criterion</b>:</p>\n" + 
				"\n" + 
				"<p>Offers different possibilities to check for snake convergence during optimization:\n" + 
				"<ul><li>\n" + 
				"<p>MTBTermMaxIterations: stops optimization after a predefined number of iterations</p>\n" + 
				"</li><li>\n" + 
				"<p>MTBTermAreaDiff: stops optimization after a predefined number of iterations or if the change of the area enclosed by the snake falls below a threshold between two iterations</p>\n" + 
				"</li><li>\n" + 
				"<p>MTBMotionDiff: stops optimization if a certain ratio of snake points has stopped moving<br>\n" + 
				"<br>\n" + 
				"Note that the different termination checkers may have individual configuration parameters. To set these parameters choose a criterion and pop-up its configuration window by pressing the \"Configure...\" button.</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Initial Gammy Value</b>:</p>\n" + 
				"\n" + 
				"<p>In iterative optimization a step-size is used in each step. This gamma value controls the movements of the snake. The larger the value is, the larger movements the snake performs. Note that too large steps might result in uncontrollable behaviour.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Gamma Update Strategy</b>:</p>\n" + 
				"\n" + 
				"<p>The step-size can be updated over time. Different modes are possible:\n" + 
				"<ul><li>\n" + 
				"<p>MTBGammaNone: the initial gamma value is never changed</p>\n" + 
				"</li><li>\n" + 
				"<p>MTBGammaFixed: after each iteration the gamma value is decreased by a specified fraction</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Supplemental parameters:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Verbose</b>:</p>\n" + 
				"\n" + 
				"<p>Prints additional information to the console.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Show Intermediate Results</b>:</p>\n" + 
				"\n" + 
				"<p>Pops-up additional windows during segmentation to show contour evolution.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Save Intermediate Results</b>:</p>\n" + 
				"\n" + 
				"<p>Saves intermediate segmentation results.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Save Intermediate Results To...</b>:</p>\n" + 
				"\n" + 
				"<p>Path where to save intermediate results.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Show Intermediate Snakes Stack</b>:</p>\n" + 
				"\n" + 
				"<p>Generates an additional stack as result which shows intermediate segmentation results.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Saving Interval for Stack</b>:</p>\n" + 
				"\n" + 
				"<p>Interval for saving intermediate results, e.g. every 3rd or 5th result can be saved. Note that saving all results might cause memory issues if many iterations are done.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Collect energy data</b>:</p>\n" + 
				"\n" + 
				"<p>Logs energy data during optimization and displays a table at the end.</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Output:</h3>\n" + 
				"\n" + 
				"<p>The operator returns...\n" + 
				"<ul><li>\n" + 
				"<p>the final contour (as set with one element)</p>\n" + 
				"</li><li>\n" + 
				"<p>an image overlay showing the input image and the segmentation result</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"\n" + 
				"<p>Note that the snake optimizer allows for interaction. At the bottom of the control window there are buttons to pause/resume the optimizer during segmentation or to run it step-wise.</p>";
	}
}
