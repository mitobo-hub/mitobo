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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.awt.geom.Point2D;
import java.util.*;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;

/**
 * A set of 2D borders.
 * <p>
 * A border is an unordered list of border pixels. The border set is living in 
 * a common domain which is a rectangular subset of R x R. Each border is of 
 * type {@link MTBBorder2D}.
 * 
 * @author Birgit Moeller
 */
@ALDParametrizedClass
public class MTBBorder2DSet extends ALDData implements Cloneable {

	/**
	 * Minimal x-coordinate of the domain of this border set.
	 */
	@ALDClassParameter(label="Minimal x in domain.")
	protected double xMin;

	/**
	 * Minimal y-coordinate of the domain of this border set.
	 */
	@ALDClassParameter(label="Minimal y in domain")
	protected double yMin;

	/**
	 * Maximal x-coordinate of the domain of this border set.
	 */
	@ALDClassParameter(label="Maximal x in domain")
	protected double xMax;

	/**
	 * Maximal y-coordinate of the domain of this border set.
	 */
	@ALDClassParameter(label="Maximal y in domain")
	protected double yMax;

	/**
	 * The set of borders represented as a {@link java.util.Vector}.
	 */
	@ALDClassParameter(label="Set of borders") 
	protected Vector<MTBBorder2D> borderSet;

	/**
	 * Standard constructor.
	 */
	public MTBBorder2DSet() {
		this.xMin = 0.0;
		this.yMin = 0.0;
		this.xMax = 0.0;
		this.yMax = 0.0;
		this.borderSet = new Vector<MTBBorder2D>();
		setProperty("xMin", new Double(0.0));
		setProperty("yMin", new Double(0.0));
		setProperty("xMax", new Double(0.0));
		setProperty("yMax", new Double(0.0));
	}

	/**
	 * Construct an empty set of borders with given extent of domain.
	 * 
	 * @param _xMin		Minimal x-coordinate of the domain.
	 * @param _yMin  Minimal y-coordinate of the domain.
	 * @param _xMax  Maximal x-coordinate of the domain.
	 * @param _yMax  Maximal y-coordinate of the domain.
	 */
	public MTBBorder2DSet(
			double _xMin, double _yMin,	double _xMax, double _yMax) {
		this.xMin = _xMin;
		this.yMin = _yMin;
		this.xMax = _xMax;
		this.yMax = _yMax;
		this.borderSet = new Vector<MTBBorder2D>();
		setProperty("xMin", new Double(_xMin));
		setProperty("yMin", new Double(_yMin));
		setProperty("xMax", new Double(_xMax));
		setProperty("yMax", new Double(_yMax));
	}

	@Override
	public MTBBorder2DSet clone() {
		MTBBorder2DSet newSet = 
				new MTBBorder2DSet(this.xMin, this.yMin, this.xMax, this.yMax);
		newSet.borderSet = new Vector<MTBBorder2D>();
		for (MTBBorder2D cont: this.borderSet) {
			newSet.borderSet.add(cont.clone());
		}
		newSet.setProperty("xMin", new Double(this.xMin));
		newSet.setProperty("yMin", new Double(this.yMin));
		newSet.setProperty("xMax", new Double(this.xMax));
		newSet.setProperty("yMax", new Double(this.yMax));
		return newSet;
	}

	/**
	 * Get the number of borders of this set.
	 * @return Number of borders.
	 */
	public int size() {
		return this.borderSet.size();
	}

	/**
	 * Get a border by index.
	 * @param i Index of border to retrieve.
	 * @return The i-th border.
	 */
	public MTBBorder2D elementAt(int i) {
		if (i < 0 || i >= size())
			throw new ArrayIndexOutOfBoundsException();
		return this.borderSet.elementAt(i);
	}

	/**
	 * Get the minimal x-coordinate of the domain of this border set.
	 * @return	Minimal x-coordinate.
	 */
	public double getXMin() {
		return this.xMin;
	}

	/**
	 * Get the maximal x-coordinate of the domain of this border set.
	 * @return	Maximal x-coordinate.
	 */
	public double getXMax() {
		return this.xMax;
	}

	/**
	 * Get the minimal y-coordinate of the domain of this border set.
	 * @return	Minimal y-coordinate.
	 */
	public double getYMin() {
		return this.yMin;
	}

	/**
	 * Get the maximal y-coordinate of the domain of this border set.
	 * @return	Maximal y-coordinate.
	 */
	public double getYMax() {
		return this.yMax;
	}

	/**
	 * Append a border (at the end) to the set of borders.
	 * @param border		Border to be added.
	 * @return True in case of success.
	 */
	public boolean add(MTBBorder2D border) {
		return this.borderSet.add(border);
	}

	/**
	 * Set the i-th border of the set.
	 * @param border		New border element.
	 * @param i					Index of position where to put it.
	 */
	public void setElementAt(MTBBorder2D border, int i) {
		this.borderSet.setElementAt(border, i);
	}

	/**
	 * Delete the i-th border from the set.
	 * @param i		Index of border to be removed.
	 */
	public void removeElementAt(int i) {
		this.borderSet.removeElementAt(i);
	}

	/**
	 * Read a set of 2D border objects from ASCII files in xSV format from the
	 * given directory.
	 * <p>
	 * It is assumed that all files have the same format and that there are only
	 * files containing borders in the directory. Not that the directory is not
	 * processed recursively.
	 * 
	 * @param dir				Directory from where to read the files.
	 * @param delim			Delimiter in the file.
	 * @param skipLines	Number of header lines to skip.
	 * @return Set of borders.
	 */
	public static MTBBorder2DSet readBordersFromASCIIFiles(
			String dir, String delim, int skipLines) {

		MTBBorder2DSet set = new MTBBorder2DSet();
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymax = Double.MIN_VALUE;
		
		DirectoryTree dt = new DirectoryTree(dir, false);
		Vector<String> files = dt.getFileList();
		MTBBorder2D b;
		for (String f: files) {
			b = MTBBorder2D.readBorderFromASCIIFile(f, delim, skipLines);
			for (Point2D.Double p: b.getPoints()) {
				if (p.x < xmin) xmin = p.x;
				if (p.x > xmax) xmax = p.x;
				if (p.y < ymin) ymin = p.y;
				if (p.y > ymax) ymax = p.y;			
			}
			set.add(b);
		}
		set.xMin = xmin;
		set.xMax = xmax;
		set.yMin = ymin;
		set.yMax = ymax;
		return set;
	}
	
	//		/**
	//		 * Read a contour set from an xml file <code>filename</code> and set the MTB
	//		 * polygon set accordingly. The processing history is read also if available.
	//		 * <p>
	//		 * WARNING: currently assume filename WITHOUT extension.
	//		 * 
	//		 * @param filename
	//		 *          Filename to read from, WITHOUT extension (for the moment).
	//		 * @throws IOException 
	//		 * @throws XmlException 
	//		 */
	//		public void read(String filename) throws IOException, XmlException {
	//
	//			MTBXMLContour2DSetDocument xmlContourSetDocument;
	//			File file = new File(filename + ".xml");
	//			setLocation(file.getCanonicalPath());
	//
	//			// instantiate XML document by file parsing
	//			xmlContourSetDocument = MTBXMLContour2DSetDocument.Factory.parse(file);
	//			MTBXMLContour2DSetType xmlContourSet = xmlContourSetDocument
	//					.getMTBXMLContour2DSet();
	//
	//			// get bounding box
	//			this.xMin = xmlContourSet.getXMin();
	//			this.yMin = xmlContourSet.getYMin();
	//			this.xMax = xmlContourSet.getXMax();
	//			this.yMax = xmlContourSet.getYMax();
	//
	//			// resize contour set and copy contours from xml
	//			this.borderSet = new Vector<MTBBorder2D>();
	//			this.borderSet.setSize(xmlContourSet.sizeOfContoursArray());
	//			for (int idx = 0; idx < xmlContourSet.sizeOfContoursArray(); idx++) {
	//				MTBXMLContour2DType xmlContour = xmlContourSet.getContoursArray(idx);
	//				this.borderSet.setElementAt(getContour2DFromXml(xmlContour), idx);
	//			}
	//
	//			// MTBPortHashAccess.readHistory(this, filename);
	//			MTBOperator.readHistory(this, filename);
	//		}
	//
	//		/**
	//		 * Read a contour set from an xml object <code>xmlContourSet</code> and set the MTB
	//		 * polygon set accordingly. 
	//		 * <p>
	//		 * WARNING: currently assume filename WITHOUT extension.
	//		 * 
	//		 * @param filename
	//		 *          Filename to read from, WITHOUT extension (for the moment).
	//		 * @throws IOException 
	//		 * @throws XmlException 
	//		 */
	//		public void read(MTBXMLContour2DSetType xmlContourSet) throws IOException, XmlException {
	//
	//			// get bounding box
	//			this.xMin = xmlContourSet.getXMin();
	//			this.yMin = xmlContourSet.getYMin();
	//			this.xMax = xmlContourSet.getXMax();
	//			this.yMax = xmlContourSet.getYMax();
	//
	//			// resize contour set and copy contours from xml
	//			this.borderSet = new Vector<MTBBorder2D>();
	//			this.borderSet.setSize(xmlContourSet.sizeOfContoursArray());
	//			for (int idx = 0; idx < xmlContourSet.sizeOfContoursArray(); idx++) {
	//				MTBXMLContour2DType xmlContour = xmlContourSet.getContoursArray(idx);
	//				this.borderSet.setElementAt(getContour2DFromXml(xmlContour), idx);
	//			}
	//		}
	//
	//		/**
	//		 * Write this contour set into XML file <code>filename.xml</code>
	//		 * Additionally, its processing history is written.
	//		 * <p>
	//		 * WARNING: currently assume filename WITHOUT extension.
	//		 * 
	//		 * @param filename
	//		 *          Filename to write to, WITHOUT extension (for the moment)
	//		 */
	//		public void write(String filename) throws ALDException {
	//				try {
	//						BufferedWriter file = new BufferedWriter(
	//						    new FileWriter(filename + ".xml"));
	//
	//						// generate XML documents
	//						MTBXMLContour2DSetDocument xmlContourSetDocument = MTBXMLContour2DSetDocument.Factory
	//						    .newInstance();
	//						MTBXMLContour2DSetType xmlContourSet = xmlContourSetDocument
	//						    .addNewMTBXMLContour2DSet();
	//
	//						// transfer the contours to XML
	//						MTBXMLContour2DType[] cList = new MTBXMLContour2DType[this.borderSet
	//						    .size()];
	//						for (int p = 0; p < this.borderSet.size(); p++) {
	//								MTBBorder2D contour = this.borderSet.elementAt(p);
	//								cList[p] = this.getContour2DAsXml(contour, null);
	//						}
	//						xmlContourSet.setContoursArray(cList);
	//						xmlContourSet.setXMin(this.xMin);
	//						xmlContourSet.setYMin(this.yMin);
	//						xmlContourSet.setXMax(this.xMax);
	//						xmlContourSet.setYMax(this.yMax);
	//
	//						// write the xml file
	//						file.write(xmlContourSetDocument.toString());
	//						file.close();
	//				} catch (Exception e) {
	//						System.err.println("Exception" + e);
	//				}
	//				// write processing history
	//				// MTBPortHashAccess.writeHistory(this, filename);
	//				MTBOperator.writeHistory(this, filename);
	//		}
	//
	//		public MTBXMLContour2DSetType toXMLType() {
	//			MTBXMLContour2DSetType xmlContourSet = MTBXMLContour2DSetType.Factory.newInstance();
	//
	//			// transfer the contours to XML
	//			MTBXMLContour2DType[] cList = new MTBXMLContour2DType[this.borderSet
	//			                                                      .size()];
	//			for (int p = 0; p < this.borderSet.size(); p++) {
	//				MTBBorder2D contour = this.borderSet.elementAt(p);
	//				cList[p] = this.getContour2DAsXml(contour, null);
	//			}
	//			xmlContourSet.setContoursArray(cList);
	//			xmlContourSet.setXMin(this.xMin);
	//			xmlContourSet.setYMin(this.yMin);
	//			xmlContourSet.setXMax(this.xMax);
	//			xmlContourSet.setYMax(this.yMax);
	//			
	//			return xmlContourSet;
	//		}
	//		/**
	//		 * Copy the information of <code>contour</code> into the corresponding xml
	//		 * element <code>xmlContour</code>. If <code>xmlContour</code> is null, a new
	//		 * obejct is created, otherwise the passed object filled.
	//		 */
	//		public MTBXMLContour2DType getContour2DAsXml(MTBBorder2D contour,
	//		    MTBXMLContour2DType xmlC) {
	//
	//				MTBXMLContour2DType xmlContour;
	//
	//				// instantiate contour
	//				if (xmlC == null)
	//						xmlContour = MTBXMLContour2DType.Factory.newInstance();
	//				else
	//						xmlContour = xmlC;
	//
	//				// list of contour points
	//				MTBXMLPointVectorType xmlPlist = MTBXMLPointVectorType.Factory
	//				    .newInstance();
	//				Vector<java.awt.geom.Point2D.Double> points = contour.getPoints();
	//				for (int i = 0; i < points.size(); i++) {
	//						MTBXMLPoint2DDoubleType xmlPoint = xmlPlist.addNewPoint();
	//						xmlPoint.setX((int) points.elementAt(i).getX());
	//						xmlPoint.setY((int) points.elementAt(i).getY());
	//				}
	//				xmlContour.setPoints(xmlPlist);
	//
	//				// inner contours
	//				Vector<MTBBorder2D> innerContours = contour.getAllInnerBorders();
	//				MTBXMLContour2DType[] inList = new MTBXMLContour2DType[innerContours.size()];
	//				for (int i = 0; i < innerContours.size(); ++i) {
	//						inList[i] = this.getContour2DAsXml(innerContours.get(i), null);
	//				}
	//				xmlContour.setInnerArray(inList);
	//				return xmlContour;
	//		}
	//
	//		/**
	//		 * Get a new <code>MTBContour2D</code> from the information of the
	//		 * <code>xmlContour</code>.
	//		 * 
	//		 * @param xmlContour
	//		 *          xml contour object
	//		 * 
	//		 * @return MTBContour2D object.
	//		 */
	//		public MTBBorder2D getContour2DFromXml(MTBXMLContour2DType xmlContour) {
	//
	//				MTBBorder2D contour = new MTBBorder2D();
	//
	//				// get contour points
	//				MTBXMLPointVectorType plist = xmlContour.getPoints();
	//				for (int i = 0; i < plist.sizeOfPointArray(); i++) {
	//						MTBXMLPoint2DDoubleType p = plist.getPointArray(i);
	//						contour.addPixel((int) p.getX(), (int) p.getY());
	//				}
	//				// inner contours
	//				MTBXMLContour2DType[] inConts = xmlContour.getInnerArray();
	//				if (inConts != null && inConts.length > 0) {
	//						for (int i = 0; i < inConts.length; ++i) {
	//								MTBBorder2D inCont = getContour2DFromXml(inConts[i]);
	//								try {
	//	                contour.addInner(inCont);
	//                } catch (MTBDatatypeException e) {
	//                	System.err.println("[MTBBorder2DSet] could not convert " + 
	//                			"inner border... skipping!");
	//                	continue;
	//                }
	//						}
	//				}
	//				return contour;
	//		}
}
