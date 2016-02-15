package com.dubture.jenkins.digitalocean;

import hudson.util.FormValidation;

/**
 * While FormVlidationAsserter provides a way to specify FormValidation.Kind and message, most of the time they set to
 * the same values, so in order not to repeat the same code this class provides methods with FormValidation.Kind and
 * message preset, as well as with a few new convenience checks that are used in multiple places in this plugin.
 */
public class SpecializedFormValidationAsserter extends FormValidationAsserter {

    public SpecializedFormValidationAsserter(final String value) {
        super(value);
    }

    public SpecializedFormValidationAsserter(final FormValidationAsserter form) {
        super(form);
    }

    public SpecializedFormValidationAsserter isSet() {
        return new SpecializedFormValidationAsserter(
                isSet(FormValidation.Kind.ERROR, "Must be set")
        );
    }

    public SpecializedFormValidationAsserter contains(final String substring) {
        return new SpecializedFormValidationAsserter(
                contains(substring, FormValidation.Kind.ERROR, "Must contain \"" + substring + "\"")
        );
    }

    public SpecializedFormValidationAsserter isLong() {
        return new SpecializedFormValidationAsserter(
                isLong(FormValidation.Kind.ERROR, "Must be a number")
        );
    }

    public SpecializedFormValidationAsserter isGreaterThanLong(final long comparable) {
        return new SpecializedFormValidationAsserter(
                isGreaterThanLong(comparable, FormValidation.Kind.ERROR, "Must be greater than " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isLessThanLong(final long comparable) {
        return new SpecializedFormValidationAsserter(
                isLessThanLong(comparable, FormValidation.Kind.ERROR, "Must be less than " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isEqualsLong(final long comparable) {
        return new SpecializedFormValidationAsserter(
                isEqualsLong(comparable, FormValidation.Kind.ERROR, "Must be equal to " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isPositiveLong() {
        return new SpecializedFormValidationAsserter(
                isPositiveLong(FormValidation.Kind.ERROR, "Must be a positive number")
        );
    }

    public SpecializedFormValidationAsserter isNonPositiveLong() {
        return new SpecializedFormValidationAsserter(
                isNonPositiveLong(FormValidation.Kind.ERROR, "Must be a non-positive number")
        );
    }

    public SpecializedFormValidationAsserter isNegativeLong() {
        return new SpecializedFormValidationAsserter(
                isNegativeLong(FormValidation.Kind.ERROR, "Must be a negative number")
        );
    }

    public SpecializedFormValidationAsserter isNonNegativeLong() {
        return new SpecializedFormValidationAsserter(
                isNonNegativeLong(FormValidation.Kind.ERROR, "Must be a non-negative number")
        );
    }

    public SpecializedFormValidationAsserter isDouble() {
        return new SpecializedFormValidationAsserter(
                isDouble(FormValidation.Kind.ERROR, "Must be a number")
        );
    }

    public SpecializedFormValidationAsserter isGreaterThanDouble(final double comparable) {
        return new SpecializedFormValidationAsserter(
                isGreaterThanDouble(comparable, FormValidation.Kind.ERROR, "Must be greater than " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isLessThanDouble(final double comparable) {
        return new SpecializedFormValidationAsserter(
                isLessThanDouble(comparable, FormValidation.Kind.ERROR, "Must be less than " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isEqualsDouble(final double comparable) {
        return new SpecializedFormValidationAsserter(
                isEqualsDouble(comparable, FormValidation.Kind.ERROR, "Must be equal to " + comparable)
        );
    }

    public SpecializedFormValidationAsserter isPositiveDouble() {
        return new SpecializedFormValidationAsserter(
                isPositiveDouble(FormValidation.Kind.ERROR, "Must be a positive number")
        );
    }

    public SpecializedFormValidationAsserter isNonPositiveDouble() {
        return new SpecializedFormValidationAsserter(
                isNonPositiveDouble(FormValidation.Kind.ERROR, "Must be a non-positive number")
        );
    }

    public SpecializedFormValidationAsserter isNegativeDouble() {
        return new SpecializedFormValidationAsserter(
                isNegativeDouble(FormValidation.Kind.ERROR, "Must be a negative number")
        );
    }

    public SpecializedFormValidationAsserter isNonNegativeDouble() {
        return new SpecializedFormValidationAsserter(
                isNonNegativeDouble(FormValidation.Kind.ERROR, "Must be a non-negative number")
        );
    }

    public SpecializedFormValidationAsserter isSshPrivateKey() {
        return contains("-----BEGIN RSA PRIVATE KEY-----")
                .contains("-----END RSA PRIVATE KEY-----");
    }

}
