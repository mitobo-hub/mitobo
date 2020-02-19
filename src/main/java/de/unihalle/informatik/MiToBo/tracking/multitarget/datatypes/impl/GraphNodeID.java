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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import java.util.Comparator;


/**
 * A graph node object to hold a nodeID as well as a subgraphID
 * @author Oliver Gress
 *
 */
public class GraphNodeID implements Comparable<GraphNodeID>, Comparator<GraphNodeID> {

	/** The node's ID */
	public int nodeID;
	
	/** The node's subgraphID */
	public int subgraphID;
	
	/**
	 * Constructor (subgraphID=0)
	 */
	public GraphNodeID(int nodeID) {
		this(nodeID, 0);
	}
		
	/**
	 * Constructor to assign the node to a subgraph
	 */
	public GraphNodeID(int nodeID, int subgraphID) {
		this.subgraphID = subgraphID;
		this.nodeID = nodeID;
	}

	@Override
	public String toString() {
		return ("NodeID=" + this.nodeID);
	}
	
	
	// ---- Comparable interface implementation ----
	
	@Override
	public int compareTo(GraphNodeID o) {
		if (this.nodeID < o.nodeID)
			return -1;
		else if (this.nodeID > o.nodeID)
			return 1;
		else 
			return 0;
	}
	
	
	// ---- Comparator interface implementation ----
	
	@Override
	public int compare(GraphNodeID o1, GraphNodeID o2) {
		return o1.compareTo(o2);
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof GraphNodeID))
			return false;
		
		return (this.compareTo((GraphNodeID) o) == 0);
	}
	


}