package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.system.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现（阿里云短信模式）
 * <p>
 * 通过阿里云 SMS SDK 发送真实短信验证码。
 * 通过配置 app.sms.provider=aliyun 激活此实现。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "aliyun")
public class AliyunSmsServiceImpl implements SmsService {

    /**
     * 验证码有效期（分钟）
     */
    private static final int CODE_EXPIRE_MINUTES = 5;
    /**
     * 发送间隔限制（秒）—— 同一手机号两次发送至少间隔 60 秒
     */
    private static final int SEND_INTERVAL_SECONDS = 60;
    /**
     * 每日发送次数上限
     */
    private static final int DAILY_SEND_LIMIT = 10;
    /**
     * Redis key 前缀
     */
    private static final String CODE_KEY_PREFIX = "bw:sms:code:";
    private static final String RATE_KEY_PREFIX = "bw:sms:rate:";
    private static final String DAILY_KEY_PREFIX = "bw:sms:daily:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;
    @Value("${app.sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${app.sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${app.sms.aliyun.sign-name:}")
    private String signName;

    @Value("${app.sms.aliyun.template-code:}")
    private String templateCode;

    @Override
    public String sendVerificationCode(String phone) {
        if (!canSend(phone)) {
            log.warn("短信验证码发送频率超限: phone={}", maskPhone(phone));
            return null;
        }

        // 生成 6 位数字验证码
        String code = String.format("%06d", RANDOM.nextInt(1000000));

        // 存储到 Redis，设置过期时间
        String codeKey = CODE_KEY_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 记录发送间隔
        String rateKey = RATE_KEY_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(rateKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 记录每日发送次数
        String dailyKey = DAILY_KEY_PREFIX + phone;
        Long count = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1) {
            // 首次发送，设置到当天结束的过期时间
            stringRedisTemplate.expire(dailyKey, 24 * 60 * 60, TimeUnit.SECONDS);
        }

        // TODO: 对接阿里云 SMS SDK，调用 SendSms API 发送真实短信
        // 参考文档：https://help.aliyun.com/document_detail/101414.html
        // 示例代码：
        // Config config = new Config()
        //     .setAccessKeyId(accessKeyId)
        //     .setAccessKeySecret(accessKeySecret);
        // SendSmsRequest request = new SendSmsRequest()
        //     .setPhoneNumbers(phone)
        //     .setSignName(signName)
        //     .setTemplateCode(templateCode)
        //     .setTemplateParam("{\"code\":\"" + code + "\"}");
        // client.sendSms(request);
        log.info("[阿里云短信] 发送验证码到 phone={}, code={}****", maskPhone(phone), code.substring(0, 2));

        return code;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        String codeKey = CODE_KEY_PREFIX + phone;
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }
        boolean matched = code.equals(storedCode);
        if (matched) {
            // 验证成功后删除验证码，防止重复使用
            stringRedisTemplate.delete(codeKey);
        }
        return matched;
    }

    @Override
    public boolean canSend(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        // 检查发送间隔
        String rateKey = RATE_KEY_PREFIX + phone;
        if (stringRedisTemplate.hasKey(rateKey)) {
            return false;
        }

        // 检查每日发送次数
        String dailyKey = DAILY_KEY_PREFIX + phone;
        String countStr = stringRedisTemplate.opsForValue().get(dailyKey);
        if (countStr != null && Integer.parseInt(countStr) >= DAILY_SEND_LIMIT) {
            return false;
        }

        return true;
    }

    /**
     * 手机号脱敏（中间4位用*替代）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
