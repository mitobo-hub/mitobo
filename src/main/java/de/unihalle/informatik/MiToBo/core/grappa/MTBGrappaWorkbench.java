package de.unihalle.informatik.MiToBo.core.grappa;

import com.mxgraph.view.mxGraph;

import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench;
import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbenchGraph;
import de.unihalle.informatik.Alida.workflows.ALDWorkflow;

/**
 * Main frame of Grappa in MiToBo context.
 * @author Birgit Moeller
 */

public class MTBGrappaWorkbench extends ALDGrappaWorkbench {
	
	/**
	 * Default constructor
	 */
	public MTBGrappaWorkbench(MTBGrappaFrame frame) {
		super(frame);
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench#initNewTab(com.mxgraph.view.mxGraph, java.lang.String)
	 */
	@Override
  protected MTBGrappaWorkbenchTab initNewTab(ALDGrappaWorkbenchGraph graph) {
		return new MTBGrappaWorkbenchTab(this,graph);		
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench#initReloadedWorkflow(com.mxgraph.view.mxGraph, de.unihalle.informatik.Alida.workflows.ALDWorkflow)
	 */
	@Override
	protected void initReloadedWorkflow(
			mxGraph _graph, ALDWorkflow _flow) {
		// check how many workflow tabs are currently present, if there
		// is only a single empty (unused) one, delete that tab first
		if (    this.workflows.size() == 1
				&& !this.workflows.get(0).workflowHasNodes()
				&&    this.workflows.get(0).getWorkflowTitle() 
				   == ALDWorkflow.untitledWorkflowName) {
			this.removeWorkflow();
		}
		// instantiate the associated tab in window
		MTBGrappaWorkbenchTab newContent = 
				this.initReloadedTab(_graph, _flow);
		newContent.setFocusable(true);
		newContent.setToolTips(true);
		this.workflows.add(newContent);
		this.addTab(newContent.getWorkflowTitle(),newContent);
		// set reloaded workflow active
		this.setSelectedIndex(this.getComponentCount()-1);
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench#initReloadedTab(com.mxgraph.view.mxGraph, de.unihalle.informatik.Alida.workflows.ALDWorkflow)
	 */
	@Override
	protected MTBGrappaWorkbenchTab initReloadedTab(mxGraph _graph, 
			ALDWorkflow _flow) {
		return new MTBGrappaWorkbenchTab(this,_graph,_flow);				
	}
}
