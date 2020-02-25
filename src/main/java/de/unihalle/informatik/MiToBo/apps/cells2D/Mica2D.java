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

package de.unihalle.informatik.MiToBo.apps.cells2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.MiToBo.apps.cytoplasm2D.CytoplasmAnalyzer2D;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Complete;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Cytoplasm;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Nuclei;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Particles;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Complete.SegmentationMode;
import de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusDetector2D;
import de.unihalle.informatik.MiToBo.apps.particles2D.MultiChannelParticleAnalyzer2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperatorControllable;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.CalcStructureStatistics;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawPolygon2DSet;

/**
 * Operator for integrated cell image analysis.
 * <p>
 * Given a multi-channel input image this operator detects nuclei, particles
 * and the cell boundary and integrates all results to complete cell statistics. 
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION,
	shortDescription="Operator for integrated cell image analysis.")
public class Mica2D extends MTBOperatorControllable {

	/**
	 * Multi-channel input image.
	 */
	@Parameter( label= "Input image", required = true, direction= Direction.IN, 
			description = "Input image", dataIOOrder = -20)
	private transient MTBImage inImg = null;

	/**
	 * Nuclei channel.
	 */
	@Parameter( label= "Nucleus channel", required = true, dataIOOrder = -19,
		direction= Direction.IN, 
		description= "Nuclei channel (index starts with 1, -1 if none available).")
	private int nucleiChannel= -1;

	/**
	 * Channel for cell boundary detection.
	 */
	@Parameter( label= "Cytoplasm channel", required = true, dataIOOrder = -18,
			direction= Direction.IN, 
			description = "Cytoplasm channel, index starts with 1.")
	private int cellChannel = -1;

	/**
	 * Operator for nucleus detection/separation.
	 */
	@Parameter( label= "Nuclei detector", required = false, dataIOOrder = -20,
		direction= Direction.IN, description= "Nucleus detector.")
	private NucleusDetector2D nucleiDetector = null;	
	
	/**
	 * (Optional) initial snake contours for nuclei.
	 * <p>
	 * If a nucleus channel is available contours can be extracted automatically.
	 */
	@Parameter( label= "Nuclei contours", required = false, dataIOOrder = -19,
		direction= Direction.IN, description= "(Optional) set of nuclei contours.")
	private MTBPolygon2DSet nucleiContours = null;

	/**
	 * Cytoplasma detection operator.
	 */
	@Parameter( label= "Cytoplasm detector", required = false, dataIOOrder = -18,
			direction= Direction.IN, description = "Cytoplasm detector.")
	private CytoplasmAnalyzer2D cytoDetector = null;

	/**
	 * Preprocess cytoplasm channel by Gaussian smoothing.
	 */
	@Parameter( label= "Apply Gauss to cytoplasm channel", required = false,	
			direction= Direction.IN, dataIOOrder = -17,
			description = "Apply Gaussian smoothing to cytoplasm channel.")
	private boolean doGauss = false;

	/**
	 * Sigma for Gaussian smoothing.
	 */
	@Parameter( label= "Gauss sigma", required = false, direction= Direction.IN, 
			description = "Sigma for Gaussian smoothing.", dataIOOrder = -16)
	private double gaussSigma = 4.0;
	
	/**
	 * Particle detector.
	 */
	@Parameter( label= "Particle detector", required = false, dataIOOrder = -15,
			direction= Direction.IN, description = "Minimum size of particles.")
	private MultiChannelParticleAnalyzer2D particleDetector = null;

	/**
	 * Flag to ignore particle in nuclei regions.
	 */
	@Parameter( label= "Ignore particles in nuclei regions", required = false, 
			dataIOOrder = -14,
			direction= Direction.IN, description = "Ignore particles in nucleis.")
	private boolean excludeParticlesInNuclei = false;
	
	/**
	 * Units used in measurements, default is pixels.
	 */
	@Parameter( label= "Measure Units", required = false,	
			direction= Direction.IN, dataIOOrder = -10,
			description = "Units for measuring areas and sizes.")
	private MeasureUnit units = MeasureUnit.pixels;
	
	/**
	 * Result data object.
	 */
	@Parameter( label= "Result data summary", direction=Direction.OUT, 
			description = "Result segmentation masks.")
	private transient SegResult_Complete resultData = null;
	
	/**
	 * Result data object.
	 */
	@Parameter( label= "Table of result statistics", direction=Direction.OUT, 
			description = "Result statistical data.")
	private transient Mica2DTableModel resultStats = null;
	
	/*
	 * supplementals
	 */
	
	/**
	 * Flag for preparing final result image(s) stack.
	 */
	@Parameter( label= "Show final result image stack",
			direction = Direction.IN, supplemental = true, dataIOOrder = -30,
			mode = ExpertMode.STANDARD,
			description = "Flag to enable result image stack display.")
	private boolean prepareFinalResultStack = false;

	/**
	 * Flag for showing intermediate results.
	 * <p>
	 * Be careful when using this option remote from commandline!
	 */
	@Parameter( label= "Show intermediate results",
			direction = Direction.IN, supplemental = true, dataIOOrder = -20, 
			description = "Flag for showing intermediate result image(s)/data.")
	private boolean showIntermediateResults = false;

	/**
	 * Flag for saving intermediate results to a specified directory.
	 */
	@Parameter( label= "Save intermediate results",
			direction = Direction.IN, supplemental = true, dataIOOrder = -19,
			mode = ExpertMode.ADVANCED,
			description = "Flag for saving intermediate result image(s)/data.")
	private boolean saveIntermediateResults = false;

	/**
	 * Path where to save intermediate results.
	 */
	@Parameter( label= "Save intermediate results to...",
			direction = Direction.IN, supplemental = true, dataIOOrder = -18,
			mode = ExpertMode.ADVANCED,
			description = "Path where to save intermediate results.")
	private ALDDirectoryString saveIntermediateResultPath = 
		new ALDDirectoryString("/tmp");

	/**
	 * Set of image channels to copy into result stack.
	 */
	@Parameter( label= "List of channel IDs to copy to result", 
			supplemental = true, direction = Direction.IN, 
			mode = ExpertMode.ADVANCED, dataIOOrder = -15,
			description = "Array of image channels to copy to result stack.")
	private int [] channelsToCopy = null;

	/**
	 * If flag is true, only masks are copied to result stack.
	 */
	@Parameter( label= "Show b/w masks", dataIOOrder = -16,
			direction = Direction.IN, supplemental = true, 
			mode = ExpertMode.ADVANCED,
			description = "Flag for showing masks only instead of overlays.")
	private boolean imagesAsMasks = false;

	/*
	 * Internal helpers.
	 */
	
	/**
	 * Local container for cell cytoplasm segmentation result.
	 */
	private transient SegResult_Cytoplasm resultDataCells = null;
	/**
	 * Local container for set of segmentation result and channel copy images.
	 */
	private transient Vector<MTBImage> resultImages = null;

  /**
   * Default constructor.
   * @throws ALDOperatorException
   */
  public Mica2D() throws ALDOperatorException {
  	// nothing to do here
  }

  /**
   * Default constructor.
   *
   * @param img							Input multi-layer image to be processed.
   * @param nc							Nuclei channel.
   * @param cc							Cytoplasm channel.
   * @throws ALDOperatorException
   */
  public Mica2D(MTBImage img, int nc, int cc) 
  	throws ALDOperatorException {
  	this.nucleiChannel= nc;
		this.cellChannel = cc;
  	this.inImg = img;
  }

  @Override
  public boolean supportsStepWiseExecution() {
  	return false;
  }
  
  /**
   * Specify set of initial snakes.
   */
  public void setInitialSnakes(MTBPolygon2DSet f) {
  	this.nucleiContours = f;
  }
  
  /**
   * Set flag to display intermediate results.
   */
  public void showIntermediateResults(boolean flag) {
  	this.showIntermediateResults = flag;
  }
  
  /**
   * Set flag to save intermediate results.
   */
  public void saveIntermediateResults(boolean flag) {
  	this.saveIntermediateResults = flag;
  }

	/**
	 * Returns path where to save intermediate results.
	 */
	public String getSaveIntermediateResultPath() {
  	return this.saveIntermediateResultPath.getDirectoryName();
  }

	/**
	 * Set path where to save intermediate results.
	 */
	public void setSaveIntermediateResultPath(String sIntermediateResultPath) {
  	this.saveIntermediateResultPath = 
  		new ALDDirectoryString(sIntermediateResultPath);
  }

  /**
   * Specify list of channels to copy to result stack.
   */
  public void setChannelsToCopy(int [] carray) {
  	this.channelsToCopy = carray;
  }
  
  /**
   * Set flag indicating whether to show masks or overlays in result stack.
   */
  public void showMasksOnly(boolean flag) {
  	this.imagesAsMasks = flag;
  }
  
  /**
   * Specify how to measure lengths and areas.
   */
  public void setMeasureUnits(MeasureUnit mu) {
  	this.units = mu;
  }
  
	/**
	 * Specify if cell channel is to be smoothed prior to detection.
	 */
	public void setDoGauss(boolean _doGauss) {
  	this.doGauss = _doGauss;
  }

	/**
	 * Specify sigma for Gaussian smoothing.
	 */
	public void setGaussSigma(double _gaussSigma) {
  	this.gaussSigma = _gaussSigma;
  }

  /**
   * Returns result data object.
   */
  public SegResult_Complete getResultData() {
  	return this.resultData;
  }
  
  /**
   * Invokes analysis procedure on given image.
   * @throws ALDOperatorException 
   * @throws ALDProcessingDAGException 
   */
  @Override
  protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {

  	// reset of operator
  	this.resultData = null;
  	this.resultDataCells = null;
  	this.resultImages = null;
  	this.resultStats = null;
  	
  	// activate recursive event handling
  	this.notifyListenersRecursively = true;

//		boolean prepareResultImage = 
//			this.saveIntermediateResults || this.showIntermediateResults;
		
    // clip the image by throwing away the outer 2 cols/rows on each side
    MTBImage currentImage= this.inImg.getImagePart(0, 0, 0, 0, 0, 
  			this.inImg.getSizeX(), this.inImg.getSizeY(), this.inImg.getSizeZ(), 
  			this.inImg.getSizeT(), this.inImg.getSizeC());
    String imageName = this.inImg.getLocation();
    if (imageName == null)
    	imageName = this.inImg.getImagePlus().getTitle();
    currentImage.setLocation(imageName);
  	int width = currentImage.getSizeX();
  	int height = currentImage.getSizeY();

    // prepare result data stack, i.e. copy some channels if requested
  	if (this.prepareFinalResultStack) {
  	  this.resultImages= new Vector<MTBImage>();
  	}
  	
    // apply the detector(s)
    int channels= currentImage.getSizeC();
    MTBImageRGB kernelResult = null;
    
		/*
		 * nucleus detection
		 */

    // nuclei detection: check if nucleus channel available and run detector
    SegResult_Nuclei nuclResult = null;
    if (this.nucleiChannel != -1 && this.nucleiDetector != null) {
			MTBImageShort kernelImg = (MTBImageShort) MTBImage.createMTBImage(
					width, height,1,1,1,MTBImage.MTBImageType.MTB_SHORT);
			for (int y = 0; y < height; ++y)
				for (int x = 0; x < width; ++x)
					kernelImg.putValueInt(x, y, 
							currentImage.getValueInt(x, y, 0, 0, this.nucleiChannel - 1));
			kernelImg.setTitle("nucleus channel of image \"" + 
							currentImage.getTitle() + "\"...");
			this.nucleiDetector.setInputImage(kernelImg);
			this.nucleiDetector.runOp(false);
			nuclResult = this.nucleiDetector.getResultData(); 
			nuclResult.setNucleusChannel(this.nucleiChannel);

			// visual result preparation
			if ( this.prepareFinalResultStack ) {

				kernelResult= (MTBImageRGB)kernelImg.convertType(
						MTBImage.MTBImageType.MTB_RGB, true);

				// if image overlay is desired use RGB image
				MTBImageByte kernelMask = nuclResult.getMask();
				if (!this.imagesAsMasks) {
					// add result data
					for (int y=0;y<height;++y) {
						for (int x=0;x<width;++x) {
							if (kernelMask.getValueInt(x, y) > 0) {
								kernelResult.putValueR(x, y, 255);
								kernelResult.putValueG(x, y, 0);
								kernelResult.putValueB(x, y, 0);
							}
						}
					}
				}
				// ... else use 8-bit binary image
				else {
					kernelResult.fillBlack();
					// add result data
					for (int y=0;y<height;++y) {
						for (int x=0;x<width;++x) {
							if (kernelMask.getValueInt(x, y) > 0) {
								kernelResult.putValueR(x, y, 255);
								kernelResult.putValueG(x, y, 255);
								kernelResult.putValueB(x, y, 255);
							}
						}
					}
				}
				if (this.prepareFinalResultStack) {
					this.resultImages.add(kernelResult);
				}
			}
		} else {
			System.out.println("[Mica2D] Info: no nuclei data given");
		}
    
		/*
		 * particle detection
		 */

    // check if there were detectors provided
    Vector<SegResult_Particles> particleResults = null;
    if (   this.particleDetector != null
    		&& this.particleDetector.getDetectors().size()!=0) {

			// check if there are nuclei given -> mask them visually
			MTBImageByte nucMask = null;
			if (nuclResult != null)
				nucMask = nuclResult.getMask();	
			
    	this.particleDetector.setInputImage(currentImage);
    	if (this.excludeParticlesInNuclei && nucMask != null)
    		this.particleDetector.setNucleiMask(nucMask);
    	this.particleDetector.runOp(false);
    	particleResults =	
    			this.particleDetector.getResultDataArray().getResultVec();
    
			int channel = 0;
			for (SegResult_Particles res: particleResults) {
				channel++;
				if (res == null) 
					continue;
				
				MTBImageRGB finalResult = (MTBImageRGB)MTBImage.createMTBImage(
					width, height, 1, 1, 1, MTBImage.MTBImageType.MTB_RGB);
				finalResult.fillBlack();
			
				// if image overlay is desired use RGB image
				if (!this.imagesAsMasks) {

					double colorScale = 1.0;
					switch (currentImage.getType())
					{
					case MTB_BYTE:
						// 8-bit image
						colorScale = 1.0;
						break;
					case MTB_SHORT:
						// 16-bit image, or only 12-bit?!
						colorScale = 256.0;
						if (currentImage.getMinMaxDouble()[1] <= 4095)
							// not more than 12-bit used...
							colorScale = 16.0;
						break;
					default:
						// unknown
					}
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							int color= 
								(int)(currentImage.getValueInt(x,y,0,0,channel-1)/colorScale);
							finalResult.putValueR(x, y, color);
							finalResult.putValueG(x, y, color);
							finalResult.putValueB(x, y, color);
						}
					}
				}
				// paint result data into image
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						if (this.imagesAsMasks) { // 8-bit
							// structures in white
							if (res.getMask().getValueInt(x, y) > 0) {
								if (nucMask != null && nucMask.getValueInt(x, y) == 0) {
									// structure not in nucleus
									finalResult.putValueInt(x, y, ((255 & 0xff) << 16)
											+ ((255 & 0xff) << 8) + (255 & 0xff));
								}
								else if (nucMask != null) {
									// structure in nucleus region
									finalResult.putValueInt(x, y, ((120 & 0xff) << 16)
											+ ((120 & 0xff) << 8) + (120 & 0xff));
								}
								else {
									// ignore nuclei
									finalResult.putValueInt(x, y, ((255 & 0xff) << 16)
											+ ((255 & 0xff) << 8) + (255 & 0xff));
								}
							}
						}
						else {
							// structures in yellow
							if (res.getMask().getValueInt(x, y) > 0) {
								if (nucMask != null && nucMask.getValueInt(x, y) == 0) {
									// structure not in nucleus
									finalResult.putValueInt(x, y, ((255 & 0xff) << 16)
											+ ((255 & 0xff) << 8) + (0 & 0xff));							
								}
								else if (nucMask != null) {
									// structure in nucleus region
									finalResult.putValueInt(x, y, ((0 & 0xff) << 16)
											+ ((0 & 0xff) << 8) + (255 & 0xff));
								}
								else {
									// ignore nuclei
									finalResult.putValueInt(x, y, ((255 & 0xff) << 16)
											+ ((255 & 0xff) << 8) + (0 & 0xff));							
								}
							}
						}
					}
				}
				// put final result on stack
				if (this.prepareFinalResultStack)
					this.resultImages.add(finalResult);
			} // end of for-loop: all channels
    }
    else {
    	if (this.verbose.booleanValue()) {
    		System.out.println(
   				"[Mica2D] No particle detectors - skipping particle detection...");
    	}
    }

		/*
		 * cell detection (requires result of nuclei detection!)
		 */
    if ( this.cellChannel != -1 ) {
    	
    	// check if a nuclei detection result is given for snake initialization
    	if (this.nucleiChannel != -1 && nuclResult != null ) {
    		// extract initial contours from nuclei mask
    		this.nucleiContours= 
    			MTBSnake.convertRegionsToSnakes(nuclResult.getMask());
    	}

    	// here initial snakes should be available, if not, 
    	// no segmentation is possible
    	if (this.nucleiContours != null) {

    		// extract the channel to be processed
    		MTBImage tmpCellSlice= currentImage.getImagePart(0, 0, 0, 0, 
   				this.cellChannel-1, currentImage.getSizeX(), currentImage.getSizeY(), 
   				currentImage.getSizeZ(), currentImage.getSizeT(), 1);
    		MTBImageShort cellSlice= 
    			(MTBImageShort)tmpCellSlice.convertType(MTBImageType.MTB_SHORT, true); 

    		// optionally preprocess the channel image
    		if (this.doGauss) {
    			GaussFilter gaussOp = 
    				new GaussFilter(cellSlice, this.gaussSigma, this.gaussSigma);
    			gaussOp.runOp(false);
    			// convert smoothed image back from 32- to 16-bit
    			cellSlice = (MTBImageShort)(
    					gaussOp.getResultImg().convertType(MTBImageType.MTB_SHORT,true));
    		}
    		
    		// init the cytoplasma segmentation operator
//    		CytoplasmAnalyzer2D cytoAnalyzer;
    		try {
//    			cytoAnalyzer = new CytoplasmAnalyzer2D(cellSlice, this.nucleiContours, 
//    					this.snakeConfigObj, DetectMode.iterativeVoronoiExpansion,
//    					this.maxLevels);
    			this.cytoDetector.setInputImage(cellSlice);
    			this.cytoDetector.setInitialSnakes(this.nucleiContours);
    			this.cytoDetector.setVerbose(this.getVerbose());
    			if (this.showIntermediateResults)
    				this.cytoDetector.enableShowIntermediateResults();
    			else
    				this.cytoDetector.disableShowIntermediateResults();
    			if (this.saveIntermediateResults)
    				this.cytoDetector.enableSaveIntermediateResults();
    			else	
    				this.cytoDetector.disableSaveIntermediateResults();
    			this.cytoDetector.setIntermediateResultDirectory(
    					this.saveIntermediateResultPath.getDirectoryName());
    			// register analyzer as listener
    			this.addALDControlEventListener(this.cytoDetector);
    			// start the segmentation
    			this.cytoDetector.runOp(false);
    			if (this.verbose.booleanValue())
    				System.out.println("==== Cell segmentation completed....");
    			// remove listener
    			this.removeALDControlEventListener(this.cytoDetector);
    			// get result data
    			this.resultDataCells= this.cytoDetector.getResultData(); 
    			// add result image to stack - if available
    			MTBImageRGB tmpImg = this.resultDataCells.getResultCellImg();
    			if (tmpImg != null) {
    				// if available, plot nucleus regions into the image
    				if (nuclResult != null) {
    					MTBRegion2DSet nucs = nuclResult.getNucleiRegions();
    					for (int r=0;r<nucs.size();++r) {
    						MTBRegion2D reg = nucs.elementAt(r);
    						MTBContour2D regc = reg.getContour();
    						for (Point2D.Double p : regc.getPoints()) {
    							tmpImg.putValueR((int)p.x, (int)p.y, 0);
    							tmpImg.putValueG((int)p.x, (int)p.y, 255);
    							tmpImg.putValueB((int)p.x, (int)p.y, 0);
    						}		
    					}
    				}
    				// otherwise plot initial snake contours
    				else if (this.nucleiContours != null) {
    					DrawPolygon2DSet drawOp = 
    						new DrawPolygon2DSet(this.nucleiContours, tmpImg);
    					drawOp.setColor("green");
    					drawOp.runOp(false);
    					tmpImg = (MTBImageRGB)drawOp.getResultImage();
    				}
    				if (this.prepareFinalResultStack)
    					this.resultImages.add(tmpImg);
    			}
    			// convert label image to RGB
    			tmpImg = 
    				(MTBImageRGB)this.resultDataCells.getLabelImage().convertType(
    					MTBImageType.MTB_RGB, false);
  				if (this.prepareFinalResultStack)
  					this.resultImages.add(tmpImg);
    		} catch (ALDException e) {
    			e.printStackTrace();
    		}	
    	}
    } // end of cell cytoplasma segmentation

		/*
		 * prepare result object 
		 */
    
    // no individual cell segmentation
    SegResult_Complete resObj = null;
    if (this.cellChannel == -1) {
    	System.out.println("Here we are... preparing output...");
			resObj= new SegResult_Complete(imageName,channels,
					SegmentationMode.IMAGE_COMPLETE,null);
			if (particleResults != null) {
				int i=0;
				// we have one particle detection result per channel
				for (SegResult_Particles pr : particleResults) {
					if (pr.getProcessedChannel() == i+1) {
						resObj.setParticleResult(pr, i);
					}
					else {
						while (pr.getProcessedChannel() != i+1) {
							resObj.setParticleResult(null, i);
							++i;
						}
						resObj.setParticleResult(pr, i);
					}
					i++;
				}
			}
		}
		// individual cell segmentation has been performed
		else {
			resObj = new SegResult_Complete(imageName,channels,
					SegmentationMode.INDIVIDUAL_CELLS,this.resultDataCells);
			for (int i=0;i<channels;++i)
				resObj.setParticleResult(null, i);
			CalcStructureStatistics statsOp;
			if (particleResults != null) {
				// we have one particle detection result per channel
				for (SegResult_Particles pr : particleResults) {
					if (pr == null)
						continue;
					// exclude structures in nuclei regions, if mask available
					MTBImageByte nuclMask = null;
					if (nuclResult != null)
						nuclMask = nuclResult.getMask();
					// count particles per cell
					statsOp= new CalcStructureStatistics(pr.getMask(), nuclMask,
							this.resultDataCells.getLabelImage());
					statsOp.runOp(false);
					pr.setPerCellCount(statsOp.getResultDataCounts());
					pr.setPerCellAvgSize(statsOp.getResultDataAvgSize());
					resObj.setParticleResult(pr, pr.getProcessedChannel()-1);
				}
			}
		}
    // add general results...
		if (nuclResult != null) {
			resObj.setNucleiResult(nuclResult);
		}
		// generate result image stack
		if (this.resultImages != null && !this.resultImages.isEmpty())
			resObj.setResultImageStack(this.prepareResultImageStack());
		// ... and make results externally available
		this.resultData = resObj;
		this.resultStats = new Mica2DTableModel(0, 13);
		this.resultStats.addNewResult(resObj);
  }
	
  /**
   * Initialize result image stack.
   * <p>
   * Transfers result images and selected input channels to a
   * multi-channel RGB image. 
   */
  private MTBImageRGB prepareResultImageStack() {

    String imageName = this.inImg.getLocation();
    if (imageName == null)
    	imageName = this.inImg.getImagePlus().getTitle();
    int width = this.inImg.getSizeX();
    int height = this.inImg.getSizeY();

    // copy input data to result stack, if desired
    int[] bitMask = new int[6]; /* maximal 5 channels allowed */
  	if (this.channelsToCopy != null) {
  		for (int c : this.channelsToCopy) {
  			if (c>0 && c<6) {
  				bitMask[c] = 1;
  			}
  			else {
  				System.err.println("[MiCA] " + 
 						"Array of channels to copy contains invalid channel '"+c+"'...");
  			}
  		}
  	}

  	for (int i = 1; i < 5; ++i) {
  		if (bitMask[i] == 1 && !this.imagesAsMasks) {
  			// add input image channel to result stack
  			MTBImage copy= MTBImage.createMTBImage(width, height, 
  					1, 1, 1, MTBImage.MTBImageType.MTB_RGB);
  			copy.fillBlack();

  			// copy gray data into rgb image
  			double colorScale = 1.0;
  			switch (this.inImg.getType())
  			{
  			case MTB_BYTE:
  				// 8-bit image
  				colorScale = 1.0;
  				break;
  			case MTB_SHORT:
  				// 16-bit image, or only 12-bit?!
  				colorScale = 256.0;
  				if (this.inImg.getMinMaxDouble()[1] <= 4095)
  					// not more than 12-bit used...
  					colorScale = 16.0;
  				break;
  			default:
  				// unknown
  			}
  			for (int y = 0; y < height; ++y) {
  				for (int x = 0; x < width; ++x) {
  					int color= (int)(this.inImg.getValueInt(x,y,0,0,i-1)/colorScale);
  					((MTBImageRGB)copy).putValueR(x, y, color);
  					((MTBImageRGB)copy).putValueG(x, y, color);
  					((MTBImageRGB)copy).putValueB(x, y, color);
  				}
  			}
  			// insert at front of vector
  			this.resultImages.insertElementAt(copy,i-1);
  		} 
  	} // end of for-loop: check all channels
  		
    // set image title to current path, but if it is too long,
  	// cut the path to the last three components
  	String title = new String();
  	String path = imageName;
  	String[] pathParts = path.split("/");
  	int parts = pathParts.length;
  	if (parts > 4)
  		title = ".../" + pathParts[parts - 3] + "/" + pathParts[parts - 2]
                                            + "/" + pathParts[parts - 1];
  	else
  		title = path;
  	// insert suffix to uniquely identify image
  	title = ALDFilePathManipulator.removeExtension(title) + "-resultStack";
  	
  	MTBImageRGB mtbStack= 
  		(MTBImageRGB)(MTBImage.createMTBImage(width, height, 1, 1,
  				this.resultImages.size(), MTBImage.MTBImageType.MTB_RGB));
  	for (int i=0; i<this.resultImages.size(); ++i)
  		mtbStack.setImagePart(this.resultImages.get(i), 0, 0, 0, 0, i);
  	mtbStack.setTitle(title);
  	mtbStack.show();
  	return mtbStack;
  }
  
  @Override
  public String getDocumentation() {
  	return "<p>The MiCA application offers basic functionality to analyse 2D fluorescense microscopy images of cells. Features:</p>\n" + 
  			"\n" + 
  			"<ul><li>\n" + 
  			"<p>algorithms for cell boundary detection based on active contours</p>\n" + 
  			"</li><li>\n" + 
  			"<p>methods for sub-cellular structure detection based on wavelets</p>\n" + 
  			"</li><li>\n" + 
  			"<p>integrated statistical analysis of segmentation results like counts of structures per cell or other statistical numbers</p>\n" + 
  			"</li><li>\n" + 
  			"<p>visual and tabular result data presentation</p>\n" + 
  			"</li></ul>\n" + 
  			"<h2>Usage:</h2>\n" + 
  			"<h3>Required parameters:</h3>\n" + 
  			"\n" + 
  			"<ul><li>\n" + 
  			"<p><tt>Input image</tt> \n" + 
  			"<ul><li>\n" + 
  			"<p>the multi-channel image to be analyzed</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Nucleus channel</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>index of image channel with labeled nuclei</p>\n" + 
  			"</li><li>\n" + 
  			"<p>the channel index starts with 1</p>\n" + 
  			"</li><li>\n" + 
  			"<p>if the channel index is set to -1, nuclei detection is disabled</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Cytoplasm channel</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>the channel of the image to use for cell cytoplasm segmentation</p>\n" + 
  			"</li><li>\n" + 
  			"<p>the channel index starts with 1</p>\n" + 
  			"</li><li>\n" + 
  			"<p>if the channel index is set to -1, cytoplasm detection is disabled</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li></ul>\n" + 
  			"<h3>Optional parameters:</h3>\n" + 
  			"\n" + 
  			"<ul><li>\n" + 
  			"<p><tt>Nuclei detector</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>operator to be used for nuclei detection, for details refer to the documentation about <a href=\"stml:de.unihalle.informatik.MiToBo.apps.nuclei2D\">Nucleus Detection</a> in MiToBo</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Nuclei contours</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>if a channel index for nuclei detection is specified as required parameter this parameter is ignored</p>\n" + 
  			"</li><li>\n" + 
  			"<p>otherwise you can specify nuclei contours here</p>\n" + 
  			"</li><li>\n" + 
  			"<p>to load contours use one of the following modes:\n" + 
  			"<ol><li>\n" + 
  			"<p>ROI&nbsp;MANAGER: requires selections in the ImageJ ROI Manager</p>\n" + 
  			"</li><li>\n" + 
  			"<p>MTB&nbsp;XML: read file with contours saved in MiToBo XML format</p>\n" + 
  			"</li><li>\n" + 
  			"<p>SERIAL&nbsp;XML - not fully supported yet</p>\n" + 
  			"</li><li>\n" + 
  			"<p>MANUAL: manually enter contours by specifying list of points</p>\n" + 
  			"</li></ol>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p>pressing the \"Load\" button will import the selected contours.</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Cytoplasm detector</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>operator to be used for detecting the cell cytoplasm, for details refer to the documentation about <a href=\"stml:de.unihalle.informatik.MiToBo.apps.cytoplasm2D\">Cytoplasm Detection</a> in MiToBo </p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Apply Gauss to cytoplasm channel</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>enables/disables optional Gaussian smoothing of the channel for cytoplasm segmentation prior to the actual segmentation</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Gauss sigma</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>the standard deviation to be used for Gaussian smoothing</p>\n" + 
  			"</li><li>\n" + 
  			"<p>if Gaussian smoothing is disabled the parameter is ignored</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Particle Detector</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>operator to be used for particle detection, for details refer to the documentation about <a href=\"stml:de.unihalle.informatik.MiToBo.apps.particles2D\">Particle Detection</a> in MiToBo</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Measurement Units</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>currently not used</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li></ul>\n" + 
  			"<h3>Supplemental parameters:</h3>\n" + 
  			"\n" + 
  			"<ul><li>\n" + 
  			"<p><tt>Show final result image stack</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>disables/enables the display of a stack with all segmentation results, i.e. nuclei and particle masks as well as cell cytoplasm contours</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Show intermediate results</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>disables/enables display of intermediate results during operator execution</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Save intermediate results</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>disables/enables the saving of intermediate results to disk</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Save intermediate results to...</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>path where to save intermediate results</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Show b/w masks</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>enables display of binary masks</p>\n" + 
  			"</li><li>\n" + 
  			"<p>if disabled colored overlays of the segmentation results are shown</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>List of channel IDs to copy to result</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>allows to specfiy a list of channel indices for copying related channels to the result stack (ignored if stack option is disabled)</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li><li>\n" + 
  			"<p><tt>Verbose</tt>\n" + 
  			"<ul><li>\n" + 
  			"<p>disables/enables output of additional messages on console</p>\n" + 
  			"</li></ul>\n" + 
  			"</p>\n" + 
  			"</li></ul>";
  }
}
