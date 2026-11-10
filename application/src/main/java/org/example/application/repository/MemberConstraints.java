package org.example.application.repository;

public final class MemberConstraints {
    public static final int NAME_MAX = 50;
    public static final int USERNAME_MAX = 50;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX_BYTES = 72;

    private MemberConstraints() {}
}