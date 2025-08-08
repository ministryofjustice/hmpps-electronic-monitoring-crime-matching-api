package org.example.athenajdbcpoc.config

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionStatus

class DummyTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
        return DefaultTransactionStatus(null, null, false, false, false, false, false, null)
    }

    override fun commit(status: TransactionStatus) {
    }

    override fun rollback(status: TransactionStatus) {
    }
}