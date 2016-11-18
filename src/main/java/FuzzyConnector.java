import java.util.Arrays;

import ij.ImagePlus;
import ij.ImageStack;

public class FuzzyConnector 
{
	ImagePlus m_img;
	Object[] imagePixels;
	int m_width;
	int m_height;
	int m_depth;
	
	DialCache m_dial;
	float[] m_conScene;
	int[] m_seeds;
	
	float m_avg_ave;
	float m_avg_reldiff;	
	float m_sigma_ave;
	float m_sigma_reldiff;
	
	float m_threshold;
	
	private FuzzyConnector(ImagePlus img, SegmentStack seg, Point3D[] seeds, float threshold)
	{		
		m_img = img;
		ImageStack stack = m_img.getStack();		
		m_width = stack.getWidth();
		m_height  = stack.getHeight();
		m_depth  = stack.getSize();    		
		
    	Object[] imagePixels = new Object[m_width * m_height * m_depth];
    	m_conScene = seg.stack;
    	    	
    	//Grab references to the pixels of the entire stack
    	int sliceSize = m_width * m_height;
    	for(int i = 0; i < m_depth; i++)
    	{    		
    		Object[] slicePixels = (Object[]) stack.getProcessor(i + 1).getPixels();
    		System.arraycopy(slicePixels, 0, imagePixels, i * sliceSize, sliceSize);
    	}    	
		
    	m_seeds = new int[seeds.length];
    	for(int i = 0; i < seeds.length; i++)
    	{
    		Point3D pt = seeds[i];    		
    		m_seeds[i] = pt.x + pt.y * m_width + pt.z * sliceSize;
    	}
    	
		m_dial = new DialCache();		
		m_threshold = threshold;
	}	
	
	private static float gaussian(float val, float avg, float sigma)
	{
		return 0;
	}
	
	private static float affinity(int c, int d)
	{
		return 0;
	}
	
	private static boolean adjacency(int c, int d)
	{
		return false;
	}
	
	private static float ave(int c, int d)
	{
		return 0.5f;
	}
	
	public static float[] run(ImagePlus image, SegmentStack seg, Point3D[] seeds, float threshold)
	{
		return null;
	}
}
