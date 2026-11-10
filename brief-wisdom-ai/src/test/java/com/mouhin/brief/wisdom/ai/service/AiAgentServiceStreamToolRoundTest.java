package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.exception.AIException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AiAgentService 工具调用循环的逐 Token 流式测试
 * <p>
 * 验证工具轮执行后仍能把最终答案按分片实时下发，而不是整段返回。
 *
 * @author Brief-Wisdom
 * @date 2026-11-10
 */
@DisplayName("AiAgentService 工具循环逐 Token 流式测试")
class AiAgentServiceStreamToolRoundTest {

    private static final ChatOptions CHAT_OPTIONS = ChatOptions.builder().build();

    /**
     * 按脚本逐轮返回流式分片，并记录每轮请求携带的消息列表
     */
    private static final class ScriptedChatModel implements ChatModel {

        private final List<List<ChatResponse>> scriptedRounds = new ArrayList<>();

        private final List<List<Message>> capturedMessages = new ArrayList<>();

        private void addRound(List<ChatResponse> responses) {
            scriptedRounds.add(responses);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("逐 Token 流式路径不应使用 call()");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            int round = capturedMessages.size();
            capturedMessages.add(new ArrayList<>(prompt.getInstructions()));
            if (round >= scriptedRounds.size()) {
                return Flux.empty();
            }
            return Flux.fromIterable(scriptedRounds.get(round));
        }
    }

    private static ChatResponse textChunk(String text) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content(text).build())));
    }

    private static ChatResponse toolCallChunk(String id, String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private static ToolCallback stubTool(String name, String result, AtomicInteger invocations) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description("测试工具 " + name)
                .inputSchema("{}")
                .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String toolInput) {
                invocations.incrementAndGet();
                return result;
            }
        };
    }

    private static List<Message> initialMessages() {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是测试助手"));
        messages.add(new UserMessage("帮我查一下"));
        return messages;
    }

    @Test
    @DisplayName("无工具调用时按分片实时下发，不合并成整段")
    void streamsTextChunksWithoutToolCall() {
        ScriptedChatModel chatModel = new ScriptedChatModel();
        chatModel.addRound(List.of(textChunk("你好"), textChunk("，我是"), textChunk("Brief-Wisdom")));

        List<String> chunks = AiAgentService.streamToolRound(chatModel, initialMessages(), CHAT_OPTIONS,
                new ToolCallback[0], 0).collectList().block();

        assertEquals(3, chunks.size(), "应实时收到 3 个文本分片");
        assertEquals("你好，我是Brief-Wisdom", String.join("", chunks));
        assertEquals(1, chatModel.capturedMessages.size(), "无工具调用时只请求一轮");
    }

    @Test
    @DisplayName("工具轮执行后进入下一轮，最终答案仍逐分片下发")
    void executesToolRoundThenStreamsFinalAnswer() {
        ScriptedChatModel chatModel = new ScriptedChatModel();
        chatModel.addRound(List.of(toolCallChunk("call-1", "queryDatabase", "{\"sql\":\"SELECT 1\"}")));
        chatModel.addRound(List.of(textChunk("查询结果"), textChunk("是 1")));

        AtomicInteger invocations = new AtomicInteger();
        ToolCallback[] callbacks = {stubTool("queryDatabase", "1", invocations)};

        List<String> chunks = AiAgentService.streamToolRound(chatModel, initialMessages(), CHAT_OPTIONS,
                callbacks, 0).collectList().block();

        assertEquals(2, chunks.size(), "最终答案应分成 2 个分片");
        assertEquals("查询结果是 1", String.join("", chunks));
        assertEquals(1, invocations.get(), "工具应被执行一次");
        assertEquals(2, chatModel.capturedMessages.size(), "应发起两轮模型请求");

        List<Message> secondRound = chatModel.capturedMessages.get(1);
        assertEquals(4, secondRound.size(), "第二轮应追加助手工具调用消息和工具结果消息");
        assertInstanceOf(AssistantMessage.class, secondRound.get(2));
        assertTrue(((AssistantMessage) secondRound.get(2)).hasToolCalls(), "助手消息需保留工具调用");

        ToolResponseMessage toolResponse = assertInstanceOf(ToolResponseMessage.class, secondRound.get(3));
        assertEquals("call-1", toolResponse.getResponses().get(0).id());
        assertEquals("1", toolResponse.getResponses().get(0).responseData());
    }

    @Test
    @DisplayName("首轮既无文本又无工具调用时返回空响应异常")
    void failsWhenFirstRoundIsEmpty() {
        ScriptedChatModel chatModel = new ScriptedChatModel();
        chatModel.addRound(List.of());

        assertThrows(AIException.class, () -> AiAgentService.streamToolRound(chatModel, initialMessages(),
                CHAT_OPTIONS, new ToolCallback[0], 0).collectList().block());
    }
}
