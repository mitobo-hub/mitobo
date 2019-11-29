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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNodeData;

/**
 * <p>
 * 
 * @author Birgit Moeller
 */
@ALDDerivedClass
public class MTBRootTreeNodeData extends MTBTreeNodeData {

	protected double xPos;

	protected double yPos;

	protected int layer;

	protected double radius;

	protected int status;

	protected int[] connectorIDs[];

	public MTBRootTreeNodeData() {

	}

	public MTBRootTreeNodeData(double x, double y) {
		this.xPos = x;
		this.yPos = y;
	}

	@Override
	public MTBRootTreeNodeData clone() {
		MTBRootTreeNodeData nDat = new MTBRootTreeNodeData(this.xPos, this.yPos);
		nDat.layer = this.layer;
		nDat.radius = this.radius;
		nDat.status = this.status;
		nDat.connectorIDs = this.connectorIDs.clone();
		return nDat;
	}

	@Override
	public void printData() {
		// TODO Auto-generated method stub
	}
}
