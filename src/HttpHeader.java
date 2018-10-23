import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.List;

/**
 * @author chen
 *
 */
public class HttpHeader {
    public static String delimiter = "\r\n";
    public final List<String> headline;
    private final String method;
    private final String targetURL;
    private String targetHost = null;
    private String cookie = null;


    /**
     * 
     */
    public HttpHeader(List<String> headline) {
        this.headline = headline;
        method = getMethod(headline.get(0));
        targetURL = getURL(headline.get(0));
        setHostandCookie(headline);
    }

    private String getURL(String string) {
        String[] line = string.split(" ");
        return line[1];
    }

    private String getMethod(String string) {
        if (string.contains("GET")) {
            return "GET";
        }
        if (string.contains("POST")) {
            return "POST";
        }
        return null;
    }

    // TODO modify

    private void setHostandCookie(List<String> headline) {
        for (String string : headline) {
            if (string.length() <= 3) {
                continue;
            }
            String buffer = string.substring(0, 3);
            switch (buffer) {
                case "Hos":
                    targetHost = string.substring(6);
                    break;

                case "Coo":
                    cookie = string.substring(8);
                    break;

                default:
                    break;
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getTargetURL() {
        return targetURL;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public String getCookie() {
        return cookie;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String s = "";
        for (String string : headline) {
            s = s + string + delimiter;
        }
        return s;
    }

    public static void main(String[] args) {
        // String teString = "GET http://jwts.hit.edu.cn/ HTTP/1.1\r\n" + "\r\n"
        // + "Host: jwts.hit.edu.cn\r\n" + "\r\n"
        // + "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64; rv:62.0) Gecko/20100101
        // Firefox/62.0\r\n"
        // + "\r\n"
        // + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n"
        // + "\r\n"
        // + "Accept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2\r\n"
        // + "\r\n" + "Accept-Encoding: gzip, deflate\r\n" + "\r\n"
        // + "Cookie: _ga=GA1.3.1811562551.1521426796; name=value;
        // JSESSIONID=LvfRbJWQVSmWvWJRT1H18CG6jnFjWYmsJMXMPRlJFv1ZfNzxHmfQ!-102192215;
        // clwz_blc_pst=184553644.24859\r\n"
        // + "\r\n" + "Connection: keep-alive\r\n" + "\r\n"
        // + "Upgrade-Insecure-Requests: 1\r\n" + "\r\n" + "Cache-Control: max-age=0";
        // HttpHeader httpHeader = new HttpHeader(teString);
        // System.out.println(httpHeader.getMethod());
        // System.out.println(httpHeader.getTargetURL());
        // System.out.println(httpHeader.getTargetHost());
        // System.out.println(httpHeader.getCookie());
    }


}
