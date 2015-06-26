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

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;

/**
 * Data I/O provider for GUI-OpRunner for 
 * <code>java.awt.Color</code> objects.
 * 
 * @author Oliver Gress
 */
@ALDDataIOProvider
public class AwtColorDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Color.class);
		return l;
	}

	@Override
	public ColorChooserPanel createGUIElement(Field field, Class<?> cl, 
				Object obj, ALDParameterDescriptor descr)
			throws ALDDataIOProviderException {
		
		if (obj != null && !(obj instanceof Color))	{
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"Object is instance of " + obj.getClass() + " instead of " 
						+ Color.class);
		}

		return new ColorChooserPanel((Color)obj, descr, true);
	}

	@Override
	public void setValue(Field field, Class<?> cl, 
			ALDSwingComponent guiElement, Object value) 
		throws ALDDataIOProviderException {
		if (value != null && !(value instanceof Color))
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"value is instance of " + value.getClass() + " instead of " 
						+ Color.class);
		if (!(guiElement instanceof ColorChooserPanel))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"guiElement is instance of " + guiElement.getClass() 
					+ " instead of " 
					+ ColorChooserPanel.class);
		
		((ColorChooserPanel)guiElement).setChosenColor((Color)value);
	}

	@Override
	public Object readData(
			Field field, Class<?> cl, ALDSwingComponent guiElement)
		throws ALDDataIOProviderException {
		if (!(guiElement instanceof ColorChooserPanel))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"guiElement is instance of " + guiElement.getClass() 
					+ " instead of " 
					+ ColorChooserPanel.class);
		
		return ((ColorChooserPanel)guiElement).getChosenColor();
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) 
			throws ALDDataIOProviderException {
		if (obj != null && !(obj instanceof Color))	{
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"Object is instance of " + obj.getClass() + " instead of " 
						+ Color.class);
		}

		return 
				new ColorChooserPanel((Color)obj, descr, false).getJComponent();
	}
	

	/**
	 * Panel with configuration elements.
	 */
	public class ColorChooserPanel extends ALDSwingComponent 
		implements ActionListener {
		
		/**
		 * Panel containing configuration buttons.
		 */
		private JPanel configPanel;
		
		/**
		 * Button to open selection window.
		 */
		private JButton selectButton;

		/**
		 * Button to reset color to <code>null</code>.
		 */
		private JButton resetButton = null;

		/**
		 * Current color selected.
		 */
		private Color color;
		
		/**
		 * Flag to ignore presses on either button.
		 */
		private boolean ignoreButtonPress = false;
		
		/**
		 * (Operator) parameter descriptor associated with parameter. 
		 */
		private ALDParameterDescriptor paramDescr;
		
		/**
		 * Default constructor for panel without reset button.
		 * @param initialColor	Initial color to set.
		 * @param d							Descriptor associated with color parameter.
		 */
		public ColorChooserPanel(Color initialColor, 
				ALDParameterDescriptor d) {
			this(initialColor, d, false);
		}
		
		/**
		 * Default constructor.
		 * @param initialColor	Initial color to set.
		 * @param d							Descriptor associated with color parameter.
		 * @param allowReset		Allow for resetting color by reset button.
		 */
		public ColorChooserPanel(Color initialColor, 
				ALDParameterDescriptor d, boolean allowReset) {
			
			this.color = initialColor;
			this.paramDescr = d;
			
			this.selectButton = new JButton();
			this.updateButtonColor(this.color);
			
			this.selectButton.setActionCommand("openColorChooser");
			this.selectButton.addActionListener(this);
			this.selectButton.setToolTipText("Choose a color. " 
				+ "Press 'Cancel' in the ColorChooser to unset the color.");
			
			// init reset button, if requested
			if (allowReset) {
				this.resetButton = new JButton("Reset");
				this.resetButton.setActionCommand("resetColor");
				this.resetButton.addActionListener(this);
				this.resetButton.setToolTipText("Reset color to nothing.");
			}
			
			this.configPanel = new JPanel();
			this.configPanel.add(this.selectButton);
			if (this.resetButton != null)
				this.configPanel.add(this.resetButton);
		}

		@Override
		public JPanel getJComponent() {
			return this.configPanel;
		}
		
		/**
		 * Adds external action listeners to buttons.
		 * <p>
		 * Attention! Only do this if you know what you are doing. Having
		 * multiple action listeners reacting on the same events might
		 * cause severe problems.
		 * @param l	Action listener to add to the buttons.
		 */
		public void addActionListenerToButtons(ActionListener l) {
			this.selectButton.addActionListener(l);
			if (this.resetButton != null)
				this.resetButton.addActionListener(l);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.ignoreButtonPress)
				return;
			
			String cmd = e.getActionCommand();
			if (cmd.equals("openColorChooser")) {
				Color oldColor = this.color;
				this.color = 
						JColorChooser.showDialog(null, "ColorChooser", this.color);

				// check if color was chosen, if not, keep old state
				if (this.color != null)
					this.updateButtonColor(this.color);
				else
					return;
				
				// check if color changed
				if (oldColor == null 	|| !(oldColor.equals(this.color))) {
					this.fireALDSwingValueChangeEvent(
						new ALDSwingValueChangeEvent(this, this.paramDescr));
				}
			}
			else if (cmd.equals("resetColor")) {
				this.updateButtonColor(null);
			}
			
		}
		
		/**
		 * Read currently selected color.
		 * @return	Selected color.
		 */
		public Color getChosenColor() {
			return this.color;
		}
		
		/**
		 * Select given color.
		 * @param c	Color to display as selected.
		 */
		public void setChosenColor(Color c) {
			this.color = c;
			this.updateButtonColor(this.color);
		}
		
		/**
		 * Update the button icon with the given color.
		 * @param c	Color to be displayed as button icon.
		 */
		private void updateButtonColor(Color c) {
			JButton jcbutton = new JButton("dummy");
			
			int icsize = 15;
			int linecolor = 0;
			
			BufferedImage im = 
				new BufferedImage(icsize, icsize, BufferedImage.TYPE_INT_RGB);
			if (c == null) {
				int bg = jcbutton.getBackground().getRGB();
				
				for (int y = 0; y < icsize; y++) {
					for (int x = 0; x < icsize; x++) {
						if (   x == y || x == icsize-y-1 || x == 0 
								|| y == 0 || x == icsize-1 || y == icsize-1)
							im.setRGB(x, y, linecolor);
						else
							im.setRGB(x, y, bg);
					}
				}
			}
			else {
				for (int y = 0; y < icsize; y++) {
					for (int x = 0; x < icsize; x++) {
						if (x == 0 || y == 0 || x == icsize-1 || y == icsize-1)
							im.setRGB(x, y, linecolor);
						else
							im.setRGB(x, y, c.getRGB());
					}
				}
			}

			ImageIcon ic = new ImageIcon(im);
			this.selectButton.setIcon(ic);
			this.selectButton.updateUI();
		}

		@Override
    public void disableComponent() {
			this.selectButton.setEnabled(false);
    }

		@Override
    public void enableComponent() {
			this.selectButton.setEnabled(true);
    }
		
		@Override
    public void dispose() {
			// nothing to do here
    }
		
		/**
		 * Enable/disable buttons.
		 * @param flag	If true, button presses are ignored.
		 */
		public void ignoreButtonPress(boolean flag) {
			this.ignoreButtonPress = flag;
		}
	}

}
