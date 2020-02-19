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
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * A panel with a titled border and a set of radio buttons that can be 
 * reconfigured easily. 
 * @author Oliver Gress
 *
 */
public class RadioButtonPanel extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6129897738812922848L;

	/** vertical box to place the radio buttons on the panel */
	protected Box rbbox;
	
	/** the logical group of radio buttons */
	protected ButtonGroup rbgroup;
	
	/** string/label of the selected radio button */
	protected String selection;
	
	/** Property change of the panel when a selection is changed */
	public static final String SELECTION_CHANGED_PROPERTY = "SelectionChanged";
	
	/**
	 * Constructor
	 * @param title panel title
	 */
	public RadioButtonPanel(String title) {
		
		this.setBorder(BorderFactory.createTitledBorder(title));
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		this.rbbox = Box.createVerticalBox();
		
		this.add(this.rbbox);
		this.add(Box.createHorizontalGlue());
		
		this.rbgroup = new ButtonGroup();
		this.selection = null;
		
	}
	
	/**
	 * Adds radio buttons to the panel according the <code>choices</code> array.
	 * <code>defaultchoice</code> may specify a default button or may be null (no default selection).
	 * @param choices
	 * @param defaultchoice
	 */
	public void fillPanel(String[] choices, String defaultchoice) {
		JRadioButton jrb;
		for (int i = 0; i < choices.length; i++) {
			jrb = new JRadioButton(choices[i]);
			jrb.setFont(this.getFont().deriveFont(this.getFont().getSize2D() - 2));
		    jrb.setActionCommand(choices[i]);
		    jrb.addActionListener(this);

			this.rbgroup.add(jrb);
			this.rbbox.add(jrb);
			
			if (i == 0) {
				jrb.setSelected(true);
				this.selection = choices[i];
			}
			if (defaultchoice != null && choices[i].equals(defaultchoice)) {
				jrb.setSelected(true);
				this.selection = defaultchoice;
			}
		}
		
		this.rbbox.repaint();
		this.repaint();
	}
	
	/**
	 * Remove all radiobuttons from the panel.
	 */
	public void clearPanel() {
		Enumeration<AbstractButton> buttons = this.rbgroup.getElements();
		
		// remove the radio buttons
		AbstractButton[] b = new AbstractButton[this.rbgroup.getButtonCount()];
		
		int cnt = 0;
		while (buttons.hasMoreElements()) {
			b[cnt++] = buttons.nextElement();
		}
		
		for (int i = 0; i < b.length; i++) {
			this.rbgroup.remove(b[i]);
		}
		this.rbbox.removeAll();
		
		this.selection = null;
		
		this.rbbox.repaint();
		this.repaint();
	}
	
	/**
	 * Get the currently selected radio button string
	 * @return	Identifier string of selected button.
	 */
	public String getSelection() {
		return this.selection;
	}

	/**
	 * Assigns the selected radio button string and fires a PropertyChangeEvent with
	 * property <code>RadioButtonPanel.SELECTION_CHANGED_PROPERTY</code>
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String oldselection = this.selection;
		this.selection = e.getActionCommand();
		this.firePropertyChange(RadioButtonPanel.SELECTION_CHANGED_PROPERTY, oldselection, this.selection);
	}
	
}
