jQRangeSlider
-------------
* 5.0.2: 2013-03-17
	* Fixed #93 (theming with scales): ticks and handle values desynchronized on the right
* 5.0.1: 2013-03-07
	* Fixed #90 dateRangeSlider displayed outside of the innerbar when setting the range
* 5.0: 2013-02-09
	* Scales
	* New element in handles, can be used for styling. ui-range-slider-handle-inner
* 4.2.10: 2013-02-08
	* Fixed #79: Bar not correctly updated after window resizing, with auto margin on container
* 4.2.9: 2013-01-17
	* Technical release: modified manifest to appear in plugins.jquery.com
* 4.2.8: 2013-01-16
	* Fixed #73: Can't always set the slider values programatically
* 4.2.7: 2013-01-03
	* Fixed #71: Labels not hidden when scrolling with arrows
* 4.2.6: 2012-11-30
	* Fixed #59: NaN value in date slider
* 4.2.5: 2012-11-28
	* Fixed #58: Date labels are shifted after parent resize, even after calling resize method
	* Fixed #35: Event drag (used internally) conflicts with other libraries. Renamed to sliderDrag.
* 4.2.4: 2012-11-19
	* Fixed a bug in resize method, when displaying a slider in a previously hidden parent.
	* Bug in label positionning
* 4.2.3: 2012-11-16
	* Fixed #52 and #53: Labels shown when valueLabels option set to "change"
	* Fixed #51: right label display bug in Chrome
* 4.2.2: 2012-11-08
	* Fixed #46: Labels swap when they are very close
	* Fixed #45: Access to a property of a null object
	* Fixed #49: UserValuesChanged event fired when changing values programmatically
* 4.2.1: 2012-10-04
	* Fixed wheelmode setter in constructor
	* Documentation update
* 4.2: 2012-06-18
	* Draggable labels (Issue #28)
* 4.1.2: 2012-06-11
	* Fixed #29: range option in constructor is not working
* 4.1.1: 2012-06-07
	* Step option is not working in constructor
* 4.1: 2012-05-31
	* New theme: iThing
	* Bug fixes on IE
* 4.0: 2012-05-26
	* Massive rewrite of jQRangeSlider
	* Steps !
	* Native support of touch devices (tested on iOS and Android)
	* Removed min/max values Changing/Changed events. Use valuesChanged or valuesChanging instead.
	* Event log in demo
* 3.2: 2012-05-22
	* Bug #27, generate better input names for editSlider. Names are based on the element id.
* 3.1.1: 2012-05-07 eonlepapillon
	* Fixed bug #22: Event 'userValuesChanged' is not triggered after zooming with wheelmouse
* 3.1: 2012-04-16 nouvak@gmail.com
	* Added the new "userValuesChanged" event that is triggered only on the value changes that were initiated by the user ( e.g. by modifying the range with the mouse).
* 3.0.2: 2012-03-03
	* EditSlider: set values on focus lost
	* editSlider unit tests
* 3.0.1: 2012-03-02
  * Errors in package construction
* 3.0: 2012-03-01
  * **New type of slider**: edit range slider!
  * Packaging minified version of individual files
* 2.4: 2012-02-23
	* Dual license GPL and MIT
	* Small refactoring, allowing to create a modifiable range slider
* 2.3: 2011-11-27
	* Issue #14: limit the range with a minimum or maximum range length.
	* Added the range option
	* New public method for getting / setting bounds
	* use strict
* 2.2.1: 2011-11-15
	* Issue #12: impossible to drag the left handle to the max value
* 2.2: 2011-09-27
	* Issue #11: resize public method added
* 2.1.4: 2011-09-19
  * Issue #10: remove z-ordering
* 2.1.3: 2011-09-07
  * Issue #8 fix: Problem with minified version
  * Script for creating minified package
* 2.1.2: 2011-06-02
	* Issue #6: CSS fix
* 2.1.1: 2011-05-20
  * Integrated Google Closure compiler and script for generating minified version of jQRangeSlider
* 2.1: 2011-03-28 
  * Changed helpers name to labels (API change)
  * Labels replaced inside the top level parent element
* 2.0.2: 2011-03-23 bugfix
* 2.0.1: 2011-03-17 bugfix
* 2.0: 2011-03-14 
	* Value helpers
	* New events: min/maxValueChanging and min/maxValueChanged
	* Bugfixes
	* Unit tests
* 1.1.1: 2011-03-04 bugfixes on public methods
* 1.1  : 2011-03-03 Added methods for setting and getting only one value
* 1.0.2: 2011-03-03 Set values fix
* 1.0.1: 2011-03-02 Fix for IE8
* 1.0  : 2010-12-28 Initial release