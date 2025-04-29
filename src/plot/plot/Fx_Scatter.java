package plot.plot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jujutsu.tsne.PrincipalComponentAnalysis;

import dimred.Pca_;
import dimred.Tsne_;
import dimred.Umap_;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.process.LUT;
import ij.text.TextWindow;
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
import javafx.scene.control.TextInputDialog;
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

/** Generic FxPlot generator class.
 *
 */
public class Fx_Scatter {
	//Top level initialisation
	public static ArrayList<String> uniqueArrayList;	//An arraylist of group/series labels
    public static ArrayList<Color> groupColoursList;	//An arraylist of colours ascribed to each data-point group/series
	public static int[][] lookupArray;					//a 2D array defining the original stack or table position of datapoints organised as [series index][data index]
	public static String xTitle;						//x-axis title
	public static String yTitle;						//y-axis title
	public static String plotTitle;						//plot title
	public static int stackSize;						//The size of the linked stack of images or data-table
	
	static ImagePlus stack;
	//private static int neighbours; //used by Umap setTitle
	//private static String metric = Umap_.metric;
	//private static String metric; //used by Umap setTitle
	//Fx top level initialisation
	private static Scene scene;
	private static NumberAxis xAxis_Fx;
	private static NumberAxis yAxis_Fx;
	public static ScatterChart<Number,Number> sc_Fx;
	private static Label cursorCoords = null;
	private static boolean multiEnabled = false;
	private static Path multiPath = new Path();
	private static Pane pathPane = new Pane();
	private static double xPathOffset = 0;
	private static double yPathOffset = 0;
    private static double mouseStartX;
    private static double mouseStartY;
    private static int areaNodes;	//count of points within a lasso selection
    private static ArrayList<Integer> pointsInLasso = new ArrayList<Integer>();
    private static boolean pcaComputed = false;
    private static PrincipalComponentAnalysis pca;
    private static boolean colourCanBeRestored = false; 
    
	/** Constructs a new Fx_Plot with the default options.
	 * Use addSeries(xvalues,yvalues, seriesName, seriesColour) to add data.
	 * @param title the window title
	 * @param xLabel	the x-axis label
	 * @param yLabel	the y-axis label
	 * @see #addSeries(String,double[],double[])
	 */
	public Fx_Scatter(String title, String xLabel, String yLabel) {
		plotTitle = title;
		xTitle = xLabel;
		yTitle = yLabel;
		stackSize = 0;
		uniqueArrayList = new ArrayList<String>();
		groupColoursList = new ArrayList<Color>();
		show();
		
		//If I want to create a truly stand-alone Fx-Scatter class, then I should find a better way to handle stack and table interactions that don't reply on accessing the Dimred classes.
		//Maybe the stack reference can be passed over during construction. Or forget about checking processingStack status and determine stackSize via data 'addSeries' import.
		//The Lookup array is also currently being pulled from the Dimred classes, so consider using the uniqueArrayList being procedurally populated here.
		if (Pca_.processingStack || Tsne_.processingStack || Umap_.processingStack) {
			//Yucky references to Dimred stacks below
			if (Pca_.processingStack && Pca_.stack != null && Pca_.stack.isStack()) {
				stack = Pca_.stack;
				stackSize = stack.getStackSize();
			}
			else if (Tsne_.processingStack && Tsne_.stack != null && Tsne_.stack.isStack()) {
				stack = Tsne_.stack;
				stackSize = stack.getStackSize();
			}
			else if (Umap_.processingStack && Umap_.stack != null && Umap_.stack.isStack()) {
				stack = Umap_.stack;
				stackSize = stack.getStackSize();
			}
			/*
			if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack()) {
				stack = WindowManager.getCurrentImage(); //I think the stack is not being identified as the active image window when mean_out and/or eigen_out images are generated.
				stackSize = stack.getStackSize();
				IJ.log("Stack size = "+Integer.toString(stackSize));
			}
			*/
		} else if (Pca_.processingTable || Tsne_.processingTable || Umap_.processingTable) {
			if (WindowManager.getActiveTable() != null && WindowManager.getActiveTable() instanceof TextWindow) {
				ResultsTable rt = ResultsTable.getActiveTable();
				stackSize = rt.size();
			}
		}
	}
	
	/** Initialise and display the Fx plot.
	 * Called after Fx_Scatter construction and addSeries.
	 * @see #Fx_Scatter()
	 * @see #addSeries()
	 */
	public void show() {
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
	}
	
    private static void initAndShowGUI() {
        // This method is invoked on Swing thread
        JFrame frame = new JFrame("FX plot");
        final JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel);
        frame.setSize(900, 580);
        frame.setVisible(true);
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//ends the java session (including ImageJ) when the plot is closed.

    	Runnable runnable = () -> {
    		initFX(fxPanel);
            };
            FutureTask<Void> task = new FutureTask<>(runnable, null);
            Platform.runLater(task);
            try {
    			task.get();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		} catch (ExecutionException e) {
    			e.printStackTrace();
    		}
    }
    
	/** Adds a set of points to the plot.
	 * @param xDataArray	the x coordinates.
	 * @param yDataArray	the y coordinates.
	 * @param shape			(NOT YET IMPLEMENTED) CIRCLE, X, BOX, TRIANGLE, CROSS, DIAMOND, DOT.
	 * @param seriesName	Label for this set of points, used for a legend and for listing the plots.
	 * @param groupColour	Colour of points in this series.
	 */
	public void addSeries(double[] xDataArray, double[] yDataArray, String seriesName, Color groupColour) {
	    		if (sc_Fx.getData() == null) {
	    			sc_Fx.setData(FXCollections.<XYChart.Series<Number, Number>>observableArrayList());
	    			IJ.log("scatter is null");
    			}
	    		ScatterChart.Series<Number, Number> series = new ScatterChart.Series<Number, Number>();
	    		series.setName(seriesName);
	    		for (int i=0; i<xDataArray.length; i++) {
	    			series.getData().add(new ScatterChart.Data<Number, Number>(xDataArray[i], yDataArray[i]));
	    			//stackSize++;
    			}
	    		uniqueArrayList.add(seriesName); //ScatterChart.Series has a getName function, so this might not be needed for the plot save and load classes
	    		groupColoursList.add(groupColour); //It is weirdly more difficult to get the colour property of nodes, so compiling this list is more useful
	    		String hex = "#"+Integer.toHexString(groupColour.getRGB()).substring(2);
	            for (XYChart.Data < Number, Number > data: series.getData()) {
	            	data.setNode(new Circle(2));
	            	Node node = data.getNode();
	            	node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
	            	//node.setCache(true); //doesn't really help
	            	//node.setCacheHint(CacheHint.SPEED); //doesn't really help
	            	//node.setScaleX(0.5);
	            	//node.setScaleY(0.5);
	            }
	            
	    		if (Pca_.lookupArray != null) {
	    			lookupArray = Pca_.lookupArray;
	    		} else if (Tsne_.lookupArray != null) {
	    			lookupArray = Tsne_.lookupArray;
	    		} else if (Umap_.lookupArray != null) {
	    			lookupArray = Umap_.lookupArray;
	    		}
	            
	            //make the nodes mouse selectable. Code from https://stackoverflow.com/questions/44956955/javafx-use-chart-legend-to-toggle-show-hide-series-possible
	            for (Data<Number, Number> data : series.getData()) {
	                Node node = data.getNode() ;
	                node.setCursor(Cursor.HAND);
	                node.setOnMouseClicked(e -> {
	                	// when a point is selected, highlight the corresponding origin image in the input image stack, if it exists. Note: Perhaps display the image in a new window if a stack is not present.
	                	if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == stackSize) {
	                		stack = WindowManager.getCurrentImage();
	                		stack.setSlice(lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)]);	//note: add a null condition check for when a stack wasn't the input
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
	            
	        	Platform.runLater(new Runnable() {
	        		@Override
	    	    	public void run() {
	        			sc_Fx.getData().add(series);
	        			dynamicLegend();
	        		}
	    		});
    } 
    
	/** Change the x and y axis labels.
	 *  @apiNote more of a placeholder right now as its not used. Consider changing to a public class/method.
	 */
	private static void setAxis(String xString, String yString) {
    			sc_Fx.getXAxis().setLabel(xString);
    			sc_Fx.getYAxis().setLabel(yString);
	}
    
	/** Change the plot title.
	 *  @apiNote more of a placeholder right now as its not used. Consider changing to a public class/method.
	 */
    private static void setTitle(String title) {
    			sc_Fx.setTitle(title);
	}
    
    private static void initFX(JFXPanel fxPanel) {
    	//fxPanelRef = fxPanel; 	
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
    	    }
    	});
    	
    }
    
    private static void dynamicLegend() {
    	Set<Node> items = sc_Fx.lookupAll("Label.chart-legend-item");
        int it=0;
        for (Node item : items) {
        	Label label = (Label) item;
            XYChart.Data<Number, Number> newLegendPoint = new XYChart.Data<Number, Number>();	//create a new datapoint to put into the legend and give it the same style and colours as the corresponding chart data series
            newLegendPoint.setNode(new Circle(2)); //if I allow different shapes, this will need to be populated procedurally
            Node node = newLegendPoint.getNode();
            String hex = "#"+Integer.toHexString(groupColoursList.get(it).getRGB()).substring(2);
            node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
            label.setGraphic(node);		            
            label.getGraphic().setCursor(Cursor.HAND); // Hint to user that legend symbol is clickable
            label.getGraphic().setOnMouseClicked(me -> {
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
            	//if a lasso area has been previously drawn, compute new 'areaNodes' and 'pointsInLasso'
            	if (!multiPath.getElements().isEmpty()) {
            		areaNodes = 0;
    	        	pointsInLasso.clear();
    	            //Iterate over all sc_Fx nodes, series-by-series.
    	            for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
		            	for (Data<Number, Number> data : series.getData()) {
			                //Node node = data.getNode();
		                	if (multiPath.contains(data.getNode().getBoundsInParent().getMinX()+xPathOffset, data.getNode().getBoundsInParent().getMinY()+yPathOffset) && data.getNode().isVisible()) {
			                	areaNodes++;
			                	pointsInLasso.add(lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)]);
			                }
		            	}
    	            }
            	}
            	});
            it++;
            }
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
	        	MenuItem recolour = new MenuItem("Recolour by principal component");
	        	MenuItem restoreColour = new MenuItem("Restore colour");
	        	
	        	contextMenu.getItems().addAll(copy, save, recolour, restoreColour);
	        	contextMenu.show(sc_Fx, e.getScreenX(), e.getScreenY());
	        	if (!colourCanBeRestored) {
	        		restoreColour.setDisable(true);
	        	}
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
	        	recolour.setOnAction(new EventHandler<ActionEvent>() {
	        		public void handle(ActionEvent event) {
	        			//IJ.log("'Recolour by variable' was pressed.");
	        			if (Pca_.lookupArray != null || Tsne_.lookupArray != null || Umap_.lookupArray != null) {
	        				TextInputDialog dialog = new TextInputDialog("");
	        				dialog.setTitle("Recolour plot points");
	        				dialog.setHeaderText("Recolour by principal component");
	        				dialog.setContentText("Principal component:");	
	        				Optional<String> result = dialog.showAndWait();
	        				if (result.isPresent()){
	        					String num = result.get();
	        					if (num.matches("\\d+") && Integer.valueOf(num) > 0) {
	        						recolourByPC(Integer.valueOf(num));
	        						restoreColour.setDisable(false);
	        						colourCanBeRestored = true;
	        					}
	        				}
	        			}
	        		}
	        	});
	        	restoreColour.setOnAction(new EventHandler<ActionEvent>() {
	        		public void handle(ActionEvent event) {
	        			restoreColour();
	        			restoreColour.setDisable(true);
	        			colourCanBeRestored = false;
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
	        	areaNodes = 0;
	        	pointsInLasso.clear();
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
		                	pointsInLasso.add(lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)]);
		                }
	            	}
	            }
	            
	            if (areaNodes == 1) {
	            	IJ.log("1 point in the area.");
	            } else if (areaNodes > 1) {
	            	IJ.log(areaNodes+" points in the area.");
	            }
	            
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

	        	if (WindowManager.getCurrentImage() == null || !(WindowManager.getCurrentImage()).isStack() || WindowManager.getCurrentImage().getStackSize() != stackSize) {
        			toStack.setDisable(true);
        			toStack.setText("Data to stack (Open and select a stack to enable)");
        			toTable.setDisable(true);
        			toTable.setText("Data to table (Open and select a stack to enable)");
        			toStack.setStyle("-fx-stroke-color: rgba(100, 100, 100, 1)");	//disabling the menuitem overrides the colour, so this is not implemented
        			toTable.setStyle("-fx-stroke-color: rgba(100, 100, 100, 1)");	//disabling the menuitem overrides the colour, so this is not implemented
        		} else if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == (stackSize)) {
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
	        			 String titles[] = WindowManager.getImageTitles();
	        			 if (!Arrays.stream(titles).anyMatch("Sub-stack"::equals) && WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && WindowManager.getCurrentImage().getStackSize() == stackSize && areaNodes > 0) {
	        				 ImageStack subStack = WindowManager.getCurrentImage().createEmptyStack();
	        				 for (int i = 0; i < areaNodes; i++) {
	        					 WindowManager.getCurrentImage().setSlice(pointsInLasso.get(i));
	        					 subStack.addSlice(String.valueOf(pointsInLasso.get(i)), WindowManager.getCurrentImage().getProcessor());
	        				 }
	        				 ImagePlus subStackImp = new ImagePlus("Sub-stack of "+Integer.toString(areaNodes)+" datapoints", subStack);
	        				 subStackImp.show();
	        				 WindowManager.getCurrentImage().setSlice(0);
	        			 }
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
    
	/** Colours plot points across a range of values encompassing or spanning the provided double[] array.
	 * @param inputValues	Array of doubles, used to define the colour range. Array size must equal the number of samples/stack-slices.
	 * @param max			Optional maximum value to define the colour bit range.
	 * @param red			Array of red pixel values to compose a LUT (along with green and blue). Should be the length of inputValues or the full 8-bit range (256). 
	 * @param green			Array of green pixel values to compose a LUT (along with red and blue). Length as above.
	 * @param blue			Array of blue pixel values to compose a LUT (along with red and green). Length as above.
	 */
    public void colourByVariable(double[] inputValues, double max, int[] red, int[] green, int[] blue) {
    	
    	//LUT x = new LUT(8, 256, null, null, null); //bits(must be 8), size(<=256), byte[] byte[] byte[]
    	
    	// Info in below post not use here, but is an interesting way to access plot elements
    	// https://stackoverflow.com/questions/39947456/how-to-change-point-size-in-scatter-chart
    	
    	if (inputValues.length != stackSize) {
    		IJ.log("Cannot recolour the plot datapoints using the provided input values, as the input array length does not match the total number of plot points.");
    	} else {
    		
    		if (red.length != 256) {
    			//instead of breaking, perhaps attempt to scale red, green, and blue values to the correct index range.
    			//transform all red values to span the inputValues range
    			//use SplineFitter to interpolate values
    			//SplineFitter sf = new SplineFitter(blue, blue, 0);
    			IJ.log("Cannot recolour the plot datapoints, as 256 red values were not provided.");
    		} else if (green.length != 256) {
    			IJ.log("Cannot recolour the plot datapoints, as 256 green values were not provided.");
    		} else if (blue.length != 256) {
    			IJ.log("Cannot recolour the plot datapoints, as 256 blue values were not provided.");
    		} else {
				for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
		        	for (Data<Number, Number> data : series.getData()) {
		        		Color colour = new Color(red[0], green[0], blue[0]);
		        		//sc_Fx.getData().indexOf(series);
		        		//series.getData().indexOf(data);
		        		String hex = "#"+Integer.toHexString(colour.getRGB()).substring(2);
		        		Node node = data.getNode();
		                node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
		                //Region plotpoint = new Region();
		                //plotpoint.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
		                //plotpoint.setShape(new Circle(0.5));
		                //data.setNode(plotpoint);
		        	}
				}
    		}
    	}      
                
    }
    
	/** Colours plot points across a range of values encompassing or spanning the provided double[] array.
	 * @param inputValues	Array of doubles, used to define the colour range. Array size must equal the number of samples/stack-slices.
	 * @param colour		The colour to apply to the inputValues.
	 */
    public static void colourByVariable(double[] inputValues, Color colour) {
    	colourByVariable(inputValues, 0, colour);
    }
    
	/** Colours plot points across a range of values encompassing or spanning the provided double[] array.
	 * @param inputValues	Array of doubles, used to define the colour range. Array size must equal the number of samples/stack-slices.
	 * @param max			Optional maximum value to define the colour bit range.
	 * @param colour		The colour to apply to the inputValues.
	 */
    public static void colourByVariable(double[] inputValues, double max, Color colour) {
    	
    	if (inputValues.length != stackSize) {
    		//NOTE: as of 27-04-25, DimRed variables are carrying over from previous plots in the same ImageJ session
    		IJ.log("Cannot recolour the plot datapoints using the provided input values, as the input array length does not match the total number of plot points.");
    	}
    	
    	Platform.runLater(new Runnable() {
    		@Override
	    	public void run() {
    	    	int red = colour.getRed();
    	    	int green = colour.getGreen();
    	    	int blue = colour.getBlue();
    	    	int maxVal = Math.max(Math.max(red, green), blue);
    	    	double bitRatio = 255/maxVal;
    	    	red = (int) Math.floor((double) red*bitRatio);
    	    	green = (int) Math.floor((double) green*bitRatio);
    	    	blue = (int) Math.floor((double) blue*bitRatio);
    	    	
    	    	double minInputVal = 0; //need to handle possible minus values.
    	    	double maxInputVal = 0;
    	    	for (int i = 0; i<inputValues.length; i++) {
    	    		if (i == 0) {
    	    			maxInputVal = inputValues[i];
    	    			minInputVal = inputValues[i];
    	    		}
    	    		if (inputValues[i] > maxInputVal) {
    	    			maxInputVal = inputValues[i];
    	    		}
    	    		if (inputValues[i] < minInputVal) {
    	    			minInputVal = inputValues[i];
    	    		}
    	    	}
    	    	bitRatio = 255/(maxInputVal-minInputVal);
    	    	for (int i = 0; i<inputValues.length; i++) {
    	    		inputValues[i] = inputValues[i]-minInputVal; //linear transformation of data to make the minimum value = 0.
    	    		inputValues[i] = Math.floor(inputValues[i]*bitRatio); //scale all data to make maximum value = 255.
    	    		inputValues[i] = inputValues[i]/255; //scale all data between 0 and 1.
    	    	}
    	    	
    	    	
    			for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
    	        	for (Data<Number, Number> data : series.getData()) {
    	        		int lookup = lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)];
    	        		double scaledInputVal = inputValues[lookup-1];
    	        		int nodeRed = (int) Math.floor(red*scaledInputVal);
    	        		int nodeGreen = (int) Math.floor(green*scaledInputVal);
    	        		int nodeBlue = (int) Math.floor(blue*scaledInputVal);
    	        		Color nodeColour = new Color(nodeRed, nodeGreen, nodeBlue);
    	        		String hex = "#"+Integer.toHexString(nodeColour.getRGB()).substring(2);
    	                Node node = data.getNode();
    	                node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
    	                //Region plotpoint = new Region();
    	                //plotpoint.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
    	                //plotpoint.setShape(new Circle(0.5));
    	                //data.setNode(plotpoint);
    	        	}
    			}
    		}
		});
    	
    	/*
    	int red = colour.getRed(); //125 example numbers
    	int green = colour.getGreen(); //0 
    	int blue = colour.getBlue(); //0
    	int maxVal = Math.max(Math.max(red, green), blue);
    	double bitRatio = 255/maxVal; //2.04
    	red = (int) Math.floor((double) red*bitRatio); //255
    	green = (int) Math.floor((double) green*bitRatio); //0
    	blue = (int) Math.floor((double) blue*bitRatio); //0
    	
    	double minInputVal = 0; //need to handle possible minus values.
    	double maxInputVal = 0;
    	for (int i = 0; i<inputValues.length; i++) {
    		if (i == 0) {
    			maxInputVal = inputValues[i];
    			minInputVal = inputValues[i];
    		}
    		if (inputValues[i] > maxInputVal) {
    			maxInputVal = inputValues[i];
    		}
    		if (inputValues[i] < minInputVal) {
    			minInputVal = inputValues[i];
    		}
    	}
    	bitRatio = 255/(maxInputVal-minInputVal);
    	for (int i = 0; i<inputValues.length; i++) {
    		inputValues[i] = inputValues[i]-minInputVal; //linear transformation of data to make the minimum value = 0.
    		inputValues[i] = Math.floor(inputValues[i]*bitRatio); //scale all data to make maximum value = 255.
    	}
    	
    	
		for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
        	for (Data<Number, Number> data : series.getData()) {
        		int lookup = lookupArray[sc_Fx.getData().indexOf(series)][series.getData().indexOf(data)];
        		int scaledInputVal = (int) inputValues[lookup];
        		int nodeRed = (red/red)*scaledInputVal;
        		int nodeGreen = (green/green)*scaledInputVal;
        		int nodeBlue = (blue/blue)*scaledInputVal;
        		Color nodeColour = new Color(nodeRed, nodeGreen, nodeBlue);
        		String hex = "#"+Integer.toHexString(nodeColour.getRGB()).substring(2);
                Node node = data.getNode();
                node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
                //Region plotpoint = new Region();
                //plotpoint.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
                //plotpoint.setShape(new Circle(0.5));
                //data.setNode(plotpoint);
        	}
		}
                
        */        
    }
    
	/** Recolour the plot using a specified principal component.
	 * @param pc		The principal component to use.
	 */
	public static void recolourByPC(int pc) {
		
		if (Pca_.lookupArray != null) {
	    	int maxVal = Pca_.imageMatrix[0].length;
	    	if (stackSize < maxVal) {
	    		maxVal = stackSize;
	    	}
			if (pc <= maxVal) {
				//PCA already computed so just fetch the correct component
				colourByVariable(Pca_.pca.getU(pc-1), Color.RED);
			}
		} else if (Tsne_.lookupArray != null) {
	    	int maxVal = Tsne_.imageMatrix[0].length;
	    	if (stackSize < maxVal) {
	    		maxVal = stackSize;
	    	}
			if (pc <= maxVal) {
				//compute PCA
				if (!pcaComputed) { //check to see if PCA has already been computed. We only need to do it once.
					IJ.log("PCA is being computed to facilitate the user request. For large input data this can take a while.");
					pca = new PrincipalComponentAnalysis();
					pca.setup(stackSize, Tsne_.imageMatrix[0].length);
					for (int p = 0; p < stackSize; p++) {
						pca.addSample(Tsne_.imageMatrix[p]);
					}
					pca.computeBasis(pc);	
					pcaComputed = true;
				}
				colourByVariable(pca.getU(pc-1), Color.RED);
			}
		} else if (Umap_.lookupArray != null) {
	    	int maxVal = Umap_.imageMatrix[0].length;
	    	if (stackSize < maxVal) {
	    		maxVal = stackSize;
	    	}
			if (pc <= maxVal) {
				//compute PCA
				if (!pcaComputed) { //check to see if PCA has already been computed. We only need to do it once.
					IJ.log("PCA is being computed to facilitate the user request. For large input data this can take a while.");
					pca = new PrincipalComponentAnalysis();
					pca.setup(stackSize, Umap_.imageMatrix[0].length);
					for (int p = 0; p < stackSize; p++) {
						pca.addSample(Umap_.imageMatrix[p]);
					}
					pca.computeBasis(pc+1);
					pcaComputed = true;
				}
				colourByVariable(pca.getU(pc-1), Color.RED);
			}
		}
    }
	
	/** Restore plot points colour back to original.
	 */
	public static void restoreColour() {
		
		for (ScatterChart.Series<Number, Number> series : sc_Fx.getData()) {
        	for (Data<Number, Number> data : series.getData()) {
        		String hex = "#"+Integer.toHexString(groupColoursList.get(sc_Fx.getData().indexOf(series)).getRGB()).substring(2);
                Node node = data.getNode();
                node.setStyle("-fx-stroke: "+hex+"; -fx-fill:"+hex);
        	}
		}
    }
	
}