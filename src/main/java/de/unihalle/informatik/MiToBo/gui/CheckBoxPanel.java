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

package de.unihalle.informatik.MiToBo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * A panel with a titled border and a set of checkboxes that can be 
 * reconfigured easily. 
 * @author Oliver Gress
 *
 */
public class CheckBoxPanel extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9637672307282272L;

	/** container for arrangement of checkboxes */
	protected Box cbbox;
	
	/** checkbox components */
	protected JCheckBox[] cbs;
	
	/** property string that is fired when the state of any checkbox is changed */
	public static final String CHECK_CHANGED_PROPERTY = "CheckChanged";
	
	/**
	 * Constructor
	 * @param title
	 */
	public CheckBoxPanel(String title) {
		this.setBorder(BorderFactory.createTitledBorder(title));
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		this.cbbox = Box.createVerticalBox();
		
		this.add(this.cbbox);
		this.add(Box.createHorizontalGlue());
		
		this.cbs = null;
	}
	
	
	/**
	 * Adds checkboxes to the panel according the <code>entries</code> array.
	 * <code>defauls</code> may specify the default values for each entry or may be null (defaults to true).
	 * @param entries			Entries of the boxes in the panel.
	 * @param defaults		Default settings of boxes.
	 */
	public void fillPanel(String[] entries, boolean[] defaults) {
		JCheckBox jcb;
		this.cbs = new JCheckBox[entries.length];
		
		for (int i = 0; i < entries.length; i++) {
			jcb = new JCheckBox(entries[i]);
			jcb.setFont(this.getFont().deriveFont(this.getFont().getSize2D() - 2));
		    jcb.setActionCommand(entries[i]);
		    jcb.addActionListener(this);

			this.cbbox.add(jcb);
			this.cbs[i] = jcb;
			
			if (defaults != null && i < defaults.length) {
				jcb.setSelected(defaults[i]);
			}
			else {
				jcb.setSelected(true);
			}
		}
		
		this.cbbox.repaint();
		this.repaint();
	}
	
	/**
	 * Remove all radiobuttons from the panel.
	 */
	public void clearPanel() {

		this.cbbox.removeAll();
		this.cbs = null;
		
		this.cbbox.repaint();
		this.repaint();
	}
	
	public boolean[] getSelections() {
		if (this.cbs != null) {
			boolean[] checks = new boolean[this.cbs.length];
			
			for (int i = 0; i < this.cbs.length; i++)
				checks[i] = this.cbs[i].isSelected();
			
			return checks;
		}
		return null;
	}
	
	/**
	 * Fires a PropertyChangeEvent with
	 * property <code>CheckBoxPanel.CHECK_CHANGED_PROPERTY</code> if a checkbox value was changed
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		this.firePropertyChange(CheckBoxPanel.CHECK_CHANGED_PROPERTY, null, null);
	}


}
