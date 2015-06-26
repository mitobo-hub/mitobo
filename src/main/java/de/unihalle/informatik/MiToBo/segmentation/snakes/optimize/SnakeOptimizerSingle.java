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

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.*;
import de.unihalle.informatik.MiToBo.tools.system.UserTime;

/**
 * <pre>
 * Image contour segmentation using parametric snakes.
 * </pre>
 * 
 * @author moeller
 * @author misiak
 */
public abstract class SnakeOptimizerSingle extends SnakeOptimizer {

	/**
	 * Image intensity normalization mode.
	 * <p>
	 * For parameter tuning it is of advantage to use normalized image 
	 * intensities in snake segmentation. As such a normalization can be 
	 * accomplished in various ways, different modes can be chosen.
	 * The image intensities are always scaled to a range of [-1,1], but
	 * the base of normalization differs in the different modes.
	 */
	public static enum IntensityNormalizationMode {
		/**
		 * No normalization of image intensities.
		 */
		INTENSITY_NORM_NONE,
		/**
		 * Normalization is done according to the real intensity range of 
		 * the input image, i.e. based on the extreme values found in the 
		 * image.
		 */
		INTENSITY_NORM_TRUE_RANGE,
		/**
		 * Normalization is done according to the dynamic range of the input 
		 * image, i.e. based on the extreme values that can be represented
		 * in the image.
		 */
		INTENSITY_NORM_THEORETIC_RANGE
	}

	/**
	 * Snake normalization mode.
	 * 
	 * @author moeller
	 */
	public static enum EnergyNormalizationMode {
		/**
		 * No normalization of energies is done.
		 */
		NORM_NONE,
//		/**
//		 * Normalization is done based on absolute energy values.
//		 * <p>
//		 * Each energy in functional is scaled to [-1,1].
//		 */
//		NORM_ENERGY_ABSOLUTE,
		/**
		 * Normalization is done based on energy derivative values.
		 * <p>
		 * Each energy derivative is scaled to [-1,1].
		 */
		NORM_BALANCED_DERIVATIVES
	}
	
	/**
	 * Mode for normalizing image intensities.
	 */
	@Parameter(label="Image intensity normalization mode",
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 9,
			description = "Normalization mode for image intensities.")
  protected IntensityNormalizationMode intNormMode= 
  		IntensityNormalizationMode.INTENSITY_NORM_TRUE_RANGE;

	/**
	 * Mode for normalizing energies.
	 */
	@Parameter(label="Energy normalization mode",
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 10,
			description = "Normalization mode for snake/energies.")
  protected EnergyNormalizationMode normMode= 
  		EnergyNormalizationMode.NORM_BALANCED_DERIVATIVES;

	/**
	 * Target length of snake segments in resampling.
	 */
	@Parameter(label="Resample Segment Length", mode=ExpertMode.ADVANCED,
		direction=Parameter.Direction.IN, required=false, dataIOOrder = 12,
			description = "Desired resampling segment length.")
  protected Double resampleSegLength= new Double(5.0);

	/**
	 * Flag to enable/disable snake resampling.
	 */
	@Parameter(label="Do Resampling", mode=ExpertMode.ADVANCED,
			direction=Parameter.Direction.IN, required=false, dataIOOrder = 11,
			description = "Flag for snake resampling.")
	protected Boolean	doResampling= new Boolean(true);
	
	/*
   * Some internal convenience variables.
   */
  
	/**
	 * The snake on which the algorithm works.
	 */
	protected transient MTBSnake snake = null;

	/**
	 * Image on which to actually do the segmentation.
	 * <p>
	 * If image normalization is applied to the given input image, i.e. 
	 * if the input image is normalized to intensities in range [0,1], 
	 * this working image contains the normalized image.
	 */
	protected transient MTBImage workingImage;
	
	/**
	 * Old snake of previous iteration (for evolution assessment).
	 */
	protected transient MTBSnake previousSnake = null;

	/**
	 * Flag for invoking energy calculations.
	 * TODO really needed?
	 */
	protected transient boolean energyCalculationRequested = false;

	/**
	 * Current energy of the snake.
	 */
	protected transient double energy = -1.0;

	/**
	 * Snake energy in previous iteration (for change assessment).
	 */
	protected transient double previousEnergy = -1.0;

	/**
	 * Scale factor for snake normalization.
	 */
	protected transient double scaleFactor = 1.0;

	/********************************************************************/
	/*** Internal helpers for optimization procedure. ***/
	/********************************************************************/

//	/**
//	 * List of normalized energy weights.
//	 */
//	protected double [] energyWeightsNormed;

//	protected double minEnergy = Double.MAX_VALUE;
//	protected double minEnergyOld = Double.MAX_VALUE;

  /**
   * Timer object for time measurements.
   */
	protected transient UserTime timer = new UserTime();
  
  /**
   * Image for showing/saving intermediate results.
   */
  protected transient MTBImageRGB dispImg = null;
  
  /********************************************************************/
  /*** Constructors and clone()-methods, validation routines.       ***/
  /********************************************************************/

	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException
	 */
	public SnakeOptimizerSingle() throws ALDOperatorException {
		// nothing to do here
	}
	
  /********************************************************************/
  /*** Helper method for object serialization.                      ***/
  /********************************************************************/
	
	/**
	 * Function for proper initialization of deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being 
	 * deserialized from file, prior to handing the instance over to the 
	 * user. It takes care of a proper initialization of transient member 
	 * variables as they are not initialized to the default values during 
	 * deserialization. 
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

  /********************************************************************/
  /*** (Abstract) methods for operator functionality. ***/
  /********************************************************************/

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		super.validateCustom();
	}
	
  @SuppressWarnings("unused")
  @Override
	protected void initOptimizer() throws MTBSnakeException {

  	// init local variables
		this.iHeight = this.inImg.getSizeY();
		this.iWidth = this.inImg.getSizeX();
		
		// convert color image to multi-channel image
		MTBImage tmpImg = this.inImg;
		this.iChannels = this.inImg.getSizeC();
		if (this.inImg instanceof MTBImageRGB) {
			tmpImg = MTBImage.createMTBImage(
				this.iWidth, this.iHeight, 1, 1, 3, MTBImageType.MTB_BYTE);
			MTBImageRGB cImg = (MTBImageRGB)this.inImg;
			for (int y=0; y<this.iHeight; ++y) {
				for (int x=0; x<this.iWidth; ++x) {
					tmpImg.putValueInt(x, y, 0, 0, 0, cImg.getValueR(x, y));
					tmpImg.putValueInt(x, y, 0, 0, 1, cImg.getValueG(x, y));
					tmpImg.putValueInt(x, y, 0, 0, 2, cImg.getValueB(x, y));
				}
			}
			this.iChannels = 3;
		}

		// normalize input image to range [0,1], save original version
		if (this.verbose.booleanValue()) 
			System.out.println("Normalizing input image to [0,1]...");
		this.workingImage = this.normalizeInputImage(tmpImg);
		// clean-up
		tmpImg = null;
		
		// init the output image
		if (   this.saveIntermediateResults.booleanValue() 
				|| this.showIntermediateResults.booleanValue()) {
			// initialize result image
			this.dispImg = (MTBImageRGB) MTBImage.createMTBImage(this.iWidth,
					this.iHeight, 1, 1, 1, MTBImageType.MTB_RGB);
			this.dispImg.setTitle("Online segmentation result for " 
				+ "\"<" + this.inImg.getTitle() + ">\"");
		} 
//		// collect energy data, if required
//		if (this.sampleEnergyData.booleanValue())
//			this.energySamples = new Vector<Double>();
	}
	  
  /**
   * Display intermediate results by overlaying the input image with 
   * the current snake result. 
   */
  @Override
  protected void showSnake() {
  	if (!(this.inImg instanceof MTBImageRGB)) {
  		// convert input image to byte format; for the moment only the
  		// first channel is considered
  		MTBImageByte background = 
  			(MTBImageByte)this.inImg.convertType(MTBImageType.MTB_BYTE,true);

  		// plot it to the background of the disp image
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
  	// plot the snake to the image
		this.snake.drawPolygon(this.dispImg);
		int yellow = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);
		this.snake.drawPolygonPoints(this.dispImg, yellow, 1 /* shape: X */);
		this.dispImg.updateAndRepaintWindow();
  	this.dispImg.show();
		this.outSnakesImg = (MTBImageRGB)this.dispImg.duplicate();
  }

  @Override
  protected void closeWindows() {
  	if (this.dispImg != null)
  		this.dispImg.close();
  }

  /**
   * Save intermediate results. 
   */
  @Override
  protected void saveSnake() {
  	MTBPolygon2DSet snakeSet = new MTBPolygon2DSet(0, 0, 
  		this.inImg.getSizeX(), this.inImg.getSizeY());
  	snakeSet.add(this.snake);
  	try {
  		// save the snake...
  		snakeSet.write(this.saveIntermediateResultsPath.getDirectoryName() 
  			+	"/snakeSet_" + this.itCounter + ".xml");
  		// ... and a corresponding image
  		ImageWriterMTB writer = new ImageWriterMTB(this.dispImg, 
  				this.saveIntermediateResultsPath.getDirectoryName() + 
  				"/snakeImg_" + this.itCounter + ".tif");
  		writer.runOp(false);
  	} catch (ALDException e) {
  		System.err.println("SnakeOptimizerSingle: cannot write " 
  			+ "intermediate results (path: " + 
  				this.saveIntermediateResultsPath.getDirectoryName() + ")!!!");
  		e.printStackTrace();
  	}
  }

	/**
	 * Check if energies are given.
	 * @return	True, if energies are available.
	 */
	protected abstract boolean hasEnergies();

	@Override
  public void printParams() {
		System.out.println("Snake number= " + this.snakeNum);
		/*
		 * TODO parameter wieder anzeigen ! Testen ob vorhanden oder nicht wird dazu
		 * noch nötig sein. Alle Terminations haben eine get methode für die params
		 * aus dem Konstruktor um maxIterations und die AreaFraction usw abzufragen,
		 * diese kann man dann ausgeben.
		 */
		// System.out.println("Iterations= " + this.getMaxIterations());
		// System.out.println("Min. energy improve= " + this.getEnergyImprove());
		// System.out.println("Point Fraction= " + this.getPointFraction());
		// System.out.println("Area Fraction= " + this.getAreaFraction());
		System.out.println("Resample snake= " + this.doResampling);
		System.out.println("Resampling segment length= " + this.resampleSegLength);
	}

  @Override
  public String toString() {
  	return 
  		new String("SnakeOptimizerSingle - abstract base class object");
  }

  /**
	 * Returns a reference to the set of energies.
	 * @return	Set of energies.
	 */
	public abstract MTBSet_ActiveContourEnergy getEnergySet();

	/**
	 * Specify set of energies.
	 * @param eSet	Set of energies.
	 */
	public abstract void setEnergySet(MTBSet_ActiveContourEnergy eSet);
	
  /* (non-Javadoc)
   * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer#getWorkingImage()
   */
  @Override
  public MTBImage getWorkingImage() {
  	return this.workingImage;
  }
  
	/**
	 * Returns a set with current snake(!).
	 * <p>
	 * In case of the SnakeOptimizerSingle class this set will always have 
	 * exactly one element.
	 */
	@Override
  public MTBPolygon2DSet getCurrentSnakes() {
		if (this.snake == null)
			return null;
		MTBPolygon2DSet pset = 
			new MTBPolygon2DSet(0,0,this.iWidth,this.iHeight);
		pset.add(this.snake.clone());
		return pset;
	}

  /********************************************************************/
  /*** Getters and Setters. ***/
  /********************************************************************/

	/**
	 * Enable energy calculation in each iteration.
	 * TODO really needed?
	 */
	public void enableEnergyCalculation() {
		this.energyCalculationRequested = true;
	}
	
	/**
	 * Disable energy calculations completely.
	 * TODO really needed?
	 */
	public void disableEnergyCalculation() {
		this.energyCalculationRequested = false;
	}

	/**
	 * Set energy normalization mode.
	 */
	public void setNormalizationMode(EnergyNormalizationMode m) {
		this.normMode = m;
	}

	/**
	 * Returns energy normalization mode.
	 */
	public EnergyNormalizationMode getNormalizationMode() {
		return this.normMode;
	}
	
	/**
	 * Request mode for normalizing image intensities.
	 * @return	Mode of image intensity normalization.
	 */
	public IntensityNormalizationMode getIntensityNormalizationMode() {
		return this.intNormMode;
	}
	
	/**
	 * Returns a copy of the current snake.
	 * @return Current snake.
	 */
  public MTBSnake getCurrentSnake() {
		if (this.snake == null)
			return null;
		return this.snake.clone();
	}

	/**
	 * Returns the previous snake.
	 * @return Snake contour of previous iteration.
	 */
	public MTBSnake getPreviousSnake() {
		if (this.previousSnake == null)
			return null;
		return this.previousSnake.clone();
	}

	/**
	 * Get current energy value for the snake.
	 * @return Energy value of the snake.
	 */
	public double getEnergyValue() {
		return this.energy;
	}

	/**
	 * Get old energy value for the snake at last iteration.
	 * @return Old snake energy value of the previous iteration.
	 */
	public double getPreviousEnergyValue() {
		return this.previousEnergy;
	}

	/**
	 * Returns desired segment length for resampling.
	 * @return Desired snake segment length.
	 */
	public Double getSegmentLength() {
		return this.resampleSegLength;
	}

	/**
	 * Returns true if snake should be resampled during optimization.
	 * @return True, if snake is to be resampled.
	 */
	public Boolean doResampleSnake() {
		return this.doResampling;
	}

	/********************************************************************/
	/*** Convenience functions.                                       ***/
	/********************************************************************/
	
	/**
	 * Normalizes image intensities according to normalization mode of 
	 * operator.
	 * @param inimg	Image to be normalized.
	 * @return	Result image with normalized intensities.
	 */
	public MTBImage normalizeInputImage(MTBImage inimg) {

		// init result image
		MTBImage resImg = inimg.duplicate();
		
		// do normalization according to chosen mode
		double sourceMin, sourceMax, targetMin, targetMax, maxAbsVal;
		switch(this.intNormMode)
		{
		case INTENSITY_NORM_TRUE_RANGE:
			double minmax[] = inimg.getMinMaxDouble();
			// tell the user that image contains negative intensities...
			if (minmax[0] < 0) {
				System.err.println(
					"[SnakeOptimizerSingle::normalizeInputImage()]" +
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
			if (this.verbose.booleanValue()) {
				System.out.println("[SnakeOptimizerSingle] normalizing image: "+
						"[ " + sourceMin + " , " + sourceMax + " ] --> " +
						"[ " + targetMin + " , " + targetMax + " ]");
			}
			// process all channels
			for (int i=0; i<this.iChannels; ++i) {
				MTBImage cImg = inimg.getSlice(0, 0, i);
				resImg.setSlice(cImg.scaleValues(
					0, 0, sourceMin, sourceMax, targetMin, targetMax), 0, 0, i);
			}
			break;
		case INTENSITY_NORM_THEORETIC_RANGE:
			maxAbsVal = 
				(Math.abs(inimg.getTypeMin()) > 
				 Math.abs(inimg.getTypeMax()) ? 
						 Math.abs(inimg.getTypeMin()) :
							 Math.abs(inimg.getTypeMax()));
			// only negative values in image
			if (inimg.getTypeMax() < 0) {
				sourceMin = inimg.getTypeMin();
				sourceMax = inimg.getTypeMax();
				targetMin = -1;
				targetMax =  0;
			}
			// negative and positive values
			else if (inimg.getTypeMin() < 0	&& inimg.getTypeMax() >= 0) {
				sourceMin = -maxAbsVal;
				sourceMax =  maxAbsVal;
				targetMin = -1;
				targetMax =  1;
			}
			// only positive values in image
			else {
				sourceMin = inimg.getTypeMin();
				sourceMax = inimg.getTypeMax();
				targetMin = 0;
				targetMax = 1;				
			}
			// normalize the image
			if (this.verbose.booleanValue()) {
				System.out.println("[SnakeOptimizerSingle] normalizing image: "+
						"[ " + sourceMin + " , " + sourceMax + " ] --> " +
						"[ " + targetMin + " , " + targetMax + " ]");
			}
			// process all channels
			for (int i=0; i<this.iChannels; ++i) {
				MTBImage cImg = inimg.getSlice(0, 0, i);
				resImg.setSlice(cImg.scaleValues(
					0, 0, sourceMin, sourceMax, targetMin, targetMax), 0, 0, i);
			}
			break;
		case INTENSITY_NORM_NONE:
		default:
			// nothing to do here
			break;
		}
		return resImg;
	}
	
	/********************************************************************/
	/*** Protected routines.                                          ***/
	/********************************************************************/

	/**
	 * Resamples the snake using the specified segment length.
	 * <p>
	 * Note: if snake is normalized, segment length will also be normalized!
	 * Normalization of segment length is done by resampling!
	 */
	protected void resampleSnake() {
		this.snake.resample(this.resampleSegLength.doubleValue());
	}
}
