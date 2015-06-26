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

package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * Class to create a TreeNodeData object for a 2D region (MTBRegion2D).
 * 
 * @author gress
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBTreeNodeRegion2D extends MTBTreeNodeData {

	/**
	 * Tree level (level identifier).
	 */
	protected int m_level;

	/**
	 * 2D region object.
	 */
	protected MTBRegion2D m_reg;

	/**
	 * Construct a new MTBTreeNodeRegion2D from the given 2D region object.
	 * 
	 * @param reg	2D region.
	 */
	public MTBTreeNodeRegion2D(MTBRegion2D reg) {
		this.m_level = -1;
		this.m_reg = reg;
		this.node = null;
	}

	/**
	 * Return the 2D region.
	 * 
	 * @return 2D region object.
	 */
	public MTBRegion2D getRegion() {
		return this.m_reg;
	}

	/**
	 * Return the tree level (level identifier).
	 */
	public int getLevel() {
		return this.m_level;
	}

	/**
	 * Set the tree level (level identifier).
	 * 
	 * @param level	Tree level.
	 */
	public void setLevel(int level) {
		this.m_level = level;
	}

	@Override
	public void printData() {
		System.out.println("Level: " + this.m_level + " - RegionID: " 
																											+ this.m_reg.getID());
	}
}
