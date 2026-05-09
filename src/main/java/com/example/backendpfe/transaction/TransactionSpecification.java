package com.example.backendpfe.transaction;

import org.springframework.data.jpa.domain.Specification;

public class TransactionSpecification {

    public static Specification<Transaction> hasAccountId(Long accountId) {
        return (root, query, cb) -> {
            if (accountId == null) return null;

            return cb.or(
                    cb.equal(root.get("sourceAccount").get("idAccount"), accountId),
                    cb.equal(root.get("destinationAccount").get("idAccount"), accountId)
            );
        };
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Transaction> hasChannel(TransactionChannel channel) {
        return (root, query, cb) -> {
            if (channel == null) return null;
            return cb.equal(root.get("channel"), channel);
        };
    }
}