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


/**
 * Class implements an (un-) directed MTBGraph. With each MTBGraphNode or
 * MTBGraphEdge specific data can be associated.
 * 
 * @see MTBGraphNode
 * @see MTBGraphEdge
 * 
 * 
 * @author Danny Misiak
 * 
 */
public class MTBGraph {
		/**
		 * Vector of included graph nodes.
		 */
		protected Vector<MTBGraphNode<?>> nodes;
		/**
		 * Vector of included graph edges.
		 */
		protected Vector<MTBGraphEdge> edges;
		/**
		 * True if MTBGraph is directed.
		 */
		protected boolean directed;
		/**
		 * Number of nodes in the MTBGraph.
		 */
		protected int numberOfNodes;
		/**
		 * Number of edges in the MTBGraph.
		 */
		protected int numberOfEdges;

		/**
		 * Standard constructor. Creates an empty undirected MTBGraph.
		 */
		public MTBGraph() {
				this.nodes = new Vector<MTBGraphNode<?>>(0);
				this.edges = new Vector<MTBGraphEdge>(0);
				this.directed = false;
				this.numberOfNodes = 0;
				this.numberOfEdges = 0;
		}

		/**
		 * Constructor to create an empty undirected or directed MTBGraph.
		 */
		public MTBGraph(boolean directed) {
				this.nodes = new Vector<MTBGraphNode<?>>(0);
				this.edges = new Vector<MTBGraphEdge>(0);
				this.directed = directed;
				this.numberOfNodes = 0;
				this.numberOfEdges = 0;
		}

		/**
		 * Constructor to create an undirected or directed MTBGraph with the given
		 * nodes and edges.
		 * 
		 * @param nodes
		 *          vector of MTBGraphNodes
		 * @param edges
		 *          vector of MTBGraphEdges
		 * @param directed
		 *          true if MTBGraph is directed
		 */
		public MTBGraph(Vector<MTBGraphNode<?>> nodes, Vector<MTBGraphEdge> edges,
		    boolean directed) {
				this.nodes = nodes;
				this.edges = edges;
				this.directed = directed;
				this.numberOfNodes = this.nodes.size();
				this.numberOfEdges = this.edges.size();
		}

		/**
		 * Directed or undirected graph?
		 * @return true if the graph is directed, false if undirected
		 */
		public boolean isDirected() {
			return this.directed;
		}
		
		/**
		 * Get all nodes of the MTBGraph.
		 * 
		 * @return Vector of MTBGraphNodes.
		 */
		public Vector<MTBGraphNode<?>> getNodes() {
				return this.nodes;
		}

		/**
		 * Get all edges of the MTBGraph.
		 * 
		 * @return Vector of MTBGraphEdges.
		 */
		public Vector<MTBGraphEdge> getEdges() {
				return this.edges;
		}

		/**
		 * Get number of included MTBGraphNodes.
		 * 
		 * @return Number of nodes.
		 */
		public int getNodeNum() {
				return this.numberOfNodes;
		}

		/**
		 * Get number of included MTBGraphEdges.
		 * 
		 * @return Number of edges.
		 */
		public int getEdgeNum() {
				return this.numberOfEdges;
		}

		/**
		 * Add a node to MTBGraph.
		 * 
		 * @param node
		 *          MTBGraphNode to add
		 */
		public void addNode(MTBGraphNode<?> node) {
				this.nodes.addElement(node);
				this.numberOfNodes++;
		}

		/**
		 * Add a edge to MTBGraph.
		 * 
		 * @param edge
		 *          MTBGraphEdge to add
		 */
		public void addEdge(MTBGraphEdge edge) {
				this.edges.addElement(edge);
				this.numberOfEdges++;
		}

		/**
		 * Get total amount of the graph costs. (Sum of all edge costs).
		 */
		public double getGraphCost() {
				double graphCosts = 0;
				for (int i = 0; i < this.edges.size(); i++) {
						MTBGraphEdge e = this.edges.elementAt(i);
						graphCosts += e.cost;
				}
				return graphCosts;
		}
		
		/**
		 * Remove the specified edge from the graph
		 */
		public void removeEdge(MTBGraphEdge edge) {
			if (this.edges.remove(edge))
				this.numberOfEdges--;
			
			edge.src.outEdges.remove(edge);
			edge.tgt.inEdges.remove(edge);
		}
		
		/**
		 * Remove the specified node from the graph as well as the edges connected to it.
		 */
		public void removeNode(MTBGraphNode<?> node) {
			if (this.nodes.remove(node))
				this.numberOfNodes--;
			
			Vector<MTBGraphEdge> edges = node.getAllEdges();
			
			for (MTBGraphEdge edge : edges) {
				this.removeEdge(edge);
			}
		}

		// public Vector<Point2D> shortPath(MTBGraphNode<?> n1,
		// MTBGraphNode<? extends Point2D> n2) {
		// // find the shortest path between the nodes n1 and n2
		// return null;
		// }
		//
		// public Vector<Vector<Point2D>> allPaths(MTBGraphNode<?> n1, MTBGraphNode<?>
		// n2) {
		// // find all paths between the nodes n1 and n2
		// return null;
		// }

		/**
		 * Print the whole MTBGraph with all its nodes and edges.
		 */
		public void print() {
				// print all nodes
				System.out.println(" nodes:");
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBGraphNode<?> n = this.nodes.elementAt(i);
						System.out.println("  " + n.getData().toString() + ", "
						    + n.getTotalNumberOfEdges());
				}
				System.out.println();
				// print all edges
				System.out.println(" edges:");
				for (int i = 0; i < this.edges.size(); i++) {
						MTBGraphEdge e = this.edges.elementAt(i);
						System.out.println("  " + e.getSrcNode().getData().toString() + ", "
						    + e.getTgtNode().getData().toString() + " | " + e.getCost());
				}
				System.out.println();
				// print graph: all nodes with edges
				System.out.println(" graph:");
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBGraphNode<?> n = this.nodes.elementAt(i);
						Vector<MTBGraphEdge> e = n.getAllEdges();
						System.out.print("  " + n.getData().toString() + " --> ");
						for (int j = 0; j < e.size(); j++) {
								System.out.print(e.elementAt(j).getSrcNode().getData().toString()
								    + ", " + e.elementAt(j).getTgtNode().getData().toString() + "; ");
						}
						System.out.println();
				}
				System.out.println();
		}
}
