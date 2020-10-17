package dimred;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import ij.*;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import com.jujutsu.tsne.PrincipalComponentAnalysis;
import com.jujutsu.utils.MatrixUtils;

/**version 1.0.0*/

public class Pca_ implements PlugIn {
	//top level initialisation
	PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis();
	private boolean processingStack = false;
	double[][] imageMatrix;
	String[] Filelist;
	String[] labelsArray;
	int bitD;
	
	 // Options to use during the run. Defaults for some but otherwise populated when parseOptions() is called.
    private String inputFolderPath;
    private int pca_comp = 2; //was 2
    private String inputIndexPath; //show the plugin where sample labels are located.. used to colour the final output.
    private int eigen_out = -1; //eigenvector output from those computed
	
    // Variables and main() method for testing in IDEs.
    private static String debugOptions = null;
    private static double[][] debugArray = null;
    private static double[][] debugArrayx = null;
	public static void main(String[] args) {
        debugArrayx = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        	//swap rows and columns of the debugArray (go from 784x2500 to 2500x784)
        	double[][] temp = new double[debugArrayx[0].length][debugArrayx.length];
        	for (int i=0; i<debugArrayx.length; i++) {
        		for (int j=0; j<debugArrayx[0].length; j++) {
        			temp[j][i] = debugArrayx[i][j];
        		}
        	}
        debugArray = temp;
        debugOptions = "label_path=[C:/Users/Anthony/Desktop/mnist_labels.csv]";
        new Pca_().run("");
        //debugOptions = "label_path=[C:/Users/Anthony/Desktop/NumbersIndex.csv]";
        //new PCA_().run("");
	} //NOTE, pca is currently outputing vectors with 784 datapoints..
	
	public void run(String arg) {
		parseOptions();
		processingStack = false;
		 
		 if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack()) {
			 int type = WindowManager.getCurrentImage().getType();
			 int choice = JOptionPane.showConfirmDialog(null, "Do you want to process the image stack?", "PCA option",JOptionPane.YES_NO_CANCEL_OPTION);
	         if (choice == JOptionPane.CANCEL_OPTION) {return;}
	         else if (choice == JOptionPane.OK_OPTION) {
	            	processingStack = true;
	            	ImagePlus stack = WindowManager.getCurrentImage();
	            		//Get bitDepth
	            		bitD = stack.getBitDepth();
	            	Filelist = new String[stack.getStackSize()];
	            	int height = stack.getHeight();
	            	int width = stack.getWidth();
	            	imageMatrix = new double[stack.getStackSize()][width*height]; //2D array to hold all stack image data [slice number][1D array of all pixels in the slice image]
	            	for (int s = 0; s < stack.getStackSize(); s++) {
	            		Filelist[s] = Integer.toString(s+1);
	            		stack.setSlice(s+1);
	            		ImageProcessor sip = stack.getProcessor();  	
	                	double[] image1DArray = new double[width*height];	//try double, then float
	                	for (int j = 0; j < height; j++) {
	                		for (int k = 0; k < width; k++) {
	                			if (type == ImagePlus.COLOR_RGB) {
	                				image1DArray[k+(j*width)] = sip.get(k, j);
	                			} else {
	                				image1DArray[k+(j*width)] = sip.getInterpolatedValue(k,j);
	                			}
	                		}
	                	}
	                	for(int l = 0; l < (width*height); l++) {
	                		imageMatrix[s][l] = image1DArray[l];
	                	}
	            	}
	            	double[][] temp2 = new double[imageMatrix[0].length][imageMatrix.length];
	            	//double[][] temp2 = new double[imageMatrix.length][imageMatrix[0].length];
	            	for (int i=0; i<imageMatrix.length; i++) {
	            		for (int j=0; j<imageMatrix[0].length; j++) {
	            			//temp2[j][i] = imageMatrix[i][j];			//I think I am transposing the elements of the 2D image-matrix array here
	            			temp2[j][i] = imageMatrix[i][j];	
	            		}
	            	}
	            	pca.setup((width*height), stack.getStackSize());
	            	//pca.setup(stack.getStackSize(), (width*height));	//this is the normal way to feed images to PCA?
	            	for (int p = 0; p < (width*height); p++) {
	            	//for (int p = 0; p < stack.getStackSize(); p++) {
	            		//pca.addSample(temp2[p]);
	            		pca.addSample(temp2[p]);
	            	}
	            	//stack.close();
	            	stack.setSlice(0);
	            	stack.updateAndDraw();
	            } else {
	            	DirectoryChooser dc = new DirectoryChooser("Select a folder of images to process");
	                inputFolderPath = dc.getDirectory();
		                if (inputFolderPath == null) {
		                	return;
		                }
	            }
	        }
	        //specify a folder to perform tSNE on
	        else if (inputFolderPath.isEmpty() && debugArray == null) {
	        DirectoryChooser dc = new DirectoryChooser("Select a folder");
	        inputFolderPath = dc.getDirectory();
		        if (inputFolderPath == null) {
	            	return;
	            }
	    	}
		 
	    	if (debugArray != null) {
	    		// Build pca with data from debug matrix
	    		Filelist = new String[debugArray[0].length];		//was debugArray.length
	    		pca.setup(debugArray.length, debugArray[0].length);
	    		for (int d = 0; d < debugArray.length; d++) {
	    			pca.addSample(debugArray[d]);
	    		}
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
	    	    	
	    	    	pca.setup(Filelist.length, width*height);
	    			
	    			/* Optional 2D array of pixel data from all samples... setup
	    	    	// imageMatrix = new double[Filelist.length][width*height];
	    			*/
	    	    	
	    	    	IJ.log("Number of images = "+imageMatrix.length);			//300
	    			IJ.log("Dimensions (pixels) = "+imageMatrix[0].length);	//900

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
	    	        	
	    	        	double[] image1DArray = new double[width*height];
	    	        	
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
	    	        	pca.addSample(image1DArray);
	    	        	
	    	        	/* Optional population of the 2D pixel array
	    	        	for(int l = 0; l < (width*height); l++) {
	    	        		imageMatrix[i][l] = image1DArray[l];
	    	        	}
	    	        	*/
	    	        }
	    	    }
	    	 
	    	 //pca computation here
	    	 pca.computeBasis(pca_comp);
	    	 
	         //Get and use labels if a .csv file is specified.
	         if (!inputIndexPath.isEmpty()) {
	         	labelsArray = null;
	         	try {
	 				labelsArray = getLabels(inputIndexPath);
	 			} catch (IOException e) {
	 				IJ.handleException(e);
	 			}
	         	if (labelsArray == null) {
	         		IJ.log("The specified labels file is empty or corrupt.");
	         	}
	         	if (labelsArray.length != Filelist.length) {
	         		IJ.log("The number of labels in the labels file does not match the number of processed images, so it has not been used to assign groupings.");
	         		IJ.log("Y.length = "+String.valueOf(Filelist.length)+"; Label number = "+String.valueOf(labelsArray.length));
	         		IJ.log(Arrays.toString(labelsArray));
	         		labelsArray = null;
	         	}
	         }
	    	 
	    	 // get and plot first 2 principal components
	    	 if (pca_comp >= 0) {
	    		double[] Pcomp1 = pca.getBasisVector(0);
	    		double[] Pcomp2;
	    		if (pca_comp < 2) {
	    		Pcomp2 = pca.getBasisVector(0);
	    		} else {
	    			Pcomp2 = pca.getBasisVector(1);
	    		}
	    		if (eigen_out != -1) {
	    			//double[] PcompX = pca.getBasisVector(eigen_out);
	    			//double[] eigenX = pca.getEigVector(eigen_out);
	    			double[] eigenX = pca.getBasisVector(eigen_out); //This doesn't look right, but I am not using this function currently. Can come back to later.
	    			
	    				IJ.log("eigenX length = "+Integer.toString(eigenX.length));
	    				IJ.log("eigenX[0] = "+Double.toString(eigenX[0]));
	    				IJ.log("eigenX[1] = "+Double.toString(eigenX[1]));
	    				//double[] eigensample1 = pca.sampleToEigenSpace(Pcomp1);
	    				//IJ.log("eigensample1 length = "+Integer.toString(eigensample1.length));
	    				///double[] eigen1 = pca.eigenToSampleSpace(Pcomp1);
	    				///IJ.log("Pcomp1 length = "+Integer.toString(eigen1.length));
	    					//double[] stesX =  pca.sampleToEigenSpace(PcompX);
	    				//IJ.log("stesX length = "+Integer.toString(stesX.length));
	    				
	    					//double[] eigenX = pca.eigenToSampleSpace(stesX);
	    				//IJ.log("eigenX length = "+Integer.toString(eigenX.length));
	    				
	    				//display Pcomp1 reconstructed as an image of original sample dimensions
	    				ImagePlus outputPlus = NewImage.createImage("PCA vectors", eigenX.length, 1, 1, bitD, NewImage.FILL_BLACK);
	    				ImageProcessor outputProc = outputPlus.getProcessor();
	    				/*
	    			IJ.log("Pcomp1 value in position 0 = "+Double.toString(eigenX[0]));
	    				IJ.log("Position 0 cast to int = "+Integer.toString((int)eigenX[0]));
	    			IJ.log("Pcomp1 value in position 1 = "+Double.toString(eigenX[1]));
	    				IJ.log("Position 1 cast to int = "+Integer.toString((int)eigenX[1]));
	    			IJ.log("Pcomp1 value in position 2 = "+Double.toString(eigenX[2]));
	    				IJ.log("Position 2 cast to int = "+Integer.toString((int)eigenX[2]));
	    				 */
	    				for (int i = 0; i < eigenX.length; i++) {
	    					outputProc.set(i, 0, (int)Math.round(eigenX[i]));
	    				}
	    				outputPlus.show();
	    		}
	    		
	        	Plot scatter = new Plot("PCA output", "principal component 1", "principal component 2");
	        	if (labelsArray == null) {
	        		scatter.setLineWidth(5);
	        		scatter.addPoints(Pcomp1, Pcomp2, 6);
	        		scatter.show();
	        	} else {
	        		//some method to count unique entries (groups) in the labels array here. I expect processing overhead time-loss associated with this 'else' code.
	        		ArrayList<String> uniqueArrayList = new ArrayList<>();
	        	    for(int i=0; i<labelsArray.length; i++){
	        	        if(!uniqueArrayList.contains(labelsArray[i])){
	        	            uniqueArrayList.add(labelsArray[i]);
	        	        }
	        	    }
	        		int groupN = uniqueArrayList.size();
	        		String[] uniqueArray = new String[groupN];
	        		uniqueArrayList.toArray(uniqueArray);
	        		
	        		for (int y = 0; y < groupN; y++) {
	        			int counter = 0;
	        			for (int z = 0; z < labelsArray.length; z++) {
	        				if (labelsArray[z].equals(uniqueArray[y])) {
	        					counter++;
	        				}
	        			}
	        				//IJ.log(uniqueArray[y]+" is present "+String.valueOf(counter)+" time(s) in the labelsArray.");
	        				//IJ.log("debugArray.length = "+Integer.toString(debugArray.length)+", debugArray[0].length = "+Integer.toString(debugArray[0].length));
	        			double[] XXarray = new double[counter];
	        			double[] YYarray = new double[counter];
	        			counter = 0;
	        			for (int z = 0; z < labelsArray.length; z++) {
	        				if (labelsArray[z].equals(uniqueArray[y])) {
	        						//IJ.log("unique array contents = "+uniqueArray[y]+", XXarray length = "+Integer.toString(XXarray.length)+", YYarray length = "+Integer.toString(YYarray.length)+", y = "+Integer.toString(y)+", Pcomp1 length = "+Integer.toString(Pcomp1.length)+", Pcomp2 length = "+Integer.toString(Pcomp2.length)+"");
	        					XXarray[counter] = Pcomp1[z];
	        					YYarray[counter] = Pcomp2[z];
	        					counter++;
	        				}
	        			}
	            		//Randomly generate an rgb colour and add the datapoint of that colour to the plot.
	        			Random rand = new Random();
	        			int r = rand.nextInt(230);
	        			int g = rand.nextInt(230);
	        			int b = rand.nextInt(230);
	        			Color ranCol = new Color(r,g,b);
	        			/* some explict colour assignment for dirty debugging.
	        			if (y == 0) {
	        				scatter.setColor(Color.BLACK);
	        			/} else {
	        				scatter.setColor(Color.RED);
	        			}
	        			*/
	        			scatter.setColor(ranCol);
	        			scatter.setLineWidth(5);
	        			scatter.addPoints(XXarray, YYarray, 6);
	        			//scatter.addLegend(uniqueArray[y]);
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
	        		scatter.addLegend(legendLabels);
	        			//scatter.addLegend((uniqueArray.toString())); //gave odd results, depsite simplicity
	        		scatter.show();
	        		scatter.setLimitsToFit(true);
	        		//IJ.log("File list: "+Arrays.toString(Filelist));
	        	}
	    		
	    	 }
	    	 
	    	 
	    	 
	}
	
    public String[] getLabels( String inputIndexPath) throws IOException {
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
    	return labels;
    }

    private void parseOptions() {
        // Get the options string we were started using.
        String optionsStr = debugOptions == null ? Macro.getOptions() : debugOptions;
        if (optionsStr == null) optionsStr = "";

        // Input folder path to process.
        inputFolderPath = Macro.getValue(optionsStr, "input_path", "");
        
        // Path to sample labels.
        inputIndexPath = Macro.getValue(optionsStr, "label_path", "");

        // Initial dimensions to specify PCA pre-processing dimensionality-reduction.
        pca_comp = Integer.parseInt(Macro.getValue(optionsStr, "pca_comp", "2"));
        
        // Output eigenvector
        eigen_out = Integer.parseInt(Macro.getValue(optionsStr, "eigen_out", "-1"));

        // Get the perplexity value or use the default.
        // perplexity = Double.parseDouble(Macro.getValue(optionsStr, "perplexity", ""));
        
        // Get the maximum iterations for tSNE error approximation of use the default.
        //max_iterations = Integer.parseInt(Macro.getValue(optionsStr, "max_iterations ", "1000"));
        
        // See if a CSV output is requested.
        //outputCSV = optionsStr.contains("output_csv");

    }
	
}