# Dimensionality Reduction (v1.0.1)
*version release date:16/12/20*</br>

This plugin captures data from an open image stack or folder of images and performs one of three dimensionality reduction techniques (PCA, t-SNE, or UMAP) to project the high-dimensional data into a lower dimensional (2D) space that is then plotted onto an ImageJ scatter-plot. Under-the-hood, the plugin uses two really-awesome libraries (t-SNE: Leif Jonsson's [pure Java implementation of Van Der Maaten and Hinton's t-sne clustering algorithm](https://github.com/lejon/T-SNE-Java); and UMAP: Jesse Paquette's (of [tag.bio](https://tag.bio/)) [Java implementation of UMAP](https://github.com/tag-bio/umap-java), based on the reference [Python implementation](https://github.com/lmcinnes/umap)). Both are distributed under open-source licences, so even if this plugin doesn't suit you, perhaps their libraries can find a place in your respective projects!

---
## Using the 'Dimensionality Reduction' plugin on an image stack or folder of images

**NEW: [Youtube tutorial](https://www.youtube.com/watch?v=p90ZYCsKdtw)**

The plugin can be called from a GUI dropdown 'Plugins>Dimensionality Reduction>...'</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/0/1/01079646466f8060ba6597766c99df4c65d747a2.png" width="352">

Which will result in a dialogue box being presented, allowing the user to select some key parameters:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/e/2/e23b84c397d4682eb4665c9cfc94693052f70fb8.png" width="352">  (UMAP dialogue example)

Or by macro, with the following:

```javascript
run("PCA");

//or

//run t-SNE with default parameters:
run("t-SNE");
//or specifying (example) parameters:
run("t-SNE", "parallel initial_dims=30 perplexity=50 max_iterations=1000");

//or

//run UMAP with default parameters:
run("UMAP");
//or specifying (example) parameters:
run("UMAP", "n_threads=8 n_nearest=20 metric=manhattan");
```

This version does not incorporate all the parameter options offered in the parent libraries, but many of the most important are covered.

**As an example**, running UMAP on a subset (total=24,754) of 'handwritten' mnist numbers (0-3) in an image stack of this kind:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/6/2/62bc2ed6e44b7b59a0e25a81e5f6ba831c97e42e.gif" width="100"></br>
results in this projection:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/optimized/3X/d/e/deebda183bd316edd55020e28cac8c8b0ba4673f_2_345x237.png" width="352"> Which nicely represents the 4 groups of numbers!</br>
We can also specify a 'label' file upon calling the plugin, to colour the datapoints by ground-truth (or whatever), with the following macro command:

```javascript
run("UMAP", "label_path=[C:/Users/Antinos/Documents/My_label_file.csv]");
//omitting other specified parameters for clarity of the example
```
With the .csv label file structured as a single column of correctly ordered (with respect to the image stack) labels with a single column-header.

With a label file specified, this plot is produced:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/4/0/40b734a3497f90155747ee77cf39ec8b6b64341f.png" width="690">

---
## Using the plugin on non-image data
So images are just ordered arrays of data, therefore it makes sense that these dimensionality reduction techniques can be applied to any non-image data just as well as on images. For convenience, but also because it is an inherently nice way to handle data, to run the plugin on non-image data I recommend encoding that data (e.g. numerical results table/ microarray/ RNA-seq/ or whatever) in an ImageJ image-stack before calling the plugin as usual.

To get you started a convenience function 'Results to Stack' is also included with the plugin, which can be called via the GUI interface:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/8/6/863e6bd488ea6d930ab9acc9d12915c6faba7eb8.png" width="352">

Or by macro:

```javascript
run("Results to Stack");
```
Which pulls ONLY NUMERICAL data from an ImageJ results table to build an N stack of nx1 images, where N is the number of samples (stacks) and n is the number of dimensions. Maybe counter-intuitively, the table should be ordered with rows as dimensions and columns as samples.

Another convenience feature is included 'Rows to Columns', which transposes the data in an open results table. Remember, the 'Dimensionality Reduction' plugin expects N entities to occupy different columns, instead of separate rows. The rows should be reserved for the separate features/dimension data.

**As an example of the plugin applied to non-image data**, adding RNA-seq data of 837 single-cells from the GTEx project [GSE45878](https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE45878) describing the expression of 22704 genes (I may have trimmed the original set a lttle) you can generate this very odd image stack:</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/original/3X/1/2/122a7d9f889d1b0511b89909720bb4753d485224.png" width="690">

From which you can plot the following (example using UMAP):</br>
<img src="https://aws1.discourse-cdn.com/business4/uploads/imagej/optimized/3X/8/4/8400aa72ef49c5abcf54d99f488bf0ba3a472467_2_690x470.png" width="690">

---
## Installation and comments
**Download the plugin from my [googledrive](https://drive.google.com/drive/folders/100w43HWtGFJiPstJmjNOZ-YH9ZFMIy4X?usp=sharing)**. I have also included some test image-stacks and accompanying label.csv files to play with. **To install the plugin, copy 'Dimensionality_Reduction-1.0.0.jar' to your Fiji plugins folder.**

This plugin will also work well with the 'Cluster My Data' plugin: [link to GitHub repo](https://github.com/antinos/Cluster_My_Data-ImageJ)

Some of the other plugin parameters I haven't mentioned include:
* t-SNE 'parallel': as you might guess, this runs the plugin in a multi-threaded fashion.
* UMAP 'metric=': allows the user to pick between metrics to measure distance in the input space, including:
    * euclidean (default)
    * manhattan
    * chebyshev
    * minkowski
    * canberra
    * braycurtis
    * cosine
    * correlation
    * haversine
    * hamming
    * jaccard
    * dice
    * russelrao
    * kulsinski
    * rogerstanimoto
    * sokalmichener
    * sokalsneath
    * yule

---
Future ideas for extending the functions of the 'Dimensionality Reduction' plugin:
* Allow more than 2 output dimensions
    * Many of the underlying libraries already allow for this, so the ImageJ implementation just needs to be considered and effected
* Allow the user to specify an output dimension (e.g. principal component 3/4/5 etc)
* Allow low dimensional datapoints to be related to their original position/label, on an individual basis
    * Ideally, this could be achieved in an interactive manner, such that the user could specify points on the 2D output plot that are then correspondingly highlighted in the original matrix/results table/stack of images
    * The existing ImageJ plot may not be compatible with true interactivity, so another graphing solution may need to be found.