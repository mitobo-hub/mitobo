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

package de.unihalle.informatik.MiToBo.gui;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * A panel with a titled border and two columns for a variable number of generic components that are
 * placed vertically with an additional label.
 * @author Oliver Gress
 *
 */
public class TwoColumnPanel extends JPanel {


	/**
	 * 
	 */
	private static final long serialVersionUID = -8467133285753886082L;

	/** panel to hold all the entries */
	protected JPanel entriespanel;
	
	/**
	 * Constructor
	 * @param title panel title
	 */
	public TwoColumnPanel(String title) {
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(BorderFactory.createTitledBorder(title));

		this.entriespanel = new JPanel();
		Box box = Box.createVerticalBox();
		box.add(this.entriespanel);
		box.add(Box.createVerticalGlue());
		
		this.add(box);
		this.add(Box.createHorizontalGlue());
		
		this.setVisible(false);
	}
	
	/**
	 * Add the components in the <code>HashMap</code> to the panel.
	 * The HashMap's key is treated as the label and will be placed in the 
	 * left column. The corresponding component is placed in the right column.
	 * @param options
	 */
	public void fillPanel(HashMap<String, Component> options) {
		
		Set<String> labels = options.keySet();
		GridLayout gl = new GridLayout(labels.size(),2);
		this.entriespanel.setLayout(gl);
		
		for (String label : labels) {
			JLabel jl = new JLabel(label);
			jl.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			jl.setFont(this.getFont().deriveFont(this.getFont().getSize2D() - 2));
			
			this.entriespanel.add(jl);
			
			Box box = Box.createHorizontalBox();
			box.add(Box.createHorizontalGlue());
			box.add(options.get(label));
			
			this.entriespanel.add(box);
		}

		this.entriespanel.repaint();
		this.repaint();
	}
	
	/**
	 * Remove all entries from the panel.
	 */
	public void clearPanel() {
		this.entriespanel.removeAll();
		this.entriespanel.repaint();
		this.repaint();
	}
	
}
