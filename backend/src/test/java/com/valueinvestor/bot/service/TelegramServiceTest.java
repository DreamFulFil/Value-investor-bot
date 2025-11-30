package com.valueinvestor.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    @InjectMocks
    private TelegramService telegramService;

    @Test
    void sendMessage_shouldSendWhenConfigured() {
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isOk()).thenReturn(true);

        try (MockedConstruction<TelegramBot> mocked = mockConstruction(TelegramBot.class,
                (mock, context) -> when(mock.execute(any(SendMessage.class))).thenReturn(sendResponse))) {

            telegramService.sendMessage("test-token", "test-chat-id", "Hello");

            verify(mocked.constructed().get(0)).execute(any(SendMessage.class));
        }
    }

    @Test
    void sendMessage_shouldNotSendWhenTokenOrChatIdIsBlank() {
        try (MockedConstruction<TelegramBot> mocked = mockConstruction(TelegramBot.class)) {
            telegramService.sendMessage(null, "test-chat-id", "Hello");
            telegramService.sendMessage("", "test-chat-id", "Hello");
            telegramService.sendMessage("test-token", null, "Hello");
            telegramService.sendMessage("test-token", " ", "Hello");
            telegramService.sendMessage("YOUR_BOT_TOKEN", "test-chat-id", "Hello");

            verify(mocked.constructed().size(), never()).execute(any());
        }
    }

    @Test
    void sendMessage_shouldLogWhenApiFails() {
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isOk()).thenReturn(false);
        when(sendResponse.description()).thenReturn("Error Description");


        try (MockedConstruction<TelegramBot> mocked = mockConstruction(TelegramBot.class,
                (mock, context) -> when(mock.execute(any(SendMessage.class))).thenReturn(sendResponse))) {

            telegramService.sendMessage("test-token", "test-chat-id", "Hello");

            verify(mocked.constructed().get(0)).execute(any(SendMessage.class));
        }
    }
}
