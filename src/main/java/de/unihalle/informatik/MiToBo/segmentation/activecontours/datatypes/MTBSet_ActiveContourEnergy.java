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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes;

import java.util.Vector;

/**
 * Common interface for sets of active contour energies.
 * <p>
 * This interface is particularly necessary for unified handling of 
 * energy sets by the I/O providers.
 * 
 * @author moeller
 */
public interface MTBSet_ActiveContourEnergy {

	/**
	 * Sets list of energies.
	 * @param es	List of energies.
	 */
	public void setEnergyList(Vector<MTBActiveContourEnergy> es);

	/**
	 * Returns list of energies converted to generic type.
	 * @return	List of energies.
	 */
	public Vector<MTBActiveContourEnergy> getGenericEnergyList();

	/**
	 * Returns the energy with given index.
	 * @param i	Index of energy.
	 * @return	Energy with index i.
	 */
	public MTBActiveContourEnergy getEnergy(int i);

	/**
	 * Sets the weights for the energies.
	 * @param ws	List of weights.
	 */
	public void setWeights(Vector<Double> ws);
	
	
	/**
	 * Returns the list of weights.
	 * @return	Vector of weights.
	 */
	public Vector<Double> getWeights();
	
	/**
	 * Returns the weight of the energy with given index. 
	 * @param i	Index of requested weight.
	 * @return	Value of weight.
	 */
	public Double getWeight(int i);
}
