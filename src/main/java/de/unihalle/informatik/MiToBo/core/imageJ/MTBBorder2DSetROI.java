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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;

/**
 * Class to visualize and handle a {@link MTBBorder2DSet} in the ImageJ
 * ROI-Manager. All entries of the border 2D set are listed in one Roi-Manager
 * entry.
 * 
 * @see MTBBorder2DSet
 * 
 * @author Danny Misiak
 * 
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBBorder2DSetROI extends Roi {

		/**
		 * Set of 2D borders.
		 */
		MTBBorder2DSet borderSet;

		/**
		 * Constructor of super class.
		 * 
		 * @param r
		 *          a given Roi
		 */
		public MTBBorder2DSetROI(Roi r) {
				super(r.getBounds());
		}

		/**
		 * Constructor to create a new Roi-Manager-Object, based on a MTBBorder2DSet.
		 * 
		 * @param borderSet
		 *          set of 2D borders
		 * @param roiLabel
		 *          label of the Roi-Manager entry
		 */
		public MTBBorder2DSetROI(MTBBorder2DSet borderSet, String roiLabel) {
				super(new Rectangle());
				this.borderSet = borderSet;
				this.setName(roiLabel);

		}

		@Override
		public MTBBorder2DSetROI clone() {
				MTBBorder2DSet bSet = this.borderSet.clone();
			MTBBorder2DSetROI newRoi = 
					new MTBBorder2DSetROI(bSet, this.getName());
			return newRoi;
		}

		@Override
    public boolean equals(Object obj) {
			if (!(obj instanceof MTBBorder2DSetROI))
					return false;
			if (((MTBBorder2DSetROI)obj).borderSet != this.borderSet)
				return false;
			return true;
		}

		/**
		 * Get 2D border set from Roi.
		 */
		public MTBBorder2DSet getBorderSet() {
				return this.borderSet;
		}

		/**
		 * Called method to temporary view the selected set of borders from the
		 * Roi-Manager into the current image.
		 */
		@Override
		public void draw(Graphics g) {
				// set Roi color, default is yellow
				Color color = strokeColor != null ? strokeColor : ROIColor;
				g.setColor(color);
				// draw borders from the set
				for (int c = 0; c < this.borderSet.size(); c++) {
						/*
						 * Calculate current coordinates for each outer-border point, depending
						 * on image-zoom and -scrolling.
						 */
						Vector<Point2D.Double> points = this.borderSet.elementAt(c).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x));
								y[i] = screenY((int) Math.round(points.elementAt(i).y));
								g.drawLine(x[i], y[i], x[i], y[i]);
						}
						
						/*
						 * Calculate current coordinates for each inner-border point, depending
						 * on image-zoom and -scrolling.
						 */
						for (int j = 0; j < this.borderSet.elementAt(c).getAllInnerBorders().size(); j++) {
								Vector<Point2D.Double> in_points = this.borderSet.elementAt(c)
								    .getInner(j).getPoints();
								int[] in_x = new int[in_points.size()];
								int[] in_y = new int[in_points.size()];
								for (int i = 0; i < in_points.size(); i++) {
										in_x[i] = screenX((int) Math.round(in_points.elementAt(i).x));
										in_y[i] = screenY((int) Math.round(in_points.elementAt(i).y));
										g.drawLine(in_x[i], in_y[i], in_x[i], in_y[i]);
								}
						}
				}

		}

		/**
		 * Called method to draw the selected entry from the Roi-Manager into the
		 * current image.
		 */
		@Override
		public void drawPixels(ImageProcessor ip) {
				// draw borders from the set
				for (int c = 0; c < this.borderSet.size(); c++) {
						/*
						 * Calculate current coordinates for each outer-border point, depending
						 * on image-zoom and -scrolling.
						 */
						Vector<Point2D.Double> points = this.borderSet.elementAt(c).getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x));
								y[i] = screenY((int) Math.round(points.elementAt(i).y));
								ip.drawLine(x[i], y[i], x[i], y[i]);
						}
					
						/*
						 * Calculate current coordinates for each inner-border point, depending
						 * on image-zoom and -scrolling.
						 */
						for (int j = 0; j < this.borderSet.elementAt(c).getAllInnerBorders().size(); j++) {
								Vector<Point2D.Double> in_points = this.borderSet.elementAt(c)
								    .getInner(j).getPoints();
								int[] in_x = new int[in_points.size()];
								int[] in_y = new int[in_points.size()];
								for (int i = 0; i < in_points.size(); i++) {
										in_x[i] = screenX((int) Math.round(in_points.elementAt(i).x));
										in_y[i] = screenY((int) Math.round(in_points.elementAt(i).y));
										ip.drawLine(in_x[i], in_y[i], in_x[i], in_y[i]);
								}
						}
				}
		}
}
