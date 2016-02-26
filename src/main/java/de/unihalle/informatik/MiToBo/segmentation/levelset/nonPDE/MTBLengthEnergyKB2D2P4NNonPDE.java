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

package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/** Implements the (standard) length energy using length approximation
 *  due to Kolmogorov/Boykov for tw0 phases and 4 neighborhood.
 */

@ALDParametrizedClass
public class MTBLengthEnergyKB2D2P4NNonPDE extends MTBLengthEnergyKB2D2PNonPDE {

	/**
	 * constructor
	 */
	public MTBLengthEnergyKB2D2P4NNonPDE() {
		this(0.0);
	}

	/**
	 * constructor
	 * 
	 * @param mu
	 */
	public MTBLengthEnergyKB2D2P4NNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (Kolmogorov/Boykov, 2D, two phase, 4 neighborhood)");
	}

	/** Here potentially anisotropic grids may be reflected setting weights accordingly
	 */
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		e_x = new int[]{1,0};

		e_y = new int[]{0,1};

		w = new double[]{
				(0.5*Math.PI)/(2*1), 
				(0.5*Math.PI)/(2*1) };

		return this;
	}
}
