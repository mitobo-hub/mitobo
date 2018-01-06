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

import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * Base class representing results of {@link MTB_CellCounter} 
 * presegmentation stage(s).
 *
 * @author Birgit Moeller
 */
public abstract class CellCntrSegResult {

	/**
	 * Reference to the image from which the segmentation data originates.
	 */
	protected MTBImage image;

	/**
   * Set of pre-segmented borders.
   */
  protected MTBBorder2DSet detectedBorders = null;

  /**
   * Vector tagging regions as active or not during presegmentation phase.
   */
  protected Vector<Boolean> activityArray = null;

  /**
   * Default constructor.
   * @param img
   */
  protected CellCntrSegResult(MTBImage img) {
  	this.image = img;
  }

	/**
	 * Remove an item from the set.
	 * @param n		Index of data item to remove.
	 */
	public void removeItem(int n) {
		if (this.activityArray != null)
			this.activityArray.remove(n);
		if (this.detectedBorders != null)
			this.detectedBorders.removeElementAt(n);
	}
	
	/**
	 * Removes the last data item.
	 */
	public void removeLastItem(){
		if (this.activityArray != null && this.activityArray.size() > 0)
			this.activityArray.removeElementAt(this.activityArray.size()-1);
		if (this.detectedBorders != null)
			this.detectedBorders.removeElementAt(this.detectedBorders.size()-1);
	}

	/**
	 * Clears all data.
	 */
	public void clearData() {
		this.activityArray = new Vector<Boolean>();
		this.detectedBorders = null;
	}
	
  /**
   * Get set of region borders.
   * @return	Set of borders.
   */
  public MTBBorder2DSet getBorders() {
  	return this.detectedBorders;
  }

  /**
   * Get number of borders in set.
   * @return	Number of borders.
   */
  public int getBorderCount() {
  	return this.detectedBorders.size();
  }
  
  /**
   * Get reference to activity array.
   * @return	Activity array.
   */
  public Vector<Boolean> getActivityArray() {
  	return this.activityArray;
  }
}
