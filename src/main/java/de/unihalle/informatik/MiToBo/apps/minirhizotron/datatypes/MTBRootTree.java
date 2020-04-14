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
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

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
	
	/**
	 * Draws the root tree to the given image.
	 * <p>
	 * The colors of the segments are chosen according to the status:
	 * <ul>
	 * <li> LIVING = green
	 * <li> DEAD = red
	 * <li> DECAYED = orange
	 * <li> GAP = yellow
	 * <li> CONNECTOR = magenta
	 * <li> VIRTUAL = gray
	 * <li> VIRTUAL_RSML = lightgreen
	 * <li> UNDEFINED = blue
	 * <li> all other = white
	 * </ul>
	 * Currently diameter information is ignored and just the centerlines are plotted.
	 * 
	 * @param img	Image to which the tree is to be drawn.
	 */
	public void drawToImage(MTBImageRGB img) {
		this.drawChilds(img, this.root);
	}
	
	/**
	 * Recursively draws all subtrees outgoing from the given node to the image. 
	 * @param img		Target image.
	 * @param node	Source node.
	 */
	private void drawChilds(MTBImageRGB img, MTBTreeNode node) {
		MTBRootTreeNodeData nd = (MTBRootTreeNodeData)node.getData();
		MTBRootTreeNodeData cnd;
		for (MTBTreeNode c: node.getChilds()) {
			cnd = (MTBRootTreeNodeData)c.getData();
			int segColor = 0x00FFFFFF;
			if (cnd.status == 0) // LIVING
				segColor = 0x0000FF00;
			else if (cnd.status == 1) // DEAD
				segColor = 0x00FF0000;
			else if (cnd.status == 2) // DECAYED
				segColor = 0x00FFA500;			
			else if (cnd.status == 3) // GAP
				segColor = 0x00FFFF00;
			else if (cnd.status == -3) // CONNECTOR
				segColor = 0x00CD93CC;
			else if (cnd.status == -2) // VIRTUAL
				segColor = 0x00939393;
			else if (cnd.status == -4) // VIRTUAL_RSML
				segColor = 0x0093cd93;
			else if (cnd.status == -1) // UNDEFINED
				segColor = 0x000000FF;			
			img.drawLine2D((int)nd.xPos, (int)nd.yPos, (int)cnd.xPos, (int)cnd.yPos, segColor);
			this.drawChilds(img, c);
		}
	}
	
	
}
