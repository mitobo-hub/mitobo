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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.annotations;

import java.util.Map;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperatorCollectionElement;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;
import ij.ImagePlus;

/**
 * Abstract super class for operators targeting at improving root 
 * annotations in rhizoTrak.
 * 
 * @author Birgit Moeller
 */
public abstract class RootAnnotationEnhanceOperator 
	extends ALDOperatorCollectionElement 
{
  /**
   * Input image.
   */
  protected ImagePlus image;
	
  /**
	 * Treelines to enhance.
   */
	protected Vector<MTBRootTree> inputTreelines;

	/**
   * Enhanced treeline annotations.
   */
	protected Vector<MTBRootTree> resultTreelines;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public RootAnnotationEnhanceOperator() throws ALDOperatorException {
		super();
	}

	/**
	 * Setter for the input image.
	 * @param img - ImagePlus
	 */
	public void setImage(ImagePlus img) {
		this.image = img;
	}
	
	/**
	 * Provide treeline to be enhanced.
	 * @param tls		Set if treelines.
	 */
	public void setInputTreelines(Vector<MTBRootTree> tls) {
		this.inputTreelines = tls;
	}

	/**
	 * Getter for the input image.
	 * @return the image as an ImagePlus
	 */
	public ImagePlus getImage() {
		return this.image;
	}
	
	/**
	 * Getter for the result treelines.
	 * @return List of updated treelines.
	 */
	public Vector<MTBRootTree> getResultTreelines() {
		return this.resultTreelines;	
	}
	
}
