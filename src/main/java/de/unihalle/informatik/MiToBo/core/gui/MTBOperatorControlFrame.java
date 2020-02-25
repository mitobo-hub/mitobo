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

import java.awt.event.ActionEvent;
import java.util.Calendar;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import ij.IJ;
import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.gui.ALDOperatorGUIExecutionProxy;
import de.unihalle.informatik.Alida.gui.ALDOperatorControlFrame;
import de.unihalle.informatik.Alida.gui.ALDOperatorDocumentationFrame;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.events.ALDOpParameterUpdateEventListener;
import de.unihalle.informatik.Alida.version.ALDVersionProviderFactory;
import de.unihalle.informatik.MiToBo.core.helpers.MTBIcon;

/**	This frame is used to display and modify the input parameters of an ALDOperator.
 *  If the operator is configured, i.e. iput parameters set, it may be executed
 *  using an ALDExecuteOperatorFrame. This JFrame is free to decide, what executing
 *  precisily means, usually it is to start the operator via its runOp method
 *  and display or store the results appropriately.
 *  <p>
 *  There are thres methodes which control the handling of each input parameter
 *  of the operator to a certain degree.  These are <code>resetCreateInParameterPanel</code>,
 *  <code>registerDescriptor</code>, and <code>setDefaultValue</code>.
 *  This customizing requires quit some knowledge
 *  of the implementation of <code></code>.
 * @author Stefan Posch
 */
public class MTBOperatorControlFrame extends ALDOperatorControlFrame 
	implements StatusListener {

	/**
	 * Construct a JFrame to intantiate and configure an operator <code>opName</code>
   * and use <code>executeOperator</code> to execute this operator if requested by the user.
	 *
	 * @param	_op	Associated operator.
	 * @param	em	Execution proxy.
	 * @throws ALDOperatorException 
	 */
	public MTBOperatorControlFrame(ALDOperator _op, 
		ALDOperatorGUIExecutionProxy em, ALDOpParameterUpdateEventListener pL) 
			throws ALDOperatorException {
		super(_op, em, pL);
		this.titleString = "MTBOperatorControlFrame: " + this.op.getName();
		this.setTitle(this.titleString);
		Class<?>[] classes = (this.op.getClass().getInterfaces());
		for (Class<?> c: classes) {
			if (c.equals(StatusReporter.class)) {
				((StatusReporter)this.op).addStatusListener(this);
			}
		}
	}

	@Override
	protected JMenu generateHelpMenu() {
		JMenu helpM = new JMenu("Help");
		JMenuItem itemHelp = new JMenuItem("Operator Documentation");
		// add operator documentation entry if documentation available
		if (this.op.getDocumentation() != null && !this.op.getDocumentation().isEmpty()) {
			itemHelp.setActionCommand("helpM_docu");
			itemHelp.addActionListener(this);
		}
		else {
			itemHelp.setEnabled(false);
		}
		JMenuItem itemAbout = new JMenuItem("About MiToBo");
		itemAbout.setActionCommand("helpM_about");
		itemAbout.addActionListener(this);
		helpM.add(itemHelp);
		helpM.add(itemAbout);
		return helpM;
	}

	@Override
  public MTBOperatorParameterPanel setupParamConfigPanel() {		
		MTBOperatorParameterPanel opPanel = 
			new MTBOperatorParameterPanel(this.op, this.displayMode, true, this);
		return opPanel;
	}
	
	/* (non-Javadoc)
	 * @see loci.common.StatusListener#statusUpdated(loci.common.StatusEvent)
	 */
	@Override
	public void statusUpdated(StatusEvent e) {
		IJ.showStatus(e.getStatusMessage());
		IJ.showProgress(e.getProgressValue(), e.getProgressMaximum());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		// get the event command
		String command = e.getActionCommand();

		if (command.equals("helpM_about")) {
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
		else if (command.equals("helpM_docu")) {
			String docText = this.op.getDocumentation();
			MTBOperatorDocumentationFrame doc = 
					new MTBOperatorDocumentationFrame(this.op.name, 
							this.op.getClass().getName(), docText);
			doc.setVisible(true);
		}
		// all other events are passed to the super class
		else {
			super.actionPerformed(e);
		}
	}
}
