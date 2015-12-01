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
                    app.performUSSD();
                });
            },
            "/search_interface.html" : function(){
                function setUpAuto(){
                    var location = localStorage.getItem("save_location");
                    location = !location ? "phone" : location;
                    app.selectSaveLocation(location);
                    if(location === "phone"){
                        var autocomplete = app.autocomplete_data_local;
                        app.setUpAuto(autocomplete.data_type);
                        app.setUpAuto(autocomplete.primary_key_value);
                    }
                    else {
                        var autocomplete = app.autocomplete_data_cloud;
                        app.setUpAuto(autocomplete.data_type);
                        app.setUpAuto(autocomplete.primary_key_value);
                    }
                }
                
                $("#phone_save").click(function () {
                    app.selectSaveLocation("phone");
                    setUpAuto();
                });
                $("#cloud_save").click(function () {
                    app.selectSaveLocation("cloud");
                    setUpAuto();
                });
                setUpAuto();
            }
        };
        onload.always();
        !onload[path] ? null : onload[path](); 
    });
}

App.prototype.performUSSD = function(){
    var obj = {};
    var prefix = $("#ussd_prefix").val().trim();
    var postfix = $("#ussd_postfix").val().trim();
    if(!prefix){
        app.msg("Please specify a prefix e.g 141");
        return;
    }
    if(!postfix){
        app.msg("Please specify the ussd recharge code");
        return;
    }
    jse.performUSSD(prefix,postfix);
    jse.saveRecordModel(prefix,postfix,"ussd_action"); 
};

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
    var text = content === "!#__blanks__#!" ? "" : content;
    $("#text_container").addClass("black-view");
    $("#text_container").html("<span id='detected_text'>"+text+"</span>");
    var actionSelect = $(
            "<div id='action_select_container'>"+
            "<span id='action_select'>Select Action</span>"+
            "<span style='float:right'><img src='img/down.png' style='width:35px'></span>"+
            "</div>");
    actionSelect.click(function(){
        app.launchActionSelect();
    });
    $("#text_container").append(actionSelect);
    if(content === "!#__blanks__#!") return; //this is just a screen refresh
    //after content is loaded we get the contents category from our models
    
    var obj = JSON.parse(jse.getCategoryFromModels(text));
    var category = obj.category;
    var action = obj.action;
    if(!category){
        //this is indecision, prompt for user action
        app.launchActionSelect(function(){
            var type = $("#action_select").attr("current-item"); //get the selected action
            if (!type) {
                app.msg("Whoopsy! Nothing selected!");
                return;
            }
            //if the category is unspecified we prompt the user to select an action
            jse.startActionActivity(type, text, category); 
        },text);
    }
    else {
        //we have a category and action, so just proceed
        //we need to take our delays into mind and execute them
        var appDelay = jse.getItem("app_delay");
        var delay = !appDelay ? 5 : parseInt(appDelay); //this is the delay we take before going forward
        //we will need to countdown in the proceed button to show that we are going to proceed
        //this gives room for the user to cancel a selected category and action 
        
        var descripAction;
        if(action === "ussd_action"){
            descripAction = "USSD";
        }
        else if(action === "associate_action"){
            descripAction = "Save Record";
        }
        else if(action === "web_search_action"){
            descripAction = "Web Search";
        }
        app.briefShow({
            title : "Detected Values",
            content : "Category : <span>"+category+"</span>\n\
                    <br>Action : <span>"+descripAction+"</span>\n\
                    <br>Detected text : <span>"+text+"</span>",
            delay : 4000
        });
        app.runLater(4000,function(){//start this once we finish displaying the results
            if (delay > 0) {
                var position = {
                    id: "count_area",
                    width: {on: "width", factor: 0.25},
                    height: {on: "width", factor: 0.25},
                    top: {on: "height", factor: 0.79},
                    left: {on: "width", factor: 0.72},
                    "line-height": {on: "width", factor: 0.25}
                };
                app.countDown(delay,"img_container", position,function () {
                    $("#action_select").attr("current-item", action);
                    jse.startActionActivity(action, text, category); //proceed to perform the detected action
                });
            }
            else {
                jse.startActionActivity(action, text, category);
            }
        });
    }
};


App.prototype.countDown = function(delay,append,position,onfinish){
    var div = $("<div id='count_area' class='round_icon'>"+delay+"</div>");
    $("#"+append).append(div);
    app.resize(position);
    var interval = setInterval(function(){
        var currDelay = parseInt($("#count_area").html());
        var newDelay = currDelay === 0 ? 0 : --currDelay;
        $("#count_area").html(newDelay);
        if (newDelay === 0){
            $("#count_area").remove();
            clearInterval(interval);
            if(onfinish) onfinish();
        }
    },1000);
    div.click(function () {
        $("#count_area").remove();
        clearInterval(interval);
    });
};

App.prototype.populateExisting = function(detectedText,category){
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
            if (keys[x] === primaryKeyName) continue; 
            var propValue = props[keys[x]];
            app.addProperty(keys[x], propValue);
        }
    }
    
    if(saveLocation === "cloud"){
        jse.startLoad("Retrieving from cloud");
        //do an xhr and fetch the data
        app.xhr({
            svc: "open_data",
            msg: "get",
            data : {
                kind : "OCR_DATA",
                where_user_id : app.user_id,
                where_primary_key_value : content,
                order : "desc"
            },
            success: function (data) {
                console.log(JSON.stringify(data));
                var r = data.response.data;
                if(!r.category){ //this means this record does not exist yet
                    app.xhr({
                        svc: "open_data",
                        msg: "get",
                        data: {
                            kind: "OCR_DATA",
                            where_user_id: app.user_id,
                            where_category : category,
                            order : "desc"
                        },
                        success: function (data) {
                            console.log(JSON.stringify(data));
                            var d = data.response.data;
                            var props = JSON.parse(d.properties[0]);
                            for(var prop in props){
                                props[prop] = "";
                            }
                            var cloudData = [d.category[0], d.primary_key_name[0],content , JSON.stringify(props)];
                            populate(cloudData);
                        }
                    });
                }
                else { //we have existing data yay!
                    var cloudData = [r.category[0], r.primary_key_name[0], r.primary_key_value[0], r.properties[0]];
                    populate(cloudData);   
                }
                jse.stopLoad();
            },
            error: function () {
                jse.stopLoad();
                app.msg("There was a problem retrieving from the cloud. Please check your internet connection");
            }
        }); 
    }
    else { //we are getting data from the phone
        var data = JSON.parse(jse.getExistingData(content));
        if(data.length === 0){
            var props = JSON.parse(jse.getCategoryProperties(category));
            data = [category,props.primary_key_name,content,props.props];
            populate(data);
            return;
        }
        populate(data);
    }
};

App.prototype.launchActionSelect = function(func,title){
    title = !title ? "Action to take" : title;
    var actionKeys = ["ussd_action", "web_search_action", "associate_action"];
    var actionHtml = ["USSD", "Web Search", "Save Record"];
    app.launchSelect({id: "action_select", title:title, values: actionKeys, html: actionHtml},func);  
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
        if (!type) {
            //well seems we dont have a default action type
            //tell the user this
            app.launchActionSelect();
            return;
        }
        jse.startActionActivity(type, text, "");
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



App.prototype.launchSelect = function (options,func) {
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
            if(func) func();
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

App.prototype.loadUSSDField = function(detectedText,category){
    var content = decodeURIComponent(detectedText);
    $("#ussd_prefix").val(category);
    $("#ussd_postfix").val(content);
    var appDelay = jse.getItem("app_delay");
    var delay = !appDelay ? 5 : parseInt(appDelay); //this is the delay we take before going forward
    var position = {
        id: "count_area",
        width: {on: "width", factor: 0.3},
        height: {on: "width", factor: 0.3},
        top: {on: "height", factor: 0.48},
        left: {on: "width", factor: 0.34},
        "line-height": {on: "width", factor: 0.3}
    };
    app.countDown(delay, "button_area", position,function(){
        app.performUSSD();
    });
    
};

App.prototype.loadRecordField = function(detectedText,category){
    var content = decodeURIComponent(detectedText);
    app.addProperty("",content);
    $(".prop_value").attr("id", "primary_key_value");
    $(".prop_name").attr("id", "primary_key_name");
    app.populateExisting(detectedText,category);
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
    app.scrollTo(id);
};

App.prototype.addFrozenProperty = function(propName,propValue){
    var div = $("<div><label style='font-size:20px'>"+propName+"</label>\n\
                <div style='font-size:16px;'>"+propValue+"</div><hr></div>");
    $("#property_div").append(div);
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
    var request = {};
    request.kind = "OCR_DATA";
    request.prop_names = ["user_id","category","primary_key_name","primary_key_value"];
    request.prop_values = [app.user_id,data.category,data.primary_key_name,data.primary_key_value];
    request.extra_props = ["properties"];
    request.extra_values = [data.properties];
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

App.prototype.searchUI = function(){
    jse.startActionActivity("search_action","", ""); 
};


App.prototype.launchSettings = function(){
    var html = "<div id='settings_area'></div>";
    var m = app.ui.modal(html, "Settings",{
       okText : "Save",
       cancelText : "Cancel",
       ok : function(){
           var delay = $("#app_delay").val();
           jse.setItem("app_delay",delay);
           m.modal('hide');
       }
    });
    var settingsArea = $("#settings_area");
    //render settings from app.settings
    $.each(app.settings, function (setting) {
        app.renderDom(app.settings[setting], settingsArea);
    });
    var delay = jse.getItem("app_delay");
    if(delay) $("#app_delay").val(delay);
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


App.prototype.autocomplete_data_local = {
    data_type: {
        autocomplete: {
            id: "data_type",
            table: "OCR_DATA",
            column: "category,timestamp",
            orderby : "category asc",
            where: function () {
                return "category  LIKE '" + $("#data_type").val() + "%'";
            },
            limit: 10,
            key: "category",
            data: {},
            selected: [],
            after: function (data, index) {
         
            }
        }
    },
    primary_key_value: {
        autocomplete: {
            id: "primary_key_value",
            table: "OCR_DATA",
            column: "primary_key_value,timestamp,properties",
            orderby: "timestamp desc",
            where: function () {
                var category =  $("#data_type").val();
                var extra = "";
                if(category) {
                    extra = " AND category ='"+category+"'" ;//the category is specified
                }
                return "primary_key_value  LIKE '" + $("#primary_key_value").val() + "%'"+extra;
            },
            limit: 10,
            key: "primary_key_value",
            data: {},
            selected: [],
            after: function (data, index) {
                $("#property_div").html("");
                var props = JSON.parse(data.properties[index]);
                for(var prop in props){
                    app.addFrozenProperty(prop,props[prop]);
                }
            }
        }
    }
};


App.prototype.autocomplete_data_cloud = {
    data_type: {
        autocomplete: {
            id: "data_type",
            entity: ["OCR_DATA"],
            where_operators : [">="],
            where_cols : function(){
                return ["category"];
            },
            where_values : function () {
                return [$("#data_type").val()];
            },
            limit: 10,
            key: "category",
            data: {},
            after: function (data, index) {

            }
        }
    },
    primary_key_value: {
        autocomplete: {
            id: "primary_key_value",
            entity: "OCR_DATA",
            where_operators: [">="],
            where_cols: function(){
                var category = $("#data_type").val();
                if (category) {
                    return  ["#primary_key_value", "category"];
                }
                return  ["#primary_key_value"];
            },
            where_values : function () {
                var category = $("#data_type").val();
                if (category) {
                    return  [$("#primary_key_value").val(),category];
                }
                return  [$("#primary_key_value").val()];
            },
            limit: 10,
            key: "primary_key_value",
            data: {},
            selected: [],
            after: function (data, index) {
                $("#property_div").html("");
                var props = JSON.parse(data.properties[index]);
                for (var prop in props) {
                    app.addFrozenProperty(prop, props[prop]);
                }
            }
        }
    }
};

App.prototype.setUpAuto = function (field) {
    var id = field.autocomplete.id;
    $("#" + id).typeahead({
        source: function (query, process) {
            app.autocomplete(field, function (data) {
                field.autocomplete.data = data;
                var arr = [];
                $.each(data[field.autocomplete.key], function (x) {
                    var val = data[field.autocomplete.key][x];
                    if (val)
                        arr.push(val);
                });
                return process(arr);
            });
        },
        minLength: 1,
        updater: function (item) {
            var data = field.autocomplete.data;
            var key = field.autocomplete.key;
            var index = data[key].indexOf(item);
            if (data.ID) {
                $("#" + id).attr("current-item", data.ID[index]);
            }
            else {
                $("#" + id).attr("current-item", item);
            }
            if (field.autocomplete.after)
                field.autocomplete.after(data, index);

            if (field.autocomplete_handler)
                app.defaultAutoHandler(field.autocomplete_handler, data, index);
            return item;
        }
    });
};

App.prototype.defaultAutoHandler = function (autoHandler, data, index) {
    //{id : key, id : key}
    $.each(autoHandler.fields, function (id) {
        var key = autoHandler.fields[id];
        $("#" + id).val(data[key][index]);
    });
    if (autoHandler.after) {
        autoHandler.after(data, index);
    }

};


App.prototype.autocomplete = function (field, func) {
    var auto = field.autocomplete;
    console.log(JSON.stringify(auto));
    var saveLocation = localStorage.getItem("save_location");
    var requestData = saveLocation === "phone" ? 
    {
        table: auto.table,
        column: auto.column,
        where: auto.where(),
        orderby: auto.orderby,
        limit: auto.limit
    } : {
        entity: auto.entity,
        where_cols: auto.where_cols(),
        where_values: auto.where_values(),
        where_operators: auto.where_operators,
        limit: auto.limit
    };
    if(saveLocation === "phone"){
        var data = JSON.parse(jse.search(JSON.stringify(requestData)));
        func(data);   
    }
    else {
        console.log("cloud xhr");
        app.xhr({
            data: requestData,
            svc: "open_data",
            msg: "auto_complete",
            load: false,
            success: function (data) {
                console.log(JSON.stringify(data));
                if (data && data.response.data === "FAIL") {
                    app.showMessage(data.response.reason);
                }
                else {
                    func(data);
                }
            }
        });
    }
};

App.prototype.scrollTo = function (id) {
    // Scroll
    if (!$("#" + id)[0])
        return;
    $('html,body').animate({
        scrollTop: $("#" + id).offset().top},
    'slow');
};

window.app = new App();