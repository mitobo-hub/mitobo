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
 * Marker.java
 *
 * Created on December 13, 2005, 8:41 AM
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

/**
 * Class to represent a single marker in the image.
 * <p>
 * In the MTB Cell Counter in addition to a position a marker also owns 
 * a shape, e.g., a region contour. 
 *
 * @author Kurt De Vos
 * @author Birgit Moeller
 */
public class CellCntrMarker {
	
    /**
     * x-coordinate of marker.
     */
    private int x;
    /**
     * y-coordinate of marker.
     */
    private int y;
    /**
     * z-coordinate of marker.
     */
    private int z;
    
    /**
     * (Optional) shape of marker.
     */
    private CellCntrMarkerShape shape = null;
    
    /**
     * Flag to indicate if marker is active, by default true.
     */
    private boolean isActive = true;
    
    /** 
     * Creates a new instance of marker class.
     */
    @SuppressWarnings("unused")
		private CellCntrMarker() {
    	// nothing to do here
    }
    
    /**
     * Constructor with given coordinates and shape.
     * @param xc	x-coordinate of marker.
     * @param yc	y-coordinate of marker.
     * @param zc	z-coordinate of marker.
     * @param s		Shape of marker.
     */
    public CellCntrMarker(int xc, int yc, int zc, CellCntrMarkerShape s) {
    	this.x = xc;
    	this.y = yc;
    	this.z = zc;
    	this.shape = s;
    	this.isActive = true;
    }

    /**
     * Get x-coordinate.
     * @return	x-coordinate of marker.
     */
    public int getX() {
    	return this.x;
    }

    /**
     * Set x-coordinate of marker.
     * @param xc	New x-coordinate.	
     */
    public void setX(int xc) {
    	this.x = xc;
    }

    /**
     * Get y-coordinate.
     * @return	y-coordinate of marker.
     */
    public int getY() {
    	return this.y;
    }

    /**
     * Set y-coordinate of marker.
     * @param yc	New y-coordinate.	
     */
    public void setY(int yc) {
    	this.y = yc;
    }

    /**
     * Get z-coordinate.
     * @return	z-coordinate of marker.
     */
    public int getZ() {
    	return this.z;
    }

    /**
     * Set z-coordinate of marker.
     * @param zc	New z-coordinate.	
     */
    public void setZ(int zc) {
    	this.z = zc;
    }

    /**
     * Get shape of marker.
     * @return	Shape object.
     */
    public CellCntrMarkerShape getShape() {
    	return this.shape;
    }
    
    /**
     * Set shape of marker.
     * @param s	New shape object.
     */
    public void setShape(CellCntrMarkerShape s) {
    	this.shape = s;
    }
    
    /**
     * Delete shape object, i.e. reset to null.
     */
    public void resetShape() {
    	this.shape = null;
    }
    
    /**
     * Check if marker is active.
     * @return	True if marker is active, otherwise false.
     */
    public boolean isActive() {
    	return this.isActive;
    }
    
    /**
     * Set marker active.
     */
    public void setActive() {
    	this.isActive = true;
    }
    
    /**
     * Set marker inactive.
     */
    public void setInactive() {
    	this.isActive = false;
    }
}
