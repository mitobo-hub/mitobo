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

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentCheckBox;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentTextField;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBBooleanData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;

/**
 * Class for loading/saving wrapper datatypes in MiToBo.
 * 
 * @author moeller
 */
@ALDDataIOProvider
public class MTBWrapperDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	/**
	 * Interface method to announce class for which IO is provided for.
	 * 
	 * @return  Collection of classes provided.
	 */
	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add( MTBBooleanData.class);
		classes.add( MTBDoubleData.class);
		classes.add( MTBIntegerData.class);
		classes.add( MTBStringData.class);
		return classes;
	}

	@Override
	public ALDSwingComponent createGUIElement(Field field, 
			Class<?> cl, Object obj, ALDParameterDescriptor descr) {

		// handle boolean wrapper datatype
		if (cl.equals(MTBBooleanData.class)) {
			ALDSwingComponentCheckBox checkbox = new ALDSwingComponentCheckBox(descr);
			if ( obj != null )
				checkbox.getJComponent().setSelected(((MTBBooleanData)obj).getValue());
			return checkbox;
		}
		
		// handle all the rest
		ALDSwingComponentTextField textfield = 
				new ALDSwingComponentTextField(cl, descr, 25);
		if ( obj != null ) {
			if (cl.equals(String.class))
				textfield.setText((String)obj);
			else if (cl.equals(MTBDoubleData.class))
				textfield.setText(((MTBDoubleData)obj).getValue().toString());
			else if (cl.equals(MTBIntegerData.class))
				textfield.setText(((MTBIntegerData)obj).getValue().toString());
			else if (cl.equals(MTBStringData.class))
				textfield.setText(((MTBStringData)obj).getString());
			else 
				textfield.setText( obj.toString());
		}
		return textfield;
	}

	@Override
	public void setValue(Field field, Class<?> cl, 
			ALDSwingComponent guiElement,	Object value) 
		throws ALDDataIOProviderException {
		if (value == null)
			return;
		
		// handle boolean wrapper datatype
		if (cl.equals(MTBBooleanData.class)) {
			ALDSwingComponentCheckBox checkbox = (ALDSwingComponentCheckBox)guiElement;
			checkbox.getJComponent().setSelected(((MTBBooleanData)value).getValue());
			return;
		}

		// handle all the rest
		ALDSwingComponentTextField textfield = 
				(ALDSwingComponentTextField)guiElement;
		if (cl.equals(String.class))
			textfield.setText((String)value);
		else if (cl.equals(MTBDoubleData.class))
			textfield.setText(((MTBDoubleData)value).getValue().toString());
		else if (cl.equals(MTBIntegerData.class))
			textfield.setText(((MTBIntegerData)value).getValue().toString());
		else if (cl.equals(MTBStringData.class))
			textfield.setText(((MTBStringData)value).getString());
		else 
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT, 
				"MTBWrapperDataIOSwing: setValue() received wrong object type!");
	}

	@Override
	public Object readData(Field field, Class<?> cl,
			ALDSwingComponent guiElem) {
		
		// handle boolean wrapper datatype
		if (cl.equals(MTBBooleanData.class)) {
			ALDSwingComponentCheckBox checkbox = (ALDSwingComponentCheckBox)guiElem;			
			return new MTBBooleanData(checkbox.getJComponent().isSelected());
		}
		
		// handle all the rest
		String text = ((ALDSwingComponentTextField)guiElem).getText();

		if (cl.equals(String.class)) 
			return text;

		try {
			if (cl.equals(MTBDoubleData.class))
				return new MTBDoubleData( Double.valueOf(text));
			if (cl.equals(MTBIntegerData.class))
				return new MTBIntegerData( Integer.valueOf(text));
			if (cl.equals(MTBStringData.class))
				return new MTBStringData( text);
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {

		// handle boolean wrapper datatype
		if (obj.equals(MTBBooleanData.class)) {
			JCheckBox checkbox = new JCheckBox();
			checkbox.setSelected( ((MTBBooleanData)obj).getValue() );
			checkbox.setEnabled( false);
			return checkbox;
		}
		
		// handle all the rest
		JTextField textfield = new JTextField(25);
		if (obj.getClass().equals(MTBDoubleData.class))
			textfield.setText( ((MTBDoubleData)obj).getValue().toString());
		else
			return null;

		textfield.setEditable( false);
		return textfield;
	}
}
