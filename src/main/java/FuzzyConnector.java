import java.util.HashSet;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

public class FuzzyConnector 
{
	static ImagePlus m_img;
	static short[] m_imagePixels;
	static int m_width;
	static int m_height;
	static int m_depth;
	
	static int m_pixelsPerSlice;
	
	static DialCache m_dial;
	static float[] m_conScene;
	static int[] m_seeds;
	
	static float m_mean_ave;
	static float m_mean_reldiff;	
	static float m_sigma_ave;
	static float m_sigma_reldiff;
	
	static float m_threshold;
	
	private static void initialize(ImagePlus img, SegmentStack seg, float threshold)
	{		
		m_img = img;
		ImageStack stack = m_img.getStack();		
		m_width = stack.getWidth();
		m_height  = stack.getHeight();
		m_depth  = stack.getSize();    		
		
		m_pixelsPerSlice = m_width * m_height;
		
    	m_imagePixels = new short[m_pixelsPerSlice * m_depth];
    	seg.stack = new float[m_pixelsPerSlice * m_depth];
    	m_conScene = seg.stack;
    	    	
    	//Grab references to the pixels of the entire stack
    	for(int i = 0; i < m_depth; i++)
    	{    		
    		short[] slicePixels = (short[]) stack.getProcessor(i + 1).getPixels();
    		System.arraycopy(slicePixels, 0, m_imagePixels, i * m_pixelsPerSlice, m_pixelsPerSlice);
    	}    	

		Calibration calib = img.getCalibration();			
		double[] coeffs = calib.getCoefficients();
		double a = coeffs[1];
		double b = coeffs[0];
		    	
		System.out.println("Calibrating image range...");
    	for(int i = 0; i < m_depth * m_pixelsPerSlice; i++)
    	{
    		m_imagePixels[i] = (short)(m_imagePixels[i] * a + b);
    	}
		
    	m_seeds = new int[seg.seeds.size()];
    	for(int i = 0; i < seg.seeds.size(); i++)
    	{
    		Point3D pt = seg.seeds.get(i);    		
    		m_seeds[i] = pt.x + pt.y * m_width + pt.z * m_pixelsPerSlice;
    		
    		//Set this voxel as 1.0 in the connectivity scene, since its a seed
    		m_conScene[m_seeds[i]] = 1.0f;
    	}
    	
		m_dial = new DialCache();		
		m_threshold = threshold;
		
		calculateMeansAndSigmas();
	}	
	
	/**
	 * Sets the values of m_mean_ave, m_sigma_ave, m_mean_reldiff, m_sigma_reldiff
	 * to the corresponding mean and standard deviation values of ave and reldiff between
	 * all combinations of neighbors around all seeds (3x3x3 centered on each)
	 */
	private static void calculateMeansAndSigmas()
	{		
		//Will never add duplicates, since we use HashSets
		HashSet<Integer> spels = new HashSet<Integer>();		
		for(int i : m_seeds)
		{						
			int[] neighbors = getNeighbors(i);	
			for(int j : neighbors)
			{
				if(j == -1)
					continue;
				
				int[] neighborsNeighbors = getNeighbors(j);
				
				for(int k : neighborsNeighbors)
				{
					if(k == -1)
						continue;
					
					spels.add(k);
				}
			}					
		}
		
		//Push all combinations of ave and reldiff to the arrays
		int numSpels = spels.size();		
		
		//Weird java stuff to get all spels from the hasmap into an int array
		Integer[] temp = spels.toArray(new Integer[numSpels]);
		int[] spelsArray = new int[temp.length];
		for(int i = 0; i < numSpels; i++)
			spelsArray[i] = temp[i];
		
		int numCombinations = (numSpels * (numSpels - 1)) / 2;
		float[] aves = new float[numCombinations];
		float[] reldiffs = new float[numCombinations];
		
		int count = 0;		
		for(int i = 0; i < numSpels - 1; i++)
		{
			for(int j = i+1; j < numSpels; j++)
			{
				aves[count] = ave(spelsArray[i], spelsArray[j]);
				reldiffs[count] = reldiff(spelsArray[i], spelsArray[j]);
				
				count++;
			}
		}	
		
		float[] ave_meanSigma = welford(aves);
		float[] reldiff_meanSigma = welford(reldiffs);	
		
		m_mean_ave = ave_meanSigma[0];
		m_sigma_ave = ave_meanSigma[1];
		
		m_mean_reldiff = reldiff_meanSigma[0];
		m_sigma_reldiff = reldiff_meanSigma[1];
		
		System.out.println("ave mean: " + m_mean_ave);
		System.out.println("ave sigma: " + m_sigma_ave);
		System.out.println("reldiff mean: " + m_mean_reldiff);
		System.out.println("reldiff sigma: " + m_sigma_reldiff);		
	}
	
	/**
	 * Single-pass average and standard deviation calculation
	 * https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
	 * @param elements list of elements to calculate the average and standard deviation of
	 * @return length 2 float array with [average, standardDeviation]
	 */
	private static float[] welford(float[] els)
	{
		int n = 0;
		double mean = 0;
		double M2 = 0;
		double var = 0;
		double delta = 0;
		
		for(float x : els)
		{
			n += 1;			
			delta = x - mean;
			mean += delta / n;
			M2 += delta * (x - mean);
		}
		
		if(n < 2)
			return null;
		else
			var = M2 / (n - 1);
		
		float[] result = new float[2];
		result[0] = (float)mean;
		result[1] = (float)Math.sqrt(var);
		
		return result;
	}
	
	private static float gaussian(float val, float avg, float sigma)
	{
		return (float) Math.exp(-(1.0/(2*sigma*sigma)) * (val - avg) * (val - avg));
	}
	
	private static float affinity(int c, int d)
	{								
		float g_ave = gaussian(ave(c, d), m_mean_ave, m_sigma_ave);
		float g_reldiff = gaussian(reldiff(c, d), m_mean_reldiff, m_sigma_reldiff);
		
		return Math.min(g_ave, g_reldiff);
	}
	
	private static float ave(int c, int d)
	{		
		return 0.5f * ((float)m_imagePixels[c] + (float)m_imagePixels[d]);
	}
	
	private static float reldiff(int c, int d)
	{		
		float fc = m_imagePixels[c];
		float fd = m_imagePixels[d];
		
		return fc == -fd ? 0 : (Math.abs(fc - fd)) / (fc + fd);
	}
	
	private static int[] getNeighbors(int c)
	{
		int z = c / m_pixelsPerSlice;
		int y = (c % m_pixelsPerSlice) / m_width;
		int x = (c % m_pixelsPerSlice) % m_width; 
		
		int[] result = new int[6];
		result[0] = x < m_width-1 ? 			c + 1 : -1;
		result[1] = x > 0 ? 					c - 1 : -1;
		result[2] = y < m_height-1 ? 			c + m_width : -1;
		result[3] = y > 0 ? 					c - m_width : -1;
		result[4] = z < m_depth-1 ?			 	c + m_pixelsPerSlice : -1;
		result[5] = z > 0 ? 					c - m_pixelsPerSlice : -1;
		
		return result;
	}
	
	public static float[] run(ImagePlus image, SegmentStack seg, float threshold)
	{
		initialize(image, seg, threshold);		
		
		//Push all seeds o to Q
		for(int s : m_seeds)
		{
			m_dial.Push(s, DialCache.MaxIndex);
		}		
		
		while(m_dial.m_size > 0)
		{
			int c = m_dial.Pop();
			
			int[] neighbors = getNeighbors(c);
			for(int e : neighbors)
			{
				//We get -1 when we are at an edge (e.g. on first row and want the neighbor on the row below)
				if(e == -1) 
					continue;
				
				float aff_c_e = affinity(c, e);
				
				if(aff_c_e < m_threshold) 
				{
					m_dial.Visit(e);
					continue;
				}						
				
				float f_min = Math.min(m_conScene[c], aff_c_e);
				if(f_min > m_conScene[e])
				{
					m_conScene[e] = f_min;
										
					if(m_dial.Contains(e))
						m_dial.Update(e, (int)(DialCache.MaxIndex * f_min + 0.5f));
					else
						m_dial.Push(e, (int)(DialCache.MaxIndex * f_min + 0.5f));
				}
			}
		}
		
		System.out.println("Done");
		IJ.showStatus("Done!");
		return m_conScene;
	}
}
