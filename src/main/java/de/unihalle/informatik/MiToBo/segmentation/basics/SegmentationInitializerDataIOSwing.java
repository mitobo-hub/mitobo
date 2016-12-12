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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeReporter;
import de.unihalle.informatik.Alida.exceptions.*;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer.InputMode;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer.SegmentationDimension;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer.ShapeType;

import javax.swing.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Class for loading/saving objects of type 
 * {@link SegmentationInitializer}.
 * <p>
 * If no concrete class is requested, the type of the returned data 
 * object depends on the input data (e.g. 3D data yields an object of 
 * type {@link MTBSegmentation3D} to be returned, 2D data results in an 
 * object of type {@link MTBSegmentation3D}). <br>
 * If a concrete class is given an object of a corresponding class is 
 * returned, if available. In case that the dimensions of input data and 
 * desired class are not consistent, the input data is automatically 
 * converted if possible. This might result in a loss of data. 
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=10)
public class SegmentationInitializerDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	/**
	 * Interface method to announce class for which IO is provided for.
	 * @return  Collection of classes provided.
	 */
	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add(SegmentationInitializer.class);
		return classes;
	}
	
	/** 
	 * Generic reading of segmentation objects.
	 */
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.helpers.ALDDataIOSwing#createGUIElement(java.lang.Class, java.lang.Object)
	 */
	@Override
	public ALDSwingComponent createGUIElement(Field field, Class<?> cl,
			Object obj, ALDParameterDescriptor descr) 
		throws ALDDataIOProviderException {
		try {
			return new ConfigureSegmentationButton(field, cl, obj, descr);
		} catch(ALDDataIOException ex) {
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
				"[SegmentationInitializerDataIOSwing] could not init button..."
					+ ex.getCommentString());
		}
	}

	@Override
  public void setValue(Field field, Class<?> cl, 
  		ALDSwingComponent guiElem, Object value) 
  	throws ALDDataIOProviderException {
		if (!(guiElem instanceof ConfigureSegmentationButton)) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
						"SegmentationInitializerDataIO: " + 
								"setValue received invalid GUI element!");
		}
		try {
	    ((ConfigureSegmentationButton)guiElem).setValue(value);
    } catch (ALDDataIOException e) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
						"SegmentationInitializerDataIO: " + 
								"setValue() failed!\n" + e.getCommentString());
    }
  }

	@Override
	public Object readData(Field field, Class<?> cl,
			ALDSwingComponent guiElem) 
		throws ALDDataIOProviderException {
		if (!(guiElem instanceof ConfigureSegmentationButton)) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
						"SegmentationInitializerDataIO: " + 
								"readData received invalid GUI element!");
		}
		try {
	    return ((ConfigureSegmentationButton)guiElem).readData(field, cl);
    } catch (ALDDataIOException e) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
					"SegmentationInitializerDataIO: " + 
							"error on reading data!\n"+e.getCommentString());
    } catch (ALDOperatorException e) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
					"SegmentationInitializerDataIO: " + 
							"error on reading data!\n"+e.getCommentString());
    }
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {
		return new JLabel("The output of objects of type \n" + 
				"<SegmentationInitializer> is not yet supported!");
	}

	/**
	 * GUI element for configuring segmentations.
	 * <p>
	 * This button has a configuration window attached to it where data to 
	 * initialize a segmentation object can be specified.
	 *  
	 * @author moeller
	 */
	protected class ConfigureSegmentationButton extends ALDSwingComponent 
		implements ActionListener, ALDSwingValueChangeListener {

		/**
		 * Configuration button.
		 */
		protected JButton configButton;
 		
		/**
		 * Configuration window.
		 */
		protected SegmentationConfigWin confWin;

		/**
		 * Constructor.
		 * @param field	Field of associated parameter.
		 * @param cl		Class of associated parameter.
		 * @param obj		Initial value.
		 * @param descr	Associated parameter descriptor.
		 * @throws ALDDataIOException	Thrown in case of failure.
		 */
		protected ConfigureSegmentationButton(
				@SuppressWarnings("unused") Field field, Class<?> cl, 
				@SuppressWarnings("unused") Object obj, 
				ALDParameterDescriptor descr)
			throws ALDDataIOException {
			this.confWin = new SegmentationConfigWin(null, cl, null, descr);
			this.confWin.addValueChangeEventListener(this);
			this.configButton = 
					new JButton("Configure initial segmentation...");
			this.configButton.setActionCommand("configButtonPressed");
			this.configButton.addActionListener(this.confWin);
		}
		
		/**
		 * Gets the data from the configuration window.
		 * 
		 * @param field	Field of collection.
		 * @param cl	Class of collection.
		 * @return Current data.
		 * @throws ALDDataIOException 
		 * @throws ALDOperatorException 
		 */
		protected Object readData(Field field, Class<?> cl) 
				throws ALDDataIOException, ALDOperatorException {
			return this.confWin.readData(field, cl);
		}
		
		/**
		 * Set new values.
		 * @param value		New object data.
		 * @throws ALDDataIOException 
		 */
		protected void setValue(Object value) throws ALDDataIOException {
			this.confWin.setValue(value);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand(); 
			if (cmd.equals("configButtonPressed")) {
				this.confWin.window.setVisible(true);
			}	
		}	

		@Override
	  public JComponent getJComponent() {
		  return this.configButton;
	  }

		@Override
	  public void disableComponent() {
			this.configButton.setEnabled(false);
			this.confWin.disableComponent();
	  }

		@Override
	  public void enableComponent() {
			this.configButton.setEnabled(true);
			this.confWin.enableComponent();
	  }

		@Override
	  public void dispose() {
			this.confWin.dispose();
	  }

		@Override
    public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
    }
	}	
	
	/**
	 * Segmentation configuration window.
	 * @author moeller
	 */
	protected class SegmentationConfigWin 
		extends ALDSwingValueChangeReporter
		implements ActionListener, ALDSwingValueChangeListener {

		/**
		 * Main frame.
		 */
		protected JFrame window;

		/**
		 * Fixed width of window.
		 */
		private final int frameWidth = 800;
		
		/**
		 * Fixed height of window.
		 */
		private final int frameHeight = 400;

		/**
		 * Main panel of main frame.
		 */
		private JPanel mainPanel = null;
		
		/**
		 * Button to close the configuration window.
		 */
		private JButton closeButton;
		/**
		 * Button to show currently configured segmentation.
		 */
		private JButton showButton;
		/**
		 * Button to select binary mask mode.
		 */
		private JRadioButton modeSelectMask;
		/**
		 * Button to select label image mode.
		 */
		private JRadioButton modeSelectLabelImg;
		/**
		 * Button to select region set mode.
		 */
		private JRadioButton modeSelectRegions;
		/**
		 * Button to select image thresholding mode.
		 */
		private JRadioButton modeSelectThreshold;
		/**
		 * Button to select shape mode.
		 */
		private JRadioButton modeSelectShape;
		
		/**
		 * I/O component to read the target dimension.
		 */
		private ALDSwingComponent targetDimensionIO;
		/**
		 * I/O component to read a label image.
		 */
		private ALDSwingComponent labelIO;
		/**
		 * I/O component to read a binary image.
		 */
		private ALDSwingComponent maskIO;
		/**
		 * I/O component to read a region set.
		 */
		private ALDSwingComponent regionIO;
		/**
		 * I/O component to configure binary mode for region sets.
		 */
		private ALDSwingComponent regionIObin;
		/**
		 * I/O component to read an image for thresholding.
		 */
		private ALDSwingComponent thresholdIOImage;
		/**
		 * I/O component to read a threshold.
		 */
		private ALDSwingComponent thresholdIOThresh;
		/**
		 * I/O component to select shape.
		 */
		private ALDSwingComponent shapeIOShape; 
		/**
		 * I/O component to set x dimension of shape mask.
		 */
		private ALDSwingComponent shapeIOxDim; 
		/**
		 * I/O component to set y dimension of shape mask.
		 */
		private ALDSwingComponent shapeIOyDim; 
		/**
		 * I/O component to set z dimension of shape mask.
		 */
		private ALDSwingComponent shapeIOzDim; 
		/**
		 * I/O component to set x position of shape.
		 */
		private ALDSwingComponent shapeIOxPos; 
		/**
		 * I/O component to set y position of shape.
		 */
		private ALDSwingComponent shapeIOyPos; 
		/**
		 * I/O component to set z position of shape.
		 */
		private ALDSwingComponent shapeIOzPos;
		/**
		 * I/O component to set size of shape in x.
		 */
		private ALDSwingComponent shapeIOxSize; 
		/**
		 * I/O component to set size of shape in y.
		 */
		private ALDSwingComponent shapeIOySize; 
		/**
		 * I/O component to set size of shape in z.
		 */
		private ALDSwingComponent shapeIOzSize;

		/**
		 * Default constructor.
		 * @param field		Field to specify input data objects.
		 * @param cl			Class of collection elements.
		 * @param obj			Initial value of collection.
		 * @param descr		Optional descriptor for additional information.
		 */
		@SuppressWarnings("unused")
    protected SegmentationConfigWin(Field field, Class<?> cl, Object obj,	
				ALDParameterDescriptor descr) 
				throws ALDDataIOException {

			// initialize the window
			this.window = new JFrame();
			String title = "unknown";
			if (descr != null) 
				title = descr.getLabel();
			this.window.setTitle("Configure initial segmentation for operator <" 
					+title+ "> ");
			this.window.setSize(this.frameWidth,this.frameHeight);

			this.mainPanel = new JPanel();
			BoxLayout bl = new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS);
			this.mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			this.mainPanel.setLayout(bl);
			
			JPanel dimPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			this.targetDimensionIO =	
					ALDDataIOManagerSwing.getInstance().createGUIElement(
							null, SegmentationDimension.class, null, null);
			this.targetDimensionIO.addValueChangeEventListener(this);
			dimPanel.add(new JLabel("Target Dimension:   "));
			dimPanel.add(this.targetDimensionIO.getJComponent());
			this.mainPanel.add(dimPanel);

			// init radio buttons for mode selection
			this.modeSelectMask = new JRadioButton("Binary Mask");
			this.modeSelectMask.setActionCommand("modeMask");
			this.modeSelectMask.addActionListener(this);
			this.modeSelectLabelImg = new JRadioButton("Label Image");
			this.modeSelectLabelImg.setActionCommand("modeLabel");
			this.modeSelectLabelImg.addActionListener(this);
			this.modeSelectRegions = new JRadioButton("Region Set");
			this.modeSelectRegions.setActionCommand("modeRegion");
			this.modeSelectRegions.addActionListener(this);
			this.modeSelectThreshold = new JRadioButton("Threshold Image");
			this.modeSelectThreshold.setActionCommand("modeThreshold");
			this.modeSelectThreshold.addActionListener(this);
			this.modeSelectShape = new JRadioButton("Synthetic Shape");
			this.modeSelectShape.setActionCommand("modeShape");
			this.modeSelectShape.addActionListener(this);
			
			ButtonGroup modeSelectButtons = new ButtonGroup();
			modeSelectButtons.add(this.modeSelectMask);
			modeSelectButtons.add(this.modeSelectLabelImg);
			modeSelectButtons.add(this.modeSelectRegions);
			modeSelectButtons.add(this.modeSelectThreshold);
			modeSelectButtons.add(this.modeSelectShape);
			
			JPanel radioButtonPanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JLabel modeLabel = new JLabel("Input mode:");
			radioButtonPanel.add(modeLabel);
			radioButtonPanel.add(this.modeSelectMask);
			radioButtonPanel.add(this.modeSelectLabelImg);
			radioButtonPanel.add(this.modeSelectRegions);
			radioButtonPanel.add(this.modeSelectThreshold);
			radioButtonPanel.add(this.modeSelectShape);
			this.mainPanel.add(radioButtonPanel);
			
			JSeparator modeSep = new JSeparator(SwingConstants.HORIZONTAL);
			int width = (int)(this.frameWidth - this.frameWidth/10.0);
			modeSep.setPreferredSize(new Dimension(width, 1));
			this.mainPanel.add(modeSep);

			// panels and components to read data
			JPanel maskPanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JPanel labelPanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JPanel regionPanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JPanel thresholdPanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JPanel shapePanel = 
					new JPanel(new FlowLayout(FlowLayout.LEADING));
			JComponent maskComponent;
			JComponent labelComponent;
			JComponent regionComponent;
			JComponent thresholdComponent;
			JComponent shapeComponent;

			this.maskIO =	ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, MTBImage.class, null, descr);
			this.maskIO.addValueChangeEventListener(this);
			maskComponent = this.maskIO.getJComponent();
			maskPanel.add(new JLabel("Binary Mask:   "));
			maskPanel.add(maskComponent);
			this.labelIO= ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, MTBImage.class, null, descr);
			this.labelIO.addValueChangeEventListener(this);
			labelComponent = this.labelIO.getJComponent();
			labelPanel.add(new JLabel("Label Image:   "));
			labelPanel.add(labelComponent);
			this.regionIO= ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, MTBRegion2DSet.class, null, descr);
			this.regionIO.addValueChangeEventListener(this);
			regionComponent = this.regionIO.getJComponent();
			regionPanel.add(new JLabel("Region Set:   "));
			regionPanel.add(regionComponent);
			this.regionIObin= ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, boolean.class, null, descr);
			this.regionIObin.addValueChangeEventListener(this);
			JLabel regionBinLabel = new JLabel("  Binary Segmentation?");
			regionBinLabel.setToolTipText(
					"If selected a binary segmentation is initialized.");
			regionPanel.add(regionBinLabel);
			regionPanel.add(this.regionIObin.getJComponent());
			this.thresholdIOThresh= 
					ALDDataIOManagerSwing.getInstance().createGUIElement(
							null, Double.class, null, descr);
			this.thresholdIOThresh.addValueChangeEventListener(this);
			thresholdComponent = this.thresholdIOThresh.getJComponent();
			thresholdPanel.add(new JLabel("Threshold:   "));
			thresholdPanel.add(thresholdComponent);
			((JTextField)thresholdComponent).setColumns(10);
			thresholdPanel.add(new JLabel("     Image:   "));
			this.thresholdIOImage = 
					ALDDataIOManagerSwing.getInstance().createGUIElement(
							null, MTBImage.class, null, descr);
			this.thresholdIOImage.addValueChangeEventListener(this);
			thresholdPanel.add(this.thresholdIOImage.getJComponent());
			this.shapeIOShape = ALDDataIOManagerSwing.getInstance().createGUIElement(
							null, ShapeType.class, null, descr);
			this.shapeIOShape.addValueChangeEventListener(this);
			shapeComponent = this.shapeIOShape.getJComponent();
			shapePanel.add(new JLabel("Shape configuration:"));
			shapePanel.add(shapeComponent);
			JLabel labelTargetSize = 
					new JLabel("     Target mask size:    x = ");
			labelTargetSize.setToolTipText("... should fit size of input image!");
			shapePanel.add(labelTargetSize);
			this.shapeIOxDim =	ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, int.class, null, descr);
			this.shapeIOxDim.addValueChangeEventListener(this);
			shapeComponent = this.shapeIOxDim.getJComponent();
			((JTextField)shapeComponent).setColumns(6);
			shapePanel.add(shapeComponent);
			shapePanel.add(new JLabel("   y = "));
			this.shapeIOyDim =	ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, int.class, null, descr);
			this.shapeIOyDim.addValueChangeEventListener(this);
			shapeComponent = this.shapeIOyDim.getJComponent();
			((JTextField)shapeComponent).setColumns(6);
			shapePanel.add(shapeComponent);
			shapePanel.add(new JLabel("   z = "));
			this.shapeIOzDim =	ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, int.class, null, descr);
			this.shapeIOzDim.addValueChangeEventListener(this);
			shapeComponent = this.shapeIOzDim.getJComponent();
			((JTextField)shapeComponent).setColumns(6);
			shapePanel.add(shapeComponent);
			JPanel shapeSpecPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			shapeSpecPanel.add(new JLabel("X-Pos:"));
			this.shapeIOxPos = ALDDataIOManagerSwing.getInstance().createGUIElement(
							null, Double.class, null, descr);
			this.shapeIOxPos.addValueChangeEventListener(this);
			JComponent doubleComp = this.shapeIOxPos.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);
			shapeSpecPanel.add(new JLabel("Y-Pos:"));
			this.shapeIOyPos = ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, Double.class, null, descr);
			this.shapeIOyPos.addValueChangeEventListener(this);
			doubleComp = this.shapeIOyPos.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);
			shapeSpecPanel.add(new JLabel("Z-Pos:"));
			this.shapeIOzPos = ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, Double.class, null, descr);
			this.shapeIOzPos.addValueChangeEventListener(this);
			doubleComp = this.shapeIOzPos.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);
			shapeSpecPanel.add(new JLabel("X-Size:"));
			this.shapeIOxSize = ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, Double.class, null, descr);
			this.shapeIOxSize.addValueChangeEventListener(this);
			doubleComp = this.shapeIOxSize.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);
			shapeSpecPanel.add(new JLabel("Y-Size:"));
			this.shapeIOySize = ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, Double.class, null, descr);
			this.shapeIOySize.addValueChangeEventListener(this);
			doubleComp = this.shapeIOySize.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);
			shapeSpecPanel.add(new JLabel("Z-Size:"));
			this.shapeIOzSize = ALDDataIOManagerSwing.getInstance().createGUIElement(
					null, Double.class, null, descr);
			this.shapeIOzSize.addValueChangeEventListener(this);
			doubleComp = this.shapeIOzSize.getJComponent();
			((JTextField)doubleComp).setColumns(6);
			shapeSpecPanel.add(doubleComp);

			this.mainPanel.add(maskPanel);
			this.mainPanel.add(labelPanel);
			this.mainPanel.add(regionPanel);
			this.mainPanel.add(thresholdPanel);
			this.mainPanel.add(shapePanel);
			this.mainPanel.add(shapeSpecPanel);

			// show button
			this.showButton = new JButton("Show Segmentation");
			this.showButton.setActionCommand("show");
			this.showButton.addActionListener(this);

			// close button
			this.closeButton = new JButton("Close");
			this.closeButton.setActionCommand("close");
			this.closeButton.addActionListener(this);

			JPanel buttonPanel = new JPanel();
			buttonPanel.add(this.showButton);
			buttonPanel.add(this.closeButton);
			this.mainPanel.add(buttonPanel);

			// default mode
			this.modeSelectMask.setSelected(false);
			this.modeSelectLabelImg.setSelected(false);
			this.modeSelectRegions.setSelected(false);
			this.modeSelectThreshold.setSelected(false);
			this.modeSelectShape.setSelected(false);
			this.maskIO.disableComponent();
			this.labelIO.disableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();

			this.window.add(this.mainPanel);
		}
		
		/**
		 * Extracts current data.
		 * 
		 * @param field		Field.
		 * @param cl			Class.
		 * @return	Current initializer.
		 * @throws ALDDataIOException 
		 * @throws ALDOperatorException 
		 */
		@SuppressWarnings("unused")
    protected SegmentationInitializer readData(Field field, Class<?> cl) 
				throws ALDDataIOException, ALDOperatorException {

			if (this.getCurrentMode() == null)
				return null;
			
			SegmentationInitializer initOp = new SegmentationInitializer();
			
			SegmentationDimension targetDim = 
				(SegmentationDimension)ALDDataIOManagerSwing.getInstance().readData(
						field, SegmentationDimension.class, this.targetDimensionIO);
			initOp.setSegDim(targetDim);
			
			// read data according to selected mode
			switch(this.getCurrentMode())
			{
			case MODE_LABEL_IMAGE:
				MTBImage labelImg = 
					(MTBImage)ALDDataIOManagerSwing.getInstance().readData(
						field, MTBImage.class, this.labelIO);
				initOp.setLabelImage(labelImg);
				initOp.setInputMode(InputMode.MODE_LABEL_IMAGE);
				break;
			case MODE_BINARY_IMAGE:
				MTBImage binImg = 
					(MTBImage)ALDDataIOManagerSwing.getInstance().readData(
						field, MTBImage.class, this.maskIO);
				initOp.setBinaryImage(binImg);
				initOp.setInputMode(InputMode.MODE_BINARY_IMAGE);
				break;
			case MODE_REGION_SET:
				MTBRegion2DSet regions = 
					(MTBRegion2DSet)ALDDataIOManagerSwing.getInstance().readData(
						field, MTBRegion2DSet.class, this.regionIO);
				initOp.setRegionSet(regions);
				Boolean binary= (Boolean)ALDDataIOManagerSwing.getInstance().readData(
						null, boolean.class, this.regionIObin);
				initOp.setRegionBinaryMode(binary.booleanValue());
				initOp.setInputMode(InputMode.MODE_REGION_SET);
				break;
			case MODE_THRESHOLD:
				MTBImage thresholdImg = 
	 				(MTBImage)ALDDataIOManagerSwing.getInstance().readData(
						field, MTBImage.class, this.thresholdIOImage);
				initOp.setThresholdImage(thresholdImg);
				Double value = 
						(Double)ALDDataIOManagerSwing.getInstance().readData(
								field, Double.class, this.thresholdIOThresh);
				initOp.setThreshold(value.doubleValue());
				initOp.setInputMode(InputMode.MODE_THRESHOLD);
				break;
			case MODE_SHAPE:
				ShapeType shapeType = 
					(ShapeType)ALDDataIOManagerSwing.getInstance().readData(
							field, ShapeType.class, this.shapeIOShape);
				Integer xDim = 
					(Integer)ALDDataIOManagerSwing.getInstance().readData(
							field, Integer.class, this.shapeIOxDim);
				Integer yDim =
					(Integer)ALDDataIOManagerSwing.getInstance().readData(
							field, Integer.class, this.shapeIOyDim);
				Integer zDim =
					(Integer)ALDDataIOManagerSwing.getInstance().readData(
							field, Integer.class, this.shapeIOzDim);
				Double xPos = 
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOxPos);
				Double yPos =
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOyPos);
				Double zPos =
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOzPos);
				Double xSize = 
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOxSize);
				Double ySize =
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOySize);
				Double zSize =
					(Double)ALDDataIOManagerSwing.getInstance().readData(
							field, Double.class, this.shapeIOzSize);
				// transfer the data
				initOp.setShape(shapeType);
				initOp.setShapeMaskSizeX(xDim.intValue());
				initOp.setShapeMaskSizeY(yDim.intValue());
				initOp.setShapeMaskSizeZ(zDim.intValue());
				initOp.setShapePosX(xPos.doubleValue());
				initOp.setShapePosY(yPos.doubleValue());
				initOp.setShapePosZ(zPos.doubleValue());
				initOp.setShapeDimX(xSize.doubleValue());
				initOp.setShapeDimY(ySize.doubleValue());
				initOp.setShapeDimZ(zSize.doubleValue());
				initOp.setInputMode(InputMode.MODE_SHAPE);
				break;
			}
			return initOp;
		}

		/**
		 * Extracts currently selected mode, i.e. data source.
		 * @return	Current mode.
		 */
		protected InputMode getCurrentMode() {
			if (this.modeSelectLabelImg.isSelected())
				return InputMode.MODE_LABEL_IMAGE;
			if (this.modeSelectMask.isSelected())
				return InputMode.MODE_BINARY_IMAGE;
			if (this.modeSelectRegions.isSelected())
				return InputMode.MODE_REGION_SET;
			if (this.modeSelectThreshold.isSelected())
				return InputMode.MODE_THRESHOLD;
			if (this.modeSelectShape.isSelected())
				return InputMode.MODE_SHAPE;
			// happens only once directly after initialization
			return null;
		}

		/**
		 * Updates current initializer configuration.
		 * @param value	New value.
		 * @throws ALDDataIOException 
		 */
		protected void setValue(Object value) throws ALDDataIOException {
			
			// check if object type is correct
			if (!(value instanceof SegmentationInitializer))
				return;
			
			SegmentationInitializer initOp = (SegmentationInitializer)value;
			ALDDataIOManagerSwing.getInstance().setValue(null, 
					SegmentationDimension.class, this.targetDimensionIO, 
					initOp.getSegDim());
			ALDDataIOManagerSwing.getInstance().setValue(null,
					MTBImage.class, this.labelIO, initOp.getLabelImage());
			ALDDataIOManagerSwing.getInstance().setValue(null,
					MTBImage.class, this.maskIO, initOp.getBinaryImage());
			ALDDataIOManagerSwing.getInstance().setValue(null,
					MTBRegion2DSet.class, this.regionIO, initOp.getRegionSet());
			ALDDataIOManagerSwing.getInstance().setValue(null,
					boolean.class, this.regionIObin, 
					new Boolean(initOp.isRegionBinaryMode()));
			ALDDataIOManagerSwing.getInstance().setValue(null,
					MTBImage.class, this.thresholdIOImage, initOp.getThresholdImage());
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class, 
					this.thresholdIOThresh, new Double(initOp.getThreshold()));
			ALDDataIOManagerSwing.getInstance().setValue(null,
					ShapeType.class, this.shapeIOShape, initOp.getShape());
			ALDDataIOManagerSwing.getInstance().setValue(null, Integer.class, 
					this.shapeIOxDim, new Integer(initOp.getShapeMaskSizeX()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Integer.class, 
					this.shapeIOyDim, new Integer(initOp.getShapeMaskSizeY()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Integer.class, 
					this.shapeIOzDim, new Integer(initOp.getShapeMaskSizeZ()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOxPos, new Double(initOp.getShapePosX()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOyPos, new Double(initOp.getShapePosY()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOzPos, new Double(initOp.getShapePosZ()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOxSize, new Double(initOp.getShapeDimX()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOySize, new Double(initOp.getShapeDimY()));
			ALDDataIOManagerSwing.getInstance().setValue(null, Double.class,
					this.shapeIOzSize, new Double(initOp.getShapeDimZ()));
			
			// update GUI
			switch(initOp.getInputMode()) 
			{
			case MODE_BINARY_IMAGE:
				this.modeSelectMask.setSelected(true);
				break;
			case MODE_LABEL_IMAGE:
				this.modeSelectLabelImg.setSelected(true);
				break;
			case MODE_REGION_SET:
				this.modeSelectRegions.setSelected(true);
				break;
			case MODE_THRESHOLD:
				this.modeSelectThreshold.setSelected(true);
				break;
			case MODE_SHAPE:
				this.modeSelectShape.setSelected(true);
				break;
			}
		}

		/**
		 * Deactivates the configuration window to prohibit value changes.
		 */
		protected void disableComponent() {
			this.maskIO.disableComponent();
			this.labelIO.disableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();
		}
		
		/**
		 * Reactivates the configuration window to allow for value changes.
		 */
		protected void enableComponent() {
			if (this.modeSelectMask.isSelected()) {
				this.configGUIModeMask();
			}
			else if (this.modeSelectLabelImg.isSelected()) {
				this.configGUIModeLabel();
			}
			else if (this.modeSelectRegions.isSelected()) {
				this.configGUIModeRegion();
			}
			else if (this.modeSelectThreshold.isSelected()) {
				this.configGUIModeThreshold();
			}
			else if (this.modeSelectShape.isSelected()) {
				this.configGUIModeShape();
			}
		}
		
		/**
		 * Disposes this window and all sub-components.
		 */
		protected void dispose() {
			this.window.dispose();
		}
		
		/**
		 * Enable GUI components for mask mode.
		 */
		private void configGUIModeMask() {
			this.maskIO.enableComponent();
			this.labelIO.disableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();					
		}
		
		/**
		 * Enable GUI components for label mode.
		 */
		private void configGUIModeLabel() {
			this.maskIO.disableComponent();
			this.labelIO.enableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();				
		}

		/**
		 * Enable GUI components for region mode.
		 */
		private void configGUIModeRegion() {
			this.maskIO.disableComponent();
			this.labelIO.disableComponent();
			this.regionIO.enableComponent();
			this.regionIObin.enableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();
		}

		/**
		 * Enable GUI components for threshold mode.
		 */
		private void configGUIModeThreshold() {
			this.maskIO.disableComponent();
			this.labelIO.disableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.enableComponent();
			this.thresholdIOThresh.enableComponent();
			this.shapeIOShape.disableComponent(); 
			this.shapeIOxDim.disableComponent(); 
			this.shapeIOyDim.disableComponent(); 
			this.shapeIOzDim.disableComponent(); 
			this.shapeIOxPos.disableComponent(); 
			this.shapeIOyPos.disableComponent(); 
			this.shapeIOzPos.disableComponent();
			this.shapeIOxSize.disableComponent(); 
			this.shapeIOySize.disableComponent(); 
			this.shapeIOzSize.disableComponent();
		}

		/**
		 * Enable GUI components for shape mode.
		 */
		private void configGUIModeShape() {
			this.maskIO.disableComponent();
			this.labelIO.disableComponent();
			this.regionIO.disableComponent();
			this.regionIObin.disableComponent();
			this.thresholdIOImage.disableComponent();
			this.thresholdIOThresh.disableComponent();
			this.shapeIOShape.enableComponent(); 
			this.shapeIOxDim.enableComponent(); 
			this.shapeIOyDim.enableComponent(); 
			this.shapeIOzDim.enableComponent(); 
			this.shapeIOxPos.enableComponent(); 
			this.shapeIOyPos.enableComponent(); 
			this.shapeIOzPos.enableComponent();
			this.shapeIOxSize.enableComponent(); 
			this.shapeIOySize.enableComponent(); 
			this.shapeIOzSize.enableComponent();
		}

		@Override
    public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand(); 
			if (cmd.equals("configButtonPressed")) {
				this.window.setVisible(true);
				if (this.modeSelectMask.isSelected()) {
					this.configGUIModeMask();
				}
				else if (this.modeSelectLabelImg.isSelected()) {
					this.configGUIModeLabel();
				}
				else if (this.modeSelectRegions.isSelected()) {
					this.configGUIModeRegion();
				}
				else if (this.modeSelectThreshold.isSelected()) {
					this.configGUIModeThreshold();
				}
				else if (this.modeSelectShape.isSelected()) {
					this.configGUIModeShape();
				}
			}
			else if (cmd.equals("close")) {
				this.window.setVisible(false);
			}
			else if (cmd.equals("modeMask")) {
				this.configGUIModeMask();
			}
			else if (cmd.equals("modeLabel")) {
				this.configGUIModeLabel();
			}
			else if (cmd.equals("modeRegion")) {
				this.configGUIModeRegion();
			}
			else if (cmd.equals("modeThreshold")) {
				this.configGUIModeThreshold();
			}
			else if (cmd.equals("modeShape")) {
				this.configGUIModeShape();
			}
			else if (cmd.equals("show")) {
				try {
					SegmentationInitializer currentOp =	this.readData(null, null);
					currentOp.runOp();
					MTBSegmentationInterface currentSeg = currentOp.getSegmentation();
					if (currentSeg instanceof MTBSegmentation2D) {
						MTBSegmentation2D seg2D = (MTBSegmentation2D)currentSeg;
						MTBImage segVisualization = seg2D.getLabelImage();
						segVisualization.show();
					}
					else {
						JOptionPane.showMessageDialog(this.window, 
							"Cannot display 3D segmentation data, sorry!");						
					}
				} catch (ALDException ex) {
					JOptionPane.showMessageDialog(this.window, 
							"Cannot visualize segmentation!\n" + ex.getCommentString());
					ex.printStackTrace();
				}
			}
		}

		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}		
	}
}
