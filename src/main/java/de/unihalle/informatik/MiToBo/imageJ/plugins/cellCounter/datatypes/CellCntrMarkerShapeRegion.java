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

import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;

/**
 * Class using a region as shape for markers.
 *
 * @author Birgit Moeller
 */
public class CellCntrMarkerShapeRegion extends CellCntrMarkerShape {
	
	/**
	 * Region object representing the shape of the marker.
	 */
	protected MTBRegion2D mRegion = null;
	
	/**
	 * Default constructor, it's protected to avoid constructing objects
	 * without region data.
	 */
	@SuppressWarnings("unused")
	private CellCntrMarkerShapeRegion() {
		// nothing to do here, should never be called explicitly
	}
	
	/**
	 * Default constructor with region object.
	 * @param r	Region object.
	 */
	public CellCntrMarkerShapeRegion(MTBRegion2D r) {
		this.mRegion = r;
		try {
			this.mBorder = this.mRegion.getBorder();
		} catch (Exception e) {
			System.err.println("Something went wrong extracting the border...");
		}
	}
	
	/* (non-Javadoc)
	 * @see mtb_cellcounter.CellCntrMarkerShape#getArea()
	 */
	@Override
	public double getArea() {
		return this.mRegion.getArea();
	}

	
	/**
	 * Get region representing the shape.
	 * @return	Region object.
	 */
	public MTBRegion2D getRegion() {
		return this.mRegion;
	}
}
