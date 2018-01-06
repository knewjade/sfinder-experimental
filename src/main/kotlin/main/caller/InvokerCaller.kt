package main.caller

import main.invoker.MessageInvoker
import main.aws.AWS
import main.domain.Cycle
import main.domain.ResultPath
import main.domain.Results

class InvokerCaller(
        private val aws: AWS,
        private val invoker: MessageInvoker,
        private val path: ResultPath,
        private val cycle: Cycle
) : Caller {
    override fun call(): Results {
        val results = invoker.invoke(cycle)
        put(results, path)
        return results
    }

    private fun put(results: Results, path: ResultPath) {
        val (allCount, details) = results

        if (details.isEmpty()) {
            aws.putObject(path.path, "$allCount")
        } else {
            val output = details.joinToString(";") {
                "${it.mino},${it.success},${it.fieldData}"
            }
            aws.putObject(path.path, "$allCount?$output")
        }
    }
}