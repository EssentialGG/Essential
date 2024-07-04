/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package org.ice4j.attribute;

import org.ice4j.StunException;
import org.ice4j.message.Message;

import java.time.Duration;
import java.util.Objects;

/**
 * https://datatracker.ietf.org/doc/rfc7982/
 */
public class TransactionTransmitCounterAttribute extends Attribute {
    public static final String NAME = "TRANSACTION-TRANSMIT-COUNTER";
    public static final char TYPE = 0x8025;
    public static final int DATA_LENGTH = 4;

    public int req;
    public int resp;
    public transient Duration rtt;

    public TransactionTransmitCounterAttribute() {
        super(TYPE);
    }

    @Override
    public char getDataLength() {
        return DATA_LENGTH;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionTransmitCounterAttribute that = (TransactionTransmitCounterAttribute) o;
        return req == that.req && resp == that.resp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(req, resp);
    }

    @Override
    public byte[] encode() {
        byte[] bytes = new byte[HEADER_LENGTH + DATA_LENGTH];

        // Type
        bytes[0] = (byte) (getAttributeType() >> 8);
        bytes[1] = (byte) (getAttributeType() & 0x00ff);
        // Length
        bytes[2] = (byte) (getDataLength() >> 8);
        bytes[3] = (byte) (getDataLength() & 0x00ff);
        // Data
        bytes[4] = (byte) 0;
        bytes[5] = (byte) 0;
        bytes[6] = (byte) req;
        bytes[7] = (byte) resp;

        return bytes;
    }

    @Override
    void decodeAttributeBody(byte[] attributeValue, char offset, char length) throws StunException {
        if (length != DATA_LENGTH) {
            throw new StunException("length invalid: " + length);
        }

        req = attributeValue[offset + 2] & 0xff;
        resp = attributeValue[offset + 3] & 0xff;
    }

    public static TransactionTransmitCounterAttribute get(Message message) {
        return (TransactionTransmitCounterAttribute) message.getAttribute(TYPE);
    }
}
