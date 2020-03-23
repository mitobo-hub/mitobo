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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.segmentation;

import java.util.HashMap;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;

/** 
 * Container class to exchange information about layers between rhizoTrak and MiToBo.
 * 
 * @author Birgit Moeller
 */
public class RhizoProjectLayerMetadataContainer {
	
	/**
	 * Z coordinate of layer used as unique ID.
	 */
	protected int layerZ;
	
	/**
	 * Roi defined for given layer.
	 */
	protected MTBPolygon2D roi;
	
	/**
	 * Direction of gravitation in image of layer.
	 * <p>
	 * The direction is stored as angle in degrees with a range between 0 and 360.
	 * 0 means that the gravitational direction points to the right, 90 refers to 
	 * a direction vertically to the bottom. I.e., the definition follows standard
	 * mathematical conventions in an upper left coordinate system with angles
	 * defined relative to the x-axis.
	 */
	protected double gravitationalDirection;
	
	/**
	 * Default constructor.
	 * @param z	Z coordinate of layer.
	 */
	public RhizoProjectLayerMetadataContainer(int z) {
		this.layerZ = z;
	}

	/**
	 * Get Z coordinate of the layer.
	 * @return	Z coordinate.
	 */
	public int getLayerZ() {
		return this.layerZ;
	}
	
	/**
	 * Set region of interest.
	 * @param r	Region of interest.
	 */
	public void setROI(MTBPolygon2D r) {
		this.roi = r;
	}
	
	/**
	 * Get region of interest.
	 * @return Region of interest as polygon.
	 */
	public MTBPolygon2D getROI() {
		return this.roi;
	}
	
	/**
	 * Set gravitational direction.
	 * @param gd	Angle of gravitational direction.
	 */
	public void setGravitationalDirection(double gd) {
		this.gravitationalDirection = gd;
	}
	
	/**
	 * Get gravitational direction.
	 * @return Gravitational direction in degrees between 0 and 360.
	 */
	public double getGravitationalDirection() {
		return this.gravitationalDirection;
	}
	
}
