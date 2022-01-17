package dimred;

import ij.*;
import ij.gui.Plot;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;

import com.jujutsu.utils.MatrixUtils;

import tagbio.umap.Umap;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import plot.plot.SaveFxPlot;

/**version 1.0.4*/

/**
 * UMAP java implementation using library from https://github.com/tag-bio/umap-java
 * Process an image-stack, pass a directory of images to the plugin, or choose one when a dialog opens.
 */
public class Umap_ implements PlugIn {
	//Top level initialisation
	int choice; //JOption 'process the image stack?' choice outcome
	public static ImagePlus stack;
	double[][] imageMatrix;
	String[] Filelist;
	public static String[] labelsArray;
	public static String[] uniqueArray;
	private boolean processingStack = false;
	public static double[] Xarray;
    public static double[] Yarray;
    static String groupLabel;
	static double[] XXarray;
	static double[] YYarray;
	static Color ranCol;
	public static Color[] groupColours;
	public static int[][] lookupArray;
	public static String xTitle;
	public static String yTitle;
	public static String plotTitle;
	//Fx top level initialisation
	//private static JFXPanel fxPanelRef;
	private static NumberAxis xAxis_Fx;
	private static NumberAxis yAxis_Fx;
	private static ScatterChart<Number,Number> sc_Fx;
	private static Scene scene;
	private static Label cursorCoords = null;
	private static boolean multiEnabled = false;
	static Path multiPath = new Path();
	static Pane pathPane = new Pane();
	static double xPathOffset = 0;
	static double yPathOffset = 0;
    static double mouseStartX;
    static double mouseStartY;
	
    // Options to use during the run. Defaults for some but otherwise populated when parseOptions() is called.
    private String inputFolderPath;
    private String inputIndexPath; //show the plugin where sample labels are located.. used to colour the final output.
    private String nNeighbours;
    	private static int neighbours = 15;
    private String nThreads;
    	private int threads = 1;
	public static String metric;
	public static String minDis;
		private static double minDist = 0.1;
    private String ranSeed;
		private int seed = 5;
    int availableThreads = Runtime.getRuntime().availableProcessors(); //Get available threads so that we can default to 1 if the user selects more.
    private boolean outputCSV = false; //create a CSV copy of the dimensionality-reduction output XYs on the users desktop.
    boolean cancelled; //JOption cancel flag.
    private boolean suppressStackAsk = false;
    
    // Variables and main() method for testing in IDEs.
    private static String debugOptions = null;
    private static double[][] debugArray = null;
    public static void main(String[] args) {
        // To debug, set a CSV file to open, we'll use that in the ResultsTable for the run.
        debugArray = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        new Umap_().run("");
    }
    

    public void run(String arg) {
        // Fill in the options we'll use during this run.
        parseOptions();
        processingStack = false;
        if (ranSeed != null && !("").equals(ranSeed) && !ranSeed.matches(".*[A-Za-z].*") && Double.valueOf(ranSeed) > 0) {
        	seed = Integer.valueOf(ranSeed);
        }
		Random rand = new Random();
		rand.setSeed(seed);
        
        if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack()) {
        	int type = WindowManager.getCurrentImage().getType();
        	if (!suppressStackAsk) {
        		choice = JOptionPane.showConfirmDialog(null, "Do you want to process the image stack?", "UMAP option",JOptionPane.YES_NO_CANCEL_OPTION);
        	} else {
        		choice = JOptionPane.OK_OPTION;
        	}
            if (choice == JOptionPane.CANCEL_OPTION) {return;}
            else if (choice == JOptionPane.OK_OPTION) {
            	processingStack = true;
            	stack = WindowManager.getCurrentImage();
            	Filelist = new String[stack.getStackSize()];
            	int height = stack.getHeight();
            	int width = stack.getWidth();
            	imageMatrix = new double[stack.getStackSize()][width*height]; //2D array to hold all images in stack [slice number][pixels as 1D array]
            	for (int s = 0; s < stack.getStackSize(); s++) {
            		Filelist[s] = Integer.toString(s+1);
            		stack.setSlice(s+1);
            		ImageProcessor sip = stack.getProcessor();  	
                	float[] image1DArray = new float[width*height];	//try double, then float
                	for (int j = 0; j < height; j++) {
                		for (int k = 0; k < width; k++) {
                			if (type == ImagePlus.COLOR_RGB) {
                				image1DArray[k+(j*width)] = sip.get(k, j);
                			} else {
                				image1DArray[k+(j*width)] = (float) sip.getInterpolatedValue(k,j); //not sure if casting is appropriate here
                			}
                		}
                	}
                	for(int l = 0; l < (width*height); l++) {
                		imageMatrix[s][l] = image1DArray[l];
                	}
            	}
            	//stack.close();
            	stack.setSlice(0);
            	stack.updateAndDraw();
            } else {
            	DirectoryChooser dc = new DirectoryChooser("Select a folder");
                inputFolderPath = dc.getDirectory();
                if (inputFolderPath == null) {
                	return;
                }
            }
        }
        //specify a folder to perform uMAP on
        else if (inputFolderPath.isEmpty() && debugArray == null) {
        DirectoryChooser dc = new DirectoryChooser("Select a folder of images to process");
        inputFolderPath = dc.getDirectory();
	        if (inputFolderPath == null) {
	        	return;
	        }
    	}
    	
    	if (debugArray != null) {
    		Filelist = new String[2500]; //Dirty Filelist creation for debugging
    	} else if (!inputFolderPath.isEmpty()) {
    		//Generate an array containing the file names to process, also allowing us to count them. NOTE: order is not ensured, without sorting.
    		Filelist = (new File(inputFolderPath)).list();
    		// Attempt to sort the Filelist numerically.. Arrays.sort sorts by ASCII values, which results in a strange order.
    		Arrays.sort(Filelist, new Comparator<String>() {
    			public int compare(String s1, String s2) {
    				try {
    		            int i1 = Integer.parseInt((s1).replaceAll("[^0-9]", "")); //Some chance for NumberFormatExceptions using this Regex.
    		            int i2 = Integer.parseInt((s2).replaceAll("[^0-9]", ""));
    		            return i1 - i2;
    		        } catch(NumberFormatException e) {
    		            throw new AssertionError(e);
    				}
    			}
    		});
    		//IJ.log("File list: "+Arrays.toString(Filelist));
    			
    		if (Filelist==null) {
    			IJ.log("The specified directory does not contain images to process.");
    			return;
    		}
    	}
		
    if (debugArray == null && !processingStack) {
		//Open the first image in the folder to get dimensions.. could have done it in the loop
		ImagePlus imp0 = IJ.openImage(inputFolderPath + Filelist[0]);
    	int width = imp0.getWidth();
    	int height = imp0.getHeight();
    	imp0.close();
		
		imageMatrix = new double[Filelist.length][width*height];
		IJ.log("Number of images = "+imageMatrix.length);
		IJ.log("Dimensions (pixels) = "+imageMatrix[0].length);

        for (int i = 0; i < Filelist.length; i++) {
        	if (IJ.escapePressed()) break;
			String path = inputFolderPath + Filelist[i];
        	
        	ImagePlus imp = IJ.openImage(path);
        	if (imp==null) {
				IJ.log("openImage() returned null: "+path);
				continue;
			}
        	
        	int type= imp.getType();
        	ImageProcessor ip = imp.getProcessor();
        	
        	float[] image1DArray = new float[width*height];
        	
        	for (int j = 0; j < height; j++) {
        		for (int k = 0; k < width; k++) {
        			if (type == ImagePlus.COLOR_RGB) {
        				image1DArray[k+(j*width)] = ip.get(k, j);
        			} else {
        				image1DArray[k+(j*width)] = (float) ip.getInterpolatedValue(k,j); //not sure if casting is appropriate here
        			}
        		}
        	}
        	imp.close();
        	
        	for(int l = 0; l < (width*height); l++) {
        		imageMatrix[i][l] = image1DArray[l];
        	}
        	
        }
    }
        
    	//UMAP here
		if (nNeighbours != null && !("").equals(nNeighbours) && !nNeighbours.matches(".*[A-Za-z].*") && Double.valueOf(nNeighbours) > 0) {
			neighbours = Integer.valueOf(nNeighbours);
		}
		if (nThreads != null && !("").equals(nThreads) && !nThreads.matches(".*[A-Za-z].*") && Double.valueOf(nThreads) > 0) {
			if (Double.valueOf(nThreads) > availableThreads) {
				IJ.log("More CPU threads specified than available for use. The default of 1 has been set.");
			} else {
			threads = Integer.valueOf(nThreads);
			}
		}
		if (minDis != null && !("").equals(minDis) && !minDis.matches(".*[A-Za-z].*") && Double.valueOf(minDis) > 0) {
			minDist = Double.valueOf(minDis);
		}
        final Umap umap = new Umap();
        umap.setNumberComponents(2); // number of dimensions in result
        umap.setNumberNearestNeighbours(neighbours); //Larger values result in more global views of the manifold, while smaller values result in more local data being preserved. In general values should be in the range 2 to 100. The default is 15.
        umap.setThreads(threads); // use > 1 to enable parallelism. Over 1 will prevent a deterministic result.
        umap.setMetric(metric);
        umap.setMinDist((float)minDist);
        final double[][] Y = umap.fitTransform(imageMatrix);
        
        //Get and use labels if a .csv file is specified.
        if (!inputIndexPath.isEmpty() && inputIndexPath != null) {
        	labelsArray = null;
        	try {
				labelsArray = getLabels(inputIndexPath);
			} catch (IOException e) {
				IJ.handleException(e);
			}
        	if (labelsArray == null) {
        		IJ.log("The specified labels file is empty or corrupt.");
        	}
        	if (labelsArray.length != Y.length) {
        		IJ.log("The number of labels in the labels file does not match the number of processed images, so it has not been used to assign groupings.");
        		IJ.log("Y.length = "+String.valueOf(Y.length)+"; Label number = "+String.valueOf(labelsArray.length));
        		IJ.log(Arrays.toString(labelsArray));
        		labelsArray = null;
        	}
        } else {
        	labelsArray = null;	//if this is not here, then the labelsArray String[] can remain populated from previous runs
        }
        
    	//If the outputCSV toggle is set, output the UMAP result to the users desktop
        if (outputCSV) {
        	try {
        		toCSV(Y, Filelist);
        	} catch (IOException e) {
        		IJ.handleException(e);
        	}
        }
        
        Xarray = new double[Y.length];
        Yarray = new double[Y.length];
        for (int m = 0; m < Y[0].length; m++) { //columns
        	for (int n = 0; n < Y.length; n++) { //rows
        		if (m==0) {
        			Xarray[n] = Y[n][m];
        		} else {
        			Yarray[n] = Y[n][m];
        		}
        	}
        }     
        
        plotTitle = new String("UMAP output (nNeighbours = "+String.valueOf(neighbours)+"; metric = "+metric+")");
        xTitle = "UMAP 1";
        yTitle = "UMAP 2";
    	Plot scatter = new Plot(plotTitle, xTitle, yTitle);
    	
    	Runnable runnable = () -> {
            initAndShowGUI();
        };
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        SwingUtilities.invokeLater(task);
        try {
			task.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		    	
    	if (labelsArray == null) {
    		scatter.setLineWidth(5);
    		scatter.addPoints(Xarray, Yarray, 6);
    		scatter.show();
    		uniqueArray = new String[1];
    		uniqueArray[0] = "Data";
    		labelsArray = new String[Xarray.length];	//Creating and filling the label array is maybe not ideal
    		Arrays.fill(labelsArray, "Data");
    		groupColours = new Color[1];
    		groupColours[0] = Color.BLACK;
    		
    		//javafx code below
			groupLabel = "Data";
        	Platform.runLater(new Runnable() {
        		@Override
    	    	public void run() {
        			sc_Fx = addSeries(sc_Fx, Xarray, Yarray, groupLabel, Color.BLACK);
        		}
    		});
        	lookupArray = new int[1][Xarray.length];
        	//Arrays.setAll(lookupArray, i -> i + 1); //lambda equivalent of below loop
        	for (int z = 0; z < Xarray.length; z++) {
        		lookupArray[0][z] = z+1;
        	}
    	} else {
    		//some method to count unique entries (groups) in the labels array here. I expect processing overhead time-loss associated with this 'else' code.
    		ArrayList<String> uniqueArrayList = new ArrayList<>();
    	    for(int i=0; i<labelsArray.length; i++){
    	        if(!uniqueArrayList.contains(labelsArray[i])){
    	            uniqueArrayList.add(labelsArray[i]);
    	        }
    	    }
    		int groupN = uniqueArrayList.size();
    		uniqueArray = new String[groupN];
    		uniqueArrayList.toArray(uniqueArray);
    		lookupArray = new int[groupN][];	// Set the number of label groups to a staggered lookup table 2D array... will be used to find the position of images in the original stack
    		
    		groupColours = new Color[groupN];
    		for (int y = 0; y < groupN; y++) {
    			int counter = 0;
    			for (int z = 0; z < labelsArray.length; z++) {	
    				if (labelsArray[z].equals(uniqueArray[y])) {
    					counter++;
    				}
    			}
    			lookupArray[y] = new int[counter];
    				//IJ.log(uniqueArray[y]+" is present "+String.valueOf(counter)+" time(s) in the labelsArray.");
    			XXarray = new double[counter];
    			YYarray = new double[counter];
    			counter = 0;
    			for (int z = 0; z < labelsArray.length; z++) {
    				if (labelsArray[z].equals(uniqueArray[y])) {
    					XXarray[counter] = Xarray[z];
    					YYarray[counter] = Yarray[z];
    					lookupArray[y][counter] = z+1;
    					counter++;  					
    				}
    			}
        		//Randomly generate an rgb colour and add the datapoint of that colour to the plot.
    			int r = rand.nextInt(230);
    			int g = rand.nextInt(230);
    			int b = rand.nextInt(230);
    			ranCol = new Color(r,g,b);
    			groupColours[y] = ranCol;
    			scatter.setColor(ranCol);
    			scatter.setLineWidth(5);
    			scatter.addPoints(XXarray, YYarray, 6);
    			
    			//javafx code below
				groupLabel = uniqueArray[y];
				final int w = y;
		    	Runnable runnable2 = () -> {
		    		sc_Fx = addSeries(sc_Fx, XXarray, YYarray, groupLabel, groupColours[w]);
		    	};
	    		FutureTask<Void> task2 = new FutureTask<>(runnable2, null);
	            Platform.runLater(task2);
	            try {
	    			task2.get();
	    		} catch (InterruptedException e) {
	    			e.printStackTrace();
	    		} catch (ExecutionException e) {
	    			e.printStackTrace();
	    		}

    		}
    		
    		StringBuilder sb = new StringBuilder();
    		for (int y = 0; y < groupN; y++) {
    			if (y != groupN-1) {
    				sb.append(uniqueArray[y]);
    				sb.append(System.getProperty("line.separator"));
    			} else {
    				sb.append(uniqueArray[y]);
    			}
    		}
    		String legendLabels = sb.toString();
    		scatter.setLineWidth(1);
    		scatter.addLegend(legendLabels);
    		scatter.show();
    		scatter.setLimitsToFit(true);
    		//scatter.setLegend("", Plot.AUTO_POSITION);	//automatically add the legend... off by default as it runs off the chart when many labels are present
    		//IJ.log("File list: "+Arrays.toString(Filelist));
    	}
    	
    	/*
    	//Code block for updating the legend from https://stackoverflow.com/questions/12197877/javafx-linechart-legend-style... did not work for me but is an interesting method to access chart subcomponents
    	Platform.runLater(new Runnable() {
    	    @Override
    	    public void run() {
	    	for(Node n : sc_Fx.getChildrenUnmodifiable()){
	    		if(n instanceof Legend){
	    			int i = 0;
	    			for(Legend.LegendItem legendItem : ((Legend)n).getItems()){
	    				//legendItem.setSymbol(new Circle(2));
	    				legendItem.getSymbol().setStyle(symbolStyles.get(0));    				
	    				IJ.log(legendItem.getText());
	    				IJ.log(symbolStyles.get(0));
	    				i++;
	    			}
	    		}
	    	}
    	    }
    	});
    	*/
    	
    	//Code block for updating the legend adapted from https://stackoverflow.com/questions/34881129/javafx-scatter-chart-custom-legend
    	Platform.runLater(new Runnable() {
    	    @Override
    	    public void run() {
		    	Set<Node> items = sc_Fx.lookupAll("Label.chart-legend-item");
		        int it=0;
		        for (Node item : items) {
		        	Label label = (Label) item;
		            XYChart.Data<Number, Number> newLegendPoint = new XYChart.Data<Number, Number>();	//create a new datapoint to put into the legend and give it the same style and colours as the corresponding chart data series
		            newLegendPoint.setNode(new Circle(2)); //if I allow different shapes, this will need to be populated procedurally
		            Node node = newLegendPoint.getNode();
		            String hex = "#"+Integer.toHexString(groupColours[it].getRGB()).substring(2);
		            node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
		            label.setGraphic(node);
		            //label.setGraphic(nodeList.get(it));	//the approach from stackoverflow, which almost worked (it moved a datapoint to the legend instead of only copying the graphics)		            
                    label.getGraphic().setCursor(Cursor.HAND); // Hint to user that legend symbol is clickable
                    label.getGraphic().setOnMouseClicked(me -> {
                    	//IJ.log("Legend item pressed.");
                    	if (!label.isUnderline()) {
                    		label.setUnderline(true);
                    	} else {
                    		label.setUnderline(false);
                    	}
                    	for (XYChart.Series<Number, Number> s : sc_Fx.getData()) {
                    		if (s.getName().equals(label.getText())) {
		                		for (XYChart.Data<Number, Number> d : s.getData()) {
		                			if (d.getNode().isVisible()) {
		                				d.getNode().setVisible(false);
		                			} else {
		                				d.getNode().setVisible(true);
		                			}
		                		}
                    		}
                    	}
                    });
		            it++;
		        }
		        //more run() code can go here if needed.
	    	}
    	});
    }
    
    /*
     * https://stackoverflow.com/questions/34958829/how-to-save-a-2d-array-into-a-text-file-with-bufferedwriter
     */
    public static void toCSV(double array[][], String Filelist[]) throws IOException {
  	  StringBuilder builder = new StringBuilder();
  	  for(int i = 0; i < array.length; i++)//for each row
  	  {
  	     for(int j = 0; j < array[0].length; j++)//for each column
  	     {
  	    	 if (j == 0) {
  	    		 builder.append(Filelist[i]);
  	    		 builder.append(",");
  	    	 }
  	    	 builder.append(array[i][j]+"");//append to the output string
  	    	 if(j < array.length - 1)//if this is not the last row element
  	    		 builder.append(",");//then add comma (if you don't like commas you can use spaces)
  	     }
  	     builder.append("\n");//append new line at the end of the row
  	  }
  	  String desktopPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "tSNE_result.csv";
  	  BufferedWriter writer = new BufferedWriter(new FileWriter(""+desktopPath));
  	  writer.write(builder.toString());//save the string representation of the board
  	  writer.close();
  	  }
    
    public String[] getLabels( String labelIndexPath) throws IOException {
    	inputIndexPath = labelIndexPath;
    	BufferedReader br = null;
    	List<String> labelList = new ArrayList<String>();
    	
    	if (inputIndexPath.endsWith(".csv")) {
    		try {
    			String line = "";
    			br = new BufferedReader(new FileReader(inputIndexPath));
    			int lineCounter = 0;
    			while ((line = br.readLine()) != null) {
    				if (lineCounter == 0) {
    					lineCounter++;
    					continue;
    				} else {
    					lineCounter++;
    					labelList.add(line.replaceAll(",", ""));
    				}
    				
    			}
    			} catch (FileNotFoundException e) {
    			IJ.handleException(e);
    			} catch (IOException e) {
    			IJ.handleException(e);} finally {
    				if (br != null) {
    					try {
    						br.close(); 
    					} catch (IOException e) {
    						IJ.handleException(e);
    					}
    				}
    			}
    	} else {
    		IJ.log("The user has specified a labels file but it is not a .csv.");
    	}
    	String[] labels = new String[labelList.size()];
    	//IJ.log("labelList size is: "+String.valueOf(labelList.size()));
    	labels = labelList.toArray(labels);
    	//IJ.log("getLabels produces this: "+Arrays.toString(labels));
    	//IJ.log("getLabels result contains "+String.valueOf((labels.length)+" elements"));
    	inputIndexPath = null;
    	return labels;
    }
    
    /**
     * Parses the options string used to run the plugin and extracts options for it. Uses defaults for any which aren't
     * specified.
     */
    private void parseOptions() {
        // Get the options string we were started using.
        String optionsStr = debugOptions == null ? Macro.getOptions() : debugOptions;
        if (optionsStr == null) optionsStr = "";

        // Input folder path to process.
        inputFolderPath = Macro.getValue(optionsStr, "input_path", "");
        
        // Path to sample labels.
        inputIndexPath = Macro.getValue(optionsStr, "label_path", "");
        
        //
        nNeighbours = Macro.getValue(optionsStr, "n_nearest", "");
        
        //
        nThreads = Macro.getValue(optionsStr, "n_threads", "");
        
        // See if a CSV output is requested.
        outputCSV = optionsStr.contains("output_csv");
        
	    /** 
	     * Set the metric to use to compute distances in high dimensional space by name.
	     * Valid string metrics include:
	     * euclidean,
	     * manhattan,
	     * chebyshev,
	     * minkowski,
	     * canberra,
	     * braycurtis,
	     * cosine,
	     * correlation,
	     * haversine,
	     * hamming,
	     * jaccard,
	     * dice,
	     * russelrao,
	     * kulsinski,
	     * rogerstanimoto,
	     * sokalmichener,
	     * sokalsneath,
	     * yule.
	     * @param metric metric function specified by name
	     */
        metric = Macro.getValue(optionsStr, "metric", "euclidean");
        
        //
        minDis = Macro.getValue(optionsStr, "min_dis", "0.1");
        
        // Get a random seed from the user or use the default.
        ranSeed = Macro.getValue(optionsStr, "seed", "5");
        
        // Suppress the 'Do you want to run on the image stack' prompt
        suppressStackAsk = optionsStr.contains("no_prompt");

    }
    
    private static void initAndShowGUI() {
        // This method is invoked on Swing thread
        JFrame frame = new JFrame("Dimensionality Reduction Plot");
        final JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel);
        frame.setSize(900, 580);
        frame.setVisible(true);
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//ends the java session (including ImageJ) when the plot is closed.

    	Runnable runnable3 = () -> {
    		initFX(fxPanel);
            };
            FutureTask<Void> task3 = new FutureTask<>(runnable3, null);
            Platform.runLater(task3);
            try {
    			task3.get();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		} catch (ExecutionException e) {
    			e.printStackTrace();
    		}
    }
    
	public static ScatterChart<Number,Number> addSeries(ScatterChart<Number,Number> sc, double[] xDataArray, double[] yDataArray, String seriesName, Color groupColour) {
	    		if (sc.getData() == null) {
	    			sc.setData(FXCollections.<XYChart.Series<Number, Number>>observableArrayList());
	    			IJ.log("scatter is null");
    			}
	    		ScatterChart.Series<Number, Number> series = new ScatterChart.Series<Number, Number>();
	    		series.setName(seriesName);
	    		for (int i=0; i<xDataArray.length; i++) {
	    			series.getData().add(new ScatterChart.Data<Number, Number>(xDataArray[i], yDataArray[i]));
    			}
	    		
	    		//String hex = "#"+Integer.toHexString(ranCol.getRGB()).substring(2);
	    		String hex = "#"+Integer.toHexString(groupColour.getRGB()).substring(2);
	            for (XYChart.Data < Number, Number > data: series.getData()) {
	            	data.setNode(new Circle(2));
	            	Node node = data.getNode();
	            	node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
	            	//node.setScaleX(0.5);
	            	//node.setScaleY(0.5);
	            }
	            
	            //make the nodes mouse selectable. Code from https://stackoverflow.com/questions/44956955/javafx-use-chart-legend-to-toggle-show-hide-series-possible
	            for (Data<Number, Number> data : series.getData()) {
	                Node node = data.getNode() ;
	                node.setCursor(Cursor.HAND);
	                node.setOnMouseClicked(e -> {
	                	// when a point is selected, highlight a corresponding image in the input image stack (or another if wanted and its the same size), if it exists. Note: Perhaps display the image in a new window if a stack is not present.
	                	//NOTE originally getStackSize() was checked against stack.getStackSize(). This worked until the stack was closed. Even with a new image open, this value would be locked to 1.
	                	if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == Xarray.length) {
	                		ImagePlus stack2 = WindowManager.getCurrentImage();
	                		stack2.setSlice(lookupArray[sc.getData().indexOf(series)][series.getData().indexOf(data)]);	//note: add a null condition check for when a stack wasn't the input
	                	}
	                });
	                
	                //scroll zoom method over nodes... mouse scroll wheel zoom over the background is handled elsewhere
	            	node.setOnScroll(new EventHandler<ScrollEvent>() {
	            	    public void handle(ScrollEvent event) {
	            	        event.consume();
	            	        if (event.getDeltaY() == 0) {
	            	            return;
	            	        }
	            	        
	            	        // move chart area-view closer to node position
	            	        double rawX = node.getBoundsInParent().getMinX();
	            	        double rawY = node.getBoundsInParent().getMinY();
	            	        double xPosition = xAxis_Fx.getValueForDisplay(rawX).doubleValue();
	            	        double yPosition = yAxis_Fx.getValueForDisplay(rawY).doubleValue();
	            	        double xDisplace = xPosition-((((xAxis_Fx.getUpperBound()-xAxis_Fx.getLowerBound())/2)+xAxis_Fx.getLowerBound()));
	            	        xDisplace = xDisplace/4;	//fudge factor to make chart area moving less jumpy
	            	        double yDisplace = yPosition-((((yAxis_Fx.getUpperBound()-yAxis_Fx.getLowerBound())/2)+yAxis_Fx.getLowerBound()));
	            	        yDisplace = yDisplace/4;	//fudge factor to make chart area moving less jumpy
	            	        xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound()+xDisplace);
	            	        xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound()+xDisplace);
	            	        yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound()+yDisplace);
	            	        yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound()+yDisplace); 	        
	            	        
	            	        // zoom in 10%
	            	        double SCALE_DELTA_X = (xAxis_Fx.getUpperBound()-xAxis_Fx.getLowerBound())*0.1;
	            	        double SCALE_DELTA_Y = (yAxis_Fx.getUpperBound()-yAxis_Fx.getLowerBound())*0.1;
	            	        xAxis_Fx.setAutoRanging(false); 
	            	        yAxis_Fx.setAutoRanging(false);
	            	        double xScaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA_X : SCALE_DELTA_X * (-1);
	            	        double yScaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA_Y : SCALE_DELTA_Y * (-1);
	            	        //x
	            	        xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound() + xScaleFactor);
	            	        xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound() - xScaleFactor);
	            	        //y
	            	        yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound() + yScaleFactor);
	            	        yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound() - yScaleFactor);
	            	        
	            	    }
	            	});
	                
	                /*
	                //add nodePaths to each node here, so that we can look for overlaps with hand-drawn selection areas later
	                Path nodePath = new Path();
	            	nodePath.setStroke(javafx.scene.paint.Color.TRANSPARENT);
	            	nodePath.setStrokeWidth(1);
	            	nodePath.setFill(javafx.scene.paint.Color.TRANSPARENT);
	                nodePath.getElements().add(new MoveTo(node.getBoundsInParent().getMinX(), node.getBoundsInParent().getMinY()));
	                nodePath.getElements().add(new ClosePath());
	                */
	                
	                /*// Hilarious working code for moving the points around on the chart (from the above stackoverflow post)
	                node.setOnMouseDragged(e -> {
	                    Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
	                    double xAxisLoc = xAxis_Fx.sceneToLocal(pointInScene).getX();
	                    double yAxisLoc = yAxis_Fx.sceneToLocal(pointInScene).getY();
	                    Number x = xAxis_Fx.getValueForDisplay(xAxisLoc);
	                    Number y = yAxis_Fx.getValueForDisplay(yAxisLoc);
	                    data.setXValue(x);
	                    data.setYValue(y);
	                });
	                */
	                
	            }
	            
	    		sc.getData().add(series);
	    		//((Node) sc.lookupAll(".chart-legend-item-symbol").toArray()[sc.lookupAll(".chart-legend-item-symbol").toArray().length]).setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
	    		return sc;
    } 
    
	/*
    private static void setAxis(String xString, String yString) {
    			sc_Fx.getXAxis().setLabel(xString);
    			sc_Fx.getYAxis().setLabel(yString);
	}
    
    private static void setTitle(String title) {
    			sc_Fx.setTitle(title);
	}
	*/
    
    private static void initFX(JFXPanel fxPanel) {
    	//JFXPanel fxPanelRef = fxPanel;
    	xAxis_Fx = new NumberAxis();	//can give the constructor axis ranges instead, if preferred
    	xAxis_Fx.setForceZeroInRange(false);
    	yAxis_Fx = new NumberAxis();
    	yAxis_Fx.setForceZeroInRange(false);
        sc_Fx = new ScatterChart<Number,Number>(xAxis_Fx,yAxis_Fx);
        sc_Fx.setAnimated(false);
        xAxis_Fx.setAnimated(false);
        yAxis_Fx.setAnimated(false);
        xAxis_Fx.setLabel(xTitle);
        yAxis_Fx.setLabel(yTitle);
        //sc_Fx.setTitle("UMAP output (nNeighbours = "+String.valueOf(neighbours)+"; metric = "+metric+")");
        sc_Fx.setTitle(plotTitle);
        cursorCoords = new Label();
        
        ///scene  = new Scene(sc_Fx, 900, 580);
        sc_Fx.setPrefSize(900, 500);
        sc_Fx.setStyle("-fx-border-color: black; -fx-border-insets: 0 4 0 4;"); //border insets top, right, bottom, left
        //scene.getStylesheets().add("stylesheet.css");		//this is the recommended way to set javafx chart/gui visual parameters... but some nodes are not updated properly in my testing
    	Node horizontalGridLines = sc_Fx.lookup(".chart-horizontal-grid-lines"); //see https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html
    		horizontalGridLines.setMouseTransparent(true);
    	Node verticalGridLines = sc_Fx.lookup(".chart-vertical-grid-lines");
	    	verticalGridLines.setMouseTransparent(true);
    		
        scene  = new Scene(new Group());
        final VBox vbox = new VBox();
        final HBox hbox = new HBox();
        final StackPane spane = new StackPane();
        pathPane = new Pane();
	        pathPane.prefWidthProperty().bind(vbox.widthProperty());
	        pathPane.prefHeightProperty().bind(vbox.heightProperty());
	        pathPane.setPickOnBounds(false);
        hbox.setSpacing(10);
        hbox.getChildren().add(cursorCoords);
        spane.getChildren().addAll(sc_Fx);
        vbox.getChildren().addAll(spane, hbox);  
        hbox.setPadding(new Insets(5, 5, 5, 20));
        vbox.setFillWidth(true);
        hbox.setFillHeight(true);
        HBox.setHgrow(sc_Fx, Priority.ALWAYS);
        VBox.setVgrow(hbox, Priority.ALWAYS);
        cursorCoords = coordinateListener();
        
        zoomAndPan();
        multiSelectToolandContextMenu();
        
        sc_Fx.prefWidthProperty().bind(vbox.widthProperty());
        sc_Fx.prefHeightProperty().bind(vbox.heightProperty());
        vbox.prefWidthProperty().bind(scene.widthProperty());
        vbox.prefHeightProperty().bind(scene.heightProperty());
        ((Group)scene.getRoot()).getChildren().add(vbox);
        spane.getChildren().add(pathPane);
        pathPane.getChildren().add(multiPath);
        
        //resize multipath when window is resized
        //multiPath.setScaleX(1);
        //multiPath.setScaleY(1);
        //((Group)scene.getRoot()).getChildren().add(multiPath);
        
        fxPanel.setScene(scene);
    }
    
    private static Label coordinateListener() {
        //show cursor coordinate code from https://gist.github.com/jewelsea/5552705
        Node chartBackground = sc_Fx.lookup(".chart-plot-background");
        chartBackground.setOnMouseEntered(new EventHandler<MouseEvent>() {
        	@Override public void handle(MouseEvent mouseEvent) {
        		cursorCoords.setVisible(true);
            }
        });
        chartBackground.setOnMouseMoved(new EventHandler<MouseEvent>() {
        	@Override public void handle(MouseEvent mouseEvent) {
        		cursorCoords.setText(
        		//IJ.log(
                String.format(
                		"(X=%.2f, Y=%.2f)",
                		xAxis_Fx.getValueForDisplay(mouseEvent.getX()),
                		yAxis_Fx.getValueForDisplay(mouseEvent.getY())
                )
                );
            }
        });
        chartBackground.setOnMouseExited(new EventHandler<MouseEvent>() {
        	@Override public void handle(MouseEvent mouseEvent) {
        		cursorCoords.setVisible(false);
        		}
        });
        return cursorCoords;
    }
    
    private static void zoomAndPan() {
        //adapted from https://stackoverflow.com/questions/22099650/zoom-bar-chart-with-mouse-wheel
    	Node chartBackground = sc_Fx.lookup(".chart-plot-background");
    	chartBackground.setOnScroll(new EventHandler<ScrollEvent>() {
    	    public void handle(ScrollEvent event) {
    	        event.consume();
    	        if (event.getDeltaY() == 0) {
    	            return;
    	        }
    	        
    	        // move chart area-view closer to cursor position
    	        double rawX = event.getX();
    	        double rawY = event.getY();
    	        double xPosition = xAxis_Fx.getValueForDisplay(rawX).doubleValue();
    	        double yPosition = yAxis_Fx.getValueForDisplay(rawY).doubleValue();
    	        double xDisplace = xPosition-((((xAxis_Fx.getUpperBound()-xAxis_Fx.getLowerBound())/2)+xAxis_Fx.getLowerBound()));
    	        xDisplace = xDisplace/4;	//fudge factor to make chart area moving less jumpy
    	        double yDisplace = yPosition-((((yAxis_Fx.getUpperBound()-yAxis_Fx.getLowerBound())/2)+yAxis_Fx.getLowerBound()));
    	        yDisplace = yDisplace/4;	//fudge factor to make chart area moving less jumpy
    	        xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound()+xDisplace);
    	        xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound()+xDisplace);
    	        yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound()+yDisplace);
    	        yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound()+yDisplace); 	        
    	        
    	        // zoom in 10%
    	        double SCALE_DELTA_X = (xAxis_Fx.getUpperBound()-xAxis_Fx.getLowerBound())*0.1;
    	        double SCALE_DELTA_Y = (yAxis_Fx.getUpperBound()-yAxis_Fx.getLowerBound())*0.1;
    	        xAxis_Fx.setAutoRanging(false); 
    	        yAxis_Fx.setAutoRanging(false);
    	        double xScaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA_X : SCALE_DELTA_X * (-1);
    	        double yScaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA_Y : SCALE_DELTA_Y * (-1);
    	        //x
    	        xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound() + xScaleFactor);
    	        xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound() - xScaleFactor);
    	        //y
    	        yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound() + yScaleFactor);
    	        yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound() - yScaleFactor);
    	        
    	    }
    	});
    	
    	// Move the view with the mouse...
    	
    	// From https://stackoverflow.com/questions/46890927/javafx-draggable-background-alternative-to-using-a-camera
        //scene.setOnScroll(e -> {
            //sc_Fx.setTranslateX(sc_Fx.getTranslateX() + e.getDeltaX());
            //sc_Fx.setTranslateY(sc_Fx.getTranslateY() + e.getDeltaY());
        //});
    	
    	/* My attempt... does not work well.
    	chartBackground.setOnMouseDragged(new EventHandler<MouseEvent>() {
    		public void handle(MouseEvent event) {
    	    	if (event.isPrimaryButtonDown()) {
    	    		
    	        	xAxis_Fx.setAutoRanging(false);
    	        	yAxis_Fx.setAutoRanging(false);
    	        	
    	        	/*
    	        	int firstFlag = 0;
    	        	double xLow;
    	        	double xHigh;
    	        	double yLow;
    	        	double yHigh;
    	        	
    	        	if (firstFlag == 0) {
	        	        xLow = xAxis_Fx.getLowerBound();
	        	        xHigh = xAxis_Fx.getUpperBound();
	        	        yLow = yAxis_Fx.getLowerBound();
	        	        yHigh = yAxis_Fx.getUpperBound();
	        	        firstFlag = 1;
    	        	}
    	        	
    	    		//rawXX = event.getX();
    	        	
        			//IJ.log("Middle button is down.");
        			//chartBackground.setCursor(Cursor.CLOSED_HAND);
    	    		
    	    		double rawXX = event.getX();
    	    		double rawYY = event.getY();
    		        double xxPosition = xAxis_Fx.getValueForDisplay(rawXX).doubleValue();
    		        double originalX = xPosition[0];
    		        //double newX;
    		        double yyPosition = yAxis_Fx.getValueForDisplay(rawYY).doubleValue();
    		        double originalY = yPosition[0];
    		        //double newY;
        	        //double xDisplace = xxPosition-((((xAxis_Fx.getUpperBound()-xAxis_Fx.getLowerBound())/2)+xAxis_Fx.getLowerBound()));
    		        double xDisplace = originalX-xxPosition;
        	        //double xLDisplace = xxPosition;
        	        //double xUDisplace = xxPosition;
        	        //xDisplace = xDisplace/4;	//fudge factor to make chart area moving less jumpy
        	        //double yDisplace = yyPosition-((((yAxis_Fx.getUpperBound()-yAxis_Fx.getLowerBound())/2)+yAxis_Fx.getLowerBound()));
        	        double yDisplace = originalY-yyPosition;
        	        //double yLDisplace = xxPosition;
        	        //double yUDisplace = xxPosition;
        	        //yDisplace = yDisplace/4;	//fudge factor to make chart area moving less jumpy
    		        
        	        //xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound()+(xxPosition - xPosition[0]));
        	        //xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound()+(xxPosition - xPosition[0]));
        	        //yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound()+(yyPosition - yPosition[0]));
        	        //yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound()+(yyPosition - yPosition[0]));
        	        xAxis_Fx.setLowerBound(xAxis_Fx.getLowerBound()+(xDisplace));
        	        xAxis_Fx.setUpperBound(xAxis_Fx.getUpperBound()+(xDisplace));
        	        	rawXX = event.getX();
        	        	xxPosition = xAxis_Fx.getValueForDisplay(rawXX).doubleValue();
        	        	originalX = xPosition[0];
        	        yAxis_Fx.setLowerBound(yAxis_Fx.getLowerBound()+(yDisplace));
        	        yAxis_Fx.setUpperBound(yAxis_Fx.getUpperBound()+(yDisplace));
	    	        	rawYY = event.getY();
	    	        	yyPosition = yAxis_Fx.getValueForDisplay(rawYY).doubleValue();
        	        	originalY = xPosition[0];
    	    		//rawXX = event.getX();
    	    		//rawYY = event.getY();
    		        //xxPosition = xAxis_Fx.getValueForDisplay(rawXX).doubleValue();
    		        //yyPosition = yAxis_Fx.getValueForDisplay(rawYY).doubleValue();
        		}
    	    }
    	});
    	*/
    	
    }
    
    private static void multiSelectToolandContextMenu() {
    	Node chartBackground = sc_Fx.lookup(".chart-plot-background");
    	SelectionModel selectionModel = new SelectionModel();
    	
    	multiPath = new Path();
    	multiPath.setStroke(javafx.scene.paint.Color.BLUE);
    	multiPath.setStrokeWidth(1);
    	multiPath.setStrokeLineCap(StrokeLineCap.ROUND);
    	multiPath.setFill(javafx.scene.paint.Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));
    	/*
    	multiPath.setOnMousePressed(new EventHandler<MouseEvent>() {
    	    public void handle(MouseEvent event) {
    	    	if (event.getButton() == MouseButton.SECONDARY) {
    	    		//IJ.log("Shape successfully selected.");
    	    		//Opportunity to add some context functions here... like an option to add a group label to the selected points. Or maybe just a way to cancel the area.
    	    	}
    	    	event.consume();
    	    }
    	});
    	*/
    	
        multiEnabled = false;
        //Could go here.. optional inclusion of array wrapper for mouseStartX and Y if I do not want to initialise them as global variables
        
        chartBackground.setOnMousePressed(e -> {
        	if (e.getClickCount() == 1 && e.getButton() == MouseButton.PRIMARY) {
	        	if(multiEnabled) {
	        		return;
	        	}
	        	mouseStartX = e.getSceneX();
	        	mouseStartY = e.getSceneY();
	        	
	        	//Chart background to window(scene) coordinate offset... allows us to compare node positions with the multiPath area later
	        	xPathOffset = e.getSceneX()-e.getX();
	        	yPathOffset = e.getSceneY()-e.getY();
	        	//IJ.log("xOffset = "+Double.toString(xPathOffset)+", yOffset = "+Double.toString(yPathOffset));
	
	    		multiPath.getElements().clear();
	    		multiPath.getElements().add(new MoveTo(e.getSceneX(), e.getSceneY()));
	    		
	            e.consume();
	
	            multiEnabled = true;
        	}
        	if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
	        	xAxis_Fx.setAutoRanging(true);
	        	yAxis_Fx.setAutoRanging(true);
	        	e.consume();
        	}
        	if (e.getButton() == MouseButton.SECONDARY) {
	        	final ContextMenu contextMenu = new ContextMenu();
	        	MenuItem copy = new MenuItem("Copy to clipboard");
	        	MenuItem save = new MenuItem("Save interactive plot");
	        	contextMenu.getItems().addAll(copy, save);
	        	contextMenu.show(sc_Fx, e.getScreenX(), e.getScreenY());
	        	copy.setOnAction(new EventHandler<ActionEvent>() {
	        		 public void handle(ActionEvent event) {
		    	        	WritableImage image = scene.snapshot(null);
		    	        	//WritableImage image = chart.snapshot(new SnapshotParameters(), null);
		    	            ClipboardContent cc = new ClipboardContent();
		    	            cc.putImage(image);
		    	            Clipboard.getSystemClipboard().setContent(cc);
	        		}
	        	});
	        	save.setOnAction(new EventHandler<ActionEvent>() {
	        		public void handle(ActionEvent event) {
	    	        	SaveFxPlot savePlot = new SaveFxPlot();
	    	        	savePlot.run(null);
	        		}
	        	});
	        	e.consume();
	        }
        });
        
        chartBackground.setOnMouseDragged(e -> {
        	if (e.getButton() == MouseButton.PRIMARY) {
        		if (!multiEnabled) {
        			return;
        		}
	        	multiPath.getElements().add(new LineTo(e.getSceneX(), e.getSceneY()));
	        	
	        	e.consume();
        	}
        });
        
        chartBackground.setOnMouseReleased(e -> {
        	if (e.getButton() == MouseButton.PRIMARY) {
        		if (!multiEnabled) {
        			return;
        		}
	        	int areaNodes = 0;
	        	multiPath.getElements().add(new LineTo(mouseStartX, mouseStartY)); //see if moving the line to the path origin makes the next closePath call less jumpy.
	        	multiPath.getElements().add(new ClosePath());
	        	
	        	//if the fist multiPath point is the same as the last, then no path was drawn and the path will be deleted
	        	if (mouseStartX == e.getSceneX() && mouseStartY == e.getSceneY()) {
	        		multiPath.getElements().clear();
	        		e.consume();
	        		multiEnabled = false;
	        		return;
	        	}
	        	
	            if(!e.isControlDown() || !e.isShiftDown()) {
	            	selectionModel.clear();
	            }
	            
	            //Iterate over all sc_Fx nodes, series-by-series. Perhaps there is a better way of achieving this.
	            for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
		            	for (Data<Number, Number> data : series.getData()) {
			                Node node = data.getNode();
	
		                	if (multiPath.contains(node.getBoundsInParent().getMinX()+xPathOffset, node.getBoundsInParent().getMinY()+yPathOffset) && node.isVisible()) {
			                	areaNodes++;
			                	//IJ.log("Overlap found.");
			                	//IJ.log("Overlap coordinates = "+Double.toString(node.getBoundsInParent().getMinX())+", "+Double.toString(node.getBoundsInParent().getMinY()));
			                	//ImagePlus stack3 = WindowManager.getCurrentImage();
			                	//stack3.setSlice(lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)]);
			                	//If (){
			                		//
			                	//}
			                }
			                //int positionInStack = lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)];
		            	}
	            }
	            
	            if (areaNodes == 1) {
	            	IJ.log("1 point in the area.");
	            } else if (areaNodes > 1) {
	            	IJ.log(areaNodes+" points in the area.");
	            }
	            
	            //multiPath.getElements().clear();
	            
	        	e.consume();
	        	
	        	multiEnabled = false;
        	}
        });
        
        //right clicking on the multipath area creates a dropdown menu, allowing an image stack or results table of the selected data to be created
        multiPath.setOnMousePressed(e2 -> {
        	if (e2.getClickCount() == 1 && e2.getButton() == MouseButton.SECONDARY) {
	        	final ContextMenu exportMenu = new ContextMenu();
	        	MenuItem toStack = new MenuItem("Data to stack");
	        	MenuItem toTable = new MenuItem("Data to table");

	        	if (WindowManager.getCurrentImage() == null || !(WindowManager.getCurrentImage()).isStack() || WindowManager.getCurrentImage().getStackSize() != Xarray.length) {
        		//if (!Stack.exists()) {
        			toStack.setDisable(true);
        			toStack.setText("Data to stack (Open and select a stack to enable)");
        			toTable.setDisable(true);
        			toTable.setText("Data to table (Open and select a stack to enable)");
        			toStack.setStyle("-fx-stroke-color: rgba(100, 100, 100, 1)");	//disabling the menuitem overrides the colour, so this is not implemented
        			toTable.setStyle("-fx-stroke-color: rgba(100, 100, 100, 1)");	//disabling the menuitem overrides the colour, so this is not implemented
        		} else if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == (Xarray.length)) {
        		//} else if (Stack.exists()) {
        			toStack.setDisable(false);
        			toStack.setText("Data to stack");
        			toTable.setDisable(false);
        			toTable.setText("Data to table");
        			toStack.setStyle("-fx-stroke-color: rgba(0, 0, 0, 1)");
        			toTable.setStyle("-fx-stroke-color: rgba(0, 0, 0, 1)");
        		}
	        	exportMenu.getItems().clear();
	        	exportMenu.getItems().addAll(toStack, toTable);
	        	exportMenu.show(sc_Fx, e2.getScreenX(), e2.getScreenY());
	        	toStack.setOnAction(new EventHandler<ActionEvent>() {
	        		 public void handle(ActionEvent event) {
	        			 //ImagePlus stack3 = WindowManager.getCurrentImage();
	        			 String titles[] = WindowManager.getImageTitles();
	        			 if (!Arrays.stream(titles).anyMatch("Sub-stack"::equals) && WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == Xarray.length) {
	        				 //int type = WindowManager.getCurrentImage().getType();
	        				 //ImagePlus subStack = new ImagePlus();
	        				 IJ.log("toStack was pressed.");
	        			 }
	        			 //stack3.setSlice(lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)]);
	        		}
	        	});
	        	toTable.setOnAction(new EventHandler<ActionEvent>() {
	        		public void handle(ActionEvent event) {
	        			IJ.log("toTable was pressed.");
	        		}
	        	});
	        	e2.consume();
        	}
        	
        	// recount the enclosed points after left clicking on the multiPath selection area 
        	if (e2.getClickCount() == 1 && e2.getButton() == MouseButton.PRIMARY) {
        		int areaNodes = 0;
        		for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
	            	for (Data<Number, Number> data : series.getData()) {
		                Node node = data.getNode();

	                	if (multiPath.contains(node.getBoundsInParent().getMinX()+xPathOffset, node.getBoundsInParent().getMinY()+yPathOffset) && node.isVisible()) {
		                	areaNodes++;
		                }
	            	}
            }
            
            if (areaNodes == 1) {
            	IJ.log("1 point in the area.");
            } else if (areaNodes > 1) {
            	IJ.log(areaNodes+" points in the area.");
            }
        	}
        });
        
    }
    
    private static class SelectionModel {

        Set<Node> selection = new HashSet<>();

        @SuppressWarnings("unused")
		public void add( Node node) {

            if( !node.getStyleClass().contains("highlight")) {
                node.getStyleClass().add( "highlight");
            }

            selection.add( node);
        }

        public void remove( Node node) {
            node.getStyleClass().remove( "highlight");
            selection.remove( node);
        }

		public void clear() {

            while( !selection.isEmpty()) {
                remove( selection.iterator().next());
            }

        }

        @SuppressWarnings("unused")
		public boolean contains( Node node) {
            return selection.contains(node);
        }

        @SuppressWarnings("unused")
		public int size() {
            return selection.size();
        }

        @SuppressWarnings("unused")
		public void log() {
            System.out.println( "Items in model: " + Arrays.asList( selection.toArray()));
        }

    }
    
    
}