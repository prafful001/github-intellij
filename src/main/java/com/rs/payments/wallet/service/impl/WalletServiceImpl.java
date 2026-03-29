package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.DuplicateResourceException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet createWalletForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (walletRepository.existsByUser(user)) {
            throw new DuplicateResourceException("User already has a wallet");
        }

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        // Fetch wallet
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Update balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription("Deposit");
        transactionRepository.save(transaction);

        return wallet;
    }
    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // 400 - Insufficient funds
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Update balance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription("Withdrawal");
        transactionRepository.save(transaction);

        return wallet;
    }
    @Override
    @Transactional
    public void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        // Fetch both wallets
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found"));
        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination wallet not found"));

        // 400 - Insufficient funds — rolls back entirely due to @Transactional
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Update balances
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        // Create TRANSFER_OUT transaction for sender
        Transaction outTransaction = new Transaction();
        outTransaction.setWallet(fromWallet);
        outTransaction.setAmount(amount);
        outTransaction.setType(TransactionType.TRANSFER_OUT);
        outTransaction.setTimestamp(LocalDateTime.now());
        outTransaction.setDescription("Transfer out to wallet: " + toWalletId);
        transactionRepository.save(outTransaction);

        // Create TRANSFER_IN transaction for receiver
        Transaction inTransaction = new Transaction();
        inTransaction.setWallet(toWallet);
        inTransaction.setAmount(amount);
        inTransaction.setType(TransactionType.TRANSFER_IN);
        inTransaction.setTimestamp(LocalDateTime.now());
        inTransaction.setDescription("Transfer in from wallet: " + fromWalletId);
        transactionRepository.save(inTransaction);
    }
    @Override
    public BigDecimal getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return wallet.getBalance();
    }
}