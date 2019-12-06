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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTree;

/**
 * Tree datatype to represent plant roots.
 * <p>
 * All relevant data required to properly represent the nodes of a
 * plant root tree is stored in the node data objects of 
 * type {@link MTBRootTreeNodeData}.
 * 
 * @author moeller
 * @see MTBRootTreeNodeData
 */
@ALDDerivedClass
public class MTBRootTree extends MTBTree {

	/**
	 * Default constructor.
	 * <p>
	 * The tree is initialized with just a root node with empty node data.
	 */
	public MTBRootTree() {
		super(new MTBRootTreeNodeData());
	}

	/**
	 * Constructor with given data.
	 * <p>
	 * The tree is initialized with the given node data for the root node.
	 */
	public MTBRootTree(MTBRootTreeNodeData d) {
		super(d);
	}
}
