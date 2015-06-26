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

package de.unihalle.informatik.MiToBo.tracking.multitarget.algo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphEdge;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphNode;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.MatchingAdjacencyMatrix;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.PartitGraphNodeID;

/**
 * greedyGourmet graph partitioning algorithm following: <br>
 * J. Kutzera, "Gruppierung von LC/MS-Pseudospektren aus multiplen Messungen", 
 * Martin Luther University Halle-Wittenberg, 2009, Diploma Thesis
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class GreedyGourmetPartitioning {
	
	protected MatchingAdjacencyMatrix adjMatrix;
	protected boolean maxWeights;
	private Vector<MTBGraph> subgraphs;
	HashMap<Integer, Vector<MTBGraphNode<PartitGraphNodeID>>> partitions;
	double limit;
	
	/**
	 * Constructor.
	 * @param adjMatrix adjacency matrix of the multipartite graph
	 * @param maximizeWeights flag if sum of weights are to be maximized
	 * @param limit only edge weights larger (maximize weights) or smaller (minimize weights) are considered
	 */
	public GreedyGourmetPartitioning(MatchingAdjacencyMatrix adjMatrix, boolean maximizeWeights, double limit) {
		this.adjMatrix = adjMatrix;
		this.maxWeights = maximizeWeights;
		this.limit = limit;
	}

	/**
	 * Compute subgraphs with greedyGourmet algorithm.
	 * @return 
	 */
	public Vector<MTBGraph> computeSubgraphs() {
		
		// get available nodes
		PartitGraphNodeID[] nodes = this.adjMatrix.getNodes();
		
		// sets of nodes: subgraphs cannot contain more than one node of a set
		this.partitions = new HashMap<Integer, Vector<MTBGraphNode<PartitGraphNodeID>>>();
		
		// setIDs
		TreeSet<Integer> partitionIDs = null;
		
		// vector of subgraphs
		this.subgraphs = new Vector<MTBGraph>(nodes.length);
		
		// refs for temporary use
		MTBGraph subgraph = null;
		Vector<MTBGraphNode<PartitGraphNodeID>> partition = null;
		
		// maximum partitionID
		int maxPartitionID = -1;
		
		// determine partitions and initialize subgraphs:
		// at the beginning, each node corresponds to a subgraph
		MTBGraphNode<PartitGraphNodeID> gnode = null;
		for (int n = 0; n < nodes.length; n++) {
			
			nodes[n].subgraphID = n;
			
			// create a subgraph for each node
			subgraph = new MTBGraph();
			gnode = new MTBGraphNode<PartitGraphNodeID>(nodes[n]);
			subgraph.addNode(gnode);
			this.subgraphs.add(subgraph);
			
			// determine maximum partitionID
			if (nodes[n].partitionID > maxPartitionID)
				maxPartitionID = nodes[n].partitionID;
			
			// determine and store partitions
			if (this.partitions.containsKey(nodes[n].partitionID)) {
				this.partitions.get(nodes[n].partitionID).add(gnode);
			}
			else {
				partition = new Vector<MTBGraphNode<PartitGraphNodeID>>();
				partition.add(gnode);
				this.partitions.put(nodes[n].partitionID, partition);
			}
		}
		
		// partitionIDs in ascending order 
		partitionIDs = new TreeSet<Integer>(partitions.keySet());

		// refs and variables for temp use
		MTBGraphNode<PartitGraphNodeID> n1 = null, n2 = null;
		int partitionID;
		
		// iterator over partitionIDs
		Iterator<Integer> partitionIDiter = partitionIDs.iterator();
	
		// iterate over all partitions (partitionIDs)
		while (partitionIDiter.hasNext()) {
			
			// current partitionID
			partitionID = partitionIDiter.next();
		
			// nodes of current partition
			partition = this.partitions.get(partitionID);
			
			// partitionIDs higher than current partitionID
			SortedSet<Integer> higherPartitionIDs = partitionIDs.tailSet(partitionID, false);

			// iterator for partitionIDs higher than the current partitionID
			Iterator<Integer> hpartitionIDiter = higherPartitionIDs.iterator();
			while (hpartitionIDiter.hasNext()) {
				// partitionID
				int p = hpartitionIDiter.next();	
					
			// iterate over all nodes of the current partition
			for (int nIdx = 0; nIdx < partition.size(); nIdx++) {

				// iterator for partitionIDs higher than the current partitionID
			//	Iterator<Integer> hpartitionIDiter = higherPartitionIDs.iterator();

				boolean found = false;
			//	int hpartsToTest = higherPartitionIDs.size();
				// iterate over partitions with higher partitionIDs than current partition
			//	while (hpartitionIDiter.hasNext() && /*hpartsToTest > 0*/!found) {
				//for (int p = nodes[nIdx].partitionID + 1; p < this.partitions.size(); p++) {
					
					// partitionID
				//	int p = hpartitionIDiter.next();

					n1 = this.getOptimalNeighbor(partition.get(nIdx), p);
					
					if (n1 != null) {
						n2 = this.getOptimalNeighbor(n1, partition.get(nIdx).getData().partitionID);
						
						if (n2 != null && (n2.getData().compareTo(partition.get(nIdx).getData()) == 0)) {
							
							MTBGraphNode<PartitGraphNodeID> nstar1 = this.getGraphNode(n1.getData().subgraphID, n2.getData().partitionID);
							MTBGraphNode<PartitGraphNodeID> nstar2 = this.getGraphNode(n2.getData().subgraphID, n1.getData().partitionID);
							
							

							if ((nstar1 != null) ^ (nstar2 != null)) {
							// case 1
								
								found = this.connectNodesCase1(n1, n2, nstar1, nstar2, true);
						//		if (found)
						//			hpartsToTest = 2;
								
							}
							else if ((nstar1 != null) && (nstar2 != null)) {
							// case 2	
								
								boolean ja_n1 = this.connectNodesCase1(n1, n2, nstar1, null, false);
								boolean ja_n2 = this.connectNodesCase1(n1, n2, null, nstar2, false);
								
								double m0 = this.subgraphs.get(n1.getData().subgraphID).getGraphCost() 
											+ this.subgraphs.get(n2.getData().subgraphID).getGraphCost();
								
						//		m0 /= this.subgraphs.get(n1.getData().subgraphID).getEdgeNum()
						//				+ this.subgraphs.get(n2.getData().subgraphID).getEdgeNum();
								
								Vector<MTBGraphNode<PartitGraphNodeID>> nhash = n1.getNeighbors();
								Vector<MTBGraphNode<PartitGraphNodeID>> ndollar = n2.getNeighbors();
								
								
								double m1 = 0.0;
								int cnt = 0;
								for (int x = 0; x < nhash.size(); x++) {
									if (this.adjMatrix.getWeight(n2.getData(), nhash.get(x).getData()) > this.limit) {
										m1 += this.adjMatrix.getWeight(n2.getData(), nhash.get(x).getData());
										cnt++;
									}
									
									for (int z = x+1; z < nhash.size(); z++) {
										if (this.adjMatrix.getWeight(nhash.get(z).getData(), nhash.get(x).getData()) > this.limit) {
											m1 += this.adjMatrix.getWeight(nhash.get(z).getData(), nhash.get(x).getData());
											cnt++;
										}
									}
								}
						//		m1 /= cnt;
								
								
								double m2 = 0.0;
								cnt = 0;
								for (int x = 0; x < ndollar.size(); x++) {
									if (this.adjMatrix.getWeight(n1.getData(), ndollar.get(x).getData()) > this.limit) {
										m2 += this.adjMatrix.getWeight(n1.getData(), ndollar.get(x).getData());
										cnt++;
									}
									
									for (int z = x+1; z < ndollar.size(); z++) {
										if (this.adjMatrix.getWeight(ndollar.get(z).getData(), ndollar.get(x).getData()) > this.limit) {
											m2 += this.adjMatrix.getWeight(ndollar.get(z).getData(), ndollar.get(x).getData());
											cnt++;
										}
									}
								}
							//	m2 /= cnt;
								
								if (ja_n1 && (m1 > m2) && (m1 > m0)) {
									
									subgraph = this.subgraphs.get(n2.getData().subgraphID);
									subgraph.removeNode(n2);
									
									n2.getData().subgraphID = n1.getData().subgraphID;
									
									subgraph = this.subgraphs.get(n1.getData().subgraphID);
									subgraph.addNode(n2);
									
									for (int x = 0; x < nhash.size(); x++) {
										if (this.adjMatrix.getWeight(n2.getData(), nhash.get(x).getData()) > this.limit) {
											subgraph.addEdge(new MTBGraphEdge(n2, nhash.get(x), null, this.adjMatrix.getWeight(n2.getData(), nhash.get(x).getData())));
										}
									}
									found = true;
							//		hpartsToTest = 2;
								}
								else if (ja_n2 && (m2 > m1) && (m2 > m0)) {
									
									subgraph = this.subgraphs.get(n1.getData().subgraphID);
									subgraph.removeNode(n1);
									
									n1.getData().subgraphID = n2.getData().subgraphID;
									
									subgraph = this.subgraphs.get(n2.getData().subgraphID);
									subgraph.addNode(n1);
									
									for (int x = 0; x < ndollar.size(); x++) {
										if (this.adjMatrix.getWeight(n1.getData(), ndollar.get(x).getData()) > this.limit) {
											subgraph.addEdge(new MTBGraphEdge(n1, ndollar.get(x), null, this.adjMatrix.getWeight(n1.getData(), ndollar.get(x).getData())));
										}
									}	
									found = true;
							//		hpartsToTest = 2;
								}
								
								
								
							}
							else {
								// case 0
								subgraph = this.subgraphs.get(partition.get(nIdx).getData().subgraphID);
								MTBGraph subgraph1 = this.subgraphs.get(n1.getData().subgraphID);
								subgraph1.removeNode(n1);
								
								subgraph.addNode(n1);
								n1.getData().subgraphID = partition.get(nIdx).getData().subgraphID;
								
								Vector<MTBGraphNode<?>> snodes = subgraph.getNodes();

								for (MTBGraphNode<?> nh : snodes) {
									if (this.adjMatrix.getWeight((PartitGraphNodeID) nh.getData(), n1.getData()) > this.limit)
										subgraph.addEdge(new MTBGraphEdge(nh, n1, null, this.adjMatrix.getWeight((PartitGraphNodeID) nh.getData(), n1.getData())));
								}
								found = true;
						//		hpartsToTest = 2;
							}
							
							
						}
						
						
					}
				
				//	hpartsToTest--;
					
				}
			}
			
			
			
			
		}
		
		return subgraphs;
	}
	

	/**
	 * Connect nodes if case 1
	 */
	private boolean connectNodesCase1(MTBGraphNode<PartitGraphNodeID> n1, MTBGraphNode<PartitGraphNodeID> n2, MTBGraphNode<PartitGraphNodeID> nstar1, MTBGraphNode<PartitGraphNodeID> nstar2, boolean do_connect) {
		MTBGraphNode<PartitGraphNodeID> ne = null, ne_tick = null;
		MTBGraphNode<PartitGraphNodeID> nstar = null;
		MTBGraph subgraph = null;
		
		if (nstar1 != null) {
			ne = n1;
			ne_tick = n2;
			nstar = nstar1;
		}
		else {
			ne = n2;
			ne_tick = n1;
			nstar = nstar2;
		}
		
		// edges connected to node nstar in subgraph containing nstar
		Vector<MTBGraphEdge> ehash = nstar.getAllEdges();
		
		// nodes connected to nstar in the corresponding subgraph
		Vector<MTBGraphNode<PartitGraphNodeID>> nhash = nstar.getNeighbors();
		
		// compute mean of weights of marked edges to nstar
		double mwhash = 0.0;
		int cnt = 0;
		for (int x = 0; x < ehash.size(); x++) {
			if (ehash.get(x).getCost() > this.limit) {
				mwhash += ehash.get(x).getCost();
				cnt++;
			}	
		}
	//	mwhash /= cnt;
		

		// compute mean of weights of unmarked edges of neighbors of nstar to ne_tick
		double mwdollar = 0.0;
		cnt = 0;
		for (int x = 0; x < nhash.size(); x++) {
			if (this.adjMatrix.getWeight((PartitGraphNodeID) nhash.get(x).getData(), ne_tick.getData()) > this.limit) {
				mwdollar += this.adjMatrix.getWeight((PartitGraphNodeID) ehash.get(x).getTgtNode().getData(), ne_tick.getData());
				cnt++;
			}	
		}
	//	mwdollar /= cnt;
		
		if (mwdollar > mwhash) {
			
			if (do_connect) {
				subgraph = this.subgraphs.get(ne.getData().subgraphID);
				MTBGraph subgraph_tick = this.subgraphs.get(ne_tick.getData().subgraphID);
				MTBGraphNode<?> gne_tick = this.getGraphNode(ne_tick.getData().subgraphID, ne_tick.getData().partitionID);
				
				subgraph.removeNode(nstar);
				subgraph_tick.removeNode(gne_tick);
				
				ne_tick.getData().subgraphID = ne.getData().subgraphID;
				subgraph.addNode(gne_tick);
				
				for (MTBGraphNode<PartitGraphNodeID> nh : nhash) {
					if (this.adjMatrix.getWeight(nh.getData(), ne_tick.getData()) > this.limit)
					subgraph.addEdge(new MTBGraphEdge(nh, gne_tick, null, this.adjMatrix.getWeight(nh.getData(), ne_tick.getData())));
				}
			}
			
			return true;
		}
		else 
			return false;
		
		
		
	}
	
	
	/**
	 * Get node of partition 'partitionID' which is connected to current node and has optimal weight
	 * @param currentNode current node
	 * @param partitionID ID of partition
	 * @param limit optimal neighbors must have edge weights larger (max weight) or smaller (min weight) than limit to be considered
	 * @return optimal neighbor node
	 */
	private MTBGraphNode<PartitGraphNodeID> getOptimalNeighbor(MTBGraphNode<PartitGraphNodeID> currentNode, int partitionID) {

		Vector<MTBGraphNode<PartitGraphNodeID>> pnodes = this.partitions.get(partitionID);
		
		double optWeight = this.limit;
		MTBGraphNode<PartitGraphNodeID> optNode = null;
		if (this.maxWeights) {
		
			for (int n = 0; n < pnodes.size(); n++) {
				
				if (this.adjMatrix.getWeight(currentNode.getData(), pnodes.get(n).getData()) > optWeight) {
					optWeight = this.adjMatrix.getWeight(currentNode.getData(), pnodes.get(n).getData());
					optNode = pnodes.get(n);
				}
			}	
		}
		else {
			
			for (int n = 0; n < pnodes.size(); n++) {
				
				if (this.adjMatrix.getWeight(currentNode.getData(), pnodes.get(n).getData()) < optWeight) {
					optWeight = this.adjMatrix.getWeight(currentNode.getData(), pnodes.get(n).getData());
					optNode = pnodes.get(n);
				}
			}
		}
		
		return optNode;
	}
	
	/**
	 * Get the graph node from partition 'partitionID' in subgraph 'subgraphID'. If none is available in the subgraph, null is returned.
	 */
	@SuppressWarnings("unchecked")
	private MTBGraphNode<PartitGraphNodeID> getGraphNode(int subgraphID, int partitionID) {

		MTBGraph subgraph = this.subgraphs.get(subgraphID);
		Vector<MTBGraphNode<?>> snodes = subgraph.getNodes();

		for (int n = 0; n < snodes.size(); n++) {
			
			if (((PartitGraphNodeID)snodes.get(n).getData()).partitionID == partitionID) {//this.adjMatrix.getWeight(currentNode, (NodeID) gnodes.get(n).getData()) > 0) {
				return (MTBGraphNode<PartitGraphNodeID>) snodes.get(n);
			}
		}	
		return null;
	}
	
	/**
	 * Get number of nodes from partition 'partitionID' connected to current node
	 */
	private int numOfConnectedNodes(PartitGraphNodeID currentNode, int partitionID) {

		MTBGraph subgraph = this.subgraphs.get(currentNode.subgraphID);
		Vector<MTBGraphNode<?>> snodes = subgraph.getNodes();
		
		int num = 0;
		for (int n = 0; n < snodes.size(); n++) {
			
			if (((PartitGraphNodeID)snodes.get(n).getData()).partitionID == partitionID) {//this.adjMatrix.getWeight(currentNode, (NodeID) gnodes.get(n).getData()) > 0) {
				num++;
			}
		}	
		return num;
	}
	
	

}
