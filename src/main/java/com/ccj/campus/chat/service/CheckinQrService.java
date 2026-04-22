package com.ccj.campus.chat.service;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 签到二维码生成与校验。
 *
 * 二维码内容格式: CC.{sessionId}.{expireAtEpochSec}.{sigBase64Url}
 *   - sig = 前 16 字节( HMAC-SHA256(secret, "{sessionId}.{expireAtSec}") )
 *   - 默认 TTL 60 秒；前端每 10 秒轮询刷新一次二维码
 *   - 截屏或远程转发的二维码会因为过期而失效
 */
@Slf4j
@Service
public class CheckinQrService {

    private static final String PREFIX = "CC";
    private static final String SEP = ".";

    @Value("${campus.checkin.qr-secret:campus-checkin-default-secret-please-override}")
    private String secret;

    @Value("${campus.checkin.qr-ttl-seconds:60}")
    private int ttlSeconds;

    @Value("${campus.checkin.qr-size:280}")
    private int qrSize;

    private byte[] secretBytes;

    @PostConstruct
    public void init() {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** 为指定签到会话生成一次性二维码内容（含 TTL 与签名） */
    public QrContent generate(Long sessionId) {
        long expireAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = sessionId + SEP + expireAt;
        String sig = signUrlSafe(payload);
        String content = PREFIX + SEP + payload + SEP + sig;
        return new QrContent(content, expireAt);
    }

    /** 将二维码内容渲染为 PNG 的 Base64 Data URI，直接可以放到 <image src> 上 */
    public String generateImageBase64(String content) {
        try {
            QrConfig config = new QrConfig(qrSize, qrSize);
            config.setMargin(1);
            BufferedImage image = QrCodeUtil.generate(content, config);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("generate qrcode image failed", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "二维码生成失败");
        }
    }

    /** 解析并校验二维码内容，返回签到会话 ID。任何异常都抛 BusinessException */
    public Long verify(String content) {
        if (content == null || content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "二维码内容为空");
        }
        // 用 SEP 的字面量切，避免 . 在正则里的陷阱
        String[] parts = content.split("\\Q" + SEP + "\\E");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "二维码格式错误");
        }
        long sessionId;
        long expireAt;
        try {
            sessionId = Long.parseLong(parts[1]);
            expireAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "二维码格式错误");
        }
        String expectedSig = signUrlSafe(parts[1] + SEP + parts[2]);
        if (!constantTimeEquals(expectedSig, parts[3])) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "二维码已被篡改");
        }
        if (Instant.now().getEpochSecond() > expireAt) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "二维码已过期，请让教师刷新二维码后重新扫码");
        }
        return sessionId;
    }

    private String signUrlSafe(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 截取前 16 字节，足够安全且能显著缩短二维码容量
            byte[] truncated = new byte[16];
            System.arraycopy(raw, 0, truncated, 0, 16);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Data
    @AllArgsConstructor
    public static class QrContent {
        /** 二维码文本内容，写到二维码里 */
        private String content;
        /** 过期时间（秒级 epoch） */
        private long expireAt;
    }
}