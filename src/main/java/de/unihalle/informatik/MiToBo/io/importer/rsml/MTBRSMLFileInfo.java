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

package de.unihalle.informatik.MiToBo.io.importer.rsml;

import java.util.HashMap;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;

/**
 * Class to hold all information about a single RSML file.
 *   
 * @author Birgit Moeller
 */
public class MTBRSMLFileInfo {

	/**
	 * Image hash.
	 */
	protected String imageSHA256;

	/**
	 * Full image name including path.
	 */
	protected String imageName;
	
	/**
	 * ID in time-series.
	 */
	protected int timeSeriesID;
	
	/**
	 * Number of plants.
	 */
	protected int plantCount;
	
	/**
	 * Map holding the number of roots for each plant.
	 */
	protected HashMap<Integer, Integer> rootCountPerPlant = new HashMap<Integer, Integer>();

	/**
	 * Map holding for each plant the root trees.
	 * <p>
	 * The map of root trees per plant is indexed with the root identifier. Root trees being 
	 * conencted over time share the same identifier.
	 */
	protected HashMap<Integer, HashMap<String, MTBRootTree>> rootSystems = 
			new HashMap<Integer, HashMap<String, MTBRootTree>>();

	/**
	 * Get image hash code.
	 * @return	Hash code.
	 */
	public String getImageSHA256() {
		return this.imageSHA256;
	}
	
	/**
	 * Get image name.
	 * @return	Name of image including full path.
	 */
	public String getImageName() {
		return this.imageName;
	}

	/**
	 * Get ID of file in time-series.
	 * @return	Time-series ID.
	 */
	public int getTimeSeriesID() {
		return this.timeSeriesID;
	}

	/**
	 * Get number of plants in scene.
	 * @return	Number of plants.
	 */
	public int getPlantCount() {
		return this.plantCount;
	}

	/**
	 * Get root systems for plant with given ID.
	 * @param plantID	ID of plant.
	 * @return	Hashmap of root systems indexed with root identifier.
	 */
	public HashMap<String, MTBRootTree> getRootSystem(int plantID) {
		return this.rootSystems.get(plantID);
	}
	
	/**
	 * Prints file information to standard output.
	 */
	public void print() {
		System.out.println("\t===================");
		System.out.println("\tMTB-RSML File Info:");
		System.out.println("\t===================");
		System.out.println("\t- image name: " + this.imageName);
		System.out.println("\t- plant count: " + this.plantCount);
		for (int i=0; i<this.plantCount; ++i) {
			System.out.println("\t\t- plant " + i + " => " + this.rootCountPerPlant.get(i) + " root(s)");
		}
	}
}
