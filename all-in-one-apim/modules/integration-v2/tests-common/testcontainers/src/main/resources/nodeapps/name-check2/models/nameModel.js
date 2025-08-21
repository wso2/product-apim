class NameModel {
    constructor() {
        this.name = "lasitha";
        this.fileName = "File 2";
    }

    getMessage() {
        return `Hello ${this.name} from ${this.fileName}`;
    }
}

module.exports = NameModel;
