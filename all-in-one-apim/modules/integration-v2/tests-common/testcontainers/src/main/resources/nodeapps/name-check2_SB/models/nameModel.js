class NameModel {
    constructor() {
        this.name = "WSO2";
        this.fileName = "File 2_Sandbox";
    }

    getMessage() {
        return `Hello ${this.name} from ${this.fileName}`;
    }
}

module.exports = NameModel;
