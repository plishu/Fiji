This directory contains the download links to the various pre-packaged Fiji distribution with the MAPIR plugin pre-installed

## Latest Version (1.3.4)


####[Fiji Windows 32-bit](http://www.docs.peauproductions.com/fiji/fiji-win32-20160914.zip)

####[Fiji Windows 64-bit](http://www.docs.peauproductions.com/fiji/fiji-win64-20160914.zip)


## Change Log
All notable changes to this project will be documented in this file.

## [1.3.4] - 2016-09-14
### Fixed
- Code to collect highest/lowest pixel values across entire input directory not on a per picture basis
- Null pointer exceptions created when calling CameraEXIF.equals().

## [1.3.3] - 2016-09-08
### Changed
- Updated text to "Create Index and Apply LUT to Images" from "Apply LUT to Images" to better reflect functionality at that step.
- Added default return values to EXIFContainer.get* mutater functions.

### Fixed
- DNG EXIF data extraction
- pre-process: Support for DNG only pre-processing out of bounds exception.

>>>>>>> origin/master

## [1.3.2] - 2016-08-17
### Added
- pre-process: Added support for DNG only pre-processing (JPG counterparts are not required)

## [1.3.1] - 2016-08-08
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



## Older versions

####[Fiji Windows 32-bit (1.3.2)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160908.zip)

####[Fiji Windows 64-bit (1.3.2)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160908.zip)

####[Fiji Windows 32-bit (1.3.2)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160817.zip)

####[Fiji Windows 64-bit (1.3.2)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160817.zip)

####[Fiji Windows 32-bit (1.3.1)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160808.zip)

####[Fiji Windows 64-bit (1.3.1)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160808.zip)

####[Fiji Windows 32-bit (1.3.0)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160726.zip)

####[Fiji Windows 64-bit (1.3.0)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160726.zip)

####[Fiji Windows 32-bit (1.2.1)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160722.zip)

####[Fiji Windows 64-bit (1.2.1)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160722.zip)

####[Fiji Windows 32-bit (1.2.0)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160719.zip)

####[Fiji Windows 64-bit (1.2.0)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160719.zip)

####[Fiji Windows 32-bit (1.1.0)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160714.zip)

####[Fiji Windows 64-bit (1.1.0)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160714.zip)

####[Fiji Windows 32-bit (2016-06-14)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160614.zip)

####[Fiji Windows 64-bit (2016-06-14)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160614.zip)

Notice: All Fiji packages containing the MAPIR plugins and additional resources are under the Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International license. For more information, please visit: https://github.com/mapircamera/Fiji/blob/master/LICENSE.md
