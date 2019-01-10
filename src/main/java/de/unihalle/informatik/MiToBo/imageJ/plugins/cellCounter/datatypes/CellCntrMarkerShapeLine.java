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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;

/**
 * Class using a line segment as shape for markers.
 *
 * @author Birgit Moeller
 */
public class CellCntrMarkerShapeLine extends CellCntrMarkerShape {
	
	/**
	 * Start point of segment.
	 */
	protected Point2D.Double sp = null;

	/**
	 * End point of segment.
	 */
	protected Point2D.Double ep = null;

	/**
	 * Default constructor, it's protected to avoid constructing objects
	 * without curve data.
	 */
	@SuppressWarnings("unused")
	private CellCntrMarkerShapeLine() {
		// nothing to do here, should never be called explicitly
	}
	
	/**
	 * Default constructor.
	 * @param s	Start point of segment.
	 * @param t End point of segment.
	 */
	public CellCntrMarkerShapeLine(Point2D.Double s, Point2D.Double t) {
		this.sp = s;
		this.ep = t;
		// line segment does not have a border...
		Vector<Point2D.Double> b = new Vector<>();
		b.add(s);
		b.add(t);
		this.mBorder = new MTBBorder2D(b, BorderConnectivity.CONNECTED_8);
	}
	
	/* (non-Javadoc)
	 * @see mtb_cellcounter.CellCntrMarkerShape#getArea()
	 */
	@Override
	public double getArea() {
		return Double.NaN;
	}
	
	/**
	 * Get the start point of the line segment.
	 * @return	Start point of line segment.
	 */
	public Point2D.Double getStartPoint() {
		return this.sp;
	}
	
	/**
	 * Get the end point of the line segment.
	 * @return	End point of line segment.
	 */
	public Point2D.Double getEndPoint() {
		return this.ep;
	}
}
