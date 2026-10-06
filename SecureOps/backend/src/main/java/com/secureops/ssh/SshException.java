package com.secureops.ssh;

/** Message is user-safe (no secrets, no stack traces). */
public class SshException extends Exception {
    public SshException(String message) { super(message); }
    public SshException(String message, Throwable cause) { super(message, cause); }
}
