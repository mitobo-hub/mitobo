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
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociationFactory;

/**
 * Factory class for exclusive data association objects
 * @author Oliver Gress
 *
 */
public class DataAssociationExclusiveFactory implements DataAssociationFactory {

	@Override
	public DataAssociation createDataAssociation() {
		return new DataAssociationExclusive();
	}

	@Override
	public DataAssociation createDataAssociation(int numTargets,
			int numObservations) {
		return new DataAssociationExclusive();
	}

}
