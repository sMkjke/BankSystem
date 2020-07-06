package com.github.smkjke.banking;

import com.github.smkjke.banking.manager.InputManager;
import com.github.smkjke.banking.system.BankSystem;

import java.util.Scanner;


public class Main {

    InputManager inputManager;
    BankSystem bankingSystem;


    public Main(String fileName) {
        Scanner scanner = new Scanner(System.in);
        bankingSystem = new BankSystem(fileName);
        inputManager = new InputManager(scanner);
    }

    public static void main(String[] args) {
            String fileName = "default";
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    if ("-fileName".equals(args[i])) {
                        fileName = args[i + 1];
                    }
                }
            }
        Main application = new Main(fileName);
        application.start();
    }

    public void start() {
        inputManager.startInput(bankingSystem);
    }
}
