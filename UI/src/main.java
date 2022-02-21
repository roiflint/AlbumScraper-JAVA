import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Scanner;

public class main {

    public static void main(String[] args) {
        try {
            engineInter eng = new Engine();;
            eng.run();
        } catch (IOException e) {
            System.out.println("Error: Check directory");
        } catch (MessagingException e) {
            System.out.println("Error: Check mail account");
        } catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
}
