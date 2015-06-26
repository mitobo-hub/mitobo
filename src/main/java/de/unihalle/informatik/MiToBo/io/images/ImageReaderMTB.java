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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferFloat;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.DimensionSwapper;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.gui.AWTImageTools;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.tools.ImageIOUtils;

/**
 * Image reader operator to create MTBImage or ImagePlus objects from image files while reading
 * and restoring image history from corresponding history files (.mph). This reader operator is based on Bio-Formats, thus the
 * available formats depend on Bio-Formats (and its available extension).
 * 
 * @author Oliver Gress
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL,level=Level.APPLICATION)
public class ImageReaderMTB extends MTBOperator implements StatusReporter {
	
	private transient Vector<StatusListener> statusListeners = null;
	
	protected transient IFormatReader reader = null;
	protected String omexml = null;
	protected transient IMetadata omemeta = null;
	
	@Parameter( label= "Image index", required = true, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 2,
	        description = "Index of the image (aka series) in the file that has to be read")
	protected int imageIndex = 0;

	@Parameter( label= "Output image type", required = true,  direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 3,
	        description = "Determines the image object class that is created as result image")
	protected OutImageType outImageType = OutImageType.MTB_IMAGE;

	@Parameter( label= "Filename", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
	        description = "Filename of the image to be read")
	protected ALDFileString fileName = null;

	@Parameter( label= "Result ImagePlus", required = false, direction = Direction.OUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 2,
	        description = "Image that has been read from disk returned as ImagePlus")
	protected transient ImagePlus resultImagePlus = null;

	@Parameter( label= "Result MTBImage", required = false, direction = Direction.OUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
	        description = "Image that has been read from disk returned as MTBImage")
	protected transient MTBImage resultMTBImage = null;

	/**
	 * Specifies the image object that is constructed by the reader
	 */
	public enum OutImageType {
		MTB_IMAGE, IMAGE_PLUS, DATASET
	}
	

	public ImageReaderMTB() throws ALDOperatorException {
		super();

		this.statusListeners = new Vector<StatusListener>(1);
	}

	/**
	 * Constructor of an image reader instance by filename.
	 * @param filename
	 * @throws ALDOperatorException
	 * @throws FormatException
	 * @throws IOException
	 * @throws DependencyException
	 * @throws ServiceException
	 */
	public ImageReaderMTB(String filename) throws ALDOperatorException, FormatException, IOException, DependencyException, ServiceException {
		super();
		
		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setFileName(filename);
	}
	
	/**
	 * Init function for deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being deserialized
	 * from file, prior to handing the instance over to the user. It takes care
	 * of a proper initialization of transient member variables as they are not
	 * initialized to the default values during deserialization. 
	 * @return	Initialized deserialized object.
	 */
	@Override
  protected Object readResolve() {
		super.readResolve();

		this.statusListeners = new Vector<StatusListener>(1);
		this.reader = null;
		this.omemeta = null;
		this.resultImagePlus = null;
		this.resultMTBImage = null;
		return this;
	}

	@SuppressWarnings("unused")
  @Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		try {
	    this.initReader();
    } catch (FormatException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
    } catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
    } catch (DependencyException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
    } catch (ServiceException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
    }

		if (this.getVerbose().booleanValue()) {
			System.out.println(ImageIOUtils.imgReadInfo(this.reader));
		}
		
		if (this.getOutImageType() == OutImageType.MTB_IMAGE) {
		
			try {
				
				this.setResultMTBImage(this.readMTBImage(this.getIndexOfImageToRead()));
				
			} catch (IllegalArgumentException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): IllegalArgumentException:\n" + e.getMessage());
			} catch (FormatException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): FormatException:\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): IOException:\n" + e.getMessage());
			} catch (DependencyException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): DependencyException:\n" + e.getMessage());
			} catch (ServiceException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): ServiceException:\n" + e.getMessage());
			}
		}
		else if (this.getOutImageType() == OutImageType.IMAGE_PLUS) {
			try {
				
				this.setResultImagePlus(this.readImagePlus(this.getIndexOfImageToRead()));
				
			} catch (IllegalArgumentException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): IllegalArgumentException:\n" + e.getMessage());
			} catch (FormatException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): FormatException:\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): IOException:\n" + e.getMessage());
			} catch (DependencyException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): DependencyException:\n" + e.getMessage());
			} catch (ServiceException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.operate(): ServiceException:\n" + e.getMessage());
			}
		}
	}
	
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.outImageType.equals(OutImageType.DATASET)) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
					"[ImageReaderMTB] Cannot read IJ 2.0 Dataset, " +
					"use ImageDataReaderMTBIJ2 reader instead!");
		}
		
		if (!(new File(this.getFileName())).exists()) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "MTBImageReader.validateCustom():" +
					" Cannot read file '"+this.getFileName()+"'. File does not exists.");
		}
		
		// reader may not be initialized when OpRunner uses the standard constructor
		// but doesnt set the filename using the setFileName(..)-method
		if (this.reader == null) {
			try {
				this.initReader();
			} catch (FormatException e) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
						"OmeTiffReader.validateCustom(): Reader could not be initialized:\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
						"OmeTiffReader.validateCustom(): Reader could not be initialized:\n" + e.getMessage());
			} catch (DependencyException e) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
						"OmeTiffReader.validateCustom(): Reader could not be initialized:\n" + e.getMessage());
			} catch (ServiceException e) {
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
						"OmeTiffReader.validateCustom(): Reader could not be initialized:\n" + e.getMessage());
			}
		}
		
		if (this.getIndexOfImageToRead() < 0 || this.getIndexOfImageToRead() >= this.getImageCount()) {

			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "MTBImageReader.validateCustom():" +
					" Index of image must be >= 0 and < number of images in the file. Index: " + this.getIndexOfImageToRead() +
					"   Number of images in file: " + this.getImageCount());
		}
	}
	
	/**
	 * Initialize the reader (this method is called in the constructor)
	 */
	protected void initReader() throws FormatException, 
									IOException, DependencyException, ServiceException {
		
		
		this.reader = DimensionSwapper.makeDimensionSwapper(new ImageReader().getReader(this.getFileName()));

		// create a OME XML meta data store
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		this.omemeta = service.createOMEXMLMetadata();
		this.omexml = service.getOMEXML(this.omemeta);

		this.reader.setMetadataStore(this.omemeta);

		this.reader.setId(this.getFileName());
		
	}
	

	/**
	 * Read image <code>imageIdx</code> from the specified file and return it as MTBImage object. Some formats
	 * can store a series of image, e.g. different experiment runs (not to confuse with a time series). The <code>imageIdx</code> specifies
	 * which image of a series has to be read.
	 */
	protected MTBImage readMTBImage(int imageIdx) throws FormatException, IOException, 
							IllegalArgumentException, DependencyException, ServiceException, ALDOperatorException {
		
		if (imageIdx < 0 || imageIdx >= this.reader.getSeriesCount()) {
			throw new IllegalArgumentException("OmeTiffReader.readImagePlus(..): Image index is invalid. Must be >= 0 and < image count.");
		}

		this.reader.setSeries(imageIdx);
		
		
		int pixeltype = this.reader.getPixelType();
		int samplesperpixel = this.reader.getRGBChannelCount();
		
		if ((pixeltype == FormatTools.UINT8 && samplesperpixel != 3) || 
									 pixeltype == FormatTools.UINT16 ||
									 pixeltype == FormatTools.FLOAT) {
			MTBImage image = MTBImage.createMTBImage(this.readImagePlus(imageIdx));
			image.setLocation(this.fileName.getFileName());
			return image;
		}
		
		int nBytes = 0;
		boolean fp = false;
		boolean signed = true;
		
		// determine MTBImageType
		MTBImageType mtbtype = null;
		
		if (pixeltype == FormatTools.INT32) {
				mtbtype = MTBImageType.MTB_INT;
				nBytes = 4;
		}
		else if (pixeltype == FormatTools.DOUBLE) {
				mtbtype = MTBImageType.MTB_DOUBLE;
				nBytes = 8;
				fp = true;
		}
		else if (pixeltype == FormatTools.UINT8 && samplesperpixel == 3) {
			mtbtype = MTBImageType.MTB_RGB;
			nBytes = 1;
			signed = false;
		}
		else if (pixeltype == FormatTools.INT8 && samplesperpixel == 3) {
			mtbtype = MTBImageType.MTB_RGB;
			nBytes = 1;
			signed = true;
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.readMTBImage(..): " +
					"Cannot open image of type '" + FormatTools.getPixelTypeString(pixeltype) + "' with " + 
					samplesperpixel + " samples per pixel as MTBImage.");
		}
		
		// reconfigure dimension order to match ImageJ's XYCZT dimension order
		if (! this.reader.getDimensionOrder().equals("XYCZT")) {

			((DimensionSwapper)this.reader).setOutputOrder("XYCZT");
			if (this.getVerbose().booleanValue()) {
				System.out.println("* Swapping dimensions to dimension order 'XYCZT' in output image");
			}
		}
	
		
		// obtain information about the currently active series (core metadata)
		int nImages = this.reader.getImageCount();  // number of images (slices) of the current series
		int sizeX = this.reader.getSizeX();
		int sizeY = this.reader.getSizeY();
		int sizeZ = this.reader.getSizeZ();
		int sizeT = this.reader.getSizeT();
		int sizeC = this.reader.getSizeC();
		boolean rgb = this.reader.isRGB();
		boolean indexed = this.reader.isIndexed();
		boolean littleEndian = this.reader.isLittleEndian();
		boolean interleaved = this.reader.isInterleaved();
		
		MTBImage img = null;
		if (samplesperpixel == 3 && pixeltype == FormatTools.UINT8) {
			img = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC/samplesperpixel, mtbtype);
		}
		else {
			img = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, mtbtype);
		}

		// read pixel data
		for (int i = 0; i < nImages; i++) {
			
			if (nImages > 1) {
				this.notifyListeners(new StatusEvent(i, nImages, "Reading slice " + (i + 1) + "/" + nImages));
				if (this.verbose.booleanValue()) {
					if (i != 0)
						System.out.print("\r");
					System.out.print("Reading slice " + (i + 1) + "/" + nImages + "...");
				}
			}
			else {
				this.notifyListeners(new StatusEvent("Reading image..."));
				if (this.verbose.booleanValue())
					System.out.print("Reading image...");
			}
	
			
			BufferedImage bimg = AWTImageTools.makeImage(this.reader.openBytes(i), 
						sizeX, sizeY, samplesperpixel, interleaved, nBytes, fp, littleEndian, signed);

			int nEnd = -1;
			
			if (mtbtype == MTBImageType.MTB_INT) {
				
				int[][] data = AWTImageTools.getInts(bimg);
				
				nEnd = sizeC;
				if (data.length > sizeC) {
					System.err.println("ImageReaderMTB.readMTBImage(.): Channel specification does " +
							"not match available channel data. " + data.length + " channels available, " + sizeC + " specified.");
				}
				else if (data.length < sizeC) {
					nEnd = data.length;
					System.err.println("ImageReaderMTB.readMTBImage(.): Channel specification does " +
							"not match available channel data. " + data.length + " channels available, " + sizeC + " specified.");
				}
				
				for (int n = 0; n < nEnd; n++) {
					img.setCurrentSliceIndex(i * samplesperpixel + n);
					
					for (int y = 0; y < sizeY; y++)
						for (int x = 0; x < sizeX; x++)
							img.putValueInt(x, y, data[n][y*sizeX + x]);
				}				
			}
			else if (mtbtype == MTBImageType.MTB_DOUBLE) {
				
				double[][] data = AWTImageTools.getDoubles(bimg);
				
				nEnd = sizeC;
				if (data.length > sizeC) {
					System.err.println("ImageReaderMTB.readMTBImage(.): Channel specification does " +
							"not match available channel data. " + data.length + " channels available, " + sizeC + " specified.");
				}
				else if (data.length < sizeC) {
					nEnd = data.length;
					System.err.println("ImageReaderMTB.readMTBImage(.): Channel specification does " +
							"not match available channel data. " + data.length + " channels available, " + sizeC + " specified.");
				}
				
				for (int n = 0; n < nEnd; n++) {
					img.setCurrentSliceIndex(i * samplesperpixel + n);
				
					for (int y = 0; y < sizeY; y++)
						for (int x = 0; x < sizeX; x++)
							img.putValueDouble(x, y, data[n][y*sizeX + x]);
				}
			}
			else if (mtbtype == MTBImageType.MTB_RGB) {
				img.setCurrentSliceIndex(i);
				
				byte[][] data = AWTImageTools.getBytes(bimg);
				MTBImageRGB rgbimg = (MTBImageRGB)img;

				for (int y = 0; y < sizeY; y++) {
					for (int x = 0; x < sizeX; x++) {
						rgbimg.putValueR(x, y, (0xff & data[0][y*sizeX + x]));
						rgbimg.putValueG(x, y, (0xff & data[1][y*sizeX + x]));
						rgbimg.putValueB(x, y, (0xff & data[2][y*sizeX + x]));
					}
				}
			}
			
			
			if (samplesperpixel == 3 && pixeltype == FormatTools.UINT8) {

				if (i % (sizeC/samplesperpixel) < this.omemeta.getChannelCount(imageIdx) && this.omemeta.getChannelName(0, i % (sizeC/samplesperpixel)) != null)
					img.setCurrentSliceLabel(this.omemeta.getChannelName(imageIdx, i % (sizeC/samplesperpixel)));
			}
			else if (samplesperpixel == 3 && pixeltype == FormatTools.INT8) {

				if (i % (sizeC/samplesperpixel) < this.omemeta.getChannelCount(imageIdx) && this.omemeta.getChannelName(0, i % (sizeC/samplesperpixel)) != null)
					img.setCurrentSliceLabel(this.omemeta.getChannelName(imageIdx, i % (sizeC/samplesperpixel)));
			}
			else {
				
				for (int n = 0; n < nEnd; n++) {
					if ((i+n) % sizeC >= this.omemeta.getChannelCount(imageIdx) || this.omemeta.getChannelName(0, (i+n) % sizeC) == null) 
						img.setCurrentSliceLabel(this.omemeta.getChannelName(imageIdx, (i+n) % sizeC));
				}
			}

		}

		img.setCurrentSliceIndex(0);
	
		img.setTitle(this.omemeta.getImageName(imageIdx));
		
		img.setXML(this.omexml);
		
		img.setLocation(this.fileName.getFileName());
		
		
		// set calibration
		Calibration cal = img.getCalibration();
		
		if (cal == null) {
			cal = new Calibration();
		}
		
		ImageIOUtils.physicalPixelSize_from_OME(cal, this.omemeta, imageIdx);
		
		img.setCalibration(cal);
		
//		MTBPortHashAccess.readHistory(img, this.getFileName());
		ALDOperator.readHistory(img, this.getFileName());
		
		return img;
	}
		
	
	
	/**
	 * Read image <code>imageIdx</code> from the specified file and return it as ImagePlus object. Some formats
	 * can store a series of image, e.g. different experiment runs (not to confuse with a time series). The <code>imageIdx</code> specifies
	 * which image of a series has to be read.
	 */
	@SuppressWarnings("unused")
  protected ImagePlus readImagePlus(int imageIdx) 
			throws FormatException, IOException, DependencyException, 
					ServiceException, IllegalArgumentException, ALDOperatorException {
		
		if (imageIdx < 0 || imageIdx >= this.reader.getSeriesCount()) {
			throw new IllegalArgumentException("ImageReaderMTB.readImagePlus(..): Image index is invalid. Must be >= 0 and < image count.");
		}
		

		this.reader.setSeries(imageIdx);
		
		// reconfigure dimension order to match ImageJ's XYCZT dimension order
		if (! this.reader.getDimensionOrder().equals("XYCZT")) {

			((DimensionSwapper)this.reader).setOutputOrder("XYCZT");
			if (this.getVerbose().booleanValue()) {
				System.out.println("* Swapping dimensions to dimension order 'XYCZT' in output image");
			}
		}	
		
		// obtain information about the currently active series (core metadata)
		int nImages = this.reader.getImageCount();  // number of images (slices) of the current series
		int sizeX = this.reader.getSizeX();
		int sizeY = this.reader.getSizeY();
		int sizeZ = this.reader.getSizeZ();
		int sizeT = this.reader.getSizeT();
		int sizeC = this.reader.getSizeC();
		int samplesperpixel = this.reader.getRGBChannelCount();
		boolean rgb = this.reader.isRGB();
		boolean indexed = this.reader.isIndexed();
		boolean littleEndian = this.reader.isLittleEndian();
		boolean interleaved = this.reader.isInterleaved();	
		int pixeltype = this.reader.getPixelType();
		
		ImagePlus imp = null;
		double[] minmax = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
		boolean floatProc = false;
		
		ImageStack stack = new ImageStack(sizeX, sizeY);
		
		// read pixel data of every slice
		for (int i = 0; i < nImages; i++) {
			
			if (nImages > 1) {
				this.notifyListeners(new StatusEvent(i, nImages, "Reading slice " + (i + 1) + "/" + nImages));
				if (this.verbose.booleanValue()) {
					if (i != 0)
						System.out.print("\r");
					System.out.print("Reading slice " + (i + 1) + "/" + nImages + "...");
				}
			}
			else {
				this.notifyListeners(new StatusEvent("Reading image..."));
				if (this.verbose.booleanValue())
					System.out.print("Reading image...");
			}

			ImageProcessor[] ip = new ImageProcessor[samplesperpixel];

			
			if (pixeltype == FormatTools.UINT8) {
				
				if (samplesperpixel == 1) {
					ip[0] = new ByteProcessor(AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 1, false, littleEndian, false));
				}
				if (samplesperpixel == 3) {
					ip[0] = new ColorProcessor(AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 1, false, littleEndian, false));
				}
				else {
					BufferedImage bi = AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 1, false, littleEndian, false);

					int[] px = new int[samplesperpixel]; 
					
					for (int n = 0; n < samplesperpixel; n++) {
						ip[n] = new ByteProcessor(sizeX, sizeY);
					}
					
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							bi.getRaster().getPixel(x, y, px);
							for (int n = 0; n < samplesperpixel; n++) {
								ip[n].putPixel(x, y, px[n]);
							}
						}
					}
				}
				
			}
			else if (pixeltype == FormatTools.UINT16) {
				
				if (samplesperpixel == 1) {
					ip[0] = new ShortProcessor(AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 2, false, littleEndian, false));
				}
				else {
					BufferedImage bi = AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 2, false, littleEndian, false);
					
					int[] px = new int[samplesperpixel];
					
					for (int n = 0; n < samplesperpixel; n++) {
						ip[n] = new ShortProcessor(sizeX, sizeY);
					}
					
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							bi.getRaster().getPixel(x, y, px);
							for (int n = 0; n < samplesperpixel; n++) {
								ip[n].putPixel(x, y, px[n]);
							}
						}
					}
				}
				
			}
			else if (pixeltype == FormatTools.FLOAT) {
				floatProc = true;
				
				if (samplesperpixel == 1) {
					BufferedImage bi = AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 4, true, littleEndian, true);
					ip[0] = new FloatProcessor(sizeX, sizeY, ((DataBufferFloat)bi.getRaster().getDataBuffer()).getData(), null);
					double m = ip[0].getMin();
					if (m < minmax[0])
						minmax[0] = m;
					m = ip[0].getMax();
					if (m > minmax[1])
						minmax[0] = m;
				}
				else {
					BufferedImage bi = AWTImageTools.makeImage(this.reader.openBytes(i), sizeX, sizeY, samplesperpixel, interleaved, 4, true, littleEndian, true);
					
					float[] px = new float[samplesperpixel];
					
					for (int n = 0; n < samplesperpixel; n++) {
						ip[n] = new FloatProcessor(sizeX, sizeY);
					}
					
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							bi.getRaster().getPixel(x, y, px);
							for (int n = 0; n < samplesperpixel; n++) {
								ip[n].putPixel(x, y, Float.floatToIntBits(px[n]));
							}
						}
					}
				}
			}
			else {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "OmeTiffReader.readImagePlus(..): " +
						"Cannot open image of type '" + FormatTools.getPixelTypeString(pixeltype) + "' with " + 
						samplesperpixel + " samples per pixel as MTBImage.");
			}
			
			if (samplesperpixel == 3 && pixeltype == FormatTools.UINT8) {

				if (i % (sizeC/samplesperpixel) >= this.omemeta.getChannelCount(imageIdx) || this.omemeta.getChannelName(0, i % (sizeC/samplesperpixel)) == null) 
					stack.addSlice("", ip[0]);
				else {
					stack.addSlice(this.omemeta.getChannelName(imageIdx, i % (sizeC/samplesperpixel)), ip[0]);
				}
			}
			else {
				
				for (int n = 0; n < ip.length; n++) {
					if ((i+n) % sizeC >= this.omemeta.getChannelCount(imageIdx) || this.omemeta.getChannelName(0, (i+n) % sizeC) == null) 
						stack.addSlice("", ip[n]);
					else
						stack.addSlice(this.omemeta.getChannelName(imageIdx, (i+n) % sizeC), ip[n]);
				}
			}
		}
		
		String title = this.omemeta.getImageName(imageIdx);
		if (title == null || title.equals(""))
			title = this.omemeta.getImageID(imageIdx);
		if (title == null || title.equals("")) {
			title = this.fileName.getFileName();
			
			if (this.getImageCount() > 1)
				title += " IMG" + imageIdx;
		}
		
//		if (indexed) {
//			
//			if (pixeltype == FormatTools.UINT8 && samplesperpixel != 3) {
//				byte[][] lut = this.reader.get8BitLookupTable();
//				ColorModel cm = new IndexColorModel(8, 256, lut[0], lut[1], lut[2]);
//				
//				stack.setColorModel(cm);
//			}
//			else if (pixeltype == FormatTools.UINT16) {
//				short[][] lut16 = this.reader.get16BitLookupTable();
//				ColorModel cm16 = new Index16ColorModel(16, lut16[0].length, lut16, littleEndian);
//				
//				stack.setColorModel(cm16);
//			}
//		}
		
		imp = new ImagePlus(title, stack);

		if (samplesperpixel == 3 && pixeltype == FormatTools.UINT8)
			imp.setDimensions(sizeC/samplesperpixel, sizeZ, sizeT);
		else 
			imp.setDimensions(sizeC, sizeZ, sizeT);
		
		imp.setOpenAsHyperStack(true);
		
	
		// set file info
		FileInfo fi = imp.getFileInfo();
		
		if (fi == null) {
			fi = new FileInfo();
		}

		fi.description = this.omexml;
		File f = new File(this.fileName.getFileName());
		fi.fileName = f.getName();
		fi.directory = f.getParent();


		if (floatProc)
			fi.displayRanges = minmax;

		imp.setFileInfo(fi);
		

		
		// set calibration
		Calibration cal = imp.getCalibration();
		
		if (cal == null) {
			cal = new Calibration();
		}

//		ImageIOUtils.physicalPixelSize_from_Reader(cal, this.reader, imageIdx);
		
		ImageIOUtils.physicalPixelSize_from_OME(cal, this.omemeta, imageIdx);

		
		imp.setCalibration(cal);
		
		ALDOperator.readHistory(imp, this.getFileName());
		
		return imp;
	}
	
	
	
	
	
	/**
	 * Set resulting image
	 */
	protected void setResultMTBImage(MTBImage img) {
		this.resultMTBImage = img;
	}
	
	/**
	 * Set resulting image
	 */
	protected void setResultImagePlus(ImagePlus img) {
		this.resultImagePlus = img;
	}
	
	/**
	 * Get resulting image that was read from disk.
	 */
	public MTBImage getResultMTBImage() {
		return this.resultMTBImage;
	}
	
	/**
	 * Get resulting image that was read from disk.
	 */
	public ImagePlus getResultImagePlus() {
		return this.resultImagePlus;
	}
	
	/**
	 * Set the filename of the image that has to be read.
	 */
	public void setFileName(String filename) throws FormatException, IOException, DependencyException, ServiceException {
		this.fileName = new ALDFileString(filename);
		
		this.initReader();
	}
	
	/**
	 * Get the filename of the image that has to be read.
	 */
	public String getFileName() {
		return this.fileName.getFileName();
	}
	
	/**
	 * Set the type of image object that is created as result image. Default is OutImageType.MTB_IMAGE
	 */
	public void setOutImageType(OutImageType outtype) {
		this.outImageType = outtype;
	}
	
	/**
	 * Get the type of image object that is created as result image. Default is OutImageType.MTB_IMAGE
	 */
	public OutImageType getOutImageType() {
		return this.outImageType;
	}
	
	/**
	 * Get the number of (multi-dimensional) images stored in the specified file.
	 */
	public int getImageCount() {
		// some files can store multiple series (multiple multi-dimensional images from different trials)
		return this.reader.getSeriesCount();
	}
	
	/**
	 * Get the name of the imageIdx-th image in the file, if available
	 */
	public String getImageName(int imageIdx) {
		if (imageIdx >= 0 && imageIdx < this.omemeta.getImageCount())
			return this.omemeta.getImageName(imageIdx);
		return null;
	}
	
	/**
	 * Get the ID of the imageIdx-th image in the file, if available
	 */
	public String getImageID(int imageIdx) {
		if (imageIdx >= 0 && imageIdx < this.omemeta.getImageCount())
			return this.omemeta.getImageID(imageIdx);
		return null;
	}
	
	/**
	 * Set the index of the image that has to be read from the file. Some files may contain
	 * more than one multi-dimensional image. Default is 0.
	 */
	public void setIndexOfImageToRead(int imageIdx) {
		this.imageIndex = imageIdx;
	}
	
	/**
	 * Get the index of the image that has to be read from the file. Some files may contain
	 * more than one multi-dimensional image. Default is 0.
	 */
	public int getIndexOfImageToRead() {
		return this.imageIndex;
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


	

}
