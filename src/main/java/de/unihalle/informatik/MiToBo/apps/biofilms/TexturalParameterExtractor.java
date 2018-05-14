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
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * operator for calculating textural parameters from biofilm images as described in
 * Yang et al., Quantifying biofilm structure using image analysis.
 * Journal of Microbiological Methods 39 (2000) 109–119.
 * 
 * input image should be a 8-bit gray scale image
 * 
 * @author glass
 * 
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION)
public class TexturalParameterExtractor extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image (should be 8 bit gray scale)", dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "calculate textural entropy", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the textural entropy of the image be calculated", dataIOOrder = 3)
	private boolean calcEntropy = true;
	
	@Parameter(label = "calculate angular second moment", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the angular second moment of the image be calculated", dataIOOrder = 4)
	private boolean calcASM = true;
	
	@Parameter(label = "calculate inverse difference moment", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the inverse difference moment of the image be calculated", dataIOOrder = 4)
	private boolean calcIDM = true;
	
	@Parameter(label = "result value digits", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "number of digits for output values", mode=ExpertMode.ADVANCED, dataIOOrder = 9)
	private int outDigits = 3;
	
	//---------------------------------------- output parameters ----------------------------------------
	//@Parameter(label = "textural entropy", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "textural entropy", dataIOOrder = 0)
	private MTBDoubleData texturalEntropy = null;
	
	//@Parameter(label = "angular second moment", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "angular second moment", dataIOOrder = 1)
	private MTBDoubleData angularSecondMoment = null;
	
	//@Parameter(label = "inverse difference moment", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "inverse difference moment", dataIOOrder = 2)
	private MTBDoubleData inverseDifferenceMoment = null;
	
	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "table containing the resulting values")
	private MTBTableModel resultsTable = null;
	

	public TexturalParameterExtractor() throws ALDOperatorException
	{
		super();
	}
	
	
	public TexturalParameterExtractor(MTBImage inImg) throws ALDOperatorException
	{
		this.inImg = inImg;
	}

	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		// initialize (reset) all output values
		this.texturalEntropy = new MTBDoubleData(-1.0);
		this.angularSecondMoment = new MTBDoubleData(-1.0);
		this.inverseDifferenceMoment = new MTBDoubleData(-1.0);
		
		// convert input image to 8 bit
		MTBImage image = inImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true);
		
		//MTBImage image = inImg;
		
		Double[][] P_n = calcNormSpatialDependenceMatrix(image);
		//printMatrix(P_n);
		
		if(calcEntropy)
		{
			texturalEntropy = new MTBDoubleData(calcTexturalEntropy(P_n));
		}
		
		if(calcASM)
		{
			angularSecondMoment = new MTBDoubleData(calcAngularSecondMoment(P_n));
		}
		
		if(calcIDM)
		{
			inverseDifferenceMoment = new MTBDoubleData(calcInverseDifferenceMoment(P_n));
		}
		
		resultsTable = makeTable();
	}

	
	// ------------------------------ measurement functions ------------------------------
	/**
	 * 
	 * @param image 8 bit grayscale image
	 * @return normalized spatial dependence matrix
	 */
	private Double[][] calcNormSpatialDependenceMatrix(MTBImage image)
	{
		int sizeX = image.getSizeX();
		int sizeY = image.getSizeY();
		
		int[][] P_h = new int[256][256];	// horizontal spatial dependence matrix
		int[][] P_v = new int[256][256];	// vertical spatial dependence matrix
		int[][] P_hv = new int[256][256];	// sum of horizontal and vertical spatial dependence matrix
		Double[][] P_n = new Double[256][256];	// normalized sum of horizontal and vertical spatial dependence matrix
		
		int sum = 0;
		
		// calculate horizontal spatial dependencies (i.e., count transitions from gray value s to gray value r between horizontally adjacent pixels)
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX-1; x++)
			{
				int p = image.getValueInt(x, y);
				int q = image.getValueInt(x+1, y);
				
				P_h[p][q]++;
				P_h[q][p]++;
			}
		}
		
		// calculate vertical spatial dependencies (i.e., count transitions from gray value s to gray value r between vertically adjacent pixels)
		for(int y = 0; y < sizeY-1; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				int p = image.getValueInt(x, y);
				int q = image.getValueInt(x, y+1);
				
				P_v[p][q]++;
				P_v[q][p]++;
			}
		}
		
		// calculate sum of horizontal and vertical dependence matrix and normalization factor
		for(int i = 0; i < 256; i++)
		{
			for(int j = 0; j < 256; j++)
			{
				int val = P_h[i][j] + P_v[i][j];
				
				P_hv[i][j] = val;
				sum += val;
			}
		}
		
		System.out.println(sum);
		
		// normalize matrix by dividing each entry by the sum of all entries
		for(int i = 0; i < 256; i++)
		{
			for(int j = 0; j < 256; j++)
			{
				P_n[i][j] = (double)(P_hv[i][j]) / sum;
			}
		}
		
		return P_n;
	}
	
	
	/**
	 * 
	 * @param P normalized spatial dependence matrix
	 * @return textural entropy of the input image
	 */
	private double calcTexturalEntropy(Double[][] P)
	{
		double te = 0;
		int n = P.length;
		int m = P[0].length;
		
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if(P[i][j] != 0.0)
				{
					double val = P[i][j];
					te -= val * Math.log(val);
				}
			}
		}
		
		return te;
	}
	
	/**
	 * 
	 * @param P normalized spatial dependence matrix
	 * @return angular second moment of the input image
	 */
	private double calcAngularSecondMoment(Double[][] P)
	{
		double asm = 0;
		
		int n = P.length;
		int m = P[0].length;
		
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if(P[i][j] != 0.0)
				{
					double val = P[i][j];
					asm += val * val;
				}
			}
		}
		
		return asm;
	}
	
	/**
	 * 
	 * @param P normalized spatial dependence matrix
	 * @return inverse difference moment of the input image
	 */
	private double calcInverseDifferenceMoment(Double[][] P)
	{
		double idm = 0;
		
		int n = P.length;
		int m = P[0].length;
		
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if(P[i][j] != 0.0)
				{
					double val = P[i][j];
					idm += (val / (1 + (i + j) * (i + j)));
				}
			}
		}
		
		return idm;
	}
	
	
	private MTBTableModel makeTable()
	{
		// display options
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(this.outDigits);
		
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("image");
		header.add("texural entropy");
		header.add("angular second moment");
		header.add("inverse difference moment");
		
		MTBTableModel table = new MTBTableModel(1, header.size(), header);
		
		// insert values into results table
		table.setValueAt(this.inImg.getTitle(), 0, 0);	// file name
		table.setValueAt(nf.format(this.texturalEntropy.getValue()), 0, 1);
		table.setValueAt(nf.format(this.angularSecondMoment.getValue()), 0, 2);
		table.setValueAt(nf.format(this.inverseDifferenceMoment.getValue()), 0, 3);
		
		return table;
	}
	
	// ------------------------------ auxiliary functions ------------------------------
	
	private void printMatrix(Double[][] matrix)
	{
		int n = matrix.length;
		int m = matrix[0].length;
		
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				System.out.print(matrix[i][j] + " ");
				if(j == m-1)
				{
					System.out.println();
				}
			}
		}
	}
}
