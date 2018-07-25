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

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.*;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.*;
import de.unihalle.informatik.MiToBo.core.exceptions.*;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperatorControllable;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * Image contour segmentation using parametric snakes.
 * <p>
 * This class defines a generic interface for segmenting contours in an image
 * based on parametric active contour models, i.e. snakes. It supports 
 * thread-based optimization and interaction with a graphical user interface.
 * 
 * @author moeller
 */
public abstract class SnakeOptimizer extends MTBOperatorControllable {

	/**
	 * Input image to be segmented.
	 */
	@Parameter(label="Input Image", mode= ExpertMode.STANDARD,
			direction=Parameter.Direction.IN, required=true,
			description = "Input Image.", dataIOOrder = 0)
	protected transient MTBImage inImg= null;

	/**
	 * Set of initial snake contours.
	 */
	@Parameter(label="Initial Snake(s)", mode= ExpertMode.STANDARD,
			direction=Parameter.Direction.IN, required=true,
			description = "Initial snakes.", dataIOOrder = 1)
	protected MTBPolygon2DSet initialSnakes= null;
	
	/**
	 * Result contours.
	 */
	@Parameter(label="outSnakes",	direction=Parameter.Direction.OUT,
			description = "Final snake(s).")
	protected transient MTBPolygon2DSet outSnakes= null;
//																						new MTBPolygon2DSet(0, 0, 0, 0);

	/**
	 * Image with final snake contours overlayed.
	 */
	@Parameter(label="Snake Plot", direction=Parameter.Direction.OUT,
			description = "Overlay with final snake(s).")
	protected transient MTBImageRGB outSnakesImg= null;
//		(MTBImageRGB)MTBImage.createMTBImage(10,10,10,1,1,MTBImageType.MTB_RGB);

	/*
	 * Supplemental parameters.
	 */
	
	/**
	 * Flag to enable/disable showing of intermediate results.
	 */
	@Parameter(label="Show Intermediate Results", mode=ExpertMode.STANDARD,
			direction=Parameter.Direction.IN, dataIOOrder = 1, 
			supplemental = true, description = "Show intermediate results to user.")
	protected Boolean showIntermediateResults= new Boolean(false);

  /**
   * Flag to enable/disable saving of intermediate results.
   */
  @Parameter(label="Save Intermediate Results",	mode=ExpertMode.ADVANCED,
			direction=Parameter.Direction.IN, dataIOOrder = 2,
			supplemental = true, description = "Save intermediate results to disc.")
  protected Boolean saveIntermediateResults= new Boolean(false);

  /**
   * Path to where intermediate results should be stored.
   */
  @Parameter(label="Save Intermediate Results To...", mode=ExpertMode.ADVANCED,
  		supplemental = true, direction=Parameter.Direction.IN, dataIOOrder = 3,
  		description = "Path for saving intermediate results.")
  protected ALDDirectoryString saveIntermediateResultsPath = 
  																						new ALDDirectoryString("/tmp");

  /**
   * Flag to request a stack of intermediate result images.
   */
  @Parameter(label="Show Intermediate Snakes Stack", mode=ExpertMode.ADVANCED,
  		direction = Direction.IN, dataIOOrder = 4, supplemental = true,
  		description = "Flag to show stack with intermediate segmentations.")
  protected Boolean outIntermediateResultsStackWanted= new Boolean(false);

  /**
   * Interval for saving intermediate results in stack.
   * <p>
   * Note that memory issues might occur if number of iterations is high
   * and the interval small...
   */
  @Parameter(label="Saving Interval for Stack", mode=ExpertMode.ADVANCED,
  		direction = Direction.IN, dataIOOrder = 5, supplemental = true,
  		description = "Interval for saving results to stack.")
  protected int outIntermediateResultsStackInterval= 3;

  /**
   * Image stack with intermediate result images.
   */
  @Parameter(label="Stack with Intermediate Results",
  		direction = Direction.OUT, supplemental = true,
  		description = "Optional stack of intermediate segmentation results.")
  protected transient MTBImageRGB outIntermediateResultsStack= null;
//  	(MTBImageRGB)MTBImage.createMTBImage(10,10,10,1,1,MTBImageType.MTB_RGB);

  /*
   * Supplementals.
   */

  @Parameter(label="Collect energy data", supplemental = true,
  		direction=Parameter.Direction.IN, required=false, dataIOOrder = 15,
  		description = "Flag for collecting energy data in each iteration.")
  protected Boolean	sampleEnergyData= new Boolean(false);

  @Parameter(label="Energy data",
  		direction=Parameter.Direction.OUT, required=false, dataIOOrder = 10,
  		description = "Table of iteration-wise energies.")
  protected transient MTBTableModel energyData = null;

  /***********************************************************************/
  /*** Local defines and datatypes. ***/
  /***********************************************************************/

  /**
   * Indicates the current (internal) state of the snake calculations.
   * <p>
   * Note that the enumeration needs to be public (instead of protected or 
   * even private) because it is used by the termination checkers being 
   * implemented in a subpackage of the optimizer package.
   */
  public static enum Snake_status {
    /**
     * Calculation is finished, i.e. termination criterion is fulfilled.
     */
    SNAKE_DONE,
    /**
     * Something went wrong during the current iteration.
     */
    SNAKE_FAIL,
    /**
     * Iteration succeeded, but termination criterion failed.
     */
    SNAKE_SUCCESS
  }

  /*
   * Some internal convenience variables.
   */
  
  /**
   * Number of snakes currently managed.
   */
  protected transient int snakeNum= 1;

	/**
	 * Iteration counter.
	 */
	protected transient int itCounter = 0;

  /**
   * Image width.
   */
  protected transient int iWidth;

  /**
   * Image height.
   */
  protected transient int iHeight;

  /**
   * Image channels.
   */
  protected transient int iChannels;

  /**
   * Mask to exclude image pixels from calculations.
   */
  protected transient boolean[][] excludeMask = null;

  /**
   * Flag to indicate if snakes are required to be sorted counter-clockwise.
   */
  protected transient boolean counterClockwiseSnakePointOrderRequested = false;

  /**
   * Set of intermediate segmentation results, required for stack generation.
   */
  protected transient Vector<MTBPolygon2DSet> intermediateResults = null;
  
  
  /***********************************************************************/
  /*** Constructors and clone()-methods, validation routines. ***/
  /***********************************************************************/

  /** 
   * Default constructor
   */
  public SnakeOptimizer() throws ALDOperatorException {
  	// history should be extracted based on data-dependencies
  	this.completeDAG = false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public abstract SnakeOptimizer clone();
  
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (   this.inImg == null 
				|| this.initialSnakes == null || this.initialSnakes.size() == 0)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"SnakeOptimizer - no image and/or snakes given!");
		return;
	}
	
	@Override
	public boolean supportsStepWiseExecution() {
		return true;
	}
	
  /***********************************************************************/
  /*** Helper method for object serialization.                         ***/
  /***********************************************************************/
	
	/**
	 * Init function for deserialized objects.
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
		this.snakeNum= 0;
		this.itCounter = 0;
		this.counterClockwiseSnakePointOrderRequested = false;
		return this;
	}
	
  /***********************************************************************/
  /*** Abstract methods for operator functionality.                    ***/
  /***********************************************************************/

  /**
   * Initializes the optimizer.
   * <p>
   * Needs to be implemented by any derived class. This routine is 
   * automatically called on invoking the local operate routine.
   */
  protected abstract void initOptimizer() throws MTBSnakeException;

  /**
   * Here the main work should be done.
   * @return Indicates if the iteration was successful.
   */
  protected abstract Snake_status doIteration() throws MTBException;

  /**
   * Display current result contour by overlaying the current snake result
   * onto the input image.
   */
  protected abstract void showSnake();

  /**
   * Close all windows openened by this operator (for clean-up). 
   */
  protected abstract void closeWindows();

  /**
   * Save intermediate results. 
   */
  protected abstract void saveSnake();

	/**
	 * Returns a copy of the current snake(s).
	 * <p>
	 * If the optimizer deals with a single snake, the set contains only
	 * a single snake polygon.
	 * @return Current set of snake contours.
	 */
	public abstract MTBPolygon2DSet getCurrentSnakes();

	/**
	 * Print current parameter settings to standard output device.
	 */
	public abstract void printParams();
	

  /***********************************************************************/
  /*** Getters and Setters. ***/
  /***********************************************************************/

	/**
	 * Returns the current iteration count.
	 */
	public final int getIterationCount() {
		return this.itCounter;
	}

	/**
	 * Specify an input image for the segmentation.
	 * @param img	Input image.
	 */
	public final void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	/**
	 * Specify initial snakes.
	 * @param inS Initial snakes.
	 */
	public final void setInitialSnakes(MTBPolygon2DSet inS) {
		this.initialSnakes = inS;
	}
	
	/**
	 * Returns input image.
	 * @return Input image of operator.
	 */
	public final MTBImage getInputImage() {
		return this.inImg;
	}
	
	/**
	 * Returns working image.
	 * <p>
	 * Usually this will be the input image, however, e.g. in case of image 
	 * normalization being applied the normalized image will be returned.
	 * 
	 * @return Image on which the operator works.
	 */
	public abstract MTBImage getWorkingImage();

	/**
	 * Returns initial snake.
	 * @return Set of initial snakes.
	 * @throws ALDOperatorException 
	 */
	public final MTBPolygon2DSet getInitialSnakes() {
		return this.initialSnakes;
	}
	
	/**
	 * Returns image with snakes contours.
	 * @return Image with colored result snake contour overlay.
	 * @throws ALDOperatorException 
	 */
	public MTBImageRGB getResultSnakeImage()  {
		return this.outSnakesImg;
	}
	
  /**
   * Returns the number of snakes currently managed.
   * @return Number of snakes currently handled by the optimizer.
   */
  public final int getSnakeNumber() {
  	return this.snakeNum;
  }
  
  /**
   * Returns the set of result snakes.
   * @return Set of snakes, object may be null!
   */
  public final MTBPolygon2DSet getResultSnakes() {
		return this.outSnakes;
	}

  /**
   * Activates display of intermediate results.
   */
  public final void enableShowIntermediateResults() {
	  this.showIntermediateResults = new Boolean(true);
  }
  
  /**
   * Deactivates display of intermediate results.
   */
  public final void disableShowIntermediateResults() {
	  this.showIntermediateResults = new Boolean(false);
  }
  
  /**
   * Activates writing of intermediate results to disc.
   */
  public final void enableSaveIntermediateResults() {
  	this.saveIntermediateResults = new Boolean(true);
  }
  
  /**
   * Deactivates writing of intermediate results.
   */
  public final void disableSaveIntermediateResults() {
	  this.saveIntermediateResults = new Boolean(false);
  }

  /**
   * Set path for intermediate results. 
   */
  public final void setIntermediateResultPath(String path) {
  	this.saveIntermediateResultsPath = new ALDDirectoryString(path);
  }
  
	/**
	 * Flag for turning on/off generation of stack with intermediate results.
	 */
	public final void wantStackWithIntermediateResults(boolean flag) {
		this.outIntermediateResultsStackWanted = new Boolean(flag);		
	}
	
	/**
	 * Returns a stack with intermediate segmentation results.
	 * @return Stack of intermediate segmentation results.
	 */
	public final MTBImageRGB getStackWithIntermediateResults() {
		return this.outIntermediateResultsStack;
	}
	
	/**
	 * Returns the current exclude mask.
	 * @return Mask array, indicating which image pixels to exclude from calcs.
	 */
	public final boolean[][] getExcludeMask() {
		return this.excludeMask;
	}

	/**
	 * Set exclude mask.
	 */
	public final void setExcludeMask(boolean[][] mask) {
		this.excludeMask = mask;
	}
	
//	/**
//	 * Intended for energies to request energy calculations.
//	 */
//	public void requireEnergyCalculation() {
//		this.energyCalculationRequested= true;
//	}

  /************************************************************************/
  /*** Operator routines. ***/
  /************************************************************************/

  @Override
  protected final void operate() throws ALDOperatorException {

  	// reset variables
  	this.outSnakes = null;
  	this.outSnakesImg = null;
  	this.outIntermediateResultsStack = null;
  	
  	// check some configuration settings
  	if (this.outIntermediateResultsStackWanted.booleanValue()) {
  		this.intermediateResults = new Vector<MTBPolygon2DSet>();
  	}
  	
    // initialize the optimizer
  	try {
	    this.initOptimizer();
    } catch (MTBSnakeException e) {
    	throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
    		"[SnakeOptimizer] Initialization of optimizer failed! " 
    				+ e.getCommentString());
    }
  	
    // counter for steps
    int steps = 0;
    this.setControlStatus(OperatorControlStatus.OP_RUN);
    
    // main optimization loop
    int loopCounter = 0;
    while (true) {

    	if (this.getControlStatus() == OperatorControlStatus.OP_STOP) {
    		System.err.println("Snake Optimizer... cancelled!");
    		break;
    	}
    	else if (this.getControlStatus() == OperatorControlStatus.OP_PAUSE) {
    		System.err.println("SnakeOptimizer paused...");
    		do {
					try {
	          Thread.sleep(500);
          } catch (InterruptedException e) {
          	// just ignore the exception
          }
    		} while (this.getControlStatus() != OperatorControlStatus.OP_RESUME);
    		System.err.println("SnakeOptimizer running again...");
    	}

      switch (this.getControlStatus()) {
        case OP_RUN:
          if (this.stepWiseExecution) {
            if (steps == this.stepSize) {
              this.setControlStatus(OperatorControlStatus.OP_PAUSE);
              while (!(this.getControlStatus()==OperatorControlStatus.OP_STEP)) { 
              	// just wait to continue or interrupt process
              	if (this.getControlStatus() == OperatorControlStatus.OP_STOP) {
              		if (this.outSnakesImg == null) 
                		this.plotSnakesToImage();
                	if (this.outIntermediateResultsStackWanted.booleanValue())
                		this.generateStackWithIntermediateResults();
                	return;
              	}
              }
              this.setControlStatus(OperatorControlStatus.OP_RUN);
              steps = 1;
            } else {
              steps++;
            }
          }
          break;
        case OP_PAUSE:
        	while (!(this.getControlStatus() == OperatorControlStatus.OP_RESUME)) { 
        		// just wait to continue
          }
          break;
        case OP_STOP:
        	if (this.outSnakesImg == null) 
        		this.plotSnakesToImage();
        	if (this.outIntermediateResultsStackWanted.booleanValue())
        		this.generateStackWithIntermediateResults();
        	//        	// get collected energy data, if requested
        	//        	if (this.sampleEnergyData.booleanValue())
        	//        		this.energyData = this.getEnergyData();
          return;
        case OP_INIT:
        case OP_STEP:
        case OP_RESUME:
        case OP_KILL:
        	break;
      }

      // do one step: optimize, resample, simplify, ...
      SnakeOptimizer.Snake_status stat= Snake_status.SNAKE_SUCCESS;
      try {
	      stat = this.doIteration();
	      this.outSnakes = this.getCurrentSnakes();
      } catch (MTBException e) {
      	// convert exception and pass it to calling object
      	throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
      			e.getCommentString());
      }

      // check failure
      if (stat == Snake_status.SNAKE_FAIL) {
      	if (this.outSnakesImg == null) 
      		this.plotSnakesToImage();
        throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
        		"SnakeOptimizer: snake iteration failed...!!!");
      }

      // show intermediate results, if desired
      if (this.showIntermediateResults.booleanValue()) {
    	  this.showSnake();
      }

      // save intermediate results, if desired
      if (this.saveIntermediateResults.booleanValue()) {
    	  this.saveSnake();
      }
      
      // save intermediate result for later stack generation,
      // but only every n steps...
      if (this.outIntermediateResultsStackWanted.booleanValue()) {
      	if (loopCounter%this.outIntermediateResultsStackInterval == 0)
      		this.intermediateResults.add(this.getCurrentSnakes());
      }
      
      // check termination
      if (stat == Snake_status.SNAKE_DONE) {
        break;
      }
      // increment loop counter
      ++loopCounter;
    }
  	if (this.outSnakesImg == null) 
  		this.plotSnakesToImage();
  	this.outSnakesImg.setTitle("Snake segmentation result for image \"" + 
  			this.inImg.getTitle() + "\"");

  	// if desired, produce a stack and return it
  	if (this.outIntermediateResultsStackWanted.booleanValue()) {
  		this.generateStackWithIntermediateResults();
  		this.outIntermediateResultsStack.setTitle("Intermediate results for \"" 
  				+	this.inImg.getTitle() + "\"");
  	}
  	// get collected energy data, if requested
  	//  	if (this.sampleEnergyData.booleanValue()) {
  	//  		this.energyData = this.getEnergyData();
  	//  	}
  	// clean-up
  	this.closeWindows();  	
    return;
  }
  
  /**
   * Returns overlay of output snakes onto current input image.
   */
  private void plotSnakesToImage() {
  	this.outSnakesImg = 
  		SnakeOptimizer.plotSnakesToImage(this.outSnakes, this.inImg, null);
  }
  
  /**
   * Returns overlay of given snakes onto given image.
   * 
   * @param polyset	Set of snakes.
   * @param image   Input image where to plot the snakes.
   * @return Color image overlayed with snakes.
   */
  private static MTBImageRGB plotSnakesToImage(
  						MTBPolygon2DSet polyset, MTBImage image, int [] colors) {
  	// convert input image to byte format
  	MTBImageByte background = 
  		(MTBImageByte)image.convertType(MTBImageType.MTB_BYTE, true);
  	int iWidth = image.getSizeX();
  	int iHeight = image.getSizeY();
		MTBImageRGB outImg = (MTBImageRGB) MTBImage.createMTBImage(iWidth, iHeight, 
				1, 1, 1, MTBImageType.MTB_RGB);

  	// plot it to the background of the disp image
  	int gray = 0, color = 0;
  	for (int y = 0; y < iHeight; ++y) {
  		for (int x = 0; x < iWidth; ++x) {
  			gray = background.getValueInt(x, y);
  			color = ((gray & 0xff) << 16) + ((gray & 0xff) << 8) + (gray & 0xff);
  			outImg.putValueInt(x, y, color);
  		}
  	}
		// fill the color array with default values,
		// array maybe lateron overridden by using 'setColorArray()'
  	int snakeNum = polyset.size();
  	int [] colorArray = null;
  	if (colors == null || colors.length < snakeNum) {
  		colorArray= new int[snakeNum];
  		for (int i = 0; i < colorArray.length; i++) {
  			Random rand= new Random();
  			colorArray[i]= rand.nextInt(Integer.MAX_VALUE);
  		}
  	}
  	else {
  		colorArray = colors;
  	}
  	
  	// plot the snake(s) to the image
		for (int n=0; n<snakeNum; ++n) {
			MTBSnake s= (MTBSnake)polyset.elementAt(n);
			if (s.getPointNum()<5)
				continue;
			s.drawPolygon(outImg, colorArray[n]);
			// add index of snake
			int comX= (int)s.getCOMx();
			int comY= (int)s.getCOMy();	

			int red= (colorArray[n] & 0xff0000)>>16;
			int green= (colorArray[n] & 0x00ff00)>>8;
			int blue= (colorArray[n] & 0x0000ff);

			// red channel
			MTBImageByte imgtmp= (MTBImageByte)outImg.getChannelR();
			ImageProcessor ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(red);
			ip.drawString(new Integer(n).toString());
			// green channel
			imgtmp= (MTBImageByte)outImg.getChannelG();
			ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(green);
			ip.drawString(new Integer(n).toString());
			// blue channel
			imgtmp= (MTBImageByte)outImg.getChannelB();
			ip= imgtmp.getImagePlus().getProcessor();
			ip.moveTo(comX, comY);
			ip.setColor(blue);
			ip.drawString(new Integer(n).toString());
		}
		return outImg;
  }

  /**
   * Generates a stack with intermediate results.
   */
  private void generateStackWithIntermediateResults() {
  	this.outIntermediateResultsStack = (MTBImageRGB)MTBImage.createMTBImage(
  			this.iWidth,this.iHeight,1,1,this.intermediateResults.size(),
  			MTBImageType.MTB_RGB);
  	// initialize array with colors
		int [] colorArray= new int[this.snakeNum];
		for (int i = 0; i < colorArray.length; i++) {
			Random rand= new Random();
			colorArray[i]= rand.nextInt(Integer.MAX_VALUE);
		}
		// generate stack
		int slice = 0;
  	for (MTBPolygon2DSet polyset: this.intermediateResults) {
  		MTBImageRGB tmpImg = 
  			SnakeOptimizer.plotSnakesToImage(polyset, this.inImg, colorArray);
  		this.outIntermediateResultsStack.setSlice(tmpImg, 0, 0, slice);
  		++slice;
  	}
  }
  
  @Override
  public String toString() {
  	return new String("SnakeOptimizer - abstract base class object");
  }
}
