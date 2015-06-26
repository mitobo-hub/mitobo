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
 * Abstract class to define properties for data objects associated with
 * TreeNodes. Each data object has to provide a method printData() for printing
 * its data in a reasonable way. In addition, it will by default contain a
 * reference to the TreeNode it is associated with.
 * 
 * @see MTBTree
 * @see MTBTreeNode
 * 
 * @author Birgit Möller
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public abstract class MTBTreeNodeData {

		/**
		 * Reference to the associated node of the tree.
		 */
		protected MTBTreeNode node;

		/**
		 * Sets the node the object is associated with.
		 * 
		 * @param n	TreeNode the data object belongs to.
		 */
		public void setNode(MTBTreeNode n) {
				this.node = n;
		}

		/**
		 * Return the node object containing the data.
		 * 
		 * @return Node the data is associated with.
		 */
		public MTBTreeNode getNode() {
				return this.node;
		}

		/**
		 * Method to print the data associated with the node.
		 */
		public abstract void printData();
}
