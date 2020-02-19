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
 * Class implements MTBGraphEdges for the MTBGraph. Each MTBGraphEdges has data
 * and a source / target MTBGraphNode.
 * 
 * 
 * @author misiak
 * 
 */
public class MTBGraphEdge {
		/**
		 * Source node of the MTBGraphEdge.
		 */
		protected MTBGraphNode<?> src;
		/**
		 * Target node of the MTBGraphEdge.
		 */
		protected MTBGraphNode<?> tgt;
		/**
		 * Cost of the MTBGraphEdge.
		 */
		protected double cost;
		/**
		 * Data of the MTBGraphEdge, included in a vector object.
		 */
		protected Vector<?> edgeData;

		/**
		 * Constructor to create a new MTBGraphEdge. The cost of the MTBGraphEdge will
		 * be set to the size of the data vector by default.
		 * 
		 * @param src
		 *          source node
		 * @param tgt
		 *          target node
		 * @param edgeData
		 *          data of the MTBGraphEdge
		 */
		public MTBGraphEdge(MTBGraphNode<?> src, MTBGraphNode<?> tgt,
		    Vector<?> edgeData) {
				this.src = src;
				this.tgt = tgt;
				this.edgeData = edgeData;
				this.cost = this.edgeData.size();
				this.src.addOutEdge(this);
				this.tgt.addInEdge(this);
		}

		/**
		 *Constructor to create a new MTBGraphEdge.
		 * 
		 * @param src
		 *          source node
		 * @param tgt
		 *          target node
		 * @param edgeData
		 *          data of the MTBGraphEdge
		 * @param cost
		 *          cost of the MTBGraphEdge
		 */
		public MTBGraphEdge(MTBGraphNode<?> src, MTBGraphNode<?> tgt,
		    Vector<?> edgeData, double cost) {
				this.src = src;
				this.tgt = tgt;
				this.edgeData = edgeData;
				this.cost = cost;
				this.src.addOutEdge(this);
				this.tgt.addInEdge(this);
		}

		/**
		 * Get source node of the MTBGraphEdge.
		 */
		public MTBGraphNode<?> getSrcNode() {
				return this.src;
		}

		/**
		 * Set source node of the MTBGraphEdge.
		 */
		protected void setSrcNode(MTBGraphNode<?> node) {
				this.src = node;
		}

		/**
		 * Get target node of the MTBGraphEdge.
		 */
		public MTBGraphNode<?> getTgtNode() {
				return this.tgt;
		}

		/**
		 * Set target node of the MTBGraphEdge.
		 */
		protected void setTgtNode(MTBGraphNode<?> node) {
				this.tgt = node;
		}

		/**
		 * Set data for current MTBGraphEdge with the given cost.
		 */
		public void setData(Vector<?> _edgeData, double _cost) {
				this.edgeData = _edgeData;
				this.cost = _cost;
		}

		/**
		 * Set data for current MTBGraphEdge. Cost is by default set to the number of
		 * elements of the edge data vector.
		 */
		public void setData(Vector<?> _edgeData) {
				this.edgeData = _edgeData;
				this.cost = _edgeData.size();
		}

		/**
		 * Get data for current MTBGraphEdge.
		 */
		public Vector<?> getData() {
				return this.edgeData;
		}

		/**
		 * Get edge cost as double value.
		 * 
		 * @return Edge cost.
		 */
		public double getCost() {
				return this.cost;
		}

		// TODO handle undirected edges ?
		public boolean nodeIsSrc(MTBGraphNode<?> node) {
				return (node.getData().equals(this.src.getData()));
		}

		public boolean nodeIsTgt(MTBGraphNode<?> node) {
				return (node.getData().equals(this.tgt.getData()));
		}

		public String toString() {
				// TODO add tgt and src node for output ?
				String edgeToString = "cost: " + this.cost + "; edge data: ";
				for (int i = 0; i < this.edgeData.size(); i++) {
						edgeToString += this.edgeData.elementAt(i).toString() + "; ";
				}
				return edgeToString;
		}
}
