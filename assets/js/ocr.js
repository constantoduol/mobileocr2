function App() {
    this.resizeables = [];
    window.onresize = App.prototype.doResize;
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
    console.log(content);
    content = decodeURIComponent(content);
    console.log(content);
    $("#text_container").html(content);
};

App.prototype.loadImages = function (imgs) {
    $("#img_container").html(imgs);
};

App.prototype.startLoad = function () {
    $("#img_container").html("<img src='img/loader.gif' id='loader'>");
    app.resize({
        id: "loader",
        width: {on: "width", factor: 0.15},
        height: {on: "width", factor: 0.15}
    });
};

App.prototype.stopLoad = function () {
    $("#img_container").html("");
};

App.prototype.loadUserAgree = function () {
    $("#img_container").append("<hr>");
    var img = $("<img src='img/cancel.png' id='user_cancel' class='round_icon' style='background-color:red;position:absolute'>");
    img.click(function(){
        jse.setSafeToTakePicture('true');
    });
    $("#img_container").append(img);
    app.resize({
        id: "user_cancel",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        top :{on:"height",factor : 0.78}
    });
    
    var img = $("<img src='img/proceed.png' id='user_proceed' class='round_icon blink' style='position:absolute'>");
    img.click(function () {
        //jse.setSafeToTakePicture('true');
    });
    $("#img_container").append(img);
    app.resize({
        id: "user_proceed",
        width: {on: "width", factor: 0.25},
        height: {on: "width", factor: 0.25},
        top :{on:"height",factor : 0.78},
        left : {on :"width",factor:0.7}
    });
};

window.app = new App();