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

package de.unihalle.informatik.MiToBo.core.batch.provider.output.swing;

import de.unihalle.informatik.Alida.annotations.ALDBatchOutputProvider;
import de.unihalle.informatik.Alida.batch.ALDBatchRunResultInfo;
import de.unihalle.informatik.Alida.batch.provider.output.swing.ALDBatchOutputSummarizerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * Implementation of {@link de.unihalle.informatik.Alida.batch.provider.input.swing.ALDBatchInputIteratorSwing} 
 * for {@link MTBTableModel}.
 * <p>
 * The summarizer assumes that each table in the incoming set of tables has the
 * same number of columns, but the number of rows is allowed to vary. The 
 * summarizer generates a new table and summarizes the set of incoming ones
 * by appending the rows of all tables. Note that for now the result table does
 * not explicitly mark the transitions between the different result tables. 
 * 
 * @author moeller
 */
@ALDBatchOutputProvider(priority=1)
public class MTBSummarizerTableModel
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
		classes.add( MTBTableModel.class);
		return classes;
	}
	
	@Override
	public JComponent writeData(ALDBatchRunResultInfo batchInfo,
																ALDParameterDescriptor descr) {
		// fetch result data
		Vector<Object> resultDataCollection = batchInfo.getResultDataVec();
		
		// we get a set of tables, examine first one for extracting meta data
		MTBTableModel tab = (MTBTableModel)resultDataCollection.elementAt(0);
		
		// ... all tables must share same number of columns and also headers
		int cols = tab.getColumnCount();
		// determine total number of rows, might differ between tables
		int rows = 0;
  	for (int i=0; i<resultDataCollection.size(); i++) {
  		tab = (MTBTableModel)resultDataCollection.get(i);
  		rows += tab.getRowCount();
  	}
  	// init result table and set headers
		MTBTableModel summaryTab = new MTBTableModel(rows, cols);
		for (int i=0; i<cols; ++i) {
			summaryTab.setColumnName(i, tab.getColumnName(i));
		}
			
		// fill summary table
		rows = 0;
  	for (int i=0; i<resultDataCollection.size(); i++) {
  		tab = (MTBTableModel)resultDataCollection.get(i);
  		for (int r = rows; r < rows + tab.getRowCount(); ++r) {
  			for (int c = 0; c < cols; ++c) {
  				summaryTab.setValueAt(tab.getValueAt(r - rows, c), r, c);
  			}
  		}
  		rows += tab.getRowCount();
  	}
  	
  	// return data I/O provider for table model
  	try {
			ALDDataIOSwing imageProvider =
					(ALDDataIOSwing)ALDDataIOManagerSwing.getInstance().getProvider(
																	MTBTableModel.class, ALDDataIOSwing.class);
			return imageProvider.writeData(summaryTab, descr);
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
