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

package de.unihalle.informatik.MiToBo.apps.particles2D;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Common super class for all particle detectors.
 * <p>
 * This class is mainly used for enabling Alida/MiToBo operators to 
 * handle different particle detection algorithms in a generic fashion.
 * 
 * @author moeller
 */
public abstract class ParticleDetector extends MTBOperatorControllable {

	/**
	 * Empty constructor.
	 */
	public ParticleDetector() throws ALDOperatorException {
		// nothing to be done here
	}

  @Override
  protected abstract void operate() 
		throws ALDOperatorException, ALDProcessingDAGException;
}
