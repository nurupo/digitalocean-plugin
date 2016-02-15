package com.dubture.jenkins.digitalocean;

import hudson.util.FormValidation;

/**
 * Allows you to assert some basic properties of the input value and provide a FormValidation.Kind (WARNING or ERROR)
 * and message that should be displayed to the user in case the assertion doesn't hold.
 * 
 * Construct with a value you want to use in assertions, chain some assertions and get the result.
 * Result is FormValidation.ok() if no assertions failed, or FormValidation.warning() with the message of the first
 * failed WARNING assertion, or FormValidation.error() with the message of the first failed ERROR assertion. If both
 * WARNING and ERROR assertions fail, no matter the chaining order, the result will be the ERROR.
 *
 * Passing FormValidation.Kind.OK into assertions does nothing.
 */
public class FormValidationAsserter {

    private final String value;
    private FormValidation validation;

    public FormValidationAsserter(String value) {
        this.value = value;
        this.validation = FormValidation.ok();
    }

    public FormValidationAsserter(FormValidationAsserter copy) {
        this.value = copy.value;
        this.validation = copy.validation;
    }

    public interface Condition {
        public boolean evaluate();
    }

    public FormValidationAsserter isCondition(Condition condition, FormValidation.Kind kind, String message) {
        if (validation.kind == FormValidation.Kind.ERROR) {
            return this;
        } else if (validation.kind == FormValidation.Kind.WARNING && kind != FormValidation.Kind.ERROR) {
            return this;
        }

        if (!condition.evaluate()) {
            if (kind == FormValidation.Kind.ERROR) {
                validation = FormValidation.error(message);
            } else if (kind == FormValidation.Kind.WARNING) {
                validation = FormValidation.warning(message);
            }
        }

        return this;
    }

    public FormValidation result() {
        return validation;
    }

    public FormValidationAsserter isNotNull(FormValidation.Kind kind, String message) {
        return isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                return value != null;
            }
        }, kind, message);
    }

    public FormValidationAsserter isNotEmpty(FormValidation.Kind kind, String message) {
        return isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                return !value.isEmpty();
            }
        }, kind, message);
    }

    public FormValidationAsserter isSet(FormValidation.Kind kind, String message) {
        return isNotNull(kind, message).isNotEmpty(kind, message);
    }

    public FormValidationAsserter contains(final String substring, FormValidation.Kind kind, String message) {
        return isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                return value.contains(substring);
            }
        }, kind, message);
    }

    public FormValidationAsserter isLong(FormValidation.Kind kind, String message) {
        return isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                try {
                    long number = Long.parseLong(value);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        }, kind, message);
    }

    public FormValidationAsserter isGreaterThanLong(final long comparable, FormValidation.Kind kind, String message) {
        return isLong(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                long number = Long.parseLong(value);
                return number > comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isLessThanLong(final long comparable, FormValidation.Kind kind, String message) {
        return isLong(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                long number = Long.parseLong(value);
                return number < comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isEqualsLong(final long comparable, FormValidation.Kind kind, String message) {
        return isLong(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                long number = Long.parseLong(value);
                return number == comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isPositiveLong(FormValidation.Kind kind, String message) {
        return isGreaterThanLong(0, kind, message);
    }

    public FormValidationAsserter isNonPositiveLong(FormValidation.Kind kind, String message) {
        return isLessThanLong(1, kind, message);
    }

    public FormValidationAsserter isNegativeLong(FormValidation.Kind kind, String message) {
        return isLessThanLong(0, kind, message);
    }

    public FormValidationAsserter isNonNegativeLong(FormValidation.Kind kind, String message) {
        return isGreaterThanLong(-1, kind, message);
    }

    public FormValidationAsserter isDouble(FormValidation.Kind kind, String message) {
        return isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                try {
                    double number = Double.parseDouble(value);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        }, kind, message);
    }

    public FormValidationAsserter isGreaterThanDouble(final double comparable, FormValidation.Kind kind, String message) {
        return isDouble(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                double number = Double.parseDouble(value);
                return number > comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isLessThanDouble(final double comparable, FormValidation.Kind kind, String message) {
        return isDouble(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                Double number = Double.parseDouble(value);
                return number < comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isEqualsDouble(final double comparable, FormValidation.Kind kind, String message) {
        return isDouble(kind, message).isCondition(new Condition() {
            @Override
            public boolean evaluate() {
                double number = Double.parseDouble(value);
                return number == comparable;
            }
        }, kind, message);
    }

    public FormValidationAsserter isPositiveDouble(FormValidation.Kind kind, String message) {
        return isGreaterThanDouble(0.0, kind, message);
    }

    public FormValidationAsserter isNonPositiveDouble(FormValidation.Kind kind, String message) {
        return isEqualsDouble(0.0, kind, message).isLessThanDouble(0.0, kind, message);
    }

    public FormValidationAsserter isNegativeDouble(FormValidation.Kind kind, String message) {
        return isLessThanDouble(0.0, kind, message);
    }

    public FormValidationAsserter isNonNegativeDouble(FormValidation.Kind kind, String message) {
        return isEqualsDouble(0.0, kind, message).isGreaterThanDouble(0.0, kind, message);
    }

}
