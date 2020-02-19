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


/**
 * 
 * Class implements a MTBSkeletonGraphNode, which is a type of the MTBGraphNode.
 * Each skeleton node has an specific MTBSkeletonNodeType. The type defines if
 * the kind of the node, like a start node in the skeleton graph, a branch node
 * or a end node of the skeleton graph.
 * 
 * @see MTBGraphNode
 * 
 * 
 * @author misiak
 * 
 * @param <T>
 *          Type parameter of MTBGraphNode. Possible choices are Point, Point2D,
 *          Integer, etc.
 */
public class MTBNeuriteSkelGraphNode<T> extends MTBGraphNode<T> {

		/**
		 * Type of the MTBGraphNode
		 * 
		 * 
		 * @author misiak
		 * 
		 */
		public static enum MTBSkeletonNodeType {
				START, END, BRANCH
		}

		/**
		 * MTBSkeletonNodeType of the MTBSkeletonGraphNode. A MTBSkeletonGraph can
		 * include nodes from type start, branch and end.
		 */
		private MTBSkeletonNodeType nodeType;

		/**
		 * Constructor to create a new MTBGraphNode object.
		 * 
		 * @param data
		 *          data of the MTBGraphNode
		 * @param nodeType
		 *          type of the MTBGraphNode
		 */
		public MTBNeuriteSkelGraphNode(T data, MTBSkeletonNodeType nodeType) {
				super(data);
				this.nodeType = nodeType;
		}

		public MTBSkeletonNodeType getNodeType() {
				return this.nodeType;
		}

		protected void setNodeType(MTBSkeletonNodeType nodeType) {
				this.nodeType = nodeType;
		}

		@Override
		public String toString() {
				String nodeToString = "node data: " + data.toString() + "; #in edges: "
				    + this.inEdges.size() + "; #out edges: " + this.outEdges.size()
				    + "; node type: " + this.nodeType.toString();
				return nodeToString;
		}

}
