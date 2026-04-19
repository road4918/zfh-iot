package com.zfh.iot.common.utils;

import com.zfh.iot.common.exception.BusinessException;

import java.security.SecureRandom;

public class PasswordUtils {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$%^&*!?";
    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final SecureRandom random = new SecureRandom();

    public static String generateRandomPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8");
        }

        StringBuilder sb = new StringBuilder(length);
        sb.append(randomChar(UPPERCASE));
        sb.append(randomChar(LOWERCASE));
        sb.append(randomChar(SPECIAL));
        sb.append(randomChar(DIGITS));

        for (int i = sb.length(); i < length; i++) {
            sb.append(randomChar(ALL_CHARACTERS));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[swapIndex];
            chars[swapIndex] = temp;
        }
        return new String(chars);
    }

    public static String generateRandomPassword() {
        return generateRandomPassword(8);
    }

    public static void validateComplexPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位，且需包含大小写字母和特殊字符");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasSpecial = false;

        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(ch)) {
                hasLowercase = true;
            } else if (SPECIAL.indexOf(ch) >= 0) {
                hasSpecial = true;
            }
        }

        if (!hasUppercase || !hasLowercase || !hasSpecial) {
            throw new BusinessException("密码长度不能少于8位，且需包含大小写字母和特殊字符");
        }
    }

    private static char randomChar(String chars) {
        return chars.charAt(random.nextInt(chars.length()));
    }
}
