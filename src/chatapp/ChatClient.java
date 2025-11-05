package chatapp;

import java.io.*;
import java.net.*;

public class ChatClient {

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost", 5000);
        BufferedReader server = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // Thread for reading server messages
        new Thread(() -> {
            try {
                String msg;
                while ((msg = server.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception e) {}
        }).start();

        // Sending messages to server
        while (true) {
            String input = userInput.readLine();
            out.println(input);
        }
    }
}
