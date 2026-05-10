import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.fromProcess
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

val gemma4e4bModel: LLModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "google/gemma-4-e4b",
    capabilities = listOf(
        LLMCapability.Schema.JSON.Standard,
        LLMCapability.Completion,
        LLMCapability.Tools,
        LLMCapability.ToolChoice,
        LLMCapability.Vision.Image,
        LLMCapability.OpenAIEndpoint.Completions,
        LLMCapability.OpenAIEndpoint.Responses
    )
)

class GreetToolSet : ToolSet {

    @Tool
    @LLMDescription("Greets the user")
    fun greetUser(@LLMDescription("User name") userName: String): String {
        return "Hello, my dear $userName"
    }
}

fun main(): Unit = runBlocking {
    val apiKey = "<lmstudio ignores apikeys>"
    val lmStudioUrl = "http://localhost:1234"
    val openaiConfig = OpenAIClientSettings(lmStudioUrl, ConnectionTimeoutConfig())
    val executor: PromptExecutor = MultiLLMPromptExecutor(OpenAILLMClient(apiKey, openaiConfig))
    val model = gemma4e4bModel
    val toolRegistry = ToolRegistry {
        tools(GreetToolSet())
    }

    val process = ProcessBuilder("docker", "run", "-i", "--rm", "mcp/fetch")
        .redirectErrorStream(true)
        .start()
    val mcpToolRegistry = McpToolRegistryProvider.fromProcess(process = process)

    val tools = toolRegistry + mcpToolRegistry

    // Create the runner
    val agent = AIAgent(
        promptExecutor = executor,
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = model,
        toolRegistry = tools
    )

    val answer = agent.run("Fetch https://anadea.info page in md format")
    println("Answer: $answer")
}