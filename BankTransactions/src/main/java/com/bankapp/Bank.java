package com.bankapp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {
    private final ConcurrentHashMap<Long, Account> accounts = new ConcurrentHashMap<>();

    public static class Account {
        private final AtomicLong balance;
        private final ReentrantLock lock;

        public Account(long initialBalance) {
            this.balance = new AtomicLong(initialBalance);
            this.lock = new ReentrantLock();
        }
    }

    public void deposit(long accountNumber, long amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        account.lock.lock();
        try {
            account.balance.addAndGet(amount);
        } finally {
            account.lock.unlock();
        }
    }

    public void withdraw(long accountNumber, long amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        account.lock.lock();
        if (account.balance.get() < amount) {
            throw new IllegalArgumentException("Insufficient Funds");
        }
        try {
            account.balance.addAndGet(-amount);
        } finally {
            account.lock.unlock();
        }
    }

    public void transfer(long fromAccountNumber, long toAccountNumber, long amount) {
        Account fromAccount = accounts.get(fromAccountNumber);
        Account toAccount = accounts.get(toAccountNumber);
        if (fromAccount == null || toAccount == null) {
            throw new IllegalArgumentException("Account not found");
        }
        if (fromAccount.balance.get() < amount) {
            throw new IllegalArgumentException("Insufficient Funds");
        }
        fromAccount.lock.lock();
        toAccount.lock.lock();
        try {
            fromAccount.balance.addAndGet(-amount);
            toAccount.balance.addAndGet(amount);
        } finally {
            fromAccount.lock.unlock();
            toAccount.lock.unlock();
        }
    }

    public long getBalance(long accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        return account.balance.get();
    }

    public long getTotalMoney() {
        return accounts.values().stream()
                .mapToLong(account -> account.balance.get())
                .sum();
    }

    public void createAccount(long accountNumber) {
        accounts.put(accountNumber, new Account(0));
    }
}