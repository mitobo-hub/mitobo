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

package de.unihalle.informatik.MiToBo.apps.scratchAssay;

import ij.IJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBImageHistogram;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBStructuringElement;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;
import de.unihalle.informatik.MiToBo.filters.nonlinear.StatisticsFilter;
import de.unihalle.informatik.MiToBo.morphology.*;
import de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.tools.image.ImageValueTools;

/**
 * class for segmenting the wound area of a scratch assay image</br>
 * </br>
 * First the image is smoothed.</br>
 * Subsequently, the shannon entropy for every pixel is calculated.</br>
 * Then a 2-phase topology preserving levelset segmentation is performed on the</br>
 * entropy image to separate the cells from the scratch area.</br>
 * Finally, the resulting scratch area is measured.
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING, 
	level=Level.STANDARD,
	shortDescription="Segments the wound area of a scratch assay image.")
public class ScratchAssaySegmenter extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", mode=ExpertMode.STANDARD, dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "initialization mask", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "initialization mask", mode=ExpertMode.ADVANCED, dataIOOrder = 1)
	private transient MTBImage initMask = null;
	
	@Parameter(label = "horizontal scratch", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "horizontally or vertically oriented scratch", mode=ExpertMode.STANDARD, dataIOOrder = 2)
	private Boolean isHorizontal = true;
	
	@Parameter(label = "\u03C3", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of gauss filter", mode=ExpertMode.STANDARD, dataIOOrder = 3)
	private Integer sigma = 5;
	
	@Parameter(label = "entropy filter size", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "size of entropy filter mask", mode=ExpertMode.STANDARD, dataIOOrder = 4)
	private Integer entropyFilterSize = 33;

	@Parameter(label = "maximum iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations of level set segmentation", mode=ExpertMode.ADVANCED, dataIOOrder = 5)
	private Integer maxIter = 2000;

	@Parameter(label = "don't check for scratch presence", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "don't check for scratch presence prior to segmentation", mode=ExpertMode.STANDARD, dataIOOrder = 6)
	private Boolean noCheck = false;

	@Parameter(label = "use external svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should an external svm file be used for classification", mode=ExpertMode.ADVANCED, dataIOOrder = 7)
	private Boolean useExternalSVM = false;
	
	@Parameter(label = "external svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "absolute path to external svm model file", mode=ExpertMode.ADVANCED, dataIOOrder = 8)
	private ALDFileString svmFile = new ALDFileString(IJ.getDirectory("current"));
	
	@Parameter(label = "scratch area", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "detected scratch area")
	private Double scratchArea = null;

	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting segmented image")
	private transient MTBImage resultImg = null;
	
	@Parameter(label = "number of iterations", required = false, direction = Parameter.Direction.OUT, supplemental = true, description = "number of iterations required for the segmentation")
	private Integer numIterations = null;
	
	@Parameter(label = "runtime", required = false, direction = Parameter.Direction.OUT, supplemental = true, description = "time required to perform the segmentation")
	private Long runtime = null;
	
	@Parameter(label = "entropy image", required = false, direction = Parameter.Direction.OUT, supplemental = true, description = "entropy image")
	private transient MTBImage entropyImg = null;
	
	private int sizeX;	// width of input image
	private int sizeY;	// heigth of input image
	
	private double[] scratchFeatures = new double[3];
	
	private String modelFile = "/share/data/scratch_svm.txt";	// path to internal svm configuration file
	
	private Integer closingMaskSize = 5;	// size of closing mask
	
	
	/**
	 * 
	 * @throws ALDOperatorException
	 */
	public ScratchAssaySegmenter() throws ALDOperatorException
	{
		
	}
	
	
	/**
	 * 
	 * @param inImg				input image
	 * @param sigma				variance of gauss filter
	 * @param entropyFilterSize	size of mask for entropy filtering
	 * @param isHorizontal		is scratch horizontally oriented (assumed to be vertically oriented else)
	 * @param noCheck			don't check for scratch presence prior to segmentation
	 * @param maxIter			maximum number of iterations
	 * @throws ALDOperatorException
	 */
	public ScratchAssaySegmenter(MTBImage inImg, int sigma, int entropyFilterSize, boolean isHorizontal, boolean noCheck, int maxIter) throws ALDOperatorException
	{
		this.inImg = inImg;
		this.sigma = sigma;
		this.entropyFilterSize = entropyFilterSize;
		this.isHorizontal = isHorizontal;
		this.noCheck = noCheck;
		this.maxIter = maxIter;
	}
	

	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		long start = System.currentTimeMillis();
		
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		
		// convert image to 8-bit, if neccessary 
		if(inImg.getType() != MTBImage.MTBImageType.MTB_BYTE)
		{
			inImg = inImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true);
		}
		
		// smooth input image using a Gauss filter
		if(verbose)
		{
			System.out.println("smoothing input image, sigma: " + sigma);
		}
		
		GaussFilter gf = new GaussFilter(inImg, sigma, sigma);
		gf.setSigmaInterpretation(SigmaInterpretation.PIXEL);
		gf.runOp();
		MTBImage filterImg = gf.getResultImg();
		
		// calculate entropy in a defined neighborhood of every pixel
		if(verbose)
		{
			System.out.println("entropy filtering of input image, filter size: " + entropyFilterSize);
		}
		StatisticsFilter sf = new StatisticsFilter(filterImg, StatisticsFilter.FilterMethod.ENTROPY, entropyFilterSize);
		sf.runOp();
		filterImg = sf.getResultImage();
	
		filterImg.setTitle(inImg.getTitle() + "_entropy");
		
		// as filterImg is an output parameter it is shown anyway
//		if(verbose)
//		{
//			filterImg.show();
//		}
		
		this.entropyImg = filterImg;
		
		//resultImg = (MTBImageByte)MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		
		if(initMask == null)
		{
			initMask = createInitBar(isHorizontal);
		}
		
//		MTBImage initImg = createInitBar(isHorizontal);
		//initImg.show();
		
		// segment filtered image using level sets
		if(verbose)
		{
			System.out.println("segmenting image using topology preserving level sets");
		}
		
		MTBLevelsetMembership levelset = new MTBLevelsetMembership(initMask, null);
		
		MTBGenericEnergyNonPDE energy = new MTBCVFittingEnergyNonPDE(filterImg, levelset, 1, 1);
		
		LevelsetSolveNonPDE solver = new LevelsetSolveNonPDE(energy, levelset, maxIter, 0, null, true);
		solver.setVerbose(verbose);
		//solver.setSpacingIntermediate(100);
		solver.runOp();
		
		resultImg = solver.getResultImage().convertType(MTBImage.MTBImageType.MTB_BYTE, false);
		
		numIterations = solver.getNumIterations();
		
		
//			Vector<MTBImageByte> intermediates = solver.getIntermediateLS();
//			for(int i = 0; i < intermediates.size(); i++)
//			{
//				intermediates.elementAt(i).show();
//			}
		
		
		if(verbose)
		{
			System.out.println("postprocessing segmented image");
		} 

		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(resultImg.getValueInt(x, y) == 0)
				{
					resultImg.putValueDouble(x, y, 0);
				}
				else
				{
					resultImg.putValueDouble(x, y, 255);
				}
			}
		}
			
		// closing
		MTBStructuringElement mask = MTBStructuringElement.createCircularElement(closingMaskSize);
		BasicMorphology morphology = new BasicMorphology(resultImg, mask);
		morphology.setMode(BasicMorphology.opMode.CLOSE);
		morphology.runOp();
		resultImg = morphology.getResultImage();
		
		// fill holes
		FillHoles2D fh = new FillHoles2D(resultImg);
		fh.runOp();
		resultImg = fh.getResultImage();
		
		// measure wound area
		scratchArea = measure(); //new MTBDoubleData(measure());
			
		
		// classification
		if(!noCheck)
		{
			double c = 1;	// c = 1; scratch present, c = 0: no scratch present
//			try
//			{
				c = classify(filterImg, resultImg, modelFile);
				
				if(verbose)
				{
					if(c == 1)
					{
						System.out.println("scratch present");
					}
					else
					{
						System.out.println("scratch not present");
					}
					
				}
//			} 
//			catch(IOException e1)
//			{
//				e1.printStackTrace();
//			}
				
			if(c != 1)	// no scratch present
			{
				scratchArea = 0d;
				resultImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImageType.MTB_BYTE);
				resultImg.fillBlack();
			}
		}
		
		
		resultImg.setTitle("result");
		
		long stop = System.currentTimeMillis();
		
		runtime = (stop - start) / 1000;
		
	}
	
	
	/**
	 * 
	 * @return	the resulting image
	 * @throws 	ALDOperatorException
	 */
	public MTBImage getResultImage() throws ALDOperatorException 
	{
		return this.resultImg;
	}
	
	
	/**
	 * 
	 * @return	the measured scratch area
	 * @throws 	ALDOperatorException
	 */
	public Double getScratchArea() throws ALDOperatorException
	{
		return this.scratchArea;
	}
	
	
	/**
	 * 
	 * @return the number of iterations required for the level set segmentation
	 */
	public Integer getNumIterations()
	{
		return this.numIterations;
	}
	
	
	/**
	 * 
	 * @return time in sec required for the segmentation 
	 */
	public Long getRuntime()
	{
		return this.runtime;
	}
	
	
	/**
	 * 
	 * @return entropy filter image
	 */
	public MTBImage getEntropyImage()
	{
		return this.entropyImg;
	}
	
	
	public void useExternalSVM(boolean useExternalSVM)
	{
		this.useExternalSVM = useExternalSVM;
	}
	
	
	public void setSVMFile(String path)
	{
		this.svmFile = new ALDFileString(path);
	}
	
	
	public void setInitMask(MTBImage initMask)
	{
		this.initMask = initMask;
	}
	
	
	/**
	 * 
	 * @return array containing features extracted from the histogram of the segmented scratch area from the entropy image
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	public double[] getScratchFeatures() throws ALDOperatorException, ALDProcessingDAGException
	{
		return scratchFeatures;
	}
	
	/**
	 * measures the scratch area, i.e. number of pixels that don't have value 0
	 * 
	 * @throws ALDOperatorException
	 */
	private double measure() throws ALDOperatorException
	{
		double sum = 0;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(resultImg.getValueInt(x, y) != 0)
				{
					sum++;
				}
			}
		}
		
		return sum;
	}
	
	
	/**
	 * create rectangular shaped bar as initialization for the scratch
	 * 
	 * @param 	horizontal should the craeted bar be horizontally oriented
	 * @return 	Image containing a rectangular bar in the middle of the image
	 */
	private MTBImage createInitBar(boolean horizontal)
	{
		MTBImage initImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		
		int centerX = sizeX/2;
		int centerY = sizeY/2;
	
		if(horizontal)	// horizontal scratch
		{
			int halfheight = (int)((sizeY * 0.1)/2);
			for(int y = centerY - halfheight; y < centerY + halfheight; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					initImg.putValueInt(x,y,255);
				}
			}
		}
		else	// vertical scratch
		{
			int halfwidth = (int)((sizeX * 0.1)/2);
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = centerX - halfwidth; x < centerX + halfwidth; x++)
				{
					initImg.putValueInt(x,y,255);
				}
			}
		}
		
		//initImg.show();
		
		return initImg;
	}
	
	/*************************************************************************
	 *                   methods used for classification                     *
	 *************************************************************************/
	
	
	/**
	 * decide, whether the given entropy image contains an open scratch or not
	 * 
	 * @param entropyImg
	 * @param scratchMask
	 * @param modelFile
	 * @return 1, if a scratch is present; -1, if not
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private double classify(MTBImage entropyImg, MTBImage scratchMask, String modelFile) throws ALDOperatorException, ALDProcessingDAGException
	{
		double ed = getEntropyMeanDifference(entropyImg, scratchMask);
		double bc = getBhattacharyyaCoefficient(entropyImg, scratchMask);
		double ks = getKolmogorovSmirnovStatistic(entropyImg, scratchMask);
		
		scratchFeatures = new double[]{ed, bc, ks};
		
		if(verbose)
		{
			System.out.println("entropy difference: " + ed);
			System.out.println("Bhattacharyya coefficient: " + bc);
			System.out.println("Kolmogorov-Smirnov statistic: " + ks);
		}
		
		// read svm file
		InputStream is = null;
		BufferedReader br = null;
		svm_model model;
		
		// initialize file reader 
		try
		{
			if(!useExternalSVM)
			{
				if(verbose)
				{
					System.out.println("Searching for internal svm config file");
				}
				
				File svm_config = new File("./" + modelFile);
				
				if(svm_config.exists())
				{
					if(verbose)
					{
						System.out.println("Reading file from given path...");
					}
					
					model = svm.svm_load_model("./" + modelFile);
				}
				else
				{
					// try to find it inside a jar archive....
					if(verbose)
					{
						System.out.println("Reading file from given path...");
						System.out.println("Reading file from jar...");
					}
					
					is = ScratchAssaySegmenter.class.getResourceAsStream(modelFile);
					br = new BufferedReader(new InputStreamReader(is));
					model = svm.svm_load_model(br);
				}
				
				if(verbose)
				{
					System.out.println("Successfully read model file");
				}
				

			}
			else
			{
				if(verbose)
				{
					System.out.println("reading external model file...");
				}
				
				model = svm.svm_load_model(svmFile.getFileName());
			}
						
			svm_node[] x = new svm_node[3];
			x[0] = new svm_node();
			x[0].index = 1;
//			x[0].value = bc;
			x[0].value = ed;
			
			x[1] = new svm_node();
			x[1].index = 2;
//			x[1].value = ed;
			x[1].value = bc;
			
			x[2] = new svm_node();
			x[2].index = 3;
			x[2].value = ks;
			
			double v = svm.svm_predict(model, x);
			
			return v;
		}
		catch(IOException e) 
		{
			System.err.println("Could not read model file!");
		}
		catch(NullPointerException e)
		{
			System.err.println("Could not read model file!");
		}
		
		return 1;	// svm file could not be loaded, result is classified as scratch containing image
	}
	
	
	/**
	 * 
	 * @param entropyImg
	 * @param scratchMask
	 * @return the difference between the mean entropies of the segmented scratch and cell areas 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private double getEntropyMeanDifference(MTBImage entropyImg, MTBImage scratchMask) throws ALDOperatorException, ALDProcessingDAGException
	{
		double meanScratch = 0;
		double meanCells = 0;
		double sumScratch = 0;
		double sumCells = 0;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(scratchMask.getValueDouble(x, y) != 0)
				{
					meanScratch += entropyImg.getValueDouble(x, y);
					sumScratch++;
				}
				else
				{
					meanCells += entropyImg.getValueDouble(x, y);
					sumCells++;
				}
			}
		}
		
		if(sumScratch != 0)
		{
			meanScratch /= sumScratch;
		}
		if(sumCells != 0)
		{
			meanCells /= sumCells;
		}
		
		return Math.abs(meanScratch - meanCells);
		
		
	}
	
	
	/**
	 * 
	 * @param entropyImg
	 * @param scratchMask
	 * @return Bhattacharyya coefficient of histograms for segmented scratch area and the rest of the image (cell area)
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private double getBhattacharyyaCoefficient(MTBImage entropyImg, MTBImage scratchMask)  throws ALDOperatorException, ALDProcessingDAGException
	{
		int bins = 256;
		
		// create normalized histograms of segmented scratch and cell area
		MTBImageHistogram scratchHist = new MTBImageHistogram(entropyImg, scratchMask, bins, 
												    entropyImg.getMinMaxDouble()[0], entropyImg.getMinMaxDouble()[1]);
		scratchHist.normalize();
		
		MTBImage cellMask = scratchMask.duplicate();
		ImageValueTools.invertImage(cellMask, null);
		
		MTBImageHistogram cellHist = new MTBImageHistogram(entropyImg, cellMask, bins, 
												 entropyImg.getMinMaxDouble()[0], entropyImg.getMinMaxDouble()[1]);
		
		cellHist.normalize();
		
		// calculate Bhattacharyya coefficient of the two histograms
		double[] sData = scratchHist.getData();
		double[] cData = cellHist.getData();
		double bc = 0;

		for(int i = 0; i < bins; i++)
		{
			bc += Math.sqrt(sData[i] * cData[i]);
		}
		
		if(Double.isNaN(bc))
		{
			bc = 1;
		}
		
		return bc;
	}
	
	
	/**
	 * 
	 * @param entropyImg
	 * @param scratchMask
	 * @return Kolmogorov-Smirnov statistic of histograms for segmented scratch area and the rest of the image (cell area)
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private double getKolmogorovSmirnovStatistic(MTBImage entropyImg, MTBImage scratchMask) throws ALDOperatorException, ALDProcessingDAGException
	{
		int bins = 1024;
		
		// create normalized histograms of segmented scratch and cell area
		MTBImageHistogram scratchHist = new MTBImageHistogram(entropyImg, scratchMask, bins, 
												    entropyImg.getMinMaxDouble()[0], entropyImg.getMinMaxDouble()[1]);
		scratchHist.normalize();
		scratchHist.cumulate();
		
		MTBImage cellMask = scratchMask.duplicate();
		ImageValueTools.invertImage(cellMask, null);
		
		MTBImageHistogram cellHist = new MTBImageHistogram(entropyImg, cellMask, bins, 
												 entropyImg.getMinMaxDouble()[0], entropyImg.getMinMaxDouble()[1]);
		
		cellHist.normalize();
		cellHist.cumulate();
		
		double[] scratchData = scratchHist.getData();
		double[] cellData = cellHist.getData();
		
		double maxDiff = 0;
		
		for(int i = 0; i < scratchData.length; i++)
		{
			double diff = Math.abs(scratchData[i] - cellData[i]);
			
			if(diff > maxDiff)
			{
				maxDiff = diff;
			}
		}
		
		return maxDiff;
	}
	
	@Override
	public String getDocumentation() {
		return "<ul><li>\r\n" + 
				"<p>operator for segmenting the wound area of a scratch assay image</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h2>Usage:</h2>\r\n" + 
				"<h3>required parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>input image</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>image to be segmented</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>horizontal scratch</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>is scratch horizontally oriented (else it is assumed to be vertically oriented)</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>entropy filter size</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>size of entropy filter mask</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>increase lets the scratch area decrease</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>sigma</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>standard deviation of gauss filter</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>increase leads to more image smoothing and scratch area tends to decrease</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h3>optional parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>maximum iterations</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>maximum number of iterations for level set segmentation</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>don't check for scratch presence</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>don't check for scratch presence prior to segmentation</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>deactivate, if built-in check for scratch presence fails</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>alternative: train a new svm model, cf. <a href=\"stml:de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssaySVMTrainer\">Scratch Assay SVM Trainer</a></p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>use external svm file</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>should an external svm file be used for classification</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p>the automatic scratch detection uses a built-in support vector machine model to decide whether an image contains a scratch or not, if this detection doesn't work properly an external model file created with the <a href=\"stml:de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssaySVMTrainer\">Scratch Assay SVM Trainer</a> can be used for this task</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li><li>\r\n" + 
				"<p><tt>external svm file</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>absolute path to an external svm model file</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>\r\n" + 
				"<h3>supplemental parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul><li>\r\n" + 
				"<p><tt>Verbose</tt>\r\n" + 
				"<ul><li>\r\n" + 
				"<p>output some additional information</p>\r\n" + 
				"</li></ul>\r\n" + 
				"</p>\r\n" + 
				"</li></ul>";
	}
}
