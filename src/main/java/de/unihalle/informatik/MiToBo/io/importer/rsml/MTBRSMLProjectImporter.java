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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Element;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTreeNodeData;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNode;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.xsd.rsml.PointType;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType;
import de.unihalle.informatik.MiToBo.xsd.rsml.Rsml;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType.Geometry;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType.Functions.Function;
import de.unihalle.informatik.MiToBo.xsd.rsml.Rsml.Scene;

/**
 * Reads the contents of the RSML files in the given directory.
 *   
 * @author Birgit Moeller
 * @author Stefan Posch
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class MTBRSMLProjectImporter extends MTBOperator {
	
	private static final String PROPERTY_NAME_PARENTNODE = "parent-node";
	private static final String FUNCTION_NAME_DIAMETER = "diameter";
	private static final String FUNCTION_NAME_STATUSLABEL = "statusLabel";

	public static final int STATUS_UNDEFINED = -1;
	
	/**
	 * to represent (virtual) segments to connect 
	 * branches (polylines) of a root to form one (connected) treeline
	 * <br>
	 * used to represent genuine rizoTrak virtual segments
	 * 
	 */
	public static final int STATUS_VIRTUAL = -2;
	
	/**
	 * to represent (virtual) segments created on import from RSML to connect all
	 * branches (polylines) of a root to form one (connected) treeline
	 * 	 * <br>
	 * used to represent virtual segments create for/from RSML
	 * 
	 */
	public static final int STATUS_VIRTUAL_RSML = -4;

	/**
	 * segments of a connector treeline
	 */
	public static final int STATUS_CONNECTOR = -3;

	private static byte default_statuslabel = 0;

	/**
	 * maximal distance allowed for parent nodes to deviate from precise location w
	 */
	private static final Double EPSILON = 0.01;

	/**
	 * Directory of RSML files.
	 */
	@Parameter( label= "RSML Directory", required = true,
		direction=Direction.IN, dataIOOrder=0, mode=ExpertMode.STANDARD,
		description = "RSML input directory.")
	protected ALDDirectoryString rsmlDir = null;

	MTBRSMLProjectInfo rsmlProjectInfo;
	
	private static String opIdentifier = "[MTBRSMLFileReader]";
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public MTBRSMLProjectImporter() throws ALDOperatorException {
		// nothing happens here
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		
		this.rsmlProjectInfo = new MTBRSMLProjectInfo();
		
		// parse input directory (non-recursively)
		DirectoryTree dt = new DirectoryTree(this.rsmlDir.getDirectoryName(), false);
		this.rsmlProjectInfo.baseDir = this.rsmlDir.getDirectoryName();
		
		// parse all RSML files
		for (String fileName : dt.getFileList()) {
			File rsmlFile = new File(fileName);
			try {
				JAXBContext context = JAXBContext.newInstance( Rsml.class);
				Unmarshaller um = context.createUnmarshaller();
				this.rsmlProjectInfo.rsmls.add( (Rsml) um.unmarshal( rsmlFile));
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(opIdentifier +	
					" cannot read RSML from " + rsmlFile.getName() + "... skipping!");
				continue;
			}
			this.rsmlProjectInfo.rsmlFiles.add(rsmlFile);
		}

		// get status labels from RSML files
		for(int i = 0; i < this.rsmlProjectInfo.rsmls.size(); i++) {
			HashMap<Integer, String> rsmlMap = getStatusLabelMappingFromScene(this.rsmlProjectInfo.rsmls.get(i));
			if(null != rsmlMap && rsmlMap.size() > 0) {
				for(int j: rsmlMap.keySet()) {
					MTBRSMLProjectInfo.MTBRSMLStatusLabel rsl = new MTBRSMLProjectInfo.MTBRSMLStatusLabel(
						rsmlMap.get(j),	rsmlMap.get(j).substring(0, 1), 
							MTBRSMLProjectInfo.MTBRSMLStatusLabel.DEFAULT_STATUS_COLOR);
					this.rsmlProjectInfo.statusLabels.add(rsl);
				}
			}
			else {
				System.out.println("No label mapping was defined in " + this.rsmlProjectInfo.rsmlFiles.get(i).getName() + ".\n");
			}
		}

		// check for unified in all files
		boolean allUnified = true;
		for(Rsml rsml: this.rsmlProjectInfo.rsmls) {
			if(null != rsml.getMetadata() && null != rsml.getMetadata().getTimeSequence()) {
				if(!rsml.getMetadata().getTimeSequence().isUnified()) {
					allUnified = false;
					break;
				}
			}
			else {
				allUnified = false;
				break;
			}
		}
		if(!allUnified) {
			System.out.println("At least one unified flag has not been set or does not exist in the selected RSML files.\n"
					+ "Consequently connectors will not be created on import.");
		}

		// loop over the RSML files and parse plant and root data
		for ( int i = 0 ; i < this.rsmlProjectInfo.rsmls.size() ; i++ ) {
			MTBRSMLFileInfo info = this.parseRsmlFile(
					this.rsmlProjectInfo.rsmls.get(i), this.rsmlProjectInfo.rsmlFiles.get(i)); //, topLevelIdTreelineListMap);
			this.rsmlProjectInfo.rsmlInfos.add(info);
		}

//
//
//		if(allUnified)
//		{
//			// collect connectors before the loop so we dont get the newly created ones
//			List<Connector> connectors = RhizoUtils.getConnectorsBelowRootstacks(rhizoMain.getProject());
//			// create connector -> id map of current connectors
//			Map<String, Connector> connectorIds = new HashMap<String, Connector>();
//			for(Connector connector: connectors)
//			{
//				connectorIds.put(getRsmlIdForTreeline(null, connector), connector);
//			}
//
//			for(String id: topLevelIdTreelineListMap.keySet())
//			{
//				List<Treeline> treelineList = topLevelIdTreelineListMap.get(id);
//
//				boolean connectorFound = false;
//
//				// find connector
//				Connector c = connectorIds.get(id);
//				if(null != c)
//				{
//					for(Treeline treeline: treelineList)
//					{
//						if(!c.addConTreeline(treeline))
//						{
//							Utils.log("rhizoTrak", "Can not add treeline to connector " + c.getId());
//						}
//					}
//
//					connectorFound = true;
//				}
//
//				// no connector found and more than one treeline in list
//				// then create new connector
//				if(!connectorFound && treelineList.size() > 1)
//				{
//					List<Displayable> connector = RhizoUtils.addDisplayableToProject(rhizoMain.getProject(), "connector", 1);
//					Connector newConnector = (Connector) connector.get(0);
//
//					for(Treeline treeline: treelineList)
//					{
//						if(!newConnector.addConTreeline(treeline))
//						{
//							Utils.log("rhizoTrak", "Can not add treeline to connector " + newConnector.getId());
//						}
//					}
//				}
//			}
//		}
//
		
		this.rsmlProjectInfo.print();
	}
	
	/**
	 * Parses the status label mapping from an RSML file.
	 * @param rsml 
	 * @return A HashMap of the status label mapping or null if none is defined in the RSML file
	 */
	private HashMap<Integer, String> getStatusLabelMappingFromScene(Rsml rsml) {
		
		if(		 null == rsml.getScene() 
				|| null == rsml.getScene().getProperties() 
				|| null == rsml.getScene().getProperties().getAny()) 
			return null;
		
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		
		for( Element e: rsml.getScene().getProperties().getAny() ) {

			if(     e.getAttributes().getLength() < 2 
					|| !e.getNodeName().equals("statusLabelMapping") ) 
				continue;
			
			int b = Integer.parseInt(e.getAttribute("int"));
			String s = e.getAttribute("value");
			
			map.put(b, s);
		}
		return map;
	}
	
	/** import the RSML file  <code>file</code> into rhizoTrak for <code>layer</code>.
	 * @param rsml
	 * @param layer
	 * @param imageFilePath Is <code> null </code> if image has not been found for this rsml file
	 * @param topLevelIdTreelineListMap 
	 */
	private MTBRSMLFileInfo parseRsmlFile(Rsml rsml, File rsmlFile ) { //, String imageFilePath) {
		//, HashMap<String, List<Treeline>> topLevelIdTreelineListMap) {
	
		MTBRSMLFileInfo info = new MTBRSMLFileInfo();
		
		if( null != rsml.getMetadata() && null != rsml.getMetadata().getImage() ) {
			info.imageSHA256 = rsml.getMetadata().getImage().getSha256();
			info.imageName = rsml.getMetadata().getImage().getName();
		}
		
		try {
			
			// extract some general scene information
			info.plantCount = rsml.getScene().getPlant().size();

			// import the root
			int plantID = 0;
			for ( Scene.Plant plant : rsml.getScene().getPlant() ) {
				
				info.rootCountPerPlant.put(plantID, plant.getRoot().size());

				Vector<MTBRootTree> roots = new Vector<MTBRootTree>();
				for ( RootType root : plant.getRoot() ) {
					MTBRootTree rt = this.createTreeForRoot(root);
					roots.add(rt);
				}
				info.rootSystems.put(plantID, roots);
				++plantID;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return info;
	}

	/** Create a treeline for the toplevel <code>root</code>
	 * 
	 * @param root
	 * @param layer project layer in which to insert the treeline
	 * @return
	 */
	private MTBRootTree createTreeForRoot(RootType root) {
		MTBRootTree rt = new MTBRootTree();
		this.fillTreeFromRoot( root, rt, null);
		return rt;
	}
	
	/** This adds one root/polyline to the treeline
	 * 
	 * @param root to be added
	 * @param treeline to add the root to
	 * @param parentTreelineNodes hashmap of indices in the root as represented in RSML to the corresponding RadiusNode
	 * @param layer
	 */
	private void fillTreeFromRoot( RootType root, MTBRootTree tree, 
			HashMap<Integer, MTBTreeNode> parentTreelineNodes) {
		
		Geometry geometry = root.getGeometry();
		Function diameters = this.getFunctionByName( root, FUNCTION_NAME_DIAMETER);
		Function statuslabels = this.getFunctionByName( root, FUNCTION_NAME_STATUSLABEL);
				
		Iterator<PointType> pointItr = geometry.getPolyline().getPoint().iterator();
		if ( ! pointItr.hasNext()) {
			// no nodes at all
			return;
		}
		
		// create the rhizoTrak nodes and segments for the polyline/root
		// index of the next node to insert into the treeline
		int pointIndex = 0;
		
		// hash map to map the index of a point in the RSML polyline to  treeline nodes created for this RSML point
		// coordinates are as in the RSML polyline
		// indices start at 0
		HashMap<Integer,MTBTreeNode> treelineNodes = 
				new HashMap<Integer, MTBTreeNode>( geometry.getPolyline().getPoint().size());
		
		// this radius node is the last one when we will iterate the nodes of this treeline
		MTBTreeNode previousnode;
		
		PointType firstPoint = pointItr.next();
		double firstPointX = firstPoint.getX().doubleValue();
		double firstPointY = firstPoint.getY().doubleValue();

		/*
		 * create first node and connect to parent node within the treeline (if existing)
		 */

		// toplevel root/polyline
		if ( parentTreelineNodes == null) {
			
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
					firstPoint.getX().doubleValue(), firstPoint.getY().doubleValue());
			nd.setRadius(this.getRadiusFromRsml( diameters, pointIndex));
			nd.setStatus((byte)0);
			tree.getRoot().setData(nd);
			previousnode = tree.getRoot();
			treelineNodes.put( pointIndex, previousnode);
		} 
		// non-toplevel root/polyline 
		else {

			// find the parent node in the tree from which the current root/polyline branches 
			int parentNodeIndex = getParentNodeIndex( root);
			
			MTBTreeNode parentNode = null;
			if ( parentNodeIndex != (-1) ) {
				parentNode = parentTreelineNodes.get( parentNodeIndex );
			}
			
			boolean foundParentNode;
			double dist, minDist;
			MTBRootTreeNodeData rnd;

			// if no parent node is specified or parent node as read from RSML is broken due to invalid index
			if ( parentNode == null ) {
				foundParentNode = false;
				// nearest node of the parent root/polyline
				minDist = Double.MAX_VALUE;
				for ( MTBTreeNode node : parentTreelineNodes.values() ) {
					rnd = (MTBRootTreeNodeData)node.getData();
					dist = (firstPointX - rnd.getXPos()) * (firstPointX - rnd.getXPos())
							+ (firstPointY - rnd.getYPos()) * (firstPointY - rnd.getYPos());
					if ( dist < minDist) {
						parentNode = node;
						minDist = dist;
					}
				}
			} 
			else {
				foundParentNode = true;
				rnd = (MTBRootTreeNodeData)parentNode.getData();
				minDist = (firstPointX - rnd.getXPos()) * (firstPointX - rnd.getXPos())
						+ (firstPointY - rnd.getYPos()) * (firstPointY - rnd.getYPos());
			}
			
			// create the first treeline node linking it into the treeline
			byte statuslabel = this.getStatuslabelFromRsml( statuslabels, pointIndex);
 
			if (	 this.isStatuslabelFromRsmlDefined(statuslabels, pointIndex) 
					&& statuslabel == STATUS_UNDEFINED 
					&& minDist < EPSILON) {
				
				// skip the first node of this root/polyline if there are mode nodes
				if ( pointItr.hasNext()) {
					pointIndex++;
					firstPoint = pointItr.next();
					statuslabel = this.getStatuslabelFromRsml( statuslabels, pointIndex);	
				}
			} 
			else {
				if ( ! foundParentNode ) {
					statuslabel = STATUS_VIRTUAL_RSML;
				} 
				else {
					statuslabel = STATUS_VIRTUAL;
				}
			}
			
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
					firstPoint.getX().doubleValue(), firstPoint.getY().doubleValue());
			nd.setRadius(this.getRadiusFromRsml( diameters, pointIndex));
			nd.setStatus(statuslabel);
			MTBTreeNode childNode = new MTBTreeNode(nd); 
			parentNode.addChild(childNode);
			previousnode = childNode;
			treelineNodes.put( pointIndex, previousnode);
		}
		// increase point index
		++pointIndex;
		
		// loop over the remaining points
		while ( pointItr.hasNext() ) {
			PointType point = pointItr.next();			
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
					point.getX().doubleValue(), point.getY().doubleValue());
			nd.setRadius(this.getRadiusFromRsml( diameters, pointIndex));
			nd.setStatus(this.getStatuslabelFromRsml( statuslabels, pointIndex));
			MTBTreeNode node = new MTBTreeNode(nd); 
			previousnode.addChild(node);
			treelineNodes.put( pointIndex, node);
			previousnode = node;
			++pointIndex;
		}
		
		// recursively add the child roots
		for ( RootType childRoot : root.getRoot() ) {
			this.fillTreeFromRoot( childRoot, tree, treelineNodes);
		}
	}

	/** Parse the parent-node or parentNode attribute from the root
	 * @param root
	 * @return parent node index (starting with 1 by RSML convetions) or <code>-1</code> if not found
	 */
	private int getParentNodeIndex(RootType root) {
		int parentNodeIndex = -1;
		if ( root.getProperties() != null && root.getProperties().getAny() != null ) {
			for ( Element e : root.getProperties().getAny()) {
				if (   e.getNodeName().equals( PROPERTY_NAME_PARENTNODE) 
						|| e.getNodeName().equals( "parent-node") ) {
					parentNodeIndex =  Integer.valueOf( e.getAttribute( "value"));
				} 
			}
		}
		return parentNodeIndex;
	}

	/**Get the status label for index <code>pointIndex</code> from the function <code>statuslabels</code>
	 * or <code>default_statuslabel</code> if function <code>statuslabels</code> is null or index out of range
	 * @param statuslabels
	 * @param pointIndex
	 * @return
	 */
	private byte getStatuslabelFromRsml(Function statuslabels, int pointIndex) {
		if ( statuslabels != null && statuslabels.getAny().size() > pointIndex && pointIndex >= 0) {
			return getSampleValue( statuslabels.getAny().get( pointIndex)).byteValue();
		} else {
			return default_statuslabel;
		}

	}
	
	private boolean isStatuslabelFromRsmlDefined( Function statuslabels, int pointIndex) {
		// original xsd schema with sample values as attributes
//		if ( statuslabels != null && statuslabels.getSample().size() >= pointIndex && pointIndex >= 0) {
//			return true;
//		} else {
//			return false;
//		}
		if ( statuslabels != null && statuslabels.getAny().size() > pointIndex && pointIndex >= 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param root
	 * @param name
	 * 
	 * @return the first function with name <code>name</code>, null if none exists
	 */
	private Function getFunctionByName(RootType root, String name) {
		if ( root.getFunctions() == null || root.getFunctions().getFunction() == null ) {
			return null;
		}
		
		ListIterator<Function> itr = root.getFunctions().getFunction().listIterator();
		
		while ( itr.hasNext() ) {
			Function fct = itr.next();
			if ( fct.getName().equals(name) ) {
				return fct;
			}
		}
		return null;
	}

	/** Get the radius for index <code>pointIndex</code> from the function <code>diameters</code>
	 * or null if  <code>diameters</code> is null or index out of range
	 * @param diameters
	 * @param pointIndex
	 * @return
	 */
	private float getRadiusFromRsml(Function diameters, int pointIndex) {
		// original xsd schema with sample values as attributes
		//	if ( diameters != null && diameters.getSample().size() > pointIndex && pointIndex >= 0) {
		//		return 0.5f * diameters.getSample().get( pointIndex).getValue().floatValue();
		//	} else {
		//		return 0.0f;
		//	}
		if ( diameters != null && diameters.getAny().size() > pointIndex && pointIndex >= 0) {
			return 0.5f * getSampleValue( diameters.getAny().get( pointIndex)).floatValue();
		} else {
			return 0.0f;
		}
	}
	
	/** Get the value of a sample element in a function.
	 * Use the value attribute or the text content
	 * 
	 * @param element
	 * @return null, if neither value attribute nor the text content defined
	 */ 
	private BigDecimal getSampleValue(Element element) {
		if ( element == null) {
			return null;
		} else if ( element.getAttribute( "value") != null && ! element.getAttribute( "value").isEmpty()) {
			return BigDecimal.valueOf( Double.valueOf( element.getAttribute( "value")));
		} else if ( element.getTextContent() != null ) {
			return BigDecimal.valueOf( Double.valueOf( element.getTextContent()));
		} else {
			return null;
		}
	}

}
