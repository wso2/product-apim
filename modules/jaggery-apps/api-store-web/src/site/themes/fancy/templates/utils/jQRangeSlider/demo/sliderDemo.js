
(function($, undefined){
	"use strict";

	$.widget("ui.sliderDemo", {
		options:{
		},

		_title: "Float values",
		_name: "rangeSlider",

		_create: function(){
			this.element.addClass("ui-sliderDemo");

			this._elements = {};
			this._createTitle();
			this._createZones();
			this._createOptions();
			this._createSlider();
			this._createLog();
			this._createCode();
		},

		destroy: function(){
			this.element.empty();
		},

		_setOption: function(name, value){
			this._elements.slider[this._name]("option", name, value);
		},

		_getOption: function(name){
			return this._elements.slider[this._name]("option", name);
		},

		_easyOptionChange: function(e){
			var target = $(e.target),
				value = target.val(),
				name = target.attr("name");

			if (value === "false"){
				value = false
			} else if (value === "null"){
				value = null
			} else if (!isNaN(parseFloat(value)) && parseFloat(value).toString() == value){
				value = parseFloat(value)
			}

			this._setOption(name, value);
		},

		_createZones: function(){
			var wrapper = $("<div class='wrapper' />").appendTo(this.element),
			inputZone = $("<div class='sliderZone' />").appendTo(wrapper),
				optionsZone = $("<div class='options' />").appendTo(this.element);

			this._elements.sliderZone = $("<form onsubmit='return false' />").appendTo(inputZone);
			this._elements.optionsZone = $("<form onsubmit='return false' />").appendTo(optionsZone);
			this._elements.logZone = $("<div class='logZone' />").appendTo(wrapper);
		},

		_createTitle: function(){
			var title = $("<h2 />");
			title.text(this._title);

			this.element.append(title);
		},

		_createInputs: function(){
			var inputs = $("<dl />"),
				minInputContainer = $("<dd />"),
				maxInputContainer = $("<dd />");

			this._elements.minInput = $("<input type='text' name='min' />").appendTo(minInputContainer);
			this._elements.maxInput = $("<input type='text' name='max' />").appendTo(maxInputContainer);

			inputs.append("<dt>min</dt>")
				.append(minInputContainer)
				.append("<dt>max</dt>")
				.append(maxInputContainer)
				.appendTo(this._elements.sliderZone);
		},

		_createSlider: function(){
			var slider = $("<div />").appendTo(this._elements.sliderZone);
			slider[this._name]();

			this._elements.slider = slider;
		},

		_format: function(value){
			return value;
		},

		_createOptions: function(){
			this._elements.options = $("<dl />").appendTo(this._elements.optionsZone);

			this._createBoundsOptions();
			this._createRangeOptions();
			this._createStepOption();
			this._createWheelModeOption();
			this._createWheelSpeedOption();
			this._createArrowsOption();
			this._createLabelsOption();
		},

		_createBoundsOptions: function(){
			this._createDT("Bounds");

			var minSelect = this._createSelect("min", "Bound"),
				maxSelect = this._createSelect("max", "Bound");

			this._addOption(minSelect, 0);
			this._addOption(minSelect, 10);
			this._addOption(minSelect, 20);

			this._addOption(maxSelect, 100);
			this._addOption(maxSelect, 90);
			this._addOption(maxSelect, 80);

			minSelect.bind("change", "min", $.proxy(this._changeBound, this));
			maxSelect.bind("change", "max", $.proxy(this._changeBound, this));
		},

		_changeBound: function(event, ui){
			var value = $(event.target).val(),
				bounds = this._getOption("bounds");

			bounds[event.data] = parseFloat(value);
			this._setOption("bounds", bounds);
		},

		_createRangeOptions: function(){
			this._createDT("Range limit");

			var minSelect = this._createSelect("min", "RangeLimit"),
			maxSelect = this._createSelect("max", "RangeLimit");

			this._fillMinSelect(minSelect);
			this._fillMaxSelect(maxSelect);

			minSelect.bind("change", $.proxy(this._minSelectChange, this));
			maxSelect.bind("change", $.proxy(this._maxSelectChange, this));
		},

		_createStepOption: function(){
			this._createDT("Step");

			var select = $("<select name='step' />");

			this._createDD(select);

			select.bind("change", $.proxy(this._easyOptionChange, this));

			this._addOption(select, "false");
			this._addOption(select, "2");
			this._addOption(select, "5");
			this._addOption(select, "10");
		},

		_createDT: function(text){
			var dt = $("<dt />");
			dt.text(text);

			this._elements.options.append(dt);
		},

		_createSelect: function(name, suffix){
			var select = $("<select />").attr("name", name + suffix);

			this._elements.options.append($("<dd />")
				.append(name + ":")
				.append(select));

			return select;
		},

		_fillMinSelect: function(select){
			this._addOption(select, "false");
			this._addOption(select, 10);
			this._addOption(select, 20);
			this._addOption(select, 30);
			this._addOption(select, 40);
		},

		_fillMaxSelect: function(select){
			this._addOption(select, "false");
			this._addOption(select, 50);
			this._addOption(select, 60);
			this._addOption(select, 70);
			this._addOption(select, 80);
		},

		_addOption: function(select, text, value){
			var value,
				option = $("<option />");
			
			if (typeof value === "undefined"){
				value = text;
			}

			option.attr("value", value);
			option.text(text);

			select.append(option);

			return option;
		},

		_minSelectChange: function(e){
			var value = $(e.target).val();
			this._setRangeOption(value, "min");
		},

		_maxSelectChange: function(e){
			var value = $(e.target).val();
			this._setRangeOption(value, "max");
		},

		_setRangeOption: function(value, optionName){
			var option = {};

			if (value == "false"){
				option[optionName] = false;
			}else{
				option[optionName] = parseFloat(value);
			}

			this._setOption("range", option);
		},

		_createWheelModeOption: function(){
			this._createDT("Wheel mode");

			var select = $("<select name='wheelMode' />");
			
			this._createDD(select);

			this._addOption(select, "null", "null");
			this._addOption(select, "scroll", "scroll");
			this._addOption(select, "zoom", "zoom");

			select.change($.proxy(this._easyOptionChange, this));	
		},

		_createDD: function(element){
			$("<dd />").append(element).appendTo(this._elements.options);
		},

		_createWheelSpeedOption: function(){
			this._createDT("Wheel speed");

			var input = $("<input type='number' name='wheelSpeed' value='4' />");
			this._createDD(input);

			input.on("click keyup", $.proxy(this._easyOptionChange, this));
		},

		_createArrowsOption: function(){
			this._createDT("Arrows");

			var label = $("<label>Activate arrows</label>"),
				input = $("<input type='checkbox' name='arrows' checked='checked' />").prependTo(label);

			this._createDD(label);

			input.click($.proxy(this._activateArrows, this));
		},

		_activateArrows: function(e){
			var checked = $(e.target).is(":checked");

			this._setOption("arrows", checked);
		},

		_createLabelsOption: function(){
			this._createDT("Value labels");

			var select = $("<select name='valueLabels' />");

			this._addOption(select, "show");
			this._addOption(select, "hide");
			this._addOption(select, "on change", "change");

			this._createDD(select);

			select.change($.proxy(this._easyOptionChange, this));
		},

		_createCode: function(){
			this.element.append("<hr />")
			this.element.append("<h3>Code</h3>");
			var pre = $("<pre />").appendTo(this.element),
				container = $("<code />").appendTo(pre);

			this._fillCode(container);
		},

		_fillCode: function(container){
			this._addComment(container, "Default constructor");
			this._addLine(container, '$("#element").'+ this._name +'();');
		},

		_addComment: function(container, text){
			var span = $("<span class='comment'></span>");

			span.text("// " + text);
			this._addLine(container, span);
		},

		_addLine: function(container){
			for (var i=1; i<arguments.length; i++){
				container.append($("<div class='line'/>").append(arguments[i]));
			}
		},

		_addBlankLine: function(container){
			container.append("<br />");
		},

		_createLog: function(){
			this._elements.logZone.append("<h3>Events</h3>");

			this._elements.log = $("<ol class='log' />").appendTo(this._elements.logZone);

			this._bindEvents();
		},

		_bindEvents: function(){
			this._elements.slider.bind("valuesChanging valuesChanged userValuesChanged", $.proxy(this._log, this));
		},

		_log: function(e, data){
			var line = $("<li />").appendTo(this._elements.log)
				.addClass(e.type);

			line.text(e.type + " " + this._returnValues(data));
			this._elements.log.scrollTop(this._elements.log[0].scrollHeight);
		},

		_returnValues: function(data){
			return "min:" + data.values.min + " max:" + data.values.max;
		}

	});

})(jQuery);