
(function($, undefined){

	function createDemos(){
		var simple = $("<div id='slider' />").appendTo("body"),
			date = $("<div id='date' />").appendTo("body"),
			modifiable = $("<div id='modifiable' />").appendTo("body");

		simple.sliderDemo();
		date.dateSliderDemo();
		modifiable.editSliderDemo();
	}

	function changeTheme(e){
		var target = $(e.currentTarget),
			path = "../css/",
			theme;

		if (target.hasClass("selected")){
			return
		}

		$("#themeSelector .selected").removeClass("selected");

		theme = target.attr("class");

		$("#themeSelector ."+theme).addClass("selected");

		$("#themeCSS").attr("href", path + theme + ".css");

		setTimeout(function(){
			$(window).resize();
		}, 500);
	}

	function initTheme(){
		$("#themeSelector dd, #themeSelector dt").click(changeTheme);
	}

	$(document).ready(function(){
		createDemos();
		initTheme();
	});

})(jQuery);