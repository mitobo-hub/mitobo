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

package de.unihalle.informatik.MiToBo.segmentation.regions.convert;

import java.awt.geom.Point2D;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Operator to convert 2D label image to a MTBRegionsSet2D.
 * @author posch
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD)
public class Region2DSetFromLabelimage extends MTBOperator implements StatusReporter {
	
	/**
	 * Input image.
	 */
	@Parameter(label= "Label image", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 1, description = "Label image")
		private transient MTBImage labelImage = null;

	/**
	 * If true all background pixels, defined as pixels with label equals <code>backgroundLabel</code>
	 * are excluded
	 */
	@Parameter(label= "ExcludeBackground", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2, description = "Exclude background region?")
		private  boolean excludeBackground = false;

	@Parameter(label= "BackgroundLabel", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 3, description = "Label of background pixels")
		private  int backgroundLabel = 0;

	/**
	 * Resulting region set.
	 */
	@Parameter(label= "Regionset", required = true, direction = Direction.OUT,
		mode = ExpertMode.STANDARD, dataIOOrder = 1, description = "Region set from label image")
	private transient MTBRegion2DSet regionSet = null;

	/** vector of installed StatusListeners */
	private transient Vector<StatusListener> m_statusListeners;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public Region2DSetFromLabelimage() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>();
	}
	
	@Override
	public 	void validateCustom() throws ALDOperatorException {
		if (labelImage.getSizeC() > 1 || labelImage.getSizeT() > 1 || labelImage.getSizeZ() > 1) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				    "\n>>>>>>> Region2DSteFromLabelimage: validation failed!\nOnly 2D images allowed.");

		}

		if (labelImage.getType() != MTBImageType.MTB_BYTE && 
				labelImage.getType() != MTBImageType.MTB_SHORT &&
						labelImage.getType() != MTBImageType.MTB_INT ) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				    "\n>>>>>>> Region2DSteFromLabelimage: validation failed!\nOnly BYTE an SHORT and INT label image allowed.");

		}
	}
	
	/**
	 * Init function for deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being deserialized
	 * from file, prior to handing the instance over to the user. It takes care
	 * of a proper initialization of transient member variables as they are not
	 * initialized to the default values during deserialization. 
	 * @return
	 */
	protected Object readResolve() {
		super.readResolve();
		this.m_statusListeners = new Vector<StatusListener>();
		return this;
	}

	/**
	 * Get reference to the current Label image.
	 */
	public MTBImage getLabelImage() {
		return this.labelImage;
	}
	
	/**
	 * Set Label image.
	 */
	public void setLabelImage(MTBImage labelImage) {
		this.labelImage = labelImage;
	}
	
	/**
	 * @return the regionSet
	 */
	public MTBRegion2DSet getRegionSet() {
		return regionSet;
	}


	@Override
	protected void operate() {
		if ( excludeBackground ) {
			regionSet = new MTBRegion2DSet(labelImage, backgroundLabel);
		} else {
			regionSet = new MTBRegion2DSet(labelImage);
		}
	}

	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		this.m_statusListeners.add(statuslistener);	
	}

	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			this.m_statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.m_statusListeners.remove(statuslistener);
	}

}
