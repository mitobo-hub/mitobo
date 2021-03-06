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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.segmentation;

import java.util.HashMap;
import java.util.EnumSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperatorCollectionElement;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;
import ij.ImagePlus;

/**
 * Abstract super class for operators segmenting roots from images.
 * 
 * @author Birgit Moeller
 */
public abstract class RootImageSegmentationOperator 
	extends ALDOperatorCollectionElement 
{

	public static enum OpWorkingMode {
		/**
		 * Creates completely new annotations and segmentation results, respectively,
		 * which have nothing to do with the input treelines anymore.
		 */
		SEGMENTATION_CREATE,
		/**
		 * Modifies given treelines and does not create any new treelines.
		 */
		SEGMENTATION_UPDATE,
		/**
		 * Updates existing treelines, but may also add some new.
		 */
		SEGMENTATION_CREATE_AND_UPDATE
	}

	public static enum LayerSubset {
		/**
		 * Currently active layer.
		 */
		ACTIVE,
		/**
		 * Layer preceeding active layer.
		 */
		PREVIOUS,
		/**
		 * Layer following the active layer.
		 */
		NEXT,
		/**
		 * All layers.
		 */
		ALL,
		/**
		 * Layers from zero to the one preceeding the active layer.
		 */
		FIRST_TO_PREVIOUS,
		/**
		 * Layers from layer after the active one to last one.
		 */
		NEXT_TO_LAST,
	}

  /**
   * Input images.
	 * <p>
	 * The hashmap stores for layer indices the corresponding images.
   */
  protected HashMap<Integer, ImagePlus> inputImages;
	
  /**
	 * Input treelines to be improved and updated.
	 * <p>
	 * The hashmap stores for layer indices the corresponding treelines.
   */
	protected HashMap<Integer, Vector<MTBRootTree>> inputTreelines;

  /**
	 * Metadata for layers.
	 * <p>
	 * The hashmap stores for each layer available metadata objects.
   */
	protected HashMap<Integer, RhizoProjectLayerMetadataContainer> layerMetadata;

	/**
   * Resulting enhanced treeline annotations.
   */
	protected HashMap<Integer, Vector<MTBRootTree>> resultTreelines;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public RootImageSegmentationOperator() throws ALDOperatorException {
		super();
	}

	/**
	 * Request the set of layers from which to provide images to the operator.
	 * @return	Subset of layers from which the operator wants to get the images.
	 */
	public abstract EnumSet<LayerSubset> getLayerSubsetForInputImages();

	/**
	 * Request the set of layers from which to provide treelines to the operator.
	 * @return	Subset of layers from which the operator wants to get treeline annotations.
	 */
	public abstract EnumSet<LayerSubset> getLayerSubsetForInputTreelines();

	/**
	 * Ask operator if only selected or all treelines should be provided.
	 * @return	True if only selected treelines are desired, otherwise false.
	 */
	public abstract boolean getOnlySelectedTreelines();

	/**
	 * Request working mode of the operator, i.e., how the operator deals with treelines.
	 * @return	Working mode of the operator.
	 */
	public abstract OpWorkingMode getOperatorWorkingMode();
	
	/**
	 * Ask operator if diameter of previous nodes should be transfered to new nodes on update.
	 * @return True if diameter should be transfered, otherwise false.
	 */
	public abstract boolean isToTransferDiameterOnUpdate();
	
	/**
	 * Ask operator if status labels of previous nodes should be transfered to new nodes on update.
	 * @return True if status labels should be transfered, otherwise false.
	 */
	public abstract boolean isToTransferStatusOnUpdate();
	
	/**
	 * Setter for the input image.
	 * @param imgs - ImagePlus images per layer to be processed.
	 */
	public void setInputImages(HashMap<Integer, ImagePlus> images) {
		this.inputImages = images;
	}
	
	/**
	 * Provide treelines to be enhanced.
	 * @param tls		Set of treelines per layer.
	 */
	public void setInputTreelines(HashMap<Integer, Vector<MTBRootTree>> tls) {
		this.inputTreelines = tls;
	}

	/**
	 * Provide layer metadata for each layer.
	 * @param md		Set of metadata objects.
	 */
	public void setLayerMetadata(HashMap<Integer, RhizoProjectLayerMetadataContainer> lmd) {
		this.layerMetadata = lmd;
	}

	/**
	 * Getter for all input images.
	 * @return The map of input images indexed with layer as key.
	 */
	public HashMap<Integer, ImagePlus> getAllInputImages() {
		return this.inputImages;
	}

	/**
	 * Getter for the input image of specified layer.
	 * @param layer		Input layer for which image is requested.
	 * @return The requested input image as an ImagePlus, null if non-existent.
	 */
	public ImagePlus getInputImage(int layer) {
		return this.inputImages.get(layer);
	}
	
	/**
	 * Getter for the input treelines of the specified layer.
	 * @return The map of input treelines indexed with layer as key.
	 */
	public HashMap<Integer, Vector<MTBRootTree>> getAllInputTreelines() {
		return this.inputTreelines;	
	}

	/**
	 * Getter for the input treelines of the specified layer.
	 * @param layer		Input layer for which treelines are requested.
	 * @return List of input treelines of desired layer, null is non-existent.
	 */
	public Vector<MTBRootTree> getInputTreelines(int layer) {
		return this.resultTreelines.get(layer);	
	}

	/**
	 * Getter for complete set of metadata objects of all layers.
	 * @return Set of metadata objects.
	 */
	public HashMap<Integer, RhizoProjectLayerMetadataContainer> getAllLayerMetadata() {
		return this.layerMetadata;
	}

	/**
	 * Getter for layer metadata object of specified layer.
	 * @param layer		Input layer for which metadata is requested.
	 * @return Corresponding metadata object, or null if non-existent.
	 */
	public RhizoProjectLayerMetadataContainer getLayerMetadata(int layer) {
		return this.layerMetadata.get(layer);
	}

	/**
	 * Getter for all result treelines.
	 * @return List of updated treelines.
	 */
	public HashMap<Integer, Vector<MTBRootTree>> getAllResultTreelines() {
		return this.resultTreelines;	
	}

	/**
	 * Getter for the result treelines of a specific layer.
	 * @return List of updated treelines for given layer.
	 */
	public Vector<MTBRootTree> getResultTreelines(int layer) {
		return this.resultTreelines.get(layer);	
	}
	
}
