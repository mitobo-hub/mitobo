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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassCurvature;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;

/**
 * Fixed value parameter adaptation for Kass et al. curvature energy.
 * 
 * @author Danny Misiak
 * @author Birgit Moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_KassCurvature_ParamAdaptFix extends
    MTBSnakeEnergyCD_KassCurvature_ParamAdapt {

	/**
	 * Value by which beta is decreased in each iteration.
	 */
	@ALDClassParameter(label="Percentual Beta Decrease")
	private double betaDecrease;

	/**
	 * Default constructor.
	 */
	public MTBSnakeEnergyCD_KassCurvature_ParamAdaptFix() {
		// nothing to do here
	}

	/**
	 * Construct a new updater for the internal energy using a fixed value.
	 */
	public MTBSnakeEnergyCD_KassCurvature_ParamAdaptFix(double iB, double bDecr) {
		this.initBeta = iB;
		this.betaDecrease = bDecr;
	}

	/**
	 * Get the fix beta decrease value.
	 * @return Beta decrease value.
	 */
	public double getBetaDecrease() {
		return this.betaDecrease;
	}

	@Override
  public void init(MTBSnakeEnergyCD_KassCurvature ener) {
		this.initBeta = ener.getInitBeta();
	}

	@Override
  public double[] betaUpdate(SnakeOptimizerSingle o, double[] curBetas) {
		System.out.println("[MTBSnakeE - KassCurvature-FixedUpdate] " +
				"\u03B2 update@ " + this.betaDecrease);

		//get all relevant data
		MTBSnake s = o.getCurrentSnake();
		int pNum = s.getPointNum();
		
		// new beta vector
		double[] newBetas = new double[pNum];
		for (int i = 0; i < pNum; ++i) {
			if (curBetas[0] > this.betaDecrease) {
				newBetas[i] = curBetas[0] - this.betaDecrease;
			} else {
				newBetas[i] = 0.0;
			}
		}
		return newBetas;
	}

	@Override
	public Double getMaxBeta() {
		return (new Double(this.initBeta));
	}

	@Override
	public Double getMinBeta() {
		return (new Double(0.0));
	}

	@Override
	public String toString() {
		return new String("Kass_ParamUpdate: Fixed Decrease");
	}

}
