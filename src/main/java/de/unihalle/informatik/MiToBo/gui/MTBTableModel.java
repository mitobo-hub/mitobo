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

package de.unihalle.informatik.MiToBo.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import javax.swing.table.*;

import de.unihalle.informatik.Alida.dataio.provider.swing.components.*;

/**
 * Table model for MiToBo data tables. 
 * <p>
 * The class implements a table model for data results in 
 * MiToBo. One main feature is the dynamic reallocation 
 * of memory when inserting new data.
 * Objects of this class can be integrated in graphical
 * user interfaces by using class {@link de.unihalle.informatik.MiToBo.core.dataio.provider.swing.components.MTBTableWindow}.
 *
 * @author moeller
 */
public class MTBTableModel extends DefaultTableModel {
	
	/**
	 * Number of rows of the table.
	 */
	protected int rows;

	/**
	 * Number of columns of the table. 
	 */
	protected int cols;
		
	/**
	 * The data contained in the table.
	 */
	protected Object [][] data;
	
	/**
	 * Delimiter to be used when exporting the model.
	 */
	protected ALDTableWindow.TableModelDelimiter delimiter = 
																			ALDTableWindow.TableModelDelimiter.TAB;
	
	/**
	 * Header defines.
	 */
	protected Vector<String> headerStrings;
	
	/**
	 * Default constructor.
	 * 
	 * @param r	Initial number of rows.
	 * @param c Initial number of columns.
	 */
	public MTBTableModel(int r, int c) {
		this.rows= r;
		this.cols= c;		
		this.data= new Object[this.rows][this.cols];
		this.headerStrings= new Vector<String>(this.cols);
		// init headers with empty strings
		for (int i=0;i<this.cols;++i)
			this.headerStrings.add(new String(""));
	}
	
	/**
	 * Constructor with given header strings.
	 * 
	 * @param r	Initial number of rows.
	 * @param c Initial number of cols.
	 * @param headers	Header strings.
	 */
	public MTBTableModel(int r, int c, Vector<String> headers) {
		this.rows= r;
		this.cols= c;
		this.data= new Object[this.rows][this.cols];
		this.headerStrings= headers;
	}
	
	/**
	 * Configure the delimiter to be used when exporting the table.
	 * @param delim		Delimiter type to use.
	 */
	public void setDelimiter(ALDTableWindow.TableModelDelimiter delim) {
		this.delimiter = delim;
	}

	/**
	 * Returns the currently chosen delimiter.
	 * @return	Delimiter for table export.
	 */
	public ALDTableWindow.TableModelDelimiter getDelimiter() {
		return this.delimiter;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int col) {
		return this.headerStrings.get(col);
	}
	
	/**
	 * Sets the header of column to specified string.
	 * 
	 * @param col	Index of column.
	 * @param name	New title string.
	 */
	public void setColumnName(int col, String name) {
		this.headerStrings.set(col, name);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
  public int getColumnCount() {
		return this.cols; 
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
  public int getRowCount() {
		return this.rows;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
  public Object getValueAt(int row, int col) {
		if (row >= this.data.length)
			return null;
		if (col >= this.data[row].length)
			return null;
		return this.data[row][col]; 
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
	 * 
	 * @todo Make more efficient! Don't resize every time!
	 */
	@Override
	public void setValueAt(Object o, int row, int col) {

		boolean resize= false;
		int newRowNum= 0;
		int newColNum= 0;
		if (row >= this.rows) {
			newRowNum= row+1;
			resize= true;
		}
		else
			newRowNum= this.rows;
		if (col >= this.cols) { 
			newColNum= col+1;
			resize= true;
		}
		else
			newColNum= this.cols;

		if (resize) {
			// resize object array
			Object [][] newData= new Object[newRowNum][newColNum];
			for (int i=0;i<this.rows;++i) {
				for (int j=0;j<this.cols;++j) {
					newData[i][j]= this.data[i][j];
				}
			}
			// resize header string vector
			Vector<String> newHeader= new Vector<String>(newColNum);
			for (int i=0;i<this.cols;++i)
				newHeader.add(this.headerStrings.get(i));
			// set members correctly
			this.rows= newRowNum;
			this.cols= newColNum;
			this.data= newData;
		}
		this.data[row][col]= o;
	}
	
	/**
	 * Appends a set of results to the table, i.e. adds a new row at the end.
	 * 
	 * @param resultData	Result data to be appended.
	 */
	public void insertData(Vector<Object []> resultData) {

        // check if there is any data given
        if (resultData.size()==0)
        	return;
        
        // insert result data into table
        int entriesInTab= this.getRowCount();
        for (Object [] dataVec: resultData) {
        	for (int entry= 0; entry<dataVec.length; ++entry) {
        		this.setValueAt(dataVec[entry], entriesInTab, entry);
        	}
        	entriesInTab++;
        }
        this.fireTableDataChanged();
	}
	
	/**
	 * Deletes all(!) data inside the table and resets size.
	 */
	public void clear() {
		
		this.cols= 0;
		this.rows= 0;
		this.data= null;

		// update view
		this.fireTableDataChanged();
	}
	
	/**
	 * Saves the contents of the table to given file, TSV format.
	 */
	public void saveTable(File file) {
		try{
			FileWriter ow= new FileWriter(file.getPath());
			StringBuffer [] tab= this.tableToString();
			for (int i=0;i<tab.length;++i)
				ow.write(tab[i].toString());
			ow.close();
		} catch (IOException e) {
			System.err.println("Error!!! DataTableModel: " +
					"Could not open output file" + file.getPath());
		}
	}

	/**
	 * Converts the contents of the table to a string array
	 * in CSV format (suitable for import in Excel).
	 * <p>
	 * For separating the columns the formerly configured delimiter is used,
	 * e.g., tabulators or just spaces. 
	 * 
	 * @return Array with contents of table (arranged line-wise).
	 */
	public StringBuffer [] tableToString() {
		
		// number of lines in table (without headlines)
		int lines= this.getRowCount();

		StringBuffer [] tabS= new StringBuffer[lines+1];
		StringBuffer headings= new StringBuffer();
		StringBuffer newline;
		
		String delimString = "\t";
		switch (this.delimiter)
		{
		case TAB:
			delimString = "\t";
			break;
		case SPACE:
			delimString = " ";
			break;
		}
		
		// transform headlines...
		for (int j=0; j<this.headerStrings.size()-1; ++j) {
			headings.append(this.headerStrings.get(j) + delimString);
		}
		headings.append(this.headerStrings.get(this.headerStrings.size()-1) 
																	  + "\n");
		tabS[0]= headings;
		
		// ... and then the rows
		String buf= null;
		for (int i= 0; i<lines; ++i) {

			newline= new StringBuffer();
			newline.append(this.getValueAt(i, 0) + delimString);
			for (int j=1; j<this.getColumnCount()-1;++j) {

				if (this.getValueAt(i,j) == null) {
					newline.append(0 + delimString);
					continue;
				}
				buf= (this.getValueAt(i,j)).toString();
				// append to line
				newline.append(buf + delimString);
			}
			if (this.getValueAt(i, this.getColumnCount()-1) != null) {
				buf= (this.getValueAt(i, this.getColumnCount()-1)).toString();
				
				newline.append(buf + "\n");
			}
			else {
				newline.append("\n");
			}
			tabS[i+1]= newline;
		}
		return tabS;
	}
}
