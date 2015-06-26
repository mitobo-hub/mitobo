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

// TODO:  output of intermediate results for plugin
// TODO: optional: filename base fuer intermediate LS
// TODO: javadoc for all energy types
package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;

/**
 * Operator intended as user interface for level set segmentation
 * using nonPDE optimization and optionally topology preserving.
 * <br>
 * An image containing the initial segmentation may be supplied.
 * Also a mask image indicating invalid pixels/voxels may be given.
 * <p>
 * <b>Details</b>
 * <p>
 * 
 * 
 * The input image is segmented using a level set technique.
 * Segmentation by be done using two or multiple phases, if <b>multiphase</b> is true.
 * Optionally a topology preserving mode is available (only 2D currently)
 * which assumes initially object phases with exactly one connected component each
 * and no holes. This condition is preserved during optimization.
 * <br>
 * The maximal number of iterations is given as <b>maxiter</b>.
 * <br> 
 * If <b>invalidImg</b> all pixels/voxels in this image with non-zero intensities are
 * interpreted as invalid. This is in principle as if this pixels/voxels would not exist
 * (similar to pixels/voxels outside the image domain):
 * They are neither assigned a phase to, nor are image intensities used, e.g. to compute
 *  gradients.
 * <p>
 * <b>Initialization</b>
 * <br>If no initialization is given, a default initialization,
 * usually a circle/sphere is used.
 * If both <b>initLabelImg</b> and <b>initBinImg</b> are supplied, <b>initLabelImg</b> is used.
 * If a two phase approach is used (i.e. <b>multiphase</b> is false) then both
 * types of initialization are identical: all pixels/voxels with intensity zero are
 * assumed as background (phase), all other as foreground phase.
 * In the multi phase case the labels in <b>initLabelImg</b> are assumed to be the
 * phases to be used for initialization. Again, pixels/voxels with intensity zero are
 * assumed as background (phase).
 * If in the multi phase case <b>initBinImg</b> is given, the image is interpreted
 * as a binary image (zero is again background), a compontent labeling is
 * done and each resulting connected component assign to one phase
 * (in addition to the background phase).
 * <p>
 * <b>Result</b>
 * <p>The final segmentation is output as a label image.
 * <p>
 * <b>Additional Output</b>
 * <p>If <b>verbose</b> is set output is written to stdout.
 * The <b>debug</b> argument is a bit mask to control debuggung output.
 * This is intended for developers and not (well) documented, also behaviour may
 * change over time without notice.
 * If <b>spacing</b> is non-zero, intermediate levelset functions after each <b>spacing</b>-th
 * iteration are output to files with prefix <b>ibase</b> (default intermediateLS)
 * and incrementally numbered. As a zero-th levelset function the initialization is
 * prepended and the final result added as a last levelset image (which might happen
 * to show up twice).
 * Additionally the input image is overlayed with the final contours of the object phases
 * as well as with the contours of the intialization. This image is output
 * to the file <b>contourResult</b> (default: contourImg.tif).
 * 
 * <p><b>Limitations</b>
 * Currently not all options are implemented for the plugin version.
 *
 * @author Stefan Posch, Markus Glass, Birgit Moeller
 *
 */

@ALDAOperator(level=Level.APPLICATION,genericExecutionMode=ExecutionMode.ALL)
public class LevelsetSegmentationNonPDE extends MTBOperator {

	static final Integer DEFAULT_MAXITER = 100;
	static final Double DEFAULT_MU = 1.0;
	static final Double DEFAULT_LAMBDA_FG = 1.0;
	static final Double DEFAULT_LAMBDA_BG = 1.0;
	static final Double DEFAULT_NU = 0.0;
	static final Double DEFAULT_ALPHA = 0.0;
	static final Double DEFAULT_SIGMA = 20.0;
	static final Double DEFAULT_KAPPA = 0.0;

	@Parameter( label= "Input image", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = 0,
		description = "Input image to segment.")
	private transient MTBImage inImg = null;

	@Parameter( label= "Energy", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = 1,
		description = "Energy to use for optimization.")
	private MTBGenericEnergyNonPDE energy;

	@Parameter( label= "Max. iterations", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = 2,
		description = "Maximal number of iterations for optimization.")
	private Integer maxIter = DEFAULT_MAXITER;

	@Parameter( label= "Preserve topology", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = 3,
		description = "Preserve topology during optimization.")
	private Boolean preserveTopology = new Boolean(false);

	@Parameter( label= "Multiphase", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = 4,
		mode = ExpertMode.ADVANCED, description = "Use multiphase level sets.")
	private Boolean multiphase = new Boolean(false);

	@Parameter( label= "Initial segmentation (binary)", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 6,
		description = "Binary image for initialization.")
	private transient MTBImage initBinImg = null;

	@Parameter( label= "Initial segmentation (labeled)", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 5,
		description = "Label image for initialization.")
	private transient MTBImage initLabelImg = null;

	@Parameter( label= "Initial regions", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder = 7,
			description = "Regions for initialization.")
	private MTBRegion2DSet initRegions = null;

	@Parameter( label= "Initialization threshold", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 8, 
		description = "Threshold to initialize the level set function.")
	private Double threshold = new Double(Double.NEGATIVE_INFINITY);

	@Parameter( label= "Invalid pixel map", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 9,
		description = "Image of invalid pixels (pixels != 0 are invalid).")
	private transient MTBImage invalidImg = null;
	
	@Parameter( label= "Result segmentation", direction= Parameter.Direction.OUT,
		description = "Resulting label image.", dataIOOrder = 0)
	private transient MTBImage resultImg;

	@Parameter( label= "Result contour image", direction= Parameter.Direction.OUT,
		description = "Resulting contour image.", dataIOOrder = 1)
	private transient MTBImage resultContourImage;

  @Parameter( label= "Number of iterations", direction= Parameter.Direction.OUT,
    description = "Number of iterations performed.", dataIOOrder = 2)
  private Integer numIterations;

	@Parameter( label= "Sampling rate for intermediate results", 
		direction = Parameter.Direction.IN, dataIOOrder = -2, supplemental = true,
		description = "Spacing of intermediate results (none if spacing == 0).")
	private Integer spacing = new Integer(0);

  @Parameter( label= "Debugging", direction = Parameter.Direction.IN,
		supplemental = true, dataIOOrder = -1, 
		description = "Bit mask for debugging of level set solver.")
	private Integer debug = new Integer(0);

	@Parameter( label= "Intermediate results", required = false, 
		direction = Parameter.Direction.OUT, supplemental = true, dataIOOrder = 3,
		description = "Intermediate level set segmentation results.")
	private transient MTBImage intermediateLS;


	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public LevelsetSegmentationNonPDE() throws ALDOperatorException {
		// nothing to do here
	}

  @Override
  protected void operate() 
  		throws ALDOperatorException,ALDProcessingDAGException {

  	// create solver and set parameters and input arguments 
  	LevelsetSolveNonPDE solver = initSolver();

  	if ( this.getVerbose().booleanValue() ) {
  		solver.print();
  	}
  	solver.runOp();

  	// retrieve outputs of solver and set operator outputs
  	this.setNumIterations( solver.getNumIterations());
  	this.setResultImage( solver.getResultImage());
  	this.resultImg.setTitle(
  			"Level set result for <" + this.inImg.getTitle() + ">");

  	// WARNING, TODO only 2D for now !!
  	if (   solver.getIntermediateLS() != null 
  			&& solver.getIntermediateLS().size() > 0) {
  		this.intermediateLS = MTBImage.createMTBImage( 
  				this.resultImg.getSizeX(), this.resultImg.getSizeY(), 
  				solver.getIntermediateLS().size(), 1, 1, this.resultImg.getType() );
  		for ( int s = 0 ; s < solver.getIntermediateLS().size() ; s++) {
  			this.intermediateLS.setSlice(
  					solver.getIntermediateLS().elementAt(s), s, 0, 0);
  		}
  		this.intermediateLS.setTitle(" Intermediate level set results for <" 
  				+ this.inImg.getTitle() + ">");
  	}
  	MTBImage ctrImg = this.drawContours( this.inImg, solver.getResultImage(), 
  			this.getInitLabelImg());
  	this.setResultContourImage( ctrImg);
  	this.resultContourImage.setTitle(
  			"Level set contours for <" + this.inImg.getTitle() + ">");
  }

	// ===============================================
  // ---- helper functions 

  /**
   * Create a parameter object and set/initialize all objects.
   */
  private LevelsetSolveNonPDE initSolver() throws ALDOperatorException {
  	// initialize level set function
  	MTBLevelsetMembership phi = this.initLS();

  	this.energy = this.energy.init( this.inImg, phi);
  	this.energy.setDebug( this.debug.intValue() );

  	// create solver
  	LevelsetSolveNonPDE solver = new LevelsetSolveNonPDE( this.energy, phi, 
  			this.maxIter.intValue(), this.spacing.intValue(), null, 
  			this.preserveTopology.booleanValue());
  	solver.setVerbose( getVerbose());
  	solver.setDebug( this.debug);
  	return solver;
  }

  /**
   * Construct an initial level set function. 
   * <p>
   * If both initBinImg and initLabelImg are supplied, initLabelImg is 
   * preferred for initialization. If both are null, contours are imported from
   * ImageJ's ROI manager (if available). Only if none the three formerly 
   * mentioned parameters is non-null, the image is binarized applying the 
   * specified threshold.
   * 
   * @see de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.MTBLevelsetMembership
   */
  // TODO: consider invalidImg if intiImg==null

  private MTBLevelsetMembership initLS() {
  	if ( this.initLabelImg != null )  {
  		// initialize level set function from label image
  		return new MTBLevelsetMembership( this.initLabelImg, this.invalidImg, 
  				this.multiphase.booleanValue());
  	} else if ( this.initBinImg != null )  {
  		// initialize level set function from binary image
  		if ( this.multiphase.booleanValue() ) {
  			try {
  				// component labeling with 4-neighborhood
  				LabelComponentsSequential regLabler= 
  						new LabelComponentsSequential( this.initBinImg, false);
  				regLabler.runOp();
  				MTBRegion2DSet regions= regLabler.getResultingRegions();

  				return new MTBLevelsetMembership( 
  					this.initBinImg.getSizeX(), this.initBinImg.getSizeY(), regions, 
  					this.invalidImg, this.multiphase.booleanValue());
  			} catch (ALDException e) {
  				e.printStackTrace();
  				return null;
  			}
  		} 
 			return new MTBLevelsetMembership( this.initBinImg, this.invalidImg, 
 					this.multiphase.booleanValue());
  	} else if (this.initRegions != null && this.initRegions.size() > 0) {
  		// initialize level set function from imported regions
  		return new MTBLevelsetMembership(
 				this.inImg.getSizeX(), this.inImg.getSizeY(), this.initRegions, 
 				this.invalidImg, this.multiphase.booleanValue());
  	}
  	else if ( this.threshold.doubleValue() != Double.NEGATIVE_INFINITY ) {
  		// initialize level set function by thresholding input image
  		if( this.getMultiphase().booleanValue() ) {
  			try {
  				ImgThresh thresholder = 
  						new ImgThresh(this.inImg, this.threshold.doubleValue());
  				thresholder.runOp();
  				MTBImage binaryImg = thresholder.getResultImage();

  				LabelComponentsSequential labeler = 
  						new LabelComponentsSequential(binaryImg, false);
  				labeler.runOp();
  				labeler.getResultingRegions().size();

  				MTBImage labelImg = labeler.getLabelImage();

  				return new MTBLevelsetMembership(labelImg, this.invalidImg, 
  						this.multiphase.booleanValue());
  			} catch (ALDException e) {
  				e.printStackTrace();
  				return null;
  			}
  		} 
  		return new MTBLevelsetMembership(
  			this.inImg.getSizeX(), this.inImg.getSizeY(), this.inImg.getSizeZ(), 
  			this.inImg, this.threshold.intValue(), this.invalidImg);
  	} else {
  		return new MTBLevelsetMembership( 
  			this.inImg.getSizeX(), this.inImg.getSizeY(), this.inImg.getSizeZ(), 
  			this.invalidImg);
  	}
  }

	/** Create a new image where all contour pixels of img are set to 1,
	 *  all other pixels to 0.
	 */
    private static MTBImage getContourImg( MTBImage img) {
		int deltaX[] = new int[] {0,1,1,1,0,-1,-1,-1};
		int deltaY[] = new int[] {-1,-1,0,1,1,1,0,-1};

		MTBImage tmpImg = img.duplicate();
        for ( int y = 0 ; y < tmpImg.getSizeY() ; y++ ) {
            for ( int x = 0 ; x < tmpImg.getSizeX() ; x++ ) {
				int val = img.getValueInt( x, y);
				tmpImg.putValueInt( x, y, 0);
				for ( int n=0 ; n<8 ; n++ ) {
					if ( 0 <= x+deltaX[n] && x+deltaX[n] < tmpImg.getSizeX()  &&
					     0 <= y+deltaY[n] && y+deltaY[n] < tmpImg.getSizeY()  &&
                         val != img.getValueInt( x+deltaX[n], y+deltaY[n]) ) {

						tmpImg.putValueInt( x, y, 1);
						
						break;
                	}
                }
			}
		}

		return tmpImg;
	}

	/** Draw contours of resultImg and if non null initLabelImg 
	 *  into inIMg and return this result in a new image
	 */
    private MTBImage drawContours( MTBImage inImg, MTBImage resultImg, MTBImage initLabelImg) {
		double[] minMaxVal = inImg.getMinMaxDouble();
		// scale intenisites of input inage to output range
		double scale = 0.9*minMaxVal[1]/(double)inImg.getTypeMax(); //minMaxVal[1];

		// first draw contours found in the result
		// with intensity: max * 0.9
		MTBImage ctrImg = getContourImg( resultImg);
        for ( int y = 0 ; y < ctrImg.getSizeY() ; y++ ) {
            for ( int x = 0 ; x < ctrImg.getSizeX() ; x++ ) {
				if (  ctrImg.getValueInt( x, y) == 0 )  
					ctrImg.putValueInt( x, y, (int)(scale*(double)(inImg.getValueInt( x, y))));
				else
					ctrImg.putValueInt( x, y, (int)(0.9*ctrImg.getTypeMax()));
			}
		}

		// if initLabelImg is non null, draw contours of initLabelImg
		// with intensity: max * 0.8
		if ( initLabelImg != null ) {
			MTBImage ctrImg2 = getContourImg( initLabelImg);

            for ( int y = 0 ; y < ctrImg.getSizeY() ; y++ ) {
                for ( int x = 0 ; x < ctrImg.getSizeX() ; x++ ) {
					if (  ctrImg2.getValueInt( x, y) > 0 ) 
						ctrImg.putValueInt( x, y, (int)(0.8*ctrImg.getTypeMax()));
				}
			}
		}


		return ctrImg;
	}

	/** Get value of debug.
	  * Explanation: Bit mask for debugging of levelset solver.
	  * @return value of debug
	  */
	public java.lang.Integer getDebug(){
		return this.debug;
	}

	/** Set value of debug.
	  * Explanation: Bit mask for debugging of levelset solver.
	  * @param value New value of debug
	  */
	public void setDebug( java.lang.Integer value){
		this.debug = value;
	}
    /** Get value of energy.
     * @return value of energy
     */

	public MTBGenericEnergyNonPDE getEnergy() {
		return this.energy;
	}

    /** Set value of energy.
     * @param energy New value of energy
     */

	public void setEnergy(MTBGenericEnergyNonPDE energy) {
		this.energy = energy;
	}



    /** Get value of threshold.
      * Explanation: Maximal number of iterations for optimization.
      * @return value of threshold
      */
    public Double getThreshold(){
            return this.threshold;
    }

    /** Set value of threshold.
      * Explanation: Maximal number of iterations for optimization.
      * @param value New value of threshold
      */
    public void setThreshold( Double value){
        this.threshold = value;
    }

	/** Get value of maxIter.
	  * Explanation: Maximal number of iterations for optimization.
	  * @return value of maxIter
	  */
	public java.lang.Integer getMaxIter(){
		return this.maxIter;
	}

	/** Set value of maxIter.
	  * Explanation: Maximal number of iterations for optimization.
	  * @param value New value of maxIter
	  */
	public void setMaxIter( java.lang.Integer value){
		this.maxIter = value;
	}

    /** Get value of preserveTopology.
      * Explanation: Preserve topology during optimization.
      * @return value of preserveTopology
      */
    public java.lang.Boolean getPreserveTopology(){
            return this.preserveTopology;
    }

    /** Set value of preserveTopology.
      * Explanation: Preserve topology during optimization.
      * @param value New value of preserveTopology
      */
    public void setPreserveTopology( java.lang.Boolean value){
            this.preserveTopology = value;
    }


	/** Get value of spacing.
	  * Explanation: Spacing of intermediate results (none if spacing == 0) solver.
	  * @return value of spacing
	  */
	public java.lang.Integer getSpacing(){
		return this.spacing;
	}

	/** Set value of spacing.
	  * Explanation: Spacing of intermediate results (none if spacing == 0) solver.
	  * @param value New value of spacing
	  */
	public void setSpacing( java.lang.Integer value){
		this.spacing = value;
	}


	/** Get value of multiphase.
	  * Explanation: Use multiphase levelsets.
	  * @return value of multiphase
	  */
	public java.lang.Boolean getMultiphase(){
		return this.multiphase;
	}

	/** Set value of multiphase.
	  * Explanation: Use multiphase levelsets.
	  * @param value New value of multiphase
	  */
	public void setMultiphase( java.lang.Boolean value){
		this.multiphase = value;
	}


	/** Get value of invalidImg.
	  * Explanation: Image of invalid pixels (pixels <> 0 are invalid.
	  * @return value of invalidImg
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getInvalidImg(){
		return this.invalidImg;
	}

	/** Set value of invalidImg.
	  * Explanation: Image of invalid pixels (pixels <> 0 are invalid.
	  * @param value New value of invalidImg
	  */
	public void setInvalidImg( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.invalidImg = value;
	}

	/** Get value of initLabelImg.
	  * Explanation: Label image for initialization.
	  * @return value of initLabelImg
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getInitLabelImg(){
		return this.initLabelImg;
	}

	/** Set value of initLabelImg.
	  * Explanation: Label image for initialization.
	  * @param value New value of initLabelImg
	  */
	public void setInitLabelImg( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.initLabelImg = value;
	}

	/** Get value of inImg.
	  * Explanation: Input image to segment
	  * @return value of inImg
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getInImg(){
		return this.inImg;
	}

	/** Set value of inImg.
	  * Explanation: Input image to segment
	  * @param value New value of inImg
	  */
	public void setInImg( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.inImg = value;
	}

	/** Get value of initBinImg.
	  * Explanation: Binary image for initialization.
	  * @return value of initBinImg
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getInitBinImg(){
		return this.initBinImg;
	}

	/** Set value of initBinImg.
	  * Explanation: Binary image for initialization.
	  * @param value New value of initBinImg
	  */
	public void setInitBinImg( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.initBinImg = value;
	}

	/** Get value of ResultContourImage.
	  * Explanation: Resulting contour image.
	  * @return value of ResultContourImage
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getResultContourImage(){
		return this.resultContourImage;
	}

	/** Set value of ResultContourImage.
	  * Explanation: Resulting contour image.
	  * @param value New value of ResultContourImage
	  */
	public void setResultContourImage( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.resultContourImage = value;
	}

	/** Get value of ResultImage.
	  * Explanation: Resulting label image.
	  * @return value of ResultImage
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getResultImage(){
		return this.resultImg;
	}

	/** Set value of ResultImage.
	  * Explanation: Resulting label image.
	  * @param value New value of ResultImage
	  */
	public void setResultImage( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.resultImg = value;
	}

	/** Get value of numIterations
	  * Explanation: Number of iterations performed
	  * @return value of numIterations
	  */
	public Integer getNumIterations(){
		return this.numIterations;
	}

	/** Set value of numIterations
	  * Explanation: Number of iterations performed
	  * @param New value of numIterations
	  */
	public void setNumIterations( Integer value){
		this.numIterations = value;;
	}

	public MTBImage getIntermediateLS() {
		return this.intermediateLS;
	}


	public void setIntermediateLS(MTBImage intermediateLS) {
		this.intermediateLS = intermediateLS;
	}


}
