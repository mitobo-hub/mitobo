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

package de.unihalle.informatik.MiToBo.morphology;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.images.*;

/**
 * Helper functions for analyzing region skeletons.
 * <p>
 * The methods in this class assume skeletons strictly enforcing 8-neighborhood 
 * among their pixels. In particular this implies that each endpoint has
 * exactly (!) one neighbor, each junction point has at least three neighbors, 
 * and all others points of the skeleton have exactly (!) two neigbors. 
 * 
 * @author Birgit Moeller
 */
public class SkeletonAnalysisHelper {
	
	/**
	 * Traces a skeleton branch and returns the list of its pixels.
	 * <p>
	 * The tracing starts at the given position and ends at the next 
	 * branch point, i.e., the next pixel having more than one neighbor.
	 * 
	 * @param img									Skeleton image.
	 * @param x										x-coordinate where to start.
	 * @param y										y-coordinate where to start.
	 * @return	List of branch pixels.
	 */
	public static Vector<Point2D.Double> traceBranch(MTBImage img, int x, int y) {
		
		Vector<Point2D.Double> branchPoints = new Vector<>();
		
		int iwidth = img.getSizeX();
		int iheight = img.getSizeY();
		
		boolean tracingActive = true;
		int nx = x, ny = y;

		// init working image and mark point as processed
		MTBImage memImg = img.duplicate();
		memImg.putValueInt(x, y, 0);
		branchPoints.add(new Point2D.Double(x, y));

		// search next neighbor point
		boolean neighborFound = false;
		for (int dx=-1; !neighborFound && dx<=1; ++dx) {
			for (int dy=-1; !neighborFound && dy<=1; ++dy) {
				if (dx == 0 && dy == 0)
					continue;
				if (   nx+dx >= 0 && nx+dx < iwidth 
						&& ny+dy >= 0 && ny+dy < iheight
						&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
					neighborFound = true;
					nx = nx+dx;
					ny = ny+dy;
				}
			}
		}

		// continue tracing
		while(tracingActive) {
			// mark current point as processed
			memImg.putValueInt(nx, ny, 0);
			branchPoints.add(new Point2D.Double(nx, ny));
			int nCount = numberOfNeighbors(memImg, nx, ny);
			if (nCount == 1) {
				// continue path
				neighborFound = false;
				for (int dx=-1; !neighborFound && dx<=1; ++dx) {
					for (int dy=-1; !neighborFound && dy<=1; ++dy)	 {
						if (dx == 0 && dy == 0)
							continue;
						if (   nx+dx >= 0 && nx+dx < iwidth 
								&& ny+dy >= 0 && ny+dy < iheight
								&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
							neighborFound = true;
							nx = nx+dx;
							ny = ny+dy;
						}
					}
				}
			}
			else if (nCount == 0) {
				tracingActive = false;
			}
			else {
				// either reached another endpoint or a branch point
				tracingActive = false;
			}
		}
		return branchPoints;
	}
	
	/**
	 * Counts the number of neighboring skeleton pixels.
	 * <p>
	 * The 8-neighborhood is considered here.
	 * 
	 * @param img	Skeleton image.
	 * @param x		x-coordinate of pixel to examine.
	 * @param y		y-coordinate of pixel to examine.
	 * @return	Number of neighbors found.
	 */
	public static int numberOfNeighbors(MTBImage img, int x, int y) {
		// init some local variables
		int width = img.getSizeX();
		int height = img.getSizeY();
		int nCount = 0;
		for (int dx=-1;dx<=1;++dx) {
			for (int dy=-1;dy<=1;++dy) {
				// skip pixel itself
				if (dx == 0 && dy == 0)
					continue;
				if (   x+dx >= 0 && x+dx < width && y+dy >= 0 && y+dy < height 
						&& img.getValueInt(x+dx, y+dy) > 0) {
					++nCount;
				}
			}
		}
		return nCount;
	}
}
