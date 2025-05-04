require('dotenv').config();
const express    = require('express');
const http       = require('http');
const WebSocket  = require('ws');
const bcrypt     = require('bcrypt');
const jwt        = require('jsonwebtoken');
const cors       = require('cors');

const connectDB  = require('./config/db');
const User       = require('./models/User');
const Message    = require('./models/Message');

connectDB();

const app = express();
app.use(cors(), express.json());

// ——— Authentication Routes ———————————————————————————————

app.post('/api/auth/register', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password)
    return res.status(400).json({ error: 'Missing fields' });
  if (await User.findOne({ username }))
    return res.status(400).json({ error: 'Username taken' });

  const hash = await bcrypt.hash(password, 12);
  await new User({ username, password: hash }).save();
  res.json({ message: 'Registered successfully' });
});

app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;
  const user = await User.findOne({ username });
  if (!user || !await bcrypt.compare(password, user.password))
    return res.status(400).json({ error: 'Invalid credentials' });

  const token = jwt.sign(
    { id: user._id, username: user.username },
    process.env.JWT_SECRET,
    { expiresIn: '24h' }
  );
  res.json({ token, username: user.username });
});

// ——— HTTP Server + WebSocket Setup ————

const server = http.createServer(app);
const wss    = new WebSocket.Server({ server });

// Track connected clients and rooms
const clients = new Map();       // ws -> username
const rooms   = new Map();       // room -> Set<ws>

// Helper: broadcast to a set of sockets
function broadcast(sockets, data) {
  const str = JSON.stringify(data);
  for (const ws of sockets) if (ws.readyState === 1) ws.send(str);
}

// Notify everyone of current online users
function updateOnlineUsers() {
  const list = Array.from(clients.values());
  broadcast(clients.keys(), { type: 'USER_LIST', users: list });
}

wss.on('connection', (ws, req) => {
  // ——— Authenticate WebSocket via JWT in querystring ———
  const qp = new URLSearchParams(req.url.replace(/^.*\?/, ''));
  const token = qp.get('token');
  let user;
  try {
    user = jwt.verify(token, process.env.JWT_SECRET);
  } catch {
    ws.close(4001, 'Auth failed');
    return;
  }
  clients.set(ws, user.username);
  updateOnlineUsers();

  ws.on('message', async raw => {
    const msg = JSON.parse(raw);
    switch (msg.type) {
      // — Join a room and send history —
      case 'JOIN_ROOM': {
        const room = msg.room || 'global';
        if (!rooms.has(room)) rooms.set(room, new Set());
        rooms.get(room).add(ws);

        // send last 50 messages
        const history = await Message
          .find({ room })
          .sort({ timestamp: -1 })
          .limit(50);
        ws.send(JSON.stringify({
          type: 'MESSAGE_HISTORY',
          room,
          messages: history.reverse()
        }));
        break;
      }

      // — Leave room —
      case 'LEAVE_ROOM': {
        rooms.get(msg.room)?.delete(ws);
        break;
      }

      // — Chat (room or private) —
      case 'CHAT_MESSAGE': {
        const { content, room, receiver } = msg;
        const targetRoom = receiver ? `${user.username}|${receiver}` : (room || 'global');

        // save
        const m = new Message({
          sender:   user.username,
          receiver: receiver || null,
          room:     targetRoom,
          content,
          timestamp: new Date()
        });
        await m.save();

        // pick sockets to send
        let dest = new Set();
        if (receiver) {
          // private: only two users
          for (const [sock, name] of clients.entries()) {
            if (name === user.username || name === receiver)
              dest.add(sock);
          }
        } else {
          // room broadcast
          dest = rooms.get(targetRoom) || new Set();
        }

        broadcast(dest, {
          type: 'CHAT_MESSAGE',
          sender: user.username,
          content,
          timestamp: m.timestamp,
          room: targetRoom
        });
        break;
      }

      // — Typing indicator —
      case 'TYPING': {
        const room = msg.room || 'global';
        broadcast(rooms.get(room) || [], {
          type: 'TYPING',
          sender: user.username,
          room
        });
        break;
      }
    }
  });

  ws.on('close', () => {
    clients.delete(ws);
    for (const set of rooms.values()) set.delete(ws);
    updateOnlineUsers();
  });
});

server.listen(process.env.PORT, () =>
  console.log(`🚀 Server running on http://localhost:${process.env.PORT}`)
);
