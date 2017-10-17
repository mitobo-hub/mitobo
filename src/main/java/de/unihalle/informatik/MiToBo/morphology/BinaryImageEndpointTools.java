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

import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;

/**
 * Tool functions for extracting endpoints and branches from 
 * binary images, particularly images of skeletons. 
 * 
 * @author moeller
 */
public class BinaryImageEndpointTools {

	/**
	 * Extracts set of endpoints from a given binary image.
	 * <p>
	 * An endpoint (e.g., of a skeleton) is defined as a foreground 
	 * pixel having exactly one neighbor or having two neighbors which are
	 * located side by side. For determining the endpoints in an image
	 * the 8-neighborhood of all foreground pixels is checked. 
	 * 
	 * @param img	Image to analyze (background black, skeleton white).
	 * @return	Set of endpoints.
	 */
	public static Vector<Point2D.Double> findEndpoints(MTBImageByte img) {
		
		int width = img.getSizeX();
		int height = img.getSizeY();

		Vector<Point2D.Double> endPoints = new Vector<Point2D.Double>();
		
		int nCount = 0, nSum = 0;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				
				// only foreground pixels are relevant
				if (img.getValueInt(x, y) > 0) {

					// count the neighbors, check if we have an endpoint
					nCount = 0; nSum = 0;
					for (int dx=-1;dx<=1;++dx) {
						for (int dy=-1;dy<=1;++dy) {
							
							// ignore the pixel itself
							if (dx == 0 && dy == 0)
								continue;

							if (   x+dx >= 0 && x+dx < width 
									&& y+dy >= 0 && y+dy < height) {
								
								/*
								 * To check for endpoints the neighborhood is encoded
								 * as follows. Each neighboring pixel gets a code 
								 * according to the following scheme where X is the
								 * pixel under consideration:
								 * 
								 *    128    1    2
								 *    
								 *     64    X    4
								 *     
								 *     32   16    8
								 *     
								 * The pixel in question is an endpoint if there is
								 * only a single neighbor, or if there are two neighbors
								 * located close to each other. Given the sum of the 
								 * codes for all neigbors, an endpoint is present if the 
								 * sum is equal to any sum of two subsequent code values
								 * in clockwise ordering, i.e., if it is equal to 3, 6,
								 * 12, 24, 48, 96, 192 or 129.
								 */
								if (img.getValueInt(x+dx, y+dy) > 0) {
									++nCount;
									if (dx == -1) {
										if (dy == -1)
											nSum += 128;
										if (dy == 0)
											nSum += 64;
										if (dy == 1)
											nSum += 32;
									}
									if (dx == 0) {
										if (dy == -1)
											nSum += 1;
										if (dy == 1)
											nSum += 16;
									}
									if (dx == 1) {
										if (dy == -1)
											nSum += 2;
										if (dy == 0)
											nSum += 4;
										if (dy == 1)
											nSum += 8;
									}
								}
							}
						}
					}
					if (        nCount==1 
							|| (    nCount==2 
						     && (nSum==3  || nSum==6  || nSum==12  || nSum==24 
						      || nSum==48 || nSum==96 || nSum==192 || nSum==129))) {
						endPoints.add(new Point2D.Double(x, y));
					}
				}
			}
		}
		return endPoints;
	}
	
	/**
	 * Extracts set of endpoint branches from a given binary image.
	 * <p>
	 * An endpoint branch (e.g., of a skeleton) is defined as a sequence
	 * of pixels starting at an endpoint 
	 * (see {@link #findEndpoints(MTBImageByte)} for details) and ending
	 * up at the next branch point. In this context a branch point is a 
	 * pixel with more than two neighbors.
	 * <p>
	 * Note that strict 8-neighborhood is assumed within the branch paths.
	 * Particularly, a branch point is assumed if a pixel has more than
	 * two neighbors. If the 8-neighborhood assumption is broken, a pixel
	 * not being a branch point can also have three neighbors. In such a
	 * case the algorithm will fail! 
	 * 
	 * @param img	Image to analyze (background black, foreground white).
	 * @return	Endpoint branches, first point of each list is endpoint.
	 */
	public static Vector<Vector<Point2D.Double>> findEndpointBranches(
			MTBImageByte img) {
				
		Vector<Vector<Point2D.Double>> endPtBranches = 
				new Vector<Vector<Point2D.Double>>();
		
		// extract endpoints
		Vector<Point2D.Double> endPts = BinaryImageEndpointTools.findEndpoints(img);
		for (Point2D.Double p: endPts) {
			Vector<Point2D.Double> branch = new Vector<Point2D.Double>();
			BinaryImageEndpointTools.traceBranch(
					(MTBImageByte)img.duplicate(HidingMode.HIDDEN), branch, p); 
			endPtBranches.add(branch);
		}
		return endPtBranches;
	}
	
	/**
	 * Continues a given branch at the given pixel recursively.
	 * <p>
	 * Note that strict 8-neighborhood is assumed in the path. If this 
	 * assumption is broken and a pixel in the middle of the path which
	 * is not a branch point has more than two neighbors, the tracing 
	 * will fail!
	 * 
	 * @param img			Image to analyze.
	 * @param branch	List of already detected points of the branch.
	 * @param p				Next branch pixel to examine.
	 */
	private static void traceBranch(MTBImageByte img,
			Vector<Point2D.Double> branch, Point2D.Double p) {
		
		// mark point as found
		img.putValueInt((int)p.x, (int)p.y, 0);
		branch.add(p);
		
		// search next neighbor on branch path
		int neighborCount=0;
		for (int dx=-1; dx<=1; ++dx) {
			for (int dy=-1; dy<=1; ++dy) {
				if (dx==0 && dy==0)
					continue;
				if (img.getValueInt((int)p.x+dx, (int)p.y+dy) == 255)
					++neighborCount;
			}
		}
		// if there is only one, continue tracing
		if (neighborCount == 1) {
			for (int dx=-1; dx<=1; ++dx) {
				for (int dy=-1; dy<=1; ++dy) {
					if (dx==0 && dy==0)
						continue;
					if (img.getValueInt((int)p.x+dx, (int)p.y+dy) == 255)
						traceBranch(img, branch, new Point2D.Double(p.x+dx, p.y+dy));
				}
			}
		}
	}
}
