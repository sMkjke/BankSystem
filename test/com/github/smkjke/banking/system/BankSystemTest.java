package com.github.smkjke.banking.system;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;


import java.util.Date;
import java.util.concurrent.CountDownLatch;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BankSystemTest {

    private static final int THREADS = 10;
    private static final int TRANSFERS_PER_THREAD = 10;

    private BankSystem bankSystem;
    private AccountDaoImpl dao;
    private Account sender = new Account(0);
    private Account receiverFirst = new Account(1);
    private Account receiverSecond = new Account(2);

    @BeforeAll
    public void createTask() {
        dao = new AccountDaoImpl("testcard");
        bankSystem = new BankSystem(dao);

        dao.dropTable();
        dao.createIfNotExists();
    }

    @AfterEach
    public void clearDBAfterEachTest() {
        dao.dropTable();
        dao.createIfNotExists();
    }

    @Test
    public void multithreadedTransferTest() {
        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());

        final int totalCount = THREADS * TRANSFERS_PER_THREAD;

        bankSystem.addIncome(sender, totalCount);

        final CountDownLatch latch = new CountDownLatch(THREADS);

        Runnable task = () -> {
            for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
                bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), 1, 0, false);
            }
            latch.countDown();
        };

        for (int i = 0; i < THREADS; i++) {
            new Thread(task).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assertions.assertEquals(totalCount, bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber()));
        Assertions.assertEquals(0, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
    }

    @Test
    public void readWriteAtomicityCheck() throws InterruptedException {

        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.addCardToDB(receiverSecond);

        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());

        final int balance = 10;
        bankSystem.addIncome(sender, balance);

        final CountDownLatch latch = new CountDownLatch(THREADS);
        Runnable task = () -> {
            bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), balance);
            latch.countDown();
            System.out.println(latch);
        };

        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(task);
            thread.start();
        }

        try {
            System.out.println("waiting " + new Date());
            latch.await();
            System.out.println("stopped " + new Date());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(balance, bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber()));
        Assertions.assertEquals(0, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
    }


    @Test
    public void intermediateFailConsistencyCheck() {
        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.addCardToDB(receiverSecond);

        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());


        final int balance = 10;
        bankSystem.addIncome(sender, balance);

        bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), balance, 0, true);

        Assertions.assertEquals(balance, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
        Assertions.assertEquals(0, bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber()));
    }

    @Test
    public void interleavingCorrectness() {

        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.addCardToDB(receiverSecond);
        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());

        final int balance = 10;
        bankSystem.addIncome(sender, balance);

        final CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), balance);
            latch.countDown();
            System.out.println(latch);
        }).start();

        new Thread(() -> {
            bankSystem.doTransfer(sender.getCardNumber(), receiverSecond.getCardNumber(), balance);
            latch.countDown();
            System.out.println(latch);
        }).start();


        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final int firstReceiverBalance = bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber());
        final int secondReceiverBalance = bankSystem.getCurrentCardBalance(receiverSecond.getCardNumber());

        Assertions.assertTrue(firstReceiverBalance >= 0);
        Assertions.assertTrue(secondReceiverBalance >= 0);

        Assertions.assertEquals(balance, firstReceiverBalance + secondReceiverBalance);
        Assertions.assertEquals(0, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
    }


    @Test
    public void outdatedBalanceCheck() throws InterruptedException {

        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());

        final int senderBalance = 20;
        bankSystem.addIncome(sender, senderBalance);

        final CountDownLatch latch = new CountDownLatch(1);
        // start before with delay for testing if balances are cached
        new Thread(() -> {
            bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), 10, 1000, false);
            latch.countDown();
        }).start();

        Thread.sleep(500);

        bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), 10);

        latch.await();

        Assertions.assertEquals(senderBalance, bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber()));
        Assertions.assertEquals(0, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
    }

    @Test
    public void deadLockTwoWayTransactionsCheck() throws InterruptedException {

        bankSystem.addCardToDB(sender);
        bankSystem.addCardToDB(receiverFirst);
        bankSystem.tryToLogInAccount(sender.getCardNumber(), sender.getPinCode());

        final int balance = 10;
        bankSystem.addIncome(sender, balance);
        bankSystem.addIncome(receiverFirst, balance);

        final CountDownLatch latch = new CountDownLatch(1);
        // в
        new Thread(() -> {
            bankSystem.doTransfer(sender.getCardNumber(), receiverFirst.getCardNumber(), balance);
            latch.countDown();
        }).start();

        Thread.sleep(50);

        bankSystem.doTransfer(receiverFirst.getCardNumber(), sender.getCardNumber(), balance);

        latch.await();

        Assertions.assertEquals(balance, bankSystem.getCurrentCardBalance(receiverFirst.getCardNumber()));
        Assertions.assertEquals(balance, bankSystem.getCurrentCardBalance(sender.getCardNumber()));
    }
}