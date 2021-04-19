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
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetDocument;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DType;

/**
 * A set of 2D regions living in a common domain which is a rectangular subset
 * of R x R. Each regions is of type {@link MTBRegion2D}.
 * 
 * @author Stefan Posch
 */
@ALDParametrizedClass
public class MTBRegion2DSet extends ALDData 
	implements Cloneable, MTBRegionSetInterface, Iterable<MTBRegion2D> {

		@SuppressWarnings("unused")
		private boolean debug = false;

		// subset of R x R
		/**
		 * Minimal x coordinate of the domain of this region set.
		 */
		@ALDClassParameter(label="Minimal x in domain.")
		private double xMin;

		/**
		 * Minimal y coordinate of the domain of this region set.
		 */
		@ALDClassParameter(label="Minimal y in domain")
		private double yMin;

		/**
		 * Maximal x coordinate of the domain of this region set.
		 */
		@ALDClassParameter(label="Maximal x in domain")
		private double xMax;

		/**
		 * Maximal y coordinate of the domain of this region set.
		 */
		@ALDClassParameter(label="Maximal y in domain")
		private double yMax;

		/**
		 * The set of regions represented as a Vector.
		 */
		@ALDClassParameter(label="Set of regions") 
		protected Vector<MTBRegion2D> regionSet;

		/** 
		 * Information about this region set. 
		 */
		private String info;

		/**
		 * Standard constructor.
		 */
		public MTBRegion2DSet() {
				this.xMin = 0.0;
				this.yMin = 0.0;
				this.xMax = 0.0;
				this.yMax = 0.0;
				this.info = "";
				this.regionSet = new Vector<MTBRegion2D>();
				setProperty("xMin", new Double(0.0));
				setProperty("yMin", new Double(0.0));
				setProperty("xMax", new Double(0.0));
				setProperty("yMax", new Double(0.0));
				setProperty("info", this.info);
		}

		/**
		 * Construct an empty set of regions with given extent of domain
		 */
		public MTBRegion2DSet(double _xMin, double _yMin, double _xMax, double _yMax) {
				this.xMin = _xMin;
				this.yMin = _yMin;
				this.xMax = _xMax;
				this.yMax = _yMax;
				this.info = "";
				this.regionSet = new Vector<MTBRegion2D>();
				setProperty("xMin", new Double(this.xMin));
				setProperty("yMin", new Double(this.yMin));
				setProperty("xMax", new Double(this.xMax));
				setProperty("yMax", new Double(this.yMax));
				setProperty("info", this.info);
		}

		/**
		 * Construct an empty set of regions with given extent of domain.
		 */
		public MTBRegion2DSet(Vector<MTBRegion2D> regions, double _xMin,
		    double _yMin, double _xMax, double _yMax) {
				this.xMin = _xMin;
				this.yMin = _yMin;
				this.xMax = _xMax;
				this.yMax = _yMax;
				this.info = "";

				this.regionSet = new Vector<MTBRegion2D>();
				for (int i = 0; i < regions.size(); i++)
						add(regions.elementAt(i));

				setProperty("xMin", new Double(this.xMin));
				setProperty("yMin", new Double(this.yMin));
				setProperty("xMax", new Double(this.xMax));
				setProperty("yMax", new Double(this.yMax));
				setProperty("info", this.info);
		}

		/**
		 * Construct a set of regions from a xml-file that was written by the
		 * {@link write} method of this class. (These xml-files conform to xml-scheme
		 * MTBXMLRegion2DSet.xsd defined for MiToBo)
		 * 
		 * @param filename
		 *          path to the xml-file
		 * @throws IOException
		 *           thrown if parsing of xml-file failed
		 * @throws XmlException
		 *           thrown if parsing of xml-file failed
		 */
		public MTBRegion2DSet(String filename) throws XmlException, IOException {
				this(MTBXMLRegion2DSetDocument.Factory.parse(new File(filename))
				    .getMTBXMLRegion2DSet());

				this.setLocation(filename);

				MTBOperator.readHistory(this, filename);
		}

		/**
		 * Construct a set of regions from a label image.
		 * It is assumed that each set of pixels sharing the same label
		 * constitute one connected region.
		 * Admissible image types are BYTE, SHORT and INT.
		 * 
		 * @param labelimage	Image from where to init the region set.
		 */
		public MTBRegion2DSet(MTBImage labelimage) {
			this( 0, 0, labelimage.getSizeX()-1, labelimage.getSizeY()-1);
			
			HashMap<Integer,MTBRegion2D> labelSet = new HashMap<Integer,MTBRegion2D>();
			for ( int y = 0 ; y < labelimage.getSizeY() ; y++) {
				for ( int x = 0 ; x < labelimage.getSizeX() ; x++ ) {
					int value = labelimage.getValueInt(x, y);
					MTBRegion2D region = labelSet.get(value);
					if ( region == null ) {
						region = new MTBRegion2D();
						labelSet.put(value, region);
						this.add(region);
					}
					region.addPixel(x, y);
				}
			}
		}
		
		/**
		 * Construct a set of regions from a label image.
		 * It is assumed that each set of pixels sharing the same label
		 * constitute one connected region.
		 * The background of pixels with label equals <code>backgroundLabel</code> 
		 * is excluded.
		 * <p>
		 * Admissible image types are BYTE, SHORT and INT.
		 * 
		 * @param labelimage				Image from where to init the region set.
		 * @param backgroundLabel		Label of background which is to be ignored.
		 */
		public MTBRegion2DSet(MTBImage labelimage, int backgroundLabel) {
			this( 0, 0, labelimage.getSizeX()-1, labelimage.getSizeY()-1);
			
			HashMap<Integer,MTBRegion2D> labelSet = new HashMap<Integer,MTBRegion2D>();
			for ( int y = 0 ; y < labelimage.getSizeY() ; y++) {
				for ( int x = 0 ; x < labelimage.getSizeX() ; x++ ) {
					int value = labelimage.getValueInt(x, y);
					if ( value != backgroundLabel) {
						MTBRegion2D region = labelSet.get(value);
						if ( region == null ) {
							region = new MTBRegion2D();
							labelSet.put(value, region);
							this.add(region);
						}
						region.addPixel(x, y);
					}
				}
			}
		}
		
		/**
		 * Construct a new MTBRegion2DSet from a region set object that was
		 * constructed from a xml-representation. Only used for reading region set
		 * bags ({@link MTBRegion2DSetBag}) from file.
		 * 
		 * @param xmlregion2Dset
		 *          object that represents the region set read from xml
		 */
		public MTBRegion2DSet(MTBXMLRegion2DSetType xmlregion2Dset) {

				// get bounding box
				this.xMin = xmlregion2Dset.getXMin();
				this.yMin = xmlregion2Dset.getYMin();
				this.xMax = xmlregion2Dset.getXMax();
				this.yMax = xmlregion2Dset.getYMax();
				this.info = xmlregion2Dset.getInfo();

				setProperty("xMin", new Double(this.xMin));
				setProperty("yMin", new Double(this.yMin));
				setProperty("xMax", new Double(this.xMax));
				setProperty("yMax", new Double(this.yMax));
				setProperty("info", this.info);

				// get regions
				MTBXMLRegion2DType[] regarray = xmlregion2Dset.getRegionsArray();
				this.regionSet = new Vector<MTBRegion2D>(regarray.length);

				for (int i = 0; i < regarray.length; i++) {
						this.regionSet.add(new MTBRegion2D(regarray[i]));
				}
		}


		@Override
		public MTBRegion2DSet clone() {
			MTBRegion2DSet newSet = 
					new MTBRegion2DSet(this.xMin, this.yMin, this.xMax, this.yMax);
			newSet.regionSet = new Vector<MTBRegion2D>();
			for (MTBRegion2D reg: this.regionSet) {
				newSet.regionSet.add(reg.clone());
			}
			newSet.setProperty("xMin", new Double(this.xMin));
			newSet.setProperty("yMin", new Double(this.yMin));
			newSet.setProperty("xMax", new Double(this.xMax));
			newSet.setProperty("yMax", new Double(this.yMax));
			newSet.info = new String(this.info);
			return newSet;
		}
		
		/**
		 * Construct an object that represents this region by xml. Only used for
		 * writing region set bags ({@link MTBRegion2DSetBag}) to file.
		 */
		public MTBXMLRegion2DSetType toXMLType() {
				MTBXMLRegion2DSetType xmlregion2Dset = MTBXMLRegion2DSetType.Factory
				    .newInstance();

				xmlregion2Dset.setXMin(this.xMin);
				xmlregion2Dset.setXMax(this.xMax);
				xmlregion2Dset.setYMin(this.yMin);
				xmlregion2Dset.setYMax(this.yMax);
				xmlregion2Dset.setInfo(this.info);

				MTBXMLRegion2DType[] regions = new MTBXMLRegion2DType[this.regionSet.size()];

				for (int i = 0; i < this.regionSet.size(); i++) {
						regions[i] = this.regionSet.get(i).toXMLType();
				}

				xmlregion2Dset.setRegionsArray(regions);

				return xmlregion2Dset;
		}

		/**
		 * Write this region set to disk in XML format using MiToBo's xml-scheme
		 * MTBRegion2DSet.xsd
		 */
		public void write(String filename) throws IOException,
		    ALDProcessingDAGException, ALDOperatorException {

				// generate XML documents
				MTBXMLRegion2DSetDocument regsetDoc = MTBXMLRegion2DSetDocument.Factory
				    .newInstance();

				// set xml-region2Dset
				regsetDoc.setMTBXMLRegion2DSet(this.toXMLType());

				// write the xml-file
				regsetDoc.save(new File(filename));

				MTBOperator.writeHistory(this, filename);
		}

		/**
		 * Get the number of regions of this region set
		 * 
		 * @return number of regions
		 */
		public int size() {
				return this.regionSet.size();
		}

		/**
		 * Set minimal x coordinate of region set domain.
		 * @param v	Value to set.
		 */
		public void setXmin(double v) {
			this.xMin = v;
			setProperty("xMin", new Double(this.xMin));
		}
		
		public double getXmin() {
				return this.xMin;
		}

		/**
		 * Set maximal x coordinate of region set domain.
		 * @param v	Value to set.
		 */
		public void setXmax(double v) {
			this.xMax = v;
			setProperty("xMax", new Double(this.xMax));
		}

		public double getXmax() {
				return this.xMax;
		}

		/**
		 * Set minimal y coordinate of region set domain.
		 * @param v	Value to set.
		 */
		public void setYmin(double v) {
			this.yMin = v;
			setProperty("yMin", new Double(this.yMin));
		}

		public double getYmin() {
				return this.yMin;
		}

		/**
		 * Set maximal y coordinate of region set domain.
		 * @param v	Value to set.
		 */
		public void setYmax(double v) {
			this.yMax = v;
			setProperty("yMax", new Double(this.yMax));
		}

		public double getYmax() {
				return this.yMax;
		}

		public String getInfo() {
				return this.info;
		}

		public void setInfo(String info) {
				this.info = info;
				setProperty("info", this.info);
		}

		/**
		 * Remove all regions from this set
		 */
		public void clear() {
				this.regionSet.clear();
		}

		/**
		 * Get a region by index
		 * 
		 * @return i-th region
		 */
		public MTBRegion2D get(int i) {
				return this.regionSet.get(i);
		}

		/**
		 * Get a region by index
		 * 
		 * @return i-th region
		 */
		public MTBRegion2D elementAt(int i) {
				if (i < 0 || i >= size())
						throw new ArrayIndexOutOfBoundsException();

				return this.regionSet.elementAt(i);
		}

		/**
		 * Append a region (at the end) to the set of regions.
		 * 
		 * @param region
		 *          region to add
		 */
		public boolean add(MTBRegion2D region) {
				return this.regionSet.add(region);
		}

		/**
		 * Set the i-th region from the set
		 * 
		 * @param i
		 *          index of region to remove
		 */
		public void setElementAt(MTBRegion2D region, int i) {
				this.regionSet.setElementAt(region, i);
		}

		/**
		 * Delete the i-th region from the set
		 * 
		 * @param i
		 *          index of region to remove
		 */
		public void removeElementAt(int i) {
				this.regionSet.removeElementAt(i);
		}
		
		/**
		 * Delete the given region object from the set,
		 * if contained.
		 * If the region object is contained multiple times, the first occurence
		 * is removed.
		 * 
		 * @param regionToRemove
		 */
		public void remove(MTBRegion2D regionToRemove) {
//			for ( int i = 0 ; i < regionSet.size() ; i++) {
//				if ( regionSet.get(i) == regionToRemove ) {
//					regionSet.removeElementAt(i);
//					break;
//				}
//			}
			Iterator<MTBRegion2D> iter = this.iterator();
			while ( iter.hasNext()) {
				MTBRegion2D reg = iter.next();
				if ( reg == regionToRemove) {
					regionSet.remove(regionToRemove);
					break;
				}
			}
		}
		
	
		/**
		 * Calculates the minimum size of the set.
		 * 
		 * @return minimum size of all regions, -1 if vector is empty
		 */
		public int calcMinSize() {
				if (this.regionSet.size() == 0)
						return -1;
				int minSize = Integer.MAX_VALUE;
				for (int i = 0; i < this.regionSet.size(); ++i) {
						MTBRegion2D r = this.regionSet.elementAt(i);
						if (r.getArea() < minSize) {
								minSize = r.getArea();
						}
				}
				return minSize;
		}

		/**
		 * Calculates the maximum size of the set.
		 * 
		 * @return maximum size of all regions, -1 if vector is empty
		 */
		public int calcMaxSize() {
				if (this.regionSet.size() == 0)
						return -1;
				int maxSize = Integer.MIN_VALUE;
				for (int i = 0; i < this.regionSet.size(); ++i) {
						MTBRegion2D r = this.regionSet.elementAt(i);
						if (r.getArea() > maxSize) {
								maxSize = r.getArea();
						}
				}
				return maxSize;
		}
		

		/**
		 * Calculates the average size of the set.
		 * 
		 * @return average size of all regions, -1 if vector is empty
		 */
		public double calcAverageSize() {

				if (this.regionSet.size() == 0)
						return -1;

				double avgSize = 0;
				for (int i = 0; i < this.regionSet.size(); ++i) {
						MTBRegion2D r = this.regionSet.elementAt(i);
						avgSize = avgSize + r.getArea();
				}
				return avgSize / this.regionSet.size();
		}

		public MTBRegion2DSet selectLargeRegions(int minSize)
		    throws ALDOperatorException, ALDProcessingDAGException {
				RegionSelector rs = new RegionSelector(this, minSize);
				rs.runOp(null);
				return rs.outRegs;
		}

		// public MTBImageShort makeLabelImageGray(int imgW, int imgH)
		// throws MTBOperatorException {
		// ImageMaker im = new ImageMaker(this, imgW, imgH);
		// return (MTBImageShort) im.getOutput("outImg");
		// }

		private class RegionSelector extends MTBOperator {

				@Parameter(label = "minSize", required = true, type = Parameter.Type.PARAMETER, description = "Minimal size")
				int minSize = 0;

				@Parameter(label = "inRegs", required = true, type = Parameter.Type.INPUT, description = "Input regions")
				MTBRegion2DSet inRegs = null;

				@Parameter(label = "outRegs", required = true, type = Parameter.Type.OUTPUT, description = "Output regions")
				MTBRegion2DSet outRegs = null;

				@SuppressWarnings("unused")
				public RegionSelector() throws ALDOperatorException {
						// nothing to do here
				}

				public RegionSelector(MTBRegion2DSet input, int msize)
				    throws ALDOperatorException {
						this.inRegs = input;
						this.minSize = msize;
				}

				@Override
				protected void operate() {
						this.outRegs = new MTBRegion2DSet(this.inRegs.getXmin(), this.inRegs
						    .getYmin(), this.inRegs.getXmax(), this.inRegs.getYmax());
						for (int i = 0; i < this.inRegs.size(); ++i) {
								MTBRegion2D reg = this.inRegs.elementAt(i);
								if (reg.getArea() >= this.minSize)
										this.outRegs.add(reg);
						}
				}
		}

		@Override
		public Iterator<MTBRegion2D> iterator() {
			Iterator<MTBRegion2D> iter = regionSet.iterator();
			return iter;
		}
}
