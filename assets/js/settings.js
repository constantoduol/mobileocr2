
App.prototype.settings = {  
    app_delay: {
        type : "select",
        id : "app_delay",
        option_names : ["None","Short","Long"],
        option_values : ["0","5","10"],
        required : true,
        selected : "5",
        label : "Application delay",
        class : "form-control"
    },
    hr : {
        type : "<hr>"
    },
    public_data: {
        type: "select",
        id : "public_data",
        option_names: ["Yes", "No"],
        option_values: ["yes", "no"],
        required : true,
        selected : "no",
        label : "Make data public on cloud",
        class : "form-control"
    }
};
