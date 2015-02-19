$(document).ready(function () {
    $('ul.nav-tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    });
    //$('#tab0').show();
});
