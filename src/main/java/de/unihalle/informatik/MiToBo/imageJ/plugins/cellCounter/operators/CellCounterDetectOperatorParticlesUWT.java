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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.operators;

import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOpParameterDescriptor;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.apps.plantCells.plastids.PlastidDetector2DParticlesUWT;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarker;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShape;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapeRegion;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents.BorderType;

/**
 * Cell counter detector for detecting plastids.
 *  
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE)
@ALDDerivedClass
public class CellCounterDetectOperatorParticlesUWT 
	extends CellCounterDetectOperator {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[Particles with UWT] ";

	/**
	 * Minimal scale to consider.
	 */
	@Parameter( label= "Jmin", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Minimum scale index.")
	private Integer Jmin = new Integer(3);

	/**
	 * Maximum scale to consider.
	 */
	@Parameter( label= "Jmax", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 3, description = "Maximum scale index.")
	private Integer Jmax = new Integer(4);

	/**
	 * Size of scale interval for calculating wavelet correlation images.
	 */
	@Parameter( label= "Scale-interval size", required = true, 
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		dataIOOrder = 4, 
		description = "Size of scale interval for correlation images.")
	private Integer scaleIntervalSize = new Integer(1);
	
	/**
	 * Threshold for correlation images.
	 */
	@Parameter( label= "Correlation threshold", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 5, 
		description = "Threshold for wavelet correlation images.")
	private Double corrThreshold = new Double(1.5);

	/**
	 * Minimal size of valid regions.
	 */
	@Parameter( label= "Minimum region size", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 6, description = "Minimum area of detected regions.")
	private int minRegionSize = 1;
	
	/**
	 * Particle detector object.
	 */
	protected ParticleDetectorUWT2D particleOp;

	/**
	 * Configuration frame for particle detector.
	 */
//	protected OperatorConfigWin particleConfigureFrame;

	/**
	 * Constructor.	
	 * @throws ALDOperatorException Thrown in case of initialization error.
	 */
	public CellCounterDetectOperatorParticlesUWT() 
			throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		// configure the particle detector, except for the input image
		// which we do not know yet
	  this.particleOp = new ParticleDetectorUWT2D();
	  this.particleOp.setJmin(3);
	  this.particleOp.setJmax(4);
	  this.particleOp.setScaleIntervalSize(1);
	  this.particleOp.setMinRegionSize(1);
	  this.particleOp.setCorrelationThreshold(1.5);
//	  this.particleConfigureFrame =	new OperatorConfigWin(this.particleOp);
	}

	@Override
  protected void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		
		// post ImageJ status
		String msg = opIdentifier + "running plastid detection...";	
		this.notifyListeners(new StatusEvent(msg));

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "running plastid detection...");

		PlastidDetector2DParticlesUWT pd = new PlastidDetector2DParticlesUWT();
		pd.setInputImage(this.inputImage);		
		if (this.particleOp != null) {
			this.particleOp.addStatusListener(this);
			pd.setDetector(this.particleOp);
		}
		pd.runOp();
		MTBRegion2DSet resultPlastidRegions = pd.getPlastidRegions();
		int resultPlastidCount = resultPlastidRegions.size();

		// format results
		Vector<CellCntrMarker> markers = new Vector<>();
		
		// extract borders for all regions
		BordersOnLabeledComponents blc = new BordersOnLabeledComponents(null,
			resultPlastidRegions, BorderConnectivity.CONNECTED_8, 
				BorderType.OUT_IN_BORDERS, 1);
		blc.runOp(null);				
		MTBBorder2DSet borders = blc.getResultBorders();

		for (int i=0; i<resultPlastidRegions.size(); ++i) {
			MTBRegion2D reg = resultPlastidRegions.elementAt(i);
			MTBBorder2D bor = borders.elementAt(i);

			// calculate average intensity
			double intensity = 0; 
			for (Point2D.Double p: reg.getPoints()) {
				intensity += this.inputImage.getValueDouble((int)p.x, (int)p.y,	0);
			}
			CellCntrMarkerShape s = new CellCntrMarkerShapeRegion(reg, bor);
			s.setAvgIntensity(intensity/reg.getArea());
			CellCntrMarker marker = new CellCntrMarker(
				(int)reg.getCenterOfMass_X(), (int)reg.getCenterOfMass_Y(), 
					this.detectZSlice, s);
			markers.add(marker);
		}
		this.detectResults = markers; 
				
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + 
				"\t -> Number of detected plastids: " + resultPlastidCount);

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "Operations finished!");
		
		// post ImageJ status
		msg = opIdentifier + "Operations finished!";	
		this.notifyListeners(new StatusEvent(msg));
  }

	@Override
	public String getShortName() {
		return "Particles with UWT";
	}

	@Override
	public String getUniqueClassID() {
		return "ParticlesUWT";
	}

//	@Override
//	public void openConfigFrame() {
//		this.particleConfigureFrame.setVisible(true);
//	}
//	
//	@Override
//	public void closeConfigFrame() {
//		this.particleConfigureFrame.setVisible(false);
//	}


//	@Override
//	public void dispose() {
//		this.particleConfigureFrame.setVisible(false);
//	}
	
//	@Override
//	public void addValueChangeEventListener(
//	    ALDSwingValueChangeListener listener) {
//		// just hand the listener over to the configuration frame
//		this.particleConfigureFrame.addValueChangeEventListener(listener);
//	}

//	/**
//	 * Frame to configure a {@link ParticleDetectorUWT2D} operator in context of 
//	 * the MiToBo CellCounter.
//	 * 
//	 * @author Birgit Moeller
//	 */
//	protected class OperatorConfigWin 
//			extends CellCounterDetectOperatorConfigWin {
//
//		/** 
//		 * Constructs a control frame for an operator object.
//		 * @param _op Operator to be associated with this frame object.
//		 * @throws ALDOperatorException Thrown in case of failure.
//		 */
//		public OperatorConfigWin(ParticleDetectorUWT2D _op) 
//				throws ALDOperatorException {
//			super(_op);
//			this.titleString = "Configure Particle Detector Parameters...";		
//		}
//		
//		/**
//		 * Adds the input fields for all relevant parameters.
//		 */
//		@Override
//		protected void addParameterInputFields(JPanel parentPanel) {
//			try {
//				// JMin
//				JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
//				JLabel nameLabel = new JLabel("Minimal Scale ( JMin ): ");
//				nameLabel.setToolTipText("Smallest scale on which to detect particles," 
//					+ " must be >= 1.");
//				paramPanel.add(nameLabel);
//				ALDOpParameterDescriptor descr = this.op.getParameterDescriptor("Jmin");
//				Object value = this.op.getParameter("Jmin");
//				ALDSwingComponent aldElement = 
//					ALDDataIOManagerSwing.getInstance().createGUIElement(
//						descr.getField(),	descr.getMyclass(),	value, descr);
//				aldElement.addValueChangeEventListener(this);
//				this.guiElements.put("Jmin", aldElement);
//				paramPanel.add(aldElement.getJComponent());
//				parentPanel.add(paramPanel);
//
//				// JMax
//				paramPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
//				nameLabel = new JLabel("Maximal Scale ( JMax ): ");
//				nameLabel.setToolTipText("Largest scale on which to detect particles," 
//					+ " must be >= JMin.");
//				paramPanel.add(nameLabel);
//				descr = this.op.getParameterDescriptor("Jmax");
//				value = this.op.getParameter("Jmax");
//				aldElement = ALDDataIOManagerSwing.getInstance().createGUIElement(
//					descr.getField(),	descr.getMyclass(),	value, descr);
//				aldElement.addValueChangeEventListener(this);
//				this.guiElements.put("Jmax", aldElement);
//				paramPanel.add(aldElement.getJComponent());
//				parentPanel.add(paramPanel);
//
//				// scale interval size
//				paramPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
//				nameLabel = new JLabel("Scale Interval Size: ");
//				nameLabel.setToolTipText("Number of scales to consider for each " 
//					+ "correlation image, must be <= (JMax - JMin + 1).");
//				paramPanel.add(nameLabel);
//				descr = this.op.getParameterDescriptor("scaleIntervalSize");
//				value = this.op.getParameter("scaleIntervalSize");
//				aldElement = ALDDataIOManagerSwing.getInstance().createGUIElement(
//					descr.getField(),	descr.getMyclass(),	value, descr);
//				aldElement.addValueChangeEventListener(this);
//				this.guiElements.put("scaleIntervalSize", aldElement);
//				paramPanel.add(aldElement.getJComponent());
//				parentPanel.add(paramPanel);
//
//				// correlation threshold
//				paramPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
//				nameLabel = new JLabel("Correlation Threshold: ");
//				nameLabel.setToolTipText("Threshold for correlation images, the smaller "
//					+ "the more particles will be detected.");
//				paramPanel.add(nameLabel);
//				descr = this.op.getParameterDescriptor("corrThreshold");
//				value = this.op.getParameter("corrThreshold");
//				aldElement = ALDDataIOManagerSwing.getInstance().createGUIElement(
//					descr.getField(),	descr.getMyclass(),	value, descr);
//				aldElement.addValueChangeEventListener(this);
//				this.guiElements.put("corrThreshold", aldElement);
//				paramPanel.add(aldElement.getJComponent());
//				parentPanel.add(paramPanel);
//
//				// minimal region size
//				paramPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
//				nameLabel = new JLabel("Minimum Region Size: ");
//				nameLabel.setToolTipText("Regions smaller than given threshold on " 
//					+ "the size will be ignored.");
//				paramPanel.add(nameLabel);
//				descr = this.op.getParameterDescriptor("minRegionSize");
//				value = this.op.getParameter("minRegionSize");
//				aldElement = ALDDataIOManagerSwing.getInstance().createGUIElement(
//					descr.getField(),	descr.getMyclass(),	value, descr);
//				aldElement.addValueChangeEventListener(this);
//				this.guiElements.put("minRegionSize", aldElement);
//				paramPanel.add(aldElement.getJComponent());
//				parentPanel.add(paramPanel);
//			} catch (ALDException exp) {
//				exp.printStackTrace();
//			}
//		}
//	}

}
