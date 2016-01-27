import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/*
 * This Java source file was auto generated by running 'gradle buildInit --type java-library'
 * by 'crassirostris' at '16. 1. 20 오전 12:06' with Gradle 2.8
 *
 * @author crassirostris, @date 16. 1. 20 오전 12:06
 */
public class Library {
    private String searchUrl = "http://www.tosarang2.net/bbs/board.php?bo_table=torrent_kortv_drama&sca=&sop=and&sfl=wr_subject&stx=";
    private String downloadUrl = "https://m.torrentkim1.net/";
    private BotMode mode = BotMode.NONE;
    private long chatId=0;
    private String token = "";
    private int offset = 0;
    TelegramBot bot;
    private List<Content> contents;
    private String pwd = "";

    // commend
    private static final String SEARCH_TORRENT = "Search Torrent";
    private static final String DELETE_DONE_TORRENT = "Delete done torrent";
    private static final String SHOW_STATUS = "Show status";

    public Library(String[] args) throws IOException {
        if (args.length > 0 && !"".equals(args[0])) {
            token = args[0];
        }
        if (args.length > 1 && !"".equals(args[1])) {
            token = args[1];
        }

        bot = TelegramBotAdapter.build(token);
    }

    public static final void main(String[] args) throws InterruptedException, IOException {
        Library run = new Library(args);
        run.run();
    }

    private void run() throws InterruptedException, IOException {
        System.out.println("Listen");
        while (true) {
            try {
                List<Update> updates = bot.getUpdates(offset, 50, 300).updates();
                if (updates.size() != 0) {
                    for (Update update : updates) {
                        offset = update.updateId() + 1;
                        Message message = update.message();
                        User from = message.from();
                        if (from.id() != 124562360) continue;

                        String text = message.text();
                        chatId = message.chat().id();
                        if (!"".equals(text)) {
                            resolveCommand(text);
                        }

                    }
                }
            } catch (Exception e) {
                // nothing
            }

            Thread.sleep(1000);
        }
    }

    private void resolveCommand(String text) {
        switch (text) {
            case SEARCH_TORRENT:
                bot.sendMessage(chatId, "input sentence");
                mode = BotMode.INPUT_KEYWORD;
                break;
            case DELETE_DONE_TORRENT:
                bot.sendMessage(chatId, "input number");
                mode = BotMode.INPUT_DELETE_DONE_NUMBER;
                break;
            case SHOW_STATUS:
                showStatus();
                mode = BotMode.NONE;
                break;
            default:
                switch (mode) {
                    case INPUT_KEYWORD:
                        search(text);


                        break;
                    case CHOOSE:
                        selectedOne(text);
                        break;
                    case INPUT_DELETE_DONE_NUMBER:
                        deleteTorrent(text);
                        break;
                    default:
                        sendMayIHelpYou();

                        break;
                }
                break;
        }
    }

    private void showStatus() {
        String command = "transmission-remote -l > a";
        try {
            runShell(command);

            Path a = Paths.get("a");
            List<String> strings = Files.readAllLines(a);

            bot.sendMessage(chatId, Joiner.on("\\n").join(strings));
            String deleteStatus = "rm a";
            runShell(deleteStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTorrent(String text) {
        String command = "transmission-remote -t " + text + " -r";
        try {
            runShell(command);
            bot.sendMessage(chatId, "delete OK!");
        } catch (IOException e) {
            bot.sendMessage(chatId, "Fail Delete, try again");
        }
        mode = BotMode.NONE;
        sendMayIHelpYou();
    }

    private void search(String text) {
        bot.sendMessage(chatId, "Searching..");
        try {
            Document document = null;
            Connection connect = Jsoup.connect(searchUrl + text);
            connect.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36");
            document = connect.get();
            Elements select = document.select("div#bo_l_list table>tbody>tr>td.td_subject");
            contents = select.stream().map((element1 ->
                    makeContent(element1)
            )).filter((el -> !"".equals(el.getTitle()) && !"".equals(el.getLink()) && el.getTitle().contains("720"))).collect(Collectors.toList());
        } catch (IOException e) {
            bot.sendMessage(chatId, "Fail Search");
            mode = BotMode.NONE;
            sendMayIHelpYou();
            return;
        }


        showSearchResult();

    }

    private void selectedOne(String text) {
        contents.stream().filter((content -> content.getTitle().equals(text))).findFirst().ifPresent(content1 -> downloadMagnet(content1.getLink()));
    }

    private Document getDocument(String url) throws IOException {
        Connection connect = Jsoup.connect(url);
        connect.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36");
        return connect.get();
    }

    private void downloadMagnet(String link ) {
        try {
            Document document = getDocument(link);

            Element select = document.select("div.bo_v_file a").last();
            String downUrl = select.attr("href");
            System.out.println(downUrl);

            if (!downUrl.startsWith("magnet:")) {
                bot.sendMessage(chatId, "invalid margnet link");
                mode = BotMode.NONE;
                sendMayIHelpYou();
                System.out.println(downUrl);
                return;
            }
            //String command = "transmission-remote -a " + downUrl;
            String command = "echo download " + downUrl;
            runShell(command);


        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Fail download torrent");
            mode = BotMode.NONE;
            sendMayIHelpYou();
            return;
        }
        bot.sendMessage(chatId, "Starting torrent");
        mode = BotMode.NONE;
        sendMayIHelpYou();
    }

    private void runShell(String command) throws IOException {
        Process child = Runtime.getRuntime().exec(command);
    }

    private void sendMayIHelpYou() {


        bot.sendMessage(chatId, "May I help you?", ParseMode.Markdown,        // Markdown text or null
                false,                     // disable_web_page_preview
                0,            // reply_to_message_id
                new ReplyKeyboardMarkup(new String[][]{{SEARCH_TORRENT}, {DELETE_DONE_TORRENT}, {SHOW_STATUS}}).oneTimeKeyboard(true));
    }

    private void showSearchResult() {
        if (contents.size() == 0) {
            bot.sendMessage(chatId, "검색결과가 없습니다");
            sendMayIHelpYou();
            return;
        }


        List<String[]> strings = Lists.newArrayList();
        for (Content content : contents) {
            strings.add(new String[]{content.getTitle()});
        }


        String[][] messages = makeKeyboard(contents.stream().map(c -> c.getTitle()).toArray(String[]::new));
        bot.sendMessage(chatId, "Choose Torrent", ParseMode.Markdown,        // Markdown text or null
                false,                     // disable_web_page_preview
                0,            // reply_to_message_id
                new ReplyKeyboardMarkup(messages).oneTimeKeyboard(true));
        mode = BotMode.CHOOSE;
    }

    private String[][] makeKeyboard(String[] titles) {
        switch (titles.length) {
            case 1:
                String[][] key= {{titles[0]}};
                return key;
            case 2:
                String[][] key2= {{titles[0]}, {titles[1]}};
                return key2;
            case 3:
                String[][] key3= {{titles[0]}, {titles[1]}, {titles[2]}};
                return key3;
            case 4:
                String[][] key4= {{titles[0]}, {titles[1]}, {titles[2]}, {titles[3]}};
                return key4;
            default:
                String[][] key5= {{titles[0]}, {titles[1]}, {titles[2]}, {titles[3]}, {titles[4]}};
                return key5;

        }
    }

    private Content makeContent(Element element) {

        String title = element.text();
        String link = element.select("a").attr("href");

        return Content.create(title, link);
    }
}