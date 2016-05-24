# Fiji Distribution for MAPIR Cameras
This repository contains the Fiji distribution for use with MAPIR cameras. The distribution contains various plugins and resources such as luts, flat-fields, and calibration files.

Functionality for MAPIR cameras includes:
- Apply vignette removal from RAW AND JPG images
- Convert RAW to TIF
- Image index processing
- Calibration

### Installation
Fiji is an all-in-one package that contains everything you need to run the application. We have added plugins to extend the functionality of Fiji for our MAPIR cameras. You may choose to do an easy installation or a custom installation if you already have Fiji but would like to extend its functionality to support our MAPIR cameras.

#### Easy Installation
For easy installation, download the .zip for your system in the [Packages](/Packages) directory. Everything you need is included within the package that you choose. To launch, simply run the ImageJ executable.

#### Custom Installation
If you already have your version of Fiji, but would like to extend its functionality to support our MAPIR cameras, you may do so by downloading the plugins and its dependencies. Download the Calibration, Survey2, luts, and plugins directories and place them at the root of your Fiji installation; this is the directory where the ImageJ-xxx executable resides. The plugins and luts directories should already exist, simply drag-and-drop the plugins and luts directories from this repository onto the root directory of your Fiji installation.

### Directory
- Calibration: Contains files for the calibration plugin.
- Fiji.app: Demonstrates example Fiji installation with MAPIR plugin. You can use this copy of Fiji, but please note that it is for a 32-bit Windows system.
- Packages: Contains easy installation deployment for various systems. Downloading a zip packages for your system and extracting is sufficient to start using Fiji.
- Survey2: Contains plugin dependencies such as flat-fields, exiftools, and macros.
- luts: Look up tables
- plugins: Plugins for Fiji. 
