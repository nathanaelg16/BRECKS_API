package app.brecks.model.auth;

public record User(int id, String firstName, String lastName, String displayName, String email, String username, byte[] password, String salt, UserStatus status) {}
