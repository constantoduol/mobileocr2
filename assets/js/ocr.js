function App() {
    this.resizeables = [];
    this.ui = new UI();
    this.save_location = "phone";
    this.api_key = "ns2g3h1m67ai3o4vr39j";
    var self = this;
    window.onresize = App.prototype.doResize;
    $(document).ready(function(){
        var path = window.location.pathname;
        path = path.substring(path.lastIndexOf("/"),path.length);
        var onload = {
            always : function(){
                //initialize the modal window
                var modalArea = $("<div id='modal_area'></div>");
                if (!$("#modal_area")[0])
                    $("body").append(modalArea);  
                //setup a user id
                var userId = jse.getItem("user_id");
                if(!userId || userId === "null"){
                    userId = app.rand(20);
                    jse.setItem("user_id",userId);
                }
                self.user_id = userId;
            },
            "/props_interface.html": function () {
                var height = app.getDim()[1] - 150;
                $("#property_div").css("height", height + "px");
                $("#phone_save").click(function(){
                   app.selectSaveLocation("phone"); 
                });
                $("#cloud_save").click(function () {
                    app.selectSaveLocation("cloud");
                });
                var location = localStorage.getItem("save_location");
                location = !location ? "phone" : location;
                app.selectSaveLocation(location);
                
            }
        };
        onload.always();
        !onload[path] ? null : onload[path](); 
    });
}


App.prototype.doResize = function () {
    for (var x = 0; x < app.resizeables.length; x++) {
        var options = app.resizeables[x];
        var elem = $("#" + options.id);
        var dim = App.prototype.getDim();
        var keys = Object.keys(options);
        for (var y = 0; y < keys.length; y++) {
            var key = keys[y];
            var value = options[key];
            if (key === "id")
                continue; //skip id
            var on = value.on;
            var dimen;
            if (on === "width") {
                dimen = value.factor * dim[0] + "px";
            }
            else if (on === "height") {
                dimen = value.factor * dim[1] + "px";
            }
            elem.css(key, dimen);

        }
    }
};

App.prototype.initAperture = function(){
    var height = app.getDim()[1];
    var topOffset = height*3/7;
    var apertureHeight = height*1/7;
    $("#aperture").css("top",topOffset+"px");  
    $("#aperture").css("height",apertureHeight+"px"); 
};

App.prototype.resize = function (options) {
    this.resizeables.push(options);
    App.prototype.doResize();
};

App.prototype.getDim = function () {
    var body = window.document.body;
    var screenHeight;
    var screenWidth;
    if (window.innerHeight) {
        screenHeight = window.innerHeight;
        screenWidth = window.innerWidth;
    }
    else if (body.parentElement.clientHeight) {
        screenHeight = body.parentElement.clientHeight;
        screenWidth = body.parentElement.clientWidth;
    }
    else if (body && body.clientHeight) {
        screenHeight = body.clientHeight;
        screenWidth = body.clientWidth;
    }
    return [screenWidth, screenHeight];
};


App.prototype.loadContent = function (content) {
    content = decodeURIComponent(content);
    $("#text_container").addClass("black-view");
    $("#text_container").html("<span id='detected_text'>"+content+"</span>");
    var actionSelect = $(
            "<div id='action_select_container'>"+
            "<span id='action_select'>Select Action</span>"+
            "<span style='float:right'><img src='img/down.png' style='width:35px'></span>"+
            "</div>");
    actionSelect.click(function(){
        app.launchActionSelect();
    });
    $("#text_container").append(actionSelect);
};

App.prototype.populateExisting = function(detectedText){
    var content = decodeURIComponent(detectedText);
    var saveLocation = localStorage.getItem("save_location");
    if(saveLocation === "cloud"){
        jse.startLoad("Retrieving from cloud");
        //do an xhr and fetch the data
        app.xhr({
            load: false,
            svc: "open_data",
            msg: "get",
            data: {
               where_user_id : app.user_id,
               where_primary_key_value : content
            },
            success: function (data) {
                console.log(data);
            },
            error: function () {
                jse.stopLoad();
                app.msg("There was a problem retrieving from the cloud. Please check your internet connection");
            }
        }); 
    }
    else {
        var json = jse.getExistingData(content);
        console.log(json);
    }
};

App.prototype.launchActionSelect = function(){
    var actionKeys = ["ussd_action", "web_search_action", "associate_action"];
    var actionHtml = ["USSD", "Web Search", "Save Record"];
    app.launchSelect({id: "action_select", title: "Action To Take", values: actionKeys, html: actionHtml});  
};

App.prototype.loadImages = function (imgs) {
    $("#img_container").html(imgs);
};

App.prototype.startLoad = function () {
    $("#img_container").html("<img src='img/loader.gif' id='loader' class='round_icon icon'>\n\
        <img src='img/cancel.png' id='cancel' class='icon round_icon' style='background:red'>");
    app.resize({
        id: "loader",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        left : {on : "width",factor : 0.15},
        top : {on : "height",factor : 0.15}
    });
    
    app.resize({
        id: "cancel",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        left: {on: "width", factor: 0.55},
        top: {on: "height", factor: 0.15}
    });
    $("#cancel").click(function(){
       jse.cancelOCR(); 
    });
};

App.prototype.stopLoad = function () {
    $("#img_container").html("");
};

App.prototype.runLater = function(delay,func){
    return setTimeout(func,delay);
};

App.prototype.loadUserAgree = function () {
    //$("#img_container").append("<hr>");
    var img = $("<img src='img/replay.png' id='user_cancel' class='round_icon icon'>");
    img.click(function(){
        //remove the black view where text content is shown
        $("#text_container").removeClass("black-view");
        jse.resetScreen();
        //perform this action in the future, delay by 3 seconds
        app.runLater(3000,function(){
            jse.setSafeToTakePicture('true'); 
        });
    });
    $("#img_container").append(img);
    app.resize({
        id: "user_cancel",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        top :{on:"height",factor : 0.78}
    });
    
    var img1 = $("<img src='img/proceed.png' id='user_proceed' class='round_icon blink' style='position:absolute'>");
    img1.click(function () {
        var type = $("#action_select").attr("current-item"); //get the selected action
        var text = $("#detected_text").html(); //get the ocr detected text
        if(!type){
            //well seems we dont have a default action type
            //tell the user this
            app.launchActionSelect();
            return;
        }
        jse.startActionActivity(type,text);
    });
    $("#img_container").append(img1);
    app.resize({
        id: "user_proceed",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        top :{on:"height",factor : 0.78},
        left : {on :"width",factor:0.7}
    });
};

App.prototype.launchSelect = function (options) {
    var currentValue = $("#" + options.id).attr("current-item");
    //options.values, options.html
    var m = app.ui.modal("", options.title, {cancelText: "Cancel"});
    var area = $("#modal_content_area");
    var contDiv = $("<div style='font-size:20px'></div>");
    $.each(options.values, function (x) {
        var value = options.values[x];
        var html = options.html[x];
        var div = $("<div class='long_link'>" + html + "</div>");
        if (value === currentValue)
            div.css("background-color", "orange");
        div.click(function () {
            $("#" + options.id).attr("current-item", value);
            $("#" + options.id).html(html);
            m.modal('hide');
        });
        contDiv.append(div);
    });
    area.append(contDiv);
};


App.prototype.briefShow = function (options) {
    var m = app.ui.modal(options.content, options.title, {
    });
    var delay = !options.delay ? 3000 : options.delay;
    app.runLater(delay, function () {
        m.modal('hide');
    });
};

App.prototype.loadUSSDField = function(detectedText){
    var content = decodeURIComponent(detectedText);
    $("#ussd_postfix").val(content);
};

App.prototype.loadRecordField = function(detectedText){
    var content = decodeURIComponent(detectedText);
    app.addProperty();
    $(".prop_value").val(content);
    $(".prop_value").attr("id", "primary_key_value");
    $(".prop_name").attr("id", "primary_key_name");
};

App.prototype.addProperty = function(){
    var id = "prop_id_"+app.rand(6);
    var div = $("<div><input type='button' href='#' class='close' value='x' onclick='app.removeProperty(this)' style='margin-right:10px;color:red'><br>\n\
                <input type='text' class='prop_name form-control' placeholder='e.g name' id="+id+" style='margin:10px;border-color: #51CBEE;'>\n\
                <input type='text' class='prop_value form-control' placeholder='e.g sam' style='margin:10px'><hr></div>");  
    $("#property_div").append(div);
    $("#"+id).focus();
};

App.prototype.rand = function(len){
    return Math.ceil(Math.random()*Math.pow(10,len));
};

App.prototype.removeProperty = function(elem){
    $(elem).parent().remove();
};

App.prototype.selectSaveLocation = function(location){
    if(location === "phone"){
        //show that phone is selected
        $("#phone_save").css("background-color","white");
        $("#cloud_save").css("background-color","#51CBEE");
    }
    else if(location === "cloud"){
        $("#cloud_save").css("background-color","white");
        $("#phone_save").css("background-color","#51CBEE");
    }
    app.save_location = location;
    localStorage.setItem("save_location",location);
};

App.prototype.saveRecord = function(){
    var location = localStorage.getItem("save_location");
    var type = $("#data_type").val();
    if (!type) {
        app.msg("Please select a category for your data");
        $("#data_type").focus();
        return;
    }
    var m = app.ui.modal("Save data to "+location+" ?","Save Data",{
        okText : "Save",
        cancelText : "Cancel",
        ok : function(){
            var propNames = $(".prop_name");
            var propValues = $(".prop_value");
            var props = {};
            for (var x = 0; x < propNames.length; x++) {
                var name = propNames[x].value;
                var value = propValues[x].value;
                if (!name) {
                    $(propNames[x]).focus();
                    return;
                }
                else if(!value) {
                    $(propValues[x]).focus();
                    return;
                }
                props[name] = value;
            }
            var obj = {};
            obj.properties = JSON.stringify(props);
            obj.category = type.trim();
            obj.user_id = app.user_id;
            obj.primary_key_value = $("#primary_key_value").val().trim();
            obj.primary_key_name = $("#primary_key_name").val().trim();
            m.modal('hide');
            location === "cloud" ? app.saveToCloud(obj) : jse.saveRecord(JSON.stringify(obj));
            if(location === "phone"){
                jse.toast("Data saved to phone successfully");
            }
        }
    });
};



App.prototype.xhr = function (options) {
    var request = {};
    request.request_header = {};
    request.request_header.request_svc = options.svc;
    request.request_header.request_msg = options.msg;
    request.request_object = options.data;
    if (options.load) {
        $("#load_area").html("<img src='img/loader.gif'>");
    }
    var defaultOptions = {
        method: "post",
        url: "/server",
        data: "json=" + encodeURIComponent(JSON.stringify(request)),
        dataFilter: function (data) {
            if (options.load) {
                $("#load_area").html("");
            }
            var json = JSON.parse(data);
            if (json.request_msg === "auth_required") {
                window.location = "index.html";
            }
            return json;
        }
    };
    defaultOptions.success = options.success;
    defaultOptions.error = options.error;
    return $.ajax(defaultOptions);
};

App.prototype.saveToCloud = function(data){
    jse.startLoad("Saving to cloud...");
    data.api_key = app.api_key;
    data.kind = "OCR_DATA";
    app.xhr({
        load: false,
        svc: "open_data",
        msg: "save",
        data: data,
        success: function (data) {
            if (data.response.data === "success") {
                jse.stopLoad();
                app.msg("Data saved to cloud successfully");
            }
        },
        error: function () {
            jse.stopLoad();
            app.msg("There was a problem saving to the cloud. Please check your internet connection");
        }
    });  
};

App.prototype.msg = function(content){
    app.briefShow({
        title : "Info",
        content : content,
        delay : 3000
    });
}

window.app = new App();