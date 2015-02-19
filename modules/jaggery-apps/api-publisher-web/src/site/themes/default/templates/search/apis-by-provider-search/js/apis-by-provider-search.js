$(document).ready(function() {
    $('#apiSearch').keydown(function(event) {
        if (event.which == 13) {
            event.preventDefault();
            apiProviderApp.searchAPIs();
        }
    });
});