package main.verify;

import common.datastore.Pair;

import java.util.Map;

class VerifyData {
    private final Map<Integer, Pair<Order, Short>> results;

    VerifyData(Map<Integer, Pair<Order, Short>> results) {
        this.results = results;
    }
}