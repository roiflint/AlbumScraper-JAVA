import javax.mail.*;
import java.io.File;
import org.jsoup.Jsoup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.mail.internet.InternetAddress;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class Engine implements engineInter {
    private String URL = "https://en.wikipedia.org/wiki/";
    private String mailAccount;
    private String mailPassword;
    private String directory;
    private String recipient;
    private ArrayList<String> missingArtists;

    public Engine() throws IOException, ParserConfigurationException, SAXException {
        File file = new File("config.xml");
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        org.w3c.dom.Document document = documentBuilder.parse(file);
        this.directory =  document.getElementsByTagName("Directory").item(0).getTextContent();;
        this.mailAccount =  document.getElementsByTagName("mailAccount").item(0).getTextContent();
        this.mailPassword =  document.getElementsByTagName("mailPassword").item(0).getTextContent();
        this.recipient = document.getElementsByTagName("mailRecipient").item(0).getTextContent();
        this.missingArtists = new ArrayList<String>();
    }

    //receive artist name, returns url to wikipedia
    private String buildURL(String artist){
        String name = "";
        String[] splittedartist = artist.split(" ");
        for (String s : splittedartist){
            String str = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
            name += str + "_";
        }
        name = name.substring(0, name.length() - 1);
        return URL+name;
    }

    //receive url to wikipedia of an artist, return artist's discography
    private ArrayList<String> getAlbumsByURL(String URL){
        try {
            boolean check1 = false;
            boolean check2 = false;
            boolean dataInTable = false;
            ArrayList<String> albums = new ArrayList<String>();
            Document document = null;
            document = Jsoup.connect(URL).get();
            Elements info = document.getAllElements();//document.select("ul");
            for (Element element : info) {
                if (element.id().equalsIgnoreCase("Discography")) {
                    check1 = true;
                }
                if (check1 && element.tagName().equalsIgnoreCase("ul")) {
                    check2 = true;
                }
                else if(check1 && element.tagName().equalsIgnoreCase("table")){
                    dataInTable = true;
                }
                if (check2 && !dataInTable && element.tagName().equalsIgnoreCase("li")) {
                    check1 = false;
                    albums.add(element.text().toUpperCase());
                }
                else if(check2 && dataInTable && element.tagName().equalsIgnoreCase("i")){
                    check1 = false;
                    albums.add(element.text().toUpperCase());
                }
                else if (check2 && (!check1 && element.tagName().equalsIgnoreCase("h2") || !check1 && element.tagName().equalsIgnoreCase("h3"))) {
                    return albums;
                }
            }
        }
        catch (IOException e) {
            return null;
        }
        return null;
    }

    //send mail
    private void sendMail(String mailBody) throws MessagingException {
        String mailAccount = this.mailAccount;
        String mailPassword = this.mailPassword;
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(mailAccount, mailPassword);
            }
        });

        Message message = prepareMessage(session, mailAccount, this.recipient, mailBody);
        Transport.send(message);
    }

    //mail message constructor
    private Message prepareMessage(Session session, String mailAccount, String recipient, String mailBody) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailAccount));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("Album Scraper - Results");
            message.setText(mailBody);
            return message;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    //traverse over all artists
    private String traverseDirectory() throws IOException {
        String albums = "";
        String path = "";
        File directoryPath = new File(this.directory);
        File[] fileList = directoryPath.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if(file.isDirectory()) {
                    path = buildURL(file.getName());
                    albums += returnDelta(file.getName(), path);
                }
            }
        }
        if(this.missingArtists.size() > 0){
            albums += "Error searching for artists:\n\n";
            for(String missing : this.missingArtists){
                albums += missing + "\n";
            }
        }

        return albums;
    }

    //receive artist name and path, returns delta between wikipedia and directory albums
    private String returnDelta(String artistName, String albumDirectory) throws IOException {
        String delta = "";
        boolean found = false;
        ArrayList<String> wiki = getAlbumsByURL(albumDirectory);
        if(wiki == null){
            albumDirectory += "_(band)";
            wiki = getAlbumsByURL(albumDirectory);
        }
        if(wiki == null) {
            this.missingArtists.add(artistName);
            return "";
        }
        else {
            ArrayList<String> direct = getArtistAlbums(this.directory + "/" + artistName);
            for (String album : wiki) {
                found = false;
                for (String existing : direct) {
                    if (album.contains(existing)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    delta += album + "\n";
                }
            }
            if (delta.length() > 0) {
                delta += "\n--------------------------------------------------\n";
                return artistName + " - missing albums:\n\n" + delta;
            } else {
                return "";
            }
        }
    }

    //receive path to artist, returns albums in path
    private ArrayList<String> getArtistAlbums(String path){
        ArrayList<String> albums = new ArrayList<String>();
        File directoryPath = new File(path);
        File[] fileList = directoryPath.listFiles();
        for(File file : fileList){
            albums.add(file.getName().toUpperCase());
        }
        return albums;
    }

    //wrapper
    @Override
    public void run() throws IOException, MessagingException {
        String message = traverseDirectory();
        sendMail(message);

    }
}
