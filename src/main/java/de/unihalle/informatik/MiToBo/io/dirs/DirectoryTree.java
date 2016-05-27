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

package de.unihalle.informatik.MiToBo.io.dirs;

import java.io.*;
import java.util.*;

import de.unihalle.informatik.MiToBo.core.datatypes.*;

/**
 * This class implements a data structure for representing 
 * directory trees. It is based on a conventional tree
 * data structure, but provides methods for directly parsing a 
 * directory structure into the tree. Additionally, the class
 * provides methods for getting a list of all files 
 * in all directories below the given root directory.
 * 
 * @author moeller
 * @see MTBTree
 */
public class DirectoryTree extends MTBTree {
	
	/**
	 * Root directory of the tree.
	 */
	private String mainpath;
	
	/**
	 * Flag for recursive processing of sub-directories.
	 */
	private boolean recursiveProcessing;
	
	/**
	 * Standard constructor.
	 * @param dir Root directory where to begin the parsing.
	 */
	public DirectoryTree(String dir) {
		super(new DirectoryTreeNodeData(dir));
		this.mainpath= dir;
		this.recursiveProcessing= true;
		
		// parse the directory (recursively)
		this.initFromDirectory();
	}

	/**
	 * Constructor.
	 * @param dir 						Root directory where to begin the parsing.
	 * @param recursiveFlag		Recursive processing of sub-directories.
	 * 
	 */
	public DirectoryTree(String dir, boolean recursiveFlag) {
		super(new DirectoryTreeNodeData(dir));
		this.mainpath= dir;
		this.recursiveProcessing= recursiveFlag;
		
		// parse the directory (recursively)
		this.initFromDirectory();
	}
	
	/**
	 * Collect all files in the directory tree including
	 * their complete paths.
	 * 
	 * @return	vector containing all files with absolute path
	 */
	public Vector<String> getFileList() {

		// get root node data for easier access
		DirectoryTreeNodeData rootData = 
				(DirectoryTreeNodeData)(this.root.getData());
		
		// allocate memory for result
		Vector<String> fileList= new Vector<String>();
		
		// add files in this directory
		for (int i=0; i<rootData.getFileList().size();++i)
			fileList.add(rootData.getFileList().get(i));

		// get file lists from children recursively
		Vector<MTBTreeNode> childs= this.root.getChilds();
		for (int i=0;i<childs.size();++i) {
			DirectoryTreeNodeData childdata= 
					(DirectoryTreeNodeData)(childs.get(i).getData());
			Vector<String> childlist= childdata.getSubtreeFileList();
			
			for (int j=0;j<childlist.size();++j)
				fileList.add(this.mainpath + File.separator + childlist.get(j));
		}
		return fileList;
	}
	
	/**
	 * Builds the directory tree by initiating the (recursive) parse 
	 * procedure.
	 */
	private void initFromDirectory() {
		
		DirectoryTreeNodeData thisRoot = 
				(DirectoryTreeNodeData)this.root.getData();
		
		// first check if you really work on a directory
		File f = new File(this.mainpath);
		if (!f.isDirectory()) {
			return;
		}
		
		// get list of all files and subdirectories
		String[] list = f.list();
		if (list==null)
			return;
		
		// sort list of files alphabetically
		Arrays.sort(list);
		
		// iterate over the list
		for (int i=0; i<list.length; i++) {
			String name = list[i];
			
			// insert all non-directories into the list
			File g = new File(this.mainpath + File.separator + name);
			if (!g.isDirectory()) {
				thisRoot.addFile(this.mainpath + File.separator + name);
			}
			// check if we have a directory
			else if (g.isDirectory() && this.recursiveProcessing) {
				// recursively examine directories
				MTBTreeNode subNode = 
						DirectoryTree.traverseSubdir(this.mainpath, name);
				this.root.addChild(subNode);
			}
		}
	}

	/**
	 * Recursive traversal of subdirectories.
	 * 
	 * @param path		Absolute path to parent directory.
	 * @param subdir	The subdirectory to be analyzed.
	 * @return	Tree node for the subdirectory.
	 */
	private static MTBTreeNode traverseSubdir(String path, String subdir) {
		
		// data object for the new directory
		DirectoryTreeNodeData ndir= new DirectoryTreeNodeData(subdir);
		
		// check if we really have a directory here
		String fullpath= path + File.separator + subdir;
		File f = new File(fullpath);
		if (!f.isDirectory()) {
			return null;
		}
		
		// check if there are any files or subdirectories
		String[] list = f.list();
		if (list==null) {
			return null;
		}

		// sort list of files alphabetically
		Arrays.sort(list);

		// if so, first add .tif files to the list
		MTBTreeNode node= new MTBTreeNode(ndir);
		for (int i=0; i<list.length; i++) {
			String name = list[i];
//			boolean isImageFile = name.endsWith(".tif");
			boolean isFile = new File(name).isFile();
			if (isFile) {
				ndir.addFile(name);
			}
			// check if we have a directory
			else {
				File g = new File(fullpath + File.separator + name);
				if (!g.isDirectory()) {
					// found something unknown, neither file nor directory... 
				}
				else {
					// recursive traversal
					MTBTreeNode subNode = 
							DirectoryTree.traverseSubdir(fullpath, name);
					node.addChild(subNode);
				}	
			}
		}
		return node;
	}
}
