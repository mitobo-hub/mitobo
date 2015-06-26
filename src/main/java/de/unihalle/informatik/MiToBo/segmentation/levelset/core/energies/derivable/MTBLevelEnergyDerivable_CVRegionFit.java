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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core.energies.derivable;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.energies.MTBActiveContourEnergy_CVRegionFit;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBLevelsetException;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.DeltaApproxHelper;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.LevelsetSolverDerivatives;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBLevelsetFunctionDerivable;

/**
 * Implementation of Chan-Vese energy for level set functions.
 * 
 * @author Martin Scharm
 * @author Birgit Moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBLevelEnergyDerivable_CVRegionFit
	extends MTBActiveContourEnergy_CVRegionFit
		implements MTBLevelsetEnergyDerivable {

	/**
	 * Flag to indicate if the Heaviside function should be approximated.
	 */
	private boolean useHeavisideApprox = true;

  /**
   * Helper object to calculate values of approximated function.
   */
  private DeltaApproxHelper deltaHelper;
    
  /**
   * Default constructor.
   */
  public MTBLevelEnergyDerivable_CVRegionFit() {
  	// nothing to do here
  }

  /**
   * Constructor with given image.
   * <p>
   * Other paramters are set to lambda_in = lambda_out = mu = 1 nu = 0.
   * @param img Image to process.
   */
  public MTBLevelEnergyDerivable_CVRegionFit(MTBImage img) {
  	super(img, new double[]{1.0}, new double[]{1.0});
  }

  @Override
  public String toString() {
  	return "ChanVeseFitting( Lambda in = " + 
  		this.lambda_in + ", Lambda out = " + this.lambda_out + ")";
  }

  @Override
  public double getDerivative(MTBLevelsetFunctionDerivable phi,
  		int x, int y, int z) {

  	if (this.useHeavisideApprox) {
  		return this.deltaHelper.getValue(phi.get(x, y, z))
  				* super.getDerivative(phi, x, y, z);
  	}
  	return super.getDerivative(phi, x, y, z);
  }

  @Override
  public double getDerivativeWithoutDelta(MTBLevelsetFunctionDerivable phi,
  		int x, int y, int z) {
  	return super.getDerivative(phi, x, y, z);
  }

  @Override
  public void updateStatus(MTBLevelsetFunctionDerivable phi)
  		throws MTBLevelsetException {
  	try {
  		super.updateParameters(phi);
  	} catch (MTBActiveContourException e) {
  		throw new MTBLevelsetException(ExceptionType.UPDATE_ERROR,
  				"MTBLevelEnergyPDE_CVRegionFit - "
  						+ "update failed! Reason: " + e.getCommentString());
  	}
  }

  @Override
  public boolean initEnergy(LevelsetSolverDerivatives solver) {
  	this.inImg = solver.getInputImg();
  	this.deltaHelper = solver.getDeltaApproximator();
  	try {
  		init();
    	return true;
  	} catch (MTBActiveContourException e) {
  		return false;
  	}
  }

  @Override
  public String validate() {
  	if(    this.lambda_in[0] + this.lambda_out[0] > 1 
  			|| this.lambda_in[0] + this.lambda_out[0] < 0)
  		return "Energy: CVRegionFit Lambda_in + Lambda_out have to be in [0,1]!";
  	return null;
  }

  @Override
  public void useHeavideApproximation(boolean flag) {
  	this.useHeavisideApprox = flag;
  }
}
