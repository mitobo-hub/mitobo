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

import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;

/**
 * Class using a polygon as shape for markers.
 *
 * @author Birgit Moeller
 */
public class CellCntrMarkerShapePolygon extends CellCntrMarkerShape {
	
	/**
	 * Region object representing the shape of the marker.
	 */
	protected MTBPolygon2D mPolygon = null;
	
	/**
	 * Default constructor, it's protected to avoid constructing objects
	 * without polygon data.
	 */
	@SuppressWarnings("unused")
	private CellCntrMarkerShapePolygon() {
		// nothing to do here, should never be called explicitly
	}
	
	/**
	 * Default constructor with region object.
	 * @param p	Polygon object.
	 */
	public CellCntrMarkerShapePolygon(MTBPolygon2D p) {
		this.mPolygon = p;
		try {
			this.mBorder = 
				new MTBBorder2D(p.getPoints(),BorderConnectivity.CONNECTED_8);
		} catch (Exception e) {
			System.err.println("Something went wrong extracting the border...");
		}
	}
	
	@Override
	public double getArea() {
		return Math.abs(this.mPolygon.getSignedArea());
	}

	/**
	 * Get polygon representing the shape.
	 * @return	Polygon object.
	 */
	public MTBPolygon2D getPolygon() {
		return this.mPolygon;
	}
}
