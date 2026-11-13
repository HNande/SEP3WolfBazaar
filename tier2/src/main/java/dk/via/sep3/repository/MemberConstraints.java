package dk.via.sep3.repository;

public final class MemberConstraints {
    public static final int NAME_MAX = 50;
    public static final int USERNAME_MAX = 50;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX_BYTES = 72;
    public static final int EMAIL_MAX = 100;
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private MemberConstraints() {}
}