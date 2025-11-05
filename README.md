# RealTimeChatApp (Console-Based Java Chat Application)

This is a real-time multi-user chat application written in Java using sockets and multithreading.  
It runs completely in the console (no GUI).  
Features include user login, chat rooms, private messaging, message history, timestamps, and multi-client support.

---

## Features

### User System
- Register new users
- Login existing users
- Prevent duplicate usernames
- Prevent logging in with the same user twice
- Credentials stored in `users.txt`

### Chat Features
- Real-time text messaging between multiple clients
- Messages broadcast inside the current room
- Timestamp included in every message

### Private Messaging
**Command:**
```
/pm <username> <message>
```

### Chat Rooms
**Commands:**
```
/join <room>    Join or create a room
/rooms          List all rooms
/leave          Return to lobby
```

### Message History
Each room has its own history file: `history_<room>.txt`

**Commands:**
```
/history           Show last 20 messages
/history <n>       Show last n messages
```

### Online Users
```
/users
```

### Help
```
/help
```

### Logout
```
/logout
```

---

## Project Structure

```
RealTimeChatApp/
├── README.md
├── users.txt               # auto-created
├── history_lobby.txt       # auto-created
└── src/
    └── chatapp/
        ├── ChatServer.java
        ├── ChatClient.java
        ├── User.java
        └── UserDatabase.java
```

All Java files must be inside `src/chatapp/`.

Each file must begin with:
```
package chatapp;
```

---

## Requirements
- Java JDK 17 or newer
- Terminal / PowerShell / Command Prompt or VS Code

Check Java version:
```
java -version
```

---

## How to Compile and Run

### 1. Open a terminal inside the src folder:
```bash
cd /path/to/RealTimeChatApp/src
```

### 2. Compile the project:
```bash
javac chatapp/*.java
```

### 3. Run the Server (keep this terminal open):
```bash
java chatapp.ChatServer
```
You should see:
```
Server started on port 5000 ...
```

### 4. Start a Client (open another terminal):
```bash
cd /path/to/RealTimeChatApp/src
java chatapp.ChatClient
```

Follow on-screen instructions:
- Login
- Register

Each client must run in a separate terminal window.

---

## Client Commands

- `/help` Show all commands
- `/users` List online users
- `/rooms` List available rooms
- `/join <room>` Join or create a room
- `/leave` Go back to lobby
- `/pm <user> <message>` Private message
- `/history` Last 20 messages
- `/history <n>` Last n messages
- `/logout` Logout and disconnect

Normal text (not starting with "/") is sent as a message to the entire room.

---

## Example Session

### Terminal 1 (Server)
```bash
java chatapp.ChatServer
Server started on port 5000 ...
```

### Terminal 2 (Client: Alice)
```bash
java chatapp.ChatClient
2
alice
alice123
/join study
```

### Terminal 3 (Client: Bob)
```bash
java chatapp.ChatClient
2
bob
bob123
/join study
alice: hello
/pm alice Hi Alice
```

---

## Runtime Files Created

### users.txt
**Stores:**
```
username,password
```

### history_<room>.txt
**Stores:**
```
[timestamp] username: message
```

These files appear in the project root.

---

## Troubleshooting

### Server will not start (port is busy)
Change port number in `ChatServer.java`.

### Clients cannot connect
Start server first, then run clients.

### Compilation errors
Ensure:
- Files are in `src/chatapp/`
- Each file starts with: `package chatapp;`

### History or users not saving
Ensure the server has write permission in the project folder.

---

## License
This project is open-source. You may modify or distribute it freely.
