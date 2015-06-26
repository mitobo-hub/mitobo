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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import java.util.Iterator;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * Adapter class to support tile-wise processing of images.
 * <p>
 * The class performs lazy evalutation, i.e. only actually instantiates a 
 * tile image upon request. However, the tiles are not cached, i.e. on each
 * call to {@link #getTile(int, int)} or {@link #getAllTiles()} new images 
 * are generated.
 * 
 * @author moeller
 */
public class MTBImageTileAdapter implements Iterable<MTBImage> {
	
	/**
	 * Size of a tile in x-dimension.
	 * <p>
	 * Note that the last tile might be smaller if the image size in x is not
	 * dividable by the tile size without remainder. 
	 */
	@Parameter( label = "Size X", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Size X")
	protected int tileSizeX = 64;

	/**
	 * Size of a tile in y-dimension.
	 * <p>
	 * Note that the last tile might be smaller if the image size in y is not
	 * dividable by the tile size without remainder. 
	 */
	@Parameter( label = "Size Y", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Size Y")
	protected int tileSizeY = 64;

	/**
	 * Offset of the tiles in x dimension.
	 * <p>
	 * If the offset is smaller than the specified {@link #tileSizeX}, tiles are
	 * overlapping. If it is larger, gaps result.
	 */
	@Parameter( label = "Shift X", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Shift X")
	protected int shiftX = 32;

	/**
	 * Offset of the tiles in y dimension.
	 * <p>
	 * If the offset is smaller than the specified {@link #tileSizeY}, tiles are
	 * overlapping. If it is larger, gaps result.
	 */
	@Parameter( label = "Shift Y", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Shift Y")
	protected int shiftY = 32;

	/**
	 * Image to process.
	 */
	@Parameter(label= "Input Image", required = true,
			direction = Parameter.Direction.IN, description = "Input Image")
	protected MTBImage inImg = null;
	
	/*
	 * some local helper variables
	 */
	
	/**
	 * Number of tile rows.
	 */
	protected int tileRows;
	
	/**
	 * Number of tile columns.
	 */
	protected int tileColumns;

	/**
	 * Default constructor.
	 * @param img				Image to be tiled.
	 * @param sizeX			Tile size in x dimension.
	 * @param sizeY			Tile size in y dimension.
	 * @param shiftInX Tile offset in x dimension.
	 * @param shiftInY Tile offset in y dimension.
	 */
	public MTBImageTileAdapter(MTBImage img, int sizeX, int sizeY, 
			int shiftInX, int shiftInY) {
		this.inImg = img;
		this.tileSizeX = sizeX;
		this.tileSizeY = sizeY;
		this.shiftX = shiftInX;
		this.shiftY = shiftInY;
		this.tileRows = this.inImg.getSizeY()/this.shiftY;
		if (this.inImg.getSizeY() % this.shiftY != 0)
			++this.tileRows;
		this.tileColumns = this.inImg.getSizeX()/this.shiftX;
		if (this.inImg.getSizeX() % this.shiftX != 0)
			++this.tileColumns;
	}
	
	/**
	 * Default constructor.
	 * @param img			Image to be tiled.
	 * @param sizeX		Tile size in x dimension.
	 * @param sizeY		Tile size in y dimension.
	 */
	public MTBImageTileAdapter(MTBImage img, int sizeX, int sizeY) {
		// if no shift are given, non-overlapping tiles are used
		this(img, sizeX, sizeY, sizeX, sizeY);
	}

	/**
	 * Returns number of tiles and rows, respectively, in y-dimension.
	 * @return	Number of y-tiles.
	 */
	public int getTileRows() {
		return this.tileRows;
	}

	/**
	 * Returns number of tiles and colums, respectively, in x-dimension.
	 * @return	Number of x-tiles.
	 */
	public int getTileCols() {
		return this.tileColumns;
	}

	/**
	 * Returns tile with given indices.
	 * @param idX		Column index.
	 * @param idY		Row index.
	 * @return	Tile image.
	 */
	public MTBImage getTile(int idX, int idY) {
		// calculate coordinates of first pixel column and row
		int startRow = idY * this.shiftX;
		int startCol = idX * this.shiftY;
		// calculate last row and col considering image border
		int endRow = startRow + this.tileSizeY - 1;
		if (endRow >= this.inImg.getSizeY() - 1)
			endRow = this.inImg.getSizeY() - 1;
		int endCol = startCol + this.tileSizeX - 1;
		if (endCol >= this.inImg.getSizeX() - 1)
			endCol = this.inImg.getSizeX() - 1;
		// extract subimage (but do not log implicit operator call)
		int constructionMode = ALDOperator.getConstructionMode();
		ALDOperator.setConstructionMode(2);
		MTBImage tile = this.inImg.getImagePart(startCol, startRow, 0, 0, 0, 
				endCol-startCol+1, endRow-startRow+1, 1, 1, 1);
		ALDOperator.setConstructionMode(constructionMode);
		return tile;
	}
	
	/**
	 * Generates and returns array of all tiles.
	 * <p>
	 * The tiles are arranged in the same order like in the image, i.e. the 
	 * first index refers to the column and the second index to the row.
	 * 
	 * @return	All tiles of image.
	 */
	public MTBImage[][] getAllTiles() {
		MTBImage[][] array = new MTBImage[this.tileRows][this.tileColumns];
		for (int y=0;y<this.tileRows;++y)
			for (int x=0;x<this.tileColumns;++x)
				array[y][x] = this.getTile(x, y);
		return array;
	}

	/**
	 * Saves all tiles to the given path.
	 * <p>
	 * The tiles are saved in .tiff format. The file names are formed by the
	 * prefix followed by '-<runningID>' with suffix '.tif'.
	 * Note that internally the operator {@link ImageWriterMTB} is used,
	 * however, the calls are not documented in the processing history.
	 *
	 * @param prefix Path or filename where to save the images.
	 */
	public void saveTilesToFiles(String prefix) {
		ImageWriterMTB writer;
		int runningNum = 0;
		String filename = new String();
		for (MTBImage tile: this) {
			try {
				filename = prefix + "-" + runningNum + ".tif";
				writer = new ImageWriterMTB(tile, filename);
				writer.runOp(HidingMode.HIDDEN);
			} catch (ALDException e) {
				System.err.println("[MTBImageTileAdapter] Something went wrong on " 
						+ "saving tile to " + filename + ", skipping tile!");
			}
			++runningNum;
		}
	}
	
	@Override
  public Iterator<MTBImage> iterator() {
		return new TileIterator(this);
  }
	
	/**
   * Iterates from top-left to bottom-right over the tiles of an image.
   * 
   * @author moeller
   */	
	public class TileIterator implements Iterator<MTBImage> {

		/**
		 * Adapter object to be iterated over.
		 */
		private MTBImageTileAdapter adapter;
		
		/**
		 * Index of iterator in set.
		 */
		private int currentIndexCol = -1;

		/**
		 * Index of iterator in set.
		 */
		private int currentIndexRow = 0;

		/**
		 * Default constructor.
		 */
		public TileIterator(MTBImageTileAdapter tadapter) {
			this.adapter = tadapter;
		}
		
		@Override
		public boolean hasNext() {
			return (    this.currentIndexCol < this.adapter.getTileCols()-1
					     || (this.currentIndexRow < this.adapter.getTileRows()-1));
		}

		@Override
		public MTBImage next() {
			this.currentIndexCol++;
			if (this.currentIndexCol == this.adapter.getTileCols()) {
				this.currentIndexCol = 0;
				this.currentIndexRow++;
			}
			return 
				this.adapter.getTile(this.currentIndexCol, this.currentIndexRow);
		}

		@Override
		public void remove() {
			// not supported...
		}
	}
}