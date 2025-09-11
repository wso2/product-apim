const Product = require('./product');

class Order {
    constructor(id = null, description = '') {
        this.id = id;
        this.description = description;
        this.products = new Map();
        this.init();
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

    getProduct(productId) {
        console.log(`----invoking getProduct with id: ${productId}`);
        return this.products.get(parseInt(productId));
    }

    init() {
        const product = new Product();
        product.setId(323);
        product.setDescription("product 323");
        this.products.set(product.getId(), product);
    }

    toJSON() {
        return {
            id: this.id,
            description: this.description,
            products: Array.from(this.products.values()).map(p => p.toJSON())
        };
    }
}

module.exports = Order;