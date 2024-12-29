package com.bankapp;
import java.util.*;

public class Customer implements Runnable {
    private long id;
    private long accountNumber;
    private Bank bank;

    Customer(long id, Bank bank) {
        this.id = id;
        this.accountNumber = generateAccountNumber(id);
        bank.createAccount(this.accountNumber);
        this.bank = bank;
    }

    public static long generateAccountNumber(long id) {
        return (10000000000000L + id);
    }

    private void performRandomTransaction() {
        Random r = new Random();
        long amount = r.nextLong(1000);
        boolean isDeposit = r.nextBoolean();
        if (isDeposit) {
            bank.deposit(accountNumber, amount);
            System.out.printf("Customer %d deposited %d. New balance: %d%n", id, amount,
                    bank.getBalance(accountNumber));

        } else {

            bank.withdraw(accountNumber, amount);
            System.out.printf("Customer %d withdrew %d. New balance: %d%n",
                    id, amount, bank.getBalance(accountNumber));
        }
        System.out.printf("Total money in bank: %d%n%n", bank.getTotalMoney());
    };

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                performRandomTransaction();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();// Sleep clears the interrupt flag that is why we need to set it
                                                   // again.
                return;
            }
        }
    }
}