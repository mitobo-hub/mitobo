package de.unihalle.informatik.MiToBo.apps.biofilms;

import java.text.NumberFormat;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;

/**
 * operator for calculating areal parameters from biofilm images as described in
 * Yang et al., Quantifying biofilm structure using image analysis.
 * Journal of Microbiological Methods 39 (2000) 109–119.
 * 
 * input image should be a 8-bit gray scale image
 * 
 * @author glass
 * 
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION)
public class ArealParameterExtractor extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image (should be 8 bit gray scale)", dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "calculate areal porosity", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the areal porosity of the image be calculated", dataIOOrder = 1)
	private boolean calcPorosity = true;
	
	@Parameter(label = "calculate avg. horizontal run lengths", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the average length of horizontal runs of the image be calculated", dataIOOrder = 2)
	private boolean calcAvgHorizontalRunLength = true;
	
	@Parameter(label = "calculate avg. vertical run lengths", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the average length of vertical runs of the image be calculated", dataIOOrder = 3)
	private boolean calcAvgVerticalRunLength = true;
	
	@Parameter(label = "calculate avg. diffusion distance", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the average diffusion of the image be calculated", dataIOOrder = 4)
	private boolean calcAvgDiffusionDistance = true;
	
	@Parameter(label = "calculate max. diffusion distance", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the maximum diffusion of the image be calculated", dataIOOrder = 5)
	private boolean calcMaxDiffusionDistance = true;
	
	@Parameter(label = "calculate dilation areas", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the maximum diffusion of the image be calculated", dataIOOrder = 6)
	private boolean calcDilationAreas = true;
	
	@Parameter(label = "minimum dilation mask diameter", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "the minimum diameter of dilation mask used for calculating dilation areas", dataIOOrder = 7)
	private int minDilMaskSize = 3;
	
	@Parameter(label = "maximum dilation mask diameter", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "the maximum diameter of dilation mask used for calculating dilation areas", dataIOOrder = 8)
	private int maxDilMaskSize = 9;
	
	@Parameter(label = "result value digits", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "number of digits for output values", mode=ExpertMode.ADVANCED, dataIOOrder = 9)
	private int outDigits = 3;
	
	//---------------------------------------- output parameters ----------------------------------------
	//@Parameter(label = "areal porosity", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "areal porosity", dataIOOrder = 0)
	private MTBDoubleData arealPorosity = null;
	
	//@Parameter(label = "avg. horizontal run length", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "avg. horizontal run length", dataIOOrder = 1)
	private MTBDoubleData avgHorizontalRunLength = null;
	
	//@Parameter(label = "avg. vertical run length", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "avg. vertical run length", dataIOOrder = 2)
	private MTBDoubleData avgVerticalRunLength = null;
	
	//@Parameter(label = "avg. diffusion distance", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "avg. diffusion distance", dataIOOrder = 3)
	private MTBDoubleData avgDiffusionDist = null;
	
	//@Parameter(label = "max. diffusion distance", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "max. diffusion distance", dataIOOrder = 4)
	private MTBDoubleData maxDiffusionDist = null;
	
	//@Parameter(label = "dilation areas", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "dilation areas", dataIOOrder = 5)
	private Vector<MTBIntegerData> dilationAreas = null;
	
	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "table containing the resulting values")
	private MTBTableModel resultsTable = null;
	
	
	public ArealParameterExtractor() throws ALDOperatorException
	{
		super();
	}
	
	
	public ArealParameterExtractor(MTBImage inImg) throws ALDOperatorException
	{
		this.inImg = inImg;
	}

	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		// initialize (reset) all output values
		arealPorosity = new MTBDoubleData(-1.0);
		avgHorizontalRunLength = new MTBDoubleData(-1.0);
		avgVerticalRunLength = new MTBDoubleData(-1.0);
		avgDiffusionDist = new MTBDoubleData(-1.0);
		maxDiffusionDist = new MTBDoubleData(-1.0);
		dilationAreas = new Vector<MTBIntegerData>();
		
		// convert input image to 8 bit
		MTBImage image = inImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true);
		
		MTBImage distImg = getDistanceImage(image);
		
		if(calcPorosity)
		{
			arealPorosity = new MTBDoubleData(calcArealPorosity(image));
		}
		
		if(calcAvgHorizontalRunLength)
		{
			avgHorizontalRunLength = new MTBDoubleData(calcAvgHorizontalRunLength(image));
		}
		
		if(calcAvgVerticalRunLength)
		{
			avgVerticalRunLength = new MTBDoubleData(calcAvgVerticalRunLength(image));
		}
		if(calcAvgDiffusionDistance)
		{
			avgDiffusionDist = new MTBDoubleData(calcAverageDiffusionDistance(distImg));
		}
		if(calcMaxDiffusionDistance)
		{
			maxDiffusionDist = new MTBDoubleData(calcMaximumDiffusionDistance(distImg));
		}
		if(calcDilationAreas)
		{
			dilationAreas = calcDilationAreas(image, minDilMaskSize, maxDilMaskSize);
			
			for(int i = 0; i < dilationAreas.size(); i++)
			{
				System.out.print(dilationAreas.get(i) + ", ");
			}
		}
		
		resultsTable = makeTable();
	}
	
	
	/**
	 * 
	 * @param img binary input image
	 * @return areal porosity of the input image
	 */
	private double calcArealPorosity(MTBImage img)
	{
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double sumPixels = sizeX * sizeY;
		double zeroPixels = 0;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(img.getValueInt(x, y) == 0)
				{
					zeroPixels++;
				}
			}
		}
		
		
		return zeroPixels / sumPixels;
	}
	
	
	/**
	 * 
	 * @param img binary input image
	 * @return average length of horizontal foreground runs (i.e., consecutive pixels in a row with value != 0) 
	 */
	private double calcAvgHorizontalRunLength(MTBImage img)
	{
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double numHorizontalRuns = 0;	// counter for horizontal runs (consecutive parts of horizontal foreground pixel)
		double sumHorizontalRunLengths = 0;	// sum of the lengths of all horizontal runs
		
		for(int y = 0; y < sizeY; y++)
		{
			boolean inRun = false;
			int length = 0;
			
			for(int x = 0; x < sizeX; x++)
			{
				if(img.getValueInt(x, y) != 0)	// current pixel belongs to foreground (cell cluster)
				{
					if(inRun)	// pixel belongs to an already discovered run
					{
						length++;
					}
					else		// pixel belongs to a new run
					{
						inRun = true;
						length = 1;
						numHorizontalRuns++;
					}
				}
				else	// current pixel belongs to background (interstitial space)
				{
					if(inRun)	// run is finished
					{
						inRun = false;
						sumHorizontalRunLengths += length;
						length = 0;
					}
				}
			}
		}
						
		return sumHorizontalRunLengths / numHorizontalRuns;
	}
	
	
	/**
	 * 
	 * @param img binary input image
	 * @return average length of vertical foreground runs (i.e., consecutive pixels in a column with value != 0) 
	 */
	private double calcAvgVerticalRunLength(MTBImage img)
	{
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double numVerticalRuns = 0;	// counter for vertical runs (consecutive parts of vertical foreground pixel)
		double sumVerticalRunLengths = 0;	// sum of the lengths of all vertical runs
		
		for(int x = 0; x < sizeX; x++)
		{
			int length = 0;
			boolean inRun = false;
			
			for(int y = 0; y < sizeY; y++)
			{
				if(img.getValueInt(x, y) != 0)	// current pixel belongs to foreground (cell cluster)
				{
					if(inRun)	// pixel belongs to an already discovered run
					{
						length++;
					}
					else		// pixel belongs to a new run
					{
						inRun = true;
						length = 1;
						numVerticalRuns++;
					}
				}
				else	// current pixel belongs to background (interstitial space)
				{
					if(inRun)	// run is finished
					{
						inRun = false;
						sumVerticalRunLengths += length;
						length = 0;
					}
				}
			}
		}
						
		return sumVerticalRunLengths / numVerticalRuns;
	}
	
	/**
	 * 
	 * @param img binary input image
	 * @return distance transform (euclidean) of the input image
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private MTBImage getDistanceImage(MTBImage img) throws ALDOperatorException, ALDProcessingDAGException
	{
		DistanceTransform distTransform = new DistanceTransform();
		distTransform.setInImg((MTBImageByte)img);
		distTransform.setDistMetric(DistanceTransform.DistanceMetric.EUCLIDEAN);
		distTransform.setForeground(DistanceTransform.ForegroundColor.FG_BLACK);
		
		distTransform.runOp();
		MTBImage distImg = distTransform.getDistanceImage();
		
		return distImg;
	}
	
	
	/**
	 * 
	 * @param distImg distance transform image
	 * @return average diffusion distance 
	 */
	private double calcAverageDiffusionDistance(MTBImage distImg)
	{
		int sizeX = distImg.getSizeX();
		int sizeY = distImg.getSizeY();
		
		double sumDistances = 0;
		double numFGPixels = 0;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				double val = distImg.getValueDouble(x, y);
				
				if(val != 0)
				{
					sumDistances += val;
					numFGPixels++;
				}
			}
		}
		
		return sumDistances / numFGPixels;
	}
	
	
	/**
	 * 
	 * @param distImg distance transform image
	 * @return maximum diffusion image
	 */
	private double calcMaximumDiffusionDistance(MTBImage distImg)
	{
		int sizeX = distImg.getSizeX();
		int sizeY = distImg.getSizeY();
		
		double maxDist = 0;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				double val = distImg.getValueDouble(x, y);
				
				if(val > maxDist)
				{
					maxDist = val;
				}
			}
		}
		
		return maxDist;
	}
	
	
	/**
	 * 
	 * @param img binary input image
	 * @param minSize minimum diameter of the circular dilation mask 
	 * @param maxSize maximum diameter of the circular dilation mask 
	 * @return dilation areas for the used dilation mask diameters
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	/*private MTBIntegerData[] calcDilationAreas(MTBImage img, int minSize, int maxSize) throws ALDOperatorException, ALDProcessingDAGException
	{
		BasicMorphology morphology = new BasicMorphology();	// used for consecutively dilating the image with increasing mask sizes
		morphology.setInImg(img);
		morphology.setMode(BasicMorphology.opMode.DILATE);
		
		MTBIntegerData[] dilArea = new MTBIntegerData[(maxSize - minSize)/2 + 1];
		int index = 0;
		
		for(int i = minSize; i <= maxSize; i+=2)
		{
			double r = (double)i/2;
			
			System.out.println("r: " + r);
			
			// dilate image
			morphology.setMask(BasicMorphology.maskShape.CIRCLE, i);
			morphology.runOp();
			//MTBImage dilImg = morphology.getResultImage();
			
			MTBImage dilImg = img;
			
			// extract boundary of foreground by eroding the image and subtract this from the previous image
			BasicMorphology morphology2 = new BasicMorphology();
			morphology2.setInImg(dilImg);
			morphology2.setMode(BasicMorphology.opMode.ERODE);
			morphology2.setMask(BasicMorphology.maskShape.SQUARE, 3);
			morphology2.runOp();
			MTBImage erImg = morphology2.getResultImage();
			
			//erImg.show();
			
			MTBImageArithmetics arithmetics = new MTBImageArithmetics();
			MTBImage subImg = arithmetics.sub(dilImg, erImg);
			
			MTBImageByte borderImg = (MTBImageByte)subImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true);
			
			//borderImg.show();
			
			// apply distance transform to get distances to border pixels
			DistanceTransform distTransform = new DistanceTransform();
			distTransform.setInImg(borderImg);
			distTransform.setDistMetric(DistanceTransform.DistanceMetric.EUCLIDEAN);
			distTransform.setForeground(DistanceTransform.ForegroundColor.FG_WHITE);
			distTransform.runOp();
			MTBImage distImg = distTransform.getDistanceImage();
			
			distImg.show();
			
			// count the number of pixels with distance < r (radius of dilating mask)
			int sizeX = distImg.getSizeX();
			int sizeY = distImg.getSizeY();
			
			int sum = 0;
			
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					if(distImg.getValueDouble(x, y) < r)
					{
						sum++;
					}
				}
			}
			
			dilArea[index] = new MTBIntegerData(sum);
			
			index++;
		}
		
		
		return dilArea; 
	}*/
	
	/**
	 * 
	 * @param img binary input image
	 * @param minSize minimum diameter of the circular dilation mask 
	 * @param maxSize maximum diameter of the circular dilation mask 
	 * @return dilation areas for the used dilation mask diameters
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private Vector<MTBIntegerData> calcDilationAreas(MTBImage img, int minSize, int maxSize) throws ALDOperatorException, ALDProcessingDAGException
	{
		//BasicMorphology morphology = new BasicMorphology();	// used for consecutively dilating the image with increasing mask sizes
		//morphology.setInImg(img);
		//morphology.setMode(BasicMorphology.opMode.DILATE);
		
		Vector<MTBIntegerData> dilArea = new Vector<MTBIntegerData>();
		
		for(int i = minSize; i <= maxSize; i+=2)
		{
			double r = (double)i/2;
			
			//System.out.println("r: " + r);
			
			// dilate image
			//morphology.setMask(BasicMorphology.maskShape.CIRCLE, i);
			//morphology.runOp();
			//MTBImage dilImg = morphology.getResultImage();
			
			MTBImage dilImg = img;
			
			// extract boundary of foreground by eroding the image and subtract this from the previous image
			BasicMorphology morphology2 = new BasicMorphology();
			morphology2.setInImg(dilImg);
			morphology2.setMode(BasicMorphology.opMode.ERODE);
			morphology2.setMask(BasicMorphology.maskShape.SQUARE, 3);
			morphology2.runOp();
			MTBImage erImg = morphology2.getResultImage();
			
			//erImg.show();
			
			MTBImageArithmetics arithmetics = new MTBImageArithmetics();
			MTBImage subImg = arithmetics.sub(dilImg, erImg);
			
			MTBImageByte borderImg = (MTBImageByte)subImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true);
			
			//borderImg.show();
			
			// apply distance transform to get distances to border pixels
			DistanceTransform distTransform = new DistanceTransform();
			distTransform.setInImg(borderImg);
			distTransform.setDistMetric(DistanceTransform.DistanceMetric.EUCLIDEAN);
			distTransform.setForeground(DistanceTransform.ForegroundColor.FG_WHITE);
			distTransform.runOp();
			MTBImage distImg = distTransform.getDistanceImage();
			
			//distImg.show();
			
			// count the number of pixels with distance < r (radius of dilating mask)
			int sizeX = distImg.getSizeX();
			int sizeY = distImg.getSizeY();
			
			int sum = 0;
			
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					if(distImg.getValueDouble(x, y) < r)
					{
						sum++;
					}
				}
			}
			
			dilArea.add(new MTBIntegerData(sum));
			
		}
		
		
		return dilArea; 
	}
	
	private MTBTableModel makeTable()
	{
		// display options
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(this.outDigits);
		
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("image");
		header.add("areal porosity");
		header.add("avg. horizontal run length");
		header.add("avg. vertical run length");
		header.add("avg. diffusion distance");
		header.add("max. diffusion distance");
		
		for(int s = this.minDilMaskSize; s <= this.maxDilMaskSize; s+=2)
		{
			header.add("dilation area (r = " + (double)s/2 + ")");
		}
		
		MTBTableModel table = new MTBTableModel(1, header.size(), header);
		
		// insert values into results table
		table.setValueAt(this.inImg.getTitle(), 0, 0);	// file name
		table.setValueAt(nf.format(this.arealPorosity.getValue()), 0, 1);
		table.setValueAt(nf.format(this.avgHorizontalRunLength.getValue()), 0, 2);
		table.setValueAt(nf.format(this.avgVerticalRunLength.getValue()), 0, 3);
		table.setValueAt(nf.format(this.avgDiffusionDist.getValue()), 0, 4);
		table.setValueAt(nf.format(this.maxDiffusionDist.getValue()), 0, 5);
		
		for(int i = 0; i < this.dilationAreas.size(); i++)
		{
			table.setValueAt(nf.format(this.dilationAreas.elementAt(i).getValue()), 0, i + 6);
		}
		
		return table;
	}
}
