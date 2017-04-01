# ImageJ-Seg-FuzzyConnectedness
ImageJ 1.x plugin based on [an example Maven project](https://github.com/imagej/example-legacy-plugin)
Uses a simple GUI to allow performing the Fuzzy Connectedness segmentation algorithm on DICOM image series.

## Dependencies
ImageJ version 1.50i (Java 1.8.0_77 64-bit)

## Installation
Drag [the jar file](/jar/Fuzzy_connectedness.jar) to the /plugins folder in the ImageJ installation directory
Alternatively import the project into an IDE and export the .jar file yourself

## Instructions
Open ImageJ
Open a DICOM image series by clicking File->Open... or File->Import->Image Sequence... and following the dialog
Open the Plugin GUI by clicking Plugin->Fuzzy Connectedness
Hit the 'Select seeds' toggle and click on the target seed points (these can be on any slice of the stack)
Adjust the object threshold to only incorporate pixels/voxels with at least that value of connectedness (range [0,1])
Hit 'Run' to perform the segmentation algorithm
The resulting image can be binarized by hitting the 'Binarize image' checkbox and adjusting the binarization threshold

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## References
* UDUPA, J. K.; SAHA, P. K.; LOTUFO, R. A. Relative Fuzzy Connectedness and Object Definition: Theory, Algorithms, and Applications in Image Segmentation. IEEE Transactions On Pattern Analysis And Machine Intelligence, v. 24, n. 11, p. 1485-1500, 2002.
* NYÚL, L. G.; FALCÃO, A. X.; UDUPA, J. K. Fuzzy-connected 3D image segmentation at interactive speeds. Graphical Models, v. 64, p. 259-281, 2003
* WELFORD, B. P. Note on a method for calculating corrected sums of squares and products. Technometrics 4(3):419–420, 1962
