//genenames from results table
//Table.showRowNumbers(true);
geneNames = Table.getColumn("gene");
Table.deleteColumn("gene");
Table.create("Genes");
selectWindow("Genes");
Table.setColumn("Genes", geneNames);

wait(100);

//Results to Stack
setBatchMode(true);
run("Results to Image");
run("Rotate 90 Degrees Right");
//Table.rename("Results", "Results-1");
//selectWindow("Results Table");
selectWindow("Results");
resultsNum = nResults;
	print("nResults = "+resultsNum);
headings = Table.headings;
headingsArray = split(headings,"\t");
	print("headingsArray length = "+headingsArray.length);
newImage("stack", "32-bit black", resultsNum, 1, (headingsArray.length-1));

for (j = 0; j < (headingsArray.length-1); j++) {
	selectWindow("stack");
	setSlice(j+1);
	for (i = 0; i < resultsNum; i++) {
		selectWindow("Results Table");
		pix = getPixel(i, j);
		selectWindow("stack");
		setPixel(i, 0, (pix));
	}
	if (isKeyDown("shift")){
		exit();
	}
}
setBatchMode("exit and display");
close("Results Table");
print("Finished");

//180 degree rotation of the stack image
selectWindow("stack");
run("Rotate 90 Degrees Left");
run("Rotate 90 Degrees Left");

