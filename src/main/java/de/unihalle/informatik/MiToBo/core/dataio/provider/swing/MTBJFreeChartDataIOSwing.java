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

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing.ProviderInteractionLevel;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentLabel;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;

import javax.swing.*;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Implementation of {@link ALDDataIOSwing} interface for JFreeChart 
 * objects.
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=10)
public class MTBJFreeChartDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {
	
	/**
	 * Default constructor.
	 */
	public MTBJFreeChartDataIOSwing() {
		// nothing to do here...
	}

	/**
	 * Interface method to announce class for which IO is provided for
	 * 
	 * @return	Collection of classes provided
	 */
	@Override
  public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add( JFreeChart.class);
		return classes;
	}

	@Override
	public ALDSwingComponent createGUIElement(Field field,Class<?> cl, 
			Object obj, ALDParameterDescriptor descr) {
		return new ALDSwingComponentLabel(
				"input of JFreeChart objects not supported!");
	}

	@Override
  public void setValue(
  		Field f, Class<?> cl, ALDSwingComponent guiElem, Object value){
		return;
  }

  @Override
	@SuppressWarnings("unused")
	public Object readData(Field field, Class<?> cl, 
			ALDSwingComponent guiElem) 
		throws ALDDataIOProviderException {
		return null;
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) 
		throws ALDDataIOProviderException {
		if (!(obj instanceof JFreeChart))
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
				"[MTBJFreeChartDataIOSwing:writeData()] " 
				+ "Got object of wrong type...");
		
		// create panel with image title and button
		ChartShowPanel showPanel = new ChartShowPanel(
				new ChartShowButton(obj), 
				((JFreeChart)obj).getTitle().getText());

		// check if it is ok to open the image directly;
		// if not, just return a button to open it on request only
		if (   !(ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
						  	== ProviderInteractionLevel.ALL_FORBIDDEN)
				&& !(ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
							  == ProviderInteractionLevel.WARNINGS_ONLY)) {
			showPanel.getChartShowButton().popUpChart();
		}
		return showPanel;
	}
	
	/**
	 * Panel containing the button to display an image on demand.
	 * 
	 * @author moeller
	 */
	protected class ChartShowPanel extends JPanel { 
		
		/**
		 * Button associated with the panel.
		 */
		private ChartShowButton button;
		
		/**
		 * Constructor.
		 * @param _obj	Button to be associated with the panel.
		 * @param title Title of the button.
		 */
		protected ChartShowPanel(ChartShowButton _obj, String title) {
			this.button = _obj;
			this.add(new JLabel(" < " + title + " >   "));
			this.add(this.button);
		}

		/**
		 * Returns the button object associated with the panel.
		 * @return	Button associated with panel.
		 */
		public ChartShowButton getChartShowButton() {
			return this.button;
		}
	}

	/**
	 * Button to display a chart on demand.
	 * 
	 * @author moeller
	 */
	protected class ChartShowButton extends JButton 
		implements ActionListener {
		
		/**
		 * JFreeChart object associated with the button.
		 */
		private JFreeChart chart;
		
		/**
		 * Frame to display the chart.
		 */
		private JFrame chartFrame = null;

		/**
		 * Constructor.
		 * @param obj	Chart object to be associated with the button.
		 */
		protected ChartShowButton(Object obj) {
			super("Show Chart");
			this.chart = (JFreeChart)obj;
			this.setActionCommand("show");
			this.addActionListener(this);
		}
		
		@Override
    public void actionPerformed(ActionEvent ev) {
			String cmd = ev.getActionCommand();
			if (cmd.equals("show")) {
				this.popUpChart();
			}
		}
		
		/**
		 * Returns the chart object associated with the button.
		 * @return	Chart object shown on clicking the button.
		 */
		public Object getChartObject() {
			return this.chart;
		}
		
		/**
		 * Shows the chart to the user.
		 */
		public void popUpChart() {
			if (this.chartFrame != null)
				this.chartFrame.setVisible(true);
			else {
				// init the panel for the chart
				ChartPanel stackedBarChartPanel = new ChartPanel(this.chart);
		  	stackedBarChartPanel.setPreferredSize(
		  			new java.awt.Dimension(640, 400));
		  	// init the frame
				this.chartFrame = new JFrame(this.chart.getTitle().getText());
	      this.chartFrame.setLayout(new BorderLayout(0, 5));
	      this.chartFrame.add(stackedBarChartPanel, BorderLayout.CENTER);
	      stackedBarChartPanel.setMouseWheelEnabled(true);

	      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	      this.chartFrame.add(panel, BorderLayout.SOUTH);
	      this.chartFrame.pack();
	      this.chartFrame.setLocationRelativeTo(this.getParent());
	      this.chartFrame.setVisible(true);
			}
		}
	}
}
