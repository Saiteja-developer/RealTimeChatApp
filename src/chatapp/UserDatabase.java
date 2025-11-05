package chatapp;

import java.io.*;
import java.util.*;

public class UserDatabase {
    private static final String FILE_NAME = "users.txt";
    // Register new user
    public static boolean registerUser(String username, String password) {
        if (userExists(username)) {
            return false;
        }

        try (FileWriter fw = new FileWriter(FILE_NAME, true)) {
            fw.write(username + "," + password + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    // Check login
    public static boolean loginUser(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
    // Check if username already exists
    public static boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
}
