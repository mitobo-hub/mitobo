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

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * Class representing results of {@link MTB_CellCounter} 
 * presegmentation stage(s).
 *
 * @author Birgit Moeller
 */
public class CellCntrSegResultCurves 
	extends CellCntrSegResult {

	/**
   * Set of pre-segmented regions.
   */
  private Vector<MTBQuadraticCurve2D> detectedStomata = null;

  /**
   * Constructor.
   * @param img								Image on which regions were detected.
   * @param regs							Extracted curves.
   */
  public CellCntrSegResultCurves(MTBImage img, 
  		Vector<MTBQuadraticCurve2D> regs) {
  	super(img);
  	this.detectedStomata = regs;
		this.activityArray = new Vector<Boolean>();
		for (int i=0; i<this.detectedStomata.size(); ++i)
			this.activityArray.add(new Boolean(true));
		this.detectedBorders = this.extractRegionBorders();
  }
  
	/**
	 * Extract borders of detected ellipses.
	 * @return	Set of region borders.
	 */
	private MTBBorder2DSet extractRegionBorders() {
		double xmax= 0, ymax= 0;
		Vector<MTBBorder2D> borderList = new Vector<MTBBorder2D>();
		for (MTBQuadraticCurve2D c: this.detectedStomata) {
			// get ellipse parameters
			double major = c.getSemiLengthAxisA();
			double minor = c.getSemiLengthAxisB();
			double xCenter = c.getCenterX();
			double yCenter = c.getCenterY();
			double theta = c.getOrientation();

			Vector<Point2D.Double> bps = new Vector<Point2D.Double>();
			
			// convert angle from degrees to radiant
			double trad = Math.PI/180.0*theta;
			for (int i=0;i<360; ++i) {
				double rad = Math.PI/180.0*i;
				double x = major * Math.cos(rad);
				double y = minor * Math.sin(rad);
				// rotate ellipse
				int rx = (int)(Math.cos(trad)*x - Math.sin(trad)*y + xCenter);
				int ry = (int)(Math.sin(trad)*x + Math.cos(trad)*y + yCenter);
				bps.add(new Point2D.Double(rx,ry));
				
				if (rx >= xmax)
					xmax = rx;
				if (ry >= ymax)
					ymax = ry;
			}
			borderList.add(new MTBBorder2D(bps, BorderConnectivity.CONNECTED_8));
		}
		MTBBorder2DSet borders = new MTBBorder2DSet(0,0,xmax,ymax);
		for (MTBBorder2D b: borderList)
			borders.add(b);
		return borders;
	}

	/**
	 * Remove an item from the set.
	 * @param n		Index of data item to remove.
	 */
	@Override
  public void removeItem(int n) {
		// index out of range, do nothing
		if (n >= this.detectedStomata.size())
			return;
		if (this.detectedStomata != null)
			this.detectedStomata.removeElementAt(n);
		super.removeItem(n);
	}
	
	/**
	 * Removes the last data item.
	 */
	@Override
  public void removeLastItem(){
		// no data, do nothing
		if (this.detectedStomata.size() == 0)
			return;
		if (this.detectedStomata != null)
			this.detectedStomata.removeElementAt(this.detectedStomata.size()-1);
		super.removeLastItem();
	}

	/**
	 * Clears all data.
	 */
	@Override
  public void clearData() {
		super.clearData();
		this.detectedStomata = null;
	}
	
  /**
   * Get number of stomata regions in set.
   * @return	Number of regions.
   */
  public int getStomataCount() {
  	return this.detectedStomata.size();
  }
}
