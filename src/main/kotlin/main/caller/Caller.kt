package main.caller

import main.domain.Results

interface Caller {
    fun call(): Results
}