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

package de.unihalle.informatik.MiToBo.core.imageJ;

import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException;

/**
 * Class to visualize and handle a {@link MTBRegion2DSet} in the ImageJ
 * ROI-Manager. All entries of the region 2D set are listed in one Roi-Manager
 * entry.
 * 
 * @see MTBRegion2DSet
 * 
 * @author Danny Misiak
 * 
 */
public class MTBRegion2DSetROI extends Roi {

		/**
		 * Set of 2D regions.
		 */
		private MTBRegion2DSet regionSet;
		
		/**
		 * Set of borders of the 2D regions.
		 */
		private MTBBorder2DSet borderSet;

		/**
		 * Constructor of super class.
		 * 
		 * @param r
		 *          a given Roi
		 */
		public MTBRegion2DSetROI(Roi r) {
				super(r.getBounds());
		}

		/**
		 * Constructor to create a new Roi-Manager-Object, based on a MTBRegion2DSet.
		 * 
		 * @param regSet
		 *          set of 2D regions
		 * @param roiLabel
		 *          label of the Roi-Manager entry
		 */
		public MTBRegion2DSetROI(MTBRegion2DSet regSet, String roiLabel) {
				super(new Rectangle());
				this.regionSet = regSet;
				this.setName(roiLabel);
				this.borderSet = new MTBBorder2DSet(0, 0, (int) Math.round(this.regionSet.getXmax()), (int) Math.round(this.regionSet.getYmax()));
				// get borders from regions to display regions in image via ROI manager
				for (int r = 0; r < this.regionSet.size(); r++) {
								MTBBorder2D tmpBorder = null;
        try {
		        tmpBorder = this.regionSet.elementAt(r).getBorder();
		       	this.borderSet.add(tmpBorder);
        } catch (ALDOperatorException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
        } catch (ALDProcessingDAGException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
        } 				
				}
		}

		@Override
		public MTBRegion2DSetROI clone() {
				MTBRegion2DSet polySet = this.regionSet.clone();
				MTBRegion2DSetROI newRoi = new MTBRegion2DSetROI(polySet, this.getName());
				return newRoi;
		}

		@Override
		public boolean equals(Object obj) {
				if (!(obj instanceof MTBRegion2DSetROI))
						return false;
				if (((MTBRegion2DSetROI) obj).regionSet != this.regionSet)
						return false;
				return true;
		}

		/**
		 * Get 2D region set from Roi.
		 */
		public MTBRegion2DSet getRegionSet() {
				return this.regionSet;
		}

		/**
		 * Called method to temporary view the selected set of regions from the
		 * Roi-Manager into the current image. The region is drawn only as polygon by
		 * the region border, containing a set of unsorted pixels and the region is
		 * not filled. The filled region is only drawn by selecting the draw option
		 * from the manager.
		 */
		@Override
		public void draw(Graphics g) {
				// set Roi color, default is yellow
				Color color = strokeColor != null ? strokeColor : ROIColor;
				g.setColor(color);
				// draw regions from the set
				for (int r = 0; r < this.borderSet.size(); r++) {
					
						/*
						 * Calculate current coordinates for each region outer-contour point,
						 * depending on image-zoom and -scrolling.
						 */
						MTBBorder2D tmpBorder = this.borderSet.elementAt(r);
						Vector<Point2D.Double> points = tmpBorder.getPoints();
						int[] x = new int[points.size()];
						int[] y = new int[points.size()];
						for (int i = 0; i < points.size(); i++) {
								x[i] = screenX((int) Math.round(points.elementAt(i).x));
								y[i] = screenY((int) Math.round(points.elementAt(i).y));
								g.drawLine(x[i], y[i], x[i], y[i]);
						}
						/*
						 * Calculate current coordinates for each region inner-contour point,
						 * depending on image-zoom and -scrolling.
						 */
						for (int j = 0; j < tmpBorder.getAllInnerBorders().size(); j++) {
								Vector<Point2D.Double> in_points = tmpBorder.getInner(j).getPoints();
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
		 * current image. Here the region is drawn as filled region.
		 */
		@Override
		public void drawPixels(ImageProcessor ip) {
				// draw regions from the set
				for (int r = 0; r < this.regionSet.size(); r++) {
						MTBRegion2D tmpReg = this.regionSet.elementAt(r);

						/*
						 * Calculate current pixel coordinates for each region point, depending on
						 * image-zoom and -scrolling.
						 */
						Vector<Point2D.Double> points = tmpReg.getPoints();

						for (int i = 0; i < points.size(); i++) {
								int x = screenX((int) Math.round(points.elementAt(i).x));
								int y = screenY((int) Math.round(points.elementAt(i).y));
								ip.drawPixel(x, y);
						}
				}
		}
}
