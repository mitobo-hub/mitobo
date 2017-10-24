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
 * Class using a parametric curve as shape for markers.
 *
 * @author Birgit Moeller
 */
public class CellCntrMarkerShapeCurve extends CellCntrMarkerShape {
	
	/**
	 * Curve object representing the shape of the marker.
	 */
	protected MTBQuadraticCurve2D mCurve = null;
	
	/**
	 * Default constructor, it's protected to avoid constructing objects
	 * without curve data.
	 */
	@SuppressWarnings("unused")
	private CellCntrMarkerShapeCurve() {
		// nothing to do here, should never be called explicitly
	}
	
	/**
	 * Default constructor with parametric curve object.
	 * @param c	Curve object.
	 */
	public CellCntrMarkerShapeCurve(MTBQuadraticCurve2D c) {
		this.mCurve = c;

		// get ellipse parameters
		double major = this.mCurve.getSemiLengthAxisA();
		double minor = this.mCurve.getSemiLengthAxisB();
		double xCenter = this.mCurve.getCenterX();
		double yCenter = this.mCurve.getCenterY();
		double theta = this.mCurve.getOrientation();

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
		}
		this.mBorder = new MTBBorder2D(bps, BorderConnectivity.CONNECTED_8);
	}
	
	/* (non-Javadoc)
	 * @see mtb_cellcounter.CellCntrMarkerShape#getArea()
	 */
	@Override
	public double getArea() {
		return this.mCurve.getSemiLengthAxisA() * this.mCurve.getSemiLengthAxisB()
				* Math.PI;
	}
}
