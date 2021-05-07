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

package de.unihalle.informatik.MiToBo.io.importer.rsml;

import java.awt.Color;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import de.unihalle.informatik.MiToBo.xsd.rsml.Rsml;

/**
 * Class to hold all information about a set of RSML files, i.e., a time-series project.
 *   
 * @author Birgit Moeller
 */
public class MTBRSMLProjectInfo {

	/**
	 * Base directory of the project.
	 */
	protected String baseDir;
	
	/**
	 * List of RSML files belonging to the project.
	 */
	protected List<File> rsmlFiles = new LinkedList<File>();;

	/**
	 * List of RSML objects belonging to the project.
	 */
	protected List<Rsml> rsmls = new LinkedList<Rsml>();

	/**
	 * Set of status labels as defined in RSML files.
	 */
	protected TreeSet<MTBRSMLStatusLabel> statusLabels = new TreeSet<MTBRSMLStatusLabel>();
	
	/**
	 * Set of connector identifiers.
	 */
	protected TreeSet<String> connectorIDs = new TreeSet<String>();

	/**
	 * RSML info objects for each RSML file.
	 */
	protected List<MTBRSMLFileInfo> rsmlInfos = new LinkedList<MTBRSMLFileInfo>();

	/**
	 * Prints project information to standard output.
	 */
	public void print() {
		System.out.println();
		System.out.println("======================");
		System.out.println("MTB-RSML Project Info:");
		System.out.println("======================");
		System.out.println();
		System.out.println("- project directory: " + this.baseDir);
		System.out.println("- status labels:");
		for (MTBRSMLStatusLabel label: this.statusLabels) {
			label.print();
		}
		System.out.println("- total rsml files: " + this.rsmlFiles.size());
		
		int id = 0;
		for (MTBRSMLFileInfo info: this.rsmlInfos) {
			System.out.println("- file name: " + this.rsmlFiles.get(id));
			info.print();
			++id;
		}
	}
	
	/**
	 * Access the basis directory of the project.
	 * @return	Basis directory.
	 */
	public String getBaseDir() {
		return this.baseDir;
	}
	
	/**
	 * Get list of RSML files belonging to project.
	 * @return	List of files.
	 */
	public List<File> getRSMLFileList() {
		return this.rsmlFiles;
	}
	
	/**
	 * Get set of status labels.
	 * @return	Set of labels.
	 */
	public TreeSet<MTBRSMLStatusLabel> getStatusLabels() {
		return this.statusLabels;
	}
	
	/**
	 * Get set of connector IDs.
	 * @return	Set of connector IDs.
	 */
	public TreeSet<String> getConnectorIDs() {
		return this.connectorIDs;
	}

	/**
	 * Get RSML file info objects.
	 * @return	List of RSML info objects.
	 */
	public List<MTBRSMLFileInfo> getRSMLFileInfos() {
		return this.rsmlInfos;
	}
	
	/**
	 * Class to represent rhizoTrak status labels.
	 */
	public static class MTBRSMLStatusLabel implements Comparable<MTBRSMLStatusLabel> {

		/**
		 * Default status color.
		 */
		public static final Color DEFAULT_STATUS_COLOR = new Color( 255, 255, 0);

		/**
		 * Name of the status label.
		 */
		private String name;
		
		/**
		 * Abbreviation.
		 */
		private String abbrev = "";
		
		/**
		 * Numerical identifier.
		 */
		private int numericalIdentifier;
		
		/**
		 * Color used when visualizing status in graphical environments. 
		 */
		private Color color;
		
		/**
		 * Transparency level of color.
		 */
		private int alpha = 255;

		/**
		 * Constructor.
		 * @param name		Name of status label.
		 * @param abbrev	Abbreviation.
		 * @param nID			Numerical ID.
		 * @param color		Color of status.
		 */
		public MTBRSMLStatusLabel(String name, String abbrev, int nID, Color color) {
			this.name = name;
			this.abbrev = abbrev;
			this.numericalIdentifier = nID;
			this.color = color;
		}

		/**
		 * Constructor.
		 * @param name		Name of status label.
		 * @param abbrev	Abbreviation.
		 * @param nID			Numerical ID.
		 * @param color		Color of status.
		 * @param alpha		Alpha value of color.
		 */
		public MTBRSMLStatusLabel(String name, String abbrev, int nID, Color color, int alpha) {
			this.name = name;
			this.abbrev = abbrev;
			this.numericalIdentifier = nID;
			this.color = color;
			this.alpha = alpha;
		}

		/**
		 * Get the status label name.
		 * @return Name of status.
		 */
		public String getName() {
			return name;
		} 

		/**
		 * Get abbreviation.
		 * @return Status abbreviation.
		 */
		public String getAbbrev() {
			return abbrev;
		}

		/**
		 * Get the numerical identifier.
		 * @return Numerical ID.
		 */
		public int getNumericalIdentifier() {
			return this.numericalIdentifier;
		}

		/**
		 * Get the status color.
		 * @return Color.
		 */
		public Color getColor() {
			return color;
		}

		/**
		 * Get the alpha value.
		 * @return Transparency value.
		 */
		public int getAlpha() {
			return alpha;
		}

		@Override
		public int compareTo(MTBRSMLStatusLabel obj) {
			return this.name.compareTo(((MTBRSMLStatusLabel)obj).name.toString());
		}
		
		/**
		 * Prints the status label information to standard output.
		 */
		public void print( ) {
			System.out.println(this.name + " " + this.abbrev + " (ID= " + this.numericalIdentifier + ") " 
				+ this.color + " " + this.alpha);
		}
	}
}
