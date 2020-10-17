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
	private String label_path="";
	boolean cancelled;
	boolean askForLabels;
	
	public pca_gui() {
		//optionsSelect(); //Currently useless, as only the two primary PCs are being plot. Consider adding support to display/use other PCs.
		if (cancelled) {
			return;
		}
		
		if (askForLabels) {
			label_path = IJ.getFilePath("Choose a .csv label file.");
		}
		
		//There must be a better way to cascade through the options below. Three or more parameters will get quite confusing.
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
	}
		
	
    public void optionsSelect() {
        JTextField num_comp = new JTextField(3);
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
        panel.add(new JLabel("Use a label file to colour data-points?"), constraints);
        
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        panel.add(labelCheck, constraints);
        
        constraints.gridx = 0;
        constraints.gridy = 5;
        panel.add(new JLabel("You will be asked for the file after pressing OK."), constraints);
        
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Optionally enter PCA parameters", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
        	pca_comp = num_comp.getText();
        	if (labelCheck.isSelected()) {
        		askForLabels = true;
        	}
        } else if (result == JOptionPane.CANCEL_OPTION) {
      	  cancelled = true;
        }
     }
}