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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.core.imageJ;

import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;

/**
 * Class to visualize and handle a {@link MTBContour2DSet} in the ImageJ
 * ROI-Manager. All entries of the contour 2D set are listed in one Roi-Manager
 * entry.
 * 
 * @see MTBContour2DSet
 * 
 * @author Danny Misiak
 * 
 */
public class MTBContour2DSetROI extends Roi {

		/**
		 * Set of 2D contours.
		 */
		MTBContour2DSet contourSet;

		/**
		 * Constructor of super class.
		 * 
		 * @param r
		 *          a given Roi
		 */
		public MTBContour2DSetROI(Roi r) {
				super(r.getBounds());
		}

		/**
		 * Constructor to create a new Roi-Manager-Object, based on a MTBContour2DSet.
		 * 
		 * @param contSet
		 *          set of 2D contours
		 * @param roiLabel
		 *          label of the Roi-Manager entry
		 */
		public MTBContour2DSetROI(MTBContour2DSet contSet, String roiLabel) {
				super(new Rectangle());
				this.contourSet = contSet;
				this.setName(roiLabel);

		}

		@Override
		public MTBContour2DSetROI clone() {
			MTBContour2DSet contSet = this.contourSet.clone();
			MTBContour2DSetROI newRoi = 
					new MTBContour2DSetROI(contSet, this.getName());
			return newRoi;
		}

		@Override
    public boolean equals(Object obj) {
			if (!(obj instanceof MTBContour2DSetROI))
					return false;
			if (((MTBContour2DSetROI)obj).contourSet != this.contourSet)
				return false;
			return true;
		}

		/**
		 * Get 2D contour set from Roi.
		 */
		public MTBContour2DSet getContourSet() {
				return this.contourSet;
		}

		/**
		 * Called method to temporary view the selected set of contours from the
		 * Roi-Manager into the current image.
		 */
		@Override
		public void draw(Graphics g) {
				// set Roi color, default is yellow
				Color color = strokeColor != null ? strokeColor : ROIColor;
				g.setColor(color);
				// draw contours from the set
				for (int c = 0; c < this.contourSet.size(); c++) {
						/*
						 * Calculate current coordinates for each outer-contour point, depending
						 * on image-zoom and -scrolling.
						 */
						Vector<Point2D.Double> points = this.contourSet.elementAt(c).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x));
								y[i] = screenY((int) Math.round(points.elementAt(i).y));
						}
						// create java awt polygon from MTBContour
						Polygon poly = new Polygon(x, y, points.size());
						g.drawPolygon(poly);
						/*
						 * Calculate current coordinates for each inner-contour point, depending
						 * on image-zoom and -scrolling.
						 */
						for (int j = 0; j < this.contourSet.elementAt(c).getAllInner().size(); j++) {
								Vector<Point2D.Double> in_points = this.contourSet.elementAt(c)
								    .getInner(j).getPoints();
								int[] in_x = new int[in_points.size()];
								int[] in_y = new int[in_points.size()];
								for (int i = 0; i < in_points.size(); i++) {
										in_x[i] = screenX((int) Math.round(in_points.elementAt(i).x));
										in_y[i] = screenY((int) Math.round(in_points.elementAt(i).y));
								}
								// create java awt polygon from MTBContour
								Polygon in_poly = new Polygon(in_x, in_y, in_points.size());
								g.drawPolygon(in_poly);
						}
				}

		}

		/**
		 * Called method to draw the selected entry from the Roi-Manager into the
		 * current image.
		 */
		@Override
		public void drawPixels(ImageProcessor ip) {
				// draw contours from the set
				for (int c = 0; c < this.contourSet.size(); c++) {
						/*
						 * Calculate current coordinates for each outer-contour point, depending
						 * on image-zoom and -scrolling.
						 */
						Vector<Point2D.Double> points = this.contourSet.elementAt(c).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x));
								y[i] = screenY((int) Math.round(points.elementAt(i).y));
						}
						// create java awt polygon from MTBContour
						Polygon poly = new Polygon(x, y, points.size());
						ip.drawPolygon(poly);
						/*
						 * Calculate current coordinates for each inner-contour point, depending
						 * on image-zoom and -scrolling.
						 */
						for (int j = 0; j < this.contourSet.elementAt(c).getAllInner().size(); j++) {
								Vector<Point2D.Double> in_points = this.contourSet.elementAt(c)
								    .getInner(j).getPoints();
								int[] in_x = new int[in_points.size()];
								int[] in_y = new int[in_points.size()];
								for (int i = 0; i < in_points.size(); i++) {
										in_x[i] = screenX((int) Math.round(in_points.elementAt(i).x));
										in_y[i] = screenY((int) Math.round(in_points.elementAt(i).y));
								}
								// create java awt polygon from MTBContour
								Polygon in_poly = new Polygon(in_x, in_y, in_points.size());
								ip.drawPolygon(in_poly);
						}
				}
		}
}
