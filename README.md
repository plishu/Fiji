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
- Post Process Images From Directory: Automatically corrects vignette on RAW/DNG and JPG based on camera model used. RAW/DNG images are converted to TIFF format.
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

## [1.3.1] - 2016-08-05
### Added
- In pre-process: Added warning to user if camera settings are different than those in Flat-Fields
- In calibration: Added warning to user if calibration target image camera settings are different than those of the image to be calibrated
- Added support for Survey1 NDVI
- Added calibration coefficients for the X3
- Added image sharpening for better qr detection in calibration target
- Added new luts: MAPIR_NDVI and MAPIR_NDVI_CB.
- In apply LUT: If lut reference image exists, then it will be copied to the output folder.
- Auto-Adjust NIR subtraction percent for more accurate calibration
- User can now save the plots and calibration target image used in calibration (for debugging purposes)


### Changed
- Histogram equalization to histogram stretch for qr detection. Histogram stretch seems to produce better qr detection
- Removed other MAPIR luts so that only MAPIR_NDVI and MAPIR_NDVI_CB are available.

### Fixed
- Calibration plugin crashing when an ROI exists beforehand.

## [1.3.0] - 2016-07-26
### Added
- Base calibration values for Phantom 3 & Phantom 4
- Support for Phantom 3 X/S models in pre-process, and calibration.
- Index/Lut plugin
- Log file displays version number of plugin
- Debug button in pre-process to aid users in case something goes wrong

### Changed
- JPG/TIF folders are only created if there are JPG/TIF images to calibrate/apply LUT
- In calibration: Separated Phantom 3/4 to two options: Phantom 3 (397mm), Phantom 4 (397mm)
- Changed lut name Survey2_NDVI_0_1 to Survey2_NDVI
- Changed lut name Survey1_NDVI_0_1 to Survey1_NDVI

### TODO
- Generate base calibration values for DJI X3
- QR Code recognition optimization for Red and NIR images
- Improve calibration for dual camera NDVI calibration

## [1.2.1] - 2016-07-22
### Added
- In pre-process: option to toggle vignette removal
- In pre-process: added fail-safe if no JPG images are provided for RAW processing
- In pre-process: added progress notification of processing
- In calibration: calibration target image now closing after calibration process completes
- In calibration: base tif calibration coefficients for Phantom3/Phantom4

### Changed
- In pre-process: cleaned up output log so that extraneous information is not shown anymore

### Fixed
- In pre-process: Fixed index out of bounds exception when processing JPG images only (see: https://github.com/mapircamera/Fiji/issues/1)
- In pre-process: Fixed issue where tmp files during EXIF copying are not being removed properly

### TODO
- Generate base calibratino values for DJI X3
- QR detection optimization


## [1.2.0] - 2016-07-19
### Added
- Support for DJI X3 and DJI Phantom 3 & Phantom 4

### TODO
- Generate base calibration values for DJI X3 and Phantom3/Phantom 4

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
