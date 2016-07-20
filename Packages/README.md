This directory contains the download links to the various pre-packaged Fiji distribution with the MAPIR plugin pre-installed

## Latest Version (1.2.0)

####[Fiji Windows 32-bit](http://www.docs.peauproductions.com/fiji/fiji-win32-20160719.zip)

####[Fiji Windows 64-bit](http://www.docs.peauproductions.com/fiji/fiji-win64-20160719.zip)

## Change Log
All notable changes to this project will be documented in this file.

## [1.2.0] - 2016-07-19
### Added 
- Support for DJI X3 and DJI Phantom 3 & Phantom 4

### TODO
- Generate base calibration values for DJI X3 and Phantom 3/Phantom 4
- QR code detection optimization for calibration targets

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

####[Fiji Windows 32-bit (1.1.0)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160714.zip)

####[Fiji Windows 64-bit (1.1.0)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160714.zip)

####[Fiji Windows 32-bit (2016-06-14)](http://www.docs.peauproductions.com/fiji/fiji-win32-20160614.zip)

####[Fiji Windows 64-bit (2016-06-14)](http://www.docs.peauproductions.com/fiji/fiji-win64-20160614.zip)

Notice: All Fiji packages containing the MAPIR plugins and additional resources are under the Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International license. For more information, please visit: https://github.com/mapircamera/Fiji/blob/master/LICENSE.md
