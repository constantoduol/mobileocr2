var animate = {
    timeoutData: {},
    animData: {},
    start: function (id, dx, delay, max, direction, color) {
        var area = $("#" + id);
        area.addClass("circle");
        area.addClass("motion");
        animate.animData[id] = {dx: dx, delay: delay, max: max, direction: direction, color: color};
        function runAnimation() {
            var currentLeft = parseFloat(area[0].style.left);
            area[0].style.background = animate.animData[id].color;
            currentLeft = isNaN(currentLeft) ? 0 : currentLeft;
            if (currentLeft > animate.animData[id].max) {
                animate.animData[id].direction = 1;
            }
            if (currentLeft <= 0) {
                animate.animData[id].direction = 0;
            }
            if (animate.animData[id].direction === 0) {
                currentLeft = currentLeft + animate.animData[id].dx;
            }
            else if (animate.animData[id].direction === 1) {
                currentLeft = currentLeft - animate.animData[id].dx;
            }
            area[0].style.left = currentLeft + "px";
            animate.timeoutData[id] = animate.runLater(animate.animData[id].delay, runAnimation);
        }
        animate.timeoutData[id] = animate.runLater(animate.animData[id].delay, runAnimation);
    },
    stop: function (id) {
        clearTimeout(animate.timeoutData[id]);
        var area = $("#" + id);
        area.removeClass("circle");
        area.removeClass("motion");
        delete animate.timeoutData[id];
    },
    runLater: function (limit, func) {
        return setTimeout(func, limit);
    },
    stopAll: function () {
        for (var id in animate.timeoutData) {
            var timeout = animate.timeoutData[id];
            clearTimeout(timeout);
            var area = $("#" + id);
            area.removeClass("circle");
            area.removeClass("motion");
        }
        animate.timeoutData = {};
    }

};
