package com.bankapp;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(new Customer(i, bank));
            threads[i] = t;
            threads[i].start();
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Thread t : threads) {
            t.interrupt();
        }
    }
}