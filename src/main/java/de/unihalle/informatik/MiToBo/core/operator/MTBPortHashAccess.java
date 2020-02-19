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

package de.unihalle.informatik.MiToBo.core.operator;

import de.unihalle.informatik.Alida.operator.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

import ij.ImagePlus;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** 
 * Interface to the port database of the Alida / MiToBo operator history.
 * <p>
 * This class implements an interface to access the history database.
 * It yields the only possibility to access the database, direct access
 * is blocked to guarantee database consistency.
 * <p>
 * Compared to the superclass a special treatment of MiToBo images takes place 
 * to ensure proper association of MiToBo images and underlying ImageJ 
 * ImagePlus data. Objects are usually registered by their references in the 
 * port database, however, in case of MiToBo images and underlying ImagePlus 
 * data different object references, i.e. the MTBImage and the ImagePlus, 
 * essentially represent the same data in the system. 
 * Hence, for history database validity explicit associations need to be 
 * established between ImagePlus and MTBImage data.  
 * <p>
 * Database consistency is ensured by introducing a specialized data type 
 * which is the <code>MTBHistoryImageBox</code>. If a MTBImage is fed into an 
 * operator as parameter the first time, a corresponding image box is created 
 * and registered in the local hash. 
 * Lateron, when the ImagePlus object linked to the MTBImage is directly 
 * used as parameter, the MiToBo images within all boxes are searched to find
 * the corresponding MTBImage. Then the history port associated with the 
 * box is updated, ensuring history consistency and at the same time 
 * proper association of the ImagePlus with the formerly known MTBImage.
 * <p>
 * In case of using an ImagePlus as parameter that has not been seen before,
 * the same mechanism is applied. First an image box is initialized for the 
 * new ImagePlus. When at a later point in time the MTBImage is used as 
 * parameter which is associated to the formerly seen ImagePlus, the 
 * association is explicitly established by linking the MTBImage to the box 
 * and updating the history port of the box accordingly.
 * 
 * @author moeller
 * @author posch
 */
public class MTBPortHashAccess extends ALDPortHashAccess {

	/**
	 * If true, verbose outputs are written to standard output.
	 */
	@SuppressWarnings("unused")
	private static boolean verbose = false;
	
	/** 
	 * Default extension of MiToBo processing history file.
	 */
	public static final String MPH_EXTENSION = new String("mph");

	/**
   * Associative list for links between MTBImages and associated image boxes.
   * <p>
   * The hash provides fast access to stored image boxes by keeping track
   * of all included MiToBo images as keys in the map. 
   */
  private static WeakHashMap<MTBImage, MTBHistoryImageBox> boxLinksMTBImage =
          new WeakHashMap<MTBImage, MTBHistoryImageBox>();
  
  /**
   * Associative list for links between ImagePlus and associated image boxes.
   * <p>
   * The hash provides fast access to stored image boxes by keeoing track  
   * of all included ImagePlus images as keys in the map.
   */
  private static WeakHashMap<ImagePlus, MTBHistoryImageBox> boxLinksImagePlus =
          new WeakHashMap<ImagePlus, MTBHistoryImageBox>();

	/** 
	 * Constructor without function.
	 * <p>
	 * Note that there will be only one processing history per session
	 * and not many different objects of this type.
	 */
	protected MTBPortHashAccess() {
		// nothing to be done here
	}

	/**
	 * Checks if an object is registered in the database.
	 * @param obj	Object to check.
	 * @return	True, if object is known to the database.
	 */
	@Override
	protected boolean isRegistered(Object obj) {
		if ((obj instanceof MTBImage) || (obj instanceof ImagePlus))
			return isRegisteredImageBox(obj);
		return super.isRegistered(obj);
	}
	
	/**
	 * Registers the object to the database.
	 * @param obj	Object to register.
	 */
	@Override
  protected void register(Object obj) {
		if ((obj instanceof MTBImage) || (obj instanceof ImagePlus))
			registerImageBox(obj);
		else 
			super.register(obj);
	}

	/** 
	 * Gets the port to which the object is currently linked in history.
	 * @return Current port to which data is linked.
	 */
	@Override
	protected ALDPort getHistoryLink(Object obj) { 
		if ((obj instanceof MTBImage) || (obj instanceof ImagePlus)) {
			MTBHistoryImageBox ibox = getImageBox(obj);
			if (ibox == null) 
				return null;
//			return historyAnchors.get(ibox);
			return super.getHistoryLink(ibox);
		}
		return super.getHistoryLink(obj);
	}

	/** 
	 * Sets the port to which the object is to be linked in history.
	 * @param port New port the data object is to be linked to.
	 */
	@Override
	protected void setHistoryLink(Object obj, ALDPort port) { 
		if (port != null) {
			// handle images separately
			if ((obj instanceof MTBImage) || (obj instanceof ImagePlus)) {
				if (!isRegisteredImageBox(obj))
					registerImageBox(obj);
				MTBHistoryImageBox ibox = getImageBox(obj);
//				historyAnchors.put(ibox, port);
				super.setHistoryLink(ibox, port);
			}
			else {
				super.setHistoryLink(obj, port);
			}
		}
	}

	/**
	 * Checks if the given image object is already registered.
	 * <p>
	 * An image is registered if a corresponding image container exists.
	 * 
	 * @param obj	Image object in question.
	 * @return	True, if an associated image box already exists.
	 */
	private static boolean isRegisteredImageBox(Object obj) {
		if (obj instanceof MTBImage) {
			
			// first case: MTBImage registered directly
			if (boxLinksMTBImage.containsKey(obj))
				return true;

			// second case: corresponding ImagePlus is registered
			//     => make link between ImagePlus and MTBImage explicit
			ImagePlus imp = null;
			
			// only get the ImagePlus if one is available,
			// otherwise one would be created
			if (((MTBImage)obj).hasImagePlus())
				imp = ((MTBImage)obj).getImagePlus();
			
			if (   imp != null 
					&& boxLinksImagePlus.containsKey(imp)) {
				MTBHistoryImageBox mbox = boxLinksImagePlus.get(imp);
				mbox.setMTBImage((MTBImage)obj);
				boxLinksMTBImage.put((MTBImage)obj, mbox);
				return true;
			}
		}
		else if (obj instanceof ImagePlus) {
			// an ImagePlus object can only be registered directly as there is no
			// way to get an associated MTBImage from the ImagePlus object
			return boxLinksImagePlus.containsKey(obj);
		}
		else {
			System.err.println("MTBHistoryDB:isRegisteredImageBox - " +
					"object is neither of type MTBImage nor of type ImagePlus...!");
			return false;
		}
		return false;
	}

	/**
	 * Registers the given (image) object.
	 * <p>
	 * Image objects are treated in a special way as different physical objects
	 * may relate to the same logical object. In particular, images are boxed
	 * in containers where each container keeps track of a pair of 
	 * MTBImage/ImagePlus objects. 
	 * 
	 * @param obj	Object to register.
	 */
	private void registerImageBox(Object obj) {
		
		// check if box already exists, if not, initialize
		if (obj instanceof MTBImage) {
			
			if (!boxLinksMTBImage.containsKey(obj)) {

				// get the associated ImagePlus, if available
				ImagePlus imp = null;
				if (((MTBImage)obj).hasImagePlus())
					imp = ((MTBImage)obj).getImagePlus();
				if (imp != null) {
					
					// check if the ImagePlus is already known...
					if (boxLinksImagePlus.containsKey(imp)) {						
						// if so, just update the box...
						MTBHistoryImageBox mbox = boxLinksImagePlus.get(imp);
						mbox.setMTBImage((MTBImage)obj);
						boxLinksMTBImage.put((MTBImage)obj, mbox);
					}
					else {
						// otherwise instantiate a new box
						MTBHistoryImageBox mbox= new MTBHistoryImageBox((MTBImage)obj,imp);
						boxLinksImagePlus.put(imp, mbox);
						boxLinksMTBImage.put((MTBImage)obj, mbox);
//						MTBPortHash.setHistoryLink(mbox, new MTBDataPort(mbox));
						super.setHistoryLink(mbox, new ALDDataPort(mbox));
					}
				}
				else {
					// if there is no ImagePlus we also have to init a new box
					MTBHistoryImageBox mbox= new MTBHistoryImageBox((MTBImage)obj);
					boxLinksMTBImage.put((MTBImage)obj, mbox);
//					MTBPortHash.setHistoryLink(mbox, new MTBDataPort(mbox));
					super.setHistoryLink(mbox, new ALDDataPort(mbox));
				}
			}
		}
		else if (obj instanceof ImagePlus) {

			// check if there is a box for the ImagePlus available
			if (!boxLinksImagePlus.containsKey(obj)) {				
				// if not, init a new one
				MTBHistoryImageBox mbox= new MTBHistoryImageBox((ImagePlus)obj);
				boxLinksImagePlus.put((ImagePlus)obj, mbox);
//				MTBPortHash.setHistoryLink(mbox, new MTBDataPort(mbox));
				super.setHistoryLink(mbox, new ALDDataPort(mbox));
			}
		}
	}
	
	/**
	 * Gets the image container associated with the given image object.
	 * @return Image box associated with the given object.
	 */
	private static MTBHistoryImageBox getImageBox(Object obj) {
		if (obj instanceof MTBImage) {
			return boxLinksMTBImage.get(obj);
		}
		return boxLinksImagePlus.get(obj);
	}
	
	/** 
	 * Database object boxing ImagePlus and MTBImage.
	 * <p>
	 * In MiToBo the data type <code>MTBImage</code> most of the time acts like 
	 * a wrapper for an underlying ImagePlus object. In addition, an object of 
	 * type ImagePlus is required anyway to display the MTBImage to the user. 
	 * Currently all changes done to either the MTBImage or the corresponding 
	 * ImagePlus object trigger changes in the associated object as well.
	 * <p>
	 * The MiToBo history tracks operations performed on an object during its
	 * lifetime assuming a unique identifier for each object. In case of 
	 * MTBImage this is not true anymore as the image can be accessed either 
	 * as MTBImage or as ImagePlus. These are physically two different objects,
	 * however, should logically be treated the same. Consequently, the history
	 * database needs to explicitly link both objects to each other and treat 
	 * them internally as one single object.
	 * <p>
	 * This class implements a container for a pair of associated images of type
	 * MTBImage and ImagePlus. Whenever the database is queried for an object of
	 * either of both types, the query is redirected to the associated container
	 * object. Each container object owns a unique dataport and a history, if
	 * available, i.e. may be treated as any other object in the database.
	 * 
	 * @author moeller
	 */
	private static class MTBHistoryImageBox {

		/**
		 * MTBImage object in container.
		 */
		private WeakReference<MTBImage> mtbImageRef = null;

		/**
		 * ImagePlus object in container.
		 */
		private WeakReference<ImagePlus> imgPlusRef = null;

		/**
		 * Default constructor for an image pair.
		 * @param m	MTBImage object of the pair.
		 * @param i	ImagePlus object of the pair.
		 */
		protected MTBHistoryImageBox(MTBImage m, ImagePlus i) {
			this.mtbImageRef = new WeakReference<MTBImage>(m); 
			this.imgPlusRef = new WeakReference<ImagePlus>(i);
		}
		
		/**
		 * Default constructor for an MTBImage without associated ImagePlus.
		 * @param m	MTBImage object.
		 */
		protected MTBHistoryImageBox(MTBImage m) {
			this.mtbImageRef = new WeakReference<MTBImage>(m);
			this.imgPlusRef = null;
		}
		
		/**
		 * Default constructor for an ImagePlus without linked MTBImage.
		 * @param m	MTBImage object.
		 */
		protected MTBHistoryImageBox(ImagePlus i) {
			this.mtbImageRef = null;
			this.imgPlusRef = new WeakReference<ImagePlus>(i);
		}

		/**
		 * Stores given image to the container.
		 * @param mimg	MTBImage to be stored in container.
		 */
		protected void setMTBImage(MTBImage mimg) {
			this.mtbImageRef = new WeakReference<MTBImage>(mimg);
		}

		/**
		 * Stores given image to the container.
		 * @param mimg	ImagePlus to be stored in container.
		 */
		@SuppressWarnings("unused")
		protected void setImagePlus(ImagePlus imp) {
			this.imgPlusRef = new WeakReference<ImagePlus>(imp);
		}
		
		/**
		 * Gets the stored ImagePlus.
		 * @return Stored ImagePlus, maybe <code>null</code>.
		 */
		@SuppressWarnings("unused")
		protected ImagePlus getImagePlus() {
			return this.imgPlusRef.get();
		}
		
		/**
		 * Gets the stored MTBImage.
		 * @return Stored MTBImage, maybe <code>null</code>.
		 */
		@SuppressWarnings("unused")
		protected MTBImage getMTBImage() {
			return this.mtbImageRef.get();
		}
		
		/**
		 * Checks if the box contains a MTBImage.
		 * @return True, if the box contains an image of type MTBImage.
		 */
		@SuppressWarnings("unused")
		protected boolean containsMTBImage() {
			return this.mtbImageRef != null;
		}
		
		/**
		 * Checks if the box contains an ImagePlus.
		 * @return True, if the box contains an image of type ImagePlus.
		 */
		@SuppressWarnings("unused")
		protected boolean containsImagePlus() {
			return this.imgPlusRef != null;
		}
	}	
}
