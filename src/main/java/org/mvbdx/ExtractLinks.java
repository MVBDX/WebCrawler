package org.mvbdx;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static java.net.URLDecoder.decode;

public class ExtractLinks {
    private static final int MAX_DEPTH = 5;
    public static final String URL_TO_CRAWL = "http://something.org/MOVIES/";
    public static final String MKV_FORMAT = ".mkv";
    public static final String MP4_FORMAT = ".mp4";


    // create set and nested list for storing links and articles
    private final HashSet<String> urlLinks;
    private List<List<String>> articles;

    // initialize set and list
    public ExtractLinks() {
        urlLinks = new HashSet<>();
        articles = new ArrayList<>();
    }

    public void getPageLinks(String URL, int depth) throws UnsupportedEncodingException, URISyntaxException {

        // we use the conditional statement to check whether we have already crawled the URL or not.
        // we also check whether the depth reaches to MAX_DEPTH or not
        if (!urlLinks.contains(URL) && (depth < MAX_DEPTH) && URL.startsWith(URL_TO_CRAWL)) { // urlLinks.size() != 50
            if (!URL.contains("?C="))
                System.out.println(">> Depth: " + depth + " ( " + extractURIName(URL) + ") [" + URL + "]");

            // use try catch block for recursive process
            try {
                // if the URL is not present in the set, we add it to the set
                urlLinks.add(URL);
                // fetch the HTML code of the given URL by using the connect() and get() method and store the result in Document
                Document doc = Jsoup.connect(URL).get();

                // we use the select() method to parse the HTML code for extracting links of other URLs and store them into Elements
                Elements availableLinksOnPage = doc.select("a[href]");

                // increase depth
                depth++;

                // for each extracted URL, we repeat above process
                for (Element ele : availableLinksOnPage) {
                    if (ele.attr("abs:href").startsWith(URL_TO_CRAWL)) {
                        // call getPageLinks() method and pass the extracted URL to it as an argument
                        getPageLinks(ele.attr("abs:href"), depth);
                    }
                }
            }
            // handle exception
            catch (IOException e) {
                // print exception messages
                System.err.println("For '" + URL + "': " + e.getMessage());
            }
        }
    }

    //Connect to each link saved in the article and find all the articles in the page
    public void getArticles() {
        Iterator<String> i = urlLinks.iterator();
        while (i.hasNext()) {
            // create variable doc that store document data
            Document doc;
            // we put the recursive code in a try-catch block
            try {
                doc = Jsoup.connect(i.next()).get();
                Elements availableArticleLinks = doc.select("a[href]");
                for (Element ele : availableArticleLinks) {
                    //we get only those article's  title which contain java 8
                    // use matches() and regx method to check whether text contains Java 8 or not
                    if (ele.text().contains("python")) {
                        System.out.println(ele.text());
                        // create temp list that stores articles
                        ArrayList<String> temp = new ArrayList<>();
                        temp.add(ele.text()); //get title of the article
                        temp.add(ele.attr("abs:href")); //get the URL of the article
                        // add article list in the nested article list
                        articles.add(temp);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void writeToFile(String fName) {
        FileWriter wr;
        try {
            wr = new FileWriter(fName, true);
            for (String strings : urlLinks) {
                try {
                    String article = extractURIName(strings) + " --> " + strings + "\n";
                    System.out.println(article);
                    wr.write(article);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            wr.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static String extractURIName(String URL) throws URISyntaxException {
        String extractedName = decode(URL, StandardCharsets.UTF_8).replace(URL_TO_CRAWL, "")
                .replace(".", " ");
        if (extractedName.length() == 0 || extractedName.length() == URL_TO_CRAWL.length()) return "";

        URI uri = new URI(URL);
        String path = uri.getPath();
        String lastPartName = path.substring(0, path.length() - 1);
        lastPartName = lastPartName.substring(lastPartName.lastIndexOf('/') + 1);

        return (lastPartName.length() != 0 ? lastPartName : "");
    }

    // main() method start
    public static void main(String[] args) throws UnsupportedEncodingException, URISyntaxException {
        System.setProperty("http.proxyHost", "tmg-2.tosanltd.com");
        System.setProperty("http.proxyPort", "8585");
        ExtractLinks obj = new ExtractLinks();
        obj.getPageLinks(URL_TO_CRAWL, 1);
        // obj.getArticles();
        obj.writeToFile("extracted.txt");
    }
}