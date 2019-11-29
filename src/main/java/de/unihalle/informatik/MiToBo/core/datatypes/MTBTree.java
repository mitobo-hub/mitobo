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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.util.Vector;

/**
 * This class implements a simple tree data structure.
 * <p>
 * The main component of a tree is its root node. It can have an unrestricted
 * number of children. The tree structure is implicitly given by the recursive
 * (one-way) linkage of nodes to their children.
 * 
 * Each node of the tree is associated with a certain data object. Each data
 * object has to be derived from class 'TreeNodeData' to enable one of the basic
 * features of this tree - the recursive printing of data contained within the
 * tree.
 * 
 * @see MTBTreeNode
 * @see MTBTreeNodeData
 * 
 * @author Birgit Möller
 */
public class MTBTree implements Cloneable {

	/**
	 * Root node of the tree.
	 */
	protected MTBTreeNode root;

	/**
	 * Constructor for the tree.
	 * <p>
	 * An associated data object has to be provided in any case.
	 * 
	 * @param rootObject Data object associated with the root node.
	 */
	public MTBTree(MTBTreeNodeData rootObject) {
		this.root = new MTBTreeNode(rootObject);
	}

	/**
	 * Constructor for the tree for a given root node.
	 * 
	 * @param rootNode the root node
	 */
	public MTBTree(MTBTreeNode rootNode) {
		this.root = rootNode;
	}

	@Override
	public MTBTree clone() {
		MTBTreeNodeData nData = this.root.getData().clone();
		MTBTreeNode newRoot = new MTBTreeNode(nData);
		for (MTBTreeNode c : this.root.getChilds()) {
			cloneChild(newRoot, c);
		}
		MTBTree newTree = new MTBTree(newRoot);
		return newTree;
	}

	/**
	 * Helper function to recursively clone child nodes in a deep fashion.
	 * 
	 * @param np Parent node to which the clone of the child should be added.
	 * @param c  Current child node to be cloned.
	 */
	protected static void cloneChild(MTBTreeNode np, MTBTreeNode c) {
		MTBTreeNodeData nData = c.getData().clone();
		MTBTreeNode newChild = new MTBTreeNode(nData);
		np.addChild(newChild);
		for (MTBTreeNode cc : c.getChilds()) {
			cloneChild(newChild, cc);
		}
	}

	/**
	 * Access the root node.
	 * 
	 * @return Reference to the root node of the tree.
	 */
	public MTBTreeNode getRoot() {
		return this.root;
	}

	/**
	 * Prints tree data.
	 * <p>
	 * Special feature of this tree: this function prints recursively the data
	 * contained inside the tree. Prerequisite for this is that the data objects of
	 * each node provide a method for printing their data in a reasonable way, i.e.
	 * implement the method printData() of the abstract class TreeNodeData.
	 */
	public void printTree() {
		this.root.printData();
	}

	/**
	 * Get all nodes in a depth-first sorting.
	 * @return	List of all nodes of the tree.
	 */
	public Vector<MTBTreeNode> getAllNodesDepthFirst() {
		return this.root.getAllSubtreeNodesDepthFirst();
	}
}
