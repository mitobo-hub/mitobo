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

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;

/**
 * Class to visualize and handle a {@link MTBPolygon2DSet} in the ImageJ
 * ROI-Manager. All entries of the polygon 2D set are listed in one Roi-Manager
 * entry.
 * 
 * @see MTBPolygon2DSet
 * 
 * @author Danny Misiak
 * 
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBPolygon2DSetROI extends Roi {

		/**
		 * Set of 2D polygons.
		 */
		MTBPolygon2DSet polygonSet;

		/**
		 * Constructor of super class.
		 * 
		 * @param r
		 *          a given Roi
		 */
		public MTBPolygon2DSetROI(Roi r) {
				super(r.getBounds());
		}

		/**
		 * Constructor to create a new Roi-Manager-Object, based on a MTBPolygon2DSet.
		 * 
		 * @param polySet
		 *          set of 2D polygons
		 * @param roiLabel
		 *          label of the Roi-Manager entry
		 */
		public MTBPolygon2DSetROI(MTBPolygon2DSet polySet, String roiLabel) {
				super(new Rectangle());
				this.polygonSet = polySet;
				this.setName(roiLabel);
		}

		@Override
		public MTBPolygon2DSetROI clone() {
			MTBPolygon2DSet polySet = this.polygonSet.clone();
			MTBPolygon2DSetROI newRoi = 
					new MTBPolygon2DSetROI(polySet, this.getName());
			return newRoi;
		}

		@Override
    public boolean equals(Object obj) {
			if (!(obj instanceof MTBPolygon2DSetROI))
					return false;
			if (((MTBPolygon2DSetROI)obj).polygonSet != this.polygonSet)
				return false;
			return true;
		}
		
		/**
		 * Get 2D polygon set from Roi.
		 */
		public MTBPolygon2DSet getPolygonSet() {
				return this.polygonSet;
		}

		/**
		 * Called method to temporary view the selected set of polygons from the
		 * Roi-Manager into the current image.
		 */
		@Override
		public void draw(Graphics g) {
				// set Roi color, default is yellow
				Color color = strokeColor != null ? strokeColor : ROIColor;
				g.setColor(color);
				// draw polygons from the set
				for (int p = 0; p < this.polygonSet.size(); p++) {
						/*
						 * Calculate current coordinates for each polygon point, depends on
						 * image-zoom and -scrolling.
						 */
						// scaling factor if polygon is snake
						double scaleFactor = 1.0;
						if (this.polygonSet.elementAt(p).getClass() == MTBSnake.class) {
								scaleFactor = ((MTBSnake) this.polygonSet.elementAt(p))
								    .getScaleFactor();
						}
						Vector<Point2D.Double> points = this.polygonSet.elementAt(p).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x * scaleFactor));
								y[i] = screenY((int) Math.round(points.elementAt(i).y * scaleFactor));
						}
						// create java awt polygon from MTBPolygon
						Polygon poly = new Polygon(x, y, points.size());
						g.drawPolygon(poly);
				}

		}

		/**
		 * Called method to draw the selected entry from the Roi-Manager into the
		 * current image.
		 */
		@Override
		public void drawPixels(ImageProcessor ip) {
				// draw polygons from the set
				for (int p = 0; p < this.polygonSet.size(); p++) {
						/*
						 * Calculate current coordinates for each polygon point, depends on
						 * image-zoom and -scrolling.
						 */
						// scaling factor if polygon is snake
						double scaleFactor = 1.0;
						if (this.polygonSet.elementAt(p).getClass() == MTBSnake.class) {
								scaleFactor = ((MTBSnake) this.polygonSet.elementAt(p))
								    .getScaleFactor();
						}
						Vector<Point2D.Double> points = this.polygonSet.elementAt(p).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x * scaleFactor));
								y[i] = screenY((int) Math.round(points.elementAt(i).y * scaleFactor));
						}
						// create java awt polygon from MTBPolygon
						Polygon poly = new Polygon(x, y, points.size());
						ip.drawPolygon(poly);
				}
		}

}
