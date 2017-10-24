/* IMPORTANT NOTICE:
 * This file has originally been part of the Cell_Counter plugin written by
 * Kurt De Vos, see http://rsb.info.nih.gov/ij/plugins/cell-counter.html.
 * We extended the plugin functionality to fit to the specific needs of MiToBo. 
 * You can find the original license and file header below following the 
 * MiToBo license header.
 */

/*
 * Copyright (C) 2010 - @YEAR@ by the MiToBo development team
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

/* === Original File Header === */

/*
 * ODODD.java
 *
 * Created on 23 November 2004, 22:56
 */

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.xml;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ListIterator;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarker;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShape;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapePolygon;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapeRegion;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerVector;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint2DType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPolygon2DType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DType;

/**
 * Writes markers of an image to file in XML format.
 *
 * @author  kurt
 * @author Birgit Moeller
 */
public class WriteXML{
	private OutputStream XMLFileOut;
	private OutputStream XMLBuffOut;
	private OutputStreamWriter out;

	/**
	 * Creates a new instance of ODWriteXMLODD
	 */
	public WriteXML(String XMLFilepath) 
			throws FileNotFoundException, UnsupportedEncodingException {
			this.XMLFileOut= new FileOutputStream(XMLFilepath); // add FilePath
			this.XMLBuffOut= new BufferedOutputStream(this.XMLFileOut);
			this.out = new OutputStreamWriter(this.XMLBuffOut, "UTF-8");
	}

	public boolean writeXML(String imgFilename, Vector<?> typeVector, int currentType){
		try {
			this.out.write("<?xml version=\"1.0\" ");
			this.out.write("encoding=\"UTF-8\"?>\r\n");
			this.out.write("<CellCounter_Marker_File>\r\n");

			// write the image properties
			this.out.write(" <Image_Properties>\r\n");
			this.out.write("     <Image_Filename>"+ imgFilename + "</Image_Filename>\r\n");
			this.out.write(" </Image_Properties>\r\n");

			// write the marker data
			this.out.write(" <Marker_Data>\r\n");
			this.out.write("     <Current_Type>"+ currentType + "</Current_Type>\r\n");
			ListIterator<?> it = typeVector.listIterator();
			while(it.hasNext()){
				CellCntrMarkerVector markerVector = (CellCntrMarkerVector)it.next();
				int type = markerVector.getType();
				this.out.write("     <Marker_Type>\r\n");
				this.out.write("         <Type>" +type+ "</Type>\r\n");
				this.out.write("         <Color>" 
						+ markerVector.getColor().getRed() + ","
						+ markerVector.getColor().getGreen() + ","
						+ markerVector.getColor().getBlue() + "</Color>\r\n");
				ListIterator<?> lit = markerVector.listIterator();
				while(lit.hasNext()){
					CellCntrMarker marker = (CellCntrMarker)lit.next();
					int x = marker.getX();
					int y = marker.getY();
					int z = marker.getZ();
					this.out.write("         <Marker>\r\n");
					this.out.write("             <MarkerX>" +x+ "</MarkerX>\r\n");
					this.out.write("             <MarkerY>" +y+ "</MarkerY>\r\n");
					this.out.write("             <MarkerZ>" +z+ "</MarkerZ>\r\n");
					CellCntrMarkerShape s = marker.getShape();
					if (s != null) {
						if (s.getClass().equals(CellCntrMarkerShapeRegion.class)) {
							this.out.write("             <MarkerShape>region</MarkerShape>\r\n");
							CellCntrMarkerShapeRegion sr = (CellCntrMarkerShapeRegion)s;
							MTBRegion2D r = sr.getRegion();
							MTBXMLRegion2DType rxml = r.toXMLType();
							this.out.write(rxml.toString());
						}
						else if (s.getClass().equals(CellCntrMarkerShapePolygon.class)) {
							this.out.write("             <MarkerShape>polygon</MarkerShape>\r\n");
							CellCntrMarkerShapePolygon sp = (CellCntrMarkerShapePolygon)s;
							MTBPolygon2D p = sp.getPolygon();
							MTBXMLPolygon2DType pxml = p.toXMLType(null);
							this.out.write(pxml.toString());
						}
						this.out.write("\r\n");
					}
					this.out.write("         </Marker>\r\n");
				}
				this.out.write("     </Marker_Type>\r\n");
			}

			this.out.write(" </Marker_Data>\r\n");
			this.out.write("</CellCounter_Marker_File>\r\n");
			this.out.flush();  // Don't forget to flush!
			this.out.close();
			return true;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

}
