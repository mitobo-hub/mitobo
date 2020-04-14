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

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;

/**
 * Class to hold all information about a single RSML file.
 *   
 * @author Birgit Moeller
 */
public class MTBRSMLFileInfo {

	@Parameter( label= "Root Systems",  
		direction=Direction.OUT, dataIOOrder=0, mode=ExpertMode.STANDARD,
		description = "Root systems indexed with plant ID.")
	protected HashMap<Integer, Vector<MTBRootTree>> rootSystems;

	public String imageSHA256;

	public String imageName;
	
	public int plantCount;
	
	public HashMap<Integer, Integer> rootCountPerPlant = new HashMap<Integer, Integer>();
	
	/**
	 * Get root systems from RSML file.
	 * @return	Hashmap of root systems indexed with plant ID.
	 */
	public HashMap<Integer, Vector<MTBRootTree>> getRootSystems() {
		return this.rootSystems;
	}
	
	public void print() {
		System.out.println("\t===================");
		System.out.println("\tMTB-RSML File Info:");
		System.out.println("\t===================");
		System.out.println("\t- image name: " + this.imageName);
		System.out.println("\t- plant count: " + this.plantCount);
		for (int i=0; i<this.plantCount; ++i) {
			System.out.println("\t\t- plant " + i + " => " + this.rootCountPerPlant.get(i) + " roots");
		}
	}
}
