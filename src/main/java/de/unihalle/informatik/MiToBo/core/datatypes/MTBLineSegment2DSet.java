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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * A set of 2D line segments.
 * 
 * @see de.unihalle.informatik.MiToBo.core.datatypes.MTBLineSegment2D
 * 
 * @author Birgit Moeller
 */
@ALDParametrizedClass
public class MTBLineSegment2DSet extends ALDData 
	implements Cloneable, Iterable<MTBLineSegment2D> {

	/**
	 * The set of line segments.
	 */
	@ALDClassParameter(label="Set of line segments") 
	protected Vector<MTBLineSegment2D> lineSegmentSet;

	/** 
	 * Information about this set of line segments.
	 */
	private String info;

	/**
	 * Standard constructor.
	 */
	public MTBLineSegment2DSet() {
		this.info = "";
		this.lineSegmentSet = new Vector<MTBLineSegment2D>();
		setProperty("info", this.info);
	}

	@Override
	public MTBLineSegment2DSet clone() {
		MTBLineSegment2DSet newSet = new MTBLineSegment2DSet();
		newSet.lineSegmentSet = new Vector<MTBLineSegment2D>();
		for (MTBLineSegment2D l: this.lineSegmentSet) {
			newSet.lineSegmentSet.add(l.clone());
		}
		newSet.info = new String(this.info);
		return newSet;
	}
		
//	/**
//	 * Construct an object that represents this region by xml. Only used for
//	 * writing region set bags ({@link MTBRegion2DSetBag}) to file.
//	 */
//	public MTBXMLRegion2DSetType toXMLType() {
//		MTBXMLRegion2DSetType xmlregion2Dset = MTBXMLRegion2DSetType.Factory
//				.newInstance();
//
//		xmlregion2Dset.setXMin(this.xMin);
//		xmlregion2Dset.setXMax(this.xMax);
//		xmlregion2Dset.setYMin(this.yMin);
//		xmlregion2Dset.setYMax(this.yMax);
//		xmlregion2Dset.setInfo(this.info);
//
//		MTBXMLRegion2DType[] regions = new MTBXMLRegion2DType[this.regionSet.size()];
//
//		for (int i = 0; i < this.regionSet.size(); i++) {
//			regions[i] = this.regionSet.get(i).toXMLType();
//		}
//
//		xmlregion2Dset.setRegionsArray(regions);
//
//		return xmlregion2Dset;
//	}
//
//	/**
//	 * Write this region set to disk in XML format using MiToBo's xml-scheme
//	 * MTBRegion2DSet.xsd
//	 */
//	public void write(String filename) throws IOException,
//	ALDProcessingDAGException, ALDOperatorException {
//
//		// generate XML documents
//		MTBXMLRegion2DSetDocument regsetDoc = MTBXMLRegion2DSetDocument.Factory
//				.newInstance();
//
//		// set xml-region2Dset
//		regsetDoc.setMTBXMLRegion2DSet(this.toXMLType());
//
//		// write the xml-file
//		regsetDoc.save(new File(filename));
//
//		MTBOperator.writeHistory(this, filename);
//	}

	/**
	 * Get the number of line segments in the set.
	 * @return Number of line segments.
	 */
	public int size() {
		return this.lineSegmentSet.size();
	}

	/**
	 * Get info string.
	 * @return	Info string of set.
	 */
	public String getInfo() {
		return this.info;
	}

	/**
	 * Set info string of set.
	 * @param i	Info string.
	 */
	public void setInfo(String i) {
		this.info = i;
		setProperty("info", this.info);
	}

	/**
	 * Remove all line segments from this set.
	 */
	public void clear() {
		this.lineSegmentSet.clear();
	}

	/**
	 * Get a specific line segment by index. 
	 * @param i Index of desired segment.
	 * @return i-th segment of the set.
	 */
	public MTBLineSegment2D get(int i) {
		return this.lineSegmentSet.get(i);
	}

	/**
	 * Get a specific line segment by index including a safety check.
	 * @param i Index of desired segment.
	 * @return i-th segment of the set.
	 */
	public MTBLineSegment2D elementAt(int i) {
		if (i < 0 || i >= size())
			throw new ArrayIndexOutOfBoundsException();
		return this.lineSegmentSet.elementAt(i);
	}

	/**
	 * Append a new segment (at the end) to the set of line segments.
	 * @param line	Line segment to add.
	 * @return True if operation was successful.
	 */
	public boolean add(MTBLineSegment2D line) {
		return this.lineSegmentSet.add(line);
	}

	/**
	 * Replace the i-th line segment of the set.
	 * @param line New segment to put into set.
	 * @param i	Index of segment to replace.
	 */
	public void setElementAt(MTBLineSegment2D line, int i) {
		this.lineSegmentSet.setElementAt(line, i);
	}

	/**
	 * Delete the i-th segment from the set.
	 * @param i	Index of segment to remove.
	 */
	public void removeElementAt(int i) {
		this.lineSegmentSet.removeElementAt(i);
	}

	/**
	 * Delete the given lineobject from the set, if contained.
	 * <p>
	 * If the line is contained multiple times in the set, the first occurence
	 * is removed.
	 * 
	 * @param lineToRemove	Line object to remove.
	 */
	public void remove(MTBLineSegment2D lineToRemove) {
		Iterator<MTBLineSegment2D> iter = this.iterator();
		while (iter.hasNext()) {
			MTBLineSegment2D reg = iter.next();
			if ( reg == lineToRemove ) {
				this.lineSegmentSet.remove(lineToRemove);
				break;
			}
		}
	}

	@Override
	public Iterator<MTBLineSegment2D> iterator() {
		Iterator<MTBLineSegment2D> iter = this.lineSegmentSet.iterator();
		return iter;
	}
}
