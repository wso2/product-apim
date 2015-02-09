


Handlebars.registerHelper('html_sanitize', function(context, options) {
  context = html_sanitize(context);
  return context;
});

// Load forum topics for the given page and the search term.
function forum_load_topics(page, search) {
    var params = {};

    params.parentId = parentId;

    if (page == undefined) {
        page = 1
    }
    params.page = page;

    if (search != undefined) {
        params.search = search;
    }

    $.getJSON(requestURL + 'forum/api/topic', params, function (result) {
        console.log(result);
        if (result.error == false) {

            var template = Handlebars.partials['topics_list']({
                'topics': result.data
            });
            $('#forum_topics_list').html(template);

            if(result.total_pages > 0){
                $('#forum_topics_list').show();
                $('#forum_no_topics').hide();
            }else{
                $('#forum_topics_list').hide();
                $('#forum_no_topics').show();
            }

            // Show the paginator if the list has more than one page.
            if (result.total_pages > 1) {

                //set the pages
                var options = {
                    currentPage: result.page,
                    totalPages: result.total_pages,
                    alignment: 'right',
                    onPageClicked: function (e, originalEvent, type, page) {
                        forum_load_topics(page, search);
                    }
                }

                $('#forum_topics_list_paginator').bootstrapPaginator(options);
                $('#forum_topics_list_paginator').show();
            } else {
                $('#forum_topics_list_paginator').hide();
            }

        } else {
            jagg.message({
                content: result.message,
                type: "error"
            });
        }
    });
}

// Loads replies for a topic in the give page.
function forum_load_replies(page) {
    var params = {};
    if (page == undefined) {
        page = 1
    }
    params.page = page;

    var currentLocation = window.location.pathname;
    var id = currentLocation.split('/').pop();


    $.getJSON(requestURL + 'forum/api/topic/' + id, params, function (result) {
        console.log(result);
        if (result.error == false) {
            
            var title = Handlebars.partials['topic_title']({
                'replies': result.data
            });

            $('#forum_topic_title_bar').html(title);                

            var template = Handlebars.partials['topic_details']({
                'replies': result.data
            });
            $('#forum_topic_content').html(template);

            var template = Handlebars.partials['replies_list']({
                'replies': result.data
            });
            $('#forum_replies_block').html(template);

            if (result.data.replies.length > 0) {
                $('#forum_replies_list').show();
            }

            // If there are more than one pages show the paginator.
            if (result.total_pages > 1) {
                var options = {
                    currentPage: result.page,
                    totalPages: result.total_pages,
                    alignment: 'right',
                    onPageClicked: function (e, originalEvent, type, page) {
                        forum_load_replies(page);
                    }
                }
                $('#forum_replies_paginator').bootstrapPaginator(options);
            }

            $(forum_reply_editor).summernote({
                height: 300
            });

            initStars();

        } else {
            jagg.message({
                content: result.message,
                type: "error"
            });
        }
    });
}


$(document).ready(function () {


    // START - Topic bindings

    // Add new forum topic.
    $(document).on("click", '#add-forum-topic', function () {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var currentLocation = window.location.pathname;
        var id = currentLocation.split('/').pop();

        var queryString = window.location.search;

        if (queryString) {
            var queryParameters = queryString.split('&');
        }

        var tenantDomain = "";

        if (queryParameters) {
            for (var i = 0; i < queryParameters.length; i++) {
                if (queryParameters[i].indexOf("tenant") > -1) {
                    tenantDomain = "?" + queryParameters[i];
                }
            }
        }

        // Validate inputs.
        if ($('#subject').val().trim() == "") {
            jagg.message({
                content: i18n.t('errorMsgs.topicSubjectCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        if ($('<div>').append($('#topicDescriptioEditor').code()).text().trim() == "") {
            jagg.message({
                content: i18n.t('errorMsgs.topicDescriptionCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        var topic = {
            "parentId": $('#parentId').val(),
            "subject": $('#subject').val(),
            "description": $('#topicDescriptioEditor').code()
        };
        $.ajax({
            type: 'POST',
            url: requestURL + 'forum/api/topic/',
            data: JSON.stringify(topic),
            contentType: "application/json",
            dataType: 'json',
            success: function (result) {
                window.location = requestURL + 'forum/topic/' + result.id + tenantDomain;
            }
        });

    });

    // Delete a topic
    $(document).on("click", ".forum_delete_topic_icon", function (event) {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var deleteButton = this;

        // Show confirmation dialog box.

        $('#messageModal').html($('#confirmation-data').html());
        $('#messageModal div.modal-body').text('\n\n' + i18n.t('confirm.deleteMsgForForumTopic') + '"' + $(deleteButton).attr('data-subject') + '" ?');
        $('#messageModal h3.modal-title').html(i18n.t('confirm.delete'));
        $('#messageModal a.btn-primary').html(i18n.t('info.yes'));
        $('#messageModal a.btn-other').html(i18n.t('info.no'));
        $('#messageModal a.btn-primary').click(function () {
            $.ajax({
                type: 'DELETE',
                url: requestURL + 'forum/api/topic/' + $(deleteButton).attr('data-id'),
                data: "",
                dataType: 'html',
                success: function (data) {
                    var response = JSON.parse(data);
                    if (response.error == false) {
                        $('#messageModal').modal('hide');
                        forum_load_topics(1);
                    } else {
                        var errorMessage = "Cannot delete the topic. "
                        errorMessage = errorMessage + response.message.split(':')[1];
                        jagg.message({
                            content: errorMessage,
                            type: "error"
                        });
                    }
                }
            });
        });

        $('#messageModal').modal();

    });

    // Search topics when the user hits on the enter button.
    $(document).on("keypress", '#forum_topic_search_value', function (e) {
        if (e.which == 13) {
            forum_load_topics(1, $('#forum_topic_search_value').val());
        }
    });

    // Search topic when the user hits on the search button.
    $(document).on("click", '#forum_topic_search', function () {
        forum_load_topics(1, $('#forum_topic_search_value').val());
    })

    // Show topic edit page.
    $(document).on("click", '#forum_edit_topic_icon', function (event) {

        var currentLocation = window.location.pathname;
        var topicId = currentLocation.split('/').pop();

        // Add topic edit input controls.
        var subject = $('#forum_topic_subject_lable').text().trim();
        $('#forum_topic_subject_edit_input').val(subject);

        var description = $('#forum_topic_description').html().trim();
        var topicDescriptionEditor = $("#forum_topic_description_edit_editor");
        $(topicDescriptionEditor).summernote({
            height: 300
        });

        $(topicDescriptionEditor).code(description);

        $('#forum_topic_view_block').hide();
        $('#forum_topic_edit_block').show();
        $('#forum_topic_subject_lable').hide();
        $('#forum_topic_subject_edit_input').show();
        $('#forum_edit_topic_icon').hide();
        $('#forum_topic_subject_edit_input').focus();
    });

    // Cancel topic editing.
    $(document).on("click", '#forum_cancel_topic_edit_button', function (event) {

        $('#forum_topic_edit_block').hide();
        $('#forum_topic_view_block').show();
        $('#forum_edit_topic_icon').show();
        $('#forum_topic_subject_lable').show();
        $('#forum_topic_subject_edit_input').hide();
    });

    // Saves updated topic.
    $(document).on("click", '#forum_save_updated_topic_button', function (event) {

        var currentLocation = window.location.pathname;
        var topicId = currentLocation.split('/').pop();

        // Validate inputs.
        var newSubject = $('#forum_topic_subject_edit_input').val().trim();
        if (newSubject == "") {
            jagg.message({
                content: i18n.t('errorMsgs.topicSubjectCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        var newDescription = $('#forum_topic_description_edit_editor').code();
        if ($('<div>').append(newDescription).text().trim() == "") {
            jagg.message({
                content: i18n.t('errorMsgs.topicDescriptionCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        var topic = {
            "subject": newSubject,
            "description": newDescription,
            "topicId": topicId
        };

        $.ajax({
            type: 'PUT',
            url: requestURL + 'forum/api/topic/',
            data: JSON.stringify(topic),
            contentType: "application/json",
            dataType: 'html',
            success: function (data) {
                var response = JSON.parse(data);
                if (response.error == false) {
                    forum_load_replies(1);
                } else {
                    var errorMessage = i18n.t('errorMsgs.cannotEditForumTopic');
                    errorMessage = errorMessage + response.message.split(':')[1];
                    jagg.message({
                        content: errorMessage,
                        type: "error"
                    });
                }
            }
        });

    });

    // Topic search bindings.
    function getStyleClassFuntion(shouldAddClass) {
        return shouldAddClass ? 'addClass' : 'removeClass';
    }

    $(document).on('input', '.clearable', function () {
        $(this)[getStyleClassFuntion(this.value)]('x');
    }).on('mousemove', '.x', function (e) {
        $(this)[getStyleClassFuntion(this.offsetWidth - 18 < e.clientX - this.getBoundingClientRect().left)]('onX');
    }).on('click', '.onX', function () {
        $(this).removeClass('x onX').val('');
        forum_load_topics(1); // Load all topis when the user clears the search text.
    });

    // END - Topic bindings

    // START - Reply binding

    //Add new reply.

    $(document).on("click", '#forum_add_reply_button', function () {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var currentLocation = window.location.pathname;
        var id = currentLocation.split('/').pop();

        // Validate inputs.
        var replyContent = $('#forum_reply_editor').code();
        if ($('<div>').append(replyContent).text().trim() == "") {
            jagg.message({
                content: i18n.t('errorMsgs.replyCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        var date = new Date();
        var time = date.getTime();

        var replyInfo = getDate(date) + " <br/>" + getTime(time) + " <br/> " + i18n.t('info.replyAdded');
        $('#forum_replies_list').show();
        $('#forum_reply_content_temp').html(replyContent);
        $('#forum_reply_added_block').show();
        $('#forum_reply_info_temp').html(replyInfo);

        $('#forum_reply_editor').code("");

        setTimeout(function () {
            $('#forum_reply_added_block').hide();
            forum_load_replies(1);
        }, 12000);


        $('#forum_reply_editor')

        var topic = {
            "reply": replyContent,
            "topicId": id
        };

        jagg.post("/forum/api/reply", {
            topic: JSON.stringify(topic)
        }, function (result) {
            if (result.error == false) {
                var sHTML = "";
                $("#summernote1").code(sHTML);
            } else {
                jagg.message({
                    content: result.message,
                    type: "error"
                });
            }
        }, "json");


    });

    // Shows reply edit block
    $(document).on("click", ".forum_edit_reply_icon", function (event) {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var currentLocation = window.location.pathname;
        var topicId = currentLocation.split('/').pop();
        var reply = $(this).parent().next().html();
        var id = $(this).data('id');

        // Hide reply content.
        var contentCell = $("#forum_reply_content_cell_" + id);
        contentCell.hide();

        // Show the editor.
        var editor = $("#forum_reply_edit_editor_" + id);
        $(editor).summernote({
            height: 300
        });
        $(editor).code(reply);

        var replyEditorCell = $("#forum_reply_edit_cell_" + id);
        replyEditorCell.show();

    });

    // Saves updated reply.
    $(document).on("click", '.forum_save_updated_reply_button', function (event) {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var currentLocation = window.location.pathname;
        var topicId = currentLocation.split('/').pop();
        var replyId = $(this).data('id');

        var content = $("#forum_reply_edit_editor_" + replyId).code();

        // Validate inputs.
        if ($('<div>').append(content).text().trim() == "") {
            jagg.message({
                content: i18n.t('errorMsgs.replyCannotBeEmpty'),
                type: "error"
            });
            return;
        }

        var topic = {
            "replyId": replyId,
            "reply": content,
            "topicId": topicId
        };

        $.ajax({
            type: 'PUT',
            url: requestURL + 'forum/api/reply',
            data: JSON.stringify(topic),
            contentType: "application/json",
            dataType: 'html',
            success: function (data) {
                var response = JSON.parse(data);
                if (response.error == false) {
                    forum_load_replies(1);
                } else {
                    var errorMessage = i18n.t('errorMsgs.cannotEditForumReply');
                    errorMessage = errorMessage + response.message.split(':')[1];
                    jagg.message({
                        content: errorMessage,
                        type: "error"
                    });
                }
            }
        });

    });

    // Hides reply edit block
    $(document).on("click", '.forum_cancel_reply_edit_button', function (event) {

        var replyId = $(this).data('id');

        var replyEditorCell = $("#forum_reply_edit_cell_" + replyId);
        replyEditorCell.hide();

        var contentCell = $("#forum_reply_content_cell_" + replyId);
        contentCell.show();

    });

    //Deletes a reply.
    $(document).on("click", '.forum_delete_reply_icon', function (event) {
	jagg.sessionAwareJS({redirect:'/site/pages/index.jag'});
        var deleteButton = this;

        $('#messageModal').html($('#confirmation-data').html());
        $('#messageModal div.modal-body').html(i18n.t('confirm.deleteMsgForForumReply'));
        $('#messageModal h3.modal-title').html(i18n.t('confirm.delete'));
        $('#messageModal a.btn-primary').html(i18n.t('info.yes'));
        $('#messageModal a.btn-other').html(i18n.t('info.no'));
        $('#messageModal a.btn-primary').click(function () {
            $.ajax({
                type: 'DELETE',
                url: requestURL + 'forum/api/reply/' + $(deleteButton).attr('data-id'),
                data: "",
                dataType: 'html',
                success: function (data) {
                    var response = JSON.parse(data);
                    if (response.error == false) {
                        $('#messageModal').modal('hide');
                        forum_load_replies(1);
                    } else {
                        var errorMessage = i18n.t('errorMsgs.cannotDeleteForumReply');
                        errorMessage = errorMessage + response.message.split(':')[1];
                        jagg.message({
                            content: errorMessage,
                            type: "error"
                        });
                    }
                }
            });

        });

        $('#messageModal').modal();

    });

    // END - Reply bindings


    // If we are in the topic list page.
    if ($('#forum_topics_list_page').length) {
        var source = $("#forum_template_topics_list").html();
        Handlebars.partials['topics_list'] = Handlebars.compile(source);

        forum_load_topics(1);
    }

    // If we are in the topic details page.
    if ($('#forum_topic_details_page').length) {
        
        var titleSource = $("#fourm_topic_title_template").html();
        Handlebars.partials['topic_title'] = Handlebars.compile(titleSource);    

        var source = $("#forum_topic_details_template").html();
        Handlebars.partials['topic_details'] = Handlebars.compile(source);

        var source = $("#forum_replies_list_template").html();
        Handlebars.partials['replies_list'] = Handlebars.compile(source);

        forum_load_replies(1);
        
    }

    // If we are in the add new topic page.
    if ($('#forum_add_new_topic_page').length) {
        $('#topicDescriptioEditor').summernote({
            height: 350
        });
    }


});

function getDate(date) {
    var dateStr = date.toString();
    var splitArray = dateStr.split(" ");
    var createdDate = splitArray[0] + " " + splitArray[1] + " " + splitArray[2] + " " + splitArray[3];
    return createdDate;
}

function getTime(time) {
    var date = new Date(time);
    var strArray = date.toString().split(" ");
    return strArray[4];
}

var removeTopicRating = function (topic) {
    jagg.post("/site/blocks/forum/ajax/ratings.jag", {
        action: "removeRating",
        topicId: topic.topicId
    }, function (result) {
        if (result.error == false) {
            removeTopicStars(result.averageRating);
        } else {
            jagg.message({
                content: result.message,
                type: "error"
            });
        }
    }, "json");
};

var initStars = function(){

    jagg.initStars($("#forum_topic_rating_block"), function (rating, data) {
            jagg.post("/site/blocks/forum/ajax/ratings.jag", {
                action: "rateTopic",
                topicId: data.topicId,
                rating: rating
            }, function (result) {
                if (result.error == false) {
                    addTopicRating(result.averageRating, rating);
                } else {
                    jagg.message({
                        content: result.message,
                        type: "error"
                    });
                }
            }, "json");
        }, function (data) {
            removeTopicRating(data);
        }, {
            topicId: topicId
        });


}

var addTopicRating = function (newRating, userRating) {
    var tableRow = $("#forum_topic_rating_block").find('table.table > tbody > tr:nth-child(1)');
    var firstHeader = tableRow.find('th');
    var lastCell;

    var averageRating = tableRow.find('div.average-rating');
    if (averageRating.length > 0) {
        averageRating.html(newRating.toFixed(1));
    } else {
        $("<td></td>").append('<div class="average-rating">' + newRating + '</div>').insertAfter(firstHeader);
    }

    lastCell = tableRow.find('td:last')
    lastCell.attr('colspan', 1);

    $.getScript(context + '/site/themes/' + theme + '/utils/ratings/star-generator.js', function () {
        lastCell.find('div.star-ratings').html(getDynamicStars(userRating));

        jagg.initStars($("#forum_topic_rating_block"), function (rating, data) {
            jagg.post("/site/blocks/forum/ajax/ratings.jag", {
                action: "rateTopic",
                topicId: data.topicId,
                rating: rating
            }, function (result) {
                if (result.error == false) {
                    addTopicRating(result.averageRating, rating);
                } else {
                    jagg.message({
                        content: result.message,
                        type: "error"
                    });
                }
            }, "json");
        }, function (data) {
            removeTopicRating(data);
        }, {
            topicId: topicId
        });

    });

};

var removeTopicStars = function (newRating) {
    var tableRow = $("#forum_topic_rating_block").find('table.table > tbody > tr:nth-child(1)');
    var firstHeader = tableRow.find('th');
    var lastCell = tableRow.find('td:last');

    var averageRating = tableRow.find('div.average-rating');
    if (averageRating.length > 0) {
        averageRating.html(newRating.toFixed(1));
    }

    $.getScript(context + '/site/themes/' + theme + '/utils/ratings/star-generator.js', function () {
        lastCell.find('div.star-ratings').html(getDynamicStars(0));

        jagg.initStars($("#forum_topic_rating_block"), function (rating, data) {
            jagg.post("/site/blocks/forum/ajax/ratings.jag", {
                action: "rateTopic",
                topicId: data.topicId,
                rating: rating
            }, function (result) {
                if (result.error == false) {
                    addTopicRating(result.averageRating, rating);
                } else {
                    jagg.message({
                        content: result.message,
                        type: "error"
                    });
                }
            }, "json");
        }, function (data) {
            removeTopicRating(data);
        }, {
            topicId: topicId
        });

    });

};
