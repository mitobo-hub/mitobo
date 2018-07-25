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

package de.unihalle.informatik.MiToBo.apps.cytoplasm2D;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Cytoplasm;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperatorControllable;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.morphology.*;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;


/**
 * Operator to segment cell contours from a given single-layer image.
 * <p>
 * This operator allows to iteratively segment cell membrane and 
 * cytoplasm, respectively. It basically relies on snakes applying them 
 * either in an iterative fashion or without iterative levels.
 * In the first case between the different snake runs the segmented 
 * cell regions are expanded by voronoi dilation and their internal areas 
 * are masked to enforce further region growth by decreasing the regions'
 * average intensity values.
 * <p> 
 * Further details about the iterative method can be found in the paper:
 * 
 * B. Möller, N. Stöhr, S. Hüttelmaier and S. Posch,
 * "Cascaded Segmentation of Grained Cell Tissue with Active Contour Models".
 * In Proc. of Int. Conf. on Pattern Recognition (ICPR '10), August 2010. 
 * 
 * <p>
 * Details about the non-iterative approach can be found in:
 * 
 * B. Möller and S. Posch,
 * "MiCA - Easy Cell Image Analysis with Normalized Snakes".
 * In Proc. of Workshop on Microscopic Image Analysis 
 * with Applications in Biology (MIAAB '11), Heidelberg, Germany, Sep. 2011.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level= Level.APPLICATION)
public class CytoplasmAnalyzer2D extends MTBOperatorControllable {

	/**
	 * Operator mode.
	 */
	public static enum DetectMode {
		/**
		 * Iterative expansion of contours over multiple levels.
		 */
		iterativeVoronoiExpansion,
		@Deprecated
		meanVarEnergies
	}
	
	/**
	 * Single-layer input image to be processed.
	 */
	@Parameter(label="Input image",required=true, direction=Direction.IN, 
			description= "Input Image.", dataIOOrder= -20, mode=ExpertMode.STANDARD)
	protected transient MTBImage inImg= null;

	/**
	 * Initial snakes for cells, e.g. nuclei contours.
	 */
	@Parameter(label="Initial snake(s)",required=true, direction=Direction.IN, 
			description = "Initial snakes.", dataIOOrder=-19,
			mode= ExpertMode.STANDARD)
	protected MTBPolygon2DSet initialSnakes= null;

  /**
   * Snake optimizer. 
   */
  @Parameter(label="Snake optimizer", required=true, direction=Direction.IN,
  		description = "Snake optimizer object.", dataIOOrder=-18,
  		mode= ExpertMode.STANDARD)
  protected SnakeOptimizerCoupled snakeOpter = null;

  /**
	 * Operator mode to run.
	 */
	@Parameter(label="Operation mode", required=false, direction=Direction.IN,
			description = "Mode of operation.", dataIOOrder= -15,
			mode= ExpertMode.ADVANCED)
	protected DetectMode detectionMode= DetectMode.iterativeVoronoiExpansion;

	/**
	 * Maximum number of levels to perform.
	 */
	@Parameter(label="Max. level count", required=false, direction=Direction.IN,
			description = "Maximum levels in iterative mode.",
			dataIOOrder= -14, mode= ExpertMode.ADVANCED)
	protected int maxLevels= 1;

	/**
	 * Maximum expansion by Voronoi dilation.
	 */
	@Parameter(label="Max. distance in voronoi expansion", 
			required= false, direction=Direction.IN,
			description = "Maximum expansion during Voronoi dilation.",
			dataIOOrder= -13, mode= ExpertMode.ADVANCED)
	protected int maxExpansion= 10;

	/**
	 * Lower threshold for region growth.
	 */
	@Parameter(label="Min. area growth", required=false, direction=Direction.IN,
			description = "Minimum admissible area growth between iterations.",
			dataIOOrder= -12, mode= ExpertMode.ADVANCED)
	protected double minAreaGrowth= 0.013;

	/**
	 * Minimum admissible variance in new snake interior fractions..
	 */
	@Parameter(label="Min. intensity variance", required=false,
			direction=Direction.IN,
			description = "Minimum admissible area growth between iterations.",
			dataIOOrder= -11, mode= ExpertMode.ADVANCED)
	protected double minIntensityVariance= 100;

	/**
	 * Result segmentation image.
	 */
	@Parameter(label="Result image",required=true, direction= Direction.OUT,
			description = "Result image showing snakes in overlay.")
	protected transient MTBImageRGB resultImage= null;

	/**
	 * Result data object.
	 */
	@Parameter(label="Result Statistics",required=true, direction= Direction.OUT,
			description = "Result data object.")
	protected transient SegResult_Cytoplasm resultData= null;
	
	/**
	 * Flag for displaying intermediate results.
	 */
	@Parameter(label="Show intermediate results", supplemental= true,
			direction= Direction.IN, dataIOOrder= -3,
			description = "Show intermediate results to user.")
	protected boolean showIntermediateResults= false;

  /**
   * Flag for saving intermediate results.
   */
  @Parameter(label="Save intermediate results", supplemental= true,
  		direction= Direction.IN, dataIOOrder= -2,
  		description = "Save intermediate results to disc.")
  protected boolean saveIntermediateResults= false;

  /**
   * Path where to save intermediate results.
   */
  @Parameter(label="Save intermediate results path", supplemental= true,
  		direction= Direction.IN, dataIOOrder= -1,
  		description = "Path for saving (intermediate) results.")
  protected String saveIntermediateResultsPath = "/tmp";

	/*
	 * Supplemental results. 
	 */
	@Parameter(label="Result snakes", direction= Direction.OUT,
			description = "Final snakes.", required = false)
	protected transient MTBPolygon2DSet resultSnakes= null;

	/**
	 * Minimum number of points acceptable for snakes.
	 */
	private static final int MinSnakePointNum= 5;

	/**
	 * Number of snakes.
	 */
	private transient int snakeNum = 0;
	
	/**
	 * Array with active snakes.
	 */
	protected transient boolean [] activeSnakes = null;
	
	/**
	 * Array with colors for visualization.
	 */
	protected transient int [] colorArray= null;

	/**
	 * Current set of snakes.
	 */
	protected transient MTBPolygon2DSet currentSnakes= null;

  /*
   * Local variables.
   */
  private transient MTBImageByte labelImg;
  private transient int width;
	private transient int height;
	private transient int snakeArea[];

	/**
	 * Default constructor.
	 * @throws ALDOperatorException 
	 */
	public CytoplasmAnalyzer2D() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor.
	 * 
	 * @param inImg	Image to work on. 
	 * @param initS	Set of initial contours.
	 * @param	snakeObj Snake parameter configuration object.
	 * @param	m	Detection mode.
	 * @param maxLevels Maximum number of levels.
	 * @throws ALDOperatorException 
	 */
	public CytoplasmAnalyzer2D(MTBImage img, MTBPolygon2DSet initS, 
		SnakeOptimizerCoupled snakeObj, DetectMode m, int _maxLevels) 
	throws ALDOperatorException {
		this.inImg = img;
		this.currentSnakes = initS;
		this.snakeOpter = snakeObj;
		this.detectionMode = m;
		this.maxLevels = _maxLevels;
	}

  @Override
  public boolean supportsStepWiseExecution() {
  	return false;
  }

  /**
	 * Specify input image.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}

	/**
	 * Specify initial snakes.
	 */
	public void setInitialSnakes(MTBPolygon2DSet snakes) {
		this.initialSnakes = snakes;
	}

	/**
   * Returns maximum number of levels in iterative mode.
   */
  public int getMaxLevels() {
  	return this.maxLevels;
  }

	/**
	 * Specify maximum number of levels for iterative mode.
	 */
	public void setMaxLevels(int maxL) {
  	this.maxLevels = maxL;
  }

	/**
	 * Specify detection mode.
	 */
	public void setDetectionMode(DetectMode m) {
  	this.detectionMode = m;
  }

	/**
	 * Enable display of intermediate results.
	 */
	public void enableShowIntermediateResults() {
		this.showIntermediateResults = true;
	}
	
	/**
	 * Disable display of intermediate results.
	 */
	public void disableShowIntermediateResults() {
		this.showIntermediateResults = false;
	}

	/**
	 * Save intermediate results.
	 */
	public void enableSaveIntermediateResults() {
		this.saveIntermediateResults = true;
	}

	/**
	 * Do not save intermediate results.
	 */
	public void disableSaveIntermediateResults() {
		this.saveIntermediateResults = false;
	}

	/**
	 * Set intermediate result directory.
	 */
	public void setIntermediateResultDirectory(String dir) {
		this.saveIntermediateResultsPath = dir;
	}

	/**
	 * Returns result data object.
	 */
	public SegResult_Cytoplasm getResultData() {
  	return this.resultData;
  }

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
  protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {

		// reset some variables in case of running operator multiple times
		this.resultData = null;
		this.resultImage = null;
		
		// copy set of snakes, because set is modified during execution
		this.currentSnakes = this.initialSnakes.clone();
		
  	// activate recursive event handling
  	this.notifyListenersRecursively = true;

		// init internal variables
		this.width = this.inImg.getSizeX();
		this.height = this.inImg.getSizeY();
		this.labelImg =  
			(MTBImageByte)MTBImage.createMTBImage(this.width, this.height,
	  			1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);

		// snake count
		this.snakeNum= this.currentSnakes.size();

		// get initial snake contours, preprocess
		for (int i=0;i<this.currentSnakes.size(); ++i) {
			MTBSnake s= (MTBSnake)this.currentSnakes.elementAt(i);
			if (s.getPointNum()>MinSnakePointNum) 
				s.resample(5);
		}
	  	
		// snake activity array: initially all snakes active
		this.activeSnakes= new boolean[this.snakeNum];
		for (int n = 0; n < this.snakeNum; n++) {
			this.activeSnakes[n] = true;
		}

		// generate random colors for snake visualization
		this.colorArray= new int[this.snakeNum];
		Random randGen= new Random();
		for (int n=0; n<this.snakeNum;++n) {
			this.colorArray[n]= randGen.nextInt(); 
		}

		// run selected detection
		switch(this.detectionMode)
		{
		case iterativeVoronoiExpansion:
			this.runIterativeMode();
			break;
		case meanVarEnergies:
			this.runMeanVarMode();
			break;
		}
	}
	
	/**
	 * Cell tissue segmentation in an iterative fashion.
	 * <p>
	 * Initial contours are first expanded by dilation and then iteratively 
	 * optimized using snakes. After each iteration region growth is assessed,
	 * and if it gets too small, segmentation terminates. If not, contours are 
	 * again expanded and segmentation continues. 
	 * <p>
	 * For details see:
	 * <ul>
	 * <li> B. Möller, N. Stöhr, S. Hüttelmaier and S. Posch,<br>
	 *      "Cascaded Segmentation of Grained Cell Tissue with 
	 *       Active Contour Models", in Proc. of ICPR, 2010, pp. 1481-1484. 
	 * <li> B. Möller, O. Greß, N. Stöhr, S. Hüttelmaier and S. Posch,<br>
	 *      "Adaptive Segmentation of Cells and Particles in 
	 *       Fluorescent Microscope Images", Proc. of VISAPP, pp. 2:97-106, 2010. 
	 * </ul>
	 * 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private void runIterativeMode() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// main loop, iterate until all snakes stop moving
		//=================================================
		int itCounter= 0;
		this.setControlStatus(OperatorControlStatus.OP_RUN);
		boolean notAllSnakesTerminated= true;
		do {

			// control flow
    	if (this.getControlStatus() == OperatorControlStatus.OP_PAUSE) {
    		System.err.println("CytoplasmAnalyzer2D paused...");
    		do {
					try {
	          Thread.sleep(500);
          } catch (InterruptedException e) {
          	// just ignore the exception
          }
    		} while (this.getControlStatus() != OperatorControlStatus.OP_RESUME);
    		System.err.println("CytoplasmAnalyzer2D running again...");
    	}
    	else if (this.getControlStatus() == OperatorControlStatus.OP_STOP) {
    		// leave the for-loop
    		break;
    	}
			
			if (this.verbose.booleanValue()) {
				System.out.println("Beginning of loop, next iteration...");
			}
			
			itCounter++;
			if (this.verbose.booleanValue()) {
				System.out.println("Iteration: " + itCounter);
				System.out.println("Active Snakes:");
				for (int n = 0; n < this.activeSnakes.length; n++) {
					System.out.println("\t" + n + ":\t" + this.activeSnakes[n]);
				}			
			}
			
			// remember the current set of snakes for later comparison
			MTBSnake [] snakeMemory = new MTBSnake[this.snakeNum];
			for (int n = 0; n < this.snakeNum; n++) {
				snakeMemory[n]= (MTBSnake)this.currentSnakes.elementAt(n).clone();
				if (this.verbose.booleanValue()) 
					System.out.println("Snake " + n + ": " + 
		             this.currentSnakes.elementAt(n).getPointNum() + " points.");
      }
			
			// init some helper variables
			int[][] labelMask= new int[this.height][this.width];
			MTBImageByte snakeComps= 
				(MTBImageByte)MTBImage.createMTBImage(this.width, this.height,
						1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			snakeComps.fillBlack();
			boolean[][] excludeMask= new boolean[this.height][this.width];
			for (int y=0;y<this.height;++y)
				for (int x=0;x<this.width;++x)
					excludeMask[y][x] = false;

			// update exclude mask
			for (int n=0;n<this.snakeNum;++n) {

				// ignore snakes with too few points
				if (this.currentSnakes.elementAt(n).getPointNum() == 0)
					continue;

				// initial snake mask (attention, masks may partially overlap!)
				int [][] tmpmask= 
					this.currentSnakes.elementAt(n).getBinaryMask(this.width, this.height);
				// check if there are multiple components in the mask
				// - yes, believe me - this can really happen;
				// if so, delete all regions except the largest one			
				MTBImageByte tmpMaskImage= 
					(MTBImageByte)MTBImage.createMTBImage(this.width, 
							this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
				tmpMaskImage.fillBlack();
				for (int y=0;y<this.height;++y) 
					for (int x=0;x<this.width; ++x) 
						if (tmpmask[y][x]>0)
							tmpMaskImage.putValueInt(x, y, 255);
				LabelComponentsSequential regLabler= 
					new LabelComponentsSequential(tmpMaskImage, true);
				regLabler.runOp(true); //B
				MTBRegion2DSet regions= regLabler.getResultingRegions();
				// check if there is only one component
				// => if not, delete all except largest one
				if (regions.size() > 1) {
					System.err.println("Warning: Snake " + n + 
																		": more than one region!");
					System.err.println("\t -> deleting all except largest one!");
					int maxArea= 0;
					int maxID= -1;
					for (int r=0; r<regions.size(); ++r) {
						if (regions.get(r).getArea()>maxArea) {
							maxArea= regions.get(r).getArea();
							maxID= r;
						}
					}
					for (int r=0; r<regions.size(); ++r) {
						if (r==maxID)
							continue;
						for (Point2D.Double pp: regions.get(r).getPoints())
							tmpmask[(int)pp.y][(int)pp.x]= 0;
					}
				}
				// fill exclude and label masks
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						// interior snake pixel
						if (tmpmask[y][x]>0) {
							// increase interior pointer
							snakeComps.putValueInt(x, y, snakeComps.getValueInt(x, y)+1);
							// exclude pixel
							excludeMask[y][x]= true;
							if (labelMask[y][x]!=0)
								// mark region as covered by different snakes
								labelMask[y][x]= 255;
							else
								// snake label
								labelMask[y][x]= n+1;
						}
					}
				}
			} // end of for-loop: all snakes

			/*
			 * Now check pixels in overlapping areas. Between every two
			 * snakes there should be a clear separation. To this end we
			 * dilate all overlapping areas with a 5x5 mask. 
			 */
			int maskSizeHalf= 2;
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					if (labelMask[y][x]==255) {
						// overlap region
						snakeComps.putValueInt(x, y, 0);
						// dilation around pixel
						for (int dy=-maskSizeHalf;dy<=maskSizeHalf;++dy) {
							if (y+dy<0 || y+dy >= this.height)
								continue;
							for (int dx=-maskSizeHalf;dx<=maskSizeHalf;++dx) {
								if (x+dx<0 || x+dx >= this.width)
									continue;
								snakeComps.putValueInt(x+dx, y+dy, 0);					
							}
						}
					}	
					else if (labelMask[y][x]>0) {
						// not in overlap region, but labeled -> snake interior
						int labelArray []= new int[256];
						// check if there are multiple labels in the neighborhood
						for (int dy=-maskSizeHalf;dy<=maskSizeHalf;++dy) {
							if (y+dy<0 || y+dy >= this.height)
								continue;
							for (int dx=-maskSizeHalf;dx<=maskSizeHalf;++dx) {
								if (x+dx<0 || x+dx >= this.width)
									continue;
								labelArray[labelMask[y+dy][x+dx]]++;					
							}
						}
						int labelCount=0;
						for (int i=1;i<255;++i)
							if (labelArray[i]>0)
								labelCount++;
						// if there is only one label, we are not at a region border,
						// so the pixel is valid; otherwise the pixel is excluded 
						if (labelCount==1)
							snakeComps.putValueInt(x, y, 255);
						else
							snakeComps.putValueInt(x, y, 0);
					}
				} // end of for-loop: all image pixels x
			} // end of for-loop: all image pixels y

			/*
			 *  dilate the snakes by voronoi dilation
			 */
			if (this.verbose.booleanValue()) 
				System.out.print("Doing voronoi dilation...");
			ComponentPostprocess cp= new ComponentPostprocess(snakeComps,
					ComponentPostprocess.ProcessMode.VORONOI_EXPAND);
			cp.setMaximalVoronoiExpansionDistance(this.maxExpansion);
			cp.runOp(true);//B
			MTBImage voronoiComps= cp.getResultImage();
			if (this.verbose.booleanValue()) 
				System.out.println("done.");

			// make sure that labels remain the same, if necessary do relabeleling
			int maxLabel= 0;
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					int label= voronoiComps.getValueInt(x,y);
					if (label>maxLabel)
						maxLabel= label;
				}				
			}					
			if (maxLabel != this.snakeNum) 
				System.err.println("Warning: Something wrong in region extraction, " 
						+	"found more/less regions than snakes...!?");

			// TODO Birgit: Hier wird zuviel Speicher alloziert!
			Vector<int [][]> tmpMasks= new Vector<int [][]>();
			for (int n=0;n<maxLabel;++n) {
				int tmpm[][]= new int[this.height][this.width];
				tmpMasks.add(tmpm);
			}
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					int label= voronoiComps.getValueInt(x,y);
					if (label>0)
						tmpMasks.get(label-1)[y][x]= 255;
				}				
			}					

			int [] vlabelTab = new int[this.snakeNum];
			for (int n=0;n<this.snakeNum;++n) {

				if (this.currentSnakes.elementAt(n).getPointNum() < MinSnakePointNum)
					continue;

				// search in label mask for original component label
				// (given by label of center-of-mass)
				boolean startFound= false;
				int x_ref= 0, y_ref= 0, count= 0;
				for (int y=0;!startFound && y<this.height;++y) {
					for (int x=0;!startFound && x<this.width; ++x) {
						if (labelMask[y][x]==n+1) {
							x_ref+= x;
							y_ref+= y;
							count++;
						}
					}
				}
				x_ref= (int)((double)x_ref/count);
				y_ref= (int)((double)y_ref/count);
				
        // get label of region after voronoi dilation
				int vlabel= voronoiComps.getValueInt(x_ref, y_ref);
				vlabelTab[n]= vlabel;

        // relabel the voronoi image with old label
        if (vlabel>tmpMasks.size()) {
        	System.err.println("Error!!! " +
        			"Label in voronoi image out of range..." + vlabel);
        	try {
        		System.in.read();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
 			}

			// remember dilated binary masks for later reference
			Vector<int [][]> binaryMaskMemory = new Vector<int[][]>();
			for (int n=0;n<this.snakeNum;++n) {
				
				if (this.currentSnakes.elementAt(n).getPointNum() < MinSnakePointNum)
					continue;

				int newLabel= vlabelTab[n];

				// generate binary image of new component
				int [][] tmpmask= new int[this.height][this.width];
				MTBImageByte maskImg= 
					(MTBImageByte)MTBImage.createMTBImage(this.width, 
							this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
				maskImg.fillBlack();
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						if (voronoiComps.getValueInt(x, y)==newLabel) { 
							// interior of a snake
							maskImg.putValueInt(x, y, 255);
							tmpmask[y][x]= 255;
						}
						else {
							maskImg.putValueInt(x, y, 0);
							tmpmask[y][x]= 0;
						}
					}
				}
				binaryMaskMemory.add(tmpmask);
				
				// extract the region....			
				LabelComponentsSequential regLabler= 
					new LabelComponentsSequential(maskImg, true);
				regLabler.runOp(true);//B
				MTBRegion2DSet region= regLabler.getResultingRegions();
				
				if (this.verbose.booleanValue())
					System.out.println("Region count: " + region.size());
				
				// ... and extract the contour
				// for each component, extract contour which will become our new snake
				ContourOnLabeledComponents contourExtractor= 
					new ContourOnLabeledComponents(maskImg, region,
						ContourOnLabeledComponents.ContourType.OUTER_CONTOUR, 100);
				contourExtractor.runOp(true);//B
				MTBContour2DSet contour= contourExtractor.getResultContours();

				if (this.verbose.booleanValue())
					System.out.println("Contour count: " + contour.size());

				// check if something went wrong...
				if (contour.size()!=0) {			
					// if not, convert to snake
					MTBSnake s= MTBSnake.convertContourToSnake(contour.elementAt(0));
					s.resample(15);
					s.drawPolygon(maskImg);
					this.currentSnakes.setElementAt(n, s);
					if (this.verbose.booleanValue())
						System.out.println("Snake " + n + ": " 
																				+ s.getPointNum() + " points.");
				} 
				else {
					// error handling
					ImageWriterMTB writer = 
						new ImageWriterMTB(voronoiComps, "/tmp/voronoi.tif");
					writer.setOverwrite(true);
					writer.runOp(true);
					System.err.println("Did not find snake " + n 
																			+ " (Label= " + (n+1) + ")!!!");
					System.err.println("Voronoi image written to /tmp/voronoi.tif");
					this.currentSnakes.setElementAt(n, new MTBSnake());
					try {
						System.in.read();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} // end of for-loop: all snakes
			
			// initialize snake optimizer
			this.snakeOpter.setInputImage(this.inImg);
			this.snakeOpter.setInitialSnakes(this.currentSnakes);

			// enable display/saving of intermediate results
			if (this.showIntermediateResults)
				this.snakeOpter.enableShowIntermediateResults();
			else
				this.snakeOpter.disableShowIntermediateResults();
			if (this.saveIntermediateResults)
				this.snakeOpter.enableSaveIntermediateResults();
			else
				this.snakeOpter.disableSaveIntermediateResults();
			
			// initialize parameters for multi snake optimizer
			if (this.snakeNum > 1) 
				this.snakeOpter.setActivityArray(this.activeSnakes);
			// print parameters to standard out
			if (this.verbose.booleanValue())
				this.snakeOpter.print();
			
			if (this.saveIntermediateResults) {
				// plot the snakes
				MTBImageRGB dispImg = 
					(MTBImageRGB)MTBImage.createMTBImage(this.width,this.height,1, 1, 1, 
																								MTBImage.MTBImageType.MTB_RGB);	
				// scale input image to the range [0,255]
				int inimgMax= 0;
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						if (this.inImg.getValueInt(x, y)>inimgMax)
							inimgMax= this.inImg.getValueInt(x,y);
					}
				}
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						int v= 
							(int)((double)this.inImg.getValueInt(x,y)/(double)inimgMax*255.0);
						int color = ((v & 0xff) << 16) + ((v & 0xff) << 8) + (v & 0xff);
						dispImg.putValueInt(x, y, color);
					}
				}
				// mark interior in blue
				int blue = ((0 & 0xff) << 16) + ((0 & 0xff) << 8) + (255 & 0xff);
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						if (excludeMask[y][x])
							dispImg.putValueInt(x, y, blue);
					}
				}
				// draw the snake in red
				for (int n=0;n<this.snakeNum;++n) {
					if (this.currentSnakes.elementAt(n).getPointNum()==0)
						continue;
					int color = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
					this.currentSnakes.elementAt(n).drawPolygon(dispImg,color);
				}
				// save to disc
				String outputfile= this.saveIntermediateResultsPath + 
																				"/initialSnakes_" + itCounter + ".tif";
				ImageWriterMTB writer = new ImageWriterMTB(dispImg, outputfile);
				writer.setOverwrite(true);
				writer.runOp(false);
			}

			// run the optimizer
			if (this.snakeNum > 1) {
				this.snakeOpter.setExcludeMask(excludeMask);
			}

			// register optimizer as listener
			this.addALDControlEventListener(this.snakeOpter);
			
			if (this.verbose.booleanValue())
				System.out.println("Running the optimizer...");
			this.snakeOpter.runOp(false);
			if (this.verbose.booleanValue())
				System.out.println("Optimization finished!");
		
			// unregister listener
			this.removeALDControlEventListener(this.snakeOpter);
			
			// fetch current snake(s)
			this.resultSnakes = this.snakeOpter.getResultSnakes();
			
			/*
			 * Termination checks.
			 */
			
			// check if maximum number of iterations is reached,
			// if so, stop at once
			boolean [] minorAreaChange= new boolean[this.snakeNum];
			if (itCounter == this.maxLevels)
				notAllSnakesTerminated= false;
			else {
				// check if termination criterion is fulfilled
				for (int n=0; n<this.snakeNum; ++n) {

					minorAreaChange[n]= false;

					if (this.activeSnakes[n] == false)
						continue;

					// not enough points left, snake is ready!
					if (this.resultSnakes.elementAt(n).getPointNum() < MinSnakePointNum) {
						this.activeSnakes[n] = false;	
						continue;
					}

					/*
					 * first check: region growth
					 */
					double newSum= 0;
					int newCount= 0;
					int [][] maskBefore= binaryMaskMemory.get(n);
					int [][] maskAfter= 
						this.resultSnakes.elementAt(n).getBinaryMask(this.width,this.height);
					int areaBefore=0, areaAfter= 0;
					for (int y=0;y<this.height;++y) {
						for (int x=0;x<this.width;++x) {
							if (maskBefore[y][x]>0) areaBefore++;
							if (maskAfter[y][x]>0)  areaAfter++;
							if (maskBefore[y][x]==0 && maskAfter[y][x]>0 && !excludeMask[y][x]){
								newSum+=this.inImg.getValueInt(x, y);
								newCount++;
							}

						}
					}
					if (this.verbose.booleanValue()) {
						System.out.println("Area growth: " + 
								Math.abs(1-(double)areaBefore/(double)areaAfter));
					}
					if (  Math.abs(1-(double)areaBefore/(double)areaAfter) 
							< this.minAreaGrowth) {
						if (this.verbose.booleanValue()) {
							System.out.println("Stopping snake " +n+ ", minor area change.");
						}
						this.activeSnakes[n] = false;	
						minorAreaChange[n]= true;
					}

					/* 
					 * second check: standard deviation of new parts
					 */
					double newAverage= newSum/newCount;
					double newVar= 0; newCount= 0;
					for (int y=0;y<this.height;++y) {
						for (int x=0;x<this.width;++x) {
							if (   maskBefore[y][x]==0 && maskAfter[y][x]>0 
									&& !excludeMask[y][x]) {
								newVar+=(this.inImg.getValueInt(x, y) - newAverage) *
							        	(this.inImg.getValueInt(x, y) - newAverage);
								newCount++;
							}
						}
					}
					if (newCount > 0) {
						if (this.verbose.booleanValue()) {
							System.out.println("Std-Dev. of new area= " + 
									Math.sqrt(newVar/newCount));
						}
						if (Math.sqrt(newVar/newCount) < this.minIntensityVariance) {
							if (this.verbose.booleanValue()) {
								System.out.println("\t -> Stopping snake!");
							}
							this.activeSnakes[n] = false;
						}					
					}
				}			
				// check if there is at least one active snake remaining...
				int activeSnakeCount= 0;
				for (int n=0;n<this.snakeNum;++n)
					if (this.activeSnakes[n]==true)
						activeSnakeCount++;
				if (activeSnakeCount==0)
					notAllSnakesTerminated= false;
			}
			
			if (this.verbose.booleanValue()) {
				System.out.println("End of Iteration: " + itCounter);
				System.out.println("Active Snakes:");
				for (int n = 0; n < this.activeSnakes.length; n++) {
					System.out.println("\t" + n + ":\t" + this.activeSnakes[n]);
				}
			}
			
			// do some statistics, output label image
			this.labelImg.fillBlack();

			// get all snake masks and visualize
			this.snakeArea = new int[this.snakeNum];
			for (int n=0; n<this.snakeNum; ++n) {

				if (this.resultSnakes.elementAt(n).getPointNum()<MinSnakePointNum) {
					continue;
				}

				int[][] mask= 
					this.resultSnakes.elementAt(n).getBinaryMask(this.width,this.height);
				int snakePix = 0;
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						if (mask[y][x] >0) {
							this.labelImg.putValueInt(x, y, n+1);
							++snakePix;
						}
					}
				}
				// remember mask for potential later use
				this.snakeArea[n] = snakePix;
			}

			// save set of snakes after this iteration
			String outputDir= this.saveIntermediateResultsPath;
			if (this.saveIntermediateResults) {
	      try {
	        this.resultSnakes.write(outputDir + "/snake_"+itCounter+".xml");
        } catch (ALDException e) {
	        e.printStackTrace();
        }
			}
			
			// save snake segmentation result
			if (this.saveIntermediateResults) {
				String file= this.saveIntermediateResultsPath + 
							                             "/segResult_" + itCounter + ".tif";
				ImageWriterMTB writer = 
					new ImageWriterMTB(this.snakeOpter.getResultSnakeImage(), file);
				writer.setOverwrite(true);
				writer.runOp(false);

				// save label image
				file= outputDir + "/labelImage_" + itCounter + ".tif";
				writer = new ImageWriterMTB(this.labelImg, file);
				writer.setOverwrite(true);
				writer.runOp(false);
			}
			
			// save result image
			this.resultImage = this.snakeOpter.getResultSnakeImage();
						
			/*
			 * preparation of next run
			 */

			// copy snakes to initial array for next run
			this.currentSnakes= new MTBPolygon2DSet(0,0,0,0);
			for (int n=0; n<this.snakeNum; ++n) {
				if (this.activeSnakes[n]==true)
					this.currentSnakes.add(this.resultSnakes.elementAt(n));
				else {
					// stay with given undilated snake, former snake is overwritten here!
					if (!minorAreaChange[n]) {
						this.currentSnakes.add(snakeMemory[n].clone());
					}
					else {
						this.currentSnakes.add(this.resultSnakes.elementAt(n));						
					}
				}
			}
			System.out.println("End of iteration " + itCounter + ".");
		} while (notAllSnakesTerminated);
		System.out.println("Finished iterative cell boundary segmentation!");
		// provide final results
		int totalSize= 0;
		for (int n=0;n<this.snakeNum;++n) {
			totalSize += this.snakeArea[n];
		}
		this.resultData = new SegResult_Cytoplasm(this.inImg.getTitle(), 
				this.resultSnakes, this.labelImg, this.snakeNum, this.snakeArea,
				(double)totalSize/(double)this.snakeNum);
		// if available, provide image with cell contours
		if (this.resultImage != null) {
			this.resultData.setResultCellImg(this.resultImage);
		}
	}
	
	/**
	 * Cell tissue segmentation in an iterative fashion.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	@Deprecated
	private void runMeanVarMode() 
	throws ALDOperatorException, ALDProcessingDAGException {
		
		// init some helper variables
		int[][] labelMask= new int[this.height][this.width];
		MTBImageByte snakeComps= 
			(MTBImageByte)MTBImage.createMTBImage(this.width, this.height,
					1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		snakeComps.fillBlack();		
		boolean[][] excludeMask= new boolean[this.height][this.width];
		for (int y=0;y<this.height;++y)
			for (int x=0;x<this.width;++x)
				excludeMask[y][x] = false;

		// update exclude mask
		for (int n=0;n<this.snakeNum;++n) {

			// ignore snakes with too few points
			if (this.currentSnakes.elementAt(n).getPointNum() == 0)
				continue;

			// initial snake mask (attention, masks partially overlap!)
			int [][] tmpmask= 
				this.currentSnakes.elementAt(n).getBinaryMask(this.width, this.height);
			// check if there are multiple components in the mask
			// - yes, believe me - this can really happen;
			// if so, delete all regions except the largest one			
			MTBImageByte tmpMaskImage= 
				(MTBImageByte)MTBImage.createMTBImage(this.width, 
						this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			tmpMaskImage.fillBlack();
			for (int y=0;y<this.height;++y) 
				for (int x=0;x<this.width; ++x) 
					if (tmpmask[y][x]>0)
						tmpMaskImage.putValueInt(x, y, 255);
			LabelComponentsSequential regLabler= 
				new LabelComponentsSequential(tmpMaskImage, true);
			regLabler.runOp(null);
			MTBRegion2DSet regions= regLabler.getResultingRegions();
			// check if there is only one component
			// => if not, delete all except largest one
			if (regions.size() > 1) {
				System.err.println("Warning: Snake " + n + 
																	": more than one region!");
				System.err.println("\t -> deleting all except largest one!");
				int maxArea= 0;
				int maxID= -1;
				for (int r=0; r<regions.size(); ++r) {
					if (regions.get(r).getArea()>maxArea) {
						maxArea= regions.get(r).getArea();
						maxID= r;
					}
				}
				for (int r=0; r<regions.size(); ++r) {
					if (r==maxID)
						continue;
					for (Point2D.Double pp: regions.get(r).getPoints())
						tmpmask[(int)pp.y][(int)pp.x]= 0;
				}
			}
			// fill exclude and label masks
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					// interior snake pixel
					if (tmpmask[y][x]>0) {
						// increase interior pointer
						snakeComps.putValueInt(x, y, snakeComps.getValueInt(x, y)+1);
						// exclude pixel
						excludeMask[y][x]= true;
						if (labelMask[y][x]!=0)
							// mark region as covered by different snakes
							labelMask[y][x]= 255;
						else
							// snake label
							labelMask[y][x]= n+1;
					}
				}
			}
		} // end of for-loop: all snakes

		/*
		 * Now check pixels in overlapping areas. Between every two
		 * snakes there should be a clear separation. To this end we
		 * dilate all overlapping areas with a 5x5 mask. 
		 */
		int maskSizeHalf= 2;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width; ++x) {
				if (labelMask[y][x]==255) {
					// overlap region
					snakeComps.putValueInt(x, y, 0);
					// dilation around pixel
					for (int dy=-maskSizeHalf;dy<=maskSizeHalf;++dy) {
						if (y+dy<0 || y+dy >= this.height)
							continue;
						for (int dx=-maskSizeHalf;dx<=maskSizeHalf;++dx) {
							if (x+dx<0 || x+dx >= this.width)
								continue;
							snakeComps.putValueInt(x+dx, y+dy, 0);					
						}
					}
				}	
				else if (labelMask[y][x]>0) {
					// not in overlap region, but labeled -> snake interior
					int labelArray []= new int[256];
					// check if there are multiple labels in the neighborhood
					for (int dy=-maskSizeHalf;dy<=maskSizeHalf;++dy) {
						if (y+dy<0 || y+dy >= this.height)
							continue;
						for (int dx=-maskSizeHalf;dx<=maskSizeHalf;++dx) {
							if (x+dx<0 || x+dx >= this.width)
								continue;
							labelArray[labelMask[y+dy][x+dx]]++;					
						}
					}
					int labelCount=0;
					for (int i=1;i<255;++i)
						if (labelArray[i]>0)
							labelCount++;
					// if there is only one label, we are not at a region border,
					// so the pixel is valid; otherwise the pixel is excluded 
					if (labelCount==1)
						snakeComps.putValueInt(x, y, 255);
					else
						snakeComps.putValueInt(x, y, 0);
				}
			} // end of for-loop: all image pixels x
		} // end of for-loop: all image pixels y

		// dilate the snakes by voronoi dilation
		if (this.verbose.booleanValue()) 
			System.out.print("Doing voronoi dilation...");
		//		ComponentPostprocess cp= new ComponentPostprocess(snakeComps,
		//				ComponentPostprocess.ProcessMode.VORONOI_EXPAND, -1, -1, 10);
		ComponentPostprocess cp= new ComponentPostprocess(snakeComps,
				ComponentPostprocess.ProcessMode.VORONOI_EXPAND);
		cp.setMaximalVoronoiExpansionDistance(10);
		cp.runOp(null);
		MTBImage voronoiComps= cp.getResultImage();
		if (this.verbose.booleanValue()) 
			System.out.println("done.");

		// make sure that labels remain the same, if necessary do relabeleling
		int maxLabel= 0;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width; ++x) {
				int label= voronoiComps.getValueInt(x,y);
				if (label>maxLabel)
					maxLabel= label;
			}				
		}					
		if (maxLabel != this.snakeNum) 
			System.err.println("Warning: Something wrong in region extraction, " 
					+	"found more/less regions than snakes...!?");

		// TODO Hier wird zuviel alloziert!
		Vector<int [][]> tmpMasks= new Vector<int [][]>();
		for (int n=0;n<maxLabel;++n) {
			int tmpm[][]= new int[this.height][this.width];
			tmpMasks.add(tmpm);
		}
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width; ++x) {
				int label= voronoiComps.getValueInt(x,y);
				if (label>0)
					tmpMasks.get(label-1)[y][x]= 255;
			}				
		}					

		int [] vlabelTab = new int[this.snakeNum];
		for (int n=0;n<this.snakeNum;++n) {

			if (this.currentSnakes.elementAt(n).getPointNum() < MinSnakePointNum)
				continue;

			// search in label mask for original component label
			// (given by label of center-of-mass)
			boolean startFound= false;
			int x_ref= 0, y_ref= 0, count= 0;
			for (int y=0;!startFound && y<this.height;++y) {
				for (int x=0;!startFound && x<this.width; ++x) {
					if (labelMask[y][x]==n+1) {
						x_ref+= x;
						y_ref+= y;
						count++;
					}
				}
			}
			x_ref= (int)((double)x_ref/count);
			y_ref= (int)((double)y_ref/count);

			// get label of region after voronoi dilation
			int vlabel= voronoiComps.getValueInt(x_ref, y_ref);
			vlabelTab[n]= vlabel;

			// relabel the voronoi image with old label
			if (vlabel>tmpMasks.size()) {
				System.err.println("Error!!! " +
						"Label in voronoi image out of range..." + vlabel);
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// remember dilated binary masks for later reference
		Vector<int [][]> binaryMaskMemory = new Vector<int[][]>();
		for (int n=0;n<this.snakeNum;++n) {
			
			if (this.currentSnakes.elementAt(n).getPointNum() < MinSnakePointNum)
				continue;

			int newLabel= vlabelTab[n];

			// generate binary image of new component
			int [][] tmpmask= new int[this.height][this.width];
			MTBImageByte maskImg= 
				(MTBImageByte)MTBImage.createMTBImage(this.width, 
						this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			maskImg.fillBlack();
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					if (voronoiComps.getValueInt(x, y)==newLabel) { 
						// interior of a snake
						maskImg.putValueInt(x, y, 255);
						tmpmask[y][x]= 255;
					}
					else {
						maskImg.putValueInt(x, y, 0);
						tmpmask[y][x]= 0;
					}
				}
			}
			binaryMaskMemory.add(tmpmask);
			
			// extract the region....			
			LabelComponentsSequential regLabler= 
				new LabelComponentsSequential(maskImg, true);
			regLabler.runOp(null);
			MTBRegion2DSet region= regLabler.getResultingRegions();
			
			if (this.verbose.booleanValue())
				System.out.println("Region count: " + region.size());
			
			// ... and extract the contour
			// for each component, extract contour which will become our new snake
			ContourOnLabeledComponents contourExtractor= 
				new ContourOnLabeledComponents(maskImg, region,
					ContourOnLabeledComponents.ContourType.OUTER_CONTOUR, 100);
			contourExtractor.runOp(null);
			MTBContour2DSet contour= contourExtractor.getResultContours();

			if (this.verbose.booleanValue())
				System.out.println("Contour count: " + contour.size());

			// check if something went wrong...
			if (contour.size()!=0) {			
				// if not, convert to snake
				MTBSnake s= MTBSnake.convertContourToSnake(contour.elementAt(0));
				s.resample(15);
				s.drawPolygon(maskImg);
				this.currentSnakes.setElementAt(n, s);
				if (this.verbose.booleanValue())
					System.out.println("Snake " + n + ": " 
																			+ s.getPointNum() + " points.");
			} 
			else {
				// error handling
				ImageWriterMTB writer = 
					new ImageWriterMTB(voronoiComps, "/tmp/voronoi.tif");
				writer.setOverwrite(true);
				writer.runOp(null);
				System.err.println("Did not find snake " + n 
																		+ " (Label= " + (n+1) + ")!!!");
				System.err.println("Voronoi image written to /tmp/voronoi.tif");
				this.currentSnakes.setElementAt(n, new MTBSnake());
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} // end of for-loop: all snakes
		
		// initialize snake optimizer
//		SnakeOptimizer snakeOpter;
//		if (this.snakeNum > 1) {
//			snakeOpter= new SnakeOptimizerMulti();
//			SnakeOptimizerMulti ptr= (SnakeOptimizerMulti)snakeOpter;
//			ptr.setColorArray(this.colorArray);
//		}
//		else {
//			// single snake optimizer, get parameters from file
//			snakeOpter= new SnakeOptimizerSinglePDE();
//		}
//		// specify global parameters not included in configuration files
		this.snakeOpter.setInputImage(this.inImg);
		this.snakeOpter.setInitialSnakes(this.currentSnakes);

		// get parameters from file
//		System.out.println("Reading parameters from " + 
//				this.snakeParamFile.getString() + ".xml");
//		if (null==
//			snakeOpter.setParametersFromXml(this.snakeParamFile.getString() 
//					+ ".xml",
//					"SnakeOptimizerGUIConfigurator",
//			"de.unihalle.informatik.MiToBo.segmentation.snakes.gui")){
//			// check if file is in SnakeOptimizerGUIConfigurator format
//			SnakeOptimizerGUIConfigurator gConf = 
//				new SnakeOptimizerGUIConfigurator(null);
//			if (null==
//				gConf.setParametersFromXml(this.snakeParamFile.getString() + 
//						"1.xml",
//						"SnakeOptimizerGUIConfigurator",
//				"de.unihalle.informatik.MiToBo.segmentation.snakes.gui")){				
//				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
//				"MembraneDetector2DAlgos: Param-file could not be read...exit");
//			}
//			// convert GUI configuration to optimizer configuration
//			((SnakeOptimizerSingle)snakeOpter).setParametersFromGUIConfig(gConf);
//		}
		// convert GUI configuration to optimizer configuration
//		((SnakeOptimizerSinglePDE)snakeOpter).setParametersFromGUIConfig(
//																												this.snakeOpter);

		// enable display/saving of intermediate results
		if (this.showIntermediateResults)
			this.snakeOpter.enableShowIntermediateResults();
		if (this.saveIntermediateResults)
			this.snakeOpter.enableSaveIntermediateResults();

		// initialize parameters for multi snake optimizer
		if (this.snakeNum > 1) 
			this.snakeOpter.setActivityArray(this.activeSnakes);
		// print parameters to standard out
		if (this.verbose.booleanValue())
			this.snakeOpter.print();

		if (this.saveIntermediateResults) {
			// plot the snakes
			MTBImageRGB dispImg = 
				(MTBImageRGB)MTBImage.createMTBImage(this.width,this.height,1, 1, 1, 
						MTBImage.MTBImageType.MTB_RGB);	
			// scale input image to the range [0,255]
			int inimgMax= 0;
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					if (this.inImg.getValueInt(x, y)>inimgMax)
						inimgMax= this.inImg.getValueInt(x,y);
				}
			}
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					int v= 
						(int)((double)this.inImg.getValueInt(x,y)/(double)inimgMax*255.0);
					int color = ((v & 0xff) << 16) + ((v & 0xff) << 8) + (v & 0xff);
					dispImg.putValueInt(x, y, color);
				}
			}
			// mark interior in blue
			int blue = ((0 & 0xff) << 16) + ((0 & 0xff) << 8) + (255 & 0xff);
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width; ++x) {
					if (excludeMask[y][x])
						dispImg.putValueInt(x, y, blue);
				}
			}
			// draw the snake in red
			for (int n=0;n<this.snakeNum;++n) {
				if (this.currentSnakes.elementAt(n).getPointNum()==0)
					continue;
				int color = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
				this.currentSnakes.elementAt(n).drawPolygon(dispImg,color);
			}
			// save to disc
			String outputfile= this.saveIntermediateResultsPath + 
			"/initialSnakes_" + "1.tif";
			ImageWriterMTB writer = new ImageWriterMTB(dispImg, outputfile);
			writer.setOverwrite(true);
			writer.runOp(null);
		}

		// run the optimizer
		if (this.snakeNum > 1) {
			this.snakeOpter.setExcludeMask(excludeMask);
		}

		if (this.verbose.booleanValue())
			System.out.println("Running the optimizer...");
		this.snakeOpter.runOp(null);
		if (this.verbose.booleanValue())
			System.out.println("Optimization finished!");

		// save snake segmentation result
		if (this.saveIntermediateResults) {
			String file= this.saveIntermediateResultsPath + 
			"/segResult_" + "1.tif";
			ImageWriterMTB writer = 
				new ImageWriterMTB(this.snakeOpter.getResultSnakeImage(), file);
			writer.setOverwrite(true);
			writer.runOp(null);
		}

		/*
		 * Termination checks.
		 */

		// check if termination criterion is fulfilled
		boolean [] minorAreaChange= new boolean[this.snakeNum];
		MTBPolygon2DSet csnakes = this.snakeOpter.getCurrentSnakes();
		for (int n=0; n<this.snakeNum; ++n) {

			minorAreaChange[n]= false;

			if (this.activeSnakes[n] == false)
				continue;

			// not enough points left, snake is ready!
			if (csnakes.elementAt(n).getPointNum() < MinSnakePointNum) {
				this.activeSnakes[n] = false;	
				continue;
			}

			// first check: region growth
//			double newSum= 0;
//			int newCount= 0;
//			int [][] maskBefore= binaryMaskMemory.get(n);
//			int [][] maskAfter= 
//				sarray[n].getBinaryMask(width,height);
//			int areaBefore=0, areaAfter= 0;
//			for (int y=0;y<height;++y) {
//				for (int x=0;x<width;++x) {
//					if (maskBefore[y][x]>0) areaBefore++;
//					if (maskAfter[y][x]>0)  areaAfter++;
//					if (maskBefore[y][x]==0 && maskAfter[y][x]>0 && !excludeMask[y][x]){
//						newSum+=this.inImg.getValueInt(x, y);
//						newCount++;
//					}
//
//				}
//			}
//			if (this.verbose.booleanValue()) {
//				System.out.println("Area growth: " + 
//						Math.abs(1-(double)areaBefore/(double)areaAfter));
//			}
//			if (Math.abs(1-(double)areaBefore/(double)areaAfter) < minAreaGrowth) {
//				if (this.verbose.booleanValue()) {
//					System.out.println("Stopping snake " + n + ", minor area change.");
//				}
//				this.activeSnakes[n] = false;	
//				minorAreaChange[n]= true;
//			}
//
//			// second check: standard deviation of new parts
//			double newAverage= newSum/newCount;
//			double newVar= 0; newCount= 0;
//			for (int y=0;y<height;++y) {
//				for (int x=0;x<width;++x) {
//					if (   maskBefore[y][x]==0 && maskAfter[y][x]>0 
//							&& !excludeMask[y][x]) {
//						newVar+=(this.inImg.getValueInt(x, y) - newAverage) *
//						(this.inImg.getValueInt(x, y) - newAverage);
//						newCount++;
//					}
//				}
//			}
//			if (newCount > 0) {
//				if (this.verbose.booleanValue()) {
//					System.out.println("Std-Dev. of new area= " + 
//							Math.sqrt(newVar/newCount));
//				}
//				if (Math.sqrt(newVar/newCount) < 100) {
//					if (this.verbose.booleanValue()) {
//						System.out.println("\t -> Stopping snake!");
//					}
//					this.activeSnakes[n] = false;
//				}					
//			}
		}			
//		// check if there is at least one active snake remaining...
//		int activeSnakeCount= 0;
//		for (int n=0;n<this.snakeNum;++n)
//			if (this.activeSnakes[n]==true)
//				activeSnakeCount++;
//		if (activeSnakeCount==0)
//			notAllSnakesTerminated= false;
//
//		// check if maximum number of iterations is reached
//		if (itCounter == this.maxLevels)
//			notAllSnakesTerminated= false;
//
//		if (this.verbose.booleanValue()) {
//			System.out.println("End of Iteration: " + itCounter);
//			System.out.println("Active Snakes:");
//			for (int n = 0; n < this.activeSnakes.length; n++) {
//				System.out.println("\t" + n + ":\t" + this.activeSnakes[n]);
//			}
//		}

		// copy snakes to initial array for next run
		this.currentSnakes= new MTBPolygon2DSet(0,0,0,0);
		for (int n=0; n<this.snakeNum; ++n) {
			this.currentSnakes.add(csnakes.elementAt(n));
		}

		// generate/save intermediate output image(s)
		if (this.saveIntermediateResults) {
			String outputDir= this.saveIntermediateResultsPath;
			try {
				this.currentSnakes.write(outputDir + "/snake_"+"1.xml");
			} catch (ALDException e) {
				e.printStackTrace();
			}

			MTBImageRGB outimg= 
				(MTBImageRGB)this.inImg.convertType(MTBImage.MTBImageType.MTB_RGB, 
						true);

			// plot snakes to result image
			for (int n=0; n<this.snakeNum;++n) {
				this.currentSnakes.elementAt(n).drawPolygon(outimg,this.colorArray[n]);
			}			
			// save snake segmentation result
			String file= outputDir + "/segResult_" + "1.tif";
			ImageWriterMTB writer = new ImageWriterMTB(outimg, file);
			writer.setOverwrite(true);
			writer.runOp(null);

			// output label image
			MTBImageByte maskImg= 
				(MTBImageByte)MTBImage.createMTBImage(this.width, this.height,
						1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			maskImg.fillBlack();

			// get all snake masks and visualize 
			for (int n=0; n<this.snakeNum; ++n) {

				if (this.currentSnakes.elementAt(n).getPointNum() < MinSnakePointNum)
					continue;

				int[][] mask= 
					this.currentSnakes.elementAt(n).getBinaryMask(this.width,this.height);
				for (int y=0;y<this.height;++y) {
					for (int x=0;x<this.width; ++x) {
						if (mask[y][x] >0)
							maskImg.putValueInt(x, y, (n+1)*10);
					}
				}
			}
			// save label image
			file= outputDir + "/labelImage_" + "1.tif";
			writer = new ImageWriterMTB(maskImg, file);
			writer.setOverwrite(true);
			writer.runOp(null);
		}
	}
}
