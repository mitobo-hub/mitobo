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

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * A target-ID class. Can also store time information.
 * @author Oliver Gress
 *
 */
public class TargetID implements Copyable<TargetID> {

	/** Target ID */
	public short ID;
	
	/** A variable to store time information */
	public double time;
	
	/**
	 * Constructor.
	 */
	public TargetID(short ID) {
		this.ID = ID;
		this.time = 0;
	}
	
	@Override
	public TargetID copy() {
		TargetID tid = new TargetID(this.ID);
		tid.time = this.time;
		return tid;
	}

}
