var getStaticStars = function (context, rating, theme) {

    var prefix = context + "/site/themes/"+theme+"/utils/ratings/images/";

    var getHTML = function (image) {
        return '<img src="' + prefix + image + '"/>';
    };

    var stars = Math.floor(rating);
    var nonstars = 5 - (stars + 1);
    nonstars = nonstars < 0 ? 0 : nonstars;
    var fraction = rating - stars;
    var html = "";
    for (var j = 0; j < stars; j++) {
        html += getHTML("star-1.0.png");
    }
    if (stars < 5) {
        var image;
        if (fraction >= 0.75) {
            image = "star-0.75.png";
        } else if (fraction >= 0.5) {
            image = "star-0.5.png";
        } else if (fraction >= 0.25) {
            image = "star-0.25.png";
        } else {
            image = "star-0.png";
        }
        html += getHTML(image);
    }
    for (j = 0; j < nonstars; j++) {
        html += getHTML("star-0.png");
    }
    return '<div class="static-rating-stars">' + html + '</div>';
};

var getDynamicStars = function (rating) {
    var getHTML = function (clazz) {
        return '<a class="' + clazz + '"></a>';
    };
    var html = "";
    for (var j = 0; j < 5; j++) {
        html += getHTML(j < rating ? "star-1" : "star-0");
    }
    var selectedRating;
    var returnScript = '<div class="dynamic-rating" style="width:100px;">' +
    '<span>Your rating:</span>' +
    '<span class="dynamic-rating-stars">' + html + '</span>';
    
    if (rating == 0) {
        selectedRating = 'N/A';
        returnScript = returnScript + selectedRating + '</div>';
    } else {
        selectedRating = '<a class="selected-rating">' + rating + '</a>/5';
        returnScript = returnScript + selectedRating +
        '<a title="Remove Rating" class="remove-rating"></a>' +
        '</div>';
    }
    return returnScript;
};
