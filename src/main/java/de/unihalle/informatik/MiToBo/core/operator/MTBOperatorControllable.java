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

package de.unihalle.informatik.MiToBo.core.operator;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperatorControllable;

/**
 * Operator class with inherent event handling for execution control.
 * <p>
 * This operator acts as listener for `Alida` control events. On 
 * receiving events the operator changes its control status which may be 
 * used to control operator execution, and in particular to interrupt 
 * calculations in a controlled fashion, i.e. to keep results already 
 * obtained so far.
 * 
 * @author moeller
 */
public abstract class MTBOperatorControllable 
	extends ALDOperatorControllable {

	static {
		// make sure that the correct port hash access object is used
		portHashAccess = 
			MTBOperatorConfigTools.getInstance().getPortHashAccessObject();		
	}

  /**
   * Default constructor.
   * @throws ALDOperatorException
   */
  public MTBOperatorControllable() throws ALDOperatorException {
	  super();
	  this.versionProvider = 
	  	MTBOperatorConfigTools.getInstance().getVersionProvider();
  }
}
