import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

public class Process_Pixels extends PlugInFrame implements ActionListener
{
	private Panel panel;
	private int previousID;
	private static Frame instance;
	private ImageCanvas canvas;
	
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
		
		final ImagePlus imp = WindowManager.getCurrentImage();
		if(imp != null)
		{
			canvas = imp.getCanvas();	
		}		
		
		setLayout(new FlowLayout());
		
		//Create the GUI panel and add all the controls
		panel = new Panel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
				
		JLabel thresholdLabel = new JLabel("Object threshold");
		c.gridx = 0;
		c.gridy = 0;	
		c.fill = c.NONE;
		panel.add(thresholdLabel, c);		
		
		final JSlider objThresholdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
		objThresholdSlider.setMajorTickSpacing(10);
		objThresholdSlider.setPaintTicks(true);
		c.gridx = 1;
		c.gridy = 0;
		panel.add(objThresholdSlider, c);	
		
		final JTextField objThresholdField = new JTextField(5);
		objThresholdField.setText("0.5");
		c.gridx = 2;
		c.gridy = 0;
		panel.add(objThresholdField, c);
		
		JLabel seedsLabel = new JLabel("Seeds");
		c.gridx = 0;
		c.gridy = 1;
		panel.add(seedsLabel, c);		
		
		final JToggleButton seedsButton = new JToggleButton("Select seeds");
		seedsButton.addActionListener(new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e) 
			{ 				
				IJ.setTool(Toolbar.POINT);				
			} 	
		});
		seedsButton.addKeyListener(IJ.getInstance());
		c.gridx = 1;		
		c.gridy = 1;
		c.gridwidth = 1;
		panel.add(seedsButton, c);
		
		JButton seedsResetButton = new JButton("Reset seeds");
		seedsResetButton.addActionListener(new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e) 
			{ 				
				imp.deleteRoi();
			} 	
		});
		seedsResetButton.addKeyListener(IJ.getInstance());
		c.gridx = 2;		
		c.gridy = 1;
		c.gridwidth = 1;
		panel.add(seedsResetButton, c);
		
		JLabel outputFuzzyLabel = new JLabel("Output fuzzy image");
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		panel.add(outputFuzzyLabel, c);
		
		JCheckBox outputFuzzyCheckbox = new JCheckBox();
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		panel.add(outputFuzzyCheckbox, c);
				
		JLabel outputBinaryLabel = new JLabel("Output binary image");
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		panel.add(outputBinaryLabel, c);		
		
		JCheckBox outputBinaryCheckbox = new JCheckBox();
		c.gridx = 1;
		c.gridy = 3;		 
		c.gridwidth = 2;
		panel.add(outputBinaryCheckbox, c);		
		
		JLabel binaryThresholdLabel = new JLabel("Binarization threshold");	
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		panel.add(binaryThresholdLabel, c);				
		
		JSlider binaryThresholdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
		binaryThresholdSlider.setMajorTickSpacing(10);
		binaryThresholdSlider.setPaintTicks(true);
		c.gridx = 1;
		c.gridy = 4;
		panel.add(binaryThresholdSlider, c);
		
		JTextField binaryThresholdField = new JTextField(5);
		binaryThresholdField.setText("0.5");
		c.gridx = 2;
		c.gridy = 4;
		panel.add(binaryThresholdField, c);
				
		//Create a bottom panel for the main execution controls
		Panel bottomPanel = new Panel();
		bottomPanel.setLayout(new FlowLayout());
		
		JButton runButton = new JButton("Run");
		runButton.addActionListener(new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e) 
			{ 				
				Roi roi = imp.getRoi();
				
				if(roi != null && roi instanceof PointRoi)
				{
					Polygon poly = roi.getPolygon();
					
					IJ.showStatus(Integer.toString(poly.npoints));
				}				
			} 	
		});
		bottomPanel.add(runButton, c);		
		
		JButton clearButton = new JButton("Clear");
		bottomPanel.add(clearButton, c);	
		
		JButton resetButton = new JButton("Reset parameters");
		bottomPanel.add(resetButton, c);			
		
		//Color the bottom panel differently do highlight it
		panel.setBackground(binaryThresholdSlider.getBackground());
		bottomPanel.setBackground(Color.white);
		
		//Add bottom panel at the bottom of the main parameter panel
		c.gridwidth = 3;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 5;
		c.fill = c.BOTH;
		panel.add(bottomPanel, c);
		
		//Add the main parameter panel to our window
		add(panel);		
		
		pack();
		GUI.center(this);
		setVisible(true);
				
		//Connect the slider and text fields so when one updates, so does the other
		bindSliderAndTextField(objThresholdSlider, objThresholdField);
		bindSliderAndTextField(binaryThresholdSlider, binaryThresholdField);
	}
    
    /**
     * Connects a [0, 100] slider to a text field so that when one updates the other
     * follows. The displayed value will also be divided by 100, so that it seems we're
     * manipulating a [0, 1] number
     * @param slider A JSlider with min set to 0 and max set to 100
     * @param text A JTextField
     */
    public void bindSliderAndTextField(final JSlider slider, final JTextField text)
    {
    	slider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
            	text.setText(String.valueOf((slider.getValue()) / 100.0));
            }
        });
    	text.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent ke) {
            	String typed = text.getText();
            	slider.setValue(0);
                if(!typed.matches("\\d+(\\.\\d*)?")) {
                    return;
                }
                double value = Double.parseDouble(typed);
                slider.setValue((int)(value * 100.0));
            }
        });
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
		
		//new Runner(label, imp);
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
