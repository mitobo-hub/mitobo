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

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarker;

/**
 * Container base class for all detectors used in the cell counter.
 * <p>
 * This class basically serves as a place-holder and defines the interface. 
 * Detector functionality is to be implemented in related sub-classes. 
 *  
 * @author Birgit Moeller
 */
public abstract class CellCounterDetectOperator extends MTBOperator
	implements StatusListener, StatusReporter {
	
	/**
	 * Input image to process.
	 */
	@Parameter( label = "Input image", required = true, 
		direction = Parameter.Direction.IN,	mode = ExpertMode.STANDARD, 
		dataIOOrder = 1, description = "Input image.")
	protected transient MTBImage inputImage = null;

	/**
	 * Index of input image slice if z-stack is provided as input.
	 */
	@Parameter( label = "Channel index", required = true, 
		direction = Parameter.Direction.IN,	mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Channel index in stack.")
	protected int detectZSlice;

	/**
	 * Detection result.
	 */
	@Parameter( label = "Detection Results", direction = Parameter.Direction.OUT, 
		dataIOOrder = 1, description = "Detection results.")
	protected Vector<CellCntrMarker> detectResults;
	
	/**
	 * Get common short name of operator for GUI.
	 * @return	Name of operator.
	 */
	public abstract String getShortName();
	
	/**
	 * Open the configuration frame.
	 */
	public abstract void openConfigFrame();
	
	/**
	 * Close the configuration frame.
	 */
	public abstract void closeConfigFrame();
	
	/**
	 * Get the detection results.
	 * 
	 * @return Vector of detected objects.
	 */
	public Vector<CellCntrMarker> getDetectionResults() {
		return this.detectResults;
	}
	
	/** 
	 * Vector of installed {@link StatusListener} objects.
	 */
	protected Vector<StatusListener> m_statusListeners;
	
	/**
	 * Constructor.	
	 * @throws ALDOperatorException Thrown in case of initialization error.
	 */
	public CellCounterDetectOperator() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
	}

	/**
	 * Set input image.
	 * @param img	Input image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inputImage = img;
	}	
	
	/**
	 * Set index of slice in stack, starting with 0 for first slice.
	 * @param i	Index in stack.
	 */
	public void setSliceZid(int i) {
		this.detectZSlice = i;
	}	

	/**
	 * Method for handling over value change listener to sub-windows.
	 * @param listener	Value change listener to notify in case of changes.
	 */
	public abstract void addValueChangeEventListener(
			ALDSwingValueChangeListener listener);

	/**
	 * Method is to be called for cleaning-up the operator resources.
	 */
	public abstract void dispose(); 
	
	@Override
  public void addStatusListener(StatusListener statListener) {
		this.m_statusListeners.add(statListener);	
  }

	@Override
  public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			this.m_statusListeners.get(i).statusUpdated(e);
		}
  }

	@Override
  public void removeStatusListener(StatusListener statListener) {
		this.m_statusListeners.remove(statListener);
  }
	
	@Override
  public void statusUpdated(StatusEvent e) {
		this.notifyListeners(e);
  }
}
