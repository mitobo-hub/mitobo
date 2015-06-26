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

import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * Class implements MTBGraphNodes for the MTBGraph. Each MTBGraphNode has data
 * and incoming and outgoing edges. If the edge is undirected, there is no
 * difference between incoming and outgoing edges.
 * 
 * @see MTBGraphEdge
 * 
 * 
 * @author misiak
 * 
 * @param <T>
 *          Type parameter of MTBGraphNode. Possible choices are Point, Point2D,
 *          Integer, etc.
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBGraphNode<T> {

		/**
		 * MTBGraphNode data. Type of the data is generic.
		 */
		protected T data;
		/**
		 * Vector of incoming edges of the MTBGraphNode.
		 */
		protected Vector<MTBGraphEdge> inEdges;
		/**
		 * Vector of outgoing edges of the MTBGraphNode.
		 */
		protected Vector<MTBGraphEdge> outEdges;

		/**
		 * Constructor to create a new MTBGraphNode object.
		 * 
		 * @param data
		 *          data of the MTBGraphNode
		 */
		public MTBGraphNode(T data) {
				this.data = data;
				this.inEdges = new Vector<MTBGraphEdge>(0);
				this.outEdges = new Vector<MTBGraphEdge>(0);
		}

		/**
		 * Get input edges for node.
		 * 
		 * @return Vector if incoming MTBGraphEdges.
		 */
		public Vector<MTBGraphEdge> getInEdges() {
				return this.inEdges;
		}

		/**
		 * Add a incoming MTBGraphEdge to the MTBGraphNode
		 * 
		 * @param inEdge
		 *          incoming MTBGraphEdge to add
		 */
		protected void addInEdge(MTBGraphEdge inEdge) {
				this.inEdges.addElement(inEdge);
		}

		/**
		 * Remove a incoming MTBGraphEdge from the MTBGraphNode.
		 * 
		 * @param inEdge
		 *          input edge to remove
		 */
		protected void removeInEdge(MTBGraphEdge inEdge) {
				if (!this.inEdges.isEmpty()) {
						this.inEdges.removeElement(inEdge);
				}
		}

		/**
		 * Get outgoing edges for node.
		 * 
		 * @return Vector if outgoing MTBGraphEdges.
		 */
		public Vector<MTBGraphEdge> getOutEdges() {
				return this.outEdges;
		}

		/**
		 * Add a outgoing MTBGraphEdge to the MTBGraphNode
		 * 
		 * @param outEdge
		 *          outgoing MTBGraphEdge to add
		 */
		protected void addOutEdge(MTBGraphEdge outEdge) {
				this.outEdges.addElement(outEdge);
		}

		/**
		 * Remove a outgoing MTBGraphEdge from the MTBGraphNode.
		 * 
		 * @param outEdge
		 *          outgoing edge to remove
		 */
		protected void removeOutEdge(MTBGraphEdge outEdge) {
				if (!this.outEdges.isEmpty()) {
						this.outEdges.removeElement(outEdge);
				}
		}

		/**
		 * Get all edges of the node. No decision between input edges or output edges
		 * is made.
		 * 
		 * @return Vector of all MTBGraphEdges of the MTBGraphNode.
		 */
		public Vector<MTBGraphEdge> getAllEdges() {
				Vector<MTBGraphEdge> allEdges = new Vector<MTBGraphEdge>(this.inEdges
				    .size()
				    + this.outEdges.size());
				for (int i = 0; i < inEdges.size(); i++) {
						allEdges.addElement(inEdges.elementAt(i));
				}
				for (int i = 0; i < outEdges.size(); i++) {
						allEdges.addElement(outEdges.elementAt(i));
				}
				return allEdges;
		}

		/**
		 * Get number of incoming edges for MTBGraphNode.
		 * 
		 * @return Number of incoming MTBGraphEdges.
		 */
		public int getNumberOfInEdges() {
				return this.inEdges.size();
		}

		/**
		 * Get number of outgoing edges for MTBGraphNode.
		 * 
		 * @return Number of outgoing MTBGraphEdges.
		 */
		public int getNumberOfOutEdges() {
				return this.outEdges.size();
		}

		/**
		 * Get total number of edges for MTBGraphNode.
		 * 
		 * @return Number of incoming and outgoing MTBGraphEdges of the MTBGraphNode.
		 */
		public int getTotalNumberOfEdges() {
				return (this.inEdges.size() + this.outEdges.size());
		}

		/**
		 * Get data of MTBGraphNode.
		 * 
		 * @return Data of MTBGraphNode from generic type.
		 */
		public T getData() {
				return this.data;
		}

		/**
		 * Get all neighbors of the current node. Neighbors are these nodes, which are
		 * connected with the current node via an input or output edge
		 * 
		 * @return List of neighbors for the current node.
		 */
		@SuppressWarnings("unchecked")
		public Vector<MTBGraphNode<T>> getNeighbors() {
				Vector<MTBGraphNode<T>> neighbors = new Vector<MTBGraphNode<T>>(
				    getTotalNumberOfEdges());
				// collect nodes, connected via an input edge
				for (int i = 0; i < this.inEdges.size(); i++) {
						MTBGraphEdge e = this.inEdges.elementAt(i);
						neighbors.addElement((MTBGraphNode<T>) e.getSrcNode());
				}
				// collect nodes, connected via an output edge
				for (int i = 0; i < this.outEdges.size(); i++) {
						MTBGraphEdge e = this.outEdges.elementAt(i);
						neighbors.addElement((MTBGraphNode<T>) e.getTgtNode());
				}
				return neighbors;
		}

		@Override
		public String toString() {
				String nodeToString = "node data: " + data.toString() + "; #in edges: "
				    + this.inEdges.size() + "; #out edges: " + this.outEdges.size();
				return nodeToString;
		}
}
