package com.preservinc.production.djr.response;

import lombok.NonNull;

public record RegistrationDetailsResponse (@NonNull String email, @NonNull String firstName, @NonNull String lastName, @NonNull String role) { }