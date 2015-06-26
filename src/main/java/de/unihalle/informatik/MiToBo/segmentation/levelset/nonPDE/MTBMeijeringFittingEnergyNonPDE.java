package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;

import java.io.PrintStream;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * class that implements the energy functional introduced in
 * 
 * O. Dzyubachyk, W. Niessen and E. Meijering,
 * "A VARIATIONAL MODEL FOR LEVEL-SET BASED CELL TRACKING IN TIME-LAPSE FLUORESCENCE MICROSCOPY IMAGES",
 * 4th IEEE International Symposium on Biomedical Imaging (ISBI), 2007
 * 
 * @author glass
 *
 */
@ALDParametrizedClass
@ALDDerivedClass
public class MTBMeijeringFittingEnergyNonPDE extends MTBGenericEnergyNonPDE
{
	/** Image to be segmented
	 */
	@ALDClassParameter(label="Input image")
	private MTBImage img;

	private int numPhases; // number of phases including background
	private double[] mean;  // array to hold the mean intensity values of the phases
	private double[] var; // array to hold the variancees of the intensity values of the phases
	
    private int sizeX;
	private int sizeY;
	private int sizeZ;
	
	
	/**
	 * constructor
	 * 
	 */
	public MTBMeijeringFittingEnergyNonPDE()
	{
		this.name = "MTBMeijeringFittingEnergyNonPDE";
	}
	
	/**
	 * constructor
	 * 
	 * @param img
	 * @param phi
	 */
	public MTBMeijeringFittingEnergyNonPDE(MTBImage img, MTBLevelsetMembership phi)
	{
		this();
		this.init(img, phi);
	}
	
	/**
	 * Initialize the energy object. 
	 * <p>
	 * NOTE: The image provided as argument to this
	 * method is only associated with the energy object, if not already set!!
	 * This rational behind this is to allow the energy to be supplied generically with
	 * the input image and while it is still possible to set an image
	 * deviating from this default.
	 * 
	 * @param img	Image to be segmented
	 * @param phi	Level set function to construct the energy object for
	 */
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		if ( this.img == null ) {
			this.img = img;
		}

		this.sizeX = img.getSizeX();
		this.sizeY = img.getSizeY();
		this.sizeZ = img.getSizeZ();
		this.numPhases = phi.getNumPhases();
		this.mean = new double[numPhases + 1];
		this.var = new double[numPhases + 1];

		estimateParams(phi);
		return this;
	}

	@Override
	protected void estimateParams(MTBLevelsetMembership phi)
	{
		// determine mean intensity values of all phases
		for(int z = 0; z < sizeZ; z++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					mean[phi.getPhase(x, y, z)] += img.getValueDouble(x, y, z, 0, 0);
				}
			}
		}
		
		
		for(short i = 1; i <= numPhases; i++)
		{
			mean[i] /= phi.getSizePhase(i);
		}
		
		// determine variances of intensity values of all phases
		for(int z = 0; z < sizeZ; z++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					var[phi.getPhase(x, y, z)] += ((img.getValueDouble(x, y, z, 0, 0) - mean[phi.getPhase(x, y, z)]) 
											    * (img.getValueDouble(x, y, z, 0, 0) - mean[phi.getPhase(x, y, z)]));
				}
			}
		}
		
		
		for(short i = 1; i <= numPhases; i++)
		{
			var[i] /= phi.getSizePhase(i);
		}	
	}
	
	@Override
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi)
	{
            short oldPhase = phi.getPhase(x, y, z);

            double val = img.getValueDouble(x, y, z, 0, 0);

            double diffOld = (mean[oldPhase] - val);
            double diffNew = (mean[newPhase] - val);

            double sizeOld =  phi.getSizePhase(oldPhase);
            double sizeNew =  phi.getSizePhase(newPhase);

            // update means and variances adaptively
            mean[oldPhase] = mean[oldPhase] + diffOld / (sizeOld - 1);
            mean[newPhase] = mean[newPhase] - diffNew / (sizeNew + 1);
            
            var[oldPhase] = (var[oldPhase] - (diffOld * diffOld) / (sizeOld - 1)) * (sizeOld / (sizeOld - 1));
            var[newPhase] = (var[newPhase] + (diffNew * diffNew) / (sizeNew + 1)) * (sizeNew / (sizeNew + 1));
	}
	
	
	
	@Override
	public double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi)
	{
		short oldPhase = phi.getPhase(x, y, z);
		double val = img.getValueDouble(x, y, z, 0, 0);
		
		double sizeOld =  phi.getSizePhase(oldPhase);
        double sizeNew =  phi.getSizePhase(newPhase);
		double oldMu = mean[oldPhase];
		double newMu = mean[newPhase];
		double oldSigmaSqr = var[oldPhase];
		double newSigmaSqr = var[newPhase];
			
		double sOld = (sizeOld / (sizeOld - 1)) * (oldSigmaSqr - (((val - oldMu) * (val - oldMu)) / (sizeOld - 1)));
		double sNew = (sizeNew / (sizeNew + 1)) * (newSigmaSqr + (((val - newMu) * (val - newMu)) / (sizeNew + 1)));
		
		double delta = sizeNew * Math.log(sNew / newSigmaSqr) + sizeOld * Math.log(sOld / oldSigmaSqr) + Math.log(sNew/sOld);
		
		return delta;
	}
	

	@Override
	public double E(MTBLevelsetMembership phi)
	{
		double e = 0;
		
		for(int z = 0; z < sizeZ; z++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					short phase = phi.getPhase(x, y, z);
					double val = img.getValueDouble(x, y, z, 0, 0);
					
					e += Math.log(var[phase]) + ((val - mean[phase]) * (val - mean[phase])) / (var[phase]) ;
				}
			}
		}
		
		return e;
	}
	

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(this.name + " with " + numPhases + " phases\n");
		for(int i = 1; i <= this.numPhases; i++)
		{
			sb.append("Phase " + i + ": mean = " + mean[i] + ", variance = " + var[i] + "\n");
		}
		return sb.toString();
	}

	@Override
	public void print(MTBLevelsetMembership phi, PrintStream out, String indent)
	{
		out.println(indent + name + " energy = " + E(phi));

		String newIndent = getNewIndent(indent);
		out.print(newIndent + "Means: ");
		for(int i = 1; i <= this.numPhases; i++)
		{
			out.print( indent + mean[i] + "\t");
		}  
		out.println();

        out.println(newIndent + "Variances:");
        for(int i = 1; i <= this.numPhases; i++)
        {
        	out.print(indent + var[i] + "\t");
        }
		out.println();
	}
}
