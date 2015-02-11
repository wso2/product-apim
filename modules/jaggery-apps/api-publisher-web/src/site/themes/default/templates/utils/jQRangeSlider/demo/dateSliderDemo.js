

(function($, undefined){
	"use strict";

	$.widget("ui.dateSliderDemo", $.ui.sliderDemo, {
		options: {},
		_title: "Date values",
		_name: "dateRangeSlider",

		_createInputs: function(){
			$.ui.sliderDemo.prototype._createInputs.apply(this, []);

			this._addPicker(this._elements.minInput);
			this._addPicker(this._elements.maxInput);

			(function(that){
				that._elements.minInput.change(function(){
					that._valueChanged($(this).datepicker("getDate"), "min");
				});

				that._elements.maxInput.change(function(){
					that._valueChanged($(this).datepicker("getDate"), "max");
				});
			})(this);
			
			this._elements.minInput.change($.proxy(this._minChanged, this));
		},

		_createBoundsOptions: function(){
			this._createDT("Bounds");

			var minSelect = this._createSelect("min", "Bound"),
				maxSelect = this._createSelect("max", "Bound");

			this._addDateOption(minSelect, new Date(2010, 0, 1));
			this._addDateOption(minSelect, new Date(2010, 2, 1));
			this._addDateOption(minSelect, new Date(2010, 5, 1));

			this._addDateOption(maxSelect, new Date(2011, 11, 31, 11, 59, 59));
			this._addDateOption(maxSelect, new Date(2011, 8, 31, 11, 59, 59));
			this._addDateOption(maxSelect, new Date(2011, 5, 30, 11, 59, 59));

			minSelect.bind("change", "min", $.proxy(this._changeBound, this));
			maxSelect.bind("change", "max", $.proxy(this._changeBound, this));
		},

		_addDateOption: function(select, date){
			this._addOption(select, this._format(date), date.valueOf());
		},

		_changeBound: function(event){
			var value = $(event.target).val(),
				bounds = this._getOption("bounds");

			bounds[event.data] = new Date(parseFloat(value));
			this._setOption("bounds", bounds);
		},

		_createStepOption: function(){
			this._createDT("Step");

			var select = $("<select name='step' />");

			this._createDD(select);

			select.bind("change", $.proxy(this._stepOptionChange, this));

			this._addOption(select, "false");
			this._addOption(select, "2 days", '{"days":2}');
			this._addOption(select, "7 days", '{"days":7}');
			this._addOption(select, "1 month", '{"months":1}');
		},

		_stepOptionChange: function(e){
			var target = $(e.target),
				value = target.val();

			this._setOption("step", $.parseJSON(value));
		},

		_valueChanged: function(value, name){
			this._elements.slider[this._name](name, value);
		},

		_addPicker: function(input){
			input.datepicker({
				maxDate: new Date(2012,0,1),
				minDate: new Date(2010,0,1),
				dateFormat: "yy-mm-dd",
				buttonImage: "img/calendar.png",
				buttonImageOnly: true,
				buttonText: "Choose a date",
				showOn: "both"
				});
		},

		_format: function(value){
			return $.datepicker.formatDate("yy-mm-dd", value);
		},

		_fillMinSelect: function(select){
			this._addOption(select, "false");
			this._addOption(select, "4 weeks", '{"days": 28}');
			this._addOption(select, "8 weeks", '{"days": 54}');
			this._addOption(select, "16 weeks", '{"days": 108}');
		},

		_fillMaxSelect: function(select){
			this._addOption(select, "false");
			this._addOption(select, "365 days", '{"days": 365}');
			this._addOption(select, "400 days", '{"days": 400}');
			this._addOption(select, "500 days", '{"days": 500}');
		},

		_minSelectChange: function(e){
			var value = $(e.target).val();
			this._setRangeOption($.parseJSON(value), "min");
		},

		_maxSelectChange: function(e){
			var value = $(e.target).val();
			this._setRangeOption($.parseJSON(value), "max");
		},

		_setRangeOption: function(value, optionName){
			var option = {};

			if (value == ""){
				option[optionName] = false;
			}else{
				option[optionName] = value;
			}

			this._setOption("range", option);
		},

		_returnValues: function(data){
			try{
				return "min:" + this._format(data.values.min) + " max:" + this._format(data.values.max);	
			} catch (e){
				return e;
			}
			
		}

	});

})(jQuery);