## Change Log
All notable changes to this project will be documented in this file.

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
- Generate base calibration values for DJI X3
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
