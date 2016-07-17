# Fiji Distribution for MAPIR Cameras
This repository contains the Fiji distribution for use with MAPIR cameras. The distribution contains various plugins and resources such as luts, flat-fields, and calibration files.

Camera supported:
- MAPIR Survey2 (all filter models)
- DJI Inspire X3 with [PEAU 3.97mm NDVI (Red+NIR) lens](http://www.peauproductions.com/products/gp39728) installed
- DJI Phantom 4 & 3 with [PEAU 3.97mm NDVI (Red+NIR) lens](http://www.peauproductions.com/products/gp39728) installed

Functionality for above supported cameras includes:
- Convert Survey2 RAW to TIFF, converts DJI DNG to TIFF
- Correct vignette
- Calibrate directory of images using [MAPIR Reflectance Target](http://www.mapir.camera/collections/accessories/products/mapir-camera-calibration-ground-target-package) image taken before survey or uses built-in reflectance values if no target image supplied
- Converts TIFF to JPG after calibration if desired

### Installation
Fiji is an all-in-one package that contains everything you need to run the application. We have added plugins to extend the functionality of Fiji for our MAPIR cameras. You may choose to do an easy installation or a custom installation if you already have Fiji but would like to extend its functionality to support our MAPIR cameras.

#### Easy Installation (RECOMMENDED)
For easy installation, download the .zip for your system in the [Packages](/Packages) directory. Everything you need is included within the package that you choose. To launch, simply run the ImageJ executable.

#### Custom Installation
If you already have your version of Fiji, but would like to extend its functionality to support our MAPIR cameras, you may do so by downloading the plugins and its dependencies. Download the [Calibration](/Calibration), [Survey2](/Survey2), [luts](/luts), and [plugins](/plugins) directories and place them at the root of your Fiji installation; this is the directory where the ImageJ-xxx executable resides. The plugins and luts directories should already exist, simply replace these directories with the ones in this repository.

### Using Plugin
Within Fiji's Plugin submenu, you will see a MAPIR option. The MAPIR plugin contains tools that aid with post processing:
- Post Process Images From Directory: Automatically corrects vignette on RAW and JPG based on camera model used. RAW images are converted to TIFF format.
- Calibrate Images From Directory: Automatically detects pixels of user supplied [MAPIR Reflectance Target](http://www.mapir.camera/collections/accessories/products/mapir-camera-calibration-ground-target-package) image taken just before survey and applies calibration to directory of images captured during survey. If photo of target is not provided the hard-coded values (taken during a clear sunny day) will be used instead to claibrate survey images. User can also choose to convert TIFF to JPG format, helpful for ortho-mosaic generating software that does not accept the preferred TIFF format (such as Drone Deploy).

### Directory
- [Calibration](/Calibration): Contains files for the calibration plugin.
- [Fiji.app](/Fiji.app): Demonstrates example Fiji installation with MAPIR plugin. You can use this copy of Fiji, but please note that it is for a 32-bit Windows system.
- [Packages](/Packages): Contains easy installation deployment for various systems. Downloading a zip packages for your system and extracting is sufficient to start using Fiji.
- [Survey2](/Survey2): Contains plugin dependencies such as flat-fields, exiftools, and macros.
- [luts](/luts): Look up tables
- [plugins](/plugins): Plugins for Fiji. 

### Credits
 - [Photomonitoring Plugin](https://github.com/nedhorning/PhotoMonitoringPlugin) by Ned Horning - American Museum of Natural History, Center for Biodiversity and Conservation

## Change Log
All notable changes to this project will be documented in this file.

## [1.1.0] - 2016-07-14
### Added
- Calibration progress information displayed to ImageJ log
- Calibrated images now do not replace older calibrated images (if they exist)
- Option to only calibrate tif images (jpgs are created from tif).
- Auto calibration target detection
- Added incorrect calibration target to selected camera model detection
- Better UI
- Ability to select a whole directory of images to calibrate

### Changed
- Calibrated images are now saved into Calibrated/Jpgs or Calibrated/Tifs
- Changed calibration coefficients to accurately represent calibration fix
- Optimized QR code detection for tifs
- MAPIR plugin name from Calibrate Image -> Calibrate Images From Directory
- MAPIR plugin name from Apply Calibration coefficients To Directory -> Apply Calibration coefficients To Directory [Legacy]

### Fixed
- Fixed calibration procedure for all camera types.
- Tif 3 stack to 1 fix for Fiji
- Fixed tif calibration not calibrating correctly
