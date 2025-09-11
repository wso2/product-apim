const { v4: uuidv4 } = require('uuid'); // Using uuid library for random IDs

class Order {
    constructor(drinkName = '', additions = '') {
        this.orderId = uuidv4();
        this.drinkName = drinkName;
        this.additions = additions;
        this.cost = 0.0;
        this.locked = false;
        this.timestamp = new Date().toISOString();
    }
}

module.exports = Order;