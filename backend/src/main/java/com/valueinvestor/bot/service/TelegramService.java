package com.valueinvestor.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    public void sendMessage(String botToken, String chatId, String text) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank() || botToken.equals("YOUR_BOT_TOKEN")) {
            logger.warn("Telegram bot token or chat ID is not configured. Skipping message.");
            return;
        }

        try {
            TelegramBot bot = new TelegramBot(botToken);
            SendMessage request = new SendMessage(chatId, text).disableWebPagePreview(true);
            SendResponse response = bot.execute(request);

            if (!response.isOk()) {
                logger.error("Failed to send Telegram message: {}", response.description());
            } else {
                logger.info("Successfully sent Telegram message.");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while sending Telegram message", e);
        }
    }
}
