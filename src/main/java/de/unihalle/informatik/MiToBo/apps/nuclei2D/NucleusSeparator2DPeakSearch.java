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

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusDetector2D.*;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination.*;

/**
 * Operator for separating merged nuclei regions in images.
 * <p>
 * This operator analyzes nuclei images and detects nuclei regions. The main
 * focus is thereby on seperating nuclei regions which are merged by common
 * segmentation techniques. Consequently, binary pre-segmentations of nuclei
 * regions can be provided as input to the operator.<br>
 * Anyway, if no binary image is coming along, the gray-scale input image will 
 * first be binarized by applying Otsu thresholding and some morphological
 * post-processing steps. Indeed this a standard procedure which probably 
 * won't suit your needs - better use your own nucleus detector instead. 
 * <p>
 * After binarization the resulting regions are further analyzed, i.e. the 
 * centers of present nuclei regions are determined (for details refer to 
 * Jochen's project thesis).
 * <p>
 * Given estimates for the nuclei regions snakes are initialized and run to 
 * detect the complete nuclei regions. As result extracted nuclei contours 
 * are provided as a set of polygons and overlayed to the greyscale image.
 * <p>
 * This operator has been written as part of Jochen's project in WS 2011/12.
 * 
 * @author Jochen Luechtrath
 * @author Birgit Moeller
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.STANDARD,
	shortDescription="Operator for separating merged nuclei regions in images.")
@ALDDerivedClass
public class NucleusSeparator2DPeakSearch extends NucleusSeparator2DAlgos {
	
	/**
	 * Maximal number of snake iterations in iterative optimization.
	 */
	@Parameter( label= "Max. Snake Iterations", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder=3, 
			mode = ExpertMode.STANDARD, 
			description = "Maximal number of snake iterations.")	
	private int snakeIterations = 80;
	
	/**
	 * Step-size in iterative snake optimization.
	 */
	@Parameter( label= "Snake Step Size", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder=4, 
			mode = ExpertMode.STANDARD, 
			description = "Step size gamma for snake gradient-descent.")	
	private double gamma=9.0;
		
	/**
	 * Snake segment length.
	 */
	@Parameter( label= "Snake Segment Length", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder = 5, 
			mode=ExpertMode.ADVANCED, 
			description = "Desired length of snake segments.")	
	private double segmentlength=9;
	
	/*
	 * Supplemental parameters.
	 */
	
	/**
	 * Flag to enable/disable display of snake results.
	 */
	@Parameter( label= "Show Snakes", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder = -10, 
			mode = ExpertMode.STANDARD, supplemental = true,
			description = "Flag to enable/disable snake display.")	
	private boolean showSnakes= false;
	
	/**
	 * Flag to enable/disable saving of intermediate snake results.
	 */
	@Parameter( label= "Save Snakes", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder = -9, 
			mode = ExpertMode.ADVANCED, supplemental = true,
			description = "Flag to enable/disable saving of intermediate results.")	
	private boolean saveSnakes= false;
	
	/**
	 * Path where to save intermediate results.
	 */
	@Parameter( label= "Save Results to...", required = false, 
			direction = Parameter.Direction.IN, dataIOOrder = -8, 
			mode = ExpertMode.ADVANCED, supplemental = true,
			description = "Path to where intermediate results will be stored.")	
	private ALDDirectoryString saveSnakeLoc = null;
	
	/*
	 * Result parameters.
	 */
	
	/**
	 * Set of resulting contours.
	 */
	@Parameter( label= "Result Nuclei Contours", required = false, 
			direction = Parameter.Direction.OUT, 
			description = "Set of resulting snakes.")	
	private	MTBPolygon2DSet snakes = null;
	
	/**
	 * RGB image overlay with result nuclei contours.
	 */
	@Parameter( label= "Result Snake Image", required = false, 
			direction = Parameter.Direction.OUT,
			description = "RGB image with nuclei contour overlay.")	
	private	transient MTBImageRGB resultRGBimage=null;
	
	/*
	 * Some local variables.
	 */
	
	private transient Double mad = null;
	private transient Float peakdistance = null;
	private transient Integer suppressor = null;
	private transient MTBImageByte BWImg= null;
	private transient NucleusSeparator2DPeakSearch_RegionSeparator nrs = null;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2DPeakSearch() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with input image.
	 * @param grey 	Original greyscale nucleus image.
	 */
	public NucleusSeparator2DPeakSearch(MTBImageShort grey)
			throws ALDOperatorException {
		this.inputImg = grey;
	}
	
	/**
	 * Constructor with gray-scale and binary image.
	 * @param grey 	Original greyscale nucleus image.
	 * @param label 	Label image of pre-segmented nuclei regions.
	 */
	public NucleusSeparator2DPeakSearch(MTBImageShort grey, MTBImage label)
			throws ALDOperatorException{
		this.inputImg = grey;
		this.labelImg = label;
	}
	
	/**
	 * Specify gray-scale input image.
	 * @param inImg Gray-scale input image to be processed.
	 */
	public void setInImg(MTBImage inImg){
		this.inputImg=inImg;
	}

	/**
	 * Specify label input image.
	 * @param bw 	Label input image.
	 */
	public void setLabelImg(MTBImage label){
		this.labelImg = label;
	}
	
	/**
	 * (De)activates displaying of intermediate results during snake-iterations.
	 * @param If true, displaying results is enabled.
	 */
	public void setShowSnakes(boolean show){
		this.showSnakes=show;
	}

	/**
	 * Returns the result snakes.
	 * @return Resulting nuclei contours.
	 */
	public MTBPolygon2DSet getSnakes(){
		return this.snakes;
	}
	
	/**
	 * Returns RGB color image overlayed with nuclei contours.
	 * @return Color image with nuclei contours.
	 */
	public MTBImageRGB getResultImage(){
		return this.resultRGBimage;
	}
	
	/**
	 * Returns gray-scale label image with nuclei regions.
	 * @return Gray-scale image with nuclei regions.
	 */
	public MTBImageShort getLabelImage(){
		return this.resultImg;
	}

	/**
	 * (De)activates saving of iteration step results.
	 * @param If true, saving of results is enabled.
	 */
	public void setSaveSnakes(boolean save){
		this.saveSnakes=save;
	}
	
	/**
	 * Sets the path to where intermediate snake results are to be written.
	 * @param Path in filesystem, folder must exist.
	 */
	public void setSavePath(String path){
		this.saveSnakeLoc=new ALDDirectoryString(path);
	}

	/**
	 * Sets number of iterations for snake optimization.
	 * @param its	Number of iterations.
	 */
	public void setSnakeIterations(int its){
		this.snakeIterations=its;
	}
	
	/**
	 * Configures basic snake parameters.
	 * @param g	Array of snake step-sizes.
	 * @param its Number of iterations.
	 * @param avglength Desired length of snake segments.
	 */
	public void configureSnakes(double g, int its, double avglength){
		this.gamma=g;
		this.snakeIterations=its;
		this.segmentlength=avglength;
	}

	/**
	 * Returns set of regions resulting from Otsu thresholding.
	 * @return Set of regions created by Otsu thresholding. 
	 */
	public MTBRegion2DSet getOtsuRegs() {	
		return this.nrs.getInputRegs();	
	}

	/**
	 * Returns distance image calculated intermediately.
	 * @return Distance-transformation image as done by NucleusRegionSeparator.
	 */
	public MTBImage getNRSDistImg(){
		return this.nrs.getDistImg();
	}

	/**
	 * Creates and returns a set of polygons as starting regions for the snakes.
	 * <p>
	 * The method is based on voronoi tesselation of Otsu-regions.
	 * @return	Set of initial snake polygons.
	 * @throws ALDProcessingDAGException
	 */
	public MTBPolygon2DSet getNRSVoronoidSnakePrimer() 
			throws ALDOperatorException, ALDProcessingDAGException {
		return this.nrs.getVoronoidSnakePrimer();
	}

	/**
	 * Creates and returns a set of polygons as starting regions for 
	 * snake analysis based on hypothesized ellipses from nucleus information
	 */
	public MTBPolygon2DSet getNRSEllipsoidSnakePrimer(){
		return this.nrs.getEllipsoidSnakePrimer();
	}

	/**
	 * Returns set of result regions from region separator.
	 * @return  Splitted regions.
	 */
	public MTBRegion2DSet[] getNRSResultAreas(){
		return this.nrs.getResultAreas();
	}
	
	/**
	 * Returns set of detected region centers from region separator.
	 * @return Set of detected nuclei centers from NucleusRegionSeperator. 
	 */
	public MTBRegion2DSet getNRSResultCenters(){
		return this.nrs.getResultCenters();
	}

	/**
	 * Returns possible peak locations from NucleusRegionSeperator.
	 * @return Set of possible peak locations from NucleusRegionSeperator.
	 */
	public MTBRegion2DSet getNRSCandidates(){
		return this.nrs.getCandidates();
	}
	
	/**
	 * Sets factor to adjust maximal allowed distance from lower to upper peak.
	 * @param max_d2c 	Maximal distance, default 1.5.
	 */
	public void setMax_d2c(float max_d2c) {
		this.peakdistance = new Float(max_d2c);	
	}

	/**
	 * Sets maximal allowed discrepancy of direct connection to actual profile 
	 * between two peaks.
	 * @param mad Maximal discrepancy, default 0.11.
	 */
	public void setMad(double _mad) {		
		this.mad = new Double(_mad);
	}
	
	/**
	 * Sets threshold to suppress peak analysis close to scraggy contours.
	 * @param sup 	Threshold, should be > 4.
	 */
	public void setSuppressor(int sup) {	
		this.suppressor = new Integer(sup);	
	}

	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// reset result data
		this.resultRGBimage = null;
		this.resultImg = null;
		this.snakes = null;
		
		// get image dimensions
		int res_x, res_y;
		res_x=this.inputImg.getSizeX();
		res_y=this.inputImg.getSizeY();
		
		// check if we have a pre-segmented binary image, if not, compute one
		if (this.labelImg == null){
			NucleusDetector2D nd = new NucleusDetector2D(this.inputImg,
					NuclDetectMode.OTSU_ERODEDILATE,null,true,3,55,true);
			nd.runOp();
			this.labelImg = nd.getResultData().getLabelImage();
		}

		// convert binary input image to 8-bit
		this.BWImg = (MTBImageByte)MTBImage.createMTBImage(res_x, res_y, 1, 1, 1,
																												MTBImageType.MTB_BYTE);
		for (int y=0;y<res_y;++y) {
			for (int x=0;x<res_x;++x) {
				if (this.labelImg.getValueInt(x, y) > 0)
					this.BWImg.putValueInt(x, y, 255);
			}
		}
		
		// run the nucleus region separator
		this.nrs=new NucleusSeparator2DPeakSearch_RegionSeparator(this.BWImg);
		if (this.peakdistance != null) 	
			this.nrs.setMax_d2c(this.peakdistance.doubleValue());
		if (this.suppressor != null) 	
			this.nrs.setSuppressor(this.suppressor.intValue());
		if (this.mad != null)		
			this.nrs.setMad(this.mad.doubleValue());
		this.nrs.runOp(false);
		
		// fetch results and init some variables for snake segmentation
		MTBPolygon2DSet poly = this.nrs.getEllipsoidSnakePrimer();
		
		Vector<MTBSnakeEnergyDerivable> es = new Vector<MTBSnakeEnergyDerivable>();
		Vector<Double> ws = new Vector<Double>();
		
		es.add(new MTBSnakeEnergyCD_OverlapPenalty(3, poly.size()));
		ws.add(new Double(2.5 * poly.size()));
		/* Attention: changed energy, but not yet fully verified! */
//		es.add(new MTBSnakeEnergyCD_KassInt(0.1, 0.3, 
//				new MTBSnakeEnergyCD_KassInt_ParamAdaptNone(0, 0)));
//		ws.add(new Double(1.0));
		es.add(new MTBSnakeEnergyCD_KassLength(0.1, 
				new MTBSnakeEnergyCD_KassLength_ParamAdaptNone()));
		ws.add(new Double(1.0));
		es.add(new MTBSnakeEnergyCD_KassCurvature(0.3, 
				new MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone()));
		ws.add(new Double(1.0));
		es.add(new MTBSnakeEnergyCD_CVRegionFit(this.inputImg, 
			new double[]{0.4}, new double[]{0.9}));		
		ws.add(new Double(6.0));

		MTBSet_SnakeEnergyDerivable envec = new MTBSet_SnakeEnergyDerivable(es, ws);
		boolean[] snactivity = new boolean[poly.size()];
		for (int sn=0; sn < snactivity.length; sn++)
			snactivity[sn]=true;

		// run the coupled snake optimizer
		SnakeOptimizerSingleVarCalc singleOpt = 
			new SnakeOptimizerSingleVarCalc(this.inputImg, poly, envec, 
				new MTBGammaNone(), new Double(this.gamma), 
				new MTBTermMaxIterations(this.snakeIterations), new Boolean(true), 
				new Double(this.segmentlength));
		SnakeOptimizerCoupled snakeOpter =	
				new SnakeOptimizerCoupled(this.inputImg, poly, singleOpt, snactivity);
		snakeOpter.setColorArray(new int[] { 222 });
		
		if (this.showSnakes)
			snakeOpter.enableShowIntermediateResults();

		if (this.saveSnakes){
			snakeOpter.setIntermediateResultPath(
																this.saveSnakeLoc.getDirectoryName());
			snakeOpter.enableSaveIntermediateResults();
		}
		
		if (this.verbose.booleanValue())
			System.out.println(
					"[NucleusAreaSeparator] Running snake optimization...");
		snakeOpter.runOp(false);
		if (this.verbose.booleanValue())
			System.out.println(
					"[NucleusAreaSeparator] Snake optimization finished!");
		
		// fetch result data and visualize results
		this.snakes = snakeOpter.getResultSnakes();
		this.resultRGBimage = 
				(MTBImageRGB)this.inputImg.convertType(MTBImageType.MTB_RGB,true);
		Random r=new Random();
		for(int s=0;s<this.snakes.size();s++) {
			int alt_x = 
				(int)((this.snakes.elementAt(s).getPoints().elementAt(0).x)*res_x+0.5),
					alt_y =
				(int)((this.snakes.elementAt(s).getPoints().elementAt(0).y)*res_y+0.5),
					neu_x = alt_x , neu_y = alt_y,
					color = 
				((55+r.nextInt(200))<<16)+((55+r.nextInt(200))<<8)+(55+r.nextInt(200));

			for(int c=1;c<this.snakes.elementAt(s).getPoints().size();c++) {
				neu_x = 
				(int)((this.snakes.elementAt(s).getPoints().elementAt(c).x)*res_x+0.5);
				neu_y = 
				(int)((this.snakes.elementAt(s).getPoints().elementAt(c).y)*res_y+0.5);				
				this.resultRGBimage.drawLine2D(alt_x,alt_y,neu_x,neu_y,color);
				alt_x= neu_x;
				alt_y= neu_y;
			}
			this.resultRGBimage.drawLine2D(alt_x,alt_y,
				(int)((this.snakes.elementAt(s).getPoints().elementAt(0).x)*res_x+0.5),
				(int)((this.snakes.elementAt(s).getPoints().elementAt(0).y)*res_y+0.5),
				color);
		}
		
		// mask image
		this.resultImg = 
				(MTBImageShort)MTBImage.createMTBImage(this.inputImg.getSizeX(), 
						this.inputImg.getSizeY(), 1, 1, 1, MTBImageType.MTB_SHORT);
		this.resultImg.fillBlack();
		int [][] mask = null;
		if (this.snakes.size() > 65535) {
			System.err.println("[NucleusAreaDetector] " + 
					"Attention! Too many nuclei for label image, labels not unique...");
		}
		for (int s=0;s<this.snakes.size();++s) {
			MTBSnake snake = (MTBSnake)this.snakes.elementAt(s);
			mask = 
				snake.getBinaryMask(this.inputImg.getSizeX(),this.inputImg.getSizeY());
			for (int y=0;y<this.inputImg.getSizeY();++y)
				for (int x=0;x<this.inputImg.getSizeX();++x)
					if (mask[y][x] == 1)
						this.resultImg.putValueInt(x, y, s+1);
		}
		// make sure that there is a real gap between adjacent regions
		Vector<Integer> labels = new Vector<Integer>();
		for (int y=0;y<this.inputImg.getSizeY();++y) {
			for (int x=0;x<this.inputImg.getSizeX();++x) {
				labels.clear();
				for (int dy = -1; dy<=1;++dy) {
					for (int dx = -1; dx<=1 ; ++dx) {
						if (x+dx<0 || x+dx>=this.inputImg.getSizeX())
							continue;
						if (y+dy<0 || y+dy>=this.inputImg.getSizeY())
							continue;
						int label = this.resultImg.getValueInt(x+dx, y+dy);
						if (label == 0)
							continue;
						if (!(labels.contains(new Integer(label))))
								labels.add(new Integer(label));
					}
				}
				if (labels.size() > 1) {
					for (int dy = -1; dy<=1;++dy) {
						for (int dx = -1; dx<=1 ; ++dx) {
							this.resultImg.putValueInt(x+dx, y+dy, 0);
						}
					}
				}
			}
		}
	}
	
	@Override
	public String getDocumentation() {
		return "\r\n" + 
				"<ul><li>\r\n" + 
				"<p>performs region separation by searching for local peaks in nuclei regions</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>analyses the paths between peaks and hypothesizes cuts based on heuristic criteria</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>in the end for each hypothesized nucleus region an initial elliptical approximation is extracted and iteratively improved by snake segmentation on the gray-scale image</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>output of the operator is a label images of detected (separated) nuclei regions</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h2>Usage:</h2>\r\n" + 
				"<h3>Required parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>Greyscale Input Image</tt> \r\n" + 
				"<ul><li>\r\n" + 
				"<p>the single-channel gray-scale image to be analyzed</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>if the image contains multiple channels, only the first one is processed</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt> Label Input Image</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>label image encoding result of prior nucleus separation with merged regions</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h3>Optional parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>Max. Snake Iterations</tt> \r\n" + 
				"<ul><li>\r\n" + 
				"<p>maximum number of snake iterations in region segmentation</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>the larger the nuclei, the more iterations are required</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>Snake Step Size</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>step-size in iterative snake optimization</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>the larger the size the faster the snake moves</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>Snake Segment Length</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>length of snake segments</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>the smaller the segments are, the more details can be extracted, however, at the same time the larger computation times are required until convergence</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h3>Supplemental parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>Show Snakes</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>displays intermediate results during snake segmentation</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>Save snakes</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>enables saving of intermediate results during snake segmentation</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>Save Results to...</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>path where to save intermediate results</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>Verbose</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>activates additional console output</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>";
	}
}
