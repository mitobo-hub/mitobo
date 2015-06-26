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

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageTileAdapter;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;

/**
 * Operator to split an image into tiles and save them to disk.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class ImageToTilesSplitter extends MTBOperator {

	/**
	 * Image to split.
	 */
	@Parameter( label= "Image", required = false,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=1,
      description = "Input image.")
	private MTBImage image = null;
	
	/**
	 * File from where to load the image to split.
	 */
	@Parameter( label= "File name", required = false,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=2,
      description = "Input file.")
	private ALDFileString imgFile = null;

	/**
	 * Size of tiles in x dimension.
	 */
	@Parameter( label= "SizeX", required = true,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=3,
      description = "Tile size in x.")
	private int tileSizeX = 64;
	
	/**
	 * Size of tiles in y dimension.
	 */
	@Parameter( label= "SizeY", required = true,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=4,
      description = "Tile size in y.")
	private int tileSizeY = 64;
	
	/**
	 * Path where to save the tile files.
	 */
	@Parameter( label= "Output path", required = true,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=5,
      description = "Path/filename where to save tiles.")
	private ALDFileString outputPath = new ALDFileString("/tmp/tile");
	
	/**
	 * Empty constructor.
	 */
	public ImageToTilesSplitter() throws ALDOperatorException {
		// nothing to be done here
	}

	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.image == null && this.imgFile == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
					"[ImageToTilesSplitter] Please provide an image or a filename!");
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		if (this.image == null) {
			// read image from disk
			ImageReaderMTB reader;
      try {
	      reader = new ImageReaderMTB(this.imgFile.getFileName());
				reader.runOp();
				this.image = reader.getResultMTBImage();
      } catch (Exception e) {
      	e.printStackTrace();
      	throw new ALDOperatorException(OperatorExceptionType.UNSPECIFIED_ERROR,
      			"[ImageToTilesSplitter] something went wrong on reading the image "
      				+ "\"" + this.imgFile.getFileName() + "\"...");
      }
		}
		// process image
		MTBImageTileAdapter tileAdapt = 
				new MTBImageTileAdapter(this.image,this.tileSizeX,this.tileSizeY);
		tileAdapt.saveTilesToFiles(this.outputPath.getFileName());
	}
}
