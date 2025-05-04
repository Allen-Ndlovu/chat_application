const mongoose = require('mongoose');

const MessageSchema = new mongoose.Schema({
  sender:   { type: String, required: true },
  receiver: { type: String },            
  room:     { type: String, required: true },
  content:  { type: String, required: true },
  timestamp:{ type: Date,   required: true }
});

module.exports = mongoose.model('Message', MessageSchema);
