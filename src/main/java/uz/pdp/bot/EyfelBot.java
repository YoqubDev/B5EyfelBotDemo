package uz.pdp.bot;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.pdp.model.TgUser;
import uz.pdp.model.enums.Language;

public class EyfelBot extends TelegramLongPollingBot {
    @Override
    public String getBotUsername() {
        return "B5EyfelBot";
    }

    @Override
    public String getBotToken() {
        return "5079828169:AAEtsGzyWVefWio4JJPLlU7fvFNrmYFlpuk";
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            TgUser user = BotService.getOrCreateTgUser(BotService.getChatId(update));
            if (update.getMessage().hasContact()) {
                if (user.getState().equals(BotState.SHARE_CONTACT)) {
                    execute(BotService.getContact(update.getMessage().getContact(), user));
                }
            } else if (update.getMessage().hasLocation()) {

            } else {
                String text = update.getMessage().getText();
                if (text.equals("/start")) {
                    execute(BotService.start(update));
                } else {
                    if (user.getState().equals(BotState.CHOOSE_LANG)
                            || user.getState().equals(BotState.CHANGE_LANG)) {
                        execute(BotService.getUserLang(user, text));
                    } else if (user.getState().equals(BotState.ENTER_CODE)) {
                        if (text.equals("Qayta kod jo'natish.") ||
                                text.equals("Resend code")) {
                            user.setState(BotState.RESEND_CODE);
                            BotService.saveUserChanges(user);
                            execute(BotService.resendCode(update, user));
                        } else {
                            boolean verifiedCode = BotService.getVerifiedCode(update, user);
                            if (verifiedCode) {
                                SendMessage sendMessageRemove = new SendMessage();
                                sendMessageRemove.setChatId(user.getChatId());
                                sendMessageRemove.setText(".");
                                sendMessageRemove.setReplyMarkup(new ReplyKeyboardRemove());
                                Message message = execute(sendMessageRemove);
                                DeleteMessage deleteMessage = new DeleteMessage(user.getChatId(), message.getMessageId());
                                execute(deleteMessage);
                                execute(BotService.showMenu(BotService.getOrCreateTgUser(user.getChatId())));
                            } else {
                                SendMessage sendMessage = new SendMessage();
                                sendMessage.setChatId(user.getChatId());
                                sendMessage.setText(user.getLan().equals(Language.UZ) ?
                                        "Kiritilgan kod xato. Iltimos, to'g'ri kod kiriting." : "Error code. Please, enter correct code.");
                                execute(sendMessage);
                            }

                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.equals("PRODUCTS")) {

            }
        }
    }
}
