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

public class tsne_gui {
	private String perplexity;
	private String initial_dims;
	private String label_path="";
	boolean cancelled;
	boolean askForLabels;
	
	public tsne_gui() {
		optionsSelect();
		if (cancelled) {
			return;
		}
		
		if (askForLabels) {
			label_path = IJ.getFilePath("Choose a .csv label file.");
		}
		
		//There must be a better way to cascade through the options below. Three or more parameters will get quite confusing.
		if (perplexity != null && !("").equals(perplexity) && !perplexity.matches(".*[A-Za-z].*") && Double.valueOf(perplexity) > 0){
			if (initial_dims != null && !("").equals(initial_dims) && !initial_dims.matches(".*[A-Za-z].*") && Double.valueOf(initial_dims) > 0) {
				if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
					IJ.log("t-SNE proceeding with perplexity set to "+perplexity+" and initial dimensions set to "+initial_dims+".");
					IJ.run("t-SNE", "perplexity="+perplexity+" initial_dims="+initial_dims+" label_path=["+label_path+"]");
					label_path = null;
				} else {
					IJ.log("t-SNE proceeding with perplexity set to "+perplexity+" and initial dimensions set to "+initial_dims+".");
					IJ.run("t-SNE", "perplexity="+perplexity+" initial_dims="+initial_dims);
				}
			} else {
				if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
					IJ.log("t-SNE proceeding with n perplexity set to "+perplexity+". The default of 30 initial PCA components are being used as input.");
					IJ.run("t-SNE", "perplexity="+perplexity+" label_path=["+label_path+"]");
					label_path = null;
				} else {
					IJ.log("t-SNE proceeding with n perplexity set to "+perplexity+". The default of 30 initial PCA components are being used as input.");
					IJ.run("t-SNE", "perplexity="+perplexity);
				}
			}
		} else if (initial_dims != null && !("").equals(initial_dims) && !initial_dims.matches(".*[A-Za-z].*") && Double.valueOf(initial_dims) > 0) {
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
				IJ.log("t-SNE proceeding with perplexity set to the default of 50. "+initial_dims+" initial PCA dimesions are being used as t-SNE input.");
				IJ.run("t-SNE", "initial_dims="+initial_dims+" label_path=["+label_path+"]");
				label_path = null;
			} else {
				IJ.log("t-SNE proceeding with perplexity set to the default of 50. "+initial_dims+" initial PCA dimesions are being used as t-SNE input.");
				IJ.run("t-SNE", "initial_dims="+initial_dims);
			}
		} else {
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
				IJ.log("t-SNE proceeding with default parameters (perplexity=50; initial dimensions=30).");
				IJ.run("t-SNE", "label_path=["+label_path+"]");
				label_path = null;
			} else {
				IJ.log("t-SNE proceeding with default parameters (perplexity=50; initial dimensions=30).");
				IJ.run("t-SNE");
			}
		}
	}
	
    public void optionsSelect() {
        JTextField perp = new JTextField(3);
        JTextField ini_dims = new JTextField(3);
        JCheckBox labelCheck = new JCheckBox();   
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 4, 0, 0);
        
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Perplexity:"), constraints);
        
        constraints.gridx = 1;
        panel.add(perp, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 1; 
        constraints.gridwidth = 2;
        panel.add(new JLabel("How many neighbours each data point can sense. Max = N/3."), constraints);
        constraints.gridy = 2; 
        panel.add(new JLabel("Larger values result in more global views of the manifold,"), constraints);
        constraints.gridy = 3; 
        panel.add(new JLabel("while smaller values result in more local data being preserved."), constraints);
        constraints.gridy = 4; 
        panel.add(new JLabel("In general values should be in the range 5 to 50. The default is 50."), constraints);
       
        constraints.gridy = 5; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridy = 6; 
        constraints.gridwidth = 1;
        panel.add(new JLabel("Intital (PCA) dimensions:"), constraints);
        
        constraints.gridx = 1;     
        panel.add(ini_dims, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 2;
        panel.add(new JLabel("Reduce the complexity of t-SNE by feeding it PCA components."), constraints);
        constraints.gridy = 8;
        panel.add(new JLabel("Select enough to ensure no loss of signal but not so many to introduce noise."), constraints);
        
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
        
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Optionally enter t-SNE parameters", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
        	perplexity = perp.getText();
        	initial_dims = ini_dims.getText();
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