package plot.plot;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ReadFxPlot implements PlugIn {
	//Top level initialisation
	String[] labelsArray;					//pulled from file
	private String[] uniqueArray;			//pulled from file
	static double[] Xarray;					//pulled from file
    static double[] Yarray;					//pulled from file
    static String groupLabel;
	static double[] XXarray;
	static double[] YYarray;
	private static Color[] groupColours;	//pulled from file
	private static int[][] lookupArray;		//pulled from file
	private static String xTitle;			//pulled from file
	private static String yTitle;			//pulled from file
	private static String plotTitle;		//pulled from file
	private static int stackSize;			//pulled from file
	//Fx top level initialisation
	//private static JFXPanel fxPanelRef;
	private static NumberAxis xAxis_Fx;
	private static NumberAxis yAxis_Fx;
	private static ScatterChart<Number,Number> sc_Fx;
	private static Scene scene;
	private static Label cursorCoords = null;
	
	@SuppressWarnings("unchecked")
	public void run(String arg) {
		
        JFileChooser fileChooser = new JFileChooser("Load plot");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PLOT files", "plot");
        fileChooser.setFileFilter(filter);
        
        int returnVal = fileChooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
        	File file = fileChooser.getSelectedFile();	
	        if (file.getName().endsWith(".plot")) {
				try {
					FileInputStream fi = new FileInputStream(file);
					ObjectInputStream oi = new ObjectInputStream(fi);
					
					ArrayList<Object> fxObjects = new ArrayList<Object>(10);
					fxObjects = (ArrayList<Object>) oi.readObject();
					labelsArray = (String[]) fxObjects.get(0);
					uniqueArray = (String[]) fxObjects.get(1);
					Xarray = (double[]) fxObjects.get(2);
					Yarray = (double[]) fxObjects.get(3);
					groupColours = (Color[]) fxObjects.get(4);
					lookupArray = (int[][]) fxObjects.get(5);
					xTitle = (String) fxObjects.get(6);
					yTitle = (String) fxObjects.get(7);
					plotTitle = (String) fxObjects.get(8);
					stackSize = (int) fxObjects.get(9);
					
					oi.close();
					fi.close();
					
				} catch (FileNotFoundException e) {
					IJ.log("File not found");
					IJ.handleException(e);
				} catch (ClassNotFoundException e) {
					IJ.handleException(e);
				} catch (IOException e) {
					IJ.log("Error initializing stream");
					IJ.handleException(e);
				}
				
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
		        
	    		for (int y = 0; y < uniqueArray.length; y++) {
	    			
	    			int counter = 0;
	    			for (int z = 0; z < labelsArray.length; z++) {	
	    				if (labelsArray[z].equals(uniqueArray[y])) {
	    					counter++;
	    				}
	    			}
	    			//ookupArray[y] = new int[counter];
	    				//IJ.log(uniqueArray[y]+" is present "+String.valueOf(counter)+" time(s) in the labelsArray.");
	    			
	    			XXarray = new double[counter];
	    			YYarray = new double[counter];
	    			counter = 0;
	    			for (int z = 0; z < labelsArray.length; z++) {
	    				if (labelsArray[z].equals(uniqueArray[y])) {
	    					XXarray[counter] = Xarray[z];
	    					YYarray[counter] = Yarray[z];
	    					counter++;
	    				}
	    			}
	    			
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
	    	    	}
	        	});
	    		
	        } else {
	        	IJ.log("The selected file is not a .plot file or is corrupt.");        	
	        }
        } else {
        	return;
        }
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
	                	if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == stackSize) {
	                		ImagePlus stack2 = WindowManager.getCurrentImage();
	                		stack2.setSlice(lookupArray[sc.getData().indexOf(series)][series.getData().indexOf(data)]);	//note: add a null condition check for when a stack wasn't the input
	                	}
	                });
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
        sc_Fx.setTitle(plotTitle);
        cursorCoords = new Label();
        
        ///scene  = new Scene(sc_Fx, 900, 580);
        sc_Fx.setPrefSize(900, 500);
        sc_Fx.setStyle("-fx-border-color: black; -fx-border-insets: 0 4 0 4;"); //border insets top, right, bottom, left
        //scene.getStylesheets().add("stylesheet.css");		//this is the recommended way to set javafx chart/gui visual parameters... but some nodes are not updated properly in my testing
        
        scene  = new Scene(new Group());
        final VBox vbox = new VBox();
        final HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.getChildren().add(cursorCoords);
        vbox.getChildren().addAll(sc_Fx, hbox);  
        hbox.setPadding(new Insets(5, 5, 5, 20));
        vbox.setFillWidth(true);
        hbox.setFillHeight(true);
        HBox.setHgrow(sc_Fx, Priority.ALWAYS);
        VBox.setVgrow(hbox, Priority.ALWAYS);
        cursorCoords = coordinateListener();
        
        zoomAndPan();
        
        sc_Fx.prefWidthProperty().bind(vbox.widthProperty());
        sc_Fx.prefHeightProperty().bind(vbox.heightProperty());
        vbox.prefWidthProperty().bind(scene.widthProperty());
        vbox.prefHeightProperty().bind(scene.heightProperty());
        ((Group)scene.getRoot()).getChildren().add(vbox);
        
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
    	sc_Fx.setOnScroll(new EventHandler<ScrollEvent>() {
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
    	
    	//double click to reset chart area (to auto-ranging)
    	sc_Fx.setOnMousePressed(new EventHandler<MouseEvent>() {
    	    public void handle(MouseEvent event) {
    	        if (event.getClickCount() == 2) {
    	        	xAxis_Fx.setAutoRanging(true);
    	        	yAxis_Fx.setAutoRanging(true);
    	        }
    	        if (event.getButton() == MouseButton.SECONDARY) {
    	        	final ContextMenu contextMenu = new ContextMenu();
    	        	MenuItem copy = new MenuItem("Copy to clipboard");
    	        	contextMenu.getItems().addAll(copy);
    	        	contextMenu.show(sc_Fx, event.getScreenX(), event.getScreenY());
    	        	copy.setOnAction(new EventHandler<ActionEvent>() {
    	        		 public void handle(ActionEvent event) {
		    	        	WritableImage image = scene.snapshot(null);
		    	        	//WritableImage image = chart.snapshot(new SnapshotParameters(), null);
		    	            ClipboardContent cc = new ClipboardContent();
		    	            cc.putImage(image);
		    	            Clipboard.getSystemClipboard().setContent(cc);
    	        		}
    	        	});
    	        }
    	    }
    	});
    	
    }
	
}