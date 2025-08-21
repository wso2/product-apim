class Product {
    constructor(id = null, description = '') {
        this.id = id;
        this.description = description;
    }

    getId() {
        return this.id;
    }

    setId(id) {
        this.id = id;
    }

    getDescription() {
        return this.description;
    }

    setDescription(description) {
        this.description = description;
    }

    toJSON() {
        return {
            id: this.id,
            description: this.description
        };
    }
}

module.exports = Product;