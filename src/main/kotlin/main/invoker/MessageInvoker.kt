package main.invoker

import main.domain.Cycle
import main.domain.Results

interface MessageInvoker {
    fun invoke(cycle: Cycle): Results

    fun shutdown()
}