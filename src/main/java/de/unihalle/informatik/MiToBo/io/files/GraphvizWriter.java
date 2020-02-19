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
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.io.files;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphEdge;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphNode;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.PartitGraphNodeID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.AdjacencyMatrix;


/**
 * Write graph to file using the DOT-language for visualization using graphviz tools.
 * A main graph has to be specified whether as <code>MTBGraph</code> or <code>AdjacencyMatrix</code>.
 * Further, subgraphs can be specified which then are painted in the specified color.
 * If T is of type <code>MatchingAdjacencyMatrix.NodeID</code>, the nodes of each partition are 
 * drawn with same rank, i.e. they are lined up horizontally.
 * 
 * @author Oliver Gress
 *
 * @param <T> Type of the nodes' data objects
 * 
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE,level=Level.STANDARD)
public class GraphvizWriter<T extends Comparable<?> & Comparator<?>> extends ALDOperator {

	@Parameter(label="adjmatrix", direction=Direction.IN, required=false, 
			description="Adjacency matrix of the input graph")
	protected AdjacencyMatrix<T> adjmatrix = null;
	
	@Parameter(label="graph", direction=Direction.IN, required=false, 
			description="Input graph")
	protected MTBGraph graph = null;
	
	@Parameter(label="filename", direction=Direction.IN, required=true, 
			description="Output file name")
	protected String filename = null;
	
	@Parameter(label="weightAsEdgeThickness", direction=Direction.IN, required=true, 
			description="Visualize weights by edge thickness")
	protected boolean weightAsEdgeThickness = false;
	
	@Parameter(label="dotGlobalAttributes", direction=Direction.IN, required=false, 
			description="Global attributes for graph drawing (dot commands), e.g. node/edge style, color etc.")
	protected String[] dotGlobalAttributes = null;
	
	@Parameter(label="maxWeight", direction=Direction.IN, required = false,
			description="Maximum weight/cost in the graph, needed to limit edge thickness to 10pts if weights are visualized by edge thickness.")
	protected double maxWeight = -1.0;
	
	// storage for subgraphs of type MTBGraph ...
	protected Vector<MTBGraph> subgraphs;
	// ... and the color they should be painted in
	protected Vector<Color> subgraphs_color;
	
	// storage for subgraphs of type AdjacencyMatrix ...
	protected Vector<AdjacencyMatrix<T>> subgraphs_adj;
	// ... and the color they should be painted in
	protected Vector<Color> subgraphs_adj_color;
	
	// flag if edges with zero weight are to be painted
	private boolean paintZeroWeightEdges = false;
	
	// short names of the nodes used in the dot file (n0, n1, ...)
	private HashMap<T, String> nodeNames;
	
	// storage of edges of the subgraphs (or rather source (key) and their corresponding target nodes (set of nodes))
	private HashMap<T, TreeSet<T>> subgraphEdges;
	
	// flag if the main graph is directed
	private boolean directed;
	
	/**
	 * Empty constructor
	 * @throws ALDOperatorException
	 */
	public GraphvizWriter() throws ALDOperatorException {
		
	}
	
	/**
	 * Constructor for specifying the graph by an adjacency matrix
	 */
	public GraphvizWriter(AdjacencyMatrix<T> adjmatrix, String filename) throws ALDOperatorException {
		this.adjmatrix = adjmatrix;
		this.filename = filename;
		this.nodeNames = new HashMap<T, String>();
		this.subgraphEdges = new HashMap<T, TreeSet<T>>();
		
		this.directed = this.adjmatrix.isDirected();
	}
	
	/**
	 * Constructor for specifying the graph by <code>MTBGraph</code>
	 */
	public GraphvizWriter(MTBGraph graph, String filename) throws ALDOperatorException {
		this.graph = graph;
		this.filename = filename;
		this.nodeNames = new HashMap<T, String>();
		this.subgraphEdges = new HashMap<T, TreeSet<T>>();
		
		this.directed = this.graph.isDirected();
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (!(this.adjmatrix != null ^ this.graph != null)) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
					"GraphvizWriter.validateCustom(): Whether adjacency matrix or graph must be specified exclusively.");
		}
	}
	
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {
		
		// find maximum weight in graph (subgraphs not considered)
		if (this.maxWeight <= 0) {
			double w;
			
			if (this.adjmatrix != null) {
				T[]	nodes = this.adjmatrix.getNodes();
				
				if (this.directed) {
					for (int i = 0; i < nodes.length; i++) {
						for (int j = 0; j < nodes.length; j++) {
							w = this.adjmatrix.getWeight(nodes[i], nodes[j]);
							if (w > this.maxWeight)
								this.maxWeight = w;
						}
					}
				}
				else {
					for (int i = 0; i < nodes.length; i++) {
						for (int j = i; j < nodes.length; j++) {
							w = this.adjmatrix.getWeight(nodes[i], nodes[j]);
							if (w > this.maxWeight)
								this.maxWeight = w;
						}
					}
				}
			}
			else {
				Vector<MTBGraphEdge> edges = this.graph.getEdges();
				
				for (int i = 0; i < edges.size(); i++) {
					w = edges.get(i).getCost();
					if (w > this.maxWeight) {
						this.maxWeight = w;
					}
				}
			}
		}
		
		this.nodeNames.clear();
		this.initNodeNames();
		
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(this.filename);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
					"GraphvizWriter.operate(): Cannot instantiate FileWriter: " + e1.getMessage());
		}
		BufferedWriter bw = new BufferedWriter(fw);
		
		try {
			if (this.directed)	
				bw.write("digraph G {\n");
			else
				bw.write("graph G {\n");

			this.writeGlobalAttributes(bw);
			bw.write("\n");
			this.writeSubgraphs(bw);
			bw.write("\n");
			this.writePartitions(bw);
			bw.write("\n");
			this.writeNodeLabels(bw);
			bw.write("\n");
			this.writeGraph(bw);
			
			bw.write("\n}");
			
			bw.flush();
			bw.close();
			
		} catch (IOException e1) {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
					"GraphvizWriter.operate(): Failed to write DOT-file: " + e1.getMessage());
		}
		
		
	}

	/**
	 * Set the main graph's adjacency matrix
	 */
	public void setAdjacencyMatrix(AdjacencyMatrix<T> adjmatrix) {
		this.adjmatrix = adjmatrix;
		this.graph = null;
		this.directed = this.adjmatrix.isDirected();
	}
	
	/**
	 * Set the main graph using <code>MTBGraph</code>
	 */
	public void setGraph(MTBGraph graph) {
		this.graph = graph;
		this.adjmatrix = null;
		this.directed = this.graph.isDirected();
	}
	
	/**
	 * Set the filename for output
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Set flag how edge weights are visualized. If flag is true weights are
	 * visualized by edge thickness, otherwise edges are labeled with the value of their weigth.
	 */
	public void setWeightAsEdgeThickness(boolean weightAsEdgeThickness) {
		this.weightAsEdgeThickness = weightAsEdgeThickness;
	}
	
	/**
	 * Set global attributes of the graph e.g. the style of the nodes etc.
	 * The given strings must match DOT-language commands, e.g 'node [style = "bold"];'. 
	 */
	public void setDotGlobalAttributes(String[] dotGlobalAttributes) {
		this.dotGlobalAttributes = dotGlobalAttributes;
		
		if (this.dotGlobalAttributes != null) {
			for (int i = 0; i < this.dotGlobalAttributes.length; i++) {
				this.dotGlobalAttributes[i] = this.dotGlobalAttributes[i].trim();
			}
		}
	}
	
	/**
	 * Specify the maximum weight in the graph. This value is used to limit edge-thickness to 10 pts
	 * if weights are visualized by edge thickness (edgethickness = weight/maxWeight*10). Default is 1 !!
	 */
	public void setMaxWeight(double maxWeight) {
		this.maxWeight = maxWeight;
	}
	
	/**
	 * Specify a subgraph that is to be painted in the given color.
	 */
	public void addSubgraph(MTBGraph subgraph, Color c) {
		if (this.subgraphs == null) {
			this.subgraphs = new Vector<MTBGraph>();
			this.subgraphs_color = new Vector<Color>();
		}
		
		this.subgraphs.add(subgraph);
		this.subgraphs_color.add(c);
		
	}
	
	/**
	 * Specify a subgraph that is to be painted in the given color.
	 */
	public void addSubgraphAdjaceny(AdjacencyMatrix<T> subgraph_adj, Color c) {
		if (this.subgraphs_adj == null) {
			this.subgraphs_adj = new Vector<AdjacencyMatrix<T>>();
			this.subgraphs_adj_color = new Vector<Color>();
		}
		
		this.subgraphs_adj.add(subgraph_adj);
		this.subgraphs_adj_color.add(c);
		
	}
	
	@SuppressWarnings("unchecked")
	private void initNodeNames() {
		// store nodes and their dot-ids in hashmap
		
		int nodeCnt = 0;
		
		if (this.adjmatrix != null) {
			T[] nodes = this.adjmatrix.getNodes();
			for (int n = 0; n < nodes.length; n++) {
				this.nodeNames.put(nodes[n], "n"+nodeCnt);
				nodeCnt++;
			}
		}
		else {
			Vector<MTBGraphNode<?>> gnodes = this.graph.getNodes();
			for (int n = 0; n < gnodes.size(); n++) {	
				this.nodeNames.put((T) gnodes.get(n).getData(), "n"+nodeCnt);
				nodeCnt++;
			}
		}
		
		if (this.subgraphs_adj != null) {

			for (int i = 0; i < this.subgraphs_adj.size(); i++) {
				T[] nodes = this.subgraphs_adj.get(i).getNodes();
				
				for (int n = 0; n < nodes.length; n++) {	
					if (! this.nodeNames.containsKey(nodes[n])) {
						this.nodeNames.put(nodes[n], "n"+nodeCnt);
						nodeCnt++;
					}
				}
			}
		}
		
		if (this.subgraphs != null) {

			for (int i = 0; i < this.subgraphs.size(); i++) {
				Vector<MTBGraphNode<?>> gnodes = this.subgraphs.get(i).getNodes();
				
				for (int n = 0; n < gnodes.size(); n++) {	
					if (! this.nodeNames.containsKey(gnodes.get(n).getData())) {
						this.nodeNames.put((T) gnodes.get(n).getData(), "n"+nodeCnt);
						nodeCnt++;
					}
				}
			}
		}
	}

	private void writeGlobalAttributes(BufferedWriter w) throws IOException {
		
		if (this.dotGlobalAttributes != null) {
			for (int i = 0; i < this.dotGlobalAttributes.length; i++) {
				w.write("\t" + this.dotGlobalAttributes[i] + (this.dotGlobalAttributes[i].endsWith(";") ? "" : ";") + "\n");
			}
		}
	}

	private void writeNodeLabels(BufferedWriter w) throws IOException {

		Set<T> nodes = new TreeSet<T>(this.nodeNames.keySet());
		
		Iterator<T> nodeIter = nodes.iterator();
		T node = null;
		String label = null;
		
		while (nodeIter.hasNext()) {
			
			node = nodeIter.next();
			
			label = node.toString();
			String[] lines = label.split("\n");
			String s = "\t" + this.nodeNames.get(node) + " [label=\"";
			for (int i = 0; i < lines.length; i++) {
				if (i == 0)
					s += lines[i];
				else
					s += "\\n" + lines[i];
			}
			s += "\"];\n";
			w.write(s);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void writeSubgraphs(BufferedWriter w) throws IOException {

		this.subgraphEdges.clear();
		
		T nodeSrc = null;
		T nodeTgt = null;
		String edgeSymbol = null;
		
		int col = 1;
		
		if (this.subgraphs != null) {
			Vector<MTBGraphNode<?>> nodes = null;
			
			for (int i = 0; i < this.subgraphs.size(); i++) {
				nodes = this.subgraphs.get(i).getNodes();
				
				edgeSymbol = (this.subgraphs.get(i).isDirected() ? " -> " : " -- ");
				
				if (nodes.size() > 0) {
					String hexchannel;
					String hexcolor = "";
					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getRed());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;

					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getGreen());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;

					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getBlue());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;
					
					w.write("\tsubgraph sg" + i + " {\n");
					w.write("\t\tnode [color=\"#" + hexcolor + "\"];\n");
					w.write("\t\tedge [color=\"#" + hexcolor + "\"];\n\t\t");
					
					for (int n = 0; n < nodes.size(); n++) {
						w.write(this.nodeNames.get(nodes.get(n).getData()) + "; ");
					}
					w.write("\n");
					
					Vector<MTBGraphEdge> edges = this.subgraphs.get(i).getEdges();
					MTBGraphEdge edge = null;
					
					for (int e = 0; e < edges.size(); e++) {
						edge = edges.get(e);
						
						nodeSrc = (T) edge.getSrcNode().getData();
						nodeTgt = (T) edge.getTgtNode().getData();
						
						if (this.subgraphEdges.containsKey(nodeSrc)) {
							this.subgraphEdges.get(nodeSrc).add(nodeTgt);
						}
						else {
							this.subgraphEdges.put(nodeSrc, new TreeSet<T>());
							this.subgraphEdges.get(nodeSrc).add(nodeTgt);
						}
						
						if (this.paintZeroWeightEdges || edge.getCost() != 0.0) {
							if (this.weightAsEdgeThickness) {

								w.write("\t\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
										+ " [penwidth=" + new BigDecimal(Double.toString(edge.getCost()/this.maxWeight*10)).toPlainString() + "];\n");			
							}
							else
								w.write("\t\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
										+ " [label=\"" + edge.getCost()/this.maxWeight*10 + "\"];\n");
						}			
					}
					
					
					w.write("\n\t}\n");
				}
				
				col++;
			}
		}
		
		if (this.subgraphs_adj != null) {
			T[] nodes = null;
			
			for (int i = 0; i < this.subgraphs_adj.size(); i++) {
				nodes = this.subgraphs_adj.get(i).getNodes();
				
				edgeSymbol = (this.subgraphs_adj.get(i).isDirected() ? " -> " : " -- ");
				
				if (nodes != null && nodes.length > 0) {
				
					String hexchannel;
					String hexcolor = "";
					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getRed());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;

					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getGreen());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;

					hexchannel = Integer.toHexString(this.subgraphs_color.get(i).getBlue());
					hexcolor += (hexchannel.length() == 1) ? ("0" + hexchannel) : hexchannel;
					
					w.write("\tsubgraph sg" + i + " {\n");
					w.write("\t\tnode [color=\"#" + hexcolor + "\"];\n");
					w.write("\t\tedge [color=\"#" + hexcolor + "\"];\n\t\t");
					
					for (int n = 0; n < nodes.length; n++) {
						w.write(this.nodeNames.get(nodes[n]) + "; ");
					}
					w.write("\n");
					
					for (int n1 = 0; n1 < nodes.length; n1++) {
						nodeSrc = nodes[n1];
						
						for (int n2 = (this.directed ? 0 : n1); n2 < nodes.length; n2++) {
							nodeTgt = nodes[n2];
							
							if (this.subgraphs_adj.get(i).getWeight(nodeSrc, nodeTgt) != 0) {
								if (this.subgraphEdges.containsKey(nodeSrc)) {
									this.subgraphEdges.get(nodeSrc).add(nodeTgt);
								}
								else {
									this.subgraphEdges.put(nodeSrc, new TreeSet<T>());
									this.subgraphEdges.get(nodeSrc).add(nodeTgt);
								}
							}
							
							if (this.paintZeroWeightEdges || this.subgraphs_adj.get(i).getWeight(nodeSrc, nodeTgt) > 0) {
								if (this.weightAsEdgeThickness) {

									w.write("\t\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
											+ " [penwidth=" + new BigDecimal(Double.toString(this.subgraphs_adj.get(i).getWeight(nodeSrc, nodeTgt)/this.maxWeight*10)).toPlainString() + "];\n");
								}
								else
									w.write("\t\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
											+ " [label=\"" + this.subgraphs_adj.get(i).getWeight(nodeSrc, nodeTgt) + "\"];\n");
							}
			
						}	
					}
					
					w.write("\n\t}\n");
				}
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private void writePartitions(BufferedWriter w) throws IOException {
		
		if (this.adjmatrix != null) {
				
			T[] nodes = this.adjmatrix.getNodes();
			
			HashMap<Integer, Vector<T>> partitions = new HashMap<Integer, Vector<T>>();
			
			if (nodes[0] instanceof PartitGraphNodeID) {
				
				int pID;
				
				for (int n = 0; n < nodes.length; n++) {
					pID = ((PartitGraphNodeID)nodes[n]).partitionID;
					
					if (partitions.containsKey(pID)) {
						partitions.get(pID).add(nodes[n]);
					}
					else {
						partitions.put(pID, new Vector<T>());
						partitions.get(pID).add(nodes[n]);
					}
				}
				
				TreeSet<Integer> keys = new TreeSet<Integer>(partitions.keySet());
				
				Iterator<Integer> pIter = keys.iterator();
				
				while (pIter.hasNext()) {
					pID = pIter.next();
					Vector<T> pnodes = partitions.get(pID);
					
					if (pnodes.size() > 0) {
					
						w.write("\tsubgraph partition" + pID + " {\n");
						w.write("\t\trank = same; ");
						for (int n = 0; n < pnodes.size(); n++) {
							w.write(this.nodeNames.get(pnodes.get(n)) + "; ");
						}
						w.write("\n\t}\n");
					}
				}
			}
		}
		else {
			Vector<MTBGraphNode<?>> nodes = this.graph.getNodes();
			
			HashMap<Integer, Vector<T>> partitions = new HashMap<Integer, Vector<T>>();
			
			if (nodes.get(0).getData() instanceof PartitGraphNodeID) {
				
				int pID;
				
				for (int n = 0; n < nodes.size(); n++) {
					pID = ((PartitGraphNodeID)nodes.get(n).getData()).partitionID;
					
					if (partitions.containsKey(pID)) {
						partitions.get(pID).add((T) nodes.get(n).getData());
					}
					else {
						partitions.put(pID, new Vector<T>());
						partitions.get(pID).add((T) nodes.get(n).getData());
					}
				}
				
				TreeSet<Integer> keys = new TreeSet<Integer>(partitions.keySet());
				
				Iterator<Integer> pIter = keys.iterator();
				
				while (pIter.hasNext()) {
					pID = pIter.next();
					Vector<T> pnodes = partitions.get(pID);
					
					if (pnodes.size() > 0) {
					
						w.write("\tsubgraph partition" + pID + " {\n");
						w.write("\t\trank = same; ");
						for (int n = 0; n < pnodes.size(); n++) {
							w.write(this.nodeNames.get(pnodes.get(n)) + "; ");
						}
						w.write("\n\t}\n");
					}
				}
			}
			
		}
		
	}

	
	@SuppressWarnings("unchecked")
	private void writeGraph(BufferedWriter w) throws IOException {

		T nodeSrc = null;
		T nodeTgt = null;

		String edgeSymbol = (this.directed ? " -> " : " -- ");
		
		if (this.adjmatrix != null) {
			
			T[] nodes = this.adjmatrix.getNodes();
			
			for (int n1 = 0; n1 < nodes.length; n1++) {
				nodeSrc = nodes[n1];
				
				for (int n2 = (this.directed ? 0 : n1); n2 < nodes.length; n2++) {
					nodeTgt = nodes[n2];
					
					if (!(this.subgraphEdges.containsKey(nodeSrc) && this.subgraphEdges.get(nodeSrc).contains(nodeTgt))) {
					
						if (this.directed || !(this.subgraphEdges.containsKey(nodeTgt) && this.subgraphEdges.get(nodeTgt).contains(nodeSrc))) {
						
							if (this.paintZeroWeightEdges || this.adjmatrix.getWeight(nodeSrc, nodeTgt) > 0) {
								if (this.weightAsEdgeThickness)
									w.write("\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
											+ " [penwidth=" + new BigDecimal(Double.toString(this.adjmatrix.getWeight(nodeSrc, nodeTgt)/this.maxWeight*10)).toPlainString() + "];\n");
								else
									w.write("\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
											+ " [label=\"" + this.adjmatrix.getWeight(nodeSrc, nodeTgt) + "\"];\n");
							}
						}
					}
	
				}	
			}
		}
		else {
			Vector<MTBGraphEdge> edges = this.graph.getEdges();
			MTBGraphEdge edge = null;
			
			
			for (int e = 0; e < edges.size(); e++) {
				edge = edges.get(e);
				
				nodeSrc = (T) edge.getSrcNode().getData();
				nodeTgt = (T) edge.getTgtNode().getData();
				
				if (!(this.subgraphEdges.containsKey(nodeSrc) && this.subgraphEdges.get(nodeSrc).contains(nodeTgt))) {
					
					if (this.directed || !(this.subgraphEdges.containsKey(nodeTgt) && this.subgraphEdges.get(nodeTgt).contains(nodeSrc))) {
		
						if (this.paintZeroWeightEdges || edge.getCost() != 0.0) {
							if (this.weightAsEdgeThickness)
								w.write("\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
										+ " [penwidth=" + edge.getCost()/this.maxWeight*10 + "];\n");
							else
								w.write("\t" + this.nodeNames.get(nodeSrc) + edgeSymbol + this.nodeNames.get(nodeTgt)
										+ " [label=\"" + edge.getCost()/this.maxWeight*10 + "\"];\n");
						}	
					}
				}
			}
		}
	}
	
}
