/*
OME Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
LOCI Common package: utilities for I/O, reflection and miscellaneous tasks.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden and Chris Allan.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
Bio-Formats Importer, Bio-Formats Exporter, Bio-Formats Macro Extensions,
Data Browser and Stack Slicer. Copyright (C) 2005-@year@ Melissa Linkert,
Curtis Rueden and Christopher Peterson.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

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

/*
 * This class uses the Bio-Formats and LOCI-commons packages/libraries (see the two licenses at the top)
 * as well as source code from the LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.core.batch.provider.output.swing;

import de.unihalle.informatik.Alida.annotations.ALDBatchOutputProvider;
import de.unihalle.informatik.Alida.batch.ALDBatchRunResultInfo;
import de.unihalle.informatik.Alida.batch.provider.input.swing.ALDBatchInputIteratorSwing;
import de.unihalle.informatik.Alida.batch.provider.output.swing.ALDBatchOutputSummarizerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Particles;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_ParticlesMultiChannel;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * Implementation of {@link ALDBatchInputIteratorSwing} for MitoBo images.
 * 
 * @author moeller
 */
@ALDBatchOutputProvider(priority=1)
public class MTBSummarizerSegResultParticlesMultiChannel
	implements ALDBatchOutputSummarizerSwing {
	
	/**
	 * Interface method to announce class for which IO is provided for
	 * field is ignored.
	 * 
	 * @return	Collection of classes provided
	 */
	@Override
  public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add( SegResult_ParticlesMultiChannel.class);
		return classes;
	}
	
	@Override
	public JComponent writeData(ALDBatchRunResultInfo batchInfo,
																ALDParameterDescriptor descr) {
		// fill result table
		Vector<String> headers = new Vector<String>();
		headers.add("Parameter value");
		headers.add("Image");
		headers.add("Channel");
		headers.add("#Particles");
		headers.add("Average Size");
		headers.add("Minimal Size");
		headers.add("Maximal Size");
		
		Vector<Object> parameterValues = batchInfo.getParameterValueVec();
		Vector<Object> resultObjects = batchInfo.getResultDataVec();
		MTBTableModel resultTab = 
				new MTBTableModel(resultObjects.size(), 5, headers);
		int nextRow = -1;
		for (int rCount = 0; rCount < resultObjects.size(); ++rCount) {
			++nextRow;
			SegResult_ParticlesMultiChannel segr = 
					(SegResult_ParticlesMultiChannel)resultObjects.get(rCount);
			resultTab.setValueAt(parameterValues.get(rCount), nextRow, 0);
			--nextRow;
			for (SegResult_Particles segp : segr.getResultVec()) {
				++nextRow;
				resultTab.setValueAt(segp.getImageName(), nextRow, 1);
				resultTab.setValueAt(new Integer(segp.getProcessedChannel()), nextRow, 2);
				resultTab.setValueAt(new Integer(segp.getParticleCount()), nextRow, 3);
				resultTab.setValueAt(new Double(segp.getParticleAvgSize()), nextRow, 4);
			}
		}
  	// return data I/O provider for table
  	try {
			ALDDataIOSwing imageProvider =
					(ALDDataIOSwing)ALDDataIOManagerSwing.getInstance().getProvider(
																	MTBTableModel.class, ALDDataIOSwing.class);
			return imageProvider.writeData(resultTab, descr);
		} catch (ALDDataIOManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDDataIOProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	return null;
	}
}
