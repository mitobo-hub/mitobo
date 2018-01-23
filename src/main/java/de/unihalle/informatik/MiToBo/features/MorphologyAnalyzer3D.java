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

package de.unihalle.informatik.MiToBo.features;


import java.text.NumberFormat;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBSurface3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;

/**
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.STANDARD, allowBatchMode = true)
public class MorphologyAnalyzer3D extends MTBOperator
{	
	@Parameter(label = "3D-label image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "3D-label image", dataIOOrder = 0,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient MTBImage labelImg = null;
	
	//@Parameter(label = "3D-region set", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "3D-region set", dataIOOrder = 1)
	private MTBRegion3DSet regions = null;	// input regions
	
	//@Parameter(label = "3D-surface set", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "3D-surface set", dataIOOrder = 2)
	private MTBSurface3DSet surfaces = null; // surfaces of input regions
	
	//analysis parameters
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 3)
	private Double deltaX = 1.0;
	
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 4)
	private Double deltaY = 1.0;
	
	@Parameter(label = "pixel length, z-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in z-direction", dataIOOrder = 5)
	private Double deltaZ = 1.0;
	
	@Parameter(label = "unit x/y/z", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of spatial dimensions", dataIOOrder = 6)
	private String unitXYZ = "pixel";
	
	// which features should be calculated
	@Parameter(label = "calculate volume", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's areas be calculated", dataIOOrder = 7)
	private boolean calcVolume = true;
	
	@Parameter(label = "calculate compactness", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's areas be calculated", dataIOOrder = 8)
	private boolean calcCompactness = true;
	
	@Parameter(label = "calculate surface area", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's areas be calculated", dataIOOrder = 9)
	private boolean calcSurfArea = true;
	
	@Parameter(label = "fractional digits", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "fractional digits", dataIOOrder = 10, mode=ExpertMode.ADVANCED)
	private Integer fracDigits = 3;
	
	// output parameter
	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "results table", dataIOOrder = 0)
	private MTBTableModel table = null;
	
	private NumberFormat nf = NumberFormat.getInstance();
	
	private int bgLabel = 0;	// label value for the background
	
	Vector<Integer> labels;
	Vector<Double> volumes;
	Vector<Double> compactnesses;
	Vector<Double> surfaceAreas;
	
	
	public MorphologyAnalyzer3D() throws ALDOperatorException
	{	
		
	}
	
	public MorphologyAnalyzer3D(MTBImage labelImg) throws ALDOperatorException
	{	
		this.labelImg = labelImg;
	}
	
	public MorphologyAnalyzer3D(MTBRegion3DSet regions, MTBSurface3DSet surfaces) throws ALDOperatorException
	{	
		this.regions = regions;
		this.surfaces = surfaces;
		
	}
	
	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(fracDigits);
		nf.setMaximumFractionDigits(fracDigits);
		
		if(regions == null || surfaces == null)
		{
			if(labelImg != null)
			{
				regions = LabelAreasToRegions.getRegions3D(labelImg, bgLabel);
				
				SurfaceExtraction areaExtractor = new SurfaceExtraction(labelImg, false);
				areaExtractor.runOp();
				surfaces = areaExtractor.getSurfaces();
			}
			else	// TODO: use validateCustom()
			{
				System.err.println(this.toString() + ": No input specified");
			}
			
		}
		
		getShapeFeatures();
		
		makeTable();
	}
	
	

	
	private void getShapeFeatures()
	{
		double factor = deltaX * deltaY * deltaZ;
		
		labels = new Vector<Integer>();
		volumes = new Vector<Double>();
		compactnesses = new Vector<Double>();
		surfaceAreas = new Vector<Double>();
		
		for(int i = 0; i < regions.size(); i++)
		{
			MTBRegion3D cr = regions.elementAt(i);
			int cid = cr.getID();
			labels.add(cid);
				
			// volume
			if(calcVolume)
			{
				double v = cr.getVolume() * factor;
				volumes.add(v);
			}
			
			// compactness
			if(calcCompactness)
			{
				double c = cr.getCompactness();
				compactnesses.add(c);
			}
			
			// surfaceArea
			if(calcSurfArea)
			{
				for(int j = 0; j < surfaces.size(); j++)
				{
					if(surfaces.elementAt(j).getID() == cid)
					{
						double s = surfaces.elementAt(j).getArea();	//TODO: scale area properly

						surfaceAreas.add(s);
						break;
					}
				}
			}
			
			
			// TODO: maybe remove region to speed up following searches
		}
	}
	
	
	/**
	 * create result table
	 */
	private void makeTable()
	{
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("object");
		
		if(calcVolume)
		{
			header.add("volume ("+ unitXYZ + "^3)");
		}
		if(calcCompactness)
		{
			header.add("compactness ("+ unitXYZ + ")");
		}
		if(calcSurfArea)
		{
			header.add("surface area (pixels)");
		}
		
		int n = regions.size();
		
		table = new MTBTableModel(n, header.size(), header);
		
		for(int i = 0; i < n; i++)
		{
			int col = 1;
			
			table.setValueAt(labels.elementAt(i), i, 0);
			
			if(calcVolume)
			{
				table.setValueAt(nf.format(volumes.elementAt(i)), i, col);
				col++;
			}
			if(calcCompactness)
			{
				table.setValueAt(nf.format(compactnesses.elementAt(i)), i, col);
				col++;
			}
			if(calcSurfArea)
			{
				table.setValueAt(nf.format(surfaceAreas.elementAt(i)), i, col);
				col++;
			}
		}
	}
	
	
	/**
	 * @return the labelImg
	 */
	public MTBImage getLabelImg()
	{
		return labelImg;
	}

	/**
	 * @param labelImg the labelImg to set
	 */
	public void setLabelImg(MTBImage labelImg)
	{
		this.labelImg = labelImg;
	}

	/**
	 * @return the deltaX
	 */
	public Double getDeltaX()
	{
		return deltaX;
	}

	/**
	 * @param deltaX the deltaX to set
	 */
	public void setDeltaX(Double deltaX)
	{
		this.deltaX = deltaX;
	}

	/**
	 * @return the deltaY
	 */
	public Double getDeltaY()
	{
		return deltaY;
	}

	/**
	 * @param deltaY the deltaY to set
	 */
	public void setDeltaY(Double deltaY)
	{
		this.deltaY = deltaY;
	}

	/**
	 * @return the deltaZ
	 */
	public Double getDeltaZ()
	{
		return deltaZ;
	}

	/**
	 * @param deltaZ the deltaZ to set
	 */
	public void setDeltaZ(Double deltaZ)
	{
		this.deltaZ = deltaZ;
	}

	/**
	 * @return the unitXYZ
	 */
	public String getUnitXYZ()
	{
		return unitXYZ;
	}

	/**
	 * @param unitXYZ the unitXYZ to set
	 */
	public void setUnitXYZ(String unitXYZ)
	{
		this.unitXYZ = unitXYZ;
	}

	/**
	 * @return are volumes to be calculated
	 */
	public boolean calcVolume()
	{
		return calcVolume;
	}

	/**
	 * @param calcVolume should volumes be calculated
	 */
	public void setCalcVolume(boolean calcVolume)
	{
		this.calcVolume = calcVolume;
	}

	/**
	 * @return are compactnesses to be calculated
	 */
	public boolean calcCompactness()
	{
		return calcCompactness;
	}

	/**
	 * @param calcCompactness should compactnesses be calculated
	 */
	public void setCalcCompactness(boolean calcCompactness)
	{
		this.calcCompactness = calcCompactness;
	}

	/**
	 * @return are surface areas to be calculated
	 */
	public boolean calcSurfArea()
	{
		return calcSurfArea;
	}

	/**
	 * @param calcSurfArea should surface areas be calculated
	 */
	public void setCalcSurfArea(boolean calcSurfArea)
	{
		this.calcSurfArea = calcSurfArea;
	}
	
	
	/**
	 * specify the number of fractional digits for the results table
	 * 
	 * @param digits
	 */
	public void setFractionalDigits(int digits)
	{
		this.fracDigits = digits;
	}
	

	/**
	 * @return the table
	 */
	public MTBTableModel getTable()
	{
		return table;
	}

	@SuppressWarnings("unused")
	private void getCalibration()
	{
//		try
//		{
			if(this.labelImg != null)
			{
				this.deltaX = labelImg.getCalibration().pixelWidth;
				this.deltaY = labelImg.getCalibration().pixelHeight;
				this.deltaZ = labelImg.getCalibration().pixelDepth;
				this.unitXYZ = labelImg.getCalibration().getXUnit();
				
//				if(this.hasParameter("regions"))
//				{
//					this.removeParameter("regions");
//				}
//				if(this.hasParameter("surfaces"))
//				{
//					this.removeParameter("surfaces");
//				}
			}
//			else
//			{
//				if(!this.hasParameter("regions"))
//				{
//					this.addParameter("regions");
//				}
//				if(!this.hasParameter("surfaces"))
//				{
//					this.addParameter("surfaces");
//				}
//			}
//		}
//		catch(ALDOperatorException e)
//		{
//			e.printStackTrace();
//		}
//		catch(ALDWorkflowException e)
//		{
//			e.printStackTrace();
//		}
		
	}
}
