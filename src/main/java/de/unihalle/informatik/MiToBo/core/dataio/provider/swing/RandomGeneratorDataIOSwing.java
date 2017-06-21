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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentComboBox;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentItem;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentTextField;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;

/**
 * Data I/O provider for GUI-OpRunner for <code>java.util.Random</code> 
 * objects.
 * 
 * @author Oliver Gress
 *
 */
@ALDDataIOProvider
public class RandomGeneratorDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Random.class);
		return l;
	}

	@Override
	public ALDSwingComponent createGUIElement(
			Field field, Class<?> cl, Object obj, ALDParameterDescriptor descr) 
		throws ALDDataIOProviderException {
		if ( obj != null && !(obj instanceof Random)) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"Object is instance of " + obj.getClass() + " instead of " 
							+ Random.class);
		}
		
		try {
			return new RandomGeneratorChooser((Random)obj, descr);
		} catch (Exception e) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR, 
					"Cannot instantiate RandomGeneratorChooser: "+e.getMessage());
		} 
	}

	@Override
	public void setValue(Field field, Class<?> cl, 
			ALDSwingComponent guiElement, Object value) 
		throws ALDDataIOProviderException {
		if (value == null)
			return;
		if (!(value instanceof Random))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"value is instance of " + value.getClass() + 
					" instead of " + Random.class);
		if (!(guiElement instanceof RandomGeneratorChooser))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"guiElement is instance of " + guiElement.getClass() + 
					" instead of " + RandomGeneratorChooser.class);
		
		try {
			((RandomGeneratorChooser)guiElement).setRandom((Random)value);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Cannot instantiate Random (generator): "+ 
					e.getMessage());
		} 
	}

	@Override
	public Object readData(
			Field field, Class<?> cl, ALDSwingComponent guiElement) 
		throws ALDDataIOProviderException {

		if (!(guiElement instanceof RandomGeneratorChooser))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"guiElement is instance of " + guiElement.getClass() + 
					" instead of " + RandomGeneratorChooser.class);
		
		return ((RandomGeneratorChooser)guiElement).getRandom();
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) 
			throws ALDDataIOProviderException {
		if (obj != null && !(obj instanceof Random))	{
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"Object is instance of " + obj.getClass() + 
					" instead of " + Random.class);
		}

		try {
			return 
					new RandomGeneratorChooser((Random)obj, descr).getJComponent();
		} catch (Exception e) {
			System.err.println("Cannot instantiate RandomGeneratorChooser: "+ 
					e.getMessage());
			return null;
		} 
	}
	
	private class RandomGeneratorChooser extends ALDSwingComponent
		implements ActionListener, KeyListener, ALDSwingValueChangeListener {
		
		/**
		 * Main panel.
		 */
		private JPanel mainPanel;
		
		private ALDSwingComponentComboBox cb;
		private ALDSwingComponentTextField tf;
		private Color disabledColor;
		private Color validColor;
		private Color invalidColor;
		
		/**
		 * (Operator) parameter descriptor of associated parameter.
		 */
		private ALDParameterDescriptor paramDescr;
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 4092461263781804124L;

		public RandomGeneratorChooser(Random rand, ALDParameterDescriptor d) 
			throws SecurityException, IllegalArgumentException, 
				ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
			
			this.mainPanel = new JPanel();
			this.paramDescr = d;
			
			Vector<ALDSwingComponentItem> items = 
					new Vector<ALDSwingComponentItem>();
			items.add(new ALDSwingComponentItem("None (null)",
					"None (null)", null));
			items.add(new ALDSwingComponentItem("Seeded random generator",
					"Seeded random generator", "-1 = default seed"));
			this.cb = new ALDSwingComponentComboBox(this.paramDescr, items);
			this.cb.getJComponent().addActionListener(this);
			this.cb.addValueChangeEventListener(this);
			
			this.tf = 
				new ALDSwingComponentTextField(Long.class, this.paramDescr, 25);
			this.tf.setText(new Long(-1).toString());
			this.tf.addValueChangeEventListener(this);
			this.tf.getJComponent().setToolTipText(
								"Optionally input a numerical seed for the random generator");
			this.tf.getJComponent().setEditable(false);			
			this.tf.getJComponent().addKeyListener(this);
			this.disabledColor = this.tf.getJComponent().getBackground();
			this.invalidColor = new Color(255,200,200);
			this.validColor = new Color(255,255,255);
			
			this.mainPanel.setLayout(
					new BoxLayout(this.mainPanel, BoxLayout.X_AXIS));
			this.mainPanel.add(this.cb.getJComponent());
			this.mainPanel.add(this.tf.getJComponent());
			
			this.setRandom(rand);
		}
		
		@Override
		public JComponent getJComponent() {
			return this.mainPanel;
		}
		
		public Random getRandom() throws NumberFormatException {
			if (this.cb.getJComponent().getSelectedIndex() == 0) {
				return null;
			}
			else if (this.cb.getJComponent().getSelectedIndex() == 1) {
				Long seed = Long.parseLong(this.tf.getText());
				if (seed == -1)
					return new Random();
				return new Random(Long.parseLong(this.tf.getText()));
			}
			return null;
		}
		
		public void setRandom(Random rand) 
			throws ClassNotFoundException, SecurityException, 
				NoSuchFieldException, IllegalArgumentException, 
				IllegalAccessException {
			
			if (rand != null) {

				this.cb.getJComponent().setSelectedIndex(1);
				
				Class<?> cls = Class.forName("java.util.Random");
				Field fld = cls.getDeclaredField("seed");
				fld.setAccessible(true);
				AtomicLong al = (AtomicLong) fld.get(rand);
				
				this.tf.setText(""+al.get());
				
				this.mainPanel.updateUI();
			}
			else {
				this.cb.getJComponent().setSelectedIndex(0);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("comboBoxChanged")) {
				
				if (this.cb.getJComponent().getSelectedIndex() == 0) {
					this.tf.getJComponent().setEditable(false);
					this.tf.getJComponent().setBackground(this.disabledColor);
				}
				else if (this.cb.getJComponent().getSelectedIndex() == 1) {
					this.tf.getJComponent().setEditable(true);
					this.validateSeedTextField();
				}
				
				this.mainPanel.updateUI();
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// nothing to do here
		}

		@Override
		public void keyReleased(KeyEvent e) {
			this.validateSeedTextField();
		}

		@Override
		public void keyTyped(KeyEvent e) {
			// nothing to do here
		}
		
		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}
		
		private void validateSeedTextField() {
			int pos = this.tf.getJComponent().getCaretPosition();
			try {
				Long.parseLong(this.tf.getText());
				this.tf.getJComponent().setBackground(this.validColor);
			} catch (NumberFormatException e1) {
				this.tf.getJComponent().setBackground(this.invalidColor);
			}
			this.tf.getJComponent().updateUI();
			this.tf.getJComponent().setCaretPosition(pos);
		}

		@Override
    public void disableComponent() {
			this.cb.disableComponent();
			this.tf.disableComponent();
    }

		@Override
    public void enableComponent() {
			this.cb.enableComponent();
			this.tf.enableComponent();
    }

		@Override
    public void dispose() {
			// nothing to do here
    }
	}

}
