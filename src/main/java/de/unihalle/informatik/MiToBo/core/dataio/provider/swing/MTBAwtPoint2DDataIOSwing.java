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

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentTextField;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;

import javax.swing.*;

import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Data I/O class for {@link java.awt.geom.Point2D.Double}.
 * 
 * @author moeller
 */
@ALDDataIOProvider
public class MTBAwtPoint2DDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add(Point2D.Double.class);
		return classes;
	}

	@Override
	public PointConfigPanel createGUIElement(Field field, Class<?> cl, 
			Object obj, ALDParameterDescriptor descr) {
		return new PointConfigPanel(obj, descr);
	}

	@Override
	public void setValue(Field field, Class<?> cl, 
			ALDSwingComponent guiElement,	Object value) 
		throws ALDDataIOProviderException {
		if (!(guiElement instanceof PointConfigPanel))
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
				"MTBAwtPoint2DDataIO: setValue() received invalid GUI element!");
		if (!(value instanceof Point2D.Double))
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
				"MTBAwtPoint2DDataIO: setValue() received wrong object type!");
		((PointConfigPanel)guiElement).setValue((Point2D.Double)value);
		((PointConfigPanel)guiElement).getJComponent().updateUI();
	}

	@Override
	public Object readData(
			Field field, Class<?> cl, ALDSwingComponent guiElement) 
		throws ALDDataIOProviderException {

		if (!(guiElement instanceof PointConfigPanel))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
					"AwtPoint2DDataIO: readData() received invalid GUI element!");
		return ((PointConfigPanel)guiElement).readData();
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {
		PointConfigPanel p = new PointConfigPanel(obj, descr);
		p.getJComponent().setEnabled(false);
		return p.getJComponent();
	}

	/**
	 * Panel to display and read 2D points.
	 * 
	 * @author moeller
	 */
	private class PointConfigPanel extends ALDSwingComponent 
		implements ALDSwingValueChangeListener {
		
		/**
		 * Main panel.
		 */
		private JPanel mainPanel;
		
		/**
		 * Field for x-coordinate.
		 */
		private ALDSwingComponentTextField xField;
		/**
		 * Field for y-coordinate.
		 */
		private ALDSwingComponentTextField yField;
		/**
		 * (Operator) parameter descriptor of associated parameter. 
		 */
		private ALDParameterDescriptor paramDescr;
		
		/**
		 * Default constructor. 
		 * @param obj		Default object.
		 * @param d			(Operator) parameter descriptor.
		 */
		public PointConfigPanel(Object obj, ALDParameterDescriptor d) {
			this.mainPanel = new JPanel();
			this.paramDescr = d;
			double x=0,y=0;
			if (obj != null && obj instanceof Point2D.Double) {
				x = ((Point2D.Double)obj).x;
				y = ((Point2D.Double)obj).y;
			}
			this.mainPanel.setLayout(
					new BoxLayout(this.mainPanel, BoxLayout.X_AXIS));
			this.mainPanel.add(new JLabel("x = "));
			this.xField = new ALDSwingComponentTextField(Double.class, 
					this.paramDescr, 10);
			this.xField.setText(Double.toString(x));
			this.xField.addValueChangeEventListener(this);
			this.mainPanel.add(this.xField.getJComponent());
			this.mainPanel.add(new JLabel("y = "));
			this.yField = new ALDSwingComponentTextField(Double.class, 
					this.paramDescr, 10);
			this.yField.setText(Double.toString(y));
			this.yField.addValueChangeEventListener(this);
			this.mainPanel.add(this.yField.getJComponent());
		}
		
		@Override
		public JComponent getJComponent() {
			return this.mainPanel;
		}
		
		/**
		 * Displays specified coordinates.
		 * @param p	Point to display.
		 */
		public void setValue(Point2D.Double p) {
			this.xField.setText(Double.toString(p.x));
			this.yField.setText(Double.toString(p.y));
			this.mainPanel.updateUI();
		}

		/**
		 * Gets the current coordinates.
		 * @return	Point configured in panel.
		 */
		public Point2D.Double readData() {
			double x = Double.valueOf(this.xField.getText()).doubleValue();
			double y = Double.valueOf(this.yField.getText()).doubleValue();
			return new Point2D.Double(x,y);
		}

		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}

		@Override
    public void disableComponent() {
			this.xField.disableComponent();
			this.yField.disableComponent();
    }

		@Override
    public void enableComponent() {
			this.xField.enableComponent();
			this.yField.enableComponent();
    }
		
		@Override
    public void dispose() {
			// nothing to do here
    }
	}

}
