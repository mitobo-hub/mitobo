/*
OME Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 *-----------------------------------------------------------------------------
 *
 *  Copyright (C) 2007 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee,
 *      University of Wisconsin-Madison
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *-----------------------------------------------------------------------------
 */

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
 * This class uses the Bio-Formats and OME-XML packages/libraries (see the two licenses at the top)
 */

package de.unihalle.informatik.MiToBo.io.tools;

import ij.measure.Calibration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.IFormatWriter;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import ome.xml.model.enums.PixelType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * A class of utility functions used by the MiToBo's image-IO classes
 * @author Oliver Gress
 *
 */
public class ImageIOUtils {

	/**
	 * Get the OME pixel type corresponding to the specified MTB image type
	 * @param type
	 * @return OME pixel type
	 */
	public static PixelType omePixelType(MTBImageType type) {
		if (type == MTBImageType.MTB_BYTE)
			return PixelType.UINT8;
		else if (type == MTBImageType.MTB_SHORT)
			return PixelType.UINT16;
		else if (type == MTBImageType.MTB_INT)
			return PixelType.INT32;
		else if (type == MTBImageType.MTB_FLOAT)
			return PixelType.FLOAT;
		else if (type == MTBImageType.MTB_DOUBLE)
			return PixelType.DOUBLE;
		else if (type == MTBImageType.MTB_RGB)
			return PixelType.UINT8;
		else
			return null;
	}
	
	/**
	 * Set OME meta data for image of index <code>imageIdx</code> using 
	 * information from a <code>Calibration</code> object.
	 * 
	 * @param cal				Calibration object.
	 * @param omemeta		OME object where to store calibration metadata.
	 * @param imageIdx	Index of image.
	 */
	public static void physicalPixelSize_to_OME(Calibration cal, 
			IMetadata omemeta, int imageIdx) {

		Unit<Length> ul = UNITS.MICROMETER;

		if (ImageIOUtils.toMicrons(cal.pixelWidth, cal.getXUnit()) > 0.0) {
			omemeta.setPixelsPhysicalSizeX(
				new Length(
					new Double(ImageIOUtils.toMicrons(cal.pixelWidth, cal.getXUnit())), 
						ul), imageIdx);
		}
			
		if (ImageIOUtils.toMicrons(cal.pixelHeight, cal.getYUnit()) > 0.0) {
			omemeta.setPixelsPhysicalSizeY(
				new Length(
					new Double(ImageIOUtils.toMicrons(cal.pixelHeight, cal.getYUnit())), 
						ul), imageIdx);
		}
		
		if (ImageIOUtils.toMicrons(cal.pixelDepth, cal.getZUnit()) > 0.0) {
			omemeta.setPixelsPhysicalSizeZ(
				new Length(
					new Double(ImageIOUtils.toMicrons(cal.pixelDepth, cal.getZUnit())), 
						ul), imageIdx);
		}

		Unit<Time> ut = Unit.CreateBaseUnit(cal.getTimeUnit(), "s");
		Time t = new Time(new Double(
				ImageIOUtils.toSeconds(cal.frameInterval, cal.getTimeUnit())), ut);
		omemeta.setPixelsTimeIncrement(t, imageIdx);		
	}
	
	/**
	 * Convert a value of given space unit to microns.
	 * @param val		Value to convert.
	 * @param unit	Source unit.
	 * @return	Value in microns.
	 */
	public static double toMicrons(double val, String unit) {
		if (   unit.equalsIgnoreCase("micron") || unit.equalsIgnoreCase("microns") 
				|| unit.equalsIgnoreCase("um") || unit.equalsIgnoreCase("micrometer"))
			return val;
		else if (unit.equalsIgnoreCase("pm") || unit.equalsIgnoreCase("picometer"))
			return val * 0.000001;
		else if (unit.equalsIgnoreCase("nm") || unit.equalsIgnoreCase("nanometer"))
			return val * 0.001;
		else if (unit.equalsIgnoreCase("mm") || unit.equalsIgnoreCase("millimeter"))
			return val * 1000;
		else if (unit.equalsIgnoreCase("cm") || unit.equalsIgnoreCase("centimeter"))
			return val * 10000;
		else if (unit.equalsIgnoreCase("dm") || unit.equalsIgnoreCase("decimeter"))
			return val * 100000;
		else if (unit.equalsIgnoreCase("m") || unit.equalsIgnoreCase("meter"))
			return val * 1000000;
		else if (unit.equalsIgnoreCase("km") || unit.equalsIgnoreCase("kilometer"))
			return val * 1000000000;
		else if (unit.equalsIgnoreCase("inch") || unit.equalsIgnoreCase("inches"))
			return val * 25.4 * 1000.0;
		else if (unit.equalsIgnoreCase("pixel") || unit.equalsIgnoreCase("pixels"))
			return 0.0;
		else
			return -1.0;
	}
	
	/**
	 * Convert a value of given time unit to seconds.
	 * @param val
	 * @param unit
	 * @return
	 */
	public static double toSeconds(double val, String unit) {
		if (unit.equalsIgnoreCase("sec") || unit.equalsIgnoreCase("s") 
				|| unit.equalsIgnoreCase("second") || unit.equalsIgnoreCase("seconds"))
			return val;
		else if (unit.equalsIgnoreCase("psec") || unit.equalsIgnoreCase("ps"))
			return val * 0.000000001;
		else if (unit.equalsIgnoreCase("nsec") || unit.equalsIgnoreCase("ns"))
			return val * 0.000001;
		else if (unit.equalsIgnoreCase("msec") || unit.equalsIgnoreCase("ms"))
			return val * 0.001;
		else if (unit.equalsIgnoreCase("min") || unit.equalsIgnoreCase("m"))
			return val * 60.0;
		else if (unit.equalsIgnoreCase("hour") || unit.equalsIgnoreCase("h") || unit.equalsIgnoreCase("std"))
			return val * 360.0;
		else
			return 0.0;
	}
	
	/**
	 * Set the calibration of an image from the OME meta data of image of index <code>imageIdx</code>
	 * @param cal
	 * @param omemeta
	 * @param imageIdx
	 */
	public static void physicalPixelSize_from_OME(Calibration cal, IMetadata omemeta, int imageIdx) {
		// physical pixel size in microns
		Length sx = omemeta.getPixelsPhysicalSizeX(imageIdx);
		Length sy = omemeta.getPixelsPhysicalSizeY(imageIdx);
		Length sz = omemeta.getPixelsPhysicalSizeZ(imageIdx);
		
		// time increment in seconds
		Time st = omemeta.getPixelsTimeIncrement(imageIdx);
		
		if (sx != null && sx.value().doubleValue() > 0.0) {
			cal.pixelWidth = sx.value().doubleValue();
			cal.setXUnit("microns");
		}
		else {
			cal.pixelWidth = 1;
			cal.setXUnit("pixel");
		}
		
		if (sy != null && sy.value().doubleValue() > 0.0) {
			cal.pixelHeight = sy.value().doubleValue();
			cal.setYUnit("microns");
		}
		else {
			cal.pixelHeight = 1;
			cal.setYUnit("pixel");
		}
		
		if (sz != null && sz.value().doubleValue() > 0.0) {
			cal.pixelDepth = sz.value().doubleValue();
			cal.setZUnit("microns");
		}
		else {
			cal.pixelDepth = 1;
			cal.setZUnit("pixel");
		}
		
		if (st != null) {
			cal.frameInterval = st.value().doubleValue();
			cal.setTimeUnit("sec");
		}
	}
	
	
//
//	public static void physicalPixelSize_from_Reader(Calibration cal, IFormatReader reader, int imageIdx) {
//		
//		int seriesTmp = reader.getSeries();
//		reader.setSeries(imageIdx);
//		
//		CoreMetadata[] coreMeta = reader.getCoreMetadata();
//		
//		Hashtable<String, Object> seriesMeta = reader.getSeriesMetadata();
//		
//		Hashtable<String, Object> origMeta = reader.getGlobalMetadata();
//	
//		//#TODO: look for physicalPixelSize information...
//
//		reader.setSeries(seriesTmp);
//	}
	
	
	/**
	 * Create a string of image writing information
	 * @param filename
	 * @param w
	 * @param imgIdx
	 * @return
	 */
	public static String imgWriteInfo(String filename, IFormatWriter w, int imgIdx) {
		MetadataRetrieve meta = w.getMetadataRetrieve();
		
		String s = "Configuration of image writer:\n";
		s += "--ImageWriter-- \n";
		s += "| Output file: " + filename + "\n";
		s += "| File format: " + w.getFormat() + "\n";
		s += "| Compression: " + w.getCompression() + "\n";
		s += "| Interleaved pixel order: " + w.isInterleaved() + "\n";
		s += "| Stacks supported: " + w.canDoStacks() + "\n";
		s += "--Image--\n";
		s += "| Image title: " + meta.getImageName(imgIdx) + "\n";
		s += "| Size in x: " + meta.getPixelsSizeX(imgIdx) + "\n";
		s += "| Size in y: " + meta.getPixelsSizeY(imgIdx) + "\n";
		s += "| Size in z: " + meta.getPixelsSizeZ(imgIdx) + "\n";
		s += "| Number of (time)frames: " + meta.getPixelsSizeT(imgIdx) + "\n";
		s += "| Number of channels: " + meta.getPixelsSizeC(imgIdx).getValue()/meta.getChannelSamplesPerPixel(imgIdx, 0).getValue() + "\n";
		s += "| Samples per pixel: " + meta.getChannelSamplesPerPixel(imgIdx, 0) + "\n";
		s += "| Data type: " + meta.getPixelsType(imgIdx);
		
		
		return s;
	}
	
	/**
	 * Create a string of image reading information
	 * @param r
	 * @return
	 */
	public static String imgReadInfo(IFormatReader r) {
		
		String s = "Configuration of image reader:\n";
		s += "--Image file--\n";
		s += "| Input file: " + r.getCurrentFile() + "\n";
		s += "| File format: " + r.getFormat() + "\n";
		s += "| Number of images in file: " + r.getSeriesCount() + "\n";
		s += "--Image to read--\n";
		s += "| Index of the image to read: " + r.getSeries() + "\n";
		s += "| Size in x: " + r.getSizeX() + "\n";
		s += "| Size in y: " + r.getSizeY() + "\n";
		s += "| Size in z: " + r.getSizeZ() + "\n";
		s += "| Number of (time)frames: " + r.getSizeT() + "\n";
		s += "| Number of channels: " + r.getSizeC()/r.getRGBChannelCount() + "\n";
		s += "| Samples per pixel: " + r.getRGBChannelCount() + "\n";
		s += "| Interleaved pixel order: " + r.isInterleaved() + "\n";
		s += "| Data type: " + FormatTools.getPixelTypeString(r.getPixelType()) + "\n";
		s += "| Dimension order: " + r.getDimensionOrder() + "  certain? " + r.isOrderCertain();
		
		return s;
	}
	
	/**
	 * Get available codecs of a specific writer class. The returned HashMap contains the codec ID as key
	 * and corresponding to a key a String containing the codec's name.
	 * @param writerclass
	 * @return
	 */
	public static HashMap<Integer, String> availableCodecs(Class<? extends IFormatWriter> writerclass) {
		Field[] fields = writerclass.getFields();
		
		HashMap<Integer, String> codecs = new HashMap<Integer, String>();
		
		String fname;
		int idx;
		for (int i = 0; i < fields.length; i++) {
			fname = fields[i].getName();
			
			if (fname.startsWith("CODEC_")) {
				
				idx = fname.indexOf("_");
				fname = fname.substring(idx+1);
				
				if (!fields[i].isAccessible())
					fields[i].setAccessible(true);
				
				try {
					
					codecs.put(fields[i].getInt(null), fname);
					
				} catch (IllegalArgumentException e) {
					System.err.println("OmeTiffIOUtils.availableCodecs(): Cannot retrieve value of "
							+ writerclass.toString() + "." + fields[i].getName() + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					System.err.println("OmeTiffIOUtils.availableCodecs(): Cannot retrieve value of "
							+ writerclass.toString() + "." + fields[i].getName() + ": " + e.getMessage());
				}
			}
		}	
		
		return codecs;
	}
	
	/**
	 * Get available qualities of a specific writer class. The returned HashMap contains the quality ID as key
	 * and corresponding to a key a String containing the quality's name.
	 * @param writerclass
	 * @return
	 */
	public static HashMap<Integer, String> availableQualities(Class<? extends IFormatWriter> writerclass) {
		Field[] fields = writerclass.getFields();
		
		HashMap<Integer, String> qualities = new HashMap<Integer, String>();
		
		String fname;
		int idx;
		for (int i = 0; i < fields.length; i++) {
			fname = fields[i].getName();
			
			if (fname.startsWith("QUALITY_")) {
				
				idx = fname.indexOf("_");
				fname = fname.substring(idx+1);
				
				if (!fields[i].isAccessible())
					fields[i].setAccessible(true);
				
				try {
					
					qualities.put(fields[i].getInt(null), fname);
					
				} catch (IllegalArgumentException e) {
					System.err.println("OmeTiffIOUtils.availableQualities(): Cannot retrieve value of "
							+ writerclass.toString() + "." + fields[i].getName() + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					System.err.println("OmeTiffIOUtils.availableQualities(): Cannot retrieve value of "
							+ writerclass.toString() + "." + fields[i].getName() + ": " + e.getMessage());
				}
			}
		}	
		
		return qualities;
	}
	
	/**
	 * Obtain the key for a given value of a HashMap, if available. Otherwise null is returned
	 * @param hmap
	 * @param value
	 * @return
	 */
	public static Integer getKey(HashMap<Integer, String> hmap, String value) {
		Integer key = null;
		Set<Integer> keyset = hmap.keySet();
		
		for (Integer i : keyset) {
			if (hmap.get(i).equals(value))
				key = i;
		}
		
		return key;
	}
}
