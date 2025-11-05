package chatapp;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Advanced Chat Server with:
 * - Login/Registration (uses UserDatabase)
 * - Timestamps
 * - Online user list
 * - Private messages (/pm)
 * - Rooms (/join, /leave, /rooms). Default: lobby
 * - Per-room chat history (persisted to files)
 * - /help, /users, /history, /logout
 */
public class ChatServer {

    private static final int PORT = 5000;
    private static final String DEFAULT_ROOM = "lobby";
    private static final int HISTORY_LINES_ON_JOIN = 20;

    // online users -> their handler (for PMs)
    private static final ConcurrentMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // room name -> set of client handlers
    private static final ConcurrentMap<String, CopyOnWriteArraySet<ClientHandler>> rooms = new ConcurrentHashMap<>();

    // timestamp formatter
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        System.out.println("Server started on port " + PORT + " …");
        // ensure default room exists
        rooms.putIfAbsent(DEFAULT_ROOM, new CopyOnWriteArraySet<>());

        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "ClientHandler-" + socket.getPort()).start();
            }
        }
    }

    // ========== Helpers for rooms & broadcasting ==========

    static void joinRoom(String room, ClientHandler client) {
        rooms.putIfAbsent(room, new CopyOnWriteArraySet<>());
        rooms.get(room).add(client);
    }

    static void leaveRoom(String room, ClientHandler client) {
        Set<ClientHandler> set = rooms.get(room);
        if (set != null) set.remove(client);
    }

    static void broadcastToRoom(String room, String msg, boolean persist) {
        Set<ClientHandler> set = rooms.get(room);
        if (set == null) return;
        for (ClientHandler c : set) c.send(msg);
        if (persist) appendHistory(room, msg);
    }

    static void appendHistory(String room, String line) {
        File f = new File("history_" + room + ".txt");
        try (FileWriter fw = new FileWriter(f, true)) {
            fw.write(line + System.lineSeparator());
        } catch (IOException ignored) {}
    }

    static List<String> readLastHistory(String room, int maxLines) {
        File f = new File("history_" + room + ".txt");
        if (!f.exists()) return Collections.emptyList();
        Deque<String> dq = new ArrayDeque<>(maxLines + 1);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (dq.size() == maxLines) dq.removeFirst();
                dq.addLast(line);
            }
        } catch (IOException ignored) {}
        return new ArrayList<>(dq);
    }

    static String ts() {
        return LocalDateTime.now().format(TS);
    }

    // ========== Client Handler ==========

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        private String username;
        private String room = DEFAULT_ROOM;
        private volatile boolean running = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // ===== On connect: login or register =====
                out.println("Welcome to the Chat Server!");
                out.println("1. Login");
                out.println("2. Register");
                out.println("Enter choice:");

                String choice = in.readLine();
                if ("1".equals(choice)) {
                    loginFlow();
                } else if ("2".equals(choice)) {
                    registerFlow();
                } else {
                    out.println("Invalid choice. Bye!");
                    close();
                    return;
                }

                // prevent duplicate logins
                if (onlineUsers.putIfAbsent(username, this) != null) {
                    out.println("This user is already logged in elsewhere. Disconnecting.");
                    close();
                    return;
                }

                // join default room
                joinRoom(room, this);

                // greet + show help + show recent history
                send("✅ [" + ts() + "] Logged in as " + username);
                send("Type /help for commands. You are in room: #" + room);
                List<String> recent = readLastHistory(room, HISTORY_LINES_ON_JOIN);
                if (!recent.isEmpty()) {
                    send("---- Last " + recent.size() + " messages (#" + room + ") ----");
                    for (String line : recent) send(line);
                    send("---------------------------------------------");
                }

                broadcastToRoom(room, "📢 [" + ts() + "] " + username + " joined #" + room, true);

                // ===== Main read loop =====
                String line;
                while (running && (line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    if (line.startsWith("/")) {
                        handleCommand(line);
                    } else {
                        String formatted = "[" + ts() + "] " + username + ": " + line;
                        broadcastToRoom(room, formatted, true);
                    }
                }

            } catch (Exception ignored) {
            } finally {
                shutdownCleanup();
            }
        }

        private void handleCommand(String line) {
            String[] parts = line.trim().split("\\s+", 3);
            String cmd = parts[0].toLowerCase(Locale.ROOT);

            switch (cmd) {
                case "/help":
                    send("""
                        Commands:
                        /help                     - Show this help
                        /users                    - Show online users
                        /rooms                    - List rooms
                        /join <room>              - Join/create room
                        /leave                    - Leave current room (go to #lobby)
                        /pm <user> <message>      - Private message
                        /history [n]              - Show last n lines of this room (default 20)
                        /logout                   - Logout
                        """);
                    break;

                case "/users":
                    send("Online users (" + onlineUsers.size() + "): " + String.join(", ", onlineUsers.keySet()));
                    break;

                case "/rooms":
                    send("Rooms: " + String.join(", ", rooms.keySet()));
                    break;

                case "/join":
                    if (parts.length < 2) {
                        send("Usage: /join <room>");
                        return;
                    }
                    String newRoom = parts[1].toLowerCase(Locale.ROOT);
                    if (newRoom.equals(room)) {
                        send("You are already in #" + room);
                        return;
                    }
                    leaveRoom(room, this);
                    broadcastToRoom(room, "📢 [" + ts() + "] " + username + " left #" + room, true);

                    room = newRoom;
                    joinRoom(room, this);
                    send("✅ Joined room #" + room);

                    // show last history
                    List<String> recent = readLastHistory(room, HISTORY_LINES_ON_JOIN);
                    if (!recent.isEmpty()) {
                        send("---- Last " + recent.size() + " messages (#" + room + ") ----");
                        for (String l : recent) send(l);
                        send("---------------------------------------------");
                    }
                    broadcastToRoom(room, "📢 [" + ts() + "] " + username + " joined #" + room, true);
                    break;

                case "/leave":
                    if (!room.equals(DEFAULT_ROOM)) {
                        leaveRoom(room, this);
                        broadcastToRoom(room, "📢 [" + ts() + "] " + username + " left #" + room, true);
                        room = DEFAULT_ROOM;
                        joinRoom(room, this);
                        send("✅ Returned to #" + room);
                    } else {
                        send("You are already in #" + room);
                    }
                    break;

                case "/pm":
                    if (parts.length < 3) {
                        send("Usage: /pm <username> <message>");
                        return;
                    }
                    String target = parts[1];
                    String pm = parts[2];
                    ClientHandler other = onlineUsers.get(target);
                    if (other == null) {
                        send("User '" + target + "' is not online.");
                    } else {
                        String formatted = "💬 [" + ts() + "] (PM) " + username + " → " + target + ": " + pm;
                        other.send(formatted);
                        send(formatted);
                    }
                    break;

                case "/history":
                    int n = HISTORY_LINES_ON_JOIN;
                    if (parts.length >= 2) {
                        try { n = Math.max(1, Integer.parseInt(parts[1])); } catch (NumberFormatException ignored) {}
                    }
                    List<String> hist = readLastHistory(room, n);
                    if (hist.isEmpty()) {
                        send("No history for #" + room);
                    } else {
                        send("---- Last " + hist.size() + " messages (#" + room + ") ----");
                        for (String l : hist) send(l);
                        send("---------------------------------------------");
                    }
                    break;

                case "/logout":
                    send("Logging out… Bye!");
                    close();
                    break;

                default:
                    send("Unknown command. Type /help");
            }
        }

        // ===== Login / Register =====

        private void loginFlow() throws IOException {
            while (true) {
                out.println("Enter username:");
                String u = in.readLine();
                out.println("Enter password:");
                String p = in.readLine();

                if (UserDatabase.loginUser(u, p)) {
                    this.username = u;
                    return;
                } else {
                    out.println("❌ Wrong username or password. Try again.");
                }
            }
        }

        private void registerFlow() throws IOException {
            while (true) {
                out.println("Choose username:");
                String u = in.readLine();
                out.println("Choose password:");
                String p = in.readLine();

                if (UserDatabase.registerUser(u, p)) {
                    out.println("✅ Registered successfully!");
                    this.username = u;
                    return;
                } else {
                    out.println("❌ Username already exists. Try again.");
                }
            }
        }

        // ===== I/O helpers & cleanup =====

        void send(String msg) {
            out.println(msg);
        }

        void close() {
            running = false;
            try { socket.close(); } catch (IOException ignored) {}
        }

        void shutdownCleanup() {
            // remove from online map
            if (username != null) {
                onlineUsers.remove(username, this);
                // announce leave to room
                broadcastToRoom(room, "📢 [" + ts() + "] " + username + " disconnected", true);
            }
            // remove from room
            leaveRoom(room, this);
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            if (out != null) out.close();
        }
    }
}
