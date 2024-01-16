package com.preservinc.production.djr.model.auth;

public record User(int id, String firstName, String lastName, String displayName, String email, String username, String password, String salt, UserStatus status) {}
