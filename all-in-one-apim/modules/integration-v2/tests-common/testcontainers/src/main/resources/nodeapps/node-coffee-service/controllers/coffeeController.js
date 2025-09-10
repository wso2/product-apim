// controllers/coffee.controller.js
const Order = require('../models/Order');
const Payment = require('../models/Payment');
const PaymentStatus = require('../models/PaymentStatus');

// In-memory data stores
const ordersList = new Map();
const paymentRegister = new Map();
const priceList = new Map();

// --- Helper Functions ---
const getPrice = (item, isAddition) => {
    if (!priceList.has(item)) {
        const price = isAddition ? Math.random() * 5 : Math.floor(Math.random() * 7) + 2;
        priceList.set(item, parseFloat(price.toFixed(2)));
    }
    return priceList.get(item);
};

const calculateCost = (drinkName, additions) => {
    let cost = getPrice(drinkName, false);
    if (additions) {
        cost += getPrice(additions, true);
    }
    return parseFloat(cost.toFixed(2));
};

// --- Initialization ---
const init = () => {
    if (ordersList.size > 0) return;
    let order1 = new Order('Vanilla Flavored Coffee', 'Milk');
    order1.orderId = '123';
    order1.cost = calculateCost(order1.drinkName, order1.additions);
    ordersList.set(order1.orderId, order1);

    let order2 = new Order('Chocolate Flavored Coffee');
    order2.orderId = '444';
    order2.cost = calculateCost(order2.drinkName, order2.additions);
    ordersList.set(order2.orderId, order2);
};
init();

// --- Controller Logic ---

const addOrder = (req, res) => {
    const { drinkName, additions } = req.body;
    const newOrder = new Order(drinkName, additions);
    newOrder.cost = calculateCost(drinkName, additions);
    ordersList.set(newOrder.orderId, newOrder);
    res.status(200).json(newOrder);
};

const getOrder = (req, res) => {
    const order = ordersList.get(req.params.orderId);
    if (order) {
        res.status(200).json(order);
    } else {
        res.status(404).json({ message: "Order not found" });
    }
};

const updateOrder = (req, res) => {
    const { orderId, drinkName, additions } = req.body;
    const order = ordersList.get(orderId);

    if (!order) {
        return res.status(404).json({ message: "Order not found" });
    }
    if (order.locked) {
        return res.status(304).send();
    }

    order.drinkName = drinkName || order.drinkName;
    order.additions = additions || order.additions;
    order.cost = calculateCost(order.drinkName, order.additions);
    ordersList.set(orderId, order);
    res.status(200).json(order);
};

const getPendingOrders = (req, res) => {
    const pending = Array.from(ordersList.values()).filter(order => !order.locked);
    res.status(200).json(pending);
};

const lockOrder = (req, res) => {
    const order = ordersList.get(req.params.orderId);
    if (order) {
        order.locked = true;
        res.status(200).json(order);
    } else {
        res.status(304).send();
    }
};

const removeOrder = (req, res) => {
    const { orderId } = req.params;
    if (ordersList.has(orderId)) {
        ordersList.delete(orderId);
        paymentRegister.delete(orderId);
        res.status(200).send("true");
    } else {
        res.status(304).send();
    }
};

const doPayment = (req, res) => {
    const { orderId } = req.params;
    const { name, cardNumber, expiryDate, amount } = req.body;

    if (paymentRegister.has(orderId)) {
        const status = new PaymentStatus("Duplicate Payment", paymentRegister.get(orderId));
        return res.status(304).json(status);
    }
    const order = ordersList.get(orderId);
    if (!order) {
        return res.status(304).json(new PaymentStatus("Invalid Order ID"));
    }
    if (amount < order.cost) {
        return res.status(304).json(new PaymentStatus("Insufficient Funds"));
    }

    const newPayment = new Payment(orderId, name, cardNumber, expiryDate, amount);
    paymentRegister.set(orderId, newPayment);
    const status = new PaymentStatus("Payment Accepted", newPayment);
    res.status(200).json(status);
};

const getPayment = (req, res) => {
    const payment = paymentRegister.get(req.params.orderId);
    if (payment) {
        res.status(200).json(payment);
    } else {
        res.status(404).json({ message: "Payment not found" });
    }
};

// --- Export all controller functions ---
module.exports = {
    addOrder,
    getOrder,
    updateOrder,
    getPendingOrders,
    lockOrder,
    removeOrder,
    doPayment,
    getPayment
};