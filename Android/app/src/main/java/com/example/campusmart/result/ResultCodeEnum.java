package com.example.campusmart.result;


/**
 * Common result status messages
 */
public enum ResultCodeEnum {
    SUCCESS(200, "Success"),
    FAIL(201, "Failed"),
    PARAM_ERROR(202, "Invalid params"),
    SERVICE_ERROR(203, "Service error"),
    DATA_ERROR(204, "Data error"),
    ILLEGAL_REQUEST(205, "Invalid request"),
    REPEAT_SUBMIT(206, "Duplicate submit"),
    DELETE_ERROR(207, "Delete children first"),

    ADMIN_ACCOUNT_EXIST_ERROR(301, "Account exists"),
    ADMIN_CAPTCHA_CODE_ERROR(302, "Invalid code"),
    ADMIN_CAPTCHA_CODE_EXPIRED(303, "Code expired"),
    ADMIN_CAPTCHA_CODE_NOT_FOUND(304, "Enter code"),

    ADMIN_APARTMENT_DELETE_ERROR(310,"Delete room first"),

    ADMIN_LOGIN_AUTH(305, "Please log in"),
    ADMIN_ACCOUNT_NOT_EXIST_ERROR(306, "Account not found"),
    ADMIN_ACCOUNT_ERROR(307, "Wrong username or password"),
    ADMIN_ACCOUNT_DISABLED_ERROR(308, "User disabled"),
    ADMIN_ACCESS_FORBIDDEN(309, "No permission"),

    APP_LOGIN_AUTH(501, "Please log in"),
    APP_LOGIN_PHONE_EMPTY(502, "Phone is empty"),
    APP_LOGIN_CODE_EMPTY(503, "Code is empty"),
    APP_SEND_SMS_TOO_OFTEN(504, "Too many requests"),
    APP_LOGIN_CODE_EXPIRED(505, "Code expired"),
    APP_LOGIN_CODE_ERROR(506, "Invalid code"),
    APP_ACCOUNT_DISABLED_ERROR(507, "User disabled"),


    TOKEN_EXPIRED(601, "Token expired"),
    TOKEN_INVALID(602, "Invalid token");


    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    private final Integer code;

    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
