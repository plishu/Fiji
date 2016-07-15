## Change Log
All notable changes to this project will be documented in this file.

## [1.2.0] - 2016-07-?
### Added
- Support pre-processing of DJIX3 and Phantom cameras

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
