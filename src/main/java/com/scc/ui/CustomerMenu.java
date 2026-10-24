package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class CustomerMenu {

    private final Scanner sc;
    private final UserDao userDao;

    public CustomerMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User customer) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  CUSTOMER MENU | Logged in: " + customer.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Browse Events");
            System.out.println("  [0] Logout");
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Logging out... Goodbye, " + customer.fullname() + "!");
                return;
            } else if (input.equals("1")) {
                System.out.println("Browse Events is not available yet.");
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}