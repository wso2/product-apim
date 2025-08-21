const Order = require('../models/order');

let orders = new Map();

// Initialize with one order
const init = () => {
    const order = new Order();
    order.setId(223);
    order.setDescription("order 223");
    orders.set(order.getId(), order);
};

init();

// GET /customerservice/orders/:orderId
const getOrder = (req, res) => {
  const id = parseInt(req.params.orderId);
  console.log(`----invoking getOrder, Order id is: ${id}`);
  const order = orders.get(id);

  if (order) {
    res.json(order);
  } else {
    res.status(404).send('Order not found');
  }
};

module.exports = {
  getOrder
};
