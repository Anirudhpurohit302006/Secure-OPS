package com.secureops.ssh;

/** A temporary SSH session. Only {@link ApprovedCommand}s can be executed. */
public interface RemoteSession extends AutoCloseable {
    CommandResult run(ApprovedCommand command) throws SshException;

    @Override
    void close();
}
