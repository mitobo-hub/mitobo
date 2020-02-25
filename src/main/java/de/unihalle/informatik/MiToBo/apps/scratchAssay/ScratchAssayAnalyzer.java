package de.unihalle.informatik.MiToBo.apps.scratchAssay;

import ij.IJ;

import java.text.NumberFormat;
import java.util.Vector;

import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;


/**
 * class for analyzing the scratch areas in a scratch assay/ gap closure/ wound closure assay
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode = true,
	shortDescription="Analyzes the scratch areas in a scratch assay/ gap closure/ wound closure assays.")
public class ScratchAssayAnalyzer extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", mode = ExpertMode.STANDARD, dataIOOrder = 1,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient  MTBImage inImg = null;
	
	// segmentation parameters
	@Parameter(label = "detection channel", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "detection channel", dataIOOrder = 2)
	private Integer detectionChannel = 1;
	
	@Parameter(label = "scratch orientation", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "is scratch horizontally or vertically oriented", mode = ExpertMode.STANDARD, dataIOOrder = 3)
	private ScratchOrientation orientation = ScratchOrientation.HORIZONTALLY;
	
	@Parameter(label = "\u03C3", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of gauss filter", mode=ExpertMode.STANDARD, dataIOOrder = 4)
	private Integer sigma = 2;
	
	@Parameter(label = "entropy filter size", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "size of entropy filter mask", mode=ExpertMode.STANDARD, dataIOOrder = 5)
	private Integer entropyFilterSize = 25;
	
	@Parameter(label = "maximum iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations for level set segmentation", mode=ExpertMode.ADVANCED, dataIOOrder = 6)
	private Integer maxIter = 2000;
	
	// parameters regarding check for scratch absence
//	@Parameter(label = "check for scratch absence", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "check if scratch is present in the images", mode=ExpertMode.STANDARD, dataIOOrder = 7,
//			callback = "showUseExternalSVMCheckBox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
//	private Boolean check = true;
	
	@Parameter(label = "method for checking for scratch absence", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "which method should be used to check if scratch is present in the images", mode=ExpertMode.STANDARD, dataIOOrder = 8,
			callback = "showParametersForChecking", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private AbsenceDetectionMethod checkMethod = AbsenceDetectionMethod.INCREASING_AREA;
	
	@Parameter(label = "maximum area increase", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "if detected scratch area between two consecutive frames increases more than this fraction the scratch will be considered as closed for all following frames", mode=ExpertMode.STANDARD, dataIOOrder = 9)
	private double maxIncreaseFraction = 0.1;
	
//	@Parameter(label = "use external svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should an external svm file be used for classification", mode=ExpertMode.ADVANCED, dataIOOrder = 10,
//			callback = "showExternalSVMPathTextBox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
//	private Boolean useExternalSVM = false;
	
	@Parameter(label = "path to svm file", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "absolute path to external svm model file", mode=ExpertMode.STANDARD, dataIOOrder = 11)
	private ALDFileString svmFile = new ALDFileString(IJ.getDirectory("current"));
	
	//analysis parameters
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 12)
	private Double deltaX = 1.0;
		
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 13)
	private Double deltaY = 1.0;
		
	@Parameter(label = "unit space", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 14)
	private String unitXY = "pixel";
	
	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "table containing the resulting values")
	private MTBTableModel resultsTable = null;
	
	@Parameter(label = "segmentation results", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting image(s)")
	private transient MTBImage result = null;
	
	public enum ScratchOrientation
	{
		HORIZONTALLY,
		VERTICALLY
	}
	
	
	public enum AbsenceDetectionMethod
	{
		INCREASING_AREA,
		SVM
	}
	
	private Boolean isHorizontal;
	
	private Vector<String> scratchFiles;	// file names of input scratch images
	private Vector<Double> scratchAreas;	// areas of segmented scratches
	private Vector<Double> totalAreas;		// areas of input scratch images
//	private double refArea;					// area of reference scratch (t = 0)
	private int refIndex;				// index of reference scratch image (t = 0)
	
	
	private Vector<Integer> numIterations = new Vector<Integer>();	// vector for storing the number of iterations used for segmentation
	private Vector<Long> runtimes = new Vector<Long>();	// vector for storing the runtimes of each analysis
	
	
	public ScratchAssayAnalyzer() throws ALDOperatorException
	{
		super();
	}
	
	
	/**
	 * 
	 * @param inImg
	 * @param sigma
	 * @param entropyFilterSize
	 * @param orientation
	 * @param noCheck
	 * @throws ALDOperatorException
	 */
	public ScratchAssayAnalyzer(MTBImage inImg, int sigma, int entropyFilterSize, ScratchOrientation orientation, boolean check) throws ALDOperatorException
	{
		this.inImg = inImg;
		
		this.sigma = sigma;
		this.entropyFilterSize = entropyFilterSize;
		this.orientation = orientation;
//		this.check = check;
		
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
		refIndex = 0;
		
		numIterations = new Vector<Integer>();	// vector for storing the number of iterations used for segmentation
		runtimes = new Vector<Long>();	// vector for storing the runtimes of each analysis
			
		int sizeX = inImg.getSizeX();
		int sizeY = inImg.getSizeY();
		int sizeT = inImg.getSizeT();
		
		String title = inImg.getTitle();
			
		result = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, MTBImage.MTBImageType.MTB_BYTE);
		result.setTitle(title + "_segmentation");
			
		IJ.showProgress(0);
			
		for(int i = 0; i < sizeT; i++)
		{
			MTBImage currImg = inImg.getImagePart(0, 0, 0, i, detectionChannel-1, sizeX, sizeY, 1, 1, 1);
					
			String caption = "frame " + i;
				
//			currImg.show();
				
			MTBImage currResult = segment(currImg, caption);
				
			result.setSlice(currResult, 0, i, 0);
				
			IJ.showProgress(i, sizeT);
		}
		
		if(checkMethod == AbsenceDetectionMethod.INCREASING_AREA)
		{
			checkForIncrease();
		}
			
		resultsTable = makeTable();
		
		ALDTableWindow tw = new ALDTableWindow(resultsTable);
		tw.setTitle("results");
		tw.openWindow();
			
		IJ.showStatus("done!");
		
	}
	
	
	private MTBImage segment(MTBImage img, String path) throws ALDOperatorException, ALDProcessingDAGException
	{
		ScratchAssaySegmenter segmenter;
		
		if(checkMethod == AbsenceDetectionMethod.SVM)
		{
			segmenter = new ScratchAssaySegmenter(img, sigma, entropyFilterSize, isHorizontal, false, maxIter);
			
//			try
//			{
				segmenter.useExternalSVM(true);
				segmenter.setSVMFile(svmFile.getFileName());
//			}
//			catch()
//			{
//				
//			}
		}
		else
		{
			segmenter = new ScratchAssaySegmenter(img, sigma, entropyFilterSize, isHorizontal, true, maxIter);
		}
		
		
//		segmenter.setVerbose(verbose);
		segmenter.runOp(HidingMode.HIDE_CHILDREN);	// no documentation of the inner operator
		
		double area = segmenter.getScratchArea() * deltaX * deltaY;	// get determined scratch area and scale it according to the physical pixel size
		
		MTBImage resultImg = segmenter.getResultImage();	// get segmented scratch image
		
		scratchFiles.add(path);
		scratchAreas.add(area);
		totalAreas.add((double)(img.getSizeX() * deltaX * img.getSizeY() * deltaY));
		numIterations.add(segmenter.getNumIterations());
		runtimes.add(segmenter.getRuntime());
		
		return resultImg;
	}
	
	
	/**
	 * create results table
	 * 
	 * @return table containing the results
	 */
	private MTBTableModel makeTable()
	{
		double refArea = scratchAreas.elementAt(refIndex);
		
		int n = scratchFiles.size();
		
		// display options
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("frame");
		header.add("area ("+ unitXY + "^2)");
		header.add("fraction of total area (%)");
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
			table.setValueAt(scratchFiles.elementAt(i), i, 0);	// file name
			table.setValueAt(nf.format(area), i, 1);		// absolute scratch area
			table.setValueAt(nf.format((area / totalAreas.elementAt(i)) * 100), i, 2);	// scratch area relative to image size
			
			if(refArea != 0)
			{
				table.setValueAt(nf.format((area / refArea) * 100), i, 3);	// scratch area relative to reference area
			}
			else
			{
				table.setValueAt(nf.format(0), i, 3);				 
			}
			
			table.setValueAt(nf.format(refArea - area), i, 4);		// absolute scratch area difference
			
			
			if(verbose)
			{
				table.setValueAt(numIterations.elementAt(i), i, 5);	// number of iterations
				table.setValueAt(runtimes.elementAt(i), i, 6);		// runtime
			}
			
		}
		
		return table;
	}
	
	
	private void checkForIncrease()
	{
		MTBImage temp = MTBImage.createMTBImage(result.getSizeX(), result.getSizeY(), 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		
		boolean closed = false;
		
		for(int i = 1; i < scratchAreas.size(); i++)
		{
			// if once a scratch area is larger than that from a previous frame all following frames are considered to
			// contain an already closed scratch
			if((scratchAreas.elementAt(i) / scratchAreas.elementAt(i-1))-1 > maxIncreaseFraction)
			{
				closed = true;
			}
			
			if(closed)
			{
				scratchAreas.set(i, 0.0);	// set scratch area to 0
				result.setSlice(temp, 0, i, 0);// set all pixels to background (0)
			}
		}
	}
	
	
	/**
	 * sets the path to a svm model file used for classification
	 * 
	 * @param path
	 */
//	public void setSVMFile(String path)
//	{
//		useExternalSVM = true;
//		
//		this.svmFile = new ALDFileString(path);
//	}
	
	/**
	 * 
	 * @param channel channel used for detection
	 */
	public void setDetectionChannel(int channel)
	{
		this.detectionChannel = channel;
	}
	
	
	/**
	 * set maximum number of iterations for level set segmentation
	 * 
	 * @param maxIter
	 */
	public void setMaxIterations(int maxIter)
	{
		this.maxIter = maxIter;
	}
	
	
	// ------------------------------ callback functions ------------------------------
	
	@SuppressWarnings("unused")
	private void getCalibration()
	{
		try
		{
			if(this.inImg != null)
			{
				this.deltaX = inImg.getCalibration().pixelWidth;
				this.deltaY = inImg.getCalibration().pixelHeight;
				this.unitXY = inImg.getCalibration().getXUnit();
				
				if(inImg.getSizeC() < 2)
				{
					this.detectionChannel = 1;
					
					if(this.hasParameter("detectionChannel"))
					{
						this.removeParameter("detectionChannel");
					}
				}
				else
				{	
					if(!this.hasParameter("detectionChannel"))
					{
						this.addParameter("detectionChannel");
					}
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	@SuppressWarnings("unused")
	private void showParametersForChecking()
	{
		try
		{
			if(checkMethod == AbsenceDetectionMethod.INCREASING_AREA)
			{
				if(!this.hasParameter("maxIncreaseFraction"))
				{
					this.addParameter("maxIncreaseFraction");
				}
				
				if(this.hasParameter("svmFile"))
				{
					this.removeParameter("svmFile");
				}
			}
			else if(checkMethod == AbsenceDetectionMethod.SVM)
			{
				if(!this.hasParameter("svmFile"))
				{
					this.addParameter("svmFile");
				}
				
				if(this.hasParameter("maxIncreaseFraction"))
				{
					this.removeParameter("maxIncreaseFraction");
				}
				
			}
			else
			{
				if(this.hasParameter("maxIncreaseFraction"))
				{
					this.removeParameter("maxIncreaseFraction");
				}
				
				if(this.hasParameter("svmFile"))
				{
					this.removeParameter("svmFile");
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
		
	}
	
//	@SuppressWarnings("unused")
//	private void showUseExternalSVMCheckBox()
//	{
//		try
//		{
//			if(!check)
//			{
//				if(this.hasParameter("useExternalSVM"))
//				{
//					this.removeParameter("useExternalSVM");
//				}
//				
//				if(this.hasParameter("svmFile"))
//				{
//					this.removeParameter("svmFile");
//				}
//			}
//			else
//			{
//				if(!this.hasParameter("useExternalSVM"))
//				{
//					this.addParameter("useExternalSVM");
//				}
//				
//			}
//		}
//		catch(ALDOperatorException e)
//		{
//			e.printStackTrace();
//		}
//	}
	
	
//	@SuppressWarnings("unused")
//	private void showExternalSVMPathTextBox()
//	{
//		try
//		{
//			if(useExternalSVM)
//			{
//				if(!this.hasParameter("svmFile"))
//				{
//					this.addParameter("svmFile");
//				}	
//			}
//			else
//			{
//				if(this.hasParameter("svmFile"))
//				{
//					this.removeParameter("svmFile");
//				}	
//			}
//		}
//		catch(ALDOperatorException e)
//		{
//			e.printStackTrace();
//		}
//	}

	@Override
	public String getDocumentation() {
		return "<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p>Operator for analyzing image sequences from scratch assay/ gap closure/ wound healing experiments</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p>Input can be single images or image stacks from brigthfield as well as fluorescence microscopy</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p>Results are given as segmented images and a table containing scratch areas as well as area differences</p>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n" + 
				"<h2>Usage:</h2>\r\n" + 
				"<h3>required parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>input image</tt> \r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>scratch assay image (stack)</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>if an image stack is given, it is assumed, that it represents one scratch assay experiment with the frames being ordered chronologically</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>detection channel</tt> \r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>channel to segment scratch (only available in multichannel images)</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>scratch orientation</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>horizontally or </p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>vertically</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>&#963; (sigma)</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>standard deviation of Gaussian filter</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>increase leads to more image smoothing (noise reduction) but scratch area tends to decrease</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>entropy filter size</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>size of entropy filter mask used for emphasizing cell areas</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>increase let the scratch area decrease</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	\r\n" + 
				"</ul>\r\n" + 
				"<h3>optional parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>maximum iterations [Advanced View]</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>maximum number of iterations for level set segmentation</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>method for checking for scratch absence</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>method used to check, if there is a scratch/ gap in the single frames</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>none: no check for scratch absence</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>INCREASING_AREA: if detected scratch/ gap area increases from one frame to the next about more than defined by <tt>maximum area increase</tt> then it is assumed the gap is closed already and detected gap is an artifact <br/>\r\n" + 
				"			all subsequent images are considered to contain an already closed gap</p>\r\n" + 
				"		</li>\r\n" + 
				"		<li>\r\n" + 
				"			<p>SVM: a previously trained SVM model is used to recognize images without gaps<br/>\r\n" + 
				"			the path to the SVM model has to be given by <tt>path to svm file</tt><br/>\r\n" + 
				"			a new SVM model can be trained using the <tt>ScratchAssaySVMTrainer</tt> operator, cf. <a href=\"de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssaySVMTrainer.html\">Scratch Assay SVM Trainer</a></p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>maximum area increase</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>maximum increase of gap area between consecutive frames that is not considered to indicate a segmentation artifact due to an already closed gap(only available, if <tt>method for checking for scratch absence</tt> is set to <tt>INCREASING_AREA</tt>)</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>path to svm file</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>absolute path to an external svm model file (only available, if <tt>method for checking for scratch absence</tt> is set to <tt>SVM</tt>)</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>pixel length, x-direction</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>physical length of a pixel in x-direction (without unit)</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>pixel length, y-direction (without unit)</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>physical length of a pixel in y-direction</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>unit space</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>unit of spatial pixel dimensions</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"		</p>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n" + 
				"<h3>supplemental parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>Verbose</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>output some additional information</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n";
	}
}
