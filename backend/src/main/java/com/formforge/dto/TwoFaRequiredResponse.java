package com.formforge.dto;

/**
 * Returned by {@code POST /api/auth/login} when credentials are valid
 * but the 2FA step has not yet been completed.
 * The client must prompt the user for the token and call
 * {@code POST /api/auth/verify-2fa}.
 */
public record TwoFaRequiredResponse(
        boolean twoFactorRequired,
        String message
) {
    public static TwoFaRequiredResponse pending() {
        return new TwoFaRequiredResponse(true,
                "Check the server logs for your verification code and enter it below.");
    }
}
