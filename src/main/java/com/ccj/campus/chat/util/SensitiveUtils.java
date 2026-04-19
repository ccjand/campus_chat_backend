package com.ccj.campus.chat.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 敏感字段脱敏。对齐论文 4.4 / 摘要：
 * "对手机号等敏感字段进行了脱敏处理"
 */
public final class SensitiveUtils {

    private SensitiveUtils() {}

    /** 138****1234 */
    public static String maskPhone(String phone) {
        if (StringUtils.length(phone) < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** a***@b.com */
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String prefix = email.substring(0, at);
        if (prefix.length() <= 1) {
            return email;
        }
        return prefix.charAt(0) + "***" + email.substring(at);
    }

    /** 张** （姓名） */
    public static String maskName(String name) {
        if (StringUtils.length(name) < 2) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /** 学号/工号：前3后3 */
    public static String maskAccount(String account) {
        if (StringUtils.length(account) <= 6) {
            return account;
        }
        return account.substring(0, 3) + "****" + account.substring(account.length() - 3);
    }
}