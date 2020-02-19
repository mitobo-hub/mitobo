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
import java.util.Iterator;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetBagDocument;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetBagType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetType;


/**
 * A datatype to store multiple MTBRegion2D sets
 * 
 * @author Oliver Gress
 *
 */
@ALDParametrizedClass
public class MTBRegion2DSetBag extends ALDData {

	/** the container of the region sets */
	@ALDClassParameter(label="Set of region sets")
	Vector<MTBRegion2DSet> regionsets;
	
	/**
	 * Constructor to create an empty bag of region sets
	 */
	public MTBRegion2DSetBag() {
		this.regionsets = new Vector<MTBRegion2DSet>();
	}
	
	/**
	 * Constructor to create an empty bag of region sets with the given capacity
	 */
	public MTBRegion2DSetBag(int capacity) {
		this.regionsets = new Vector<MTBRegion2DSet>(capacity);
	}
	
	
	/**
	 * Construct a bag of region sets from a xml-file that was written by the {@link write} method of this class.
	 * (These xml-files conform to xml-scheme MTBXMLRegion2DSetBag.xsd defined for MiToBo)
	 * @param filename path to the xml-file
	 * @throws IOException thrown if parsing of xml-file failed
	 * @throws XmlException thrown if parsing of xml-file failed
	 */
	public MTBRegion2DSetBag(String filename) throws XmlException, IOException {
//		MTBXMLRegion2DSetBagType regionsetbag 
//						= MTBXMLRegion2DSetBagDocument.Factory.parse(new File(filename)).getMTBXMLRegion2DSetBag();
//
//		MTBXMLRegion2DSetType[] regsets = regionsetbag.getRegionsetsArray();
//		this.regionsets = new Vector<MTBRegion2DSet>(regsets.length);
//		
//		for (int i = 0; i < regsets.length; i++) {
//			this.regionsets.add(new MTBRegion2DSet(regsets[i]));
//		}
		this(MTBXMLRegion2DSetBagDocument.Factory.parse(new File(filename)).getMTBXMLRegion2DSetBag());
		this.setLocation(filename);
		MTBOperator.readHistory(this, filename);
	}
	
	/**
	 * Construct a bag of region sets from a xml-object created by the {@link toXMLType} method of this class.
	 * (The xml-object conforms to xml-schema MTBXMLRegion2DSetBag.xsd defined for MiToBo)
	 * 
	 * @param regionsetbag
	 */
	public MTBRegion2DSetBag(MTBXMLRegion2DSetBagType regionsetbag)  {
		MTBXMLRegion2DSetType[] regsets = regionsetbag.getRegionsetsArray();
		this.regionsets = new Vector<MTBRegion2DSet>(regsets.length);
		
		for (int i = 0; i < regsets.length; i++) {
			this.regionsets.add(new MTBRegion2DSet(regsets[i]));
		}
	}
	
	
	/**
	 * Write this bag of region sets to disk in XML format using MiToBo's xml-scheme MTBRegion2DSetBag.xsd
	 */
	public void write(String filename) throws IOException, ALDProcessingDAGException, ALDOperatorException {

		// generate XML documents
		MTBXMLRegion2DSetBagDocument regsetbagDoc = 
						MTBXMLRegion2DSetBagDocument.Factory.newInstance();

//		MTBXMLRegion2DSetType[] regsetarray = new MTBXMLRegion2DSetType[this.regionsets.size()];
//		for (int i = 0; i < regsetarray.length; i++) {
//			regsetarray[i] = this.regionsets.get(i).toXMLType();
//		}
//		
//		// create xml-region2Dsetbag
//		MTBXMLRegion2DSetBagType regsetbag = MTBXMLRegion2DSetBagType.Factory.newInstance();
//		regsetbag.setRegionsetsArray(regsetarray);
		
		MTBXMLRegion2DSetBagType regsetbag = this.toXMLType();
		// set xml-region2Dsetbag
		regsetbagDoc.setMTBXMLRegion2DSetBag(regsetbag);
		
		// write the xml-file
		regsetbagDoc.save(new File(filename));
		
		MTBOperator.writeHistory(this, filename);
	}
	
	/** Create a xml representation of this bag of region sets using MiToBo's xml-scheme MTBRegion2DSetBag.xsd
	 * 
	 * @return the xml representation of this bag of region sets
	 */
	public MTBXMLRegion2DSetBagType toXMLType() {
		MTBXMLRegion2DSetType[] regsetarray = new MTBXMLRegion2DSetType[this.regionsets.size()];
		for (int i = 0; i < regsetarray.length; i++) {
			regsetarray[i] = this.regionsets.get(i).toXMLType();
		}
		
		// create xml-region2Dsetbag
		MTBXMLRegion2DSetBagType regsetbag = MTBXMLRegion2DSetBagType.Factory.newInstance();
		regsetbag.setRegionsetsArray(regsetarray);
		
		return regsetbag;
	}
	
	/**
	 * Number of region sets in this bag
	 */
	public int size() {
		return this.regionsets.size();
	}
	
	/**
	 * Add a region set to this bag
	 */
	public void add(MTBRegion2DSet regionset) {
		this.regionsets.add(regionset);
	}
	
	/**
	 * Remove a region set from this bag
	 * @param regionset set to remove
	 * @return true if the region set existed in this bag and was removed, false if it was not present
	 */
	public boolean remove(MTBRegion2DSet regionset) {
		return this.regionsets.remove(regionset);
	}
	
	/**
	 * Remove a region set from this bag by its index
	 * @param idx index of the region set to remove
	 * @return region set that was removed or null if it was not present
	 */
	public MTBRegion2DSet remove(int idx) {
		return this.regionsets.remove(idx);
	}
	
	public MTBRegion2DSet get(int idx) {
		return this.regionsets.get(idx);
	}
	
	public void set(int idx, MTBRegion2DSet regionset) {
		this.regionsets.set(idx, regionset);
	}
	
	public Iterator<MTBRegion2DSet> iterator() {
		return this.regionsets.iterator();
	}
}
