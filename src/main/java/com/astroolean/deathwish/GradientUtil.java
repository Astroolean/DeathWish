package com.astroolean.deathwish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting simple gradient tags into Minecraft hex color codes.
 * Usage in config or code:
 *   <gradient:#00BFFF:#00FFFF>Your Text</gradient>
 * It will be replaced with per-character §x§R§R§G§G§B§B style hex color codes (works on modern clients).
 */
public class GradientUtil {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<gradient:#([0-9a-f]{6}):#([0-9a-f]{6})>(.*?)</gradient>", Pattern.DOTALL);

    /**
     * Apply gradients for any gradient tags in the input string.
     * Also translates & color codes.
     */
    public static String applyGradients(String input) {
        if (input == null || input.isEmpty()) return input;
        String result = input;
        Matcher m = GRADIENT_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String startHex = m.group(1);
            String endHex = m.group(2);
            String content = m.group(3);
            String replaced = buildGradient(content, startHex, endHex);
            // escape $ signs since appendReplacement treats them specially
            replaced = Matcher.quoteReplacement(replaced);
            m.appendReplacement(sb, replaced);
        }
        m.appendTail(sb);
        // Finally translate traditional & codes
        String withSections = sb.toString().replace('&', '§');
        return withSections;
    }

    private static String buildGradient(String text, String startHex, String endHex) {
        int startR = Integer.parseInt(startHex.substring(0,2), 16);
        int startG = Integer.parseInt(startHex.substring(2,4), 16);
        int startB = Integer.parseInt(startHex.substring(4,6), 16);
        int endR = Integer.parseInt(endHex.substring(0,2), 16);
        int endG = Integer.parseInt(endHex.substring(2,4), 16);
        int endB = Integer.parseInt(endHex.substring(4,6), 16);

        StringBuilder out = new StringBuilder();
        int len = text.length();
        if (len == 0) return "";

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            double ratio = (len == 1) ? 0.0 : (double) i / (len - 1);
            int r = (int) Math.round(startR + (endR - startR) * ratio);
            int g = (int) Math.round(startG + (endG - startG) * ratio);
            int b = (int) Math.round(startB + (endB - startB) * ratio);
            out.append(toLegacyHex(r, g, b));
            out.append(c);
        }
        return out.toString();
    }

    private static String toLegacyHex(int r, int g, int b) {
        String hex = String.format("%02x%02x%02x", r, g, b);
        // build §x§R§R§G§G§B§B
        StringBuilder sb = new StringBuilder();
        char section = '\u00A7';
        sb.append(section).append('x');
        for (char ch : hex.toCharArray()) {
            sb.append(section).append(ch);
        }
        return sb.toString();
    }
}
