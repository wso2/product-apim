$(document).ready(function() {
    $('#apiSearch').keydown(function(event) {
        if (event.which == 13) {
            event.preventDefault();
            apiProviderApp.searchAPIs();
        }
    });

    $('a.help_popup').popover({
        html : true,
        content: function() {
          return $('#'+$(this).attr('help_data')).html();
        }
    });
});