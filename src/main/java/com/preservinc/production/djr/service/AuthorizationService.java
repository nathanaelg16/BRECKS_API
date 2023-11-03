package com.preservinc.production.djr.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    private final FirebaseAuth firebaseAuth;

    @Autowired
    public AuthorizationService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseToken decodeToken(String authorizationToken) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(authorizationToken, true);
    }
}
