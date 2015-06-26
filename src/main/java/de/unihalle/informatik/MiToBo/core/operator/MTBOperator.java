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

import de.unihalle.informatik.Alida.exceptions.*;
import de.unihalle.informatik.Alida.operator.ALDOperator;

/** 
 * Abstract super class for all MiToBo operators.
 * <p>
 * Compared to its super class here specialized version providers and 
 * history database access mechanisms are configured. The port hash 
 * access object used here incorporates a special treatment of images 
 * not natively included in the Alida core.
 *  
 * @author posch
 */
abstract public class MTBOperator extends ALDOperator {

	static {
		// make sure that the correct port hash access object is used
		portHashAccess = 
			MTBOperatorConfigTools.getInstance().getPortHashAccessObject();		
	}
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected MTBOperator() throws ALDOperatorException {
	  super();
	  this.versionProvider = 
	  	MTBOperatorConfigTools.getInstance().getVersionProvider();
  }
	
	/**
	 * Init function for deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being deserialized
	 * from file, prior to handing the instance over to the user. It takes care
	 * of a proper initialization of transient member variables as they are not
	 * initialized to the default values during deserialization. 
	 * @return	Updated deserialized object.
	 */
	protected Object readResolve() {
		super.readResolve();
		return this;
	}

}

