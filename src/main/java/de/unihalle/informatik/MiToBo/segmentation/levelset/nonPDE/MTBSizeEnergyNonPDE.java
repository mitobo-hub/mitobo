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


import java.io.PrintStream;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * A class to implement the size energy of the Chan-Vese energy.
 *
 * @author Stefan Posch
 *
 */

@ALDDerivedClass
@ALDParametrizedClass
public class MTBSizeEnergyNonPDE extends MTBGenericEnergyNonPDE {
	
	/** weight for this energy
	 */
	@ALDClassParameter(label="nu")
	private final double nu;
	
	/** 
	 * Constructor for size energy with weight nu.
	 *	 */
	public MTBSizeEnergyNonPDE() {
		this.name = new String( "MTBSizeEnergyNonPDE");
		this.nu = 0;
	}

	/** 
	 * Constructor for size energy with weight nu.
	 *
	 * @param nu weight of size energy
	 */
	public MTBSizeEnergyNonPDE(double nu) {
		this.name = new String( "MTBSizeEnergyNonPDE");
		this.nu = nu;
	}

@Override
	public double deltaE(int x, int y, int z, MTBLevelsetMembership phi) {
		short val = phi.getPhase(x, y, z);
		if(val <= MTBLevelsetMembership.BG_PHASE) 
			return this.nu;
		else 
			return (-1.0) * this.nu;
	}
	
	@Override
	public double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
		short val = phi.getPhase(x, y, z);
		if(val <= MTBLevelsetMembership.BG_PHASE) 
			return this.nu;
		else 
			return (-1.0) * this.nu;
	}
	
	@Override
	public double E(MTBLevelsetMembership phi) {
		return this.nu * phi.getPixelInside();
	}
	
	@Override
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
	}

	@Override
	public  String 	toString() {
        return name + ": nu = " + nu;
    }

	@Override
	public void print( MTBLevelsetMembership LSM, PrintStream out, String indent) {
		out.println( indent + name + ": " + E( LSM));
	}

}
