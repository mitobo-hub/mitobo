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
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMotionModelIDType;


/**
 * A target-ID class that additionally hold a "motion model"-ID.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MotionModelID extends TargetID {
	
	/** motion-model-ID */
	public byte mmID;
	
	/** 
	 * Constructor.
	 * @param ID target-ID
	 * @param mmID motion-model-ID
	 */
	public MotionModelID(short ID, byte mmID) {
		super(ID);
		
		this.mmID = mmID;
	}
	
	/**
	 * Constructor with the corresponding XML-Type.
	 * @param mmIDxml XML-object that represents this MotionModelID-object
	 */
	public MotionModelID(MTBXMLMotionModelIDType mmIDxml) {
		super(mmIDxml.getId());
		
		this.mmID = mmIDxml.getMmid();
		this.time = mmIDxml.getTime();
	}
	
	@Override
	public MotionModelID copy() {
		MotionModelID mmid = new MotionModelID(this.ID, this.mmID);
		mmid.time = this.time;
		return mmid;
	}
	
	/**
	 * Convert this object to its XML-representation.
	 */
	public MTBXMLMotionModelIDType toXMLType() {
		
		MTBXMLMotionModelIDType mmidXml = MTBXMLMotionModelIDType.Factory.newInstance();
		mmidXml.setId(this.ID);
		mmidXml.setMmid(this.mmID);
		mmidXml.setTime(this.time);
		
		return mmidXml;
	}
	

}
