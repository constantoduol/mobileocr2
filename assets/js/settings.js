
App.prototype.settings = {  
    app_delay: {
        type : "select",
        id : "app_delay",
        option_names : ["None","Short","Long","Very Long"],
        option_values : ["0","3","5","10"],
        required : true,
        selected : "3",
        label : "Application delay",
        class : "form-control"
    }
};
