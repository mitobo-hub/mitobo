/*
 * Copyright (C) 2010 - @YEAR@ by the MiToBo development team
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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.operators;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import javax.swing.*;

import de.unihalle.informatik.Alida.operator.*;
import de.unihalle.informatik.Alida.version.ALDVersionProviderFactory;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerXmlbeans;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.*;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.*;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOException;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.gui.OnlineHelpDisplayer;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.core.helpers.MTBIcon;

/**
 * Abstract frame class to configure a detector operator in context of the
 * MiToBo CellCounter plugin.
 *  
 * @author Birgit Moeller
 */
public abstract class CellCounterDetectOperatorConfigWin extends ALDSwingComponent 
	implements ActionListener, ALDSwingValueChangeListener {

	/**
	 * Local flag for debug output.
	 */
	@SuppressWarnings("unused")
	protected boolean debug = false;

	/**
	 * Width of the frame.
	 */
	private static final int windowWidth = 475;
	
	/**
	 * Height of the frame.
	 */
	private static final int windowHeight = 350;

	/**
	 * The operator associated with this frame.
	 */
	protected ALDOperator op = null;

	/**
	 * The top level frame.
	 */
	protected JFrame mainFrame;

	/**
	 * The top level panel of this frame.
	 */
	protected JPanel mainPanel;

	/**
	 * Title string of window.
	 */
	protected String titleString =	"Configure Detector Parameters...";
	
	/**
	 * Last directory visited, initially it's user's home.
	 */
	protected String lastDirectory = System.getProperty("user.home");

	/**
	 * Last selected file.
	 */
	protected File lastFile = new File("operatorParams.xml");

	/**
	 * Ok label to be used on button of Ok message boxes.
	 */
	protected final Object[] okOption = { "OK" };

	/**
	 * Labels to be used on buttons of Yes/No message boxes.
	 */
	protected final Object[] yesnoOption = { "YES", "NO" };

	protected HashMap<String, ALDSwingComponent> guiElements;
	
	/** 
	 * Constructs a control frame for an operator object.
	 * @param _op Operator to be associated with this frame object.
	 * @throws ALDOperatorException
	 */
	public CellCounterDetectOperatorConfigWin(ALDOperator _op) 
			throws ALDOperatorException {
		if (_op == null)
			throw new ALDOperatorException(OperatorExceptionType.INSTANTIATION_ERROR,
				"[ParticleDetectorConfigFrame] no operator given, object null!");
		this.op = _op;

		// init the window
		this.guiElements = new HashMap<String, ALDSwingComponent>();
		this.setupWindow();
	}
	
	/**
	 * Construct the frame to configure an operator.
	 */
	protected void setupWindow() {

		// set up the main panel containing input panel and status bar
		this.mainFrame = new JFrame();
		this.mainPanel = new JPanel();
		this.mainPanel.setLayout(new BorderLayout());
		
		// set up the parameter input panel
		JPanel inputPanel = new JPanel();
		BoxLayout ylayout = new BoxLayout(inputPanel, BoxLayout.Y_AXIS);
		inputPanel.setLayout(ylayout);
		
		// add fields for input parameters
		this.addParameterInputFields(inputPanel);
		
		// wrap input panel into scroll pane
		JScrollPane scrollPane = new JScrollPane(inputPanel);
		this.mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		this.mainPanel.add(Box.createVerticalGlue());
		this.mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		this.mainPanel.add(scrollPane);
		this.mainPanel.add(this.addCloseButtonPanel(), BorderLayout.SOUTH);
		
		// add pane to this window
		this.mainFrame.add(this.mainPanel);
		
		// add a nice menubar
		JMenuBar mainWindowMenu = new JMenuBar();
		JMenu fileM = new JMenu("File");
		JMenuItem itemSave = new JMenuItem("Save Settings");
		itemSave.setActionCommand("fileM_save");
		itemSave.addActionListener(this);
		JMenuItem itemLoad = new JMenuItem("Load Settings");
		itemLoad.setActionCommand("fileM_load");
		itemLoad.addActionListener(this);
		fileM.add(itemSave);
		fileM.add(itemLoad);
		mainWindowMenu.add(fileM);

		// generate help menu
		JMenu helpM = this.generateHelpMenu();
		mainWindowMenu.add(Box.createHorizontalGlue());
		mainWindowMenu.add(helpM);

		// and go ..
		this.mainFrame.setTitle(titleString);
		this.mainFrame.setJMenuBar(mainWindowMenu);
		this.mainFrame.setSize(new Dimension(windowWidth, windowHeight));

	}

	/**
	 * Adds the input fields for all relevant parameters.
	 */
	protected abstract void addParameterInputFields(JPanel parentPanel);
	
	/**
	 * Adds set of control buttons to the main panel.
	 */
	protected JPanel addCloseButtonPanel() {

		// init panel
		JPanel runPanel = new JPanel();
		runPanel.setLayout(new GridLayout(1, 1));

		// close button
		JButton quitButton = new JButton("Close");
		quitButton.setActionCommand("close");
		quitButton.addActionListener(this);

		// now set up a panel to hold the button
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.LINE_AXIS));
		controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 0));
		controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		controlPanel.add(Box.createHorizontalGlue());
		controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		controlPanel.add(quitButton);
		controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		controlPanel.add(Box.createHorizontalGlue());
		controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		runPanel.add(controlPanel);
		return runPanel;
	}

	/**
	 * Set up the help menu.
	 * 
	 * @return Generated help menu.
	 */
	protected JMenu generateHelpMenu() {
		JMenu helpM = new JMenu("Help");
		JMenuItem itemHelp = new JMenuItem("Online Help");
		itemHelp.addActionListener(OnlineHelpDisplayer.getHelpActionListener(
				itemHelp, "welcome", this.mainFrame));
		JMenuItem itemAbout = new JMenuItem("About MiToBo");
		itemAbout.setActionCommand("helpM_about");
		itemAbout.addActionListener(this);
		helpM.add(itemHelp);
		helpM.add(itemAbout);
		return helpM;
	}

	protected void updateGUI() throws ALDOperatorException, ALDDataIOException {
		Set<String> keys = this.guiElements.keySet();
		for (String k: keys) {
			Object value = this.op.getParameter(k);
			ALDDataIOManagerSwing.getInstance().setValue(null, value.getClass(), 
				this.guiElements.get(k), value);
		}
		this.mainPanel.updateUI();
	}
	
	/**
	 * Clean-up on termination.
	 * @return	True if window was closed.
	 */
	public boolean quit() {
		// dispose all resources, i.e. sub-windows
		this.dispose();
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		// local variables
		String command = e.getActionCommand();

		// close the frame
		if (   command.equals("frame_close")
				|| command.equals("close") 
				|| command.equals("fileM_quit")) {
			this.quit();
		}

		// handle menu item commands

		else if (command.equals("fileM_save")) {
			// open file chooser
			JFileChooser getFileDialog = new JFileChooser();
			getFileDialog.setApproveButtonText("Save");
			getFileDialog.setCurrentDirectory(new File(this.lastDirectory));
			getFileDialog.setSelectedFile(this.lastFile);
			getFileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = getFileDialog.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				// check if file exists already, if so ask what to do
				File file = getFileDialog.getSelectedFile();
				if (file.exists()) {
					if ( JOptionPane.showOptionDialog(null, 
							"File " + file.getAbsolutePath() + " exists, override?",
							"file exists",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
							null, null, null) != 0 ) {
						return;
					}
				}
				this.lastFile = file;
				try {
					ALDDataIOManagerXmlbeans.writeXml(file.getAbsolutePath(), this.op);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				this.lastDirectory = file.getAbsolutePath();
			}
		} else if (command.equals("fileM_load")) {
			// open file chooser
			JFileChooser getFileDialog = new JFileChooser();
			getFileDialog.setCurrentDirectory(new File(this.lastDirectory));
			getFileDialog.setSelectedFile(this.lastFile);
			getFileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = getFileDialog.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = getFileDialog.getSelectedFile();
				this.lastDirectory = file.getAbsolutePath();
				this.lastFile = file;
				try {
					ALDOperator loadedOp = (ALDOperator)ALDDataIOManagerXmlbeans.readXml(
						file.getAbsolutePath(), ALDOperator.class);
					if (!(loadedOp instanceof ParticleDetectorUWT2D)) {
						JOptionPane.showMessageDialog(this.mainFrame.getFocusOwner(), 
							"This is not a configuration file for the particle detector!");
					}
					else {
						// set the parameter values
						this.op.setParameter("Jmin", loadedOp.getParameter("Jmin"));
						this.op.setParameter("Jmax", loadedOp.getParameter("Jmax"));
						this.op.setParameter("scaleIntervalSize", 
							loadedOp.getParameter("scaleIntervalSize"));
						this.op.setParameter("corrThreshold", 
							loadedOp.getParameter("corrThreshold"));
						this.op.setParameter("minRegionSize", 
							loadedOp.getParameter("minRegionSize"));
					}
					// show new parameters in GUI
					this.updateGUI();
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
				}
			}
		}
		else if (command.equals("helpM_about")) {
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

	@Override
	public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
		Set<String> keys = this.guiElements.keySet();
		for (String k: keys) {
			Class<?> cl;
      try {
	      cl = this.op.getParameter(k).getClass();
	      Object value = ALDDataIOManagerSwing.getInstance().readData(null, 
		      	cl, this.guiElements.get(k));
	      this.op.setParameter(k, value);
      } catch (ALDException e) {
	      e.printStackTrace();
      }
		}
		// pass event to listeners
		this.fireALDSwingValueChangeEvent(event);
	}

	/**
	 * Set visibility of configuration window.
	 * @param flag	If true, frame will be visible.
	 */
	public void setVisible(boolean flag) {
		this.mainFrame.setVisible(flag);
	}
	
	@Override
  public JComponent getJComponent() {
		// Attention! This is a hack! Hopefully this method is never called in 
		// the context of the cell counter...
	  return null;
  }

	@Override
  public void disableComponent() {
		this.mainFrame.setEnabled(false);
  }

	@Override
  public void enableComponent() {
		this.mainFrame.setEnabled(true);
  }

	@Override
  public void dispose() {
		this.mainFrame.dispose();
  }
}
