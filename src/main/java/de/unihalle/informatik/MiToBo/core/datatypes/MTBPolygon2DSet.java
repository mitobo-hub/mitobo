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

import java.util.*;
import java.awt.geom.Point2D;
import java.io.*;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo_xml.*;
import de.unihalle.informatik.MiToBo_xml.impl.*;
import de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;

/**
 * Data type to represent a set of {@link MTBPolygon2D}.
 * <p>
 * A set of 2D polygons lives in a common domain which is a rectangular subset
 * of R x R. Each polygon is of type Polygon2D or a subclass. For reading and
 * writing the derived class {@link MTBSnake} is handled in addition to 
 * {@link MTBPolygon2D}.
 * 
 * @author Stefan Posch
 */
@ALDParametrizedClass
public class MTBPolygon2DSet extends ALDData 
	implements Cloneable, MTBDataExportableToImageJROI {

	/**
	 * Debug flag for internal use only.
	 */
	private boolean debug = false;

	// subset of R x R
	/**
	 * Minimal x coordinate of the domain of this polygon set.
	 */
	@ALDClassParameter(label="Minimal x in domain")
	private double xMin;

	/**
	 * Minimal y coordinate of the domain of this polygon set.
	 */
	@ALDClassParameter(label="Minimal y in domain")
	private double yMin;

	/**
	 * Maximal x coordinate of the domain of this polygon set.
	 */
	@ALDClassParameter(label="Maximal x in domain")
	private double xMax;

	/**
	 * Maximal y coordinate of the domain of this polygon set.
	 */
	@ALDClassParameter(label="Maximal y in domain")
	private double yMax;

	/**
	 * The set of polygons represented as a Vector.
	 */
	@ALDClassParameter(label="Set of polygons") 
	Vector<MTBPolygon2D> polygonSet;

	/**
	 * Standard constructor
	 */
	public MTBPolygon2DSet() {
		this.xMin = 0.0;
		this.yMin = 0.0;
		this.xMax = 0.0;
		this.yMax = 0.0;
		this.polygonSet = new Vector<MTBPolygon2D>();
		setProperty("xMin", new Double(0.0));
		setProperty("yMin", new Double(0.0));
		setProperty("xMax", new Double(0.0));
		setProperty("yMax", new Double(0.0));
	}

	/**
	 * Construct an empty set of polygons with given extent of domain.
	 * 
	 * @param _xMin
	 *          minimum value of x-coordinates for the set
	 * @param _yMin
	 *          minimum value of y-coordinates for the set
	 * @param _xMax
	 *          maximum value of x-coordinates for the set
	 * @param _yMax
	 *          maximum value of y-coordinates for the set
	 */
	public MTBPolygon2DSet(double _xMin, double _yMin, 
			double _xMax, double _yMax) {
		this.xMin = _xMin;
		this.yMin = _yMin;
		this.xMax = _xMax;
		this.yMax = _yMax;
		this.polygonSet = new Vector<MTBPolygon2D>();
		setProperty("xMin", new Double(this.xMin));
		setProperty("yMin", new Double(this.yMin));
		setProperty("xMax", new Double(this.xMax));
		setProperty("yMax", new Double(this.yMax));
	}

	/**
	 * Construct polygon set from given vector.
	 * 
	 * @param _polys
	 *          set of polygons
	 * @param _xMin
	 *          minimum value of x-coordinates for the set
	 * @param _yMin
	 *          minimum value of y-coordinates for the set
	 * @param _xMax
	 *          maximum value of x-coordinates for the set
	 * @param _yMax
	 *          maximum value of y-coordinates for the set
	 */
	public MTBPolygon2DSet(Vector<MTBPolygon2D> _polys, double _xMin,
			double _yMin, double _xMax, double _yMax) {
		this.xMin = _xMin;
		this.yMin = _yMin;
		this.xMax = _xMax;
		this.yMax = _yMax;
		this.polygonSet = _polys;
		setProperty("xMin", new Double(this.xMin));
		setProperty("yMin", new Double(this.yMin));
		setProperty("xMax", new Double(this.xMax));
		setProperty("yMax", new Double(this.yMax));
	}

	@Override
	public MTBPolygon2DSet clone() {
		MTBPolygon2DSet newSet = 
				new MTBPolygon2DSet(this.xMin, this.yMin, this.xMax, this.yMax);
		newSet.polygonSet = new Vector<MTBPolygon2D>();
		for (MTBPolygon2D poly: this.polygonSet) {
			newSet.polygonSet.add(poly.clone());
		}
		newSet.setProperty("xMin", new Double(this.xMin));
		newSet.setProperty("yMin", new Double(this.yMin));
		newSet.setProperty("xMax", new Double(this.xMax));
		newSet.setProperty("yMax", new Double(this.yMax));
		return newSet;
	}

	/**
	 * Returns minimal x coordinate of the domain.
	 */
	public double getXmin() {
		return this.xMin;
	}

	/**
	 * Returns maximal x coordinate of the domain.
	 */
	public double getXmax() {
		return this.xMax;
	}

	/**
	 * Returns minimal y coordinate of the domain.
	 */
	public double getYmin() {
		return this.yMin;
	}

	/**
	 * Returns maximal x coordinate of the domain.
	 */
	public double getYmax() {
		return this.yMax;
	}

	/**
	 * Get the number of polygons of this polygon set.
	 * 
	 * @return Number of polygons in the set.
	 */
	public int size() {
		return this.polygonSet.size();
	}

	/**
	 * Get a polygon by index.
	 * 
	 * @return Polygon at i-th position of the set.
	 */
	public MTBPolygon2D elementAt(int i) {
		if (i < 0 || i >= size())
			throw new ArrayIndexOutOfBoundsException();
		return this.polygonSet.elementAt(i);
	}

	/**
	 * Set a polygon at i-th position of the set.
	 * 
	 * @param i
	 *          position
	 * @param poly
	 *          polygon object to set at position i
	 */
	public void setElementAt(int i, MTBPolygon2D poly) {
		this.polygonSet.set(i, poly);
	}

	/**
	 * Append a polygon (at the end) to the set of polygons.
	 * 
	 * @param polygon
	 *          Polygon to add.
	 */
	public boolean add(MTBPolygon2D polygon) {
		return this.polygonSet.add(polygon);
	}

	/**
	 * Read a polygon set from an xml file <code>filename</code> and set the MTB
	 * polygon set accordingly. The processing history is read also if available.
	 * <p>
	 * WARNING: currently assume filename WITHOUT extension.
	 * 
	 * @param filename
	 *          Filename to read from, WITHOUT extension (for the moment).
	 * @throws IOException 
	 * @throws XmlException 
	 * @throws ClassNotFoundException 
	 */
	public void read(String filename) throws IOException, XmlException, ClassNotFoundException {
		// TODO: use read(MTBXMLPolygon2DSetType)
		MTBXMLPolygon2DSetDocument xmlPolygonSetDocument;
		File file = new File(filename);
		setLocation(file.getCanonicalPath());
		xmlPolygonSetDocument = MTBXMLPolygon2DSetDocument.Factory.parse(file);
		MTBXMLPolygon2DSetType xmlPolygonSet = xmlPolygonSetDocument
				.getMTBXMLPolygon2DSet();
		this.xMin = xmlPolygonSet.getXMin();
		this.yMin = xmlPolygonSet.getYMin();
		this.xMax = xmlPolygonSet.getXMax();
		this.yMax = xmlPolygonSet.getYMax();
		// resize polygon set and copy polygons from xml
		this.polygonSet.setSize(xmlPolygonSet.sizeOfPolygonArray());
		for (int idx = 0; idx < xmlPolygonSet.sizeOfPolygonArray(); idx++) {
			MTBXMLPolygon2DType xmlPolygon = xmlPolygonSet.getPolygonArray(idx);
			this.polygonSet.setElementAt(getPolygon2DFromXml(xmlPolygon), idx);
		}

		// MTBPortHashAccess.readHistory(this, filename);
		MTBOperator.readHistory(this, filename);
	}

	/**
	 * Read a polygon set from an xml object <code>xmlPolygon2DSet</code> and set the MTB
	 * polygon set accordingly.
	 * <p>
	 * WARNING: currently assume filename WITHOUT extension.
	 * 
	 * @param filename
	 *          Filename to read from, WITHOUT extension (for the moment).
	 * @throws ClassNotFoundException 
	 */
	public void read(MTBXMLPolygon2DSetType xmlPolygon2DSet) throws ClassNotFoundException  {

		this.xMin = xmlPolygon2DSet.getXMin();
		this.yMin = xmlPolygon2DSet.getYMin();
		this.xMax = xmlPolygon2DSet.getXMax();
		this.yMax = xmlPolygon2DSet.getYMax();
		// resize polygon set and copy polygons from xml
		this.polygonSet.setSize(xmlPolygon2DSet.sizeOfPolygonArray());
		for (int idx = 0; idx < xmlPolygon2DSet.sizeOfPolygonArray(); idx++) {
			MTBXMLPolygon2DType xmlPolygon = xmlPolygon2DSet.getPolygonArray(idx);
			this.polygonSet.setElementAt(getPolygon2DFromXml(xmlPolygon), idx);
		}
	}

	/**
	 * * Get a new <code>Polygon2D</code> from the information of the
	 * <code>xmlPolygon</code> . Returns Polygon2D and Snake type polygons as
	 * appropriate.
	 * 
	 * @param xmlPolygon
	 *          polygon object stored in the xmlPolygon
	 * @return Polygon2D and Snake type polygons.
	 * @throws ClassNotFoundException
	 */
	public MTBPolygon2D getPolygon2DFromXml(MTBXMLPolygon2DType xmlPolygon)
			throws ClassNotFoundException {
		if (xmlPolygon.getClass() == MTBXMLSnakeTypeImpl.class) {
			MTBXMLSnakeType xmlSnake = (MTBXMLSnakeType) xmlPolygon;
			Vector<MTBSnakePoint2D> points = MTBPolygon2DSet.extractPointVectorSnake(xmlSnake);
			MTBSnake snake = new MTBSnake(points, xmlSnake.getClosed(), xmlSnake
					.getScaleFactor(), true);
			return snake;
		} else if (xmlPolygon.getClass() == MTBXMLPolygon2DTypeImpl.class) {
			Vector<Point2D.Double> points = MTBPolygon2DSet.extractPointVectorPolygon(xmlPolygon);
			MTBPolygon2D polygon = new MTBPolygon2D(points, xmlPolygon.getClosed());
			return polygon;
		} else {
			throw new ClassNotFoundException();
		}
	}

	/**
	 * Extract the list of polygon points for a polygon.
	 * 
	 * @param xmlPolygon
	 *          the xml polygon to copy the data from
	 * @return List of polygon points.
	 */
	private static Vector<Point2D.Double> extractPointVectorPolygon(
			MTBXMLPolygon2DType xmlPolygon) {
		Vector<Point2D.Double> points = new Vector<Point2D.Double>();
		for (int i = 0; i < xmlPolygon.sizeOfPointArray(); i++) {
			points.add(new Point2D.Double(xmlPolygon.getPointArray(i).getX(),
					xmlPolygon.getPointArray(i).getY()));
		}
		return points;
	}

	/**
	 * Extract the list of polygon points for a snake.
	 * 
	 * @param xmlPolygon
	 *          the xml polygon to copy the data from
	 * @return List of snake points.
	 */
	private static Vector<MTBSnakePoint2D> extractPointVectorSnake(
			MTBXMLPolygon2DType xmlPolygon) {
		Vector<MTBSnakePoint2D> points = new Vector<MTBSnakePoint2D>();
		for (int i = 0; i < xmlPolygon.sizeOfPointArray(); i++) {
			points.add(new MTBSnakePoint2D(xmlPolygon.getPointArray(i).getX(),
					xmlPolygon.getPointArray(i).getY()));
		}
		return points;
	}

	/**
	 * Write this polygon set as xml into file <code>filename.xml</code>
	 * Additionally the processing history is written.
	 * <p>
	 * WARNING: currently assume filename WITHOUT extension.
	 * 
	 * @param filename
	 *          Filename to write to, WITHOUT extension (for the moment)
	 */
	public void write(String filename) throws ALDException {
		this.write(filename, true);
	}

	/**
	 * Write this polygon set as xml into file <code>filename</code> If flag is
	 * set to true, the processing history is written, too.
	 * 
	 * @param filename
	 *          Filename to write to.
	 * @param writeHistory
	 *          If true, the history is written as well.
	 */
	public void write(String filename, boolean writeHistory) throws ALDException {
		try {
			BufferedWriter file = new BufferedWriter(new FileWriter(filename));

			MTBXMLPolygon2DSetDocument xmlPolygonSetDocument = MTBXMLPolygon2DSetDocument.Factory
					.newInstance();
			
			//TODO: use toXMLType instead of the remaining code
			MTBXMLPolygon2DSetType xmlPolygonSet = xmlPolygonSetDocument
					.addNewMTBXMLPolygon2DSet();
			xmlPolygonSet.setVersion("1");

			xmlPolygonSet.setXMin(this.xMin);
			xmlPolygonSet.setYMin(this.yMin);
			xmlPolygonSet.setXMax(this.xMax);
			xmlPolygonSet.setYMax(this.yMax);

			// try to use cursor to insert empty elements
			// XmlCursor cursor = xmlPolygonSet.newCursor();
			// System.out.println( cursor.toNextToken());

			for (int p = 0; p < this.polygonSet.size(); p++) {
				if (this.debug)
					System.out.println("polygon " + p);
				// try to use cursor to insert empty elements ???
				// cursor.beginElement( "polygon");

				// I do not know how I could insert a MTBXMLSNakeType at this point
				// instead of MTBXMLPolygon2DType
				MTBXMLPolygon2DType xmlPolygon = xmlPolygonSet.addNewPolygon();

				MTBPolygon2D polygon = this.polygonSet.elementAt(p);
				// may we should move getPolygon2DAsXml to Polygon2D and overide it in
				// derived classes
				if (polygon.getClass() == MTBPolygon2D.class) {
					xmlPolygonSet.setPolygonArray(p, getPolygon2DAsXml(polygon, null));
				} else if (polygon.getClass() == MTBSnake.class) {
					xmlPolygonSet.setPolygonArray(p, getSnakeAsXml((MTBSnake) polygon,
							null));
				} else {
					throw new ClassNotFoundException();
				}
			}
			file.write(xmlPolygonSetDocument.toString());
			file.close();
		} catch (Exception e) {
			System.err.println("Exception" + e);
		}

		// write processing history
		if (writeHistory)
			// MTBPortHashAccess.writeHistory(this, filename);
			ALDOperator.writeHistory(this, filename);
	}

	/**
	 * Create a xml representation of this polygon set using MiToBo's xml-scheme MTBXMLPolygon2DSet.xsd
	 * 
	 * @return the xml representation of this polygon set
	 * @throws ClassNotFoundException if the class of the polygons in the set are not known to the implementation
	 * (rather an unknown extending class)
	 */
	public MTBXMLPolygon2DSetType toXMLType() throws ClassNotFoundException {
		MTBXMLPolygon2DSetType xmlPolygonSet = MTBXMLPolygon2DSetType.Factory.newInstance();
		xmlPolygonSet.setVersion("1");

		xmlPolygonSet.setXMin(this.xMin);
		xmlPolygonSet.setYMin(this.yMin);
		xmlPolygonSet.setXMax(this.xMax);
		xmlPolygonSet.setYMax(this.yMax);

		// try to use cursor to insert empty elements
		// XmlCursor cursor = xmlPolygonSet.newCursor();
		// System.out.println( cursor.toNextToken());

		for (int p = 0; p < this.polygonSet.size(); p++) {
			if (this.debug)
				System.out.println("polygon " + p);
			// try to use cursor to insert empty elements ???
			// cursor.beginElement( "polygon");

			// I do not know how I could insert a MTBXMLSNakeType at this point
			// instead of MTBXMLPolygon2DType
			MTBXMLPolygon2DType xmlPolygon = xmlPolygonSet.addNewPolygon();

			MTBPolygon2D polygon = this.polygonSet.elementAt(p);
			// may we should move getPolygon2DAsXml to Polygon2D and overide it in
			// derived classes
			if (polygon.getClass() == MTBPolygon2D.class) {
				xmlPolygonSet.setPolygonArray(p, getPolygon2DAsXml(polygon, null));
			} else if (polygon.getClass() == MTBSnake.class) {
				xmlPolygonSet.setPolygonArray(p, getSnakeAsXml((MTBSnake) polygon,
						null));
			} else {
				throw new ClassNotFoundException();
			}
		}

		return xmlPolygonSet;
	}
	/**
	 * Copy the information of <code>polygon</code> into the corresponding xml
	 * element <code>xmlPolygon</code>. If <code>xmlPolygon</code> is null, a new
	 * object is created, otherwise the passed object filled.
	 */
	public MTBXMLPolygon2DType getPolygon2DAsXml(MTBPolygon2D polygon,
			MTBXMLPolygon2DType xmlPolygon) {
		if (xmlPolygon == null)
			xmlPolygon = MTBXMLPolygon2DType.Factory.newInstance();

		xmlPolygon.setClosed(polygon.isClosed());
		Vector<java.awt.geom.Point2D.Double> points = polygon.getPoints();
		for (int i = 0; i < points.size(); i++) {
			MTBXMLPoint2DDoubleType xmlPoint = xmlPolygon.addNewPoint();
			xmlPoint.setX(points.elementAt(i).getX());
			xmlPoint.setY(points.elementAt(i).getY());
		}
		return xmlPolygon;
	}

	/**
	 * Copy the information of <code>snake</code> into the corresponding xml
	 * element <code>xmlSnake</code>. If <code>xmlSnake</code> is null, a new
	 * obejct is created, otherwise the passed object filled.
	 */
	public MTBXMLSnakeType getSnakeAsXml(MTBSnake snake, MTBXMLSnakeType xmlSnake) {
		if (xmlSnake == null)
			xmlSnake = MTBXMLSnakeType.Factory.newInstance();

		xmlSnake = (MTBXMLSnakeType) getPolygon2DAsXml(snake, xmlSnake);
		xmlSnake.setScaleFactor(snake.getScaleFactor());
		return xmlSnake;
	}

	@Override
	public String toString() {
		return new String("MTBPolygon2DSet - set of " + 
				this.polygonSet.size() + " polygons ( [" + 
				this.xMin + "," + this.yMin + "] : [" +
				this.xMax + "," + this.yMax + "] )");
	}

}
