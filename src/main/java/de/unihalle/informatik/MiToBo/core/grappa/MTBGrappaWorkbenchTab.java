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

package de.unihalle.informatik.MiToBo.core.grappa;

import java.util.Collection;
import java.util.Set;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench;
import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbenchGraph;
import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbenchTab;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.events.ALDOpParameterUpdateEvent;
import de.unihalle.informatik.Alida.workflows.ALDWorkflow;
import de.unihalle.informatik.Alida.workflows.ALDWorkflowEdge;
import de.unihalle.informatik.Alida.workflows.ALDWorkflowNode;
import de.unihalle.informatik.Alida.workflows.ALDWorkflowNodeID;
import de.unihalle.informatik.MiToBo.core.gui.MTBOperatorConfigurationFrame;

/**
 * Grappa workflow graph.
 * 
 * @author Birgit Moeller
 */
public class MTBGrappaWorkbenchTab extends ALDGrappaWorkbenchTab {
	
	/**
	 * Default constructor
	 */
	public MTBGrappaWorkbenchTab(
			MTBGrappaWorkbench _bench, ALDGrappaWorkbenchGraph _graph) {
		super(_bench, _graph);
	}

	/**
	 * Constructor to setup tab from given (reloaded) workflow.
	 */
	public MTBGrappaWorkbenchTab(ALDGrappaWorkbench bench, 
																	mxGraph _graph, ALDWorkflow _flow) {
		super(bench, _graph, _flow);
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbenchTab#restoreConfigWins()
	 */
	@Override
	protected void restoreConfigWins() {
		Set<ALDWorkflowNodeID> keys = this.graphNodes.keySet();
		ALDOperator op = null;
		for (ALDWorkflowNodeID nodeID : keys) {
			try {
				op = this.alidaWorkflow.getOperator(nodeID);
				ParameterUpdateListener pL = new ParameterUpdateListener(nodeID);
				MTBOperatorConfigurationFrame confWin = this.getNewConfigWin(op, pL);
				// check which input params (required/optional/supplemental) are linked
				Collection<String> inParams = op.getInInoutNames(new Boolean(false));
				inParams.addAll(op.getInInoutNames(new Boolean(true)));
				ALDWorkflowNode wNode = this.alidaWorkflow.getNode(nodeID);
				for (String ip: inParams) {
					// get input edges, should never be more than one!
					Collection<ALDWorkflowEdge> iEdges = wNode.getInEdgesForParameter(ip);
					if (iEdges.isEmpty()) {
						// if there are not incoming edges, skip parameter
						continue;
					}
					// get the edge and its metadata
					ALDWorkflowEdge edge = iEdges.iterator().next();
					ALDWorkflowNode sourceNode = edge.getSourceNode();
					String sourceParam = edge.getSourceParameterName();
					// mark the parameter as linked in configuration window
					confWin.setParameterLinked(
						ip, sourceNode.getOperator().getName(), sourceParam);
				}
//				confWin.addALDOpParameterUpdateEventListener(
//																		new ParameterUpdateListener(nodeID));
				mxCell node = this.graphNodes.get(nodeID);
				this.configWindows.put(node, confWin);
				// fire a parameter change event, this is the first time when the
				// listener can react on that
				confWin.fireALDOpParameterUpdateEvent(new ALDOpParameterUpdateEvent(
					this,	ALDOpParameterUpdateEvent.EventType.CHANGED));
			} catch (ALDException e) {
				String name = (op == null) ? "Unknown Op" : op.getName();
				System.err.println("[MTBGrappaWorkbenchTab::restoreConfigWins] " 
						+ "cannot instantiating config win for \"" + name + "\"...");
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbenchTab#getConfigWin(de.unihalle.informatik.Alida.operator.ALDOperator)
	 */
	@Override
	protected MTBOperatorConfigurationFrame getNewConfigWin(ALDOperator op,
		ParameterUpdateListener pListen) 
			throws ALDOperatorException {
		return new MTBOperatorConfigurationFrame(op, pListen);
	}
}
