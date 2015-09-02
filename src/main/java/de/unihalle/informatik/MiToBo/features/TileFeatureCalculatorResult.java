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

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * Result data of the operator {@link TileFeatureCalculator}.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class TileFeatureCalculatorResult 
{
	/**
	 * Result table for visual result inspection.
	 */
	@ALDClassParameter(label="Result table.")
	private MTBTableModel resultTable; 
	
	/**
	 * Number of invalid, i.e. background tiles.
	 */
	@ALDClassParameter(label="Invalid tiles.")
	private int invalidTilesNum = 0;
	
	/**
	 * Set of feature calculator identifiers.
	 */
	Vector<String> analyzerList = new Vector<String>();  
	
	/**
	 * Tile-wise result data of the different calculators.
	 */
	Vector<FeatureCalculatorResult[]> featureCalcResults = 
			new Vector<FeatureCalculatorResult[]>();
	
	/**
	 * Default constructor.
	 */
	public TileFeatureCalculatorResult() {
		// nothing to do here
	}
	
	/**
	 * Adds result data to the object.
	 * @param data	Result data to insert.
	 */
	public void addResult(FeatureCalculatorResult[] data) {
		this.featureCalcResults.add(data);
		this.updateTable();
	}
	
	/**
	 * Updates the result table as new data is added.
	 */
	private void updateTable() {
		int featCount= 0;
		Vector<String> headers = new Vector<String>();
		int tileCount = this.featureCalcResults.firstElement().length;
		
		// get dimensions and headers from first element of each result object
		for (FeatureCalculatorResult[] result: this.featureCalcResults) {
			featCount += result[0].getDimensionality();
			for (int i=0;i<result[0].getDimensionality();++i)
				headers.add(result[0].getOpIdentifier() + "_" 
																				+ result[0].getResultIdentifier(i));
		}
		
		MTBTableModel newTable = new MTBTableModel(tileCount, featCount, headers);
		int colID = 0;
		for (FeatureCalculatorResult[] res: this.featureCalcResults) {
			int i = 0;
			for (i=0;i<res.length;++i) {
				for (int j=0;j<res[i].getDimensionality();++j) {
					newTable.setValueAt(res[i].getTableEntry(j), i, (colID+j));
				}
			}
			colID += res[i-1].getDimensionality();
		}
		this.resultTable = newTable;
	}
	
	/**
	 * Get result table.
	 * @return	Result table with calculated features.
	 */
	public MTBTableModel getResultTable() {
		return this.resultTable;
	}
	
	/**
	 * Set number of invalid tiles.
	 * @param n		Number of invalid/background tiles.
	 */
	protected void setInvalidTilesNum(int n) {
		this.invalidTilesNum = n;
	}
	
	/**
	 * Get number of invalid tiles.
	 * @return	Number of invalid/background tiles.
	 */
	public int getInvalidTilesNum() {
		return this.invalidTilesNum;
	}
}
