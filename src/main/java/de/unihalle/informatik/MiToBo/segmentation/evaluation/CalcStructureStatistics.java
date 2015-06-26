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

package de.unihalle.informatik.MiToBo.segmentation.evaluation;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Extracts statistics from cell and structure segmentation results.
 * <p>
 * For each structure region a corresponding region in the given cell 
 * segmentation result is detected and the structure region assigned to it.
 * Note that structure regions along image borders are ignored. In addition,
 * optionally also regions in given nuclei regions might be excluded if a 
 * mask with detected nuclei regions is available.
 * <p>
 * In parts the code of this file has been copied from Oliver's tool
 * 'EvalParticleDetection'. However, this class is more generic in the sense 
 * that only a single cell segmentation result and a single structure
 * segmentation result are processed on call of this tool.
 * 
 * @author moeller
 *
 */

/*
 * Note: This class is not annotated as ALDAOperator because it should stay 
 * 			 hidden in the package. If annotated the constructor must be public,
 *       and this is not desired.
 */
public class CalcStructureStatistics extends MTBOperator {

	/**
	 * Mode how to assign structures to cell regions.
	 * 
	 * @author moeller
	 */
	public static enum StructureCountMode {
		/**
		 * Use COM to determine to which cell a structure belongs (default).
		 */
		COUNT_COM,
		/**
		 * Use largest overlap to determine corresponding cell region.
		 */
		COUNT_OVERLAP
	}
	
	/**
	 * Structure mask.
	 */
	@Parameter( label= "structureMask",required = true,direction = Direction.IN,
			description = "Binary mask of structures.")
	private MTBImageByte structureMask = null;

	/**
	 * Nuclei mask.
	 */
	@Parameter( label= "nucleiMask", required = false, direction = Direction.IN,
			description = "Binary mask of nuclei.")
	private MTBImageByte nucleiMask = null;

	/**
	 * Label image of cell areas.
	 */
	@Parameter( label= "cellLabelImg", required = true, direction = Direction.IN,
			description = "Label image of cell areas.")
	private MTBImageByte cellLabelImg = null;

	/**
	 * Mode for assigning structures to regions.
	 */
	@Parameter( label= "countMode", required = false, direction = Direction.IN,
			description= "Mode for assigning structures to regions.")
	private StructureCountMode countMode = StructureCountMode.COUNT_COM;	
	
	/**
	 * Minimal size of structure regions to be considered.
	 */
	@Parameter( label="minStructureSize",required=false,direction=Direction.IN,
			description= "Minimal size of structure regions to be considered.")
	private int minStructureSize = 0;

	/**
	 * Flag to ignore structures along border.
	 */
	@Parameter( label= "ignoreStructsAtBorder", required = false, 
			direction = Direction.IN,
			description= "Flag for ignoring structures along image border.")
	private boolean ignoreStructsAtBorder = false;

	/**
	 * Result data: counts.
	 */
	@Parameter( label="resultDataCounts",required=true,direction=Direction.OUT,
			description = "Result data for counts per cell.")
	private HashMap<Integer,Integer> resultDataCounts = null;

	/**
	 * Result data: total structure size per cell.
	 */
	@Parameter( label= "resultDataTotalSize", required = true, 
			direction = Direction.OUT, 
			description = "Result data for total structure size per cell.")
	private HashMap<Integer,Integer> resultDataTotalSize = null;

	/**
	 * Result data: average size.
	 */
	@Parameter( label= "resultDataAvgSize", required = true, 
			direction = Direction.OUT, 
			description = "Result data for average size per cell.")
	private HashMap<Integer,Double> resultDataAvgSize = null;

	/**
	 * Result data: area fractions.
	 */
	@Parameter( label= "resultDataAreaFractions", required = true, 
			direction = Direction.OUT, 
			description = "Result data for area fraction per cell.")
	private HashMap<Integer,Double> resultDataAreaFractions = null;

	/**
	 * Result data: cell sizes.
	 */
	@Parameter( label= "resultDataCellSizes", required = true, 
			direction = Direction.OUT, 
			description = "Result data for size per cell.")
	private HashMap<Integer,Integer> resultDataCellSizes = null;

	/* 
	 * local helpers
	 */
	private int width;
	
	private int height;
	
	private HashMap<Integer, MTBRegion2DSet> cparticles = 
		new HashMap<Integer, MTBRegion2DSet>();
	
	private boolean ignoreStructsInNuclei = false;

	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException
	 */
	protected CalcStructureStatistics() throws ALDOperatorException {
	  super();
  }

	/**
	 * Default constructor.
	 *
	 * @param sMask		Binary mask of detected structures.
	 * @param nMask		Binary mask of detected nuclei.
	 * @param labelImg	Label image of detected cell areas.
	 * @param ignoreNuclei	Flag for ignoring nuclei regions.
	 * @throws ALDOperatorException
	 */
	@Deprecated
	public CalcStructureStatistics(MTBImageByte sMask, MTBImageByte nMask,
			MTBImageByte labelImg, boolean ignoreNuclei) 
		throws ALDOperatorException {
		this.structureMask = sMask;
		this.cellLabelImg = labelImg;
		if (nMask != null)
			this.nucleiMask = nMask;
		this.ignoreStructsInNuclei = ignoreNuclei;
  }

	/**
	 * Default constructor.
	 *
	 * @param sMask		Binary mask of detected structures.
	 * @param nMask		Binary mask of detected nuclei.
	 * @param labelImg	Label image of detected cell areas.
	 * @throws ALDOperatorException
	 */
	public CalcStructureStatistics(MTBImageByte sMask, MTBImageByte nMask,
			MTBImageByte labelImg) 
		throws ALDOperatorException {
		this.structureMask = sMask;
		this.cellLabelImg = labelImg;
		if (nMask != null) {
			this.nucleiMask = nMask;
			this.ignoreStructsInNuclei = true;
		}
  }

	/**
	 * Specify mode how to assign structures to cells.
	 */
	public void setCountMode(StructureCountMode m) {
		this.countMode = m;
	}
	
	/**
	 * Specify minimal size of regions considered.
	 */
	public void setMinimalRegionSize(int minsize) {
		this.minStructureSize = minsize;
	}

	/**
	 * Set flag to include/ignore structures along image border.
	 */
	public void setIgnoreBorderFlag(boolean f) {
		this.ignoreStructsAtBorder = f;
	}
	
	/**
	 * Returns result data object with counts per cell.
	 */
	public HashMap<Integer,Integer> getResultDataCounts() {
		return this.resultDataCounts;
	}
	
	/**
	 * Returns result data object with total structure size per cell.
	 */
	public HashMap<Integer,Integer> getResultDataTotalSize() {
		return this.resultDataTotalSize;
	}

	/**
	 * Returns result data object with area fractions per cell.
	 */
	public HashMap<Integer,Double> getResultDataAreaFractions() {
		return this.resultDataAreaFractions;
	}

	/**
	 * Returns result data object with areas per cell.
	 */
	public HashMap<Integer,Integer> getResultDataCellSizes() {
		return this.resultDataCellSizes;
	}

	/**
	 * Returns result data object with average size per cell.
	 */
	public HashMap<Integer,Double> getResultDataAvgSize() {
		return this.resultDataAvgSize;
	}

	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		this.height = this.cellLabelImg.getSizeY();
		this.width = this.cellLabelImg.getSizeX();
		
		// check if nuclei data is available...
		if (this.nucleiMask == null)
			this.ignoreStructsInNuclei = false;
		
		// initialize labels vector and region vectors
		Vector<Integer> clabels = new Vector<Integer>();
		HashMap<Integer, Integer> cellSizes = new HashMap<Integer, Integer>();
		
		int label;
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				label = this.cellLabelImg.getValueInt(x, y);
				Integer key = new Integer(label);
				if (!this.cparticles.containsKey(key)) {
					this.cparticles.put(key,
							new MTBRegion2DSet(0,0,this.width-1,this.height-1));
					clabels.add(key);
				}	
				// determine size of region
				if (!cellSizes.containsKey(key)) {
					cellSizes.put(key, new Integer(1));
				}
				else {
					cellSizes.put(key, new Integer(cellSizes.get(key).intValue()+1));
				}
			}
		}
		// sort label vector
		Integer[] Ilabels = new Integer[clabels.size()];
		Ilabels = clabels.toArray(Ilabels);
		int[] ilabels = new int[Ilabels.length];
		for (int i = 0; i < Ilabels.length; i++)
			ilabels[i] = Ilabels[i].intValue();
		Arrays.sort(ilabels);
		
		// label the structure image
		LabelComponentsSequential labler = 
			new LabelComponentsSequential(this.structureMask, true);
		labler.runOp(false);
		MTBRegion2DSet regs = labler.getResultingRegions();
		
		// assign structures to cells
		switch(this.countMode)
		{
		case COUNT_COM:
			this.assignStructsToCell_com(regs);
			break;
		case COUNT_OVERLAP:
			this.assignStructsToCell_overlap(regs);
			break;
		}
		
		// create result data
		this.resultDataCounts = new HashMap<Integer, Integer>();
		this.resultDataTotalSize = new HashMap<Integer, Integer>();
		this.resultDataAvgSize = new HashMap<Integer, Double>();
		this.resultDataAreaFractions = new HashMap<Integer, Double>();
		this.resultDataCellSizes = new HashMap<Integer, Integer>();
		for (int i = 0; i < ilabels.length; i++) {
			Integer lab = new Integer(ilabels[i]);

			// skip background with label zero
			if (lab.intValue() == 0)
				continue;
			
			// compute total particle area for the actual cell
			int larea = 0;
			MTBRegion2DSet lregs = this.cparticles.get(lab);
			for (int j = 0; j < lregs.size(); j++)
				larea += lregs.get(j).getArea();
			// total size of all structures
			this.resultDataTotalSize.put(lab, new Integer(larea));
			// structure count
			this.resultDataCounts.put(lab, 
					new Integer(this.cparticles.get(lab).size()));
			double avgSize = (this.cparticles.get(lab).size() == 0) ?
					0.0 : (double)larea/this.cparticles.get(lab).size();
			this.resultDataAvgSize.put(lab, new Double(avgSize));
			// cell size
			this.resultDataCellSizes.put(lab, cellSizes.get(lab));
			// area fractions
			this.resultDataAreaFractions.put(lab, 
					new Double(larea/cellSizes.get(lab).doubleValue()));
		}
	}
	
	/**
	 * Assigns the given set of structure regions to individual cells.
	 * <p>
	 * For the assignment the center of mass of each region is calculated
	 * and the corresponding cell region chosen according to its location.
	 */
	private void assignStructsToCell_com(MTBRegion2DSet regs) {
		
		float comX, comY;
		Vector<Point2D.Double> pts;
		Point2D.Double pt;
		MTBRegion2DSet cregs = null;

		MTBRegion2D reg;
		for (int i = 0; i < regs.size(); i++) {
			reg = regs.get(i);
			pts = reg.getPoints();

			if (reg.getArea() < this.minStructureSize)
				continue;

			// test if region touches the image border
			boolean border = false;
			if (this.ignoreStructsAtBorder) {
				for (int j = 0; j < pts.size() && !border; j++) {
					pt = pts.get(j);
					if (   (int)pt.getX() == 0 || (int)pt.getX() == this.width-1
							|| (int)pt.getY() == 0 || (int)pt.getY() == this.height-1) {
						border = true;
					}
				}
			}

			// use only regions not touching the image border
			if (!border) {
				comX = reg.getCenterOfMass_X();
				comY = reg.getCenterOfMass_Y();

				// particle must be outside the nuclei regions
				if (   !this.ignoreStructsInNuclei 
						|| (this.nucleiMask.getValueInt(Math.round(comX), 
								Math.round(comY)) == 0)) {
					int label = 
						this.cellLabelImg.getValueInt(Math.round(comX), Math.round(comY));
					cregs = this.cparticles.get(new Integer(label));
					cregs.add(reg);
				}
			}
		}
	}

	/**
	 * Assigns the given set of structure regions to individual cells.
	 * <p>
	 * For the assignment the overlap of each region to all cell regions
	 * is calculated and the cell with largest overlap is chosen as 
	 * corresponding cell region.
	 */
	private void assignStructsToCell_overlap(MTBRegion2DSet regions) {

		// local variables
		Point2D.Double pt;
		Vector<Point2D.Double> points;
		
		// for each region, find corresponding cell
		for (int n=0; n<regions.size(); ++n) {
			points= regions.get(n).getPoints();

			// ignore too small regions
			if (regions.get(n).getArea() < this.minStructureSize)
				continue;

			// test if region touches the image border
			boolean border = false;
			if (this.ignoreStructsAtBorder) {
				for (int j = 0; j < points.size() && !border; j++) {
					pt = points.get(j);
					if (   (int)pt.getX() == 0 || (int)pt.getX() == this.width-1
							|| (int)pt.getY() == 0 || (int)pt.getY() == this.height-1) {
						border = true;
					}
				}
			}

			if (border)
				continue;
			
			Hashtable<Integer, Integer> labs= new Hashtable<Integer, Integer>();
			for (Point2D.Double p: points) {
				int cellLab = this.cellLabelImg.getValueInt((int)p.x, (int)p.y);
				if (cellLab == 0)
					continue;
				Integer cellLabInt= new Integer(cellLab);
				if (labs.containsKey(cellLabInt))
					labs.put(cellLabInt, new Integer(labs.get(cellLabInt).intValue()+1));
				else
					labs.put(cellLabInt, new Integer(1));
			}
			// find maxmimum entry
			int max = 0;
			Integer maxKey = new Integer(0);
			Set<Integer> keyset= labs.keySet();
			for (Integer lab: keyset) {
				if (labs.get(lab).intValue() > max) {
					max = labs.get(lab).intValue();
					maxKey = lab;
				}
			}
			this.cparticles.get(maxKey).add(regions.get(n));
		}
	}
}
