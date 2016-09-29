import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.Roi;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

public class Process_Pixels extends PlugInFrame implements ActionListener
{
	private Panel panel;
	private int previousID;
	private static Frame instance;
	
	class Runner extends Thread 
	{ 
		private String command;
		private ImagePlus imp;
	
		Runner(String command, ImagePlus imp) 
		{
			super(command);
			this.command = command;
			this.imp = imp;
			setPriority(Math.max(getPriority()-2, MIN_PRIORITY));
			start();
		}
	
		public void run() 
		{
			try 
			{
				runCommand(command, imp);
			}
			catch(OutOfMemoryError e) 
			{
				IJ.outOfMemory(command);
				if (imp!=null) imp.unlock();
			}
			catch(Exception e) 
			{
				CharArrayWriter caw = new CharArrayWriter();
				PrintWriter pw = new PrintWriter(caw);
				e.printStackTrace(pw);
				IJ.log(caw.toString());
				IJ.showStatus("");
				if (imp!=null) imp.unlock();
			}
		}
	
		void runCommand(String command, ImagePlus imp) 
		{
	    	//byte[] pixels = (byte[])ip.getPixels();
	    	//int width = ip.getWidth();
	    	//Rectangle r = ip.getRoi();
	    	//int offset, i;
	    	//for (int y=r.y; y<(r.y+r.height); y++) {
	    	//offset = y*width;
	    	//for (int x=r.x; x<(r.x+r.width); x++) {
	    	//i = offset + x;
	    	//pixels[i] = (byte)(255-pixels[i]);
	    	//}
	    	//}
	    	
	    	/*
	    	 *  If we cast a byte variable to another type we have to make sure that the sign bit is eliminated. This can be done using a binary AND:int pix = 0xff & pixels[i];
	    	 * 
	    	 */
	    	
	    	//drawDot
	    	
	        // Here is the action    	 
	    	//int channels = ip.getNChannels();
	    	//boolean gray = ip.isGrayscale();
			
			ImageProcessor ip = imp.getProcessor();
			IJ.showStatus(command + "...");			
			long startTime = System.currentTimeMillis();
			Roi roi = imp.getRoi();
			
			if (command.startsWith("Zoom")||command.startsWith("Macro")||command.equals("Threshold"))
			{
				roi = null; ip.resetRoi();
			}
			
			ImageProcessor mask = roi!=null? roi.getMask() : null;
			
			if (command.equals("Reset"))
				ip.reset();
			else if (command.equals("Flip"))
				ip.flipVertical();
			else if (command.equals("Invert"))
				ip.invert();
			else if (command.equals("Lighten")) {
				if (imp.isInvertedLut())
					ip.multiply(0.9);
				else
					ip.multiply(1.1);
			}
			else if (command.equals("Darken")) {
				if (imp.isInvertedLut())
					ip.multiply(1.1);
				else
					ip.multiply(0.9);
			}
			else if (command.equals("Rotate"))
				ip.rotate(30);
			else if (command.equals("Zoom In"))
				ip.scale(1.2, 1.2);
			else if (command.equals("Zoom Out"))
				ip.scale(.8, .8);
			else if (command.equals("Threshold"))
				ip.autoThreshold();
			else if (command.equals("Smooth"))
				ip.smooth();
			else if (command.equals("Sharpen"))
				ip.sharpen();
			else if (command.equals("Find Edges"))
				ip.findEdges();
			else if (command.equals("Add Noise"))
				ip.noise(20);
			else if (command.equals("Reduce Noise"))
				ip.medianFilter();
			
			if (mask!=null)
				ip.reset(mask);
			
			imp.updateAndDraw();
			imp.unlock();
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
		}	
	}
	
    public Process_Pixels() 
    {
		super("Process pixels");
		if (instance!=null) {
			instance.toFront();
			return;
		}		
		instance = this;
		addKeyListener(IJ.getInstance());
		
		setLayout(new FlowLayout());
		panel = new Panel();
		panel.setLayout(new GridLayout(4, 4, 5, 5));
		addButton("Reset");
		addButton("Flip");
		addButton("Invert");
		addButton("Rotate");
		addButton("Lighten");
		addButton("Darken");
		addButton("Zoom In");
		addButton("Zoom Out");
		addButton("Smooth");
		addButton("Sharpen");
		addButton("Find Edges");
		addButton("Threshold");
		addButton("Add Noise");
		addButton("Reduce Noise");
		addButton("Macro1");
		addButton("Macro2");
		add(panel);
		
		pack();
		GUI.center(this);
		setVisible(true);
	}


	void addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
		panel.add(b);
	}
    
	public int setup(String arg, ImagePlus image) 
    {
		IJ.showMessage("setup called");
		return 0;
    }    
    
	public void run(String arg) 
	{
		//IJ.showMessage("run called");
	}
    
	public void actionPerformed(ActionEvent e) 
	{
		ImagePlus imp = WindowManager.getCurrentImage();
		
		if (imp==null) 
		{
			IJ.beep();
			IJ.showStatus("No image");
			previousID = 0;
			return;
		}
		
		if (!imp.lock())
		{
			previousID = 0; 
			return;
		}
		
		int id = imp.getID();
		if (id!=previousID)
			imp.getProcessor().snapshot();
		previousID = id;
		
		String label = e.getActionCommand();
		if (label==null)
			return;
		
		new Runner(label, imp);
	}
    
	public void processWindowEvent(WindowEvent e) 
	{
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) 
			instance = null;
	}

	public static void main(String[] args) 
	{
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Process_Pixels.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/lena.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");		
	}
}
