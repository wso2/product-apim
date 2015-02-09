$(document).ready(function() {
    $('#tokenSearch').keydown(function(event) {
        if (event.which == 13) {
            event.preventDefault();
            apiProviderApp.searchAPIs();
        }
    });
});