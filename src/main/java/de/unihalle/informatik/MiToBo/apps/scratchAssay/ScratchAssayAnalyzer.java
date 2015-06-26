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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.apps.scratchAssay;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Vector;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * class for analyzing the scratch area in several scratch assay images
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.APPLICATION, allowBatchMode = false)
public class ScratchAssayAnalyzer extends MTBOperator
{
	@Parameter(label = "first image file", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "name of the first scratch assay image", mode=ExpertMode.STANDARD, dataIOOrder = 0)
	private ALDFileString fileName = new ALDFileString(IJ.getDirectory("current"));
	
	@Parameter(label = "scratch orientation", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "is scratch horizontally or vertically oriented", mode = ExpertMode.STANDARD, dataIOOrder = 1)
	private ScratchOrientation orientation = ScratchOrientation.HORIZONTALLY;
	
	@Parameter(label = "\u03C3", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of gauss filter", mode=ExpertMode.STANDARD, dataIOOrder = 2)
	private Integer sigma = 2;
	
	@Parameter(label = "entropy filter size", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "size of entropy filter mask", mode=ExpertMode.STANDARD, dataIOOrder = 3)
	private Integer entropyFilterSize = 25;
	
	@Parameter(label = "maximum iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations for level set segmentation", mode=ExpertMode.ADVANCED, dataIOOrder = 4)
	private Integer maxIter = 2000;
	
	@Parameter(label = "don't check for scratch presence", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "don't check for scratch presence prior to segmentation", mode=ExpertMode.STANDARD, dataIOOrder = 5)
	private Boolean noCheck = false;
	
//	@Parameter(label = "batch mode", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should all image files in the given directory be analyzed", mode=ExpertMode.STANDARD, dataIOOrder = 2)
//	private Boolean doBatch = true;
	
	@Parameter(label = "input type", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "which type of input data is supplied", mode=ExpertMode.STANDARD, dataIOOrder = 6)
	private InputType inType = InputType.VIDEO_FILE;
	
	@Parameter(label = "initialize with previous result", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the current segmentation be initialized with the result from the previous segmentation", mode=ExpertMode.ADVANCED, dataIOOrder = 7)
	private Boolean initWithPrevious = false;
	
	@Parameter(label = "use external svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should an external svm file be used for classification", mode=ExpertMode.ADVANCED, dataIOOrder = 8)
	private Boolean useExternalSVM = false;
	
	@Parameter(label = "external svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "absolute path to external svm model file", mode=ExpertMode.ADVANCED, dataIOOrder = 9)
	private ALDFileString svmFile = new ALDFileString(IJ.getDirectory("current"));
	
	//analysis parameters
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 10)
	private Double deltaX = 1.0;
		
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 11)
	private Double deltaY = 1.0;
		
	@Parameter(label = "unit x/y", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 12)
	private String unitXY = "pixel";

	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "table containing the resulting values")
	private MTBTableModel resultsTable = null;
	
	@Parameter(label = "silent", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should results be stored automatically", mode=ExpertMode.STANDARD)
	private Boolean silent = false;
	
	
	public enum ScratchOrientation
	{
		HORIZONTALLY,
		VERTICALLY
	}
	
	public enum InputType
	{
		VIDEO_FILE,
		MULTIPLE_IMAGES,
		SINGLE_IMAGE
	}
	
	private Boolean isHorizontal;
	
	private Vector<String> scratchFiles;
	private Vector<Double> scratchAreas;
	private Vector<Double> totalAreas;		
	private double refArea;
	
	private Vector<Integer> numIterations = new Vector<Integer>();	// vector for storing the number of iterations used for segmentation
	private Vector<Long> runtimes = new Vector<Long>();	// vector for storing the runtimes of each analysis
	
	private double lastArea = 0;
	
	public ScratchAssayAnalyzer() throws ALDOperatorException
	{
		super();
	}
	
	
//	/**
//	 * 
//	 * @param fileName	name of the first scratch assay image
//	 * @param sigma	standard deviation of gauss filter
//	 * @param entropyFilterSize	size of entropy filter mask
//	 * @param isHorizontal	is scratch horizontally oriented (assumed to be vertically oriented else)
//	 * @param noCheck	should scratch present not be checked
//	 * @param doBatch	should all image files in the given directory be analyzed
//	 * @param maxIter	maximum number of iterations for level set segmentation
//	 * @throws ALDOperatorException
//	 */
//	public ScratchAssayAnalyzer(String fileName, int sigma, int entropyFilterSize, boolean isHorizontal, boolean noCheck, boolean doBatch, int maxIter) throws ALDOperatorException
//	{
//		this.fileName = new ALDFileString(fileName);
//		
//		this.sigma = sigma;
//		this.entropyFilterSize = entropyFilterSize;
//		this.isHorizontal = isHorizontal;
//		this.noCheck = noCheck;
//		this.doBatch = doBatch;
//		this.maxIter = maxIter;
//	}
	
	/**
	 * 
	 * @param fileName	name of the first scratch assay image
	 * @param sigma	standard deviation of gauss filter
	 * @param entropyFilterSize	size of entropy filter mask
	 * @param orientation	is scratch horizontally oriented (assumed to be vertically oriented else)
	 * @param noCheck	should scratch present not be checked
	 * @param inType	which type of input data is provided
	 * @param maxIter	maximum number of iterations for level set segmentation
	 * @throws ALDOperatorException
	 */
	public ScratchAssayAnalyzer(String fileName, int sigma, int entropyFilterSize, ScratchOrientation orientation, boolean noCheck, InputType inType, int maxIter) throws ALDOperatorException
	{
		this.fileName = new ALDFileString(fileName);
		
		this.sigma = sigma;
		this.entropyFilterSize = entropyFilterSize;
		this.orientation = orientation;
//		this.isHorizontal = isHorizontal;
		this.noCheck = noCheck;
		this.inType = inType;
		this.maxIter = maxIter;
		
	}
	
	
	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{	
		switch(orientation)
		{
			case HORIZONTALLY:
				isHorizontal = true;
				break;
			case VERTICALLY:
				isHorizontal = false;
				break;
		}
		
		scratchFiles = new Vector<String>();
		scratchAreas = new Vector<Double>();
		totalAreas = new Vector<Double>();		
		refArea = Double.MIN_VALUE;
		numIterations = new Vector<Integer>();	// vector for storing the number of iterations used for segmentation
		runtimes = new Vector<Long>();	// vector for storing the runtimes of each analysis
		
		String directory = new File(fileName.getFileName()).getParent();
		
		if(inType == InputType.VIDEO_FILE)
		{	
			MTBImage video = readImage(fileName.getFileName());
			
			int n = video.getSizeT();
			
			MTBImage result = MTBImage.createMTBImage(video.getSizeX(), video.getSizeY(), 1, video.getSizeT(), 1, MTBImage.MTBImageType.MTB_BYTE);
			MTBImage initMask = null;
			
			IJ.showProgress(0);
			
			for(int i = 0; i < n; i++)
			{
				MTBImage currImg = video.getSlice(0, i, 0);
				
				if(lastArea == 0 || initWithPrevious == false)
				{
					initMask = null;
				}
				
				String caption = "frame " + i;
				
				MTBImage currResult = segment(currImg, initMask, caption);
				
				result.setSlice(currResult, 0, i, 0);
				initMask = currResult.duplicate();
				
				IJ.showProgress(i, n);
			}
			
			refArea = scratchAreas.elementAt(0);
			
			resultsTable = makeTable();
			
			if(silent)
			{
				String saveDir = directory + "/";
				String savePath;
				
				if(noCheck)
				{
					savePath = saveDir + video.getTitle() + "_segmentation_noCheck.tif";
				}
				else
				{
					savePath = saveDir + video.getTitle() + "_segmentation.tif";
				}
				
				IJ.showStatus("writing result image ...");
				
				writeImage(result, savePath);
				
				File tableFile = new File(saveDir + video.getTitle() + "ResultTab.txt");
				
				resultsTable.saveTable(tableFile);
			}
			else
			{
				result.show();
				
				ALDTableWindow tw = new ALDTableWindow(resultsTable);
				tw.setTitle("results");
				tw.openWindow();
			}
			
		}
		else if(inType == InputType.MULTIPLE_IMAGES)
		{
			File dir = new File(directory);
			
			File[] files = dir.listFiles();
			
			String saveDir = directory + "/analysis/";
			
			if(noCheck)
			{
				saveDir = directory + "/analysis_noCheck/";
			}
			
			File saveDirectory = new File(saveDir);

			boolean dirCreated = false;
			
			if(silent)
			{
				dirCreated = saveDirectory.mkdir();
			}
			
			if(!dirCreated)
			{
				silent = false;
			}

			int n = 0;
			
			if(files != null)
			{
				n = files.length;
			}
			
			MTBImage initMask = null;
			
			IJ.showProgress(0);
			
			for(int i = 0; i < n; i++)
			{
				File currFile = files[i];
				
				if(currFile.isFile())
				{
					String path = currFile.getAbsolutePath();
					
					if(verbose)
					{
						System.out.println("processing " + path);
					}
					
					
					MTBImage currImg = readImage(path);
					
					if(currImg != null)
					{
						if(lastArea == 0 || initWithPrevious == false)
						{
							initMask = null;
						}
						
						MTBImage currResultImg = segment(currImg, initMask, path);
						
						currResultImg.setTitle(path + "_segmentation");
						
						String currSavePath = saveDir + currFile.getName() + "_segmentation.tif";
						
						if(silent)
						{
							writeImage(currResultImg, currSavePath);
						}
						else
						{
							currResultImg.show();
						}
						
						initMask = currResultImg.duplicate();
					}			
				}
				
				IJ.showProgress(i, n);
			}
			
			resultsTable = makeTable();
			
			if(silent)
			{
				File tableFile = new File(saveDir + "ResultTab.txt");
				
				resultsTable.saveTable(tableFile);
			}
			else
			{
				ALDTableWindow tw = new ALDTableWindow(resultsTable);
				tw.setTitle("results");
				tw.openWindow();
			}
			
		}
		else	// only a single image has to be analyzed
		{
			String path = fileName.getFileName();
			
			if(verbose)
			{
				System.out.println("processing " + path);
			}
			
			MTBImage img = readImage(path);
			
			MTBImage resultImg = segment(img, null, path);
			
			resultsTable = makeTable();
			
			if(silent)
			{
				String saveDir = directory + "/";
				
				String savePath;
				
				if(noCheck)
				{
					savePath = saveDir + img.getTitle() + "_segmentation_noCheck.tif";
				}
				else
				{
					savePath = saveDir + img.getTitle() + "_segmentation.tif";
				}
				
				writeImage(resultImg, savePath);
				
				File tableFile = new File(saveDir + "ResultTab.txt");
				
				resultsTable.saveTable(tableFile);
			}
			else
			{
				resultImg.show();
				
				ALDTableWindow tw = new ALDTableWindow(resultsTable);
				tw.setTitle("results");
				tw.openWindow();
			}

		}
		
		IJ.showStatus("done!");
		
	}
	
	
	private MTBImage segment(MTBImage img, MTBImage initMask, String path) throws ALDOperatorException, ALDProcessingDAGException
	{
		ScratchAssaySegmenter segmenter = new ScratchAssaySegmenter(img, sigma, entropyFilterSize, isHorizontal, noCheck, maxIter);

		if(initMask != null)
		{
			segmenter.setInitMask(initMask);
		}
		
		if(useExternalSVM)
		{
			segmenter.useExternalSVM(true);
			segmenter.setSVMFile(svmFile.getFileName());
		}
		
		segmenter.setVerbose(verbose);
		segmenter.runOp(HidingMode.HIDE_CHILDREN);	// no documentation of the inner operator
		
		double area = segmenter.getScratchArea() * deltaX * deltaY;	// get determined scratch area and scale it according to the physical pixel size
		
		MTBImage resultImg = segmenter.getResultImage();	// get segmented scratch image
		
		scratchFiles.add(path);
		scratchAreas.add(area);
		totalAreas.add((double)(img.getSizeX() * deltaX * img.getSizeY() * deltaY));
		numIterations.add(segmenter.getNumIterations());
		runtimes.add(segmenter.getRuntime());
		
		if(path.equalsIgnoreCase(fileName.getFileName()))
		{
			refArea = area;
		}
	
		lastArea = area;
		
		return resultImg;
	}
	
	
	/**
	 * create results table
	 * 
	 * @param scratchFiles
	 * @param scratchAreas
	 * @param totalAreas
	 * @param refArea
	 * @return table containing the results
	 */
	private MTBTableModel makeTable()
	{
		int n = scratchFiles.size();
		
		// display options
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("file");
		header.add("area ("+ unitXY + "^2)");
		header.add("fraction of total area(%)");
		header.add("area normalized to first image (%)");
		header.add("area decrease in reference to first image (" + unitXY + "^2)");
		
		if(verbose)
		{
			header.add("number of iterations");
			header.add("runtime (sec)");
		}
		
		MTBTableModel table = new MTBTableModel(n, header.size(), header);
		
		for(int i = 0; i < n; i++)
		{
			double area = scratchAreas.elementAt(i);
			
			// insert values into results table
			table.setValueAt(scratchFiles.elementAt(i), i, 0);
			table.setValueAt(nf.format(area), i, 1);
			table.setValueAt(nf.format((area / totalAreas.elementAt(i)) * 100), i, 2);
			
			if(refArea != 0)
			{
				table.setValueAt(nf.format((area / refArea) * 100), i, 3);
			}
			else
			{
				table.setValueAt(nf.format(0), i, 3);
			}
			table.setValueAt(nf.format(refArea - area), i, 4);
			
			if(verbose)
			{
				table.setValueAt(numIterations.elementAt(i), i, 5);
				table.setValueAt(runtimes.elementAt(i), i, 6);
			}
			
		}
		
		return table;
	}
	
//	/**
//	 * create results table
//	 * 
//	 * @param scratchFiles
//	 * @param scratchAreas
//	 * @param totalAreas
//	 * @param refArea
//	 * @return table containing the results
//	 */
//	private MTBTableModel makeTable(Vector<String> scratchFiles, Vector<Double> scratchAreas, Vector<Double> totalAreas, double refArea, Vector<Integer> numIterations, Vector<Long> runtimes)
//	{
//		int n = scratchFiles.size();
//		
//		// display options
//		NumberFormat nf = NumberFormat.getInstance();
//		nf.setMaximumFractionDigits(2);
//		
//		// initialize table
//		Vector<String> header = new Vector<String>();
//		header.add("file");
//		header.add("area ("+ unitXY + ")");
//		header.add("fraction of total area(%)");
//		header.add("area normalized to first image (%)");
//		header.add("area decrease in reference to first image (" + unitXY + ")");
//		
//		if(verbose)
//		{
//			header.add("number of iterations required");
//			header.add("runtime required (sec)");
//		}
//		
//		MTBTableModel table = new MTBTableModel(n, header.size(), header);
//		
//		for(int i = 0; i < n; i++)
//		{
//			double area = scratchAreas.elementAt(i);
//			
//			// insert values into results table
//			table.setValueAt(scratchFiles.elementAt(i), i, 0);
//			table.setValueAt(nf.format(area), i, 1);
//			table.setValueAt(nf.format((area / totalAreas.elementAt(i)) * 100), i, 2);
//			
//			if(refArea != 0)
//			{
//				table.setValueAt(nf.format((area / refArea) * 100), i, 3);
//			}
//			else
//			{
//				table.setValueAt(nf.format(0), i, 3);
//			}
//			table.setValueAt(nf.format(refArea - area), i, 4);
//			
//			if(verbose)
//			{
//				table.setValueAt(numIterations.elementAt(i), i, 5);
//				table.setValueAt(runtimes.elementAt(i), i, 6);
//			}
//			
//		}
//		
//		return table;
//	}
	
	
	/**
	 * 
	 * @param path
	 * @return image read from given path (if it points to a valid image)
	 */
	private MTBImage readImage(String path)
	{		
		MTBImage img = null;
		
		try
		{
			ImageReaderMTB reader = new ImageReaderMTB(path);
			reader.runOp();
			
			img = reader.getResultMTBImage();
		} 
		catch(ALDOperatorException e1)
		{
			e1.printStackTrace();
		} 
		catch(FormatException e1)
		{
			e1.printStackTrace();
		} 
		catch(IOException e1)
		{
			e1.printStackTrace();
		} 
		catch(DependencyException e1)
		{
			e1.printStackTrace();
		} 
		catch(ServiceException e1)
		{
			e1.printStackTrace();
		} 
		catch(ALDProcessingDAGException e)
		{
			e.printStackTrace();
		}
		
		return img;
	}
	
	
	/**
	 * writes the given image to a file with the specified path
	 * 
	 * @param img MTBImage to save
	 * @param path absolute path for storing
	 */
	private void writeImage(MTBImage img, String path)
	{
		try
		{
			ImageWriterMTB writer = new ImageWriterMTB(img, path);
			
			writer.runOp();
		} 
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		} 
		catch(ALDProcessingDAGException e)
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * should results be saved without user interaction
	 * 
	 * @param silent
	 */
	public void isSilent(boolean silent)
	{
		this.silent = silent;
	}
	
	
	/**
	 * sets the path to a svm model file used for classification
	 * 
	 * @param path
	 */
	public void setSVMFile(String path)
	{
		useExternalSVM = true;
		
		this.svmFile = new ALDFileString(path);
	}
	
	
	public void initializeWithPrevious(boolean initWithPrevious)
	{
		this.initWithPrevious = initWithPrevious;
	}

}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/scratchAssay/ScratchAssayAnalyzer.html">API</a></p>
 
<ul><li>
<p>This operator analyzes scratch assay image sequences</p>
</li><li>
<p>If desired, it analyzes all images contained in the same directory</p>
</li><li>
<p>One can choose a reference image and all results then are refered to this reference image</p>
</li><li>
<p>Results are the segmented images and a table containing scratch areas as well as area differences</p>
</li></ul>
<h2>Usage:</h2>
<h3>required parameters:</h3>

<ul><li>
<p><tt>first image file</tt> 
<ul><li>
<p>name of the first (reference) scratch assay image</p>
</li><li>
<p>the only analyzed image if batch mode is deactivated</p>
</li></ul>
</p>
</li><li>
<p><tt>scratch orientation</tt>
<ul><li>
<p>horizontally or </p>
</li><li>
<p>vertically</p>
</li></ul>
</p>
</li><li>
<p><tt>entropy filter size</tt>
<ul><li>
<p>size of entropy filter mask</p>
</li><li>
<p>increase let the scratch area decrease</p>
</li></ul>
</p>
</li><li>
<p><tt>sigma</tt>
<ul><li>
<p>standard deviation of gauss filter</p>
</li><li>
<p>increase leads to more image smoothing and scratch area tends to decrease</p>
</li></ul>
</p>
</li></ul>
<h3>optional parameters:</h3>

<ul><li>
<p><tt>batch mode</tt>
<ul><li>
<p>should all image files in the given directory be analyzed</p>
</li></ul>
</p>
</li><li>
<p><tt>maximum iterations</tt>
<ul><li>
<p>maximum number of iterations for level set segmentation</p>
</li></ul>
</p>
</li><li>
<p><tt>don't check for scratch presence</tt>
<ul><li>
<p>don't check for scratch presence prior to segmentation</p>
</li><li>
<p>deactivate, if built-in check for scratch presence fails</p>
</li><li>
<p>alternative: train a new svm model, cf. <a href="de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssaySVMTrainer.html">Scratch Assay SVM Trainer</a></p>
</li></ul>
</p>
</li><li>
<p><tt>silent</tt>
<ul><li>
<p>should results be stored automatically</p>
</li><li>
<p>segmented images and results table are just displayed without saving, if unchecked</p>
</li></ul>
</p>
</li><li>
<p><tt>use external svm file</tt>
<ul><li>
<p>should an external svm file be used for classification</p>
</li><li>
<p>the automatic scratch detection uses a built-in support vector machine model to decide whether an image contains a scratch or not, if this detection doesn't work properly an external model file created with the <a href="de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssaySVMTrainer.html">Scratch Assay SVM Trainer</a> can be used for this task</p>
</li></ul>
</p>
</li><li>
<p><tt>external svm file</tt>
<ul><li>
<p>absolute path to an external svm model file</p>
</li></ul>
</p>
</li></ul>
<h3>supplemental parameters:</h3>

<ul><li>
<p><tt>Verbose</tt>
<ul><li>
<p>output some additional information</p>
</li></ul>
END_MITOBO_ONLINE_HELP*/
