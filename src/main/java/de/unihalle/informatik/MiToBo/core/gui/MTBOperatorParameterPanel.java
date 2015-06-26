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

package de.unihalle.informatik.MiToBo.core.gui;

import de.unihalle.informatik.Alida.operator.*;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDOperatorParameterPanel;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;

/**
 * Implementation of panels for MiToBo operator parameters.
 * 
 * @author Birgit Moeller
 */
public class MTBOperatorParameterPanel 
	extends ALDOperatorParameterPanel {

	/**
	 * Default constructor.
	 * @param op        Operator to be configured by this panel.
	 * @param mode			Parameter view mode for initialization.
	 * @param topLevel 	If true, the panel is used in a toplevel context.
	 * @param listener 	Listener for value change events to be added.
	 */
	public MTBOperatorParameterPanel(ALDOperator op, 
			Parameter.ExpertMode mode, boolean topLevel, 
			ALDSwingValueChangeListener listener) {
		super(op,mode,topLevel,listener);
		// specific handling of images as required parameters:
		// if there is only one image open in the GUI, this image will be set 
		// as the default for the corresponding parameters
//		Collection<String> params = op.getInNames(new Boolean(true));
//		for (String s: params) {
//			try {
//	      ALDParameterDescriptor d = op.getParameterDescriptor(s);
//	      if (isImg(d.getMyclass())) {
//	      	MTBImage win = null;
//	      	int [] winIDs = ij.WindowManager.getIDList();
//	      	if (winIDs != null && winIDs.length > 0) {
//	      		win = MTBImage.createMTBImage( IJ.getImage());
//	      		op.setParameter(d.getName(), win);
//	      	}
//	      }
//      } catch (ALDOperatorException e) {
//      	System.err.println(
//     			"[MTBOperatorParameterPanel::MTBOperatorParameterPanel] " 
//      			+ " could not set default value for image parameter, skipping...");
//      	e.printStackTrace();
//      }
//		}
	}
	
//  @Override
//  public Object getParameterValue(boolean isRequired, 
//  		boolean isSupplemental, ALDParameterDescriptor descr) {
//  	// check if parameter refers to required image
//  	if (isRequired &&  isImg( descr.getMyclass())) {
//    	int [] winIDs = ij.WindowManager.getIDList();
//  		if (winIDs == null || winIDs.length == 0)
//  			return null;
//  		return MTBImage.createMTBImage( IJ.getImage());
//  	} 
//  	return super.getParameterValue(isRequired, isSupplemental, descr);
//  }

  /**
   * Checks if image is of correct type.
   * 
   * @param cl    Class of object in question.
   * @return      True if class corresponds to valid image class.
   */
//  private boolean isImg( Class<?> cl) {
//  	return    MTBImage.class.isAssignableFrom( cl) 
//  			   || ImagePlus.class.isAssignableFrom( cl);
//  }
}
