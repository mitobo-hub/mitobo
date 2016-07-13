/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010
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

package de.unihalle.informatik.MiToBo.core.imageJ;

import ij.gui.PolygonRoi;
import ij.io.RoiEncoder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator to save data objects to native ImageJ ROIs.
 * <p>
 * Note that only objects can be saved which implement the interface
 * {@link MTBDataExportableToImageJROI}. Since the MiToBo data types
 * are converted to ImageJ data types it is not guaranteed that all
 * properties are preserved, but some information might get lost.
 * However, objects saved with this writer can be opened in any ImageJ
 * installation without requiring MiToBo to be installed.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE, 
	level=Level.STANDARD, allowBatchMode = false)
public class RoiWriter extends MTBOperator {

	/**
	 * Identifier string for this operator.
	 */
	public static final String operatorID = "[RoiWriter]";
	
	/**
	 * Data to save to file.
	 */
	@Parameter(label = "Data to save", required = true, dataIOOrder = 0, 
			direction = Direction.IN, description = "Data.")
	private MTBDataExportableToImageJROI data = null;

	/**
	 * Output file name.
	 */
	@Parameter(label = "Output file", required = true, dataIOOrder = 1,
			direction = Direction.IN,	description = "Output ROI file.")
	private ALDFileString outFile = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public RoiWriter() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Specify the file where to save the data.
	 * <p>
	 * Note that the name might be modified to meet ImageJ conventions,
	 * i.e., an ending '.roi' or '.zip' might be added.
	 * 
	 * @param file	Filename where to save the data.
	 */
	public void setOutputFile(String file) {
		this.outFile = new ALDFileString(file);
	}

	/**
	 * Specify the data to save.
	 * 
	 * @param d Data object.
	 */
	public void setData(MTBDataExportableToImageJROI d) {
		this.data = d;
	}

	@Override
	protected void operate() throws ALDOperatorException {

		if (this.data instanceof MTBContour2D) {
			
			MTBContour2D c =(MTBContour2D)this.data;			
			// convert to ImageJ ROI object
			PolygonRoi pr = c.convertToImageJRoi()[0];
			// save to file
			this.savePolygon(pr);
			
		} // end of case 'MTBContour2D'
		else if (this.data instanceof MTBContour2DSet) {
			MTBContour2DSet contours = (MTBContour2DSet)this.data;
			if (contours.size() == 0) {
				return;
			}
			if (contours.size() == 1) {
				MTBContour2D c = contours.elementAt(0);
				// convert to ImageJ ROI object
				PolygonRoi pr = c.convertToImageJRoi()[0];
				// save to file
				this.savePolygon(pr);
			}
			else {
				PolygonRoi[] prs = contours.convertToImageJRoi();
				this.savePolygonSet(prs);
			}		
		} // end of case 'MTBContour2DSet'
		else if (this.data instanceof MTBPolygon2D) {
			
			MTBPolygon2D poly = (MTBPolygon2D)this.data;
			// convert to ImageJ ROI object
			PolygonRoi pr = poly.convertToImageJRoi()[0];
			// save to file
			this.savePolygon(pr);
			
		} // end of case 'MTBPolygon2D'
		else if (this.data instanceof MTBPolygon2DSet) {
			MTBPolygon2DSet polys = (MTBPolygon2DSet)this.data;
			if (polys.size() == 0) {
				return;
			}
			if (polys.size() == 1) {
				MTBPolygon2D poly = polys.elementAt(0);
				// convert to ImageJ ROI object
				PolygonRoi pr = poly.convertToImageJRoi()[0];
				// save to file
				this.savePolygon(pr);
			}
			else {
				PolygonRoi[] prs = polys.convertToImageJRoi();
				this.savePolygonSet(prs);
			}		
		} // end of case 'MTBPolygon2DSet'
		else {
			throw new ALDOperatorException(
				OperatorExceptionType.OPERATE_FAILED, operatorID 
					+ " Unknown data type, unable to save to ROI file!");
		}

	}

	/**
	 * Saves the given polygon to the specified output file.
	 * <p>
	 * The output file written by this method contains a single polygon.
	 * <p>
	 * The code of this method in parts origins from the ImageJ source 
	 * code, for details see the method <code>save()</code> in
	 * 
	 * https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/RoiManager.java.html
	 * 
	 * @param pr	Polygon to save.
	 */
	private void savePolygon(PolygonRoi pr) {
		String outName = this.outFile.getFileName();
		if (!outName.endsWith(".roi")) 
			outName = outName + ".roi";
		String newName = outName.substring(0, outName.length()-4);
		pr.setName(newName);
		RoiEncoder re = new RoiEncoder(outName);
		try {
			re.write(pr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the set of polygons to the specified output file.
	 * <p>
	 * The output file is a zip archive containing a set of ROIs.
	 * <p>
	 * The code of this method in parts origins from the ImageJ source 
	 * code, for details see the method <code>save()</code> in
	 * 
	 * https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/RoiManager.java.html
	 * 
	 * @param prs Set of polygons to save.
	 */
	private void savePolygonSet(PolygonRoi[] prs) {
			
		String filename = this.outFile.getFileName();
		if (!(filename.endsWith(".zip") || filename.endsWith(".ZIP")))
			filename = filename + ".zip";

		DataOutputStream out = null;
		try {
			ZipOutputStream zos = new ZipOutputStream(
					new BufferedOutputStream(new FileOutputStream(filename)));
			out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);

			for (int i=0;i<prs.length; ++i) {
				PolygonRoi pr = prs[i];
				zos.putNextEntry(new ZipEntry("Roi_" + i + ".roi"));
				re.write(pr);
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out!=null)
				try {out.close();} catch (IOException e) {
					// nothing to be done here
				}
		}
	}
}
