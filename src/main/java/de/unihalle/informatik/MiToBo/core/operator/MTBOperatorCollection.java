/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.core.operator;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.gui.ALDOperatorConfigurationFrame;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.ALDOperatorCollection;
import de.unihalle.informatik.Alida.operator.ALDOperatorCollectionElement;
import de.unihalle.informatik.Alida.operator.events.*;
import de.unihalle.informatik.MiToBo.core.gui.MTBOperatorConfigurationFrame;

/**
 * Class to manage a set of operators.
 * 
 * @param <T> Class parameter, indicating which type of operators is managed.
 * @author moeller
 */
public class MTBOperatorCollection<T extends ALDOperatorCollectionElement> 
	extends ALDOperatorCollection<T> {

	/**
	 * Default constructor.
	 * 
	 * @param type	Class of generics type, simplifies internal type handling.
	 * @throws InstantiationException	Thrown in case of error or failure.
	 * @throws ALDOperatorException		Thrown in case of error or failure. 
	 */
	public MTBOperatorCollection(Class<T> type)
	    throws InstantiationException, ALDOperatorException {
		super(type);
	}

	@Override
	protected ALDOperatorConfigurationFrame getConfigWin(ALDOperator op, 
			ALDOpParameterUpdateEventListener pListen) throws ALDOperatorException {
		return new MTBOperatorConfigurationFrame(op, pListen);
	}
}
