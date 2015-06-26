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
import java.util.Vector;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion3DSetDocument;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion3DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion3DType;


/** A set of 3D regions living in a common domain which is
  * a rectangular subset of R x R.
  * Each regions is of type Region3D 
  *
  * @author Stefan Posch
 */
@ALDParametrizedClass
public class  MTBRegion3DSet extends ALDData implements MTBRegionSetInterface {

	// subset of R x R	
	/** Minimal x coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Minimal x in domain.")
	private double xMin;

	/** Minimal y coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Minimal y in domain")
	private double yMin;
	
	/** Minimal z coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Minimal z in domain")
	private double zMin;
	
	/** Maximal x coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Maximal x in domain")
	private double xMax;

	/** Maximal y coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Maximal y in domain")
	private double yMax;

	/** Maximal z coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label="Maximal z in domain")
	private double zMax;
	
	/** The set of regions represented as a Vector.
	 */
	@ALDClassParameter(label="List of regions")
	Vector<MTBRegion3D>	regionSet;

	/** Construct an empty set of regions with given extent of domain
	 */
	public MTBRegion3DSet( double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;
		

		this.regionSet = new Vector<MTBRegion3D> ();

		setProperty( "xMin", xMin);
		setProperty( "yMin", yMin);
		setProperty( "zMin", zMin);
		setProperty( "xMax", xMax);
		setProperty( "yMax", yMax);
		setProperty( "zMax", zMax);
	}
	
	/** Construct an empty set (with memory allocated for N elements) of regions with given extent of domain
	 */
	public MTBRegion3DSet(int N, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;
		

		this.regionSet = new Vector<MTBRegion3D>(N);

		setProperty( "xMin", xMin);
		setProperty( "yMin", yMin);
		setProperty( "zMin", zMin);
		setProperty( "xMax", xMax);
		setProperty( "yMax", yMax);
		setProperty( "zMax", zMax);
	}
	
	/** Construct an empty set of regions with given extent of domain
	 */
	public MTBRegion3DSet( Vector<MTBRegion3D> regions, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;

		this.regionSet = new Vector<MTBRegion3D> ();
		for ( int i=0; i < regions.size() ; i++ ) 
			add( regions.elementAt(i));

		setProperty( "xMin", xMin);
		setProperty( "yMin", yMin);
		setProperty( "zMin", zMin);
		setProperty( "xMax", xMax);
		setProperty( "yMax", yMax);
		setProperty( "zMax", zMax);
	}
	
	/**
	 * Construct set of 3D-regions from an xml-representation
	 */
	public MTBRegion3DSet(MTBXMLRegion3DSetType xmlregion3Dset) {
		this.xMin = xmlregion3Dset.getXMin();
		this.xMax = xmlregion3Dset.getXMax();
		this.yMin = xmlregion3Dset.getYMin();
		this.yMax = xmlregion3Dset.getYMax();
		this.zMin = xmlregion3Dset.getZMin();
		this.zMax = xmlregion3Dset.getZMax();
		
		setProperty( "xMin", this.xMin);
		setProperty( "yMin", this.yMin);
		setProperty( "zMin", this.zMin);
		setProperty( "xMax", this.xMax);
		setProperty( "yMax", this.yMax);
		setProperty( "zMax", this.zMax);
		
		// get regions
		MTBXMLRegion3DType[] regarray = xmlregion3Dset.getRegionsArray();
		this.regionSet = new Vector<MTBRegion3D>(regarray.length);
		
		for (int i = 0; i < regarray.length; i++) {
			this.regionSet.add(new MTBRegion3D(regarray[i]));
		}
	}
	
	/**
	 * Construct a set of 3D-regions from a xml-file that was written by the {@link write} method of this class.
	 * (These xml-files conform to xml-scheme MTBXMLRegion3DSet.xsd defined for MiToBo)
	 * @param filename path to the xml-file
	 * @throws IOException thrown if parsing of xml-file failed
	 * @throws XmlException thrown if parsing of xml-file failed
	 */
	public MTBRegion3DSet(String filename) throws XmlException, IOException  {
		this(MTBXMLRegion3DSetDocument.Factory.parse(new File(filename)).getMTBXMLRegion3DSet());

		this.setLocation(filename);

		MTBOperator.readHistory(this, filename);
	}
	
	/**
	 * Construct an xml-representation of this set of 3D-regions.
	 */
	public MTBXMLRegion3DSetType toXMLType() {
		MTBXMLRegion3DSetType xmlregion3Dset = MTBXMLRegion3DSetType.Factory.newInstance();
		
		xmlregion3Dset.setXMin(this.xMin);
		xmlregion3Dset.setXMax(this.xMax);
		xmlregion3Dset.setYMin(this.yMin);
		xmlregion3Dset.setYMax(this.yMax);
		xmlregion3Dset.setZMin(this.zMin);
		xmlregion3Dset.setZMax(this.zMax);
		
		MTBXMLRegion3DType[] regions = new MTBXMLRegion3DType[this.regionSet.size()];
		
		for (int i = 0; i < this.regionSet.size(); i++) {
			regions[i] = this.regionSet.get(i).toXMLType();
		}
		
		xmlregion3Dset.setRegionsArray(regions);
		
		return xmlregion3Dset;
	}
	
	/**
	 * Write this set of 3D-regions to disk in XML format using MiToBo's xml-scheme MTBRegion3DSet.xsd
	 */
	public void write(String filename) throws IOException, ALDProcessingDAGException, ALDOperatorException {

		// generate XML documents
		MTBXMLRegion3DSetDocument regsetDoc = 
			MTBXMLRegion3DSetDocument.Factory.newInstance();

		// set xml-region2Dset
		regsetDoc.setMTBXMLRegion3DSet(this.toXMLType());
		
		// write the xml-file
		regsetDoc.save(new File(filename));
		
		MTBOperator.writeHistory(this, filename);
	}

	public double getXmin() {
		return this.xMin;
	}
	public double getXmax() {
		return this.xMax;
	}
	public double getYmin() {
		return this.yMin;
	}
	public double getYmax() {
		return this.yMax;
	}
	public double getZmin() {
		return this.zMin;
	}
	public double getZmax() {
		return this.zMax;
	}
	
	/** Get the number of regions of this region set
	 *  @return number of regions
	 */
	public int size() {
		return this.regionSet.size();
	}

	/**
	 * Remove all regions from this set
	 */
	public void clear() {
		this.regionSet.clear();
	}
	
	/** Get a region by index
	 *
	 * @return i-th region
	 */
	public MTBRegion3D get( int i) {
		return this.regionSet.get(i);
	}
	
	/** Get a region by index
	 *
	 * @return i-th region
	 */
	public MTBRegion3D elementAt( int i) {
		if ( i < 0 || i >= size() )
			throw new ArrayIndexOutOfBoundsException();

		return this.regionSet.elementAt( i);
	}

	/** Append a region (at the end) to the set of regions.
	 * @param 	region	region to add
	 */
	public boolean add( MTBRegion3D region) {
		return this.regionSet.add( region);
	}

	/** Set the i-th region from the set
	 * @param 	i	index of region to remove
	 */
	public void setElementAt(MTBRegion3D region, int i) {
		this.regionSet.setElementAt(region, i);
	}

	/** Delete the i-th region from the set
	 * @param 	i	index of region to remove
	 */
	public void removeElementAt(int i) {
		this.regionSet.removeElementAt(i);
	}

}
