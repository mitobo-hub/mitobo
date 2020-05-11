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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTree;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNodeData;
import ij.IJ;

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
public class MTBRootTree extends MTBTree implements Cloneable {

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
	
	/**
	 * Constructor with given root node.
	 * <p>
	 * The tree is initialized with the given node as the root node.
	 */
	public MTBRootTree(MTBTreeNode n) {
		super(n);
	}
	
	/**
	 * Constructor with given set of 2D points.
	 * @param point2DSet 2D points that are assumed to be in order and represent a line.
	 */
	public MTBRootTree(MTBPoint2DSet point2DSet) {
		this();
		
		if(point2DSet.size() > 0) {
			
			MTBTreeNodeData rootData = new MTBRootTreeNodeData(point2DSet.get(0).getX(), point2DSet.get(0).getY());
			MTBTreeNode currentNode = new MTBTreeNode(rootData);
			this.root = currentNode;
			
			for(int i = 1; i < point2DSet.size(); i++) {
				MTBTreeNode childNode = new MTBTreeNode(new MTBRootTreeNodeData(point2DSet.get(i).getX(), point2DSet.get(i).getY()));
				currentNode.addChild(childNode);
				currentNode = childNode;
			}
		}
	}
	
	/**
	 * Sets the given node as the new root.
	 * @param node A node that is part of this tree.
	 * @return Success or failure.
	 */
	public boolean reroot(MTBTreeNode node)
	{
		if(null == node) return false;
		if(!getAllNodesDepthFirst().contains(node)) return false;
		
		node.setRoot();
		this.root = node;
		
		return true;
	}

	@Override
	public MTBRootTree clone() {
		MTBRootTreeNodeData nData = (MTBRootTreeNodeData) this.root.getData().clone();
		MTBTreeNode newRoot = new MTBTreeNode(nData);
		for (MTBTreeNode c : this.root.getChilds()) {
			cloneChild(newRoot, c);
		}
		MTBRootTree newTree = new MTBRootTree(newRoot);
		return newTree;
	}
}
