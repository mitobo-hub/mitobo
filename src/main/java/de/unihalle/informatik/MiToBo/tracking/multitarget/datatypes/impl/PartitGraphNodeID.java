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

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;


/**
 * A graph node object for multipartite graphs to store a partitionID, a nodeID as well as a subgraphID.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class PartitGraphNodeID extends GraphNodeID {

	/** The node's partitionID */
	public int partitionID;
	
	/**
	 * Constructor to specify partitionID and nodeID (subgraphID=0)
	 */
	public PartitGraphNodeID(int partitionID, int nodeID) {
		super(nodeID);
		this.partitionID = partitionID;
	}
	
	/**
	 * Constructor to specify partitionID, nodeID and subgraphID
	 */
	public PartitGraphNodeID(int partitionID, int nodeID, int subgraphID) {
		super(nodeID, subgraphID);
		this.partitionID = partitionID;
	}
	

	@Override
	public String toString() {
		return ("PartitionID=" + this.partitionID + "\nNodeID=" + this.nodeID);
	}

	@Override
	public int compareTo(GraphNodeID o) {
		
		if (o instanceof PartitGraphNodeID) {
		
			if (this.partitionID < ((PartitGraphNodeID)o).partitionID) 
				return -1;
			else if (this.partitionID > ((PartitGraphNodeID)o).partitionID)
				return 1;
		}

		if (this.nodeID < o.nodeID)
			return -1;
		else if (this.nodeID > o.nodeID)
			return 1;
		else 
			return 0;
	}
	
	
	@Override
	public int compare(GraphNodeID o1, GraphNodeID o2) {
		return o1.compareTo(o2);
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof PartitGraphNodeID))
			return false;
		
		return (this.compareTo((PartitGraphNodeID) o) == 0);
	}
}
