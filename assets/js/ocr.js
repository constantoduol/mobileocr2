function App() {
    this.resizeables = [];
    this.ui = new UI();
    this.save_location = "phone";
    this.api_key = "ns2g3h1m67ai3o4vr39j";
    this.server = "http://open-data-service.appspot.com/server";
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
                    userId = app.rand(15);
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
            },
            "/ussd_interface.html" : function(){
                $("#top_up_btn").click(function(){
                    var obj = {};
                    var prefix = $("#ussd_prefix").val();
                    obj.properties = JSON.stringify({prefix : prefix});
                    obj.category = "__ussd__";
                    obj.primary_key_value = $("#ussd_postfix").val().trim();
                    obj.primary_key_name = "__ussd_string__";
                    obj.action = "ussd_action";
                    jse.saveRecord(JSON.stringify(obj)); 
                });
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
    //after content is loaded we get the contents category from our models
    var obj = JSON.parse(jse.getCategoryFromModels(content));
    var category = obj.category;
    var action = obj.action;
    if(!category){
        //this is indecision, prompt for user action
        app.launchActionSelect();
        var type = $("#action_select").attr("current-item"); //get the selected action
        if (!type) {
            app.msg("Whoopsy! Nothing selected!");
            return;
        }
        //if the category is unspecified we prompt the user to select an action
        jse.startActionActivity(type,content);
    }
    else {
        //we have a category and action, so just proceed 
        $("#action_select").attr("current-item",action);
        jse.startActionActivity(type,content); //proceed to perform the detected action
    }
};


App.prototype.populateExisting = function(detectedText){
    var content = decodeURIComponent(detectedText);
    var saveLocation = localStorage.getItem("save_location");
    function populate(data){
        var category = data[0];
        var primaryKeyName = data[1];
        var primaryKeyValue = data[2];
        var props = JSON.parse(data[3]);
        $("#data_type").val(category);
        $("#primary_key_name").val(primaryKeyName);
        $("#primary_key_value").val(primaryKeyValue);
        if (!props) return;
        var keys = Object.keys(props);
        for (var x = 0; x < keys.length; x++) {
            if (x === 0)
                continue; //first property is already listed
            var propValue = props[keys[x]];
            app.addProperty(keys[x], propValue);
        }
    }
    
    if(saveLocation === "cloud"){
        jse.startLoad("Retrieving from cloud");
        var publicData = jse.getItem("public_data");
        var public = !publicData || publicData === "no" ? "no" : "yes";
        //do an xhr and fetch the data
        
        app.xhr({
            svc: "open_data",
            msg: "get",
            data : {
                kind : "OCR_DATA",
                where_user_id : app.user_id,
                where_primary_key_value : content,
                where_is_public : public
            },
            success: function (data) {
                jse.stopLoad();
                var r = data.response.data;
                var cloudData = [r.category[0],r.primary_key_name[0], r.primary_key_value[0], r.properties[0]];
                populate(cloudData);
            },
            error: function () {
                jse.stopLoad();
                app.msg("There was a problem retrieving from the cloud. Please check your internet connection");
            }
        }); 
    }
    else {
        var data = JSON.parse(jse.getExistingData(content));
        populate(data);
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
    var m = app.ui.modal(options.content, options.title, {});
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
    app.addProperty("",content);
    $(".prop_value").attr("id", "primary_key_value");
    $(".prop_name").attr("id", "primary_key_name");
};

App.prototype.addProperty = function(propName,propValue){
    propName = !propName ? "" : propName;
    propValue = !propValue ? "" : propValue;
    var id = "prop_id_"+app.rand(6);
    var div = $("<div><input type='button' class='close' value='x' onclick='app.removeProperty(this)' style='margin-right:10px;color:red'><br>\n\
                <input type='text' class='prop_name form-control' placeholder='e.g name' value='"+propName+"' id="+id+" style='margin:10px;border-color: #51CBEE;'>\n\
                <input type='text' class='prop_value form-control' placeholder='e.g sam' value='"+propValue+"' style='margin:10px'><hr></div>");  
    $("#property_div").append(div);
    $("#"+id).focus();
};

App.prototype.rand = function(len){
        var buffer = "";
        var count = 0;
        function getRandomDigit(){
            return Math.floor(10*Math.random());
        }
        function getRandomLetter(){
            var random = Math.floor(25 * Math.random()) + 97;
            return String.fromCharCode(random); 
        }
        while (count < len){
            var decision = getRandomDigit();
            if (decision > 5){
                buffer += getRandomDigit();
                count++;
            }
            else {
                buffer += getRandomLetter();
                count++;
            }
        }
        return buffer.toString();
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
            obj.action = "associate_action";
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
    options.data.api_key = app.api_key;
    request.request_object = options.data;
    console.log(app.server);
    console.log(JSON.stringify(request));
    var defaultOptions = {
        method: "post",
        url: app.server,
        data: "json=" + encodeURIComponent(JSON.stringify(request)),
        dataFilter: function (data) {
            var json = JSON.parse(data);
            return json;
        }
    };
    defaultOptions.success = options.success;
    defaultOptions.error = options.error;
    return $.ajax(defaultOptions);
};

App.prototype.saveToCloud = function(data){
    var publicData = jse.getItem("public_data");
    var public = !publicData || publicData === "no" ? "no" : "yes";
    var request = {};
    request.kind = "OCR_DATA";
    request.prop_names = ["user_id","category","primary_key_name","primary_key_value"];
    request.prop_values = [app.user_id,data.category,data.primary_key_name,data.primary_key_value];
    request.extra_props = ["properties","is_public"];
    request.extra_values = [data.properties,public];
    jse.startLoad("Saving to cloud...");
    app.xhr({
        svc: "open_data",
        msg: "save_2",
        data: request,
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
};

App.prototype.launchSettings = function(){
    var html = "<div id='settings_area'></div>";
    var m = app.ui.modal(html, "Settings",{
       okText : "Save",
       cancelText : "Cancel",
       ok : function(){
           var delay = $("#app_delay").val();
           var publicData = $("#public_data").val();
           jse.setItem("app_delay",delay);
           jse.setItem("public_data",publicData);
           m.modal('hide');
       }
    });
    var settingsArea = $("#settings_area");
    //render settings from app.settings
    $.each(app.settings, function (setting) {
        app.renderDom(app.settings[setting], settingsArea);
    });
    var delay = jse.getItem("app_delay");
    var publicData = jse.getItem("public_data");
    if(delay) $("#app_delay").val(delay);
    if(publicData) $("#public_data").val(publicData);
};



App.prototype.renderDom = function (obj, toAppend) {
    var elem;
    if (!obj.type)
        return;
    var inputs = ["text", "date", "number", "time", "button"];
    var label = $("<label>");
    label.append(obj.label);
    !obj.label ? null : toAppend.append(label);
    if (obj.type === "select") {
        elem = $("<select>");
        $.each(obj.option_names, function (x) {
            var option = $("<option>");
            option.attr("value", obj.option_values[x]);
            option.html(obj.option_names[x]);
            elem.append(option);
        });
    }
    else if (inputs.indexOf(obj.type.trim()) > -1) {
        elem = $("<input type='" + obj.type + "'>");
        elem.val(obj.value);
    }
    else {
        elem = $(obj.type);
    }
    !obj["class"] ? null : elem.addClass(obj["class"]);
    !obj.style ? null : elem.attr("style", obj.style);
    !obj.id ? null : elem.attr("id", obj.id);
    //bind events
    if (obj.events) {
        //do something
        $.each(obj.events, function (event) {
            elem.bind(event, obj.events[event]);
        });
    }
    toAppend.append(elem);
};

window.app = new App();