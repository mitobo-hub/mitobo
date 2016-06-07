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

package de.unihalle.informatik.MiToBo.io.dirs;

import java.io.File;
import java.util.*;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.*;

/**
 * Implements class TreeNodeData for DirectoryTree.
 * In particular, each node of the tree is associated
 * with an absolute path and a list of files. These
 * data are stored inside objects of this class.
 * 
 * @author moeller
 * @see MTBTreeNodeData
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class DirectoryTreeNodeData extends MTBTreeNodeData {

	/**
	 * Absolute directory path associated with this node. 
	 */
	String path;

	/**
	 * List of files (no subdirectories!) inside the directory.
	 */
	Vector<String> files;
	
	/**
	 * Default constructor.
	 * 
	 * @param p		absolute path of directory
	 */
	public DirectoryTreeNodeData(String p) {
		this.path= p;
		this.files= new Vector<String>();
	}
	
	/* (non-Javadoc)
	 * @see datatypes.TreeNodeData#setNode(datatypes.TreeNode)
	 */
	@Override
  public void setNode(MTBTreeNode n) {
		this.node= n;
	}
	
	/**
	 * Request path associated with the node.
	 * 
	 * @return	path of the node
	 */
	public String getPath() {
		return this.path;
	}
	
	/**
	 * Specify path associated with the node.
	 * 
	 * @param p		path of the node's directory
	 */
	public void setPath(String p) {
		this.path= p;
	}
	
	/**
	 * Adds a file to the list of the node.
	 * 
	 * @param f		file to be added
	 */
	public void addFile(String f) {
		this.files.add(f);
	}
	
	/**
	 * Get the list of all files inside the directory.
	 * 
	 * @return	list with files inside directory
	 */
	public Vector<String> getFileList() {
		return this.files;
	}
	
	/* (non-Javadoc)
	 * @see datatypes.TreeNodeData#printData()
	 */
	@Override
  public void printData() {
		System.out.println("=> Directory= " + this.path);
		if (this.files.size()>0)
			System.out.println("     Files: ");
		else
			System.out.println("     <no files>");
		for (int i=0;i<this.files.size();++i)
			System.out.println("     - " + this.files.get(i));
	}
	
	/**
	 * Collects the list of all files inside this 
	 * directory AND inside all subdirectories.
	 * 
	 * @return	complete file list with absolute paths
	 */
	public Vector<String> getSubtreeFileList() {
		
		// allocate result list
		Vector<String> datalist= new Vector<String>();

		// add files, if available
		for (int i=0;i<this.files.size();++i) {
			datalist.add(this.path + File.separator + this.files.get(i));
		}

		// get childs of the node associated with this directory
		Vector<MTBTreeNode> childs= this.node.getChilds();
		
		// get the strings from these childs recursively
		for (int i=0;i<childs.size();++i) {

			DirectoryTreeNodeData clist= 
					(DirectoryTreeNodeData)(childs.get(i).getData());
			Vector<String> cpaths= clist.getSubtreeFileList();
			
			// append children paths to your own
			for (int j=0;j<cpaths.size();++j) {
				datalist.add(this.path + File.separator + cpaths.get(j));
			}
		}
		return datalist;
	}
	
	public Vector<String> getSubtreeDirList() {
		
		// allocate result list
		Vector<String> datalist= new Vector<String>();

		// get childs of the node associated with this directory
		Vector<MTBTreeNode> childs= this.node.getChilds();
		
		// get the strings from these childs recursively
		for (int i=0;i<childs.size();++i) {

			DirectoryTreeNodeData clist= 
					(DirectoryTreeNodeData)(childs.get(i).getData());
			
			System.out.println("Found " + clist.getPath());
			datalist.add(clist.getPath());
			
			Vector<String> subdirs = clist.getSubtreeDirList();
			
			// append children paths to your own
			for (int j=0;j<subdirs.size();++j) {
				System.out.println("Found " + subdirs.get(j));
				datalist.add(this.path + File.separator + subdirs.get(j));
			}
		}
		return datalist;
	}
}
