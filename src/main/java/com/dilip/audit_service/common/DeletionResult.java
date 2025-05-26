package com.dilip.audit_service.common;

public sealed interface DeletionResult
        permits DeletionResult.DeletionSuccess, DeletionResult.DeletionNotFound, DeletionResult.DeletionFailure {

    record DeletionSuccess(long deletedCount) implements DeletionResult {}

    record DeletionNotFound(String message) implements DeletionResult {}

    record DeletionFailure(String reason, Throwable exception) implements DeletionResult {}
}