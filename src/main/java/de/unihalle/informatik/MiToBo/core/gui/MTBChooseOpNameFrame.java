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

package de.unihalle.informatik.MiToBo.core.gui;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.unihalle.informatik.Alida.gui.ALDChooseOpNameFrame;
import de.unihalle.informatik.Alida.gui.ALDOperatorGUIExecutionProxy;
import de.unihalle.informatik.Alida.gui.OnlineHelpDisplayer;
import de.unihalle.informatik.Alida.operator.ALDOperatorLocation;
import de.unihalle.informatik.Alida.version.ALDVersionProviderFactory;
import de.unihalle.informatik.MiToBo.core.helpers.MTBIcon;

/**
 * Main window for selecting MiToBo annotated operators for running.
 * 
 * @author Stefan Posch
 * @author Birgit Moeller
 */
public class MTBChooseOpNameFrame extends ALDChooseOpNameFrame {

	/**
	 * Debug flag (not accessible from outside).
	 */
	@SuppressWarnings("unused")
  private boolean debug = false;
	
	/**
	 * Constructor.
	 */
	public MTBChooseOpNameFrame() {
		super();
		this.setTitle("MiToBo - OpRunner: simply choose an operator...");
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.gui.ALDChooseOpNameFrame#executeOperator(de.unihalle.informatik.Alida.operator.ALDOperatorLocation)
	 */
	@Override
  protected void executeOperator(ALDOperatorLocation opLoc) {
		// do we have an operator name?
		if (opLoc != null ) {
			MTBOperatorGUIExecutionProxy execManager = 
					new MTBOperatorGUIExecutionProxy(opLoc);
			this.guiProxys.add(
					new WeakReference<ALDOperatorGUIExecutionProxy>(execManager));
			execManager.showGUI();
		}
	}

	@Override
	protected JMenu generateHelpMenu() {
		JMenu helpM = new JMenu("Help");
		JMenuItem itemHelp = new JMenuItem("Online Help");
		itemHelp.addActionListener(
				OnlineHelpDisplayer.getHelpActionListener(itemHelp,"welcome",this));
		JMenuItem itemAbout = new JMenuItem("About MiToBo");
		itemAbout.setActionCommand("showAbout");
		itemAbout.addActionListener(this);
		helpM.add(itemHelp);
		helpM.add(itemAbout);
		return helpM;
	}
	
	@Override
  protected void showAboutBox() {
		Object[] options = { "OK" };
		String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
		String rev = ALDVersionProviderFactory.getProviderInstance().getVersion();
		if (rev.contains("=")) {
			int equalSign = rev.indexOf("=");
			int closingBracket = rev.lastIndexOf("]");
			rev = rev.substring(0, equalSign + 9) + rev.substring(closingBracket);
		}
		String msg = "<html>MiToBo - A Microscope Image Analysis Toolbox, <p>" 
	    + "Release " + rev + "<p>" + "\u00a9 2010 - " + year + "   "
	    + "Martin Luther University Halle-Wittenberg<p>"
	    + "Institute of Computer Science, Faculty of Natural Sciences III<p><p>"
	    + "Email: mitobo@informatik.uni-halle.de<p>"
	    + "Internet: <i>www.informatik.uni-halle.de/mitobo</i><p>"
	    + "License: GPL 3.0, <i>http://www.gnu.org/licenses/gpl.html</i></html>";

		JOptionPane.showOptionDialog(null, new JLabel(msg),
			"Information about MiToBo", JOptionPane.DEFAULT_OPTION,
		    JOptionPane.INFORMATION_MESSAGE, MTBIcon.getInstance().getIcon(), 
		    	options, options[0]);
	}
}
