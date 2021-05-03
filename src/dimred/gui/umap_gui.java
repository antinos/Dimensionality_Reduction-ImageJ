package dimred.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ij.IJ;

public class umap_gui {
	private String nNeighbours;
	private String nThreads;
	private String label_path="";
	boolean cancelled;
	boolean askForLabels;
	
	public umap_gui() {
		optionsSelect();
		if (cancelled) {
			return;
		}
		
		if (askForLabels) {
			label_path = IJ.getFilePath("Choose a .csv label file.");
		}
		
		//There must be a better way to cascade through the options below. Three or more parameters will get quite confusing.
		if (nNeighbours != null && !("").equals(nNeighbours) && !nNeighbours.matches(".*[A-Za-z].*") && Double.valueOf(nNeighbours) > 0){
			if (nThreads != null && !("").equals(nThreads) && !nThreads.matches(".*[A-Za-z].*") && Double.valueOf(nThreads) > 1) {
				if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
					IJ.log("UMAP proceeding with 'n nearest neighbours' set to "+nNeighbours+". "+nThreads+" CPU thread(s) have been selected for use.");
					IJ.run("UMAP", "n_nearest="+nNeighbours+" n_threads="+nThreads+" label_path=["+label_path+"]");
					label_path = null;
				} else {
					IJ.log("UMAP proceeding with 'n nearest neighbours' set to "+nNeighbours+". "+nThreads+" CPU thread(s) have been selected for use.");
					IJ.run("UMAP", "n_nearest="+nNeighbours+" n_threads="+nThreads);
				}
			} else {
				if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
					IJ.log("UMAP proceeding with 'n nearest neighbours' set to "+nNeighbours+". CPU parallelisation is off.");
					IJ.run("UMAP", "n_nearest="+nNeighbours+" label_path=["+label_path+"]");
					label_path = null;
				} else {
					IJ.log("UMAP proceeding with 'n nearest neighbours' set to "+nNeighbours+". CPU parallelisation is off.");
					IJ.run("UMAP", "n_nearest="+nNeighbours);
				}
			}
		} else if (nThreads != null && !("").equals(nThreads) && !nThreads.matches(".*[A-Za-z].*") && Double.valueOf(nThreads) > 1) {
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
				IJ.log("UMAP proceeding with 'n nearest neighbours' set to 15. "+nThreads+" CPU thread(s) have been selected for use.");
				IJ.run("UMAP", "n_threads="+nThreads+" label_path=["+label_path+"]");
				label_path = null;
			} else {
				IJ.log("UMAP proceeding with 'n nearest neighbours' set to 15. "+nThreads+" CPU thread(s) have been selected for use.");
				IJ.run("UMAP", "n_threads="+nThreads);
			}
		} else {
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
				IJ.log("UMAP proceeding with default parameters.");
				IJ.run("UMAP", "label_path=["+label_path+"]");
				label_path = null;
			} else {
				IJ.log("UMAP proceeding with default parameters.");
				IJ.run("UMAP");
			}
		}
	}
	
    public void optionsSelect() {
        JTextField numNeighbours = new JTextField(3);
        JTextField numThreads = new JTextField(3);
        JCheckBox labelCheck = new JCheckBox();   
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 4, 0, 0);
        
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("n Nearest Neighbours:"), constraints);
        
        constraints.gridx = 1;
        panel.add(numNeighbours, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 1; 
        constraints.gridwidth = 2;
        panel.add(new JLabel("Larger values result in more global views of the manifold,"), constraints);
        constraints.gridy = 2; 
        panel.add(new JLabel("while smaller values result in more local data being preserved."), constraints);
        constraints.gridy = 3; 
        panel.add(new JLabel("In general values should be in the range 2 to 100. The default is 15."), constraints);
       
        constraints.gridy = 4; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridy = 5; 
        constraints.gridwidth = 1;
        panel.add(new JLabel("CPU Threads to use:"), constraints);
        
        constraints.gridx = 1;     
        panel.add(numThreads, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        panel.add(new JLabel("Values over 1 will enable parallelisation, speeding up processing."), constraints);
        constraints.gridy = 7;
        panel.add(new JLabel("However, with parallelisation, results are no longer deterministic."), constraints);
        constraints.gridy = 8;
        panel.add(new JLabel("If more threads are selected than are available, default(1) is set."), constraints);
        
        constraints.gridy = 9; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        panel.add(new JLabel("Use a label file to colour data-points?"), constraints);
        
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        panel.add(labelCheck, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 11;
        panel.add(new JLabel("You will be asked for the file after pressing OK."), constraints);
        
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Optionally set UMAP parameters", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
        	nNeighbours = numNeighbours.getText();
        	nThreads = numThreads.getText();
        	if (labelCheck.isSelected()) {
        		askForLabels = true;
        	} else {
        		askForLabels = false;
        	}
        	
        } else if (result == JOptionPane.CANCEL_OPTION) {
      	  cancelled = true;
        }

    }
    
}