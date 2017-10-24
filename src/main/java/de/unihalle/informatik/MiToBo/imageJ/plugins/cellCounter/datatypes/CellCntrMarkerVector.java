/* IMPORTANT NOTICE:
 * This file has originally been part of the Cell_Counter plugin written by
 * Kurt De Vos, see http://rsb.info.nih.gov/ij/plugins/cell-counter.html.
 * We extended the plugin functionality to fit to the specific needs of MiToBo. 
 * You can find the original license and file header below following the 
 * MiToBo license header.
 */

/*
 * Copyright (C) 2010 - @YEAR@ by the MiToBo development team
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

/* === Original File Header === */

/*
 * MarkerVector.java
 *
 * Created on December 13, 2005, 8:40 AM
 *
 */
/*
 *
 * @author Kurt De Vos 2005
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 *
 */

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes;

import java.awt.Color;
import java.awt.Point;
import java.util.ListIterator;
import java.util.Vector;

/**
 * Class to represent a set of markers of a single type.
 *
 * @author Kurt De Vos
 * @author Birgit Moeller
 */
public class CellCntrMarkerVector extends Vector<CellCntrMarker> {
	/**
	 * Type ID of markers.
	 */
	private int type;
	/**
	 * Color of markers.
	 */
	private Color color;

//	/**
//   * Set of presegmented set particle regions.
//   * <p>
//   * This set might be empty. Also not for each marker a region might be 
//   * available. Usually only for a subset of the first n markers regions are
//   * at best available.
//   */
//  private CellCntrSegResult segmentationData = null;

	/** 
	 * Creates a new instance of MarkerVector.
	 * @param		type	ID of the markers. 
	 */
	public CellCntrMarkerVector(int type) {
		super();
		this.type=type;
		this.color = createColor(type);
	}
	
	/**
	 * Adds a marker to the vector.
	 * @param marker	Marker to be added.
	 */
	public void addMarker(CellCntrMarker marker){
		add(marker);
	}

	/**
	 * Returns a marker from the vector.
	 * @param n		Index of marker to be returned.
	 * @return	Marker with index n.
	 */
	public CellCntrMarker getMarker(int n){
		return get(n);
	}
	
	/**
	 * Returns the index of the given marker.
	 * @param marker	Marker for which to retrieve its index.
	 * @return	Index of given marker in vector.
	 */
	public int getVectorIndex(CellCntrMarker marker){
		return indexOf(marker);
	}

	/**
	 * Deletes marker with given index from vector.
	 * @param n		Index of marker to be deleted.
	 */
	public void removeMarker(int n){
		remove(n);
//		// if segmentation data is given for the marker remove it, too 
//		if (   this.segmentationData != null 
//				&& n < this.segmentationData.getBorderCount())
//			this.segmentationData.removeItem(n);
	}
	
	/**
	 * Removes the last marker from the vector.
	 */
	public void removeLastMarker(){
		// remember current number of markers
		int originalSize = this.size();
		// only remove the last marker if there is at least one marker in the list
		if (this.size()>0)
			super.removeElementAt(size()-1);
//		// if there is segmentation data available for the marker remove it, too 
//		if (   this.segmentationData != null 
//				&& originalSize <= this.segmentationData.getBorderCount())
//			this.segmentationData.removeLastItem();
	}

	/**
	 * Delete shape data during reset of vector.
	 */
	public void clearShapeData() {
//		this.segmentationData = null;
		ListIterator<CellCntrMarker> it = this.listIterator();
		while(it.hasNext()){
			CellCntrMarker m = it.next();
			m.resetShape();
		}
	}
	
//	private boolean isCloser(CellCntrMarker m1,CellCntrMarker m2, Point p){
//		Point2D p1 = new Point2D.Double(m1.getX(), m1.getY());
//		Point2D p2 = new Point2D.Double(m1.getX(), m2.getY());
//		System.out.println("px = "+p.x+ " py = "+p.y);
//		System.out.println(Math.abs(p1.distance(p)) + " < "+ Math.abs(p2.distance(p)));
//		return (Math.abs(p1.distance(p)) < Math.abs(p2.distance(p)));
//	}

	/**
	 * Get marker closest to given point position in given slice.
	 * @param p						Point position.
	 * @param sliceIndex	Index of slice.
	 * @return	Closest marker, null if no marker in slice present.
	 */
	public CellCntrMarker getMarkerFromPosition(Point p, int sliceIndex){
		Vector<CellCntrMarker> v = new Vector<CellCntrMarker>();
		ListIterator<CellCntrMarker> it = this.listIterator();
		while(it.hasNext()){
			CellCntrMarker m = it.next();
			// check if marker is in correct slice
			if (m.getZ()==sliceIndex){
				v.add(m);
			}
		}
		// safety check: any marker found?
		if (v.size() == 0)
			return null;
		// init search with first marker as reference
		CellCntrMarker currentsmallest = v.get(0);
		Point p1 = new Point(currentsmallest.getX(),currentsmallest.getY());
		double currentsmallestdist = Math.abs(p1.distance(p));
		for (int i=1; i<v.size(); i++){
			CellCntrMarker m2 = v.get(i);
			Point p2 = new Point(m2.getX(),m2.getY());
			if (currentsmallestdist > Math.abs(p2.distance(p))) {
				currentsmallest = m2;
				currentsmallestdist = Math.abs(p2.distance(p));
			}
		}
		return currentsmallest;
	}

	/**
	 * Get type of markers.
	 * @return	The type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set type of markers.
	 * @param type	Type to set.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get color of markers.
	 * @return	Marker color.
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set color of markers.
	 * @param color		Color to set.
	 */
	public void setColor(Color color) {
		this.color = color;
	}
	
	/**
	 * Check if there is shape data available.
	 * @return	True if at least for one marker shape data is available.
	 */
	public boolean shapeDataAvailable() {
		boolean dataFound = false;
		ListIterator<CellCntrMarker> it = this.listIterator();
		while(it.hasNext() && !dataFound) {
			if (it.next().getShape() != null)
				dataFound = true;
		}
		return dataFound;
	}
	
  /**
   * Get the current (pre-)segmentation data.
   * @return	Segmentation data, might be null.
   */
//  public CellCntrSegResult getSegmentationData() {
//  	return this.segmentationData;
//  }
  
  /**
   * Set (pre-)segmentation data.
   * @param p		Segmentation data to attach to this marker set.
   */
//  public void setSegmentationData(CellCntrSegResult p) {
//  	this.segmentationData = p;
//  }
  
	/**
	 * Returns a marker color according to given type ID.
	 * <p>
	 * For ID from 1 to 8 pre-defined colors are used. For IDs greater than
	 * 8 a random color is chosen.
	 * 
	 * @param typeID		ID of the markers.
	 * @return Color for the marker type.
	 */
	public static Color createColor(int typeID){
		switch(typeID){
		case(1):
			return Color.blue;
		case(2):
			return Color.cyan;
		case(3):
			return Color.green;
		case(4):
			return Color.magenta;
		case(5):
			return Color.orange;
		case(6):
			return Color.pink;
		case(7):
			return Color.red;
		case(8):
			return Color.yellow;
		default:
			// by default, choose a color by random
			int red = (int)(255*Math.random());
			int green = (int)(255*Math.random());
			int blue = (int)(255*Math.random());
			Color c = new Color(red, green, blue);
			// make sure that color is different from pre-defined ones
			while(	  c.equals(Color.blue)	 || c.equals(Color.cyan) 
					   || c.equals(Color.green)  || c.equals(Color.magenta) 
					   || c.equals(Color.orange) || c.equals(Color.pink) 
					   || c.equals(Color.red)    ||	c.equals(Color.yellow)){
				red = (int)(255*Math.random());
				green = (int)(255*Math.random());
				blue = (int)(255*Math.random());
				c = new Color(red, green, blue);
			}
			return c;
		}
	}
}
