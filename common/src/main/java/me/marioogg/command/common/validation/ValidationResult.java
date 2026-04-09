package me.marioogg.command.common.validation;

import lombok.Getter;

@Getter
public class ValidationResult {
    public static final ValidationResult OK = new ValidationResult(true, null);

    private final boolean valid;
    private final String failReason;

    private ValidationResult(boolean valid, String failReason) {
        this.valid = valid;
        this.failReason = failReason;
    }

    public static ValidationResult fail(String reason) {
        return new ValidationResult(false, reason);
    }
}
