package com.formforge.model;

public enum AuditAction {
    // Authentication
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    REGISTER,

    // Applications
    READ_APPLICATIONS,
    READ_APPLICATION,
    CREATE_APPLICATION,
    UPDATE_APPLICATION,
    DELETE_APPLICATION,

    // Documents
    READ_DOCUMENTS,
    READ_DOCUMENT,
    CREATE_DOCUMENT,
    UPDATE_DOCUMENT,
    DELETE_DOCUMENT,

    // Admin
    ADMIN_VIEW_LOGS,
    ADMIN_VIEW_OBSERVATIONS,
    ADMIN_RESOLVE_OBSERVATION,

    // Security
    UNAUTHORIZED_ACCESS,

    // Password recovery
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESS
}
