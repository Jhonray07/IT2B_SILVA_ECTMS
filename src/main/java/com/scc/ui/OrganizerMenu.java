package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class OrganizerMenu {

    private final Scanner sc;
    private final UserDao userDao;

    public OrganizerMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User organizer) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  ORGANIZER MENU | Logged in: " + organizer.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Manage My Events");
            System.out.println("  [0] Logout");
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Logging out... Goodbye, " + organizer.fullname() + "!");
                return;
            } else if (input.equals("1")) {
                System.out.println("Manage My Events is not available yet.");
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}