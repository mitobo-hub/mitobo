/*
 * Copyright (C) 2010 - @YEAR@ by the MiToBo development team
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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes;

import ij.IJ;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents.BorderType;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Class representing region-based results of {@link MTB_CellCounter} 
 * presegmentation stage(s).
 *
 * @author Birgit Moeller
 */
public class CellCntrSegResultRegions 
	extends CellCntrSegResult {

	/**
   * Set of pre-segmented regions.
   */
  private MTBRegion2DSet detectedRegions = null;

  /**
   * Vector of average region intensities.
   */
  private Vector<Double> averageRegionIntensities = null;
  
  /**
   * Constructor.
   * @param img								Image on which regions were detected.
   * @param regs							Extracted regions.
   * @param borders						Corresponding borders of regions.
   * @param avgIntensities		Average intensities of regions.
   */
  public CellCntrSegResultRegions(MTBImage img, MTBRegion2DSet regs,
  		MTBBorder2DSet borders, Vector<Double> avgIntensities) {
  	super(img);
  	this.detectedRegions = regs;
  	this.detectedBorders = borders;
  	this.averageRegionIntensities = avgIntensities;
		this.activityArray = new Vector<Boolean>();
		for (int i=0; i<this.detectedRegions.size(); ++i)
			this.activityArray.add(new Boolean(true));
  }
  
  /**
   * Constructor.
   * <p>
   * The constructor automatically extracts the borders for the given 
   * set of regions and calculates the average intensities of all 
   * regions.
   * 
   * @param img			Image on which regions were detected.
   * @param regs		Extracted regions.
   */
	public CellCntrSegResultRegions(MTBImage img,
			MTBRegion2DSet regs) {
		super(img);
		this.detectedRegions = regs;
		this.detectedBorders = this.extractRegionBorders();
		if (this.detectedBorders == null) {
			// something went wrong during border extraction...
			IJ.error("Border extraction on detected regions failed!");
			this.detectedRegions = null;
			this.averageRegionIntensities = null;
			this.activityArray = null;
		}
		else {
			this.averageRegionIntensities = 
					this.calcRegionAverageIntensities();	
			this.activityArray = new Vector<Boolean>();
			for (int i=0; i<this.detectedRegions.size(); ++i)
				this.activityArray.add(new Boolean(true));
		}
	}
	
	/**
	 * Calculates the average region intensities.
	 * @return		Vector of region intensities.
	 */
	private Vector<Double> calcRegionAverageIntensities() {
		
		Vector<Double> avgIntensities = new Vector<Double>();

		MTBRegion2D reg;
		double intensitySum = 0;
		Vector<Point2D.Double> points;
		for (int i=0; i<this.detectedRegions.size(); ++i) {
			reg = this.detectedRegions.elementAt(i);
			intensitySum = 0;
			points = reg.getPoints();
			for (Point2D.Double p: points) {
				intensitySum += this.image.getValueDouble((int)p.x, (int)p.y);
			}
			avgIntensities.add(new Double(intensitySum / points.size()));
		}
		return avgIntensities;
	}

	/**
	 * Extract borders of detected regions.
	 * <p>
	 * If the border extraction fails, the border set is null.
	 * @return	Set of region borders.
	 */
	private MTBBorder2DSet extractRegionBorders() {
		try {
			// draw region set into an image and run border extraction
			DrawRegion2DSet drawOp = new DrawRegion2DSet();
			drawOp.setGrayValue(new Double(255.0));
			drawOp.setDrawType(DrawType.MASK_IMAGE);
			drawOp.setImageType(MTBImageType.MTB_BYTE);
			drawOp.setInputRegions(this.detectedRegions);
			drawOp.runOp(HidingMode.HIDDEN);
			MTBImageByte mask = (MTBImageByte)drawOp.getResultImage();
			BordersOnLabeledComponents borderOp = 
					new BordersOnLabeledComponents();
			borderOp.setInputImage(mask);
			borderOp.setBorderType(BorderType.OUTER_BORDERS);
			borderOp.runOp(HidingMode.HIDDEN);
			return borderOp.getResultBorders();
		} catch (ALDException e) {
			return null;
		}
	}

	/**
	 * Remove an item from the set.
	 * @param n		Index of data item to remove.
	 */
	@Override
  public void removeItem(int n) {
		// index out of range, do nothing
		if (n >= this.detectedRegions.size())
			return;
		if (this.averageRegionIntensities != null)
			this.averageRegionIntensities.remove(n);
		if (this.detectedRegions != null)
			this.detectedRegions.removeElementAt(n);
		super.removeItem(n);
	}
	
	/**
	 * Removes the last data item.
	 */
	@Override
  public void removeLastItem(){
		// no data, do nothing
		if (this.detectedRegions.size() == 0)
			return;
		if (this.averageRegionIntensities != null)
			this.averageRegionIntensities.removeElementAt(
				this.averageRegionIntensities.size()-1);
		if (this.detectedRegions != null)
			this.detectedRegions.removeElementAt(this.detectedRegions.size()-1);
		super.removeLastItem();
	}

	/**
	 * Clears all data.
	 */
	@Override
  public void clearData() {
		super.clearData();
		this.averageRegionIntensities = new Vector<Double>();
		this.detectedRegions = null;
	}
	
	/**
	 * Checks region sizes and average intensities against specified 
	 * intervals.
	 * 
	 * @param minSize				Minimal size of valid regions. 
	 * @param maxSize 			Maximal size of valid regions.
	 * @param minIntensity 	Minimal average intensity of valid regions.
	 * @param maxIntensity 	Maximal average intensity of valid regions.
	 */
	public void filterRegions(int minSize, int maxSize, 
			int minIntensity, int maxIntensity) {
		int regionCount = this.detectedRegions.size();
		for (int i=0; i<regionCount; ++i) {
			this.activityArray.setElementAt(new Boolean(true), i);
			if (  this.detectedRegions.elementAt(i).getArea() < minSize 
					|| this.detectedRegions.elementAt(i).getArea() > maxSize ){
				this.activityArray.setElementAt(new Boolean(false), i);		
			}
			else if (	 this.averageRegionIntensities.elementAt(i).doubleValue() 
										< minIntensity
					    || this.averageRegionIntensities.elementAt(i).doubleValue() 
										> maxIntensity) {
							this.activityArray.setElementAt(new Boolean(false), i);
			}	
		}
	}

  /**
   * Get set of segmented regions.
   * @return	Set of regions.
   */
  public MTBRegion2DSet getRegions() {
  	return this.detectedRegions;
  }
  
  /**
   * Get number of regions in set.
   * @return	Number of regions.
   */
  public int getRegionCount() {
  	return this.detectedRegions.size();
  }
  
  /**
   * Get average intensities of regions.
   * @return	Vector with average intensities of regions.
   */
  public Vector<Double> getAverageIntensities() {
  	return this.averageRegionIntensities;
  }
}
