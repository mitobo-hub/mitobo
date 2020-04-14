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
 * Class to hold all information about a set of RSML files, i.e., a project.
 *   
 * @author Birgit Moeller
 */
public class MTBRSMLProjectInfo {

	public String baseDir;
	
	public List<File> rsmlFiles = new LinkedList<File>();;

	public List<Rsml> rsmls = new LinkedList<Rsml>();

	public TreeSet<MTBRSMLStatusLabel> statusLabels = new TreeSet<MTBRSMLStatusLabel>();
	
	public List<MTBRSMLFileInfo> rsmlInfos = new LinkedList<MTBRSMLFileInfo>();

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
	
	public static class MTBRSMLStatusLabel implements Comparable<MTBRSMLStatusLabel> {

		public static final Color DEFAULT_STATUS_COLOR = new Color( 255, 255, 0);

		private String name;
		private String abbrev = "";
		private Color color;
		private int alpha = 255;

		public MTBRSMLStatusLabel(String name, String abbrev, Color color) {
			this.name = name;
			this.abbrev = abbrev;
			this.color = color;
		}

		public MTBRSMLStatusLabel(String name, String abbrev, Color color, int alpha) {
			this.name = name;
			this.abbrev = abbrev;
			this.color = color;
			this.alpha = alpha;
		}

		public MTBRSMLStatusLabel(String name, Color color, int alpha) {
			this.name = name;
			this.color = color;
			this.alpha = alpha;
		}

		public MTBRSMLStatusLabel(String name, String abbrev) {
			this.name = name;
			this.abbrev = abbrev;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		} 

		/**
		 * @return the abbrev
		 */
		public String getAbbrev() {
			return abbrev;
		}

		/**
		 * @return the color
		 */
		public Color getColor() {
			return color;
		}

		/**
		 * @return the alpha
		 */
		public int getAlpha() {
			return alpha;
		}

		@Override
		public int compareTo(MTBRSMLStatusLabel obj) {
			return this.name.compareTo(((MTBRSMLStatusLabel)obj).name.toString());
		}
		
		public void print( ) {
			System.out.println(this.name + " " + this.abbrev + " " + this.color + " " + this.alpha);
		}

	}

}
