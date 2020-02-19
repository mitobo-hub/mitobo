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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassLength;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;

/**
 * Parameter adaptation for Kass et al. length penalty energy by fixed value.
 * 
 * @author Danny Misiak
 * @author Birgit Moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_KassLength_ParamAdaptFix extends
    MTBSnakeEnergyCD_KassLength_ParamAdapt {

	/**
	 * Value by which alpha is decreased in each iteration.
	 */
	@ALDClassParameter(label="Percentual Alpha Decrease")
	private double alphaDecrease= 0.0;

	/**
	 * Default constructor.
	 */
	public MTBSnakeEnergyCD_KassLength_ParamAdaptFix() {
		// nothing to do here
	}

	/**
	 * Construct a new updater for the internal energy using a fixed value.
	 */
	public MTBSnakeEnergyCD_KassLength_ParamAdaptFix(double iA, double aDecr) {
		this.initAlpha = iA;
		this.alphaDecrease = aDecr;
	}

	/**
	 * Get the fix alpha decrease value.
	 * @return Alpha decrease value.
	 */
	public double getAlphaDecrease() {
		return this.alphaDecrease;
	}

	@Override
	public void init(MTBSnakeEnergyCD_KassLength ener) {
		this.initAlpha = ener.getInitAlpha();
	}

	@Override
	public double[] alphaUpdate(SnakeOptimizerSingle o, double[] curAlphas) {
		System.out.println("[MTBSnakeE - KassLength-FixedUpdate] " +
				"\u03B1 update@ " + this.alphaDecrease);

		// get all relevant data
		MTBSnake s = o.getCurrentSnake();
		int pNum = s.getPointNum();
		
		// new alpha vector
		double[] newAlphas = new double[pNum];
		for (int i = 0; i < pNum; i++) {
			if (curAlphas[0] > this.alphaDecrease) {
				newAlphas[i] = curAlphas[0] - this.alphaDecrease;
			} else {
				newAlphas[i] = 0.0;
			}
		} 
		return newAlphas;
	}

	@Override
	public Double getMaxAlpha() {
		return (new Double(this.initAlpha));
	}

	@Override
	public Double getMinAlpha() {
		return (new Double(0.0));
	}

	@Override
	public String toString() {
		return new String("KassLength_ParamUpdate: Fixed Decrease");
	}
}
