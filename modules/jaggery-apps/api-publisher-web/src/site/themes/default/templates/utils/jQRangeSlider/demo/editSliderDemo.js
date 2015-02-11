
(function($, undefined){
	"use strict";

	$.widget("ui.editSliderDemo", $.ui.sliderDemo, {
		_title: "Editable slider",
		_name: "editRangeSlider",

		_createOptions: function(){
			$.ui.sliderDemo.prototype._createOptions.apply(this);

			this._createInputTypeOption();
		},

		_createInputTypeOption: function(){
			this._createDT("Input type");
			var select = $("<select name='type'></select>")
				.append("<option value='number'>number</option>")
				.append("<option value='text' selected='selected'>text</option>");

			this._createDD(select);

			select.change($.proxy(this._easyOptionChange, this));
		}
	});

})(jQuery);