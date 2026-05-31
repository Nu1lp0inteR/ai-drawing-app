package com.aidrawing.backend.service;

import com.aidrawing.backend.entity.Credit;
import com.aidrawing.backend.entity.CreditTransaction;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.CreditRepository;
import com.aidrawing.backend.repository.CreditTransactionRepository;
import com.aidrawing.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class CreditService {

    private static final Logger logger = LoggerFactory.getLogger(CreditService.class);

    private static final int INITIAL_CREDITS = 50;
    private static final int SIGN_IN_BONUS = 10;
    private static final int GENERATE_COST = 1;

    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public CreditService(CreditRepository creditRepository,
                         CreditTransactionRepository transactionRepository,
                         UserRepository userRepository,
                         RedisTemplate<String, String> redisTemplate) {
        this.creditRepository = creditRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    public void createForUser(User user) {
        if (creditRepository.existsByUserId(user.getId())) return;
        Credit credit = new Credit(user);
        creditRepository.save(credit);
        logger.info("Created credits record for user: {}, balance={}", user.getUsername(), INITIAL_CREDITS);
    }

    public Credit getOrCreateForUser(User user) {
        return creditRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                Credit credit = new Credit(user);
                return creditRepository.save(credit);
            });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalanceInfo(String userId) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("积分账户不存在"));
        boolean todaySignedIn = isTodaySignedIn(credit);

        Map<String, Object> info = new HashMap<>();
        info.put("balance", credit.getBalance());
        info.put("today_signed_in", todaySignedIn);
        info.put("sign_in_bonus", SIGN_IN_BONUS);
        info.put("generate_cost", GENERATE_COST);
        return info;
    }

    public Map<String, Object> dailySignIn(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Credit credit = getOrCreateForUser(user);

        if (isTodaySignedIn(credit)) {
            throw new IllegalStateException("今日已签到");
        }

        credit.setBalance(credit.getBalance() + SIGN_IN_BONUS);
        credit.setLastSignInDate(LocalDate.now());
        creditRepository.save(credit);

        String redisKey = getSignInRedisKey(userId);
        try {
            redisTemplate.opsForValue().set(redisKey, "1",
                Duration.ofSeconds(86400));
        } catch (Exception e) {
            logger.warn("Failed to set sign-in Redis key: {}", e.getMessage());
        }

        CreditTransaction tx = new CreditTransaction(user, SIGN_IN_BONUS, "SIGN_IN",
            "每日签到奖励 +" + SIGN_IN_BONUS);
        transactionRepository.save(tx);

        logger.info("Daily sign-in: userId={}, balance={}", userId, credit.getBalance());

        Map<String, Object> result = new HashMap<>();
        result.put("balance", credit.getBalance());
        result.put("bonus", SIGN_IN_BONUS);
        result.put("message", "签到成功，获得 " + SIGN_IN_BONUS + " 积分");
        return result;
    }

    public boolean deductCredits(String userId, int amount, String description) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("积分账户不存在"));

        if (credit.getBalance() < amount) {
            return false;
        }

        credit.setBalance(credit.getBalance() - amount);
        creditRepository.save(credit);

        CreditTransaction tx = new CreditTransaction(credit.getUser(), -amount, "GENERATE", description);
        transactionRepository.save(tx);

        logger.info("Deducted {} credits: userId={}, balance={}", amount, userId, credit.getBalance());
        return true;
    }

    public void addCredits(String userId, int amount, String type, String description) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Credit credit = getOrCreateForUser(user);

        credit.setBalance(credit.getBalance() + amount);
        creditRepository.save(credit);

        CreditTransaction tx = new CreditTransaction(user, amount, type, description);
        transactionRepository.save(tx);

        logger.info("Added {} credits: userId={}, balance={}", amount, userId, credit.getBalance());
    }

    public boolean hasEnoughCredits(String userId) {
        return hasEnoughCredits(userId, GENERATE_COST);
    }

    public boolean hasEnoughCredits(String userId, int cost) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("积分账户不存在"));
        return credit.getBalance() >= cost;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTransactionHistory(String userId, int page, int size) {
        Page<CreditTransaction> txPage = transactionRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        Map<String, Object> result = new HashMap<>();
        result.put("content", txPage.getContent().stream().map(tx -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tx.getId());
            item.put("amount", tx.getAmount());
            item.put("type", tx.getType());
            item.put("description", tx.getDescription());
            item.put("created_at", tx.getCreatedAt().toString());
            return item;
        }).toList());
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", txPage.getTotalElements());
        result.put("totalPages", txPage.getTotalPages());
        return result;
    }

    public int getGenerateCost() {
        return GENERATE_COST;
    }

    private boolean isTodaySignedIn(Credit credit) {
        try {
            String redisKey = getSignInRedisKey(credit.getUser().getId());
            String cached = redisTemplate.opsForValue().get(redisKey);
            if ("1".equals(cached)) return true;
        } catch (Exception e) {
            logger.warn("Redis sign-in check failed, falling back to DB: {}", e.getMessage());
        }
        return credit.getLastSignInDate() != null
            && credit.getLastSignInDate().equals(LocalDate.now());
    }

    private String getSignInRedisKey(String userId) {
        return "signin:" + userId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
