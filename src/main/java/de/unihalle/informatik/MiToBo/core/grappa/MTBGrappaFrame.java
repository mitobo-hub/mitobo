package de.unihalle.informatik.MiToBo.core.grappa;

import java.util.Collection;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.grappa.ALDGrappaFrame;
import de.unihalle.informatik.Alida.grappa.ALDGrappaWorkbench;
import de.unihalle.informatik.Alida.operator.ALDOperatorLocation;

/**
 * Main frame of Grappa graphical editor for MiToBo.
 * 
 * @author Birgit Moeller
 */

@ALDMetaInfo(export = ExportPolicy.ALLOWED)
public class MTBGrappaFrame extends ALDGrappaFrame {
	
	/**
	 * Default constructor
	 */
	public MTBGrappaFrame(Collection<ALDOperatorLocation> standardOps,
												 Collection<ALDOperatorLocation> applicationOps) {
		super(standardOps, applicationOps);
	}
	
	@Override
  protected ALDGrappaWorkbench initWorkbench() {
		return new MTBGrappaWorkbench(this);
	}
}
