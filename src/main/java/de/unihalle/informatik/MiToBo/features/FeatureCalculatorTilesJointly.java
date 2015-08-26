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

package de.unihalle.informatik.MiToBo.features;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageTileAdapter;

/**
 * Super class for operators calculating features on images.
 * <p>
 * Operators extending this class are supposed to extract features for 
 * all tiles of a given image simultaneously. Compared to operators just 
 * extending {@link FeatureCalculator} these operators do not extract
 * features for single images, but only for tiles images. Often this is 
 * done in a global fashion: first features are calculated on the 
 * complete image and only subsequently individual tile descriptors are 
 * extracted for all given tiles.
 * <p>
 * The main intention for extending this class will most of the time be
 * efficiency since it is usually more efficient to extract features
 * from the complete image rather than to run a feature operator on each
 * tile individually. Note, however, that not all features are suitable
 * for being calculated in a global fashion, but strictly require
 * processing each tile explicitly.
 * <p>
 * Note that passing only a single tile equal to the size of the image
 * to this operator should result in the same features than running a
 * version of the operator without inherent tiling.  
 * <p>
 * As result operators should return an object of type 
 * {@link FeatureCalculatorResult}. 
 * 
 * @author moeller
 */
public abstract class FeatureCalculatorTilesJointly 
	extends FeatureCalculator
{
	/**
	 * Tiles of the input image to process.
	 */
	@Parameter(label = "Input tiles", required = true, 
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "Set of tiles to process.", dataIOOrder = -10)
	protected transient MTBImageTileAdapter imageTiles = null;
	
	/**
	 * Tiles of supplemental mask.
	 */
	@Parameter(label = "Mask tiles", required = true, 
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "Set of mask tiles to consider.", dataIOOrder = -9)
	protected transient MTBImageTileAdapter maskTiles = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	protected FeatureCalculatorTilesJointly() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Set image tiles to process.
	 * @param iTiles	Image tiles.
	 */
	public void setImageTiles(MTBImageTileAdapter iTiles) {
		this.imageTiles = iTiles;
	}

	/**
	 * Set mask tiles to process.
	 * @param mTiles	Mask tiles.
	 */
	public void setMaskTiles(MTBImageTileAdapter mTiles) {
		this.maskTiles = mTiles;
	}
}
