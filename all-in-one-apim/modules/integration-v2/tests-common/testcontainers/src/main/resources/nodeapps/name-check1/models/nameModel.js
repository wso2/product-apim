class NameModel {
    constructor() {
        this.name = "WSO2";
        this.fileName = "File 1";
    }

    getMessage() {
        return `Hello ${this.name} from ${this.fileName}`;
    }
}

module.exports = NameModel;
