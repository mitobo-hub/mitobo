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
 * $Rev:$
 * $Date:$
 * $Author:$
 * 
 */

package de.unihalle.informatik.MiToBo.core.datatypes.wrapper;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * an Integer wrapper class to be used as input or output for MTB operators<br/>
 * 
 * @author glass
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBIntegerData
{
	private Integer value;
	
	/**
	 * 
	 * @param value
	 */
	public MTBIntegerData(Integer value)
	{
		this.value = value;
	}
	
	
	/**
	 * 
	 * @return value of this object
	 */
	public Integer getValue()
	{
		return this.value;
	}
	
	
	/**
	 * sets the value for this object
	 * @param value
	 */
	public void setValue(Integer value)
	{
		this.value = value;
	}
	
	@Override
	public String toString() {
	    return Integer.toString(value.intValue());
	}
}
