/**
 * 
 */
package de.unihalle.informatik.MiToBo.core.dataconverter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import de.unihalle.informatik.Alida.annotations.ALDDataConverterProvider;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.dataconverter.ALDDataConverter;
import de.unihalle.informatik.Alida.dataconverter.ALDDataConverterManager.ALDSourceTargetClassPair;
import de.unihalle.informatik.Alida.exceptions.ALDDataConverterException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDDataConverterException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageFloat;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageInt;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;

/**
 * Convert a MTBImage to any other image and any image type to a MTBImage.
 * Otherwise we refuse to convert a MTBImageRGB to another type or
 * vice verse a non MTBImageRGB to a MTBImageRGB

 * @author posch
 *
 */

@ALDDataConverterProvider
public class MTBImageConverter extends ALDOperator implements ALDDataConverter {

	private boolean debug = false;
	
	@Parameter(label = "Source Object", required = true, 
	           direction = Parameter.Direction.IN, supplemental = false, 
	           description = "Source object to convert")
	MTBImage sourceObject;
	
	@Parameter(label = "Target Class", required = true, 
	           direction = Parameter.Direction.IN, supplemental = false, 
	           description = "Class of target object")
	Class<?> targetClass;
	
	@Parameter(label = "Target Object", 
			   direction = Parameter.Direction.OUT, supplemental = false, 
	           description = "Target object ")
	MTBImage targetObject;

	public MTBImageConverter() throws ALDOperatorException {
		super();
	}

	/**
	 * all supported classes besides RGB
	 */
	private static LinkedList<Class<?>> imageClasses = new LinkedList<Class<?>>();
	{
		 Class<?>[] numberClassesA = new Class<?>[]
				 {MTBImage.class, 
				 MTBImageByte.class, 
				 MTBImageShort.class, 
				 MTBImageInt.class, 
				 MTBImageFloat.class,
				 MTBImageDouble.class};

		imageClasses = new LinkedList<Class<?>>();
		for ( int i = 0 ; i < numberClassesA.length ; i++)
			imageClasses.add( numberClassesA[i]);
	}
	
	@Override
	public Collection<ALDSourceTargetClassPair> providedClasses() {
		LinkedList<ALDSourceTargetClassPair> res = new LinkedList<ALDSourceTargetClassPair>();
		
		for ( Class sourceClass : imageClasses ) {
			for ( Class targetClass : imageClasses ) {
			ALDSourceTargetClassPair pair = 
					new ALDSourceTargetClassPair(sourceClass, targetClass);
				res.add(pair);
			}
		}
		ALDSourceTargetClassPair pair;
		pair = new ALDSourceTargetClassPair(MTBImage.class, MTBImageRGB.class);
		res.add(pair);
		pair = new ALDSourceTargetClassPair(MTBImageRGB.class, MTBImage.class);
		res.add(pair);

		return res;
	}

	@Override
	public boolean supportConversion(Class<?> sourceClass, Type[] sourceTypes,
			Class<?> targetClass, Type[] targetTypes) {
		return true;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.dataconverter.ALDDataConverter#convert(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object convert(Object sourceObject, Type[] sourceTypes,
			Class<?> targetClass, Type[] targetTypes)
			throws ALDDataConverterException {
		if ( debug ) {
			System.out.println("MTBImageConverter::convert from class " +
					sourceObject.getClass().getName() + " to " + targetClass.getName());
		}

		this.sourceObject = (MTBImage)sourceObject;
		this.targetClass = targetClass;

		if ( ( (targetClass == MTBImageByte.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_BYTE ) ||
			 ( (targetClass == MTBImageShort.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_SHORT ) ||
			 ( (targetClass == MTBImageInt.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_INT ) ||
			 ( (targetClass == MTBImageFloat.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_FLOAT ) ||
			 ( (targetClass == MTBImageDouble.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_DOUBLE ) ||
			 ( (targetClass == MTBImageRGB.class) && 
				((MTBImage)sourceObject).getType() != MTBImageType.MTB_RGB ) ) {
			
			// we really need to convert
			if ( debug ) {
				System.out.println("MTBImageConverter::convert <" + sourceObject.getClass().getName() +
						"> to <" + targetClass.getName() + ">");
			}
			
			try {
				runOp( HidingMode.HIDE_CHILDREN);
			} catch (Exception e) {
				throw new ALDDataConverterException(ALDDataIOProviderExceptionType.CANNOT_CONVERT, 
						"MTBImageConverter unknown class <" + sourceObject.getClass().getName() +
						"> to <" + targetClass.getName() + ">");	
			}	
			return this.targetObject;
		} else if ( (targetClass == MTBImageByte.class)  ||
				(targetClass == MTBImageShort.class)  ||
				(targetClass == MTBImageInt.class) ||
				(targetClass == MTBImageFloat.class) ||
				(targetClass == MTBImageDouble.class)  ) {
			// we have already the target image type
			if ( debug ) {
				System.out.println("MTBImageConverter::convert <" + sourceObject.getClass().getName() +
						"> is alread target class");
			}

			return sourceObject;
		} else {
			throw new ALDDataConverterException(ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
					"MTBImageConverter unknown class <" + sourceObject.getClass().getName() +
					">");	
		}
	}

	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException {

		if ( targetClass == MTBImageByte.class ) {
			this.targetObject = ((MTBImage)sourceObject).convertType(MTBImageType.MTB_BYTE, true);
		} else if ( targetClass == MTBImageShort.class ) {
			this.targetObject = ((MTBImage)sourceObject).convertType(MTBImageType.MTB_SHORT, true);
		} else if ( targetClass == MTBImageInt.class ) {
			this.targetObject = ((MTBImage)sourceObject).convertType(MTBImageType.MTB_INT, true);			
		} else if ( targetClass == MTBImageFloat.class ) {
			this.targetObject = ((MTBImage)sourceObject).convertType(MTBImageType.MTB_FLOAT, true);			
		} else if ( targetClass == MTBImageDouble.class) {
			this.targetObject = ((MTBImage)sourceObject).convertType(MTBImageType.MTB_DOUBLE, true);			
		}
	}

}
