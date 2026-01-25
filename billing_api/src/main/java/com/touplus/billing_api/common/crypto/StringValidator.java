package com.touplus.billing_api.common.crypto;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

public class StringValidator implements Validator<String> {


    @Override
    public Clock getClock() {
        return Validator.super.getClock();
    }

    @Override
    public TemporalAmount getTimeToLive() {
        return java.time.Duration.ofDays(365 * 100);
    }

    @Override
    public TemporalAmount getMaxClockSkew() {
        return Validator.super.getMaxClockSkew();
    }

    @Override
    public Predicate<String> getObjectValidator() {
        return Validator.super.getObjectValidator();
    }

    @Override
    public Function<byte[], String> getTransformer() {
        return bytes ->  new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String validateAndDecrypt(Key key, Token token) {
        return Validator.super.validateAndDecrypt(key, token);
    }

    @Override
    public String validateAndDecrypt(Collection<? extends Key> keys, Token token) {
        return Validator.super.validateAndDecrypt(keys, token);
    }

}
