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

public class pca_gui {
	private String pca_comp;
	private String pc_x;
	private String pc_y;
	private String eigen_out;
	private String label_path="";
	private String arguments= "";
	boolean cancelled;
	boolean askForLabels;
	
	public pca_gui() {
		optionsSelect();
		if (cancelled) {
			return;
		}
		
		if (askForLabels) {
			label_path = IJ.getFilePath("Choose a .csv label file.");
		}
		
		if (pca_comp != null && !("").equals(pca_comp) && !pca_comp.matches(".*[A-Za-z].*") && Double.valueOf(pca_comp) > 0){
			pca_comp.trim();
			arguments = arguments + " " + "pca_comp=" + pca_comp;
		}
		
		if (pc_x != null && !("").equals(pc_x) && !pc_x.matches(".*[A-Za-z].*") && Double.valueOf(pc_x) > 0) {
			pc_x.trim();
			arguments = arguments + " " + "pc_x=" + pc_x;
		}
		
		if (pc_y != null && !("").equals(pc_y) && !pc_y.matches(".*[A-Za-z].*") && Double.valueOf(pc_y) > 0) {
			pc_y.trim();
			arguments = arguments + " " + "pc_y=" + pc_y;
		}
		
		if (eigen_out != null && !("").equals(eigen_out) && !eigen_out.matches(".*[A-Za-z].*") && Double.valueOf(eigen_out) > 0) {
			eigen_out.trim();
			arguments = arguments + " " + "eigen_out=" + eigen_out;
		}
		
		if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
			arguments = arguments + " " + "label_path=["+label_path+"]";
		}
		
		IJ.run("PCA", ""+arguments);
		
		/*
		//There must be a better way to cascade through the options below. Three or more parameters will get quite confusing. <-- There was! See above.
		if (pca_comp != null && !("").equals(pca_comp) && !pca_comp.matches(".*[A-Za-z].*") && Double.valueOf(pca_comp) > 0){
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {								
				IJ.log("PCA proceeding with retained components set to "+pca_comp+".");
				IJ.run("PCA", "pca_comp="+pca_comp+" label_path=["+label_path+"]");
			} else {
				IJ.log("PCA proceeding with retained components set to "+pca_comp+".");
				IJ.run("PCA", "pca_comp="+pca_comp);
			}
		} else {
			if (label_path != null && !("").equals(label_path) && label_path.matches(".*[A-Za-z].*")) {
				IJ.run("PCA", "label_path=["+label_path+"]");
			}
				IJ.run("PCA");
		}
		*/
	}
		
	
    public void optionsSelect() {
        JTextField num_comp = new JTextField(3);
        JTextField x_axis = new JTextField(3);
        JTextField y_axis = new JTextField(3);
        JTextField eigenvector = new JTextField(3);
        JCheckBox labelCheck = new JCheckBox();  
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 4, 0, 0);
        
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Components to keep:"), constraints);
        
        constraints.gridx = 1;
        panel.add(num_comp, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 1; 
        constraints.gridwidth = 2;
        panel.add(new JLabel("Number of vectors that will be used to describe the data."), constraints);
        constraints.gridy = 2; 
        panel.add(new JLabel("Typically much smaller than the number of elements in the input vector."), constraints);
        
        constraints.gridy = 3; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        panel.add(new JLabel("X-axis principal component:"), constraints);
        
        constraints.gridx = 1;
        panel.add(x_axis, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 5;
        panel.add(new JLabel("Y-axis principal component:"), constraints);
        
        constraints.gridx = 1;
        panel.add(y_axis, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 6; 
        constraints.gridwidth = 2;
        panel.add(new JLabel("The princiapl component axes to compare on the scatterplot."), constraints);
        constraints.gridy = 7; 
        panel.add(new JLabel("Should be set at or below the number of input stack-slices/images."), constraints);
        
        constraints.gridy = 8; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 1;
        panel.add(new JLabel("Eigenvector to display:"), constraints);
        
        constraints.gridx = 1;
        panel.add(eigenvector, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 10; 
        constraints.gridwidth = 2;
        panel.add(new JLabel("The eigenvector (eigenface) to reconstruct and display as an image."), constraints);
        constraints.gridy = 11; 
        panel.add(new JLabel("'Components to keep' should be equal or higher to this number."), constraints);
        constraints.gridy = 12; 
        panel.add(new JLabel("NOTE: PARTIALLY WORKING. I believe the vectors are correct but the pixel display range is not."), constraints);
        
        constraints.gridy = 13; 
        panel.add(new JLabel("-----------------------------------------------------------------------------------------"), constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        panel.add(new JLabel("Use a label file to colour data-points?"), constraints);
        
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        panel.add(labelCheck, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 15;
        panel.add(new JLabel("You will be asked for the file after pressing OK."), constraints);
        
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Optionally enter PCA parameters", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
        	pca_comp = num_comp.getText();
        	pc_x = x_axis.getText();
        	pc_y = y_axis.getText();
        	eigen_out = eigenvector.getText();
        	if (labelCheck.isSelected()) {
        		askForLabels = true;
        	}
        } else if (result == JOptionPane.CANCEL_OPTION) {
      	  cancelled = true;
        }
     }
}