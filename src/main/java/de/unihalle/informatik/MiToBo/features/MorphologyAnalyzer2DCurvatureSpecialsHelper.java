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

package de.unihalle.informatik.MiToBo.features;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBLineSegment2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

/**
 * Helper functions for analyzing concavities and convexities along a contour.
 * 
 * @author Birgit Moeller
 */
public class MorphologyAnalyzer2DCurvatureSpecialsHelper {

	private MTBImageRGB debugInfoImg;
	
	private MTBImage labelImg;
	
	private int width;
	
	private int height;

	double deltaXY;
	
	class CurvatureAnalysisResult {
		
		Vector<CurvatureAnalysisLevelResult> levelResults;

	}

	class CurvatureAnalysisLevelResult {
		
		MTBContour2D contour;
		int contourID;
		
  	LinkedList<LinkedList<Point2D.Double>> protrusionSegs;
  	LinkedList<LinkedList<Point2D.Double>> indentationSegs;
		LinkedList<Point2D.Double> inflections;

		int numberOfProtrusions;
		double avgEquatorProtrusionLength;
		double avgEquatorIndentationLength;
		double avgProtrusionLength;
		double avgBaselineProtrusionLength;
		double avgApicalProtrusionLength;
		double avgBasalProtrusionLength;
		double nonProtrusionArea;
		double avgDistsIndentationMidPoints;
		double minMinimalDistsIndentationMidPoints;
		double maxMinimalDistsIndentationMidPoints;

		double avgIndentationLength;
		double avgBaselineIndentationLength;
		double avgApicalIndentationLength;
		double avgBasalIndentationLength;
	}
	
  Vector<CurvatureAnalysisLevelResult> analyzeConcavitiesConvexities(
			MTBContour2DSet contours, Vector<double[]> curvatureValues, 
				int dwidth, int dheigth, double dxy, MTBImage lImg,
				int minProtrusionLength, MTBImageRGB debugImg) {
    
		Vector<CurvatureAnalysisLevelResult> curveAnalysisLevelResults = new Vector<>();
		
		this.debugInfoImg = debugImg;
		this.labelImg = lImg;		
		
		this.width = dwidth;
		this.height = dheigth;
		this.deltaXY = dxy;
		
		double lobeDepthSum = 0, neckDepthSum = 0;
		int protrusionCount = 0;
		int contourID = 0;
		Vector<int[]> curveDirections = new Vector<int[]>();
	    
		// iterate over all contours and map curvatures to directions:
		// 1 = pos. curvature, -1 = neg. curvature, 0 = below threshold
		for (double[] curvVals: curvatureValues) {
			
			CurvatureAnalysisLevelResult levelResult = 
					new CurvatureAnalysisLevelResult();
			levelResult.contourID = contourID;
			levelResult.contour = contours.elementAt(contourID);
			
			lobeDepthSum = 0;
			neckDepthSum = 0;
			int[] dirs = new int[curvVals.length];
			for (int j=0; j<curvVals.length; ++j) {
				double curvVal = curvVals[j];
				if (curvVal > 1) {
					dirs[j] = 1;
				}
				else if (curvVal < -1.0){
					dirs[j] = -1;
				}
				else {
					dirs[j] = 0;
				}    		
			}
			// map pixels with no direction to direction of closed contour
			// pixel with a clear direction
			int[] fixedDirs = new int[curvVals.length];
			for (int j=0; j<curvVals.length; ++j) {
				int curvVal = dirs[j];
				if (curvVal != 0) {
					fixedDirs[j] = curvVal;
					continue;
				}
				// search for the next pixel with direction to the left
				boolean foundLeft = false;
				int idLeft = 0;
				for (int l=j-1; !foundLeft && l!=j ; --l) {
					if (l < 0)
						l = dirs.length + l;
					if (dirs[l] != 0) {
						idLeft = l;
						foundLeft = true;
					}
				}
				// search for the next pixel with direction to the right
				boolean foundRight = false;
				int idRight = 0;
				for (int l=j+1; !foundRight && l!=j ; ++l) {
					if (l >= dirs.length)
						l = l - dirs.length;
					if (dirs[l] != 0) {
						idRight = l;
						foundRight = true;
					}
				}
				// check which is closer and set direction accordingly
				if (Math.abs(j - idLeft) < Math.abs(j - idRight)) {
					fixedDirs[j] = dirs[idLeft];
				}
				else if (Math.abs(j - idLeft) > Math.abs(j - idRight)) {
					fixedDirs[j] = dirs[idRight];
				}
				else {
					if (  Math.abs(curvVals[idLeft]) 
							> Math.abs(curvVals[idRight])) {
						fixedDirs[j] = dirs[idLeft];    				
					}
					else {
						fixedDirs[j] = dirs[idRight];
					}
				}
			}
	    	
			// increase robustness: 
			// check pixel count of protrusions/indentations, if too small, 
			// remove protrusion/indentation by inverting sign of their curvature
			removeShortProtrusions(fixedDirs, minProtrusionLength);

			LinkedList<LinkedList<Point2D.Double>> protrusionSegs = 
					new LinkedList<>();
			LinkedList<LinkedList<Point2D.Double>> indentationSegs = 
					new LinkedList<>();
			boolean onProtrusion = true;
			LinkedList<Point2D.Double> pList = new LinkedList<>();
	    	
			// count sign changes along contour and 
			// extract protrusion/indentation segments
			MTBContour2D c = contours.elementAt(contourID);
			LinkedList<Point2D.Double> inflections = 
					new LinkedList<Point2D.Double>();

			double protrusionEquatorSum = 0;
			double indentationEquatorSum = 0;

			int signChangeCounter = 0;
			int sign = fixedDirs[fixedDirs.length-1];
			if (sign > 0)
				onProtrusion = true;
			else
				onProtrusion = false;
			for (int j=0; j<fixedDirs.length; ++j) {
				if (fixedDirs[j] != sign) {

					if (onProtrusion) {
						if (!pList.isEmpty())
							protrusionSegs.add(pList);
						if (!inflections.isEmpty()) {
							protrusionEquatorSum += 
									c.getPointAt(j).distance(inflections.getLast());
						}
					}
					else {
						if (!pList.isEmpty())
							indentationSegs.add(pList);
						if (!inflections.isEmpty()) {
							indentationEquatorSum += 
									c.getPointAt(j).distance(inflections.getLast());
						}
					}

					++signChangeCounter;
					sign *= -1;
					// inflection points are defined to be the starting points
					// of new segments immediately after the sign changed,
					// i.e. each segment start point is also inflection point
					inflections.add(c.getPointAt(j));

					pList = new LinkedList<>();
					onProtrusion = !onProtrusion;
				}
				pList.add(c.getPointAt(j));
			}
			if (onProtrusion) {
				if (!pList.isEmpty())
					protrusionSegs.add(pList);
				// add length of last equator segment between first and last 
				// inflection point to corresponding sum
				protrusionEquatorSum += 
						inflections.getFirst().distance(inflections.getLast());
			}
			else {
				if (!pList.isEmpty())
					indentationSegs.add(pList);
				// add length of last equator segment between first and last 
				// inflection point to corresponding sum
				indentationEquatorSum += 
						inflections.getFirst().distance(inflections.getLast());
			}

			// check if first and last segment belong together
			if (fixedDirs[fixedDirs.length-1] == fixedDirs[0]) {
				if (fixedDirs[0] > 0) {
					protrusionSegs.getLast().addAll(protrusionSegs.pop());
				}
				else {
					indentationSegs.getLast().addAll(indentationSegs.pop());	    			
				}
			}
	    				
			// remember number of protrusions and equator lengths
			protrusionCount = (int)(signChangeCounter/2.0);
//			this.protrusionCounts.add(new Integer(protrusionCount));
//			this.avgEquatorProtrusionLengths.add(
//					new Double(protrusionEquatorSum/protrusionCount));
//			this.avgEquatorIndentationLengths.add(
//					new Double(indentationEquatorSum/protrusionCount));

			/* Birgit - neu */
			levelResult.indentationSegs = indentationSegs;
			levelResult.protrusionSegs = protrusionSegs;
			levelResult.inflections = inflections;
			levelResult.numberOfProtrusions = protrusionCount;
			levelResult.avgEquatorProtrusionLength = 
					protrusionEquatorSum/protrusionCount;
			levelResult.avgEquatorIndentationLength = 
					indentationEquatorSum/protrusionCount;
			
			// remember contour directions
			curveDirections.add(fixedDirs);

			// plot protrusions and indentations as well as equators 
			// to info image if requested
			if (debugInfoImg != null) {
				int green = ((0 & 0xff)<<16)+((255 & 0xff)<<8) + (0 & 0xff);
				for (int k=0; k<inflections.size()-1; ++k) {
					int sx = (int)inflections.get(k).x;
					int sy = (int)inflections.get(k).y;
					int ex = (int)inflections.get(k+1).x;
					int ey = (int)inflections.get(k+1).y;
					debugInfoImg.drawLine2D(sx, sy, ex, ey, green);
				}
				int sx = (int)inflections.get(inflections.size()-1).x;
				int sy = (int)inflections.get(inflections.size()-1).y;
				int ex = (int)inflections.get(0).x;
				int ey = (int)inflections.get(0).y;
				debugInfoImg.drawLine2D(sx, sy, ex, ey, green);

				Vector<Point2D.Double> ps = c.getPoints();
				int j=0;
				for (Point2D.Double p: ps) {
					int px = (int)p.x;
					int py = (int)p.y;
					if (fixedDirs[j] > 0) {
						debugInfoImg.putValueR(px, py, 255);
						debugInfoImg.putValueG(px, py, 0);
						debugInfoImg.putValueB(px, py, 0);
					}
					else if (fixedDirs[j] < 0) {
						debugInfoImg.putValueR(px, py, 0);
						debugInfoImg.putValueG(px, py, 0);
						debugInfoImg.putValueB(px, py, 255);                       
					}
					else {
						debugInfoImg.putValueR(px, py, 255);
						debugInfoImg.putValueG(px, py, 255);
						debugInfoImg.putValueB(px, py, 255);                                               
					}
					++j;
				}
			}

				// process each protrusion and indentation and calculate lengths
//	    	int t=0;
//	    	double d, pDist;
//	    	boolean go = true;
//	    	boolean reachedEnd = false;
//	    	boolean processProtrusion, processIndentation;
//				Point2D.Double ep;
//	    	while (go && !reachedEnd) {
//	    		
//	    		// get contour point to analyze next
//	    		Point2D.Double p = c.getPointAt(t);
//	    		
//	    		// check if point is inflection
//	    		if (inflections.contains(p)) {
//	    			// check if a protrusion starts here
//	    			if (fixedDirs[t] > 0) {
//	    				// get index in list
//	    				int is = inflections.indexOf(p);
//	    				// get endpoint of current protrusion/indentation
//	    				if (is + 1 < inflections.size()-1) {
//	    					ep = inflections.get(is+1);
//	    				}
//	    				else {
//	    					ep = inflections.get(0);
//	    				}
//	    				// init connecting line
//	    				Line2D.Double connectLine = 
//	    						new Line2D.Double(p.x, p.y, ep.x, ep.y);
//	    				
//	    				pDist = 0;
//	    				
//	    				++t;
//	    				if (t >= fixedDirs.length) {
//	    					t = fixedDirs.length - t;
//	    					reachedEnd = true;
//	    				}
//	    				processProtrusion = true;
//	    				while (processProtrusion) {
//	    					if (fixedDirs[t] <= 0.5) {
//	    						processProtrusion = false;
//	    						lobeDepthSum += pDist;
//	    					}
//	    					else {
//	  	    				// get next point
//	    						p = c.getPointAt(t);
//	    						// calculate distance of point to connecting line
//	    						d = connectLine.ptLineDist(p);
//	    						if (d > pDist) {
//	    							pDist = d;
//	    						}
//	    						
//	    						++t;
//	        				if (t >= fixedDirs.length) {
//	        					t = fixedDirs.length - t;
//	        					reachedEnd = true;
//	        				}
//	    					}
//	    				}
//	    			}
//	    			// neck region starts here
//	    			else {
//	    				int is = inflections.indexOf(p);
//	    				if (is + 1 < inflections.size()-1) {
//	    					ep = inflections.get(is+1);
//	    				}
//	    				else {
//	    					ep = inflections.get(0);
//	    				}
//	    				// init connecting line
//	    				Line2D.Double connectLine = 
//	    						new Line2D.Double(p.x, p.y, ep.x, ep.y);
//
//	    				pDist = 0;
//
//	    				// get next point
//	    				++t;
//	    				if (t >= fixedDirs.length) {
//	    					t = fixedDirs.length - t;
//	    					reachedEnd = true;
//	    				}
//	    				processIndentation = true;
//	    				while (processIndentation) {
//	    					if (fixedDirs[t] >= -0.5) {
//	    						processIndentation = false;
//	    						neckDepthSum += pDist;
//	    					}
//	    					else {
//	    						// get next point in neck region
//	    						p = c.getPointAt(t);
//	    						
//	    						// calculate distance of point to connecting line
//	    						d = connectLine.ptLineDist(p);
//	    						if (d > pDist)
//	    							pDist = d;
//	    						
//	    						++t;
//	    						if (t >= fixedDirs.length) {
//	    							t = fixedDirs.length - t;
//	    							reachedEnd = true;
//	    						}
//	    					}
//	    				}
//	  				} // end of neck region else-clause
//	    		}
//	    		else {
//	    			++t;
//	    		}
//	    		if (t >= c.getPointNum())
//	    			go = false;
//	    	} // end of while-loop over current lobe/neck region
//	    	this.avgLobeDepths.add(
//	    			new Double( (lobeDepthSum*this.deltaXY.doubleValue()) / protrusionCount));
//	    	this.avgNeckDepths.add(
//	    			new Double( (neckDepthSum*this.deltaXY.doubleValue()) / protrusionCount));
	    	
			// further process indentation segments to learn more about protrusions
			this.postprocessIndentationSegments(levelResult);

			this.measureIndentationDistances(levelResult);

			// further process protrusion segments to learn more about indentations
			this.postprocessProtrusionSegments(levelResult);

			curveAnalysisLevelResults.add(levelResult);
			
			// increment cell ID
			++contourID;

		} // end of for-loop over all regions
		
		return curveAnalysisLevelResults;
	}

	/**
	 * Post-process indentation segments to get more information on protrusions.
	 * <p>
	 * Here the boundary points of indentation regions are determined and
	 * different length measures are extracted.
	 * 
	 * @param c									Contour to process.
	 * @param contourID					ID of the contour.
	 * @param inflections				Set of inflection points.
	 * @param protrusionCount		Number of detected protrusions.
	 * @param indentationSegs		Indentation segments to process.
	 */
	private void postprocessIndentationSegments(
				CurvatureAnalysisLevelResult levelResult) {
			
		MTBContour2D c = levelResult.contour;
		int contourID = levelResult.contourID;
		LinkedList<Point2D.Double> inflections = levelResult.inflections;
		int protrusionCount = levelResult.numberOfProtrusions;
		LinkedList<LinkedList<Point2D.Double>> indentationSegs = 
				levelResult.indentationSegs;
		
		Vector<Point2D.Double> nonProtrusionAreaPolyPoints = 
				new Vector<Point2D.Double>();
		double protrusionBaselineSum = 0;
		double protrusionLengthSum = 0;
		double protrusionLengthApicalSum = 0;
		double protrusionLengthBasalSum = 0;
		
  	for (int n=0; n<indentationSegs.size(); ++n) {
  		
  		LinkedList<Point2D.Double> neck = indentationSegs.get(n);
			LinkedList<Point2D.Double> nextNeck;
  		if (n ==indentationSegs.size()-1 )
  			nextNeck = indentationSegs.get(0);
  		else
  			nextNeck = indentationSegs.get(n+1);

  		Point2D.Double neckMidPoint = neck.get(neck.size()/2);
			int nmpx = (int)neckMidPoint.x;
			int nmpy = (int)neckMidPoint.y;
  		
  		Point2D.Double nextNeckMidPoint = nextNeck.get(nextNeck.size()/2);
			int nnmpx = (int)nextNeckMidPoint.x;
			int nnmpy = (int)nextNeckMidPoint.y;
  			    		
  		// draw middle point to image
			if (this.debugInfoImg != null) {
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							debugInfoImg.putValueR(nmpx+dx, nmpy+dy, 255);
							debugInfoImg.putValueG(nmpx+dx, nmpy+dy, 255);
							debugInfoImg.putValueB(nmpx+dx, nmpy+dy, 255);
						}
					}						
				}
			}

			// check if baseline intersects with background
			MTBLineSegment2D baseline = 
					new MTBLineSegment2D(nmpx, nmpy, nnmpx, nnmpy);
			LinkedList<Point2D.Double> pixelList = 
					baseline.getPixelsAlongSegment();
			
			boolean outsideCell = false;
			int pixOutside = 0;
			for (Point2D.Double q: pixelList) {
				if (this.labelImg.getValueInt((int)q.x, (int)q.y) != (contourID+1)) {
					outsideCell = true;
					++pixOutside;
				}
			}
			
			Point2D.Double newStartPoint = neckMidPoint;
			Point2D.Double newEndPoint = nextNeckMidPoint;
			
			// there are pixels out of the region area, shift points
			if (outsideCell) {
				
				int nPixOutside = 0;
				int minOutside = pixOutside;
				int shift, totalShift = Integer.MAX_VALUE;

				// shift start point
				for (int ps = neck.size()/2; ps < neck.size(); ++ps) {
					Point2D.Double tps = neck.get(ps);
					for (int pe = nextNeck.size()/2; pe >= 0; --pe) {
						Point2D.Double tpe = nextNeck.get(pe);

						baseline = new MTBLineSegment2D(
								(int)tps.x, (int)tps.y,	(int)tpe.x, (int)tpe.y);
						pixelList = baseline.getPixelsAlongSegment();
						nPixOutside = 0;
						for (Point2D.Double q: pixelList) {
							if (this.labelImg.getValueInt((int)q.x,(int)q.y)!=(contourID+1)){
								++nPixOutside;
							}
						}
						// check for minimum of segment pixels outside of region
						shift = ((nextNeck.size()/2) - pe)
								+ (ps - (neck.size()/2));
						if (     nPixOutside < minOutside 
								|| ((nPixOutside == minOutside) && shift < totalShift)) {
							minOutside = nPixOutside;
							totalShift = shift;
							newStartPoint = tps;
							newEndPoint = tpe;
						}
					}
				}
			} // end of if-clause for indentation region optimization
			
			// collect set of indentation region border points
			nonProtrusionAreaPolyPoints.add(newStartPoint);
			nonProtrusionAreaPolyPoints.add(newEndPoint);
			
			// sum lengths of baselines
			protrusionBaselineSum += newStartPoint.distance(newEndPoint);
			
			Line2D.Double baseLine = new Line2D.Double(
				newStartPoint.x, newStartPoint.y, newEndPoint.x, newEndPoint.y);
			
			Vector<Point2D.Double> cPoints = c.getPoints();
			Vector<Point2D.Double> iPoints = new Vector<>();
			int sID = cPoints.indexOf(newStartPoint);
			int eID = cPoints.indexOf(newEndPoint);
			
			// check if segment overlaps contour start/end
			int lastID = (eID < sID ? cPoints.size() : eID); 
			double maxDist = 0, dist;
			Point2D.Double maxDistPoint = newStartPoint, cp;
			for (int id = sID+1; id<lastID; ++id) {
				cp = cPoints.get(id);
				dist = baseLine.ptLineDist(cp);
				if (dist > maxDist) {
					maxDist = dist;
					maxDistPoint = cp;
				}
				if (inflections.contains(cp))
					iPoints.add(cp);
			}
			if (eID < sID) {
				for (int id = 0; id<eID; ++id) {
					cp = cPoints.get(id);
					dist = baseLine.ptLineDist(cp);
					if (dist > maxDist) {
						maxDist = dist;
						maxDistPoint = cp;
					}
					if (inflections.contains(cp))
						iPoints.add(cp);
				}					
			}
			if (inflections.contains(newEndPoint))
				iPoints.add(newEndPoint);
			
			protrusionLengthSum += maxDist;		
			
			// calculate base point of distance line
			double vx = newEndPoint.x - newStartPoint.x;
			double vy = newEndPoint.y - newStartPoint.y;
			double spx = maxDistPoint.x - newStartPoint.x;
			double spy = maxDistPoint.y - newStartPoint.y;
			double blength = vx*vx + vy*vy;
			double plength = (vx*spx + vy*spy) / blength;
			
			double ppx = newStartPoint.x + plength * vx;
			double ppy = newStartPoint.y + plength * vy;

			// calculation intersection of distance line and equator
			if (iPoints.size() == 2) {
				Point2D.Double p1 = iPoints.get(0);
				Point2D.Double p2 = iPoints.get(1);
				MTBLineSegment2D equator = 
						new MTBLineSegment2D(p1.x, p1.y, p2.x, p2.y);
				MTBLineSegment2D distline = 
						new MTBLineSegment2D(ppx, ppy, maxDistPoint.x, maxDistPoint.y);
				Point2D.Double isect = equator.getIntersection(distline);
				if (!Double.isNaN(isect.x) && !Double.isNaN(isect.y)) {
					protrusionLengthApicalSum += isect.distance(maxDistPoint);
					protrusionLengthBasalSum += isect.distance(ppx, ppy);
				}
			}
			else {
				System.err.println("Error: found " + iPoints.size() + " inflection points...!");
				for (Point2D.Double p : iPoints) {
					System.out.println(p.x + " , " + p.y);
				}
				System.out.println("Done");
			}
			
			if (this.debugInfoImg != null) {
				int px = (int)newStartPoint.x;
				int py = (int)newStartPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				px = (int)newEndPoint.x;
				py = (int)newEndPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				// draw the (new) segment
				this.debugInfoImg.drawLine2D(
						(int)newStartPoint.x, (int)newStartPoint.y,
						(int)newEndPoint.x, (int)newEndPoint.y, 0x00FFA500);
				
				// draw max. distance point
				px = (int)maxDistPoint.x;
				py = (int)maxDistPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				this.debugInfoImg.drawLine2D((int)ppx, (int)ppy, 
						(int)maxDistPoint.x, (int)maxDistPoint.y, 0x00000000);

			} // end of optional drawing section
			
  	} // end of for-loop over all indentation regions			
  	
//  	this.avgProtrusionLengths.add(new Double(
//    	protrusionLengthSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgBaselineProtrusionLengths.add(new Double(
//  		protrusionBaselineSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgApicalProtrusionLengths.add(new Double(
//    	protrusionLengthApicalSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgBasalProtrusionLengths.add(new Double(
//  		protrusionLengthBasalSum*this.deltaXY.doubleValue()/protrusionCount));
  
  	/* Birgit - neu */
  	levelResult.avgProtrusionLength = 
  			protrusionLengthSum*this.deltaXY/protrusionCount;
  	levelResult.avgBaselineProtrusionLength = 
  			protrusionBaselineSum*this.deltaXY/protrusionCount;
  	levelResult.avgApicalProtrusionLength = 
  			protrusionLengthApicalSum*this.deltaXY/protrusionCount;
  	levelResult.avgBasalProtrusionLength = 
  			protrusionLengthBasalSum*this.deltaXY/protrusionCount;
  	
  	// create polygon defined by indentation region border points
  	MTBPolygon2D poly = new MTBPolygon2D(nonProtrusionAreaPolyPoints, true);
  	int[][] polyMask = poly.getBinaryMask(this.width, this.height);
	
  	// estimate non-protrusion area, 
  	// which is region area without protrusions
  	int nonProtrusionArea = 0;
  	for (int y=0;y<this.height;++y) {
  		for (int x=0;x<this.width;++x) {
  			// check if pixel is inside polygon and inside cell
  			if (   polyMask[y][x] > 0 
  					&& this.labelImg.getValueInt(x, y) == (contourID+1)) {
  				++nonProtrusionArea;
  				
  				// mark non-lobe area in image
//  				if (this.createCurvatureInfoImage) {
//  					this.curvatureInfoImg.putValueR(x, y, 125);
//  					this.curvatureInfoImg.putValueG(x, y, 125);
//  					this.curvatureInfoImg.putValueB(x, y, 125);                                               
//  				}
  			}
  		}
  	}
		// calculate ratio of non-lobe area in cell
//		this.nonProtrusionAreas.add(new Double(nonProtrusionArea
//				*this.deltaXY.doubleValue()*this.deltaXY.doubleValue()));
  	levelResult.nonProtrusionArea = 
  			nonProtrusionArea * this.deltaXY * this.deltaXY;
	}
	
	/**
	 * Calculate various distances between indentations midpoints.
	 * 
	 * @param c									Contour to process.
	 * @param contourID					ID of the contour.
	 * @param protrusionCount		Number of detected protrusions.
	 * @param iSegs							Indentation segments to process.
	 */
	private void measureIndentationDistances(
			CurvatureAnalysisLevelResult levelResult) {
			
		MTBContour2D c = levelResult.contour;
		int contourID = levelResult.contourID;
		int protrusionCount = levelResult.numberOfProtrusions;
		LinkedList<LinkedList<Point2D.Double>> iSegs = 
				levelResult.indentationSegs;
		
		// collect all mid-points
		LinkedList<Point2D.Double> iMidPoints = new LinkedList<>();
		for (int n=0; n<iSegs.size(); ++n) {
			LinkedList<Point2D.Double> iPoints = iSegs.get(n);
			Point2D.Double iMidPoint = iPoints.get(iPoints.size()/2);
			iMidPoints.add(iMidPoint);
		}

		// calculate all pairwise distances, for each point extract min and max
		double[] minDists = new double[iMidPoints.size()];
		double[] maxDists = new double[iMidPoints.size()];
		for (int i=0; i<iMidPoints.size(); ++i)
			minDists[i] = Double.MAX_VALUE;

		double dist, totalDist = 0;
		int totalCount = 0;
		for (int m=0; m<iMidPoints.size();++m) {
			for (int n=m+1; n<iMidPoints.size(); ++n) {
				dist = iMidPoints.get(m).distance(iMidPoints.get(n));
				totalDist += dist;
				++totalCount;

				if (dist < minDists[m])
					minDists[m] = dist;
				if (dist < minDists[n])
					minDists[n] = dist;
				if (dist > maxDists[m])
					maxDists[m] = dist;
				if (dist > maxDists[n])
					maxDists[n] = dist;
			}
		}
		
		double totalMinMin = Double.MAX_VALUE;
		double totalMinMax = 0;
		for (int i=0; i<iMidPoints.size(); ++i) {
			if (minDists[i] < totalMinMin) 
				totalMinMin = minDists[i];
			if (minDists[i] > totalMinMax)
				totalMinMax = minDists[i];
		}
		
//		this.avgDistsIndentationMidPoints.add(
//				new Double(totalDist*this.deltaXY.doubleValue()/totalCount));
//		this.minMinimalDistsIndentationMidPoints.add(
//				new Double(totalMinMin*this.deltaXY.doubleValue()));
//		this.maxMinimalDistsIndentationMidPoints.add(
//				new Double(totalMinMax*this.deltaXY.doubleValue()));
		
		levelResult.avgDistsIndentationMidPoints = 
			totalDist * this.deltaXY / totalCount;
		levelResult.minMinimalDistsIndentationMidPoints = 
			totalMinMin*this.deltaXY;
		levelResult.maxMinimalDistsIndentationMidPoints =
			totalMinMax*this.deltaXY;
	}

	/**
	 * Post-process protrusion segments to get more information on indentations.
	 * <p>
	 * Here the boundary points of protrusion regions are determined and
	 * different length measures are extracted.
	 * 
	 * @param c									Contour to process.
	 * @param contourID					ID of the contour.
	 * @param inflections				Set of inflection points.
	 * @param protrusionCount		Number of detected protrusions.
	 * @param indentationSegs		Protrusion segments to process.
	 */
	private void postprocessProtrusionSegments(
			CurvatureAnalysisLevelResult levelResult) {
		
		MTBContour2D c = levelResult.contour;
		int contourID = levelResult.contourID;
		LinkedList<Point2D.Double> inflections = levelResult.inflections;
		int protrusionCount = levelResult.numberOfProtrusions;
		LinkedList<LinkedList<Point2D.Double>> protrusionSegs =
				levelResult.protrusionSegs;
		
		double indentationBaselineSum = 0;
		double indentationLengthSum = 0;
		double indentationLengthApicalSum = 0;
		double indentationLengthBasalSum = 0;
		
  	for (int n=0; n<protrusionSegs.size(); ++n) {
  		
  		LinkedList<Point2D.Double> protrusion = protrusionSegs.get(n);
			LinkedList<Point2D.Double> nextProtrusion;
  		if (n == protrusionSegs.size()-1 )
  			nextProtrusion = protrusionSegs.get(0);
  		else
  			nextProtrusion = protrusionSegs.get(n+1);

  		Point2D.Double protrusionMidPoint = 
  				protrusion.get(protrusion.size()/2);
			int nmpx = (int)protrusionMidPoint.x;
			int nmpy = (int)protrusionMidPoint.y;
  		
  		Point2D.Double nextProtrusionMidPoint = 
  				nextProtrusion.get(nextProtrusion.size()/2);
			int nnmpx = (int)nextProtrusionMidPoint.x;
			int nnmpy = (int)nextProtrusionMidPoint.y;
  			    		
  		// draw middle point to image
			if (this.debugInfoImg != null) {
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(nmpx+dx, nmpy+dy, 255);
							this.debugInfoImg.putValueG(nmpx+dx, nmpy+dy, 255);
							this.debugInfoImg.putValueB(nmpx+dx, nmpy+dy, 255);
						}
					}						
				}
			}

			// check if baseline intersects with background
			MTBLineSegment2D baseline = 
					new MTBLineSegment2D(nmpx, nmpy, nnmpx, nnmpy);
			LinkedList<Point2D.Double> pixelList = 
					baseline.getPixelsAlongSegment();
			
			boolean insideCell = false;
			int pixInside = 0;
			for (Point2D.Double q: pixelList) {
				if (this.labelImg.getValueInt((int)q.x, (int)q.y) == (contourID+1)) {
					insideCell = true;
					++pixInside;
				}
			}
			
			Point2D.Double newStartPoint = protrusionMidPoint;
			Point2D.Double newEndPoint = nextProtrusionMidPoint;
			
			// there are pixels out of the region area, shift points
			if (insideCell) {
				
				int nPixInside = 0;
				int minInside = pixInside;
				int shift, totalShift = Integer.MAX_VALUE;

				// shift start point
				for (int ps = protrusion.size()/2; ps < protrusion.size(); ++ps) {
					Point2D.Double tps = protrusion.get(ps);
					for (int pe = nextProtrusion.size()/2; pe >= 0; --pe) {
						Point2D.Double tpe = nextProtrusion.get(pe);

						baseline = new MTBLineSegment2D(
								(int)tps.x, (int)tps.y,	(int)tpe.x, (int)tpe.y);
						pixelList = baseline.getPixelsAlongSegment();
						nPixInside = 0;
						for (Point2D.Double q: pixelList) {
							if (this.labelImg.getValueInt((int)q.x, (int)q.y) == (contourID+1)) {
								++nPixInside;
							}
						}
						// check for minimum of segment pixels inside region
						shift = ((nextProtrusion.size()/2) - pe)
								+ (ps - (protrusion.size()/2));
						if (     nPixInside < minInside 
								|| ((nPixInside == minInside) && shift < totalShift)) {
							minInside = nPixInside;
							totalShift = shift;
							newStartPoint = tps;
							newEndPoint = tpe;
						}
					}
				}
			} // end of if-clause for protrusion region optimization
			
			// sum lengths of baselines
			indentationBaselineSum += newStartPoint.distance(newEndPoint);
			
			Line2D.Double baseLine = new Line2D.Double(
				newStartPoint.x, newStartPoint.y, newEndPoint.x, newEndPoint.y);
			
			Vector<Point2D.Double> cPoints = c.getPoints();
			Vector<Point2D.Double> iPoints = new Vector<>();
			int sID = cPoints.indexOf(newStartPoint);
			int eID = cPoints.indexOf(newEndPoint);
			
			// check if segment overlaps contour start/end
			int lastID = (eID < sID ? cPoints.size() : eID); 
			double maxDist = 0, dist;
			Point2D.Double maxDistPoint = newStartPoint, cp;
			for (int id = sID+1; id<lastID; ++id) {
				cp = cPoints.get(id);
				dist = baseLine.ptLineDist(cp);
				if (dist > maxDist) {
					maxDist = dist;
					maxDistPoint = cp;
				}
				if (inflections.contains(cp))
					iPoints.add(cp);
			}
			if (eID < sID) {
				for (int id = 0; id<eID; ++id) {
					cp = cPoints.get(id);
					dist = baseLine.ptLineDist(cp);
					if (dist > maxDist) {
						maxDist = dist;
						maxDistPoint = cp;
					}
					if (inflections.contains(cp))
						iPoints.add(cp);
				}					
			}
			if (inflections.contains(newEndPoint))
				iPoints.add(newEndPoint);
			
			indentationLengthSum += maxDist;		
			
			// calculate base point of distance line
			double vx = newEndPoint.x - newStartPoint.x;
			double vy = newEndPoint.y - newStartPoint.y;
			double spx = maxDistPoint.x - newStartPoint.x;
			double spy = maxDistPoint.y - newStartPoint.y;
			double blength = vx*vx + vy*vy;
			double plength = (vx*spx + vy*spy) / blength;
			
			double ppx = newStartPoint.x + plength * vx;
			double ppy = newStartPoint.y + plength * vy;

			// calculation intersection of distance line and equator
			if (iPoints.size() == 2) {
				Point2D.Double p1 = iPoints.get(0);
				Point2D.Double p2 = iPoints.get(1);
				MTBLineSegment2D equator = 
						new MTBLineSegment2D(p1.x, p1.y, p2.x, p2.y);
				MTBLineSegment2D distline = 
						new MTBLineSegment2D(ppx, ppy, maxDistPoint.x, maxDistPoint.y);
				Point2D.Double isect = equator.getIntersection(distline);
				if (!Double.isNaN(isect.x) && !Double.isNaN(isect.y)) {
					indentationLengthApicalSum += isect.distance(maxDistPoint);
					indentationLengthBasalSum += isect.distance(ppx, ppy);
				}
			}
			else {
				System.err.println("Error: found " + iPoints.size() + " inflection points...!");
				for (Point2D.Double p : iPoints) {
					System.out.println(p.x + " , " + p.y);
				}
				System.out.println("Done");
			}
			
			if (this.debugInfoImg != null) {
				int px = (int)newStartPoint.x;
				int py = (int)newStartPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				px = (int)newEndPoint.x;
				py = (int)newEndPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				// draw the (new) segment
				this.debugInfoImg.drawLine2D(
						(int)newStartPoint.x, (int)newStartPoint.y,
						(int)newEndPoint.x, (int)newEndPoint.y, 0x00FFFF00);
				
				// draw max. distance point
				px = (int)maxDistPoint.x;
				py = (int)maxDistPoint.y;
				for (int dy=-1;dy<=1;++dy) {
					for (int dx=-1;dx<=1;++dx) {
						if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
							this.debugInfoImg.putValueR(px+dx, py+dy, 0);
							this.debugInfoImg.putValueG(px+dx, py+dy, 0);
							this.debugInfoImg.putValueB(px+dx, py+dy, 0);
						}
					}						
				}
				this.debugInfoImg.drawLine2D((int)ppx, (int)ppy, 
						(int)maxDistPoint.x, (int)maxDistPoint.y, 0x00000000);

			} // end of optional drawing section
			
  	} // end of for-loop over all protrusion regions			
  	
//  	this.avgIndentationLengths.add(new Double(
//    	indentationLengthSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgBaselineIndentationLengths.add(new Double(
//  		indentationBaselineSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgApicalIndentationLengths.add(new Double(
//    	indentationLengthApicalSum*this.deltaXY.doubleValue()/protrusionCount));
//  	this.avgBasalIndentationLengths.add(new Double(
//  		indentationLengthBasalSum*this.deltaXY.doubleValue()/protrusionCount));

  	levelResult.avgIndentationLength = 
 			indentationLengthSum*this.deltaXY/protrusionCount;
  	levelResult.avgBaselineIndentationLength = 
 			indentationBaselineSum*this.deltaXY/protrusionCount;
  	levelResult.avgApicalIndentationLength = 
 			indentationLengthApicalSum*this.deltaXY/protrusionCount;
  	levelResult.avgBasalIndentationLength = 
  		indentationLengthBasalSum*this.deltaXY/protrusionCount;

	}

	/**
	 * Function to remove all sub-sequences of ones shorter than the given
	 * minimum length by replacing them with -1.
	 * 
	 * @param dirArray	Array to modify.
	 * @param minLength	Minimal required length of sequences of ones.
	 */
	protected static void removeShortProtrusions(int[] dirArray, int minLength) {
		
		// iterate over the array until nothing changes anymore
  	int startPos = 0;
  	boolean changedSomething = true;
  	while (changedSomething) {
  		changedSomething = false;
  		
  		// get sign of first entry
  		int sign = dirArray[0];
  		
  		// remember start position of current sequence ...
  		startPos = 0;
  		
  		// ... and its length
  		int pixCount = 1;
  		
  		int j=1;
  		for (j=1; j<dirArray.length; ++j) {
  			// count following entries with identical sign
  			if (dirArray[j] == sign) {
  				++pixCount;
  			}
  			else {
  				// sign changes, but was a run of '-1' -> not of interest
  				if (sign == -1) {
  					pixCount = 1;
  					sign *= -1;
  					startPos = j;
  				}
  				// sign changes, check if run was long enough
  				// (if run is prefix of array, skip for now, we will test it later)
  				else {
  					if (    pixCount >= minLength 
  							|| (startPos == 0 && dirArray[dirArray.length-1] == sign)) {
  						// everything ok, just continue
  						sign *= -1;
  						pixCount = 1;
  						startPos = j;
  					}
  					else {
  						// lobe too small, remove it
  						sign *= -1;
  						for (int m=0;m<pixCount;++m) {
  							dirArray[startPos+m] = sign; 
  						}
  						changedSomething = true;
  						break;
  					}
  				}
  			}
  		}
  		// if we are at the end and in a 1-run, check if we can continue it
  		// at the beginning of the array, if not, remove it (if too short)
  		if (sign == 1 && j == dirArray.length && pixCount < minLength) {
  			int z=0;
  			for (z=0; z<dirArray.length; ++z) {
  				if (dirArray[z] == sign) {
  					++pixCount;
  				}
  				else {
  					break;
  				}
  			}
  			if (pixCount < minLength) {
  				sign *= -1;
  				for (int m=startPos;m<dirArray.length;++m) {
						dirArray[m] = sign; 
					}
  				for (int y=0;y<z;++y) {
						dirArray[y] = sign; 
					}
  			}
  		}
  	}		
	}
}
