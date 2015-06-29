$( document ).ready(function() {

    $('[data-toggle="tooltip"]').tooltip();

    $("[data-toggle=popover]").popover();

    $(".ctrl-filter-type-switcher").popover({
        html : true,
        content: function() {
          return $('#content-filter-types').html();
        }
    });

	$('#nav').affix({
	      offset: {
	        top: $('header').height()
	      }
	});

    $('.ctrl-wr-asset').click(function(){
        $(this).toggleClass('selected');
    });

    //login details submission
    $("#login-form").validationEngine();
    $("#login-btn").on("click",function(e){
        e.preventDefault();
        var status = $("#login-form").validationEngine('validate');
        if(status){
            var userId = $("#userId").val();
            var userPw = $("#userPw").val();

            $.ajax({
              method: "POST",
              url: "login",
              contentType:"application/json",
              dataType: "json",
              data: JSON.stringify({
                "username": userId,
                "password": userPw
              }),
              beforeSend: function( xhr ) {

              }
            }).done(function( data ) {
                if(data.status == "successful"){
                    location.href = "product.jag";
                }else if(data.status == "error"){
                    alert("Login failed : " + data.errorMessage);
                }else{
                    alert("Login failed : " + JSON.stringify(data));
                }
            }).fail(function( xhr, message ) {
                alert( "Request failed: " + message );
            });

        }else{

        }
    });


    //product details submission
    $("#product-form").validationEngine();
    $("#checkout-btn").on("click",function(e){
        e.preventDefault();
        var status = $("#product-form").validationEngine('validate');
        if(status){
            var prodQty = encodeURI($("#prod-qty-select").val());
            var subTotal = encodeURI($(".sub-total").text());
            var shippingCost = encodeURI($(".shipping-cost").text());
            var taxAmount = encodeURI($(".tax-amount").text());
            var total = encodeURI($(".total").text());

            $.ajax({
              method: "POST",
              url: "product",
              contentType:"application/json",
              dataType: "json",
              data: JSON.stringify({
                "quantity": prodQty,
                "subtotal": subTotal,
                "shipping": shippingCost,
                "tax": taxAmount,
                "total": total,
              }),
              beforeSend: function( xhr ) {

              }
            }).done(function( data ) {
                if(data.status == "successful"){
                    location.href = "shipping.jag";
                }
            }).fail(function( xhr, message ) {
                if(xhr.status == 401){
                  goToSessionExpiryPage();
                }else{
                    alert( "Request failed: " + message );
                }
            });

        }else{

        }
    });

    //shipping details submission
    $("#shipping-details-form").validationEngine();
    $("#billing-btn").on("click",function(e){
        e.preventDefault();
        var status = $("#shipping-details-form").validationEngine('validate');
        if(status){
            var fname = encodeURI($("#fname").val());
            var lname = encodeURI($("#lname").val());
            var address = $("#address").val();
            var city = $("#city").val();
            var state = $("#state").val();
            var postcode = $("#postcode").val();
            var countrycode = $("#countrycode").val();

            $.ajax({
              method: "POST",
              url: "shipping",
              contentType:"application/json",
              dataType: "json",
              data: JSON.stringify({
                "first_name": fname,
                "last_name": lname,
                "line1": address,
                "city": city,
                "state": state,
                "postal_code": postcode,
                "country_code": countrycode
              }),
              beforeSend: function( xhr ) {

              }
            }).done(function( data ) {
                if(data.status == "successful"){
                    location.href = "payment.jag";
                }
            }).fail(function( xhr, message ) {
              if(xhr.status == 401){
                goToSessionExpiryPage();
              }else{
                  alert( "Request failed: " + message );
              }
            });

        }else{

        }
    });

    //billing details submission
    $("#confirm-form").validationEngine();
    $("#confirm-btn").on("click",function(e){
        e.preventDefault();
        var status = $("#confirm-form").validationEngine('validate');
        if(status){
            var ccnumber = encodeURI($("#ccnumber").val());
            var cctype = encodeURI($("#cctype").val());
            var expmonth = encodeURI($("#expmonth").val());
            var expyear = encodeURI($("#expyear").val());
            var seccode = encodeURI($("#seccode").val());
            var email = $("#email").val();
            var baddress = $("#address").val();
            var bcity = $("#city").val();
            var bstate = $("#state").val();
            var bpostcode = $("#postcode").val();
            var bcountrycode = $("#countrycode").val();

            var mockClientIP = encodeURI($("#mockClientIP").val());

            $.ajax({
              method: "POST",
              url: "payment",
              contentType:"application/json",
              dataType: "json",
              data: JSON.stringify({
                "number": ccnumber,
                "type": cctype,
                "expire_month": expmonth,
                "expire_year": expyear,
                "cvv2": seccode,
                "email":email,
                "line1": baddress,
                "city": bcity,
                "state": bstate,
                "postal_code": bpostcode,
                "country_code": bcountrycode
              }),
              beforeSend: function( xhr ) {
                if(mockClientIP && mockClientIP.length > 6){
                  xhr.setRequestHeader("Fraud-Detection-Mock-Client-IP", mockClientIP);
                }
              }
            }).done(function( data ) {
                if(data.status == "successful"){
                  location.href = "thanking.jag";
                }
            }).fail(function( xhr, message ) {
              if(xhr.status == 401){
                goToSessionExpiryPage();
              }else{
                  alert( "Request failed: " + message );
              }
            });

        }else{

        }
    });



    $("#prod-qty-select").on("keyup",function(){
        var qty = Number($(this).val());
        var price = Number($(".unit-price").text());
        var subTotal = qty * price;
        if(qty == 1){
            $(".es").hide();
        }else{
            $(".es").show();
        }

        if(isNaN(qty)){
            $(".prod-qty-display").text("0");
            $(".sub-total").text("0");
        }else{
            $(".prod-qty-display").text(qty);
            $(".sub-total").text(subTotal);
            calculateTotal();
        }

    });
    calculateTotal();

});



function openCollapsedNav(){
    $('.wr-hidden-nav-toggle-btn').addClass('active');
    $('#hiddenNav').slideToggle('slideDown', function(){
        if($(this).css('display') == 'none'){
            $('.wr-hidden-nav-toggle-btn').removeClass('active');
        }
    });
}

function calculateTotal(){
    var subTotal = Number($(".sub-total").text());
    var shippingCost = Number($(".shipping-cost").text());
    var taxAmount = Number(subTotal * 0.02);
    var total = subTotal + shippingCost + taxAmount;
    $(".tax-amount").text(taxAmount);
    $(".total").text(total);
}

function goToSessionExpiryPage(){
  location.href = "unauthorized.html";
}
