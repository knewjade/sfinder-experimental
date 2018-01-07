package main.caller

import main.percent.ResultsSerializer
import main.aws.AWS
import main.domain.Cycle
import main.domain.ResultPath
import main.domain.Results
import main.invoker.MessageInvoker

class InvokerCaller(
        private val aws: AWS,
        private val invoker: MessageInvoker,
        private val path: ResultPath,
        private val cycle: Cycle,
        private val serializer: ResultsSerializer
) : Caller {
    override fun call(): Results {
        val results = invoker.invoke(cycle)
        val content = serializer.str(results)
        aws.putObject(path.path, content)
        return results
    }
}