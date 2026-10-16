package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userdao;

    public AdminMenu(Scanner sc, UserDao userdao) {
        this.sc = sc;
        this.userdao = userdao;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  EVENT & CONFERENCE TICKETING SYSTEM");
            System.out.println("  ADMIN MENU  | Logged in: " + admin.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Manage Users");
            System.out.println("  [2] Manage Events");
            System.out.println("  [3] View All Tickets");
            System.out.println("  [4] Reports");
            System.out.println("  [5] Audit Logs");
            System.out.println("  [6] System Settings");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    System.out.println("Manage Users is not available yet.");
                    break;
                case "2":
                    System.out.println("Manage Events is not available yet.");
                    break;
                case "3":
                    System.out.println("View All Tickets is not available yet.");
                    break;
                case "4":
                    System.out.println("Reports is not available yet.");
                    break;
                case "5":
                    System.out.println("Audit Logs is not available yet.");
                    break;
                case "6":
                    System.out.println("System Settings is not available yet.");
                    break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + admin.fullname() + "!");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }
}