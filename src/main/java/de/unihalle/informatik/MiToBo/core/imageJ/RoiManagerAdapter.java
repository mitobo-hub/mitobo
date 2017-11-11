/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010
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

import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.frame.RoiManager;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.*;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.*;

/**
 * Convenience functions for interaction with ImageJ's ROI manager.
 * <p>
 * This class is implemented as singleton. To access its functionality, use the
 * getAdapter() function to get the singleton instance and call its methods.
 * 
 * @author moeller
 */
public class RoiManagerAdapter {

		/**
		 * The object instance.
		 */
		private static RoiManagerAdapter roiAdapt = new RoiManagerAdapter();

		/**
		 * Lock variable to make singleton thread-safe.
		 */
		private static Object classLock = RoiManager.class;

		/**
		 * Reference to the current ImageJ roi manager.
		 */
		private RoiManager roiManager = null;

		/**
		 * Hidden constructor, never called from outside.
		 */
		private RoiManagerAdapter() {
				// nothing to do here
		}

		/**
		 * Single access point for singleton functionality.
		 * 
		 * @return Reference to the singleton instance.
		 */
		public static RoiManagerAdapter getInstance() {
				synchronized (classLock) {
						return roiAdapt;
				}
		}

		/**
		 * Adds the given polygon to the ROI manager.
		 * 
		 * @param poly Polygon to be added to ROI manager.
		 */
		public void addPolygonToRoiManager(MTBPolygon2D poly) {
			// get the current ROI manager from ImageJ environment
			openRoiManager();
			
			// get ImageJ polygon ROI
			PolygonRoi pr = poly.convertToImageJRoi()[0];

			// add to ROI manager
			this.roiManager.addRoi(pr);
		}

		/**
		 * Adds a set of polygons to the ROI manager.
		 * <p>
		 * Note that the set is destroyed by this operation, i.e. it cannot be
		 * recovered lateron from the ROI manager as it does not support to group
		 * several polygons into a logic entity.
		 * 
		 * @param polys
		 *          Set of polygons to be added to ROI manager.
		 */
		public void addPolygonsToRoiManager(MTBPolygon2DSet polys) {
				// for (MTBPolygon2D poly: polys)
				// addPolygonToRoiManager(poly);
				openRoiManager();
				MTBPolygon2DSetROI rois = 
						new MTBPolygon2DSetROI(polys, "MTBPolygon2DSet");
				this.roiManager.addRoi(rois);
		}

		/**
		 * Adds a set of 2D regions to the ROI manager.
		 * 
		 * @param regs
		 *          Set of regions to be added to ROI manager.
		 */
		public void addRegionsToRoiManager(MTBRegion2DSet regs) {
				openRoiManager();
				MTBRegion2DSetROI rois = 
						new MTBRegion2DSetROI(regs, "MTBRegion2DSet");
				this.roiManager.addRoi(rois);
		}

		/**
		 * Adds a set of 2D contours to the ROI manager.
		 * 
		 * @param conts
		 *          Set of contours to be added to ROI manager.
		 */
		public void addContoursToRoiManager(MTBContour2DSet conts) {
				openRoiManager();
				MTBContour2DSetROI rois = 
						new MTBContour2DSetROI(conts, "MTBContour2DSet");
				this.roiManager.addRoi(rois);
		}
		
		/**
		 * Adds a set of 2D borders to the ROI manager.
		 * 
		 * @param borders
		 *          Set of borders to be added to ROI manager.
		 */
		public void addBordersToRoiManager(MTBBorder2DSet borders) {
				openRoiManager();
				MTBBorder2DSetROI rois = 
						new MTBBorder2DSetROI(borders, "MTBBorder2DSet");
				this.roiManager.addRoi(rois);
		}
		
		/**
		 * Reads an ImageJ roi file into a set of regions.
		 * 
		 * @param file	Input file.
		 * @return Set of regions; always non-null, but probably empty.
		 * @throws ALDOperatorException Thrown in case of failure.
		 * @throws ALDProcessingDAGException Thrown in case of failure.
		 */
		public MTBRegion2DSet getRegionSetFromRoiFile(String file) 
				throws ALDOperatorException, ALDProcessingDAGException {
			RoiReader reader = new RoiReader(file, RoiReader.TargetFormat.REGIONS);
			reader.doUseSpecifiedDomain(false);
			reader.runOp(false);
			return reader.getRegionSet();
		}

		/**
		 * Reads an ImageJ roi file into a set of regions.
		 * 
		 * @param file	Input file.
		 * @param xMin	Minimal x value of user-specified domain.
		 * @param yMin	Minimal y value of user-specified domain.
		 * @param xMax	Maximal x value of user-specified domain.
		 * @param yMax	Maximal y value of user-specified domain.
		 * @return Set of regions; always non-null, but probably empty.
		 * @throws ALDOperatorException Thrown in case of failure.
		 * @throws ALDProcessingDAGException Thrown in case of failure.
		 */
		public MTBRegion2DSet getRegionSetFromRoiFile(String file, 
			double xMin, double yMin, double xMax, double yMax) 
				throws ALDOperatorException, ALDProcessingDAGException {
			RoiReader reader = new RoiReader(file, RoiReader.TargetFormat.REGIONS);
			reader.doUseSpecifiedDomain(true);
			reader.setDomainXMin(xMin);
			reader.setDomainYMin(yMin);
			reader.setDomainXMax(xMax);
			reader.setDomainYMax(yMax);
			reader.runOp(false);
			return reader.getRegionSet();
		}

		/**
		 * Reads an ImageJ roi file into a set of polygons/snakes.
		 * 
		 * @param file
		 *          Input file.
		 * @param asSnakes
		 *          If true, function returns snakes, otherwise polygons.
		 * @return Set of polygons/snakes; always non-null, but probably empty.
		 * @throws ALDOperatorException Thrown in case of failure.
		 * @throws ALDProcessingDAGException Thrown in case of failure.
		 */
		public MTBPolygon2DSet getPolygonSetFromRoiFile(String file, boolean asSnakes)
				throws ALDOperatorException, ALDProcessingDAGException {
			RoiReader reader;
			if (asSnakes)
				reader = 
					new RoiReader(file,RoiManagerAdapter.RoiReader.TargetFormat.SNAKES);
			else
				reader = 
					new RoiReader(file,RoiManagerAdapter.RoiReader.TargetFormat.POLYGONS);
			reader.runOp(false);
			return reader.getPolySet();
		}

		/**
		 * Reads ROI manager selections into a set of line segments.
		 * <p>
		 * Note that polylines are not automatically closed, i.e. the first and
		 * last point are not identical even if ImageJ shows a closed polygon.
		 *  
		 * @return Set of line segments; always non-null, but probably empty.
		 */
		public MTBLineSegment2DSet getLineSegmentSetFromRoiManager() {
			// get the current ROI manager from ImageJ environment
			openRoiManager();
			if (!this.checkSelectionConsistencyForLineSegments())
				return new MTBLineSegment2DSet();
			Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();

			// init result data object
			MTBLineSegment2DSet lSet = new MTBLineSegment2DSet();
			
			/*
			 * only a single entry selected
			 */
			if (roiArray.length == 1) {
				// safety check
				if (   roiArray[0] instanceof MTBContour2DSetROI
						|| roiArray[0] instanceof MTBRegion2DSetROI
						|| roiArray[0] instanceof MTBBorder2DSetROI
						|| roiArray[0] instanceof MTBPolygon2DSetROI) {
					return new MTBLineSegment2DSet();
				}
				// get selected ROI
				Roi selection = roiArray[0];
				if (selection instanceof PolygonRoi) {
					Polygon poly = ((PolygonRoi)selection).getPolygon();
					for (int i = 0; i < poly.npoints-1; ++i) {
						MTBLineSegment2D l = new MTBLineSegment2D(
							poly.xpoints[i], poly.ypoints[i],
								poly.xpoints[i+1], poly.ypoints[i+1]);
						l.setWidth(selection.getStrokeWidth());
						// default width in ImageJ is zero, we want 1
						if (l.getWidth() == 0)
							l.setWidth(1.0);
						lSet.add(l);
					}
				}
				else if (selection instanceof Line) {
					Line lSel = (Line)selection;
					MTBLineSegment2D l = new MTBLineSegment2D(
						lSel.x1d, lSel.y1d, lSel.x2d, lSel.y2d);
					l.setWidth(lSel.getStrokeWidth());
					// default width in ImageJ is zero, we want 1
					if (l.getWidth() == 0)
						l.setWidth(1.0);
					lSet.add(l);
				}
				return lSet;
			}

			/*
			 * more than one entry has been selected
			 */
			for (Roi r : roiArray) {
				if (r instanceof PolygonRoi) {
					Polygon poly = ((PolygonRoi)r).getPolygon();
					for (int i = 0; i < poly.npoints-1; ++i) {
						MTBLineSegment2D l = new MTBLineSegment2D(
								poly.xpoints[i], poly.ypoints[i],
								poly.xpoints[i+1], poly.ypoints[i+1]);
						l.setWidth(r.getStrokeWidth());
						// default width in ImageJ is zero, we want 1
						if (l.getWidth() == 0)
							l.setWidth(1.0);
						lSet.add(l);
					}
				}
				else if (r instanceof Line) {
					Line lSel = (Line)r;
					MTBLineSegment2D l = new MTBLineSegment2D(
							lSel.x1d, lSel.y1d, lSel.x2d, lSel.y2d);
					l.setWidth(lSel.getStrokeWidth());
					// default width in ImageJ is zero, we want 1
					if (l.getWidth() == 0)
						l.setWidth(1.0);
					lSet.add(l);
				}
			}
			return lSet;
		}

		/**
		 * Reads ROI manager selections into a set of polygons/snakes.
		 * 
		 * @return Set of polygons; always non-null, but probably empty.
		 */
		public MTBPolygon2DSet getPolygonSetFromRoiManager() {
				// get the current ROI manager from ImageJ environment
				openRoiManager();
				if (!this.checkSelectionConsistency())
						return new MTBPolygon2DSet(0, 0, 0, 0);
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();

				/*
				 * only a single entry selected
				 */
				// check if only one entry was selected
				if (roiArray.length == 1) {
						if (roiArray[0] instanceof MTBPolygon2DSetROI) {
								MTBPolygon2DSetROI polygonRoi = (MTBPolygon2DSetROI) roiArray[0];
								return (polygonRoi.getPolygonSet());
						}
						// safety check
						if (roiArray[0] instanceof MTBContour2DSetROI
						    || roiArray[0] instanceof MTBRegion2DSetROI
						    || roiArray[0] instanceof MTBBorder2DSetROI)
								return new MTBPolygon2DSet(0, 0, 0, 0);
						// get selected ROI
						Vector<MTBPolygon2D> polyVec = new Vector<MTBPolygon2D>();
						double xmax = 0, ymax = 0;
						Roi selection = roiArray[0];
						Polygon selPoly = selection.getPolygon();
						Vector<Point2D.Double> points = new Vector<Point2D.Double>();
						for (int i = 0; i < selPoly.npoints; i++) {
								double x = selPoly.xpoints[i];
								double y = selPoly.ypoints[i];
								points.addElement(new Point2D.Double(x, y));
								if (y > ymax)
										ymax = y;
								if (x > xmax)
										xmax = x;
						}
						// create polygon
						MTBPolygon2D polygon = new MTBPolygon2D(points, true);
						// add polygon to polygon vector
						polyVec.add(polygon);
						// create set from the current polygon
						MTBPolygon2DSet polygonSet = new MTBPolygon2DSet(polyVec, 0, 0, xmax,
						    ymax);
						return polygonSet;
				}

				/*
				 * more than one MTBPolygon2DSet or many polygons selected
				 */
				// count objects of type MTBPolygon2DSet
				int selectedSets = 0;
				for (Roi r : roiArray) {
						if (r instanceof MTBPolygon2DSetROI)
								selectedSets++;
				}
				if (selectedSets == 1) {
						for (Roi r : roiArray) {
								if (r instanceof MTBPolygon2DSetROI) {
										MTBPolygon2DSetROI polygonRoi = (MTBPolygon2DSetROI) r;
										return (polygonRoi.getPolygonSet());
								}
						}
				} else if (selectedSets > 1) {
						Object[] options = { "OK", "CANCEL" };
						int ret = JOptionPane
						    .showOptionDialog(
						        null,
						        "Attention! You have selected more than one MTBPolygon2DSet!\n"
						            + "Only the first one will be returned to the operator.\n"
						            + "If this is not what you intended, cancel and change your selection!",
						        "Warning", JOptionPane.OK_CANCEL_OPTION,
						        JOptionPane.WARNING_MESSAGE, null, options, options[1]);
						if (ret != JOptionPane.OK_OPTION) {
								return new MTBPolygon2DSet(0, 0, 0, 0);
						}
						// return the first selected polygon set
						for (Roi r : roiArray) {
								if (r instanceof MTBPolygon2DSetROI) {
										MTBPolygon2DSetROI polygonRoi = (MTBPolygon2DSetROI) r;
										return (polygonRoi.getPolygonSet());
								}
						}
				} else {
						// get all ROIs and put them into a polygon set
						Vector<MTBPolygon2D> polyVec = new Vector<MTBPolygon2D>();
						double xmax = 0, ymax = 0;
						for (int n = 0; n < roiArray.length; ++n) {
								Roi selection = roiArray[n];
								// safety check, do not convert our own ROI data types!
								if (selection instanceof MTBRegion2DSetROI
								    || selection instanceof MTBContour2DSetROI)
										return new MTBPolygon2DSet(0, 0, 0, 0);
								Polygon selPoly = selection.getPolygon();
								Vector<Point2D.Double> points = new Vector<Point2D.Double>();
								for (int i = 0; i < selPoly.npoints; i++) {
										double x = selPoly.xpoints[i];
										double y = selPoly.ypoints[i];
										points.addElement(new Point2D.Double(x, y));
										if (y > ymax)
												ymax = y;
										if (x > xmax)
												xmax = x;
								}
								// create polygon
								MTBPolygon2D polygon = new MTBPolygon2D(points, true);
								// add polygon to polygon vector
								polyVec.add(polygon);
						}
						// create set of polygons
						MTBPolygon2DSet polygonSet = new MTBPolygon2DSet(polyVec, 0, 0, xmax,
						    ymax);
						return polygonSet;
				}
				return new MTBPolygon2DSet(0, 0, 0, 0);
		}

		/**
		 * Reads a ROI manager selection into a set of regions.
		 * 
		 * @return Set of regions; always non-null, but probably empty.
		 */
		public MTBRegion2DSet getRegionSetFromRoiManager() {
				openRoiManager();
				if (!this.checkSelectionConsistency())
						return new MTBRegion2DSet(0, 0, 0, 0);
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();

				/*
				 * only a single entry selected
				 */
				if (roiArray.length == 1) {
						if (roiArray[0] instanceof MTBRegion2DSetROI) {
								MTBRegion2DSetROI regs = (MTBRegion2DSetROI) roiArray[0];
								return regs.getRegionSet();
						}
						// safety check
						if (roiArray[0] instanceof MTBContour2DSetROI
						    || roiArray[0] instanceof MTBPolygon2DSetROI
						    || roiArray[0] instanceof MTBBorder2DSetROI)
								return new MTBRegion2DSet(0, 0, 0, 0);
						// get polygon ROI
						MTBPolygon2DSet polygonSet = this.getPolygonSetFromRoiManager();
						if (polygonSet.size() == 0)
								return new MTBRegion2DSet(0, 0, 0, 0);
						int width = (int) (polygonSet.getXmax() + 0.5) + 1;
						int height = (int) (polygonSet.getYmax() + 0.5) + 1;
						Vector<MTBRegion2D> regVec = new Vector<MTBRegion2D>();
						for (int n = 0; n < polygonSet.size(); ++n) {
								MTBPolygon2D poly = polygonSet.elementAt(n);
								
								// try to figure out if we have a polygon represented by a 
								// complete list of all contour pixels, e.g., a MiToBo contour; 
								// if so we have to ensure that during mask generation all 
								// polygon pixels really become part of the final mask which 
								// does not automatically happen; most likely this is due to the 
								// sweeping algorithm of ImageJ used for that which might have 
								// problems with polygons represented by complete pixel lists...
								boolean realPolygon = false;
								Vector<Point2D.Double> points = poly.getPoints();
								if (  points.get(0).distance(points.get(points.size()-1)) 
										> Math.sqrt(2))
									realPolygon = true;
								for (int i=1; !realPolygon && i<points.size();++i) {
									if (points.get(i).distance(points.get(i-1)) > Math.sqrt(2))
										realPolygon = true;							
								}

								int[][] mask = 
										poly.getBinaryMask(width, height, !realPolygon);
								MTBRegion2D region = new MTBRegion2D();
								for (int y = 0; y < height; ++y) {
										for (int x = 0; x < width; ++x) {
												if (mask[y][x] > 0)
														region.addPixel(new Point2D.Double(x, y));
										}
								}
								// add region to region vector
								regVec.add(region);
						}
						// create region set from vector
						MTBRegion2DSet regionSet = new MTBRegion2DSet(regVec, 0, 0, polygonSet
						    .getXmax(), polygonSet.getYmax());
						return regionSet;
				}

				/*
				 * more than one MTBRegion2DSet or many regions/polygons selected
				 */
				// count objects of type MTBPolygon2DSet
				int selectedSets = 0;
				for (Roi r : roiArray) {
						if (r instanceof MTBRegion2DSetROI)
								selectedSets++;
				}
				if (selectedSets == 1) {
						for (Roi r : roiArray) {
								if (r instanceof MTBRegion2DSetROI) {
										MTBRegion2DSetROI regs = (MTBRegion2DSetROI) r;
										return regs.getRegionSet();
								}
						}
				} else if (selectedSets > 1) {
						Object[] options = { "OK", "CANCEL" };
						int ret = JOptionPane
						    .showOptionDialog(
						        null,
						        "Attention! You have selected more than one MTBRegion2DSet!\n"
						            + "Only the first one will be returned to the operator.\n"
						            + "If this is not what you intended, cancel and change your selection!",
						        "Warning", JOptionPane.OK_CANCEL_OPTION,
						        JOptionPane.WARNING_MESSAGE, null, options, options[1]);
						if (ret != JOptionPane.OK_OPTION) {
								return new MTBRegion2DSet(0, 0, 0, 0);
						}
						// return the first selected polygon set
						for (Roi r : roiArray) {
								if (r instanceof MTBRegion2DSetROI) {
										MTBRegion2DSetROI regionRoi = (MTBRegion2DSetROI) r;
										return regionRoi.getRegionSet();
								}
						}
				} else {
						// get polygon ROIs
						MTBPolygon2DSet polygonSet = this.getPolygonSetFromRoiManager();
						if (polygonSet.size() == 0)
								return new MTBRegion2DSet(0, 0, 0, 0);
						int width = (int) (polygonSet.getXmax() + 0.5) + 1;
						int height = (int) (polygonSet.getYmax() + 0.5) + 1;
						Vector<MTBRegion2D> regVec = new Vector<MTBRegion2D>();
						for (int n = 0; n < polygonSet.size(); ++n) {
								MTBPolygon2D poly = polygonSet.elementAt(n);
								
								// try to figure out if we have a polygon represented by a 
								// complete list of all contour pixels, e.g., a MiToBo contour; 
								// if so we have to ensure that during mask generation all 
								// polygon pixels really become part of the final mask which 
								// does not automatically happen; most likely this is due to the 
								// sweeping algorithm of ImageJ used for that which might have 
								// problems with polygons represented by complete pixel lists...
								boolean realPolygon = false;
								Vector<Point2D.Double> points = poly.getPoints();
								if (  points.get(0).distance(points.get(points.size()-1)) 
										> Math.sqrt(2))
									realPolygon = true;
								for (int i=1; !realPolygon && i<points.size();++i) {
									if (points.get(i).distance(points.get(i-1)) > Math.sqrt(2))
										realPolygon = true;							
								}

								int[][] mask = 
										poly.getBinaryMask(width, height, !realPolygon);
								MTBRegion2D region = new MTBRegion2D();
								for (int y = 0; y < height; ++y) {
										for (int x = 0; x < width; ++x) {
												if (mask[y][x] > 0)
														region.addPixel(new Point2D.Double(x, y));
										}
								}
								// add region to region vector
								regVec.add(region);
						}
						// create region set from vector
						MTBRegion2DSet regionSet = new MTBRegion2DSet(regVec, 0, 0, 
								polygonSet.getXmax(), polygonSet.getYmax());
						return regionSet;
				}
				return new MTBRegion2DSet(0, 0, 0, 0);
		}

		/**
		 * Reads a ROI manager selection into a set of contours.
		 * 
		 * @return Set of contours; always non-null, but probably empty.
		 */
		public MTBContour2DSet getContourSetFromRoiManager() {
				openRoiManager();
				if (!this.checkSelectionConsistency())
						return new MTBContour2DSet(0, 0, 0, 0);
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();

				/*
				 * only a single entry selected
				 */
				if (roiArray.length == 1) {
						if (roiArray[0] instanceof MTBContour2DSetROI) {
								MTBContour2DSetROI regs = (MTBContour2DSetROI) roiArray[0];
								return regs.getContourSet();
						}
						// safety check
						if (roiArray[0] instanceof MTBRegion2DSetROI
						    || roiArray[0] instanceof MTBPolygon2DSetROI
						    || roiArray[0] instanceof MTBBorder2DSetROI)
								return new MTBContour2DSet(0, 0, 0, 0);
						// get contour ROI
						MTBRegion2DSet regionSet = this.getRegionSetFromRoiManager();
						if (regionSet.size() == 0)
								return new MTBContour2DSet(0, 0, 0, 0);
						MTBContour2DSet contourSet = new MTBContour2DSet(0, 0, regionSet
						    .getXmax(), regionSet.getYmax());
						for (int i = 0; i < regionSet.size(); i++) {
								MTBContour2D contour = null;
								try {
										// get contour from selected polygon region
										contour = regionSet.elementAt(i).getContour();
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
								// add contour to contour set
								contourSet.add(contour);
						}
						return contourSet;
				}

				/*
				 * more than one MTBContour2DSet or many polygons selected
				 */
				// count objects of type MTBContour2DSet
				int selectedSets = 0;
				for (Roi r : roiArray) {
						if (r instanceof MTBContour2DSetROI)
								selectedSets++;
				}
				if (selectedSets == 1) {
						for (Roi r : roiArray) {
								if (r instanceof MTBContour2DSetROI) {
										MTBContour2DSetROI contRoi = (MTBContour2DSetROI) r;
										return contRoi.getContourSet();
								}
						}
				} else if (selectedSets > 1) {
						Object[] options = { "OK", "CANCEL" };
						int ret = JOptionPane
						    .showOptionDialog(
						        null,
						        "Attention! You have selected more than one MTBContour2DSet!\n"
						            + "Only the first one will be returned to the operator.\n"
						            + "If this is not what you intended, cancel and change your selection!",
						        "Warning", JOptionPane.OK_CANCEL_OPTION,
						        JOptionPane.WARNING_MESSAGE, null, options, options[1]);
						if (ret != JOptionPane.OK_OPTION) {
								return new MTBContour2DSet(0, 0, 0, 0);
						}
						// return the first selected contour set
						for (Roi r : roiArray) {
								if (r instanceof MTBContour2DSetROI) {
										MTBContour2DSetROI contRoi = (MTBContour2DSetROI) r;
										return contRoi.getContourSet();
								}
						}
				} else {
						MTBRegion2DSet regionSet = this.getRegionSetFromRoiManager();
						MTBContour2DSet contourSet = new MTBContour2DSet(0, 0, regionSet
						    .getXmax(), regionSet.getYmax());
						for (int i = 0; i < regionSet.size(); i++) {
								MTBContour2D contour = null;
								try {
										// get contour from selected polygon region
										contour = regionSet.elementAt(i).getContour();
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
								// add contour to contour set
								contourSet.add(contour);
						}
						return contourSet;
				}
				return new MTBContour2DSet(0, 0, 0, 0);
		}
		
		/**
		 * Reads a ROI manager selection into a set of borders.
		 * 
		 * @return Set of borders; always non-null, but probably empty.
		 */
		public MTBBorder2DSet getBorderSetFromRoiManager() {
				openRoiManager();
				if (!this.checkSelectionConsistency())
						return new MTBBorder2DSet(0, 0, 0, 0);
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();

				/*
				 * only a single entry selected
				 */
				if (roiArray.length == 1) {
						if (roiArray[0] instanceof MTBBorder2DSetROI) {
								MTBBorder2DSetROI borders = (MTBBorder2DSetROI) roiArray[0];
								return borders.getBorderSet();
						}
						// safety check
						if (roiArray[0] instanceof MTBRegion2DSetROI
						    || roiArray[0] instanceof MTBPolygon2DSetROI
						    || roiArray[0] instanceof MTBContour2DSetROI)
								return new MTBBorder2DSet(0, 0, 0, 0);
						// get border ROI
						MTBRegion2DSet regionSet = this.getRegionSetFromRoiManager();
						if (regionSet.size() == 0)
								return new MTBBorder2DSet(0, 0, 0, 0);
						MTBBorder2DSet borderSet = new MTBBorder2DSet(0, 0, regionSet
						    .getXmax(), regionSet.getYmax());
						for (int i = 0; i < regionSet.size(); i++) {
								MTBBorder2D border = null;
								try {
										// get border from selected polygon region
										border = regionSet.elementAt(i).getBorder();
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
								// add border to border set
								borderSet.add(border);
						}
						return borderSet;
				}

				/*
				 * more than one MTBBorder2DSet or many polygons selected
				 */
				// count objects of type MTBBorder2DSet
				int selectedSets = 0;
				for (Roi r : roiArray) {
						if (r instanceof MTBBorder2DSetROI)
								selectedSets++;
				}
				if (selectedSets == 1) {
						for (Roi r : roiArray) {
								if (r instanceof MTBBorder2DSetROI) {
										MTBBorder2DSetROI borderRoi = (MTBBorder2DSetROI) r;
										return borderRoi.getBorderSet();
								}
						}
				} else if (selectedSets > 1) {
						Object[] options = { "OK", "CANCEL" };
						int ret = JOptionPane
						    .showOptionDialog(
						        null,
						        "Attention! You have selected more than one MTBBorder2DSet!\n"
						            + "Only the first one will be returned to the operator.\n"
						            + "If this is not what you intended, cancel and change your selection!",
						        "Warning", JOptionPane.OK_CANCEL_OPTION,
						        JOptionPane.WARNING_MESSAGE, null, options, options[1]);
						if (ret != JOptionPane.OK_OPTION) {
								return new MTBBorder2DSet(0, 0, 0, 0);
						}
						// return the first selected border set
						for (Roi r : roiArray) {
								if (r instanceof MTBBorder2DSetROI) {
										MTBBorder2DSetROI borderRoi = (MTBBorder2DSetROI) r;
										return borderRoi.getBorderSet();
								}
						}
				} else {
						MTBRegion2DSet regionSet = this.getRegionSetFromRoiManager();
						MTBBorder2DSet borderSet = new MTBBorder2DSet(0, 0, regionSet
						    .getXmax(), regionSet.getYmax());
						for (int i = 0; i < regionSet.size(); i++) {
								MTBBorder2D border = null;
								try {
										// get border from selected polygon region
										border = regionSet.elementAt(i).getBorder();
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
								// add contour to border set
								borderSet.add(border);
						}
						return borderSet;
				}
				return new MTBBorder2DSet(0, 0, 0, 0);
		}
		
		/**
		 * Returns true if there are regions available in ROI manager.
		 * 
		 * @return True, if region manager contains at least one region.
		 */
		public boolean areRegionsInManagerAvailable() {
				RoiManager roiM = RoiManager.getInstance();
				if (roiM == null) {
						return false;
				}
				openRoiManager();
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();
				if (roiArray.length > 0)
						return true;
				return false;
		}

		/**
		 * Checks if a proper set of regions is selected in ImageJ ROI manager.
		 * @return True, if a proper set, i.e. at least one region, is
		 * 				 selected and the region(s) is/are of correct type.
		 */
		private boolean checkSelectionConsistency() {
				RoiManager roiM = RoiManager.getInstance();
				if (roiM == null) {
						return false;
				}
				openRoiManager();
				Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();
				if (roiArray == null || roiArray.length == 0)
						return false;
				Class<?> selectionType = roiArray[0].getClass();
				for (int n = 1; n < roiArray.length; ++n) {
						Roi r = roiArray[n];
						if (!(r.getClass().equals(selectionType))) {
								JOptionPane.showMessageDialog(null,
								    "ROI Manager does contain inconsistent selection..."
								        + "Please select only ROIs of a single type!", "Warning",
								    JOptionPane.WARNING_MESSAGE);
								return false;
						}
				}
				return true;
		}

		/**
		 * Checks if a proper set of ROIs convertible to line segments is 
		 * selected in ImageJ ROI manager.
		 * 
		 * @return True, if a proper set, i.e. at least one ROI, is
		 * 				 selected and the ROI(s) is/are of correct type.
		 */
		private boolean checkSelectionConsistencyForLineSegments() {
			RoiManager roiM = RoiManager.getInstance();
			if (roiM == null) {
				return false;
			}
			openRoiManager();
			Roi[] roiArray = this.roiManager.getSelectedRoisAsArray();
			if (roiArray == null || roiArray.length == 0)
				return false;
			for (int n = 0; n < roiArray.length; ++n) {
				Roi r = roiArray[n];
				if (!(r instanceof PolygonRoi || r instanceof Line)) {
					JOptionPane.showMessageDialog(null,
						"ROI Manager does contain inconsistent selection..."
							+ "Please select only ROIs convertible to line segments!", 
								"Warning", JOptionPane.WARNING_MESSAGE);
					return false;
				}
			}
			return true;
		}

		/**
		 * Class for parsing ImageJ roi files and zip archives.
		 * 
		 * @author moeller
		 */
		private static class RoiReader extends MTBOperator {

			/**
			 * Format into which the ROIs should be converted.
			 */
			public static enum TargetFormat {
				/**
				 * Convert into {@link MTBPolygon2D} and get a {@link MTBPolygon2DSet}.
				 */
				POLYGONS,
				/**
				 * Convert into {@link MTBSnake} and get a {@link MTBPolygon2DSet}.
				 */
				SNAKES,
				/**
				 * Convert into {@link MTBRegion2D} and get a {@link MTBRegion2DSet}.
				 */
				REGIONS
			}
			
			/**
			 * Input file name.
			 */
			@Parameter(label = "Input file", required = true, dataIOOrder = 0,
				direction = Direction.IN,	supplemental = false, 
				description = "Input ROI file.")
			private MTBStringData inFile = null;

			/**
			 * Target format.
			 */
			@Parameter(label = "Target format", required = true, dataIOOrder = 1,
				direction = Direction.IN, supplemental = false, 
				description = "Target format.")
			private TargetFormat format = TargetFormat.POLYGONS;

			/**
			 * Flag for using user-specified domain information.
			 */
			@Parameter(label = "Use user-specified domain?", required = true, 
				direction = Direction.IN, supplemental = false, 
				dataIOOrder = 10,	mode = ExpertMode.ADVANCED,
				description = "Flag for using user-specified domain.")
			private boolean useSpecifiedDomain = false;
			
			/**
			 * Minimal x coordinate of user-specified domain.
			 */
			@Parameter(label = "Minimal x", required = true, 
				direction = Direction.IN, supplemental = false, 
				dataIOOrder = 11, mode = ExpertMode.ADVANCED,
				description = "Minimal value in x of user-specified domain.")
			private double domainXmin; 

			/**
			 * Maximal x coordinate of user-specified domain.
			 */
			@Parameter(label = "Maximal x", required = true, 
				direction = Direction.IN, supplemental = false,
				dataIOOrder = 12, mode = ExpertMode.ADVANCED,
				description = "Maximal value in x of user-specified domain.")
			private double domainXmax; 

			/**
			 * Minimal y coordinate of user-specified domain.
			 */
			@Parameter(label = "Minimal y", required = true, 
				direction = Direction.IN, supplemental = false, 
				dataIOOrder = 13, mode = ExpertMode.ADVANCED,
				description = "Minimal value in y of user-specified domain.")
			private double domainYmin; 

			/**
			 * Maximal y coordinate of user-specified domain.
			 */
			@Parameter(label = "Maximal y", required = true, 
				direction = Direction.IN, supplemental = false,
				dataIOOrder = 14, mode = ExpertMode.ADVANCED,
				description = "Maximal value in y of user-specified domain.")
			private double domainYmax; 
			
			/**
			 * Result polygon set, if target format are polygons or snakes.
			 */
			@Parameter(label = "Polygon set", supplemental = false, 
				direction = Direction.OUT, description = "Output set of polygons.")
			private MTBPolygon2DSet polys = null;

			/**
			 * Result region set, if target format are regions.
			 */
			@Parameter(label = "Region set", supplemental = false, 
				direction = Direction.OUT, description = "Output set of regions.")
			private MTBRegion2DSet regions = null;

			/**
			 * Helper hash table.
			 */
			private Hashtable<String, Roi> roitable = new Hashtable<String, Roi>();

			/**
			 * Default constructor.
			 * 
			 * @param infile	Input filename.
			 * @param tFormat	Target format to return.
			 * @throws ALDOperatorException Thrown in case of failure.
			 */
			public RoiReader(String infile, TargetFormat tFormat)
					throws ALDOperatorException {
				MTBStringData input = new MTBStringData(infile);
				input.setLocation(infile);
				this.inFile = input;
				this.format = tFormat;
			}

			/**
			 * Enable/disable use of domain specified by the user.
			 * @param b		If true, domain usage is enabled, otherwise disabled.
			 */
			protected void doUseSpecifiedDomain(boolean b) {
				this.useSpecifiedDomain = b;
			}
			
			/**
			 * Set minimal x value of user-specified domain.
			 * @param xm	Minimal x value.
			 */
			protected void setDomainXMin(double xm) {
				this.domainXmin = xm;
			}

			/**
			 * Set maximal x value of user-specified domain.
			 * @param xm	Maximal x value.
			 */
			public void setDomainXMax(double xm) {
				this.domainXmax = xm;
			}
			
			/**
			 * Set minimal y value of user-specified domain.
			 * @param ym	Minimal y value.
			 */
			public void setDomainYMin(double ym) {
				this.domainYmin = ym;
			}

			/**
			 * Set maximal y value of user-specified domain.
			 * @param ym	Maximal y value.
			 */
			public void setDomainYMax(double ym) {
				this.domainYmax = ym;
			}

			/**
			 * Get the polygon set.
			 * 
			 * @return Polygon set read from file.
			 */
			public MTBPolygon2DSet getPolySet() {
				return this.polys;
			}

			/**
			 * Get the region set.
			 * 
			 * @return Region set read from file.
			 */
			public MTBRegion2DSet getRegionSet() {
				return this.regions;
			}

			@Override
			protected void operate() throws ALDOperatorException {
				
				switch(this.format)
				{
				case POLYGONS:
				case SNAKES:
					{
						// read all the snakes and put them into a snake set
						if (this.useSpecifiedDomain)
							this.polys = new MTBPolygon2DSet(	this.domainXmin, 
								this.domainYmin, this.domainXmax, this.domainYmax);
						else
							this.polys = new MTBPolygon2DSet(	0, 0, 0, 0);

						this.openRoiManagerFile(this.inFile.getString());
						if (this.roitable.size() == 0) {
							System.out.println("Hashtable empty!?");
							throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
									"SnakeFormatConverter: no roi's in zip file?!");
						}
						
						Set<String> keys = this.roitable.keySet();
						Object[] keyArray = keys.toArray();
						Arrays.sort(keyArray);

						// snakes or just polygons?
						if (this.format == TargetFormat.SNAKES) {
							for (Object k : keyArray) {
								Roi r = this.roitable.get(k);
								Polygon selPoly = r.getPolygon();
								Vector<MTBSnakePoint2D> points = new Vector<MTBSnakePoint2D>();
								for (int i = 0; i < selPoly.npoints; i++) {
									double x = selPoly.xpoints[i];
									double y = selPoly.ypoints[i];
									points.addElement(new MTBSnakePoint2D(x, y));
								}
								// define initial snake
								MTBSnake initialS = new MTBSnake(points, true);
								this.polys.add(initialS);
							}
						}
						// read all the polygons and put them into a set
						else {
							for (Object k : keyArray) {
								Roi r = this.roitable.get(k);
								Polygon selPoly = r.getPolygon();
								Vector<Point2D.Double> points = new Vector<Point2D.Double>();
								for (int i = 0; i < selPoly.npoints; i++) {
									double x = selPoly.xpoints[i];
									double y = selPoly.ypoints[i];
									points.addElement(new Point2D.Double(x, y));
								}
								// define initial snake
								MTBPolygon2D poly = new MTBPolygon2D(points, true);
								this.polys.add(poly);
							}
						}
					}
					break;
				case REGIONS:
					{
						// read all the regions and put them into a region set
						this.openRoiManagerFile(this.inFile.getString());
						if (this.roitable.size() == 0) {
							throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
									"[RoiManagerAdapter:RoiReader] no roi's in zip file?!");
						}
						Roi r;
						Polygon selPoly;
						MTBRegion2D region;
						MTBPolygon2D polygon;
						Vector<MTBRegion2D> tmpRegions = new Vector<MTBRegion2D>();
						Vector<Point2D.Double> points;
						int width, height;
						double domainMaxX = 0, domainMaxY = 0;
						double xmax = 0, ymax = 0;
						double x, y;
						
						// get key set and sort alpha-numerically
						Set<String> keys = this.roitable.keySet();
						Object[] keyArray = keys.toArray();
						Arrays.sort(keyArray);
						for (Object k : keyArray) {
							xmax = 0;
							ymax = 0;
							r = this.roitable.get(k);
							selPoly = r.getPolygon();
							points = new Vector<Point2D.Double>();
							for (int i = 0; i < selPoly.npoints; i++) {
									x = selPoly.xpoints[i];
									y = selPoly.ypoints[i];
									points.addElement(new Point2D.Double(x, y));
									if (y > ymax)
											ymax = y;
									if (x > xmax)
											xmax = x;
							}
							// check domain range
							if (xmax > domainMaxX)
								domainMaxX = xmax;
							if (ymax > domainMaxY)
								domainMaxY = ymax;
							
							// try to figure out if we have a polygon represented by a 
							// complete list of all contour pixels, e.g., a MiToBo contour; 
							// if so we have to ensure that during mask generation all 
							// polygon pixels really become part of the final mask which 
							// does not automatically happen; most likely this is due to the 
							// sweeping algorithm of ImageJ used for that which might have 
							// problems with polygons represented by complete pixel lists...
							boolean realPolygon = false;
							if (  points.get(0).distance(points.get(points.size()-1)) 
									> Math.sqrt(2))
								realPolygon = true;
							for (int i=1; !realPolygon && i<points.size();++i) {
								if (points.get(i).distance(points.get(i-1)) > Math.sqrt(2))
									realPolygon = true;							
							}
							
							// create polygon
							polygon = new MTBPolygon2D(points, true);
							// just for safety reasons, increase size a bit in each dimension
							width = (int) (xmax + 0.5) + 1;
							height = (int) (ymax + 0.5) + 1;
							int[][] mask = 
									polygon.getBinaryMask(width, height, !realPolygon);
							region = new MTBRegion2D();
							for (int py = 0; py < height; ++py) {
									for (int px = 0; px < width; ++px) {
											if (mask[py][px] > 0)
													region.addPixel(new Point2D.Double(px, py));
									}
							}
							tmpRegions.add(region);
						}
						// add region to region vector
						if (this.useSpecifiedDomain)
							this.regions = new MTBRegion2DSet(this.domainXmin, 
								this.domainYmin, this.domainXmax, this.domainYmax);
						else
							this.regions = new MTBRegion2DSet(0, 0, domainMaxX, domainMaxY);
						for (MTBRegion2D reg: tmpRegions)
							this.regions.add(reg);
					}
					break;
				}					
			}

			/* copied from ImageJ source */
			// Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files
			// and to not empty the current list
			/**
			 * Opens a zip file with ImageJ ROIs.
			 * @param path	Path to file that should be opened.
			 */
			private void openRoiManagerFile(String path) {
				ZipInputStream in = null;
				ByteArrayOutputStream out;
				int nRois = 0;
				byte[] buf = new byte[1024];
				int len;
				String ename = null;
				try {
					if (path.endsWith(".zip")) {
						in = new ZipInputStream(new FileInputStream(path));
						ZipEntry entry = in.getNextEntry();
						while (entry != null) {
							ename = entry.getName();
							if (ename.endsWith(".roi")) {
								out = new ByteArrayOutputStream();
								while ((len = in.read(buf)) > 0)
									out.write(buf, 0, len);
								out.close();
								byte[] bytes = out.toByteArray();
								RoiDecoder rd = new RoiDecoder(bytes, ename);
								Roi roi = rd.getRoi();
								if (roi != null) {
									ename = ename.substring(0, ename.length() - 4);
									ename = getUniqueName(ename);
									// list.add(name);
									this.roitable.put(ename, roi);
									nRois++;
								}
							}
							entry = in.getNextEntry();
						}
						in.close();
					}
					// file contains just a single roi
					else {
						FileInputStream ins = new FileInputStream(path);
						out = new ByteArrayOutputStream();
						while ((len = ins.read(buf)) > 0)
							out.write(buf, 0, len);
						out.close();
						byte[] bytes = out.toByteArray();
						RoiDecoder rd = new RoiDecoder(bytes, ename);
						Roi roi = rd.getRoi();
						if (roi != null) {
							ename = path.substring(0, path.length() - 4);
							ename = getUniqueName(ename);
							// list.add(name);
							this.roitable.put(ename, roi);
							nRois++;
						}
						ins.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
					try {
						if (in != null)
							in.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if (nRois == 0)
					System.err.println("This ZIP archive does not appear "
							+ "to contain \".roi\" files... strange!");
			}

			/**
			 * Generates a unique name for a new roi in the manager.
			 * 
			 * @param ename
			 *          Roi to rename for uniqueness.
			 * @return Unique name of roi under consideration.
			 */
			private String getUniqueName(String ename) {
				String rname = ename;
				int n = 1;
				Roi roi2 = this.roitable.get(rname);
				while (roi2 != null) {
					roi2 = this.roitable.get(rname);
					if (roi2 != null) {
						int lastDash = rname.lastIndexOf("-");
						if (lastDash != -1 && rname.length() - lastDash < 5)
							rname = rname.substring(0, lastDash);
						rname = rname + "-" + n;
						n++;
					}
					roi2 = this.roitable.get(rname);
				}
				return rname;
			}
		}

		/* private helpers */

		/**
		 * Gets an instance of the current ROI manager of ImageJ.
		 * <p>
		 * If there is already a roi manager open, get a reference to that one,
		 * otherwise open a new one.
		 */
		private void openRoiManager() {
			this.roiManager = RoiManager.getInstance();
			if (this.roiManager == null) {
				this.roiManager = new RoiManager();
				this.roiManager.run(null);
			}
		}
}
