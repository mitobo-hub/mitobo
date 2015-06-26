/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2011
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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.*;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.*;

/**
 * Container data type for energies in PDE approaches.
 * <p>
 * Objects of this class contain a set of energies.
 * With each energy an individual weight is associated which
 * allows application-dependent energy combinations. Per default the
 * weights of all energies in the set are set to 1.0.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class MTBSetWeightedEnergy<T> {

	/**
	 * List of snake energies.
	 */
	@ALDClassParameter(label="energies", mode= ExpertMode.STANDARD, 
			dataIOOrder = -10)
	protected Vector<T> energies;

	/**
	 * List of energy weights.
	 */
	@ALDClassParameter(label="weights", mode= ExpertMode.STANDARD,
			dataIOOrder = -5)
	protected Vector<Double> weights;

	/**
	 * Default contructor for empty set.
	 */
	public MTBSetWeightedEnergy() {
		this.energies = new Vector<T>();
		this.weights = new Vector<Double>();
	}

	/**
	 * Constructor for a given vector of energies.
	 * @param es	Vector with energies.
	 */
	public MTBSetWeightedEnergy(Vector<T> es) {
		this.energies = es;
		Vector<Double> ws = new Vector<Double>();
		for (int i = 0; i < es.size(); ++i)
			ws.add(new Double(1.0));
		this.weights = ws;
	}

	/**
	 * Constructor with energies and weights.
	 * <p>
	 * Both vectors must have the same size, otherwise an exception is thrown.
	 * 
	 * @param es	Vector of energies.
	 * @param ws	Vector of weights.
	 * @throws ALDOperatorException
	 */
	public MTBSetWeightedEnergy(Vector<T> es, Vector<Double> ws)
			throws ALDOperatorException {
		if (es.size() != ws.size())
			throw 
			new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"MTBSet_SnkEnergyPDE: energies and weights to fit to each other!");
		this.energies = es;
		this.weights = ws;
	}

	/**
	 * Specify a new set of energies, all weights are reset to 1.0.
	 * @param es	New vector of energies.
	 */
	public void setEnergyList(Vector<T> es) {
		this.energies = es;
		this.weights = new Vector<Double>();
		for (int i = 0; i < this.energies.size(); ++i)
			this.weights.add(new Double(1.0));
	}

	/**
	 * Specify new weights.
	 * <p>
	 * Note that it is assumed that the size of the weight vector matches 
	 * the size of the current energy set.
	 * 
	 * @param ws	List of energy weights.
	 */
	public void setWeights(Vector<Double> ws) {
		this.weights = ws;
	}

	/**
	 * Returns the list of energies.
	 * @return List of energies.
	 */
	public Vector<T> getEnergyList() {
		return this.energies;
	}

	/**
	 * Returns element i of the energy list.
	 * @param i	Index of energy to be returned.
	 * @return Requested energy object.
	 */
	public T getEnergy(int i) {
		return this.energies.elementAt(i);
	}

	/**
	 * Returns the list of weights.
	 * @return List of current weights.
	 */
	public Vector<Double> getWeights() {
		return this.weights;
	}

	/**
	 * Returns element i of the weight list.
	 * @return	Requested element of weight list.
	 */
	public Double getWeight(int i) {
		return this.weights.elementAt(i);
	}

	/**
	 * Append another energy to the list.
	 * @param e	New energy.
	 * @param w Weight for the energy.
	 */
	public void addEnergy(T e, double w) {
		this.energies.add(e);
		this.weights.add(new Double(w));
	}

	/**
	 * Append another energy to the list with default weight 1.0.
	 * @param e New energy to be appended.
	 */
	public void addEnergy(T e) {
		this.energies.add(e);
		this.weights.add(new Double(1.0));
	}

	@Override
	public String toString() {
		String energyList = "";
		for (int i = 0; i < this.energies.size(); i++) {
			energyList += new String(this.energies.elementAt(i).toString() + "\n");
		}
		return energyList;
	}
}
