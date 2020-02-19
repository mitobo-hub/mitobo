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

package de.unihalle.informatik.MiToBo.apps.cells2D;

import java.io.File;
import java.util.*;

import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Complete;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Cytoplasm;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Nuclei;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Particles;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * Implementation of a TableModel for the Granule Detector 
 * result table. One main feature is the dynamic reallocation
 * of memory when inserting new data.
 *
 * @author moeller
 */
public class Mica2DTableModel extends MTBTableModel {

	/**
	 * For convenience: always open last directory for saving.
	 */
	protected File lastDir;

	/**
	 * Header defines.
	 */
	protected final String[] header= {"File", 
			"cell ID", "size", "#nuclei", "(avg.) size",
			"#ch 1", "avg. size ch. 1", 
			"#ch 2", "avg. size ch. 2", 
			"#ch 3", "avg. size ch. 3", 
			"#ch 4", "avg. size ch. 4"};

	/**
	 * Default constructor.
	 * 
	 * @param r	initial number of rows
	 * @param c initial number of columns
	 */
	Mica2DTableModel(int r, int c) {

		super(r,c);

		// init current directory with user directory
		this.lastDir= new File(System.getProperty("user.dir"));

		this.headerStrings= new Vector<String>(this.cols);
		for (String s: this.header) {
			this.headerStrings.add(s);
		}
	}

	public void addNewResult(SegResult_Complete res) {

		// insert result into table (adds a row at the bottom)
		int entriesInTab= this.getRowCount();

		// image name
		this.setValueAt(res.getImageName(),entriesInTab,0);

		// nuclei detection result, if available
		SegResult_Nuclei nr = res.getNucleiResult();
		MTBRegion2DSet nr_regs = null;
		if (nr != null)
			nr_regs = nr.getNucleiRegions();

		// check if individual cells were segmented
		switch(res.getSegmentationMode()) 
		{
		case IMAGE_COMPLETE:
		{
			// index of column to fill next
			int colID = 1;

			// 2nd column: cell id - here -1
			this.setValueAt("-1",entriesInTab,colID);
			++colID;

			// 3rd column: size of cell area - here -1
			this.setValueAt("-1",entriesInTab,colID);
			++colID;

			// 4th column: number of nuclei
			if (nr != null)
				this.setValueAt(new Integer(nr.getNucleiCount()),
						entriesInTab,colID);
			else
				this.setValueAt(new Integer(-1),	entriesInTab,colID);
			++colID;
			// 5th column: average size
			if (nr != null)
				this.setValueAt(new Float(nr.getNucleiAvgSize()),
						entriesInTab,colID);
			else
				this.setValueAt(new Integer(-1),	entriesInTab,colID);
			++colID;
			// remaining columns: counts and average sizes
			for (int i=0;i<2*res.getNumberChannels();i=i+2) {
				SegResult_Particles pars = res.getParticleResult(i/2);
				if (pars != null) {
					this.setValueAt(new Integer(pars.getParticleCount()),
							entriesInTab,i+colID);
					this.setValueAt(new Float(pars.getParticleAvgSize()),
							entriesInTab,i+colID+1);
				}
				else {
					this.setValueAt(new Integer(-1),entriesInTab,i+colID);
					this.setValueAt(new Float(-1.0),entriesInTab,i+colID+1);
				}
			}
			break;
		}
		case INDIVIDUAL_CELLS:
		{
			// determine sizes of segmented cell regions
			SegResult_Cytoplasm cytoRes = res.getCytoplasmResult();
			int [] sizeArray = cytoRes.getCellSizes();
			// fill the table
			for (int i=0;i<2*res.getNumberChannels();i=i+2) {
				SegResult_Particles pars = res.getParticleResult(i/2);
				int rowToFill = entriesInTab;
				if (pars != null) {
					HashMap<Integer,Integer> countsPerCell = pars.getPerCellCount();
					HashMap<Integer,Double> sizePerCell = pars.getPerCellAvgSize();
					Set<Integer> keys = countsPerCell.keySet();
					for (Integer key: keys) {
						int index = key.intValue()-1;
						int colID = 1;
						// cell index (= number in result contour image)
						this.setValueAt(new Integer(key.intValue()-1),rowToFill,colID);
						++colID;
						// cell area
						this.setValueAt(new Integer(sizeArray[index]),
								rowToFill,colID);
						++colID;
						// number of nuclei (always = 1)
						this.setValueAt("1",rowToFill,colID);
						++colID;
						// size of nucleus
						if (nr_regs != null)
							this.setValueAt(new Integer(nr_regs.get(index).getArea()),
									rowToFill,colID);
						else
							this.setValueAt(new Integer("-1"), rowToFill,colID);
						++colID;
						// detected particles
						this.setValueAt(countsPerCell.get(key),rowToFill,i+colID);
						this.setValueAt(sizePerCell.get(key),rowToFill,i+colID+1);
						++rowToFill;
					}
				}
				else {
					this.setValueAt(new Integer(-1),entriesInTab,i+5);
					this.setValueAt(new Float(-1.0),entriesInTab,i+5+1);
				}
			}
			break;
		}
		}
		this.fireTableDataChanged();
//		CellImageAnalyzerTableModel.this.tabFrame.setVisible(true);
	}
}
