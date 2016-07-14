# Change Log
All notable changes to this project will be documented in this file.

## [1.1.0] - 2016-07-14
### Added
- Calibration progress information displayed to ImageJ log
- Calibrated images now do not replace older calibrated images (if they exist)
- Option to only calibrate tif images (jpgs are created from tif).

## Changed
- Calibrated images are now saved into Calibrated/Jpgs or Calibrated/Tifs
- Changed calibration coefficients to accurately represent calibration fix
- Optimized QR code detection for tifs

## Fixed
- Fixed calibration procedure for all camera types.
- Tif 3 stack to 1 fix for Fiji

## [1.0.0] - 2016-07-07
### Added
- Auto calibration target detection
- Added incorrect calibration target to selected camera model detection
- Better UI

## Changed
- MAPIR plugin name from Calibrate Image -> Calibrate Images From Directory
- MAPIR plugin name from Apply Calibration coefficients To Directory -> Apply Calibration coefficients To Directory [Legacy]


## [0.0.9] - 2016-07-06
### Added
- Ability to select a whole directory of images to calibrate


### Fixed
- Fixed tif calibration not calibrating correctly
