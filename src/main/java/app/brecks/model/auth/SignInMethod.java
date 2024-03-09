package app.brecks.model.auth;

import lombok.Getter;

@Getter
public enum SignInMethod {
    PASSWORD ("password"),
    ACCESS_KEY ("access_key"),
    SMS ("sms");

    private final String method;

    SignInMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return this.method;
    }
}
