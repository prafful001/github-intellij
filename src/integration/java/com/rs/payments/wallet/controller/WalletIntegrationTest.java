package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.WithdrawRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    // helper to create a user and wallet
    private Wallet createUserAndWallet(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user = userRepository.save(user);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        String url = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> response = restTemplate.postForEntity(url, request, Wallet.class);
        return response.getBody();
    }

    @Test
    void shouldCreateWalletForExistingUser() {
        User user = new User();
        user.setUsername("walletuser");
        user.setEmail("wallet@example.com");
        user = userRepository.save(user);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        String url = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> response = restTemplate.postForEntity(url, request, Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(UUID.randomUUID());

        String url = "http://localhost:" + port + "/wallets";
        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void shouldDepositFundsIntoWallet() {
        Wallet wallet = createUserAndWallet("deposituser", "deposit@example.com");

        DepositRequest request = new DepositRequest(BigDecimal.valueOf(100));
        String url = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        ResponseEntity<Wallet> response = restTemplate.postForEntity(url, request, Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldWithdrawFundsFromWallet() {
        Wallet wallet = createUserAndWallet("withdrawuser", "withdraw@example.com");

        // First deposit
        DepositRequest depositRequest = new DepositRequest(BigDecimal.valueOf(100));
        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        // Then withdraw
        WithdrawRequest withdrawRequest = new WithdrawRequest(BigDecimal.valueOf(50));
        String withdrawUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/withdraw";
        ResponseEntity<Wallet> response = restTemplate.postForEntity(withdrawUrl, withdrawRequest, Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void shouldReturnBadRequestWhenInsufficientFunds() {
        Wallet wallet = createUserAndWallet("insufficientuser", "insufficient@example.com");

        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(100));
        String url = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/withdraw";

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void shouldTransferFundsBetweenWallets() {
        Wallet fromWallet = createUserAndWallet("transferuser1", "transfer1@example.com");
        Wallet toWallet = createUserAndWallet("transferuser2", "transfer2@example.com");

        // Deposit into fromWallet
        DepositRequest depositRequest = new DepositRequest(BigDecimal.valueOf(100));
        String depositUrl = "http://localhost:" + port + "/wallets/" + fromWallet.getId() + "/deposit";
        restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        // Transfer
        TransferRequest transferRequest = new TransferRequest(
                fromWallet.getId(), toWallet.getId(), BigDecimal.valueOf(50));
        String transferUrl = "http://localhost:" + port + "/transfers";
        ResponseEntity<String> response = restTemplate.postForEntity(transferUrl, transferRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Check balances
        String fromBalanceUrl = "http://localhost:" + port + "/wallets/" + fromWallet.getId() + "/balance";
        String toBalanceUrl = "http://localhost:" + port + "/wallets/" + toWallet.getId() + "/balance";

        ResponseEntity<BigDecimal> fromBalance = restTemplate.getForEntity(fromBalanceUrl, BigDecimal.class);
        ResponseEntity<BigDecimal> toBalance = restTemplate.getForEntity(toBalanceUrl, BigDecimal.class);

        assertThat(fromBalance.getBody()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(toBalance.getBody()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void shouldGetWalletBalance() {
        Wallet wallet = createUserAndWallet("balanceuser", "balance@example.com");

        // Deposit first
        DepositRequest depositRequest = new DepositRequest(BigDecimal.valueOf(75));
        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        // Check balance
        String balanceUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/balance";
        ResponseEntity<BigDecimal> response = restTemplate.getForEntity(balanceUrl, BigDecimal.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }
}