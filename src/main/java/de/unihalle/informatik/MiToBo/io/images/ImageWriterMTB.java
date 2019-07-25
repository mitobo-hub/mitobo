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
LOCI Common package: utilities for I/O, reflection and miscellaneous tasks.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden and Chris Allan.

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
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
Bio-Formats Importer, Bio-Formats Exporter, Bio-Formats Macro Extensions,
Data Browser and Stack Slicer. Copyright (C) 2005-@year@ Melissa Linkert,
Curtis Rueden and Christopher Peterson.

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

/*
 * This class uses the Bio-Formats and LOCI-commons packages/libraries (see the two licenses at the top)
 * as well as source code from the LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.io.images;


import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
//import imagej.data.Dataset;
//import imagej.data.DefaultDataset;






import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

//import net.imglib2.exception.IncompatibleTypeException;
//import net.imglib2.img.Img;
//import net.imglib2.img.ImgPlus;
//import net.imglib2.io.ImgIOException;
//import net.imglib2.io.ImgSaver;
//import net.imglib2.type.numeric.RealType;






import loci.common.DataTools;
import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.meta.IMetadata;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.AVIWriter;
import loci.formats.out.LegacyQTWriter;
import loci.formats.out.QTWriter;
import loci.formats.services.OMEXMLService;
import ome.xml.meta.OMEXMLMetadataRoot;
//import ome.xml.meta.MetadataRoot;
//import ome.xml.meta.OMEXMLMetadata;
//import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.OME;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageInt;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.tools.ImageIOUtils;

/**
 * Image writer operator to store MTBImage or ImagePlus to disk along with the
 * image history (.mph) file. This writer operator is based on Bio-Formats, thus the
 * available formats depend on Bio-Formats (and its available extension). The format is
 * determined by the given filename extension.
 * Different formats allow different options, e.g. compression of images or framerate of movies.
 * If options are set by the available setter-methods that are not applicable to the current format
 * writer, the options are simply ignored.<br><br>
 * 
 * The <code>ImageWriterMTB.runOp(..)</code> throws a special <code>OverwriteException</code> in case that
 * the specified file already exists, but the overwrite-flag is set to <code>false</code>.
 * 
 * @author Oliver Gress
 *
 */

@ALDMetaInfo(export=ExportPolicy.MANDATORY)
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL,level=Level.APPLICATION)
public class ImageWriterMTB extends MTBOperator implements StatusReporter {
	
	
	private transient Vector<StatusListener> statusListeners;
	
	@Parameter( label= "Image compression", required = false, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 6,
	        description = "Image compression")
	private String compression = null;

	@Parameter( label= "overwrite", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 4,
	        description = "Overwrite permission flag")
	private boolean overwrite =true;

	@Parameter( label= "Video codec", required = false, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 8,
	        description = "Video codec (quicktime only)")
	private Integer codec = null;

	@Parameter( label= "Fps", required = false, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 7,
	        description = "Frames per second for movies")
	private Integer fps = null;

	@Parameter( label= "Ignore stack specifications", required = true, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 5,
	        description = "Flag to ignore an invalid specification of the stack")
	private boolean ignoreInvalidStackSpec = false;

	@Parameter( label= "Video quality", required = false, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 9,
	        description = "Video quality (quicktime only)")
	private Integer quality = null;

	@Parameter( label= "Filename", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 3,
	        description = "Image filename")
	private ALDFileString fileName = null;

	@Parameter( label= "Input MTBImage", required = false, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
	        description = "MTBImage to save")
	private transient MTBImage inputMTBImage = null;

	@Parameter( label= "Input ImagePlus", required = false, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2,
	        description = "ImagePlus to save")
	private transient ImagePlus inputImagePlus = null;

//	@Parameter( label= "Input Dataset", required = false, direction = Direction.IN,
//			mode = ExpertMode.STANDARD, dataIOOrder = 2,
//	        description = "Dataset to save")
//	private Dataset inputDataset = null;
	
	
	public ImageWriterMTB() throws ALDOperatorException {
		super();
		
		this.statusListeners = new Vector<StatusListener>(1);
	}
	
	/**
	 * Image file output operator for MTBImage objects.
	 * @param img image to write to disk
	 * @param filename filename to write the image to. The extension specifies the output format.
	 * @throws ALDOperatorException
	 */
	public ImageWriterMTB(MTBImage img, String filename) throws ALDOperatorException {
		super();
		
		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setInputMTBImage(img);
		this.setFileName(filename);
	}
	
	/**
	 * Image file output operator for ImagePlus objects.
	 * @param imp image to write to disk
	 * @param filename filename to write the image to. The extension specifies the output format.
	 * @throws ALDOperatorException
	 */
	public ImageWriterMTB(ImagePlus imp, String filename) throws ALDOperatorException {
		super();
		
		this.statusListeners = new Vector<StatusListener>();
		
		this.setInputImagePlus(imp);
		this.setFileName(filename);
	}
	
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException, OverwriteException {
		
		File file = new File(this.getFileName());
		
		// overwrite/existing file
		if (this.getOverwrite() == false && file.exists()) {
			throw new OverwriteException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate():" +
					" Cannot write to file '"+this.getFileName()+"'. File exists and overwrite is forbidden.");
		}		
		else if (file.exists()) {
			if (!file.delete()) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): Cannot overwrite file '" +
								this.getFileName() + "'. Deletion failed.");
			}
		}
		
		if (this.getInputMTBImage() != null) {
			try {
				this.writeMTBImage(this.getFileName(), this.getInputMTBImage());
			} catch (DependencyException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to DependencyException:\n" + e.getMessage());
			} catch (ServiceException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to ServiceException:\n" + e.getMessage());
			} catch (RuntimeException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to RuntimeException:\n" + e.getMessage());
			} catch (FormatException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to FormatException:\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to IOException:\n" + e.getMessage());
			} catch (EnumerationException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to EnumerationException:\n" + e.getMessage());
			}
		}
		else if (this.getInputImagePlus() != null){
			try {
				this.writeImagePlus(this.getFileName(), this.getInputImagePlus());
			} catch (RuntimeException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to RuntimeException:\n" + e.getMessage());
			} catch (FormatException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to FormatException:\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to IOException:\n" + e.getMessage());
			} catch (DependencyException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to DependencyException:\n" + e.getMessage());
			} catch (ServiceException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to ServiceException:\n" + e.getMessage());
			} catch (EnumerationException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.operate(): " +
						"Failed to write file due to EnumerationException:\n" + e.getMessage());
			}
		}
//		else {
//			DefaultDataset dat = (DefaultDataset)this.inputDataset;
//			ImgPlus imgp = dat.getImgPlus();
//			ImgSaver saver = new ImgSaver();
//			Img<? extends RealType<?>> img = imgp.getImg();
//			try {
//				System.out.println("Saving to " + this.getFileName());
//				saver.saveImg(this.getFileName(), imgp);
//				MTBOperator.writeHistory(dat, this.getFileName());
//			} catch (ImgIOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IncompatibleTypeException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ALDProcessingDAGException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ALDOperatorException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			System.out.println("Dataset saved to disc...");
//		}
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {

//		System.out.println(this.getInputMTBImage());
//		System.out.println(this.getInputImagePlus());
//		
//		// validate input image (MTBImage or ImagePlus given exclusively) 
//		if (!(this.getInputMTBImage() != null ^ this.getInputImagePlus() != null)) {
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom():" +
//					" An image must specified exclusively whether a MTBImage or a ImagePlus.");
//		}
//	
//		// validate compression parameter
//		String specifiedComp = this.getCompression();
//		boolean validComp = true;
//		
//		if (specifiedComp != null) {
//		
//			String[] comps = null;
//			try {
//				comps = this.getAvailableCompression();
//			} catch (FormatException e) {
//				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom():" +
//						" Could not retrieve compression information from writer:\n" + e.getMessage());
//			}
//		
//			validComp = false;
//			
//			if (comps != null) {
//				for (String comp : comps) {
//					if (specifiedComp.equals(comp)) {
//						validComp = true;
//						break;
//					}
//				}
//			}
//		}
//		
//		if (!validComp)
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom(): " +
//					"The specified compression type '" + specifiedComp + "' is not valid.");
//		
//		
//		// validate quality parameter
//		Integer specifiedQual = this.getQuality();
//		boolean validQual = true;
//		
//		if (specifiedQual != null) {
//		
//			HashMap<Integer,String> quals = null;
//			try {
//				 quals = this.getAvailableQualities();
//			} catch (FormatException e) {
//				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom():" +
//						" Could not retrieve available quality IDs from writer:\n" + e.getMessage());
//			}
//		
//			validQual = false;
//			
//			if (quals != null) {
//				for (Integer qual : quals.keySet()) {
//					if (specifiedQual.intValue() == qual.intValue()) {
//						validQual = true;
//						break;
//					}
//				}
//			}
//		}
//		
//		if (!validQual)
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom(): " +
//					"The specified quality ID '" + specifiedQual + "' is not valid.");
//		
//		
//		// validate codec parameter
//		Integer specifiedCod = this.getCodec();
//		boolean validCod = true;
//		
//		if (specifiedCod != null) {
//		
//			HashMap<Integer,String> cods = null;
//			try {
//				 cods = this.getAvailableCodecs();
//			} catch (FormatException e) {
//				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom():" +
//						" Could not retrieve available codec IDs from writer:\n" + e.getMessage());
//			}
//		
//			validCod = false;
//			
//			if (cods != null) {
//				for (Integer cod : cods.keySet()) {
//					if (specifiedCod.intValue() == cod.intValue()) {
//						validCod = true;
//						break;
//					}
//				}
//			}
//		}
//		
//		if (!validCod)
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom(): " +
//					"The specified codec ID '" + specifiedCod + "' is not valid.");
//		
//		// validate fps
//		Integer fps = this.getFps();
//		if (fps != null && fps.intValue() <= 0) {
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ImageWriterMTB.validateCustom(): " +
//					"Frames per second must be larger than 0");
//		}
	}
	
	/**
	 * Write an MTBImage to disk
	 */
	protected void writeMTBImage(String filename, MTBImage img) 
				throws DependencyException, ServiceException, ALDOperatorException, 
						RuntimeException, FormatException, IOException, EnumerationException, ALDProcessingDAGException {
		

		MTBImageType mtbtype = img.getType();
		
		int ptype = 0;
		int channels = 1;
		
		double[][] ddata = null;
		int[][] idata = null;
		byte[][] bdataR = null;
		byte[][] bdataG = null;
		byte[][] bdataB = null;
		
		if (mtbtype == MTBImageType.MTB_BYTE || mtbtype == MTBImageType.MTB_SHORT 
				|| mtbtype == MTBImageType.MTB_FLOAT) {
			// these types can be written simply using the underlying ImagePlus
			
			this.writeImagePlus(filename, img.getImagePlus());
			return;
		}
		else if (mtbtype == MTBImageType.MTB_RGB) {
			channels = 3;
			ptype = FormatTools.UINT8;
			
			// get data arrays
			try {
				
				Field dataField = MTBImageRGB.class.getDeclaredField("m_dataR");
				dataField.setAccessible(true);
				bdataR = (byte[][])dataField.get(img);
				
			} catch (NoSuchFieldException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access red channel data of MTBImageRGB:\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access red channel data of MTBImageRGB:\n" + e.getMessage());
			}
			
			try {
				
				Field dataField = MTBImageRGB.class.getDeclaredField("m_dataG");
				dataField.setAccessible(true);
				bdataG = (byte[][])dataField.get(img);
				
			} catch (NoSuchFieldException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access green channel data of MTBImageRGB:\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access green channel data of MTBImageRGB:\n" + e.getMessage());
			}
			
			try {
				
				Field dataField = MTBImageRGB.class.getDeclaredField("m_dataB");
				dataField.setAccessible(true);
				bdataB = (byte[][])dataField.get(img);
				
			} catch (NoSuchFieldException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access blue channel data of MTBImageRGB:\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access blue channel data of MTBImageRGB:\n" + e.getMessage());
			}
		}
		else if (mtbtype == MTBImageType.MTB_INT) {
			ptype = FormatTools.INT32;

			// get data arrays
			try {
				
				Field dataField = MTBImageInt.class.getDeclaredField("m_data");
				dataField.setAccessible(true);
				idata = (int[][])dataField.get(img);
				
			} catch (NoSuchFieldException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access data of MTBImageInt:\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access data of MTBImageInt:\n" + e.getMessage());
			}
		}
		else if (mtbtype == MTBImageType.MTB_DOUBLE) {
			ptype = FormatTools.DOUBLE;

			// get data arrays
			try {
				
				Field dataField = MTBImageDouble.class.getDeclaredField("m_data");
				dataField.setAccessible(true);
				ddata = (double[][])dataField.get(img);
				
			} catch (NoSuchFieldException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access data of MTBImageDouble:\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
						"ImageWriterMTB.writeMTBImage(..): Cannot access data of MTBImageDouble:\n" + e.getMessage());
			}
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageWriterMTB.writeMTBImage(..): " +
					"Unknown image type: " + mtbtype.toString());
		}

		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		int sizeZ = img.getSizeZ();
		int sizeT = img.getSizeT();
		int sizeC = img.getSizeC();
		int sizeStack = img.getSizeStack();

		
		String title = img.getTitle();
		
		// new image writer
		IFormatWriter w = new ImageWriter().getWriter(filename);
		String xml = img.getXML();

		
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
//		IMetadata store = service.createOMEXMLMetadata(xml);
		OMEXMLMetadata store = service.createOMEXMLMetadata(xml);
	
		if (xml == null) {
			store.createRoot();
		}
		else if (store.getImageCount() > 1) {
			// the original dataset had multiple series
			// we need to modify the IMetadata to represent the correct series
			// (a series is one microscopy imaging run, which can be stored with others in one experiment dataset, see e.g. lif-files) 

			ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
			for (int series=0; series<store.getImageCount(); series++) {
				String type = store.getPixelsType(series).toString();
				int pixelType = FormatTools.pixelTypeFromString(type);
				if (pixelType == ptype) {
					String imageName = store.getImageName(series);
					if (title.indexOf(imageName) >= 0) {
						matchingSeries.add(series);
					}
				}
			}

			int series = 0;
			if (matchingSeries.size() > 1) {
				for (int i=0; i<matchingSeries.size(); i++) {
					int index = matchingSeries.get(i);
					String name = store.getImageName(index);
					boolean valid = true;
					for (int j=0; j<matchingSeries.size(); j++) {
						if (i != j) {
							String compName = store.getImageName(matchingSeries.get(j));
							if (compName.indexOf(name) >= 0) {
								valid = false;
								break;
							}
						}
					}
					if (valid) {
						series = index;
						break;
					}
				}
			}
			else if (matchingSeries.size() == 1) 
				series = matchingSeries.get(0);

			// remove all non-matching series entries from the OME XML meta data object
			OMEXMLMetadataRoot root = (OMEXMLMetadataRoot)store.getRoot();
			ome.xml.model.Image exportImage = root.getImage(series);
			List<ome.xml.model.Image> allImages = root.copyImageList();
			for (ome.xml.model.Image image : allImages) {
				if (!image.equals(exportImage)) {
					root.removeImage(image);
				}
			}
			store.setRoot(root);
		}

		store.setImageName((img.getTitle().equals("")) ? filename : img.getTitle(), 0);
	    store.setPixelsSizeX(new PositiveInteger(sizeX), 0);
	    store.setPixelsSizeY(new PositiveInteger(sizeY), 0);
	    store.setPixelsSizeZ(new PositiveInteger(sizeZ), 0);
	    store.setPixelsSizeT(new PositiveInteger(sizeT), 0);
	    store.setPixelsSizeC(new PositiveInteger(sizeC*channels), 0);
	 //   store.setChannelSamplesPerPixel(new PositiveInteger(channels), 0, 0);
	    

		if (store.getImageName(0) == null) {
			store.setImageName(title, 0);
		}
	    
		if (store.getImageID(0) == null) {
			store.setImageID(MetadataTools.createLSID("Image", 0), 0);
		}
		
		if (store.getPixelsID(0) == null) {
			store.setPixelsID(MetadataTools.createLSID("Pixels", 0), 0);
		}

		if (store.getPixelsType(0) == null) {
			store.setPixelsType(PixelType.fromString(FormatTools.getPixelTypeString(ptype)), 0);
		}
		
		if (store.getPixelsBinDataCount(0) == 0 ||
				store.getPixelsBinDataBigEndian(0, 0) == null) {
			// if we don't have any information about bit order, select little endian
			store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
			store.setPixelsBigEndian(Boolean.FALSE, 0);
		}
		
		if (store.getPixelsDimensionOrder(0) == null) {
				store.setPixelsDimensionOrder(DimensionOrder.XYCZT, 0);
		}

		for (int c=0; c<img.getSizeC(); c++) {

			if (c >= store.getChannelCount(0) || store.getChannelID(0, c) == null) {

				String lsid = MetadataTools.createLSID("Channel", 0, c);
				
				store.setChannelID(lsid, 0, c);
			}
			
			if (c >= store.getChannelCount(0) || store.getChannelName(0, c) == null) {

				store.setChannelName(img.getSliceLabel(0, 0, c), 0, c);
			}
			
			store.setChannelSamplesPerPixel(new PositiveInteger(channels), 0, c);
		}

		Calibration cal = img.getCalibration();

		ImageIOUtils.physicalPixelSize_to_OME(cal, store, 0);


		// ------- configure the writer ---------
		
		// hand the meta data object to the writer
		w.setMetadataRetrieve(store);

		// set filename
		w.setId(filename);

//		if (!proc.isDefaultLut()) {
//			w.setColorModel(proc.getColorModel());
//		}

		boolean notSupportedType = !w.isSupportedType(ptype);
		if (notSupportedType) {
			throw new IllegalArgumentException("ImageWriterMTB.writeMTBImage(.): Pixel type '" 
					+ FormatTools.getPixelTypeString(ptype) + "' not supported by this format.");
		}

		if (this.getCompression() != null && ! this.getCompression().isEmpty() ) {
			w.setCompression(this.getCompression());			
		}
		
		// movie writer options
		if (w instanceof AVIWriter || w instanceof QTWriter || w instanceof LegacyQTWriter) {
			if (this.getFps() != null)
				w.setFramesPerSecond(this.getFps());
			
			if (this.getQuality() != null) {
				if (w instanceof QTWriter)
					((QTWriter)w).setQuality(this.getQuality());
				else if (w instanceof LegacyQTWriter)
					((LegacyQTWriter)w).setQuality(this.getQuality());
			}
			
			if (this.getCodec() != null) {
				if (w instanceof QTWriter)
					((QTWriter)w).setCodec(this.getCodec());
				else if (w instanceof LegacyQTWriter)
					((LegacyQTWriter)w).setCodec(this.getCodec());
			}
		}
		
		if (!w.canDoStacks() && sizeStack > 1) {
			int[] sc = img.getCurrentSliceCoords();
			System.err.println("WARNING: ImageWriterMTB.writeMTBImage(.): Image format cannot handle stacks. Writing only slice (z=" + sc[0]
			                             + ",t=" + sc[1] + ",c=" + sc[2] + ").");                                                                  
		}
		
		// convert and save slices
		boolean doStack = w.canDoStacks() && sizeStack > 1;
		int start = doStack ? 0 : img.getCurrentSliceIndex();
		int end = doStack ? sizeStack : start + 1;

		boolean littleEndian =
			!w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
		byte[] plane = null;
		w.setInterleaved(false);

		
		boolean verbose = this.getVerbose();
		
		if (verbose) {
			System.out.println(ImageIOUtils.imgWriteInfo(filename, w, 0));
		}
		
		int no = 0;
		for (int i=start; i<end; i++) {
			if (doStack) {
				this.notifyListeners(new StatusEvent(i, sizeStack, "Saving slice " + (i + 1) + "/" + sizeStack));
				if (verbose) {
					if (i != start)
						System.out.print("\r");
					System.out.print("Saving slice " + (i + 1) + "/" + sizeStack + "...");
				}
			}
			else {
				this.notifyListeners(new StatusEvent("Saving image"));
				if (verbose)
					System.out.print("Saving image...");
			}
			
	//		proc = is.getProcessor(i + 1);

//			if (proc instanceof RecordedImageProcessor) {
//				proc = ((RecordedImageProcessor) proc).getChild();
//			}

//			int x = proc.getWidth();
//			int y = proc.getHeight();

			if (mtbtype == MTBImageType.MTB_DOUBLE) {
				plane = DataTools.doublesToBytes(ddata[i], littleEndian);
			}
			else if (mtbtype == MTBImageType.MTB_INT) {
				plane = DataTools.intsToBytes(idata[i], littleEndian);
			}
			else if (mtbtype == MTBImageType.MTB_RGB) {
				plane = new byte[3 * sizeX * sizeY];
				System.arraycopy(bdataR[i], 0, plane, 0, sizeX * sizeY);
				System.arraycopy(bdataG[i], 0, plane, sizeX * sizeY, sizeX * sizeY);
				System.arraycopy(bdataB[i], 0, plane, 2 * sizeX * sizeY, sizeX * sizeY);
			}

			w.saveBytes(no++, plane);
			
		}
		w.close();

		if (verbose)
			System.out.println(" DONE");

//		MTBPortHashAccess.writeHistory(img, filename);
		MTBOperator.writeHistory(img, filename);
		
	}
	
	/**
	 * Write ImagePlus to disk
	 */
	protected void writeImagePlus(String filename, ImagePlus imp) 
				throws RuntimeException, FormatException, IOException, 
						DependencyException, ServiceException, EnumerationException, ALDOperatorException, ALDProcessingDAGException {
		
		int ptype = 0;		// pixel type ID
		int channels = 1;	// number of channels per pixel (samples per pixel/color channels...), NOT ImagePlus channels
		
		// set pixel type corresponding to the present ImageProcessor type
		switch (imp.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			ptype = FormatTools.UINT8;
			break;
		case ImagePlus.COLOR_RGB:
			channels = 3;
			ptype = FormatTools.UINT8;
			break;
		case ImagePlus.GRAY16:
			ptype = FormatTools.UINT16;
			break;
		case ImagePlus.GRAY32:
			ptype = FormatTools.FLOAT;
			break;
		}
		
		// image title
		String title = imp.getTitle();

		// new image writer
		IFormatWriter w = new ImageWriter().getWriter(filename);
		FileInfo fi = imp.getOriginalFileInfo();
		String xml = fi == null ? null : fi.description == null ? null :
			fi.description.indexOf("xml") == -1 ? null : fi.description;

		
		// ------- prepare the OME XML meta data object ---------
		
		// create a OME XML meta data store
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		IMetadata store = service.createOMEXMLMetadata(xml);

//		if (store == null) {
//			throw new RuntimeException("ImageWriterMTB.writeImagePlus(..): OME-XML Java library not available.");
//		}
		if (xml == null) {
			store.createRoot();
		}
		else if (store.getImageCount() > 1) {
			// the original dataset had multiple series
			// we need to modify the IMetadata to represent the correct series
			// (a series is one microscopy imaging run, which can be stored with others in one experiment dataset, see e.g. lif-files) 

			ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
			for (int series=0; series<store.getImageCount(); series++) {
				String type = store.getPixelsType(series).toString();
				int pixelType = FormatTools.pixelTypeFromString(type);
				if (pixelType == ptype) {
					String imageName = store.getImageName(series);
					if (title.indexOf(imageName) >= 0) {
						matchingSeries.add(series);
					}
				}
			}

			int series = 0;
			if (matchingSeries.size() > 1) {
				for (int i=0; i<matchingSeries.size(); i++) {
					int index = matchingSeries.get(i);
					String name = store.getImageName(index);
					boolean valid = true;
					for (int j=0; j<matchingSeries.size(); j++) {
						if (i != j) {
							String compName = store.getImageName(matchingSeries.get(j));
							if (compName.indexOf(name) >= 0) {
								valid = false;
								break;
							}
						}
					}
					if (valid) {
						series = index;
						break;
					}
				}
			}
			else if (matchingSeries.size() == 1) 
				series = matchingSeries.get(0);

			// remove all non-matching series entries from the OME XML meta data object
//			OME root = (OME) store.getRoot();
			OMEXMLMetadataRoot root = (OMEXMLMetadataRoot)store.getRoot();
			ome.xml.model.Image exportImage = root.getImage(series);
			List<ome.xml.model.Image> allImages = root.copyImageList();
			for (ome.xml.model.Image img : allImages) {
				if (!img.equals(exportImage)) {
					root.removeImage(img);
				}
			}
			store.setRoot(root);
		}

		// set image dimensions
		store.setPixelsSizeX(new PositiveInteger(imp.getWidth()), 0);
		store.setPixelsSizeY(new PositiveInteger(imp.getHeight()), 0);
		store.setPixelsSizeZ(new PositiveInteger(imp.getNSlices()), 0);
		store.setPixelsSizeC(new PositiveInteger(channels*imp.getNChannels()), 0);
		store.setPixelsSizeT(new PositiveInteger(imp.getNFrames()), 0);

		
		if (store.getImageName(0) == null) {
			store.setImageName(title, 0);
		}
		
		if (store.getImageID(0) == null) {
			store.setImageID(MetadataTools.createLSID("Image", 0), 0);
		}
		
		if (store.getPixelsID(0) == null) {
			store.setPixelsID(MetadataTools.createLSID("Pixels", 0), 0);
		}

		if (store.getPixelsType(0) == null) {
			store.setPixelsType(PixelType.fromString(FormatTools.getPixelTypeString(ptype)), 0);
		}

		if (store.getPixelsBinDataCount(0) == 0 ||
				store.getPixelsBinDataBigEndian(0, 0) == null) {
			// if we don't have any information about bit order, select little endian
			store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
			store.setPixelsBigEndian(Boolean.FALSE, 0);
		}
		
		if (store.getPixelsDimensionOrder(0) == null) {
				store.setPixelsDimensionOrder(DimensionOrder.XYCZT, 0);
		}

		String[] labels = imp.getStack().getSliceLabels();
		for (int c=0; c<imp.getNChannels(); c++) {
	
			if (c >= store.getChannelCount(0) || store.getChannelID(0, c) == null) {
				
				String lsid = MetadataTools.createLSID("Channel", 0, c);

				store.setChannelID(lsid, 0, c);
			}
			
			if (c >= store.getChannelCount(0) || store.getChannelName(0, c) == null) {
				if (labels != null && labels[c] != null) {
					store.setChannelName(labels[c], 0, c);
				}
			}
			
			store.setChannelSamplesPerPixel(new PositiveInteger(channels), 0, c);
		}

		Calibration cal = imp.getCalibration();


		ImageIOUtils.physicalPixelSize_to_OME(cal, store, 0);

		if (imp.getImageStackSize() !=
					imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) {
			
			if (!this.getIgnoreInvalidStackSpecification()) {
				throw new IllegalStateException("ImageWriterMTB.writeImagePlus(..): " +
					"The number of slices in the stack (" + imp.getImageStackSize() +
					") does not match the number of expected slices (" +
					(imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) + ").");
			}
			else {
				System.err.println("ImageWriterMTB.writeImagePlus(..): " +
						"The number of slices in the stack (" + imp.getImageStackSize() +
						") does not match the number of expected slices (" +
						(imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) + ")." +
						"\nOnly " + imp.getImageStackSize() +
						" planes will be exported.");
				store.setPixelsSizeZ(new PositiveInteger(imp.getImageStackSize()), 0);
				store.setPixelsSizeC(new PositiveInteger(1), 0);
				store.setPixelsSizeT(new PositiveInteger(1), 0);
			}
		}

		// ------- configure the writer ---------
		
		// hand the meta data object to the writer
		w.setMetadataRetrieve(store);

		// set filename
		w.setId(filename);
		

		// test if the output pixel type is supported by the writer
		ImageProcessor proc = imp.getImageStack().getProcessor(1);
		Image firstImage = proc.createImage();
		firstImage = AWTImageTools.makeBuffered(firstImage, proc.getColorModel());
		int thisType = AWTImageTools.getPixelType((BufferedImage) firstImage);
		if (proc instanceof ColorProcessor) {
			thisType = FormatTools.UINT8;
		}

		if (!proc.isDefaultLut()) {
			w.setColorModel(proc.getColorModel());
		}

		boolean notSupportedType = !w.isSupportedType(thisType);
		if (notSupportedType) {
			throw new IllegalArgumentException("ImageWriterMTB.writeImagePlus(..): Pixel type '" 
					+ FormatTools.getPixelTypeString(thisType) + "' not supported by this format.");
		}
		
		if (this.getCompression() != null && ! this.getCompression().isEmpty() )
			w.setCompression(this.getCompression());

		// movie writer options
		if (w instanceof AVIWriter || w instanceof QTWriter || w instanceof LegacyQTWriter) {
			if (this.getFps() != null)
				w.setFramesPerSecond(this.getFps());
			
			if (this.getQuality() != null) {
				if (w instanceof QTWriter)
					((QTWriter)w).setQuality(this.getQuality());
				else if (w instanceof LegacyQTWriter)
					((LegacyQTWriter)w).setQuality(this.getQuality());
			}
			
			if (this.getCodec() != null) {
				if (w instanceof QTWriter)
					((QTWriter)w).setCodec(this.getCodec());
				else if (w instanceof LegacyQTWriter)
					((LegacyQTWriter)w).setCodec(this.getCodec());
			}
		}
		
		// convert and save slices
		int size = imp.getImageStackSize();
		ImageStack is = imp.getImageStack();
		
		if (!w.canDoStacks() && size > 1) {
			System.err.println("WARNING: ImageWriterMTB.writeImagePlus(.): Image format cannot handle stacks. " +
					"Writing only slice " + imp.getCurrentSlice() + ".");                                                                  
		}
		
		boolean doStack = w.canDoStacks() && size > 1;
		int start = doStack ? 0 : imp.getCurrentSlice() - 1;
		int end = doStack ? size : start + 1;

		boolean littleEndian =
			!w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
		byte[] plane = null;
		w.setInterleaved(false);
		
		boolean verbose = this.getVerbose();
		
		if (verbose) {
			System.out.println(ImageIOUtils.imgWriteInfo(filename, w, 0));
		}
		
		int no = 0;
		for (int i=start; i<end; i++) {
			if (doStack) {
				this.notifyListeners(new StatusEvent(i, size, "Saving slice " + (i + 1) + "/" + size));
				if (verbose) {
					if (i != start)
						System.out.print("\r");
					System.out.print("Saving slice " + (i + 1) + "/" + imp.getStack().getSize() + "...");
				}
			}
			else {
				this.notifyListeners(new StatusEvent("Saving image"));
				if (verbose)
					System.out.print("Saving image...");
			}
			
			proc = is.getProcessor(i + 1);

//			if (proc instanceof RecordedImageProcessor) {
//				proc = ((RecordedImageProcessor) proc).getChild();
//			}

			int x = proc.getWidth();
			int y = proc.getHeight();

			if (proc instanceof ByteProcessor) {
				plane = (byte[]) proc.getPixels();
			}
			else if (proc instanceof ShortProcessor) {
				plane = DataTools.shortsToBytes(
						(short[]) proc.getPixels(), littleEndian);
			}
			else if (proc instanceof FloatProcessor) {
				plane = DataTools.floatsToBytes(
						(float[]) proc.getPixels(), littleEndian);
			}
			else if (proc instanceof ColorProcessor) {
				byte[][] pix = new byte[3][x*y];
				((ColorProcessor) proc).getRGB(pix[0], pix[1], pix[2]);
				plane = new byte[3 * x * y];
				System.arraycopy(pix[0], 0, plane, 0, x * y);
				System.arraycopy(pix[1], 0, plane, x * y, x * y);
				System.arraycopy(pix[2], 0, plane, 2 * x * y, x * y);
			}

			w.saveBytes(no++, plane);
		}
		w.close();


		if (verbose)
			System.out.println(" DONE");

//		MTBPortHashAccess.writeHistory(imp, filename);
		MTBOperator.writeHistory(imp, filename);
	}
	
	/**
	 * Get the available compression methods for the specified file format. Returns null if no compression
	 * options are available or the filename has not been specified yet.
	 */
	public String[] getAvailableCompression() throws FormatException {
		if (this.fileName != null)
			return (new ImageWriter().getWriter(this.fileName.getFileName())).getCompressionTypes();
		else
			return null;
	}
	
	/**
	 * Return available codecs for movie writers (see Bio-Formats package loci.formats.out.QTWriter) 
	 * represented by a hashmap. The key is the integer codec ID and the value
	 * a string with the codec's name. Only quicktime writers have the codec option, for any other writers (.avi or any non-movie
	 * format), this method returns null. This method will also return null, if the filename has not been specified yet.
	 */
	public HashMap<Integer, String> getAvailableCodecs() throws FormatException {
		if (this.fileName != null) {
			IFormatWriter w = new ImageWriter().getWriter(this.fileName.getFileName());
		
			if ((w instanceof LegacyQTWriter) || (w instanceof QTWriter)) {
				return ImageIOUtils.availableCodecs(QTWriter.class);
			}
			else {
				return null;
			}
		}
		else 
			return null;
	}
	
	/**
	 * Return available qualities for quicktime movie writers (see Bio-Formats package loci.formats.out.QTWriter)
	 * represented by a hashmap. The key is the integer quality ID and the value
	 * a string specifying the quality. Only quicktime writers have this option, for any other writers (.avi or any non-movie
	 * format), this method returns null. This method will also return null, if the filename has not been specified yet.
	 */
	public HashMap<Integer, String> getAvailableQualities() throws FormatException {
		if (this.fileName != null) {
			IFormatWriter w = new ImageWriter().getWriter(this.fileName.getFileName());
			
			if ((w instanceof LegacyQTWriter) || (w instanceof QTWriter)) {
				return ImageIOUtils.availableQualities(QTWriter.class);
			}
			else {
				return null;
			}
		}
		else
			return null;
	}
	
	
	
	/**
	 * Set MTBImage to store to disk
	 */
	public void setInputMTBImage(MTBImage img) {
		this.inputMTBImage = img;
		this.inputImagePlus = null;
	}
	
	/**
	 * Get MTBImage that has to be stored to disk
	 */
	public MTBImage getInputMTBImage() {
		return this.inputMTBImage;
	}
	
	
	/**
	 * Set ImagePlus to store to disk
	 */
	public void setInputImagePlus(ImagePlus imp) {
		this.inputImagePlus = imp;
		this.inputMTBImage = null;
	}
	
	/**
	 * Get ImagePlus that has to be stored to disk
	 */
	public ImagePlus getInputImagePlus() {
		return this.inputImagePlus;
	}
	
	/**
	 * Set image filename
	 */
	public void setFileName(String filename) {
		this.fileName = new ALDFileString(filename);
	}
	
	/**
	 * Get image filename
	 */
	public String getFileName() {
		return this.fileName.getFileName();
	}
	
	/**
	 * Set the compression type
	 */
	public void setCompression(String compression) {
		this.compression = compression;
	}
	
	/**
	 * Get the compression type
	 */
	public String getCompression() throws ALDOperatorException {
		return this.compression;
	}
	
	/**
	 * Set the quality (quicktime only). If the writer is not quicktime, the parameter is not assigned.
	 * @param quality quicktime movie quality ID (see Bio-Formats package loci.formats.out.QTWriter)
	 */
	public void setQuality(Integer quality) throws FormatException {
		if (this.getAvailableQualities() != null)
			this.quality = quality;
	}
	
	/**
	 * Get the quality (quicktime only)
	 * @return quicktime movie quality ID(see Bio-Formats package loci.formats.out.QTWriter) or null
	 */
	public Integer getQuality() {
		return this.quality;
	}
	
	/**
	 * Set the codec (quicktime only). If the writer is not quicktime, the parameter is not assigned.
	 * @param codec the quicktime codec ID (see Bio-Formats package loci.formats.out.QTWriter)
	 */
	public void setCodec(Integer codec) throws FormatException {
		if (this.getAvailableCodecs() != null)
			this.codec = codec;
	}
	
	/**
	 * Get the codec (quicktime only)
	 * @return quicktime movie codec ID (see Bio-Formats package loci.formats.out.QTWriter) or null
	 */
	public Integer getCodec() {
		return this.codec;
	}
	
	/**
	 * Set the frames per second for movie writers (*.avi,*.mov). If the filename has not been specified yet or 
	 * the writer is not a movie writer, the parameter is not assigned!
	 * @param fps frames per second of the written movie file (fps must be larger 0, otherwise validation fails)
	 */
	public void setFps(Integer fps) throws FormatException {
		if (this.fileName != null) {
			IFormatWriter w = new ImageWriter().getWriter(this.fileName.getFileName());
			if (w instanceof AVIWriter || w instanceof QTWriter || w instanceof LegacyQTWriter)
				this.fps = fps;
		}
	}
	
	/**
	 * Get the frames per second for movie writers (*.avi,*.mov). Null for non-movie writers
	 * @return frames per second or null
	 */
	public Integer getFps() {
		return this.fps;
	}
	
	/**
	 * Set the overwrite permission flag
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	/**
	 * Get the overwrite permission flag
	 */
	public boolean getOverwrite() {
		return this.overwrite;
	}
	
	/**
	 * Set the flag to ignore an invalid stack specification. This might happen
	 * if slices are added or removed from an image stack, then the stack size does 
	 * not match the product of size in Z, T and C dimension. If the flag is set to 'ignore'(true),
	 * then the image stack is simply written as a stack, not a hyperstack. Default is false. 
	 */
	public void setIgnoreInvalidStackSpecification(boolean ignore) {
		this.ignoreInvalidStackSpec = ignore;
	}
	
	/**
	 * Get the flag to ignore an invalid stack specification. See the <code>setIgnoreInvalidStackSpecification(.)</code>-method
	 * for more explanation
	 */
	public boolean getIgnoreInvalidStackSpecification() {
		return this.ignoreInvalidStackSpec;
	}


	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener listener) {
		this.statusListeners.add(listener);
	}

	@Override
	public void notifyListeners(StatusEvent event) {
		for (StatusListener listener : this.statusListeners) {
			listener.statusUpdated(event);
		}
	}

	@Override
	public void removeStatusListener(StatusListener listener) {
		this.statusListeners.remove(listener);
	}

	@Override
	protected Object readResolve() {
		super.readResolve();
		this.statusListeners = new Vector<StatusListener>(1);
		this.inputMTBImage = null;
		this.inputImagePlus = null;

		return this;
	}
}
