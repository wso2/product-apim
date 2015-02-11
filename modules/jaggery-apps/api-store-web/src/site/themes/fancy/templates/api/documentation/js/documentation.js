$(document).ready(function () {
    $('.api-documentation a.accordion-toggle').click(
            function () {
                $(this).parent().next().toggle('blind');
            }
    );
});
