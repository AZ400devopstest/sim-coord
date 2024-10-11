package com.tzccoordinatorpackage;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Record {
    private long transactionDate;
    private ObjectNode jsonObject;

    public Record(long transactionDate, ObjectNode jsonObject) {
        this.transactionDate = transactionDate;
        this.jsonObject = jsonObject;
    }

    public long getTransactionDate() {
        return transactionDate;
    }

    public ObjectNode getJsonObject() {
        return jsonObject;
    }
}