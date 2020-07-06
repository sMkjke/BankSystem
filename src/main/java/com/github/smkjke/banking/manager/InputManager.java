package com.github.smkjke.banking.manager;


import com.github.smkjke.banking.system.Account;
import com.github.smkjke.banking.system.BankSystem;

import java.util.Scanner;

public class InputManager {

    private final Scanner scanner;
    Account currentAccount;

    public InputManager(Scanner scanner) {
        this.scanner = scanner;
    }

    public void startInput(final BankSystem bankSystem) {

        boolean exit = true;
        label:
        while (exit) {
            showMainMenu();
            scanner.skip("((?<!\\R)\\s)*");
            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    Account account = new Account(bankSystem.getBankID());
                    System.out.println("Your card has been created");
                    System.out.println(account.getCardNumber());
                    System.out.println("Your card PIN:");
                    System.out.println(account.getPinCode());
                    bankSystem.addCardToDB(account);
                    break;
                case "2":
                    System.out.println("Enter your card number:");
                    String enteredCardNum = scanner.nextLine();
                    System.out.println("Enter your PIN:");
                    String pinCode = scanner.nextLine();
                    if (bankSystem.tryToLogInAccount(enteredCardNum, pinCode)) {
                        System.out.println("You have successfully logged in!");
                        System.out.println();
                        currentAccount = bankSystem.getAccount(enteredCardNum);
                        while (currentAccount != null && exit) {
                            showAccountMenu();

                            String insideInput = scanner.next();
                            switch (insideInput) {
                                case ("1"):
                                    System.out.println(bankSystem.getCurrentCardBalance(currentAccount.getCardNumber()));
                                    break;
                                case ("2"):
                                    System.out.println("Enter income: ");
                                    int addSum = scanner.nextInt();
                                    if (addSum > 0) {
                                        bankSystem.addIncome(currentAccount, addSum);
                                    } else {
                                        System.out.println("Wrong number!");
                                    }
                                    break;
                                case ("3"):
                                    System.out.println("Transfer");
                                    System.out.println("Enter card number: ");
                                    String receiverCard = scanner.next();
                                    if (!receiverCard.equals(currentAccount.getCardNumber())) {
                                        if (bankSystem.isCardExistsAndValid(receiverCard)) {
                                            System.out.println("Enter how much money you want to transfer:");
                                            int transferSum = scanner.nextInt();
                                            bankSystem.doTransfer(currentAccount.getCardNumber(), receiverCard, transferSum);
                                        }
                                    }
                                    break;
                                case ("4"):
                                    bankSystem.closeAccount(currentAccount.getId());
                                    currentAccount = null;
                                    break;
                                case ("5"):
                                    bankSystem.tryToLogOutAccount();
                                    currentAccount = null;
                                    break;
                                case ("0"):
                                    exit = false;
                                    System.out.println("\nBye!");
                                    break;
                            }
                        }
                    } else {
                        System.out.println("Wrong card number or PIN!");
                    }
                    break;
                case "0":
                    System.out.println("\nBye!");
                    break label;
                default:
                    System.out.println("\nIncorrect option! Try again.");
                    break;
            }
        }
    }

    private void showMainMenu() {
        System.out.println("1. Create account\n"
                + "2. Log into account\n"
                + "0. Exit");
    }

    private void showAccountMenu() {
        System.out.println();
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }
}