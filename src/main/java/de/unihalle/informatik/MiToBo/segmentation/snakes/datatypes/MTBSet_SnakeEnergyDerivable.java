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

package de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.*;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSetWeightedEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBSet_ActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyDerivable;

/**
 * Container data type for snake energies in PDE approaches.
 * <p>
 * Objects of this class contain a set of snake energies to be used in snake
 * optimization. With each energy an individual weight is associated which
 * allows application-dependent snake energy combinations. Per default the
 * weights of all energies in the set are set to 1.0.
 * <p>
 * Important notice: in MiToBo there exists a specialized data I/O provider 
 * {@link MTBSet_SnakeEnergyDerivableDataIOSwing} for this class; however, the provider
 * only supports graphical data I/O. Accordingly the class is also annotated
 * as parametrized class to allow for using the generic parametrized class data 
 * I/O provider {@link ALDParametrizedClassDataIOCmdline} when using this class 
 * in the context of commandline user interfaces. 
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class MTBSet_SnakeEnergyDerivable 
	implements MTBSet_ActiveContourEnergy {

	protected MTBSetWeightedEnergy<MTBSnakeEnergyDerivable> energySet;
	
//	/**
//	 * List of snake energies.
//	 */
//	@ALDClassParameter(label="energies", mode= ExpertMode.STANDARD, 
//			dataIOOrder = -10)
//	protected Vector<MTBSnakeEnergyDerivable> energies;
//
//	/**
//	 * List of energy weights.
//	 */
//	@ALDClassParameter(label="weights", mode= ExpertMode.STANDARD,
//			dataIOOrder = -5)
//	protected Vector<Double> weights;

	/**
	 * Default contructor for empty set.
	 */
	public MTBSet_SnakeEnergyDerivable() {
//		this.energies = new Vector<MTBSnakeEnergyDerivable>();
//		this.weights = new Vector<Double>();
		this.energySet = new MTBSetWeightedEnergy<MTBSnakeEnergyDerivable>();
	}

	/**
	 * Constructor for a given vector of energies.
	 * @param es	Vector with energies.
	 */
	public MTBSet_SnakeEnergyDerivable(Vector<MTBSnakeEnergyDerivable> es) {
//		this.energies = es;
//		Vector<Double> ws = new Vector<Double>();
//		for (int i = 0; i < es.size(); ++i)
//			ws.add(new Double(1.0));
//		this.weights = ws;
		this.energySet = new MTBSetWeightedEnergy<MTBSnakeEnergyDerivable>(es);
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
	public MTBSet_SnakeEnergyDerivable(Vector<MTBSnakeEnergyDerivable> es, Vector<Double> ws)
			throws ALDOperatorException {
//		if (es.size() != ws.size())
//			throw 
//			new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
//					"MTBSet_SnkEnergyPDE: energies and weights to fit to each other!");
//		this.energies = es;
//		this.weights = ws;
		this.energySet= new MTBSetWeightedEnergy<MTBSnakeEnergyDerivable>(es, ws);
	}

	/**
	 * Specify a new set of energies, all weights are reset to 1.0.
	 * @param es	New vector of energies.
	 */
	@Override
  public void setEnergyList(Vector<MTBActiveContourEnergy> es) {
		Vector<MTBSnakeEnergyDerivable> pdeEnergies = new Vector<MTBSnakeEnergyDerivable>();
		for (MTBActiveContourEnergy ace: es)
			if (ace instanceof MTBSnakeEnergyDerivable)
				pdeEnergies.add((MTBSnakeEnergyDerivable)ace);
		this.energySet.setEnergyList(pdeEnergies);
//		this.energies = es;
//		this.weights = new Vector<Double>();
//		for (int i = 0; i < this.energies.size(); ++i)
//			this.weights.add(new Double(1.0));
	}

	/**
	 * Specify new weights.
	 * <p>
	 * Note that it is assumed that the size of the weight vector matches 
	 * the size of the current energy set.
	 * 
	 * @param ws	List of energy weights.
	 */
	@Override
  public void setWeights(Vector<Double> ws) {
		this.energySet.setWeights(ws);
//		this.weights = ws;
	}

	/**
	 * Returns the list of energies.
	 * @return List of energies.
	 */
	@Override
  public Vector<MTBActiveContourEnergy> getGenericEnergyList() {
		Vector<MTBActiveContourEnergy> vec = 
			new Vector<MTBActiveContourEnergy>();
		for (MTBSnakeEnergyDerivable e: this.energySet.getEnergyList())
			vec.add(e);
		return vec;
//		return this.energySet.getEnergyList();
//		return this.energies;
	}

  public Vector<MTBSnakeEnergyDerivable> getEnergyList() {
		return this.energySet.getEnergyList();
//		return this.energies;
	}

  /**
	 * Returns element i of the energy list.
	 * @param i	Index of energy to be returned.
	 * @return Requested energy object.
	 */
	@Override
  public MTBSnakeEnergyDerivable getEnergy(int i) {
		return this.energySet.getEnergy(i);
//		return this.energies.elementAt(i);
	}

	/**
	 * Returns the list of weights.
	 * @return List of current weights.
	 */
	@Override
  public Vector<Double> getWeights() {
		return this.energySet.getWeights();
//		return this.weights;
	}

	/**
	 * Returns element i of the weight list.
	 * @return	Requested element of weight list.
	 */
	@Override
  public Double getWeight(int i) {
		return this.energySet.getWeight(i);
//		return this.weights.elementAt(i);
	}

	/**
	 * Append another energy to the list.
	 * @param e	New energy.
	 * @param w Weight for the energy.
	 */
	public void addEnergy(MTBSnakeEnergyDerivable e, double w) {
		this.energySet.addEnergy(e, w);
//		this.energies.add(e);
//		this.weights.add(new Double(w));
	}

	/**
	 * Append another energy to the list with default weight 1.0.
	 * @param e New energy to be appended.
	 */
	public void addEnergy(MTBSnakeEnergyDerivable e) {
		this.energySet.addEnergy(e);
//		this.energies.add(e);
//		this.weights.add(new Double(1.0));
	}

	@Override
	public String toString() {
		return this.energySet.toString();
//		String energyList = "";
//		for (int i = 0; i < this.energies.size(); i++) {
//			energyList += new String(this.energies.elementAt(i).toString() + "\n");
//		}
//		return energyList;
	}
}
