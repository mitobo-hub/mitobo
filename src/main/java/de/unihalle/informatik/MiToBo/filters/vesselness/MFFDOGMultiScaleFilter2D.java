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

package de.unihalle.informatik.MiToBo.filters.vesselness;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.vesselness.MFFDOGFilter2D.VesselMode;

/**
 * MF-FDOG multi-scale filter for vessel segmentation.
 * <p>
 * This operator applies the {@link MFFDOGFilter2D} on multiple scales.
 * The results are then combined using a logical OR operation.<br>
 * Related publication:
 * <p>
 * B. Zhang, L. Zhang, L. Zhang and F. Karray, 
 * "Retinal vessel extraction by matched filter with first-order derivative
 *  of Gaussian",<br>Comp. in Biology and Medicine, vol. 40 (2010), pp. 438-445  
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.APPLICATION)
public class MFFDOGMultiScaleFilter2D extends MTBOperator 
	implements StatusReporter {

	/** 
	 * Vector of installed StatusListeners.
	 */
	protected transient Vector<StatusListener> statusListeners;

	/**
	 * Input image to be processed.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = -10,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.")
	protected transient MTBImage inputImg = null;

	/**
	 * Scenario for detecting vessels.
	 */
	@Parameter( label= "Scenario", required = false, dataIOOrder = 1,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Relation of vessels to background.")
	protected VesselMode mode = VesselMode.DARK_ON_BRIGHT_BACKGROUND;
	
	/**
	 * Table with vessel widths and lengths to apply.
	 * <p>
	 * The operator expects an array with two columns. The first column refers 
	 * to the widths to be applied, the second column refers to related vessel
	 * length. Note that both columns need to have the same length.
	 * <p>
	 * The widths determine the standard deviation of the Gaussian kernel,
	 * i.e. sigma = w / 2.0.
	 */
	@Parameter( label= "Widths and Lengths", required = true, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Widths and lengths to apply.")
	protected Double[][] widthLengthTab = 
		new Double[][]{ {new Double(4.0), new Double(9.0)} };

	/**
	 * Sampling step size for orientations.
	 */
	@Parameter( label= "Angular Sampling Steps", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Angular sampling step size (in degrees).", dataIOOrder = 3)	
	protected int angleSampling = 15;
	
	/**
	 * Resulting binary vessel map.
	 */
	@Parameter( label= "Result Map", dataIOOrder = 0, 
		direction=Parameter.Direction.OUT, description = "Resulting vessel map.")
	protected transient MTBImageByte resultVesselMap = null;

	/**
	 * Result stack with binary vessel maps for different scales and lengths.
	 */
	@Parameter( label= "Filter Response Stack", dataIOOrder = 1, 
		direction=Parameter.Direction.OUT, description = "Filter response stack.")
	private transient MTBImageByte responseStack = null;

	/**
	 * Default constructor.
	 */
	public MFFDOGMultiScaleFilter2D() throws ALDOperatorException {
		super();
		this.statusListeners = new Vector<StatusListener>(1);
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.core.operator.MTBOperator#readResolve()
	 */
	@Override
	protected Object readResolve() {
		super.readResolve();
		this.statusListeners = new Vector<StatusListener>(1);
		this.widthLengthTab = new Double[][]{{new Double(4.0),new Double(9.0)}};
		return this;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
  public void validateCustom() throws ALDOperatorException {
		for (int i = 0; i < this.widthLengthTab.length; ++i)
			if (this.widthLengthTab[i].length != 2)
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[MFFDOGMultiScaleFilter2D] scale/length table has wrong dimensions,"
					+ " should be N x 2!");
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		int width = this.inputImg.getSizeX();
		int height = this.inputImg.getSizeY();
		
		// configure MF-FDOG filter
		MFFDOGFilter2D mffdogFilter = new MFFDOGFilter2D();
		mffdogFilter.setInputImage(this.inputImg);
		mffdogFilter.setVesselMode(this.mode);
		mffdogFilter.setSampling(this.angleSampling);
		for (StatusListener l : this.statusListeners)
			mffdogFilter.addStatusListener(l);	
		
		// run the filter for the different scales
		int scaleCount = this.widthLengthTab.length;
		MTBImageByte [] vesselMaps = new MTBImageByte[scaleCount];
		for (int s = 0; s < scaleCount; ++s) {
			mffdogFilter.setWidth(this.widthLengthTab[s][0]);
			mffdogFilter.setLength(
				new Integer(this.widthLengthTab[s][1].intValue()));
			mffdogFilter.runOp();
			vesselMaps[s] = mffdogFilter.getResultVesselMap();
		}
		
		// logical OR of all maps
		this.resultVesselMap = (MTBImageByte)MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.resultVesselMap.fillBlack();
		for (int s = 0; s < scaleCount; ++s) {
			for (int y=0; y<height; ++y) {
				for (int x=0; x<width; ++x) {
					if (vesselMaps[s].getValueInt(x, y) > 0)
						this.resultVesselMap.putValueInt(x, y, 255);
				}
			}
		}
		this.resultVesselMap.setTitle("Result of MFFDOGMultiScale Filter for " 
				+ "<" + this.inputImg.getTitle() + ">");
		
		// create output image stack
		this.responseStack = (MTBImageByte)(MTBImage.createMTBImage(
			width, height, 1, 1, scaleCount, MTBImage.MTBImageType.MTB_BYTE));
		for (int s=0; s<scaleCount; ++s) {
			this.responseStack.setImagePart(vesselMaps[s], 0, 0, 0, 0, s);
			this.responseStack.setSliceLabel("Width = " + this.widthLengthTab[s][0] 
				+ " , length = " + this.widthLengthTab[s][1], 0, 0, s);
		}
		this.responseStack.setTitle("Scale Responses of MFFDOGMultiScale Filter " 
				+ "for <" + this.inputImg.getTitle() + ">");
	}
	
	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#addStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void addStatusListener(StatusListener statuslistener) {
		this.statusListeners.add(statuslistener);
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#notifyListeners(loci.common.StatusEvent)
	 */
	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.statusListeners.size(); i++) {
			this.statusListeners.get(i).statusUpdated(e);
		}
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#removeStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.statusListeners.remove(statuslistener);
	}
}

