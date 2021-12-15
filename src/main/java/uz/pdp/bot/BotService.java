package uz.pdp.bot;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.db.DB;
import uz.pdp.model.TgUser;
import uz.pdp.model.TwilioVerification;
import uz.pdp.model.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotService {
    private static final String TWILIO_SID = "ACbeaa0f98579383276de9df2b863b1e0b";
    private static final String TWILIO_TOKEN = "1c550303b910201de10e1755f0f7abdb";
    private static final String TWILIO_PHONE = "+12185178429";

    public static String getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId().toString();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId().toString();
        }
        return "";
    }

    public static TgUser getOrCreateTgUser(String chatId) {
        for (TgUser user : DB.tgUsers) {
            if (user.getChatId().equals(chatId)) {
                return user;
            }
        }
        TgUser user = new TgUser(chatId, BotState.START);
        DB.tgUsers.add(user);
        return user;
    }

    public static TwilioVerification getOrCreateTwilioVerification(TgUser user) {
        for (TwilioVerification verification : DB.twilioVerificationList) {
            if (verification.getUser().getChatId().equals(user.getChatId())) {
                return verification;
            }
        }
        TwilioVerification verification = new TwilioVerification(user);
        DB.twilioVerificationList.add(verification);
        return verification;
    }

    public static void saveUserChanges(TgUser changedUser) {
        for (TgUser user : DB.tgUsers) {
            if (user.getChatId().equals(changedUser.getChatId())) {
                user = changedUser;
            }
        }
    }

    public static void saveVerificationChanges(TwilioVerification changedVerification) {
        for (TwilioVerification verification : DB.twilioVerificationList) {
            if (verification.getId().equals(changedVerification.getId())) {
                verification = changedVerification;
            }
        }
    }

    public static SendMessage start(Update update) {
        String chatId = getChatId(update);
        TgUser user = getOrCreateTgUser(chatId);
        if (user.getState().equals(BotState.START)) {
            return chooseLang(update);
        } else {
            return showMenu(user);
        }

    }

    public static SendMessage chooseLang(Update update) {
        String chatId = getChatId(update);
        TgUser user = getOrCreateTgUser(chatId);
        user.setState(BotState.CHOOSE_LANG);
        saveUserChanges(user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Til tanlang . Choose language.");
        sendMessage.setReplyMarkup(generateReplyKeyboardMarkup(user));
        return sendMessage;
    }

    public static ReplyKeyboardMarkup generateReplyKeyboardMarkup(TgUser user) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton row1Button1 = new KeyboardButton();
        if (user.getState().equals(BotState.CHOOSE_LANG)
                ||
                user.getState().equals(BotState.CHANGE_LANG)
        ) {
            row1Button1.setText("O'zbek");
            row1.add(row1Button1);
            KeyboardButton row1Button2 = new KeyboardButton();
            row1Button2.setText("English");
            row1.add(row1Button2);
            rowList.add(row1);
        } else if (user.getState().equals(BotState.SHARE_CONTACT)) {
            row1Button1.setText(user.getLan().equals(Language.UZ) ?
                    "JO'NATISH"
                    : "SHARE");
            row1Button1.setRequestContact(true);
            row1.add(row1Button1);
            rowList.add(row1);
        } else if (user.getState().equals(BotState.ENTER_CODE)) {
            row1Button1.setText(user.getLan().equals(Language.UZ) ?
                    "Qayta kod jo'natish." : "Resend code");
            row1.add(row1Button1);
            rowList.add(row1);
        }
        markup.setKeyboard(rowList);
        markup.setSelective(true);
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static SendMessage getUserLang(TgUser user, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        if (text.equals("O'zbek")) {
            user.setLan(Language.UZ);
        } else if (text.equals("English")) {
            user.setLan(Language.ENG);
        } else {
            sendMessage.setText("Xato tanlov. Error choose");
            return sendMessage;
        }
        if (user.getState().equals(BotState.CHOOSE_LANG)) {
            user.setState(BotState.SHARE_CONTACT);
            sendMessage.setText(user.getLan().equals(Language.UZ) ?
                    "Iltimos, Tel raqamingizni \"JO'NATISH\" tugmasi orqali yuboring."
                    : "Please, share your phoneNumber by pressing \"SHARE\" button.");
            sendMessage.setReplyMarkup(generateReplyKeyboardMarkup(user));
        } else if (user.getState().equals(BotState.CHANGE_LANG)) {
            user.setState(BotState.SHOW_MENU);
            return showMenu(user);
        }
        saveUserChanges(user);
        return sendMessage;
    }

    public static SendMessage getContact(Contact contact, TgUser user) {
        String phoneNumber = checkPhoneNumber(contact.getPhoneNumber());
        user.setPhoneNumber(phoneNumber);
        boolean sendCode = sendCode(user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        if (sendCode) {
            user.setState(BotState.ENTER_CODE);
            sendMessage.setText(user.getLan().equals(Language.UZ) ?
                    "Sizning tel raqamingizga kod jo'natildi. Iltimos, tasdiqlash kodini kiriting : " : "Code was sent to your phone.Please, enter verification code : ");
            sendMessage.setReplyMarkup(generateReplyKeyboardMarkup(user));
        } else {
            //TODO ERROR ON SENDING CODE
        }
        saveUserChanges(user);
        return sendMessage;
    }

    public static String checkPhoneNumber(String phoneNumber) {
        return phoneNumber.startsWith("+") ? phoneNumber : "+" + phoneNumber;
    }

    public static boolean sendCode(TgUser user) {
        Twilio.init(TWILIO_SID, TWILIO_TOKEN);
        String code = String.valueOf((int)((Math.random() * (999999 -100000)) + 100000));
        try {
            Message message = Message.creator(
                    new PhoneNumber(user.getPhoneNumber()),
                    new PhoneNumber(TWILIO_PHONE),
                    user.getLan().equals(Language.UZ) ?
                            "Tasdiqlash kodi : " + code : "Verification code : " + code).create();
            TwilioVerification verification = getOrCreateTwilioVerification(user);
            verification.setCode(code);
            verification.setVerified(false);
            saveVerificationChanges(verification);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean getVerifiedCode(Update update, TgUser user) {
        TwilioVerification verification = getOrCreateTwilioVerification(user);
        if (verification.getCode().equals(update.getMessage().getText())) {
            verification.setVerified(true);
            saveVerificationChanges(verification);
            user.setState(BotState.SHOW_MENU);
            saveUserChanges(user);
            return true;
        } else {
            return false;
        }
    }

    public static SendMessage showMenu(TgUser user) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        sendMessage.setText(user.getLan().equals(Language.UZ) ?
                "Menu tanlang : " : "Choose menu");
        sendMessage.setReplyMarkup(generateInlineKeyboardMarkup(user));
        user.setState(BotState.SHOW_MENU);
        saveUserChanges(user);
        return sendMessage;
    }

    private static InlineKeyboardMarkup generateInlineKeyboardMarkup(TgUser user) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton row1Button1 = new InlineKeyboardButton();
        if (user.getState().equals(BotState.SHOW_MENU)) {
            row1Button1.setText(user.getLan().equals(Language.UZ)?
                    "Mahsulotlar":"Products");
            row1Button1.setCallbackData("PRODUCTS");
            row1.add(row1Button1);
            rowList.add(row1);

            List<InlineKeyboardButton> row2=new ArrayList<>();
            InlineKeyboardButton row2Button1 = new InlineKeyboardButton();
            row2Button1.setText(user.getLan().equals(Language.UZ)?
                    "Savatchani ko'rish":"Show basket");
            row2Button1.setCallbackData("SHOW_BASKET");
            row2.add(row2Button1);
            rowList.add(row2);

            List<InlineKeyboardButton> row3=new ArrayList<>();
            InlineKeyboardButton row3Button1 = new InlineKeyboardButton();
            row3Button1.setText(user.getLan().equals(Language.UZ)?
                    "Til o'zgartirish":"Change language");
            row3Button1.setCallbackData("CHANGE_LANGUAGE");
            row3.add(row3Button1);
            rowList.add(row3);

            List<InlineKeyboardButton> row4=new ArrayList<>();
            InlineKeyboardButton row4Button1 = new InlineKeyboardButton();
            row4Button1.setText(user.getLan().equals(Language.UZ)?
                    "Buyurtmalar tarixi":"Order history");
            row4Button1.setCallbackData("SHOW_ORDER_HISTORY");
            row4.add(row4Button1);
            rowList.add(row4);

            List<InlineKeyboardButton> row5=new ArrayList<>();
            InlineKeyboardButton row5Button1 = new InlineKeyboardButton();
            row5Button1.setText(user.getLan().equals(Language.UZ)?
                    "Bizning Ijtimoiy tarmoqlardagi linklarimiz":"Our social links");
            row5Button1.setCallbackData("SOCIAL_NETS");
            row5.add(row5Button1);
            rowList.add(row5);

            List<InlineKeyboardButton> row6=new ArrayList<>();
            InlineKeyboardButton row6Button1 = new InlineKeyboardButton();
            row6Button1.setText(user.getLan().equals(Language.UZ)?
                    "Biz haqimizda":"About us");
            row6Button1.setCallbackData("ABOUT US");
            row6.add(row6Button1);
            rowList.add(row6);
        }
        markup.setKeyboard(rowList);
        return markup;
    }

    public static SendMessage resendCode(Update update, TgUser user) {
        boolean sendCode = sendCode(user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        if (sendCode) {
            user.setState(BotState.ENTER_CODE);
            sendMessage.setText(user.getLan().equals(Language.UZ) ?
                    "Sizning tel raqamingizga kod jo'natildi. Iltimos, tasdiqlash kodini kiriting : " : "Code was sent to your phone.Please, enter verification code : ");
            sendMessage.setReplyMarkup(generateReplyKeyboardMarkup(user));
        } else {
            //TODO ERROR ON SENDING CODE
        }
        saveUserChanges(user);
        return sendMessage;
    }
}
