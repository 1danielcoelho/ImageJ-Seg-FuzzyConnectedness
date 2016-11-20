import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;

import ij.ImagePlus;
import ij.ImageStack;

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
		
    	short[] imagePixels = new short[m_pixelsPerSlice * m_depth];
    	m_conScene = seg.stack;
    	    	
    	//Grab references to the pixels of the entire stack
    	for(int i = 0; i < m_depth; i++)
    	{    		
    		short[] slicePixels = (short[]) stack.getProcessor(i + 1).getPixels();
    		System.arraycopy(slicePixels, 0, imagePixels, i * m_pixelsPerSlice, m_pixelsPerSlice);
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
		//TODO: handle case where seed is on the edge shell cube of the volume
		
		//Get all neighbors around a seed in a 3x3 cube (including the seed itself)
		//Will never add duplicates, since we use HashSets
		HashSet<Integer> neighbors = new HashSet<Integer>();		
		for(int i : m_seeds)
		{			
			for(int x = -1; x <= 1; x++)
			{
				for(int y = -1; y <= 1; y++)
				{
					for(int z = -1; z <= 1; z++)
					{					
						neighbors.add(i + x * 1 + y * m_width + z * m_pixelsPerSlice);
					}
				}
			}
		}
		
		//Push all combinations of ave and reldiff to the arrays
		int numEls = neighbors.size();		
		float[] aves = new float[numEls * (numEls - 1)];
		float[] reldiffs = new float[numEls * (numEls - 1)];
		int count = 0;
		
		for(int i = 0; i < numEls - 1; i++)
		{
			for(int j = i+1; j < numEls; j++)
			{
				aves[count] = ave(i, j);
				reldiffs[count] = reldiff(i, j);
				
				count++;
			}
		}
		
		float[] ave_meanSigma = welford(aves);
		float[] reldiff_meanSigma = welford(reldiffs);	
		
		m_mean_ave = ave_meanSigma[0];
		m_sigma_ave = ave_meanSigma[1];
		
		m_mean_reldiff = reldiff_meanSigma[0];
		m_sigma_reldiff = reldiff_meanSigma[1];
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
		//TODO: Handle negative image pixel values
						
		float g_ave = gaussian(ave(c, d), m_mean_ave, m_sigma_ave);
		float g_reldiff = gaussian(reldiff(c, d), m_mean_reldiff, m_sigma_reldiff);
		
		return Math.min(g_ave, g_reldiff);
	}
	
	private static float ave(int c, int d)
	{		
		return 0.5f * (float)m_imagePixels[c] * (float)m_imagePixels[d];
	}
	
	private static float reldiff(int c, int d)
	{		
		float fc = m_imagePixels[c];
		float fd = m_imagePixels[d];
		
		return fc == -fd ? 0 : (Math.abs(fc - fd)) / (fc + fd);
	}
	
	private static int[] getNeighbors(int c)
	{
		int[] result = new int[6];
		result[0] = c - 1;
		result[1] = c + 1;
		result[2] = c + m_width;
		result[3] = c - m_width;
		result[4] = c + m_pixelsPerSlice;
		result[5] = c + m_pixelsPerSlice;
		
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
				float aff_c_e = affinity(c, e);
				
				if(aff_c_e < m_threshold) 
					continue;
				
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
		
		return null;
	}
}
