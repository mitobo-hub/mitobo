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

import ij.process.ImageProcessor;

import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.operator.events.ALDControlEvent;
import de.unihalle.informatik.Alida.operator.events.ALDControlEvent.ALDControlEventType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.exceptions.*;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCoupled;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyDerivable;

/**
 * Image contour segmentation using multiple parametric snakes.
 * <p>
 * This class provides methods to segment multiple contours in an image 
 * based on parametric active contour models, i.e. snakes. Multiple
 * snakes may be coupled in the sense that overlap will be penalized
 * by a common energy term in the functional.
 * 
 * @author Birgit Möller
 */
@ALDDerivedClass
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class SnakeOptimizerCoupled extends SnakeOptimizer {

	/**
	 * Optimizer object for a single snake.
	 */
	@Parameter(label="Snake Optimizer",
			direction=Parameter.Direction.IN, required=true, dataIOOrder = 3,
			description = "Snake optimizer for single snake segmentation.")
	protected SnakeOptimizerSingle snakeOptimizer;

	/**
	 * Array of active snakes, suitable for masking snakes in optimization.
	 * <p>
	 * Note, per default all snakes are active. The size of the array on init
	 * is just randomly chosen, but a certain size is required since otherwise
	 * the GUI will initialize the array with false values.
	 */
	@Parameter(label="Activity array", 
			direction=Parameter.Direction.IN, required=false,
			description = "Array of active snakes.")
	protected boolean [] activityArray = new boolean[]
		{true, true, true, true, true, true, true, true, true, true,
			true, true, true, true, true, true, true, true, true, true};

	/**
	 * Number of iterations done for each snake.
	 */
	@Parameter(label="Number of iterations per snake",
			direction=Parameter.Direction.OUT, description = "Iterations per snake.")
	protected transient int [] iterationsPerSnake = null;

	/**
	 * Array of individual snake optimizers.
	 */
	protected transient SnakeOptimizerSingle [] snakeOpters;
	
	/**
	 * Array containing pseudo-colors for snake visualization.
	 */
	protected transient int [] colorArray= null;
	
  /**
   * Flag to indicate if overlap mask is required by at least one energy.
   */
  protected transient boolean overlapMaskRequested = false;

	/**
	 * Mask to indicate overlap regions between snakes.
	 */
	protected transient int[][] overlapMask = null;
	
  /**
   * Image for displaying intermediate/final results.
   */
  protected transient MTBImageRGB dispImg;

  @Override
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	public void validateCustom() throws ALDOperatorException {
		super.validateCustom();
		if (this.snakeOptimizer == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[SnakeOptimizerCoupled] no optimizer given!");
		if (!this.snakeOptimizer.hasEnergies())
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[SnakeOptimizerCoupled] no energies for optimizer available!");
	}

  /**
   * Default constructor.
   * @throws ALDOperatorException
   */
  public SnakeOptimizerCoupled() throws ALDOperatorException {
  	// nothing to do here
  }

  /**
   * Default constructor.
   * 
   * @param img							Image to work on.
   * @param initSnakes			Set of initial snakes.
   * @param sopt						Snake optimizer.
   * @param activeArray			Array indicating active snakes.
   * @throws ALDOperatorException
   */
  public SnakeOptimizerCoupled(MTBImage img, MTBPolygon2DSet initSnakes, 
  		SnakeOptimizerSingle sopt, boolean [] activeArray) 
  	throws ALDOperatorException {
  		this.inImg = img;
  		this.snakeOptimizer = sopt;
  		this.initialSnakes = initSnakes;
  		if(activeArray != null) {
  		  this.activityArray = activeArray.clone();
  		}
  }

  /* (non-Javadoc)
   * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer#clone()
   */
  @Override
  public SnakeOptimizerCoupled clone() {
		try {
			SnakeOptimizerCoupled newOpt = new SnakeOptimizerCoupled();
			newOpt.inImg = this.inImg;
			newOpt.initialSnakes = this.initialSnakes;
			newOpt.outSnakes = this.outSnakes;
			newOpt.outSnakesImg = this.outSnakesImg;
			newOpt.showIntermediateResults= this.showIntermediateResults;
			newOpt.saveIntermediateResults= this.saveIntermediateResults;
			newOpt.saveIntermediateResultsPath = this.saveIntermediateResultsPath;
			newOpt.snakeOptimizer= (SnakeOptimizerSingle)this.snakeOptimizer.clone();
			return newOpt;
    } catch (ALDOperatorException e) {
    	System.err.println("SnakeOptimizerCoupled - cloning operator failed!");
    	return null;
    }

  }
  
	/**
	 * Specify set of active snakes.
	 * 
	 * @param array	Boolean array to indicate active snakes.
	 */
	public void setActivityArray(boolean [] array) { 
		this.activityArray = array.clone();
	}

	/**
	 * Returns current activity array.
	 */
  public boolean [] getActivityArray() {
		return this.activityArray.clone();
	}

	/**
	 * Specify pseudo-colors for snake visualization.
	 */
	public void setColorArray(int [] array) {
		this.colorArray= array.clone();
	}	

	/***********************************************************************/
	/*** Data access. ***/
	/***********************************************************************/
	
	/***********************************************************************/
	/*** Optimizer initialization. ***/
	/***********************************************************************/

	/**
	 * Initializes the optimizer.
	 * <p>
	 * Here internal member variables are initialized according to the given
	 * parameters, and memory for intermediate results and debug data is
	 * allocated.
	 * 
	 * @throws MTBSnakeException
	 */
	@Override
	protected void initOptimizer() throws MTBSnakeException {

		// get number of snakes and init data structures
		this.snakeNum= this.initialSnakes.size();
		this.iterationsPerSnake = new int[this.snakeNum];
		for (int i=0; i<this.snakeNum; ++i)
			this.iterationsPerSnake[i] = -1;
		
		// fill the color array with default values,
		// array maybe lateron overridden by using 'setColorArray()'
		this.colorArray= new int[this.snakeNum];
		for (int i = 0; i < this.colorArray.length; i++) {
			Random rand= new Random();
			this.colorArray[i]= rand.nextInt(Integer.MAX_VALUE);
    }
		
		// init snake optimizer array
		this.snakeOpters= new SnakeOptimizerSingle[this.snakeNum];
		
		// reset iterations counter
		this.itCounter = 0;

		// init local variables related to input image
		this.iWidth = this.inImg.getSizeX();
		this.iHeight = this.inImg.getSizeY();

		// safety checks...
		if (this.activityArray == null) {
			this.activityArray = new boolean[this.snakeNum];
			for (int i=0;i<this.snakeNum;++i)
				this.activityArray[i]= true;
		}
		else if (this.activityArray.length != this.snakeNum) {
			boolean newArray[] = new boolean[this.snakeNum];
			for (int i=0; i<this.snakeNum; ++i) {
				if (i < this.activityArray.length)
					newArray[i] = this.activityArray[i];
				else
					newArray[i] = true;
			}
			this.activityArray = newArray;
		}
		
		// init the energies of the snake optimizers in case they use additional
		// information when used with a coupled optimization procedure
		MTBSet_ActiveContourEnergy eSet = this.snakeOptimizer.getEnergySet();
		Vector<MTBActiveContourEnergy> eVec = eSet.getGenericEnergyList();
		for (MTBActiveContourEnergy e: eVec) {
			if (e instanceof MTBSnakeEnergyCoupled) {
				((MTBSnakeEnergyCoupled)e).initEnergy(this);
			}
		}

		// ask energies which additional information they require
		this.overlapMaskRequested = false;
		for (MTBActiveContourEnergy e: eVec) {
			if (e instanceof MTBSnakeEnergyCoupled)
				this.overlapMaskRequested = this.overlapMaskRequested 
					|| ((MTBSnakeEnergyCoupled)(e)).requiresOverlapMask();
			else if (e instanceof MTBSnakeEnergyComputable)
				this.overlapMaskRequested = this.overlapMaskRequested 
					|| ((MTBSnakeEnergyComputable)(e)).requiresOverlapMask();
			else if (e instanceof MTBSnakeEnergyDerivable) 
				this.overlapMaskRequested = this.overlapMaskRequested 
				|| ((MTBSnakeEnergyDerivable)(e)).requiresOverlapMask();
		}

		// initialize each single optimizers
		for (int n=0; n<this.snakeNum; ++n) {
			// get n-th snake
			MTBPolygon2DSet is= new MTBPolygon2DSet(0, 0, 0, 0);
			is.add(this.initialSnakes.elementAt(n).clone());
			
			// clone the given optimizer...
			SnakeOptimizerSingle topt = 
				(SnakeOptimizerSingle)this.snakeOptimizer.clone();
			topt.setInputImage(this.inImg);
			topt.setInitialSnakes(is);
			try {
	      topt.setVerbose(this.verbose);
      } catch (ALDOperatorException e) {
      	// there is nothing that we can do here...
      }
      this.snakeOpters[n]= topt;
			// disable display of individual snake results
			this.snakeOpters[n].disableShowIntermediateResults();
			this.snakeOpters[n].disableSaveIntermediateResults();
			this.snakeOpters[n].initOptimizer();
			if (this.activityArray[n] == true) {
				// snake is active
				ALDControlEvent event = 
					new ALDControlEvent(this,	ALDControlEventType.RUN_EVENT);
				this.snakeOpters[n].handleALDControlEvent(event);
			}
		}

		// instantiate result image if requested
		if (   this.showIntermediateResults.booleanValue() 
				|| this.saveIntermediateResults.booleanValue()) {
			this.dispImg = (MTBImageRGB) MTBImage.createMTBImage(this.iWidth,
					this.iHeight, 1, 1, 1, MTBImageType.MTB_RGB);
			this.plotImageToBackground();
		}
	}
	
	/*******************************************************************************/
	/*** Operator routines. ***/
	/*******************************************************************************/

	@Override
	protected SnakeOptimizer.Snake_status doIteration()
		throws MTBException {
		
		if (this.verbose.booleanValue())
			System.out.println("Running iteration...");
		
		// increment the iteration counter
		this.itCounter++;
		
		// if overlap mask is requested, calculate mask of current configuration
		if (this.overlapMaskRequested)
			this.updateOverlapMask();
		
		// update energies that might require additional data 
		MTBSet_ActiveContourEnergy eSet = this.snakeOptimizer.getEnergySet();
		Vector<MTBActiveContourEnergy> eVec = eSet.getGenericEnergyList();
		for (MTBActiveContourEnergy e: eVec) {
			if (e instanceof MTBSnakeEnergyCoupled) {
				((MTBSnakeEnergyCoupled)e).updateStatus(this);
			}
		}
		
		// do one step: optimize, resample, simplify
		SnakeOptimizer.Snake_status [] stats= 
			new SnakeOptimizer.Snake_status[this.snakeNum];
		MTBSnake [] resultSnakes = new MTBSnake[this.snakeNum];
		for (int n=0; n<this.snakeNum; ++n) {
			if (this.activityArray[n] == false)
				// snake n is inactive
				continue;
			if (this.initialSnakes.elementAt(n).getPointNum() < 5)
				continue;
			if (this.excludeMask!=null)
				this.snakeOpters[n].setExcludeMask(this.excludeMask);
			if (this.verbose.booleanValue())
				System.out.println("- running iteration for snake #" +n+ "...");
			stats[n]= this.snakeOpters[n].doIteration();
			resultSnakes[n] = this.snakeOpters[n].getCurrentSnake();
		}
		
		// check failure
		for (int n=0; n<this.snakeNum; ++n) {
			if (this.activityArray[n] == false)
				// snake n is inactive
				continue;
//			if (this.initialSnakes.elementAt(n).getPointNum() < 5)
//				continue;
			if (resultSnakes[n].getPointNum() < 5)
				continue;
			if (stats[n] == Snake_status.SNAKE_FAIL) {
				System.err.println("Snake " + n + " failed!");
//				return Snake_status.SNAKE_FAIL;
			}
		}
		// additional failure check, if snake grows too large
//		for (int n=0; n<this.snakeNum; ++n) {
////			MTBSnake s= this.snakeOpters[n].getCurSnake();
//			MTBSnake s= resultSnakes[n];
//			if (s.getPointNum()>1500) {
//				System.err.println("Snake " + n + " failed!");
//				this.activityArray[n] = false;
//			}
//		}
		// check termination
		boolean allReady= true;
		for (int n=0; n<this.snakeNum; ++n) {
			if (this.activityArray[n] == false)
				// snake n is inactive
				continue;
//			if (this.initialSnakes.elementAt(n).getPointNum() < 5)
//				continue;
			if (resultSnakes[n].getPointNum() < 5)
				continue;
			if (stats[n] == Snake_status.SNAKE_FAIL) {
				if (this.iterationsPerSnake[n] == -1)
					this.iterationsPerSnake[n] = this.itCounter;
				System.err.println("Snake " + n + " failed!");
				continue;
			}
			if (stats[n]==Snake_status.SNAKE_DONE) {
				if (this.iterationsPerSnake[n] == -1)
					this.iterationsPerSnake[n] = this.itCounter;
				System.out.println("Snake "+n+"... Ready!");
			}
			else
				System.out.println("Snake "+n+"... --- not ready, still running!");
			allReady = allReady && (stats[n]==Snake_status.SNAKE_DONE);
			// provide snake image
		}
		// make current snakes available
		MTBPolygon2DSet set = 
			new MTBPolygon2DSet(0, 0, this.iWidth-1, this.iHeight-1);
		for (int n=0; n<this.snakeNum; ++n) {
			set.add(resultSnakes[n]);
		}
//		this.outSnakes = set;
		// return status value
		if (allReady) {
			return Snake_status.SNAKE_DONE;
		}
		return Snake_status.SNAKE_SUCCESS;
	}
	
	/**
	 * Display input image with current snake overlayed.
	 */
	@Override
	protected void showSnake() {
		// modify internal display image
		this.plotImageToBackground();
		this.plotSnakeToImage(this.dispImg);
		this.dispImg.updateAndRepaintWindow();
		this.dispImg.show();
		this.outSnakesImg = (MTBImageRGB)this.dispImg.duplicate();
	}
	
  @Override
  protected void closeWindows() {
  	if (this.dispImg != null)
  		this.dispImg.close();
  }

	@Override
  protected void saveSnake() {
		MTBPolygon2DSet snakeSet = this.getCurrentSnakes();
		try {
			// save the snake (without history)...
			snakeSet.write(this.saveIntermediateResultsPath.getDirectoryName() + 
					"/snakeSet_" + this.itCounter + ".xml", false);
			// ... and a corresponding image
			ImageWriterMTB writer = new ImageWriterMTB(this.dispImg, 
					this.saveIntermediateResultsPath.getDirectoryName() + 
						"/snakeImg_" + this.itCounter + ".tif");
			writer.runOp(true);
		} catch (ALDException e) {
			System.err.println("SnakeOptimizerSingle: cannot write intermediate " + 
					"results (path: " + 
						this.saveIntermediateResultsPath.getDirectoryName() + ") !!!" );
			e.printStackTrace();
		}
	}

	/**
	 * Update current overlap mask for all snakes.
	 * @return Array indicating for each pixel how many snakes overlap there.
	 */
	public void updateOverlapMask() {
		this.overlapMask= new int[this.iHeight][this.iWidth];
		for (SnakeOptimizerSingle sos : this.snakeOpters) {
			MTBSnake s= sos.getCurrentSnake();
			if (s.getPointNum()<5)
				continue;
			int [][] sMask= s.getBinaryMask(this.iWidth, this.iHeight);
			for (int y=0;y<this.iHeight;++y) {
				for (int x=0;x<this.iWidth;++x) {
					this.overlapMask[y][x]+=sMask[y][x];
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer#getWorkingImage()
	 */
	@Override
	public MTBImage getWorkingImage() {
		return this.inImg;
	}
	
	/**
	 * Returns a copy of the set of current snakes.
	 */
	@Override
  public MTBPolygon2DSet getCurrentSnakes() {
		Vector<MTBPolygon2D> polyVec = new Vector<MTBPolygon2D>();
		for (int i=0;i<this.snakeNum;++i) {
			polyVec.add(this.snakeOpters[i].getCurrentSnake());
		}
		MTBPolygon2DSet polys = 
			new MTBPolygon2DSet(polyVec, 0, 0, this.iWidth-1, this.iHeight-1);
		return polys;
	}

	/**
	 * Get the current overlap mask.
	 * @return	Overlap mask of current snake configuration.
	 */
	public int[][] getCurrentOverlapMask() {
		return this.overlapMask;
	}
	
	/**
	 * Returns the total number of iterations per snake.
	 * @return	Array of iteration counts.
	 */
	public int[] getIterationsPerSnake() {
		return this.iterationsPerSnake;		
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer#printParams()
	 */
	@Override
	public void printParams() {
		System.out.println("Parameters of SnakeOptimizerCoupled: ");
		System.out.println("==================================== ");
		System.out.println("- snake number= " + this.snakeNum);
		if (this.activityArray != null) {
			String activityStatus = "[";
			for (int i=0; i<this.snakeNum; ++i) {
				activityStatus += (this.activityArray[i] ? "1" : "0");
				if (i == this.snakeNum-1)
					activityStatus += "]";
				else
					activityStatus += ",";
			}
			System.out.println("- activity array= " + activityStatus);
		}
		if (this.snakeOptimizer != null) {
			System.out.println("- snake optimizer configuration:");
			this.snakeOptimizer.printParams();
		}
		else
			System.out.println("- snake optimizer configuration: non available");
	}

  @Override
  public String toString() {
  	return new String("SnakeOptimizerCoupled - object");
  }

	/**
	 * Copies the input image as background into the output frame.
	 */
	protected void plotImageToBackground() {
  	if (!(this.inImg instanceof MTBImageRGB)) {
  		MTBImageByte background = 
 				(MTBImageByte)this.inImg.convertType(MTBImageType.MTB_BYTE,true);
  		int gray = 0, color = 0;
  		for (int y = 0; y < this.inImg.getSizeY(); ++y) {
  			for (int x = 0; x < this.inImg.getSizeX(); ++x) {
  				gray = background.getValueInt(x, y);
  				color = 
  					((gray & 0xff) << 16) + ((gray & 0xff) << 8) + (gray & 0xff);
  				this.dispImg.putValueInt(x, y, color);
  			}
  		}
  	}
  	else {
  		for (int y = 0; y < this.inImg.getSizeY(); ++y) {
  			for (int x = 0; x < this.inImg.getSizeX(); ++x) {
  				this.dispImg.putValueR(x, y, 
  					((MTBImageRGB)this.inImg).getValueR(x, y));
  				this.dispImg.putValueG(x, y, 
    				((MTBImageRGB)this.inImg).getValueG(x, y));
  				this.dispImg.putValueB(x, y, 
    				((MTBImageRGB)this.inImg).getValueB(x, y));
  			}
  		}
  	}
	}

  protected void plotSnakeToImage(MTBImageRGB img) {
		int yellow = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);
		for (int n=0; n<this.snakeNum; ++n) {
			MTBSnake s= (MTBSnake)this.snakeOpters[n].getCurrentSnakes().elementAt(0);
			if (s.getPointNum()<5)
				continue;
			s.drawPolygon(img, this.colorArray[n]);
			s.drawPolygonPoints(this.dispImg, yellow, 1 /* shape: X */);

			// add index of snake
			int comX= (int)s.getCOMx();
			int comY= (int)s.getCOMy();	

			int red= (this.colorArray[n] & 0xff0000)>>16;
			int green= (this.colorArray[n] & 0x00ff00)>>8;
			int blue= (this.colorArray[n] & 0x0000ff);

			// red channel
			MTBImageByte imgtmp= (MTBImageByte)img.getChannelR();
			ImageProcessor ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(red);
			ip.drawString(new Integer(n).toString());
			// green channel
			imgtmp= (MTBImageByte)img.getChannelG();
			ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(green);
			ip.drawString(new Integer(n).toString());
			// blue channel
			imgtmp= (MTBImageByte)img.getChannelB();
			ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(blue);
			ip.drawString(new Integer(n).toString());
		}
  }


//	/**
//	 * Copies the input image as background into the output frame.
//	 */
//	@Override
//  protected void plotImageToBackground() {
//		int [] extremeVal= this.inImg.getMinMaxInt();
//		for (int y = 0; y < this.inImg.getSizeY(); ++y) {
//			for (int x = 0; x < this.inImg.getSizeX(); ++x) {
//				int color = 0;
//				int gray = 0;
//				if (this.inImg.getType() == MTBImageType.MTB_BYTE) 
//				{
//						gray = this.inImg.getValueInt(x, y);
//						color = ((gray & 0xff) << 16) + ((gray & 0xff) << 8)
//										+ (gray & 0xff);
//				}
//				else if (this.inImg.getType() == MTBImageType.MTB_SHORT)
//				{
//						gray = 
//							(int)((double)(this.inImg.getValueInt(x, y))/
//									  (double)(extremeVal[1])*255.0);
//						color = ((gray & 0xff) << 16) + ((gray & 0xff) << 8)
//										+ (gray & 0xff);
//				}
//				
//				this.dispImg.putValueInt(x, y, color);
//			}
//		}
//	}
}
