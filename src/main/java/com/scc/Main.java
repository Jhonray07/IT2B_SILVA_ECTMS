package com.scc;

import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;

import java.util.Scanner;

public class Main{

    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();


    public static void main(){

        while (true){
            System.out.println("\n========================================");
            System.out.println("    EVENT & CONFERENCE TICKETING SYSTEM");
            System.out.println("========================================");
            System.out.println("[1] Login");
            System.out.println("[2] Register");
            System.out.println("[3] Exit");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    login();
                    break;
                case "2":
                    register();
                    break;
                case "3":
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public static void login(){
        System.out.println("\n    ======= LOG IN =======");

        String username, password;
        while (true){
            System.out.print("Enter Username: ");
            username = sc.nextLine();
            if(username.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else {
                break;
            }
        }

        while (true){
            System.out.print("Enter Password: ");
            password = sc.nextLine();
            if(password.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else{
                break;
            }
        }

        User userLoggedIn = userDao.login(username, password);

        if(userLoggedIn != null){
            switch (userLoggedIn.user_role()){
                case "ADMIN" -> new AdminMenu(sc, userDao).display(userLoggedIn);
            }
        }
    }

    public static void register(){
        System.out.println("\n    ======= REGISTER =======");

        String fullname, username, email, password, role;

        while (true) {
            System.out.print("Enter your fullname: ");
            fullname = sc.nextLine().trim();

            if (fullname.isEmpty()) {
                System.out.println("Invalid! Fullname cannot be empty.");
            } else {
                break;
            }
        }

        while (true) {
            System.out.print("Enter your Email: ");
            email = sc.nextLine().trim().toLowerCase();

            if (!email.endsWith("@gmail.com") || email.length() <= "@gmail.com".length() || email.contains(" ")) {
                System.out.println("Invalid! Enter a valid Gmail without spaces (example: Juan@gmail.com).");
            } else {
                break;
            }
        }

        while (true){
            System.out.print("Enter username: ");
            username = sc.nextLine().trim().toLowerCase();

            if (username.contains(" ")) {
                System.out.println("Invalid!, Please try Again");
            } else if (userDao.usernameExists(username)) {
                System.out.println("Invalid! Username already taken.");
            } else {
                break;
            }
        }

        while (true) {
            System.out.print("Enter Password: ");
            password = sc.nextLine().trim();

            if (password.length() < 5) {
                System.out.println("Invalid! Password must be at least 5 characters.");
                continue;
            }

            boolean confirmed = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.print("Confirm Password: ");
                String confirm = sc.nextLine().trim();

                if (confirm.equals(password)) {
                    confirmed = true;
                    break;
                }
                System.out.println("\nPasswords do not match! Attempt " + attempt + " of 3.");
            }

            if (confirmed) {
                break;
            }
            System.out.println("Too many wrong attempts. Register Again.");
            return;
        }

        while (true) {
            System.out.println("\n-- Select Role --");
            System.out.println("[1] ADMINISTRATOR");
            System.out.println("[2] EVENT ORGANIZER");
            System.out.println("[3] CUSTOMER");
            System.out.print("Enter Choice: ");
            String roleChoice = sc.nextLine().trim();

            if (roleChoice.equals("1")) {
                role = "ADMIN";
                break;
            } else if (roleChoice.equals("2")) {
                role = "ORGANIZER";
                break;
            } else if (roleChoice.equals("3")) {
                role = "CUSTOMER";
                break;
            } else {
                System.out.println("Invalid! Please select 1, 2, or 3 only.");
            }
        }

        userDao.registerUser(fullname, email, username, password, role);
    }
}
