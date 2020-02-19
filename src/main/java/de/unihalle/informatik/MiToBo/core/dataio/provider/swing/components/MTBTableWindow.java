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

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing.components;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;


/**
 * GUI window for displaying tables in MiToBo. 
 * <p> 
 * Tables to be displayed with by this window need to extend 
 * {@link MTBTableModel}.
 * 
 * @author moeller
 */
public class MTBTableWindow extends ALDTableWindow {

	/**
	 * Default constructor.
	 * @param mtm	Associated table model.
	 */
	public MTBTableWindow(MTBTableModel mtm) {
		super(mtm);
		this.optionsWindow.setDelimiter(mtm.getDelimiter());
	}

	/**
	 * Internal class that realizes a {@link FileFilter} for text
	 * files where MiToBo table data is stored.
	 *	
	 * @author moeller
	 */
	protected class DataTabFileFilter extends FileFilter {
		
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File f) {
			return (f.getName().endsWith(".txt") || f.isDirectory());
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		@Override
		public String getDescription() {
			return "MiToBo Data Table Files (*.txt)";
		}
	}

//	public static void main(String [] args) {
//		MTBTableModel model = new MTBTableModel(3, 3);
//		model.setDelimiter(TableModelDelimiter.TAB);
//		model.setValueAt("1", 0, 0);
//		model.setValueAt("2", 0, 1);
//		model.setValueAt("3", 0, 2);
//		model.setValueAt("4", 1, 0);
//		model.setValueAt("5", 1, 1);
//		model.setValueAt("6", 1, 2);
//		model.setValueAt("7", 2, 0);
//		model.setValueAt("8", 2, 1);
//		model.setValueAt("9", 2, 2);
//		MTBTableWindow win = new MTBTableWindow(model);
//		win.setVisible(true);
//	}
}
