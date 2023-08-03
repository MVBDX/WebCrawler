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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.URLDecoder.decode;

public class ExtractLinks {
    private static final int MAX_DEPTH = 5;
    public static final List<String> URL_TO_CRAWLS = List.of("http://dl.gemescape.com/film/",
            "http://dl2.gemescape.com/FILM/", "http://dl3.gemescape.com/FILM/", "http://dl4.gemescape.com/FILM/",
            "http://dl5.gemescape.com/MOVIES/"); // "https://dl.ahaang.com/02/01/01/"
    public static final List<String> IGNORE_LIST = List.of(".mkv", ".mp4", ".avi", ".srt",
            ".zip", ".rar", ".jpg", ".mp3", ".flac",
            "/ORG/", "/Trailer/", "/Dub/", "/Sub/", "/SoftSub/", "/Shot/",
            "/Blu-ray/", "/BluRay/", "/Bluray/", "/IMAX.WEB-DL/", "/WEB/", "/WEB-DL/", "/WEB.HMAX/", "/IMAX/",
            "/?C=S&O=D", "/?C=S&O=A", "/?C=M&O=D", "/?C=M&O=A", "/?C=N&O=A", "/?C=N&O=D");

    // create set and nested list for storing links and articles
    private static final HashSet<String> urlLinks = new HashSet<>();
    private static final List<List<String>> articles = new ArrayList<>();

    public static boolean isIgnoredUrl(String url) {
        for (String ignoreType : IGNORE_LIST)
            if (url.contains(ignoreType)) return true;
        return false;
    }

    public static void getPageLinks(String urls, int depth) throws UnsupportedEncodingException, URISyntaxException {

//        for (String URL_TO_CRAWL : urls) {
        // we use the conditional statement to check whether we have already crawled the URL or not.
        // we also check whether the depth reaches to MAX_DEPTH or not
        if (!urlLinks.contains(urls) && (depth < MAX_DEPTH) && urls.startsWith(urls)) { // urlLinks.size() != 50
            if (!urls.contains("?C="))
                System.out.println(">> Depth: " + depth + " ( " + extractURIName(urls) + " ) [" + urls + "]");

            // use try catch block for recursive process
            try {
                // if the URL is not present in the set, we add it to the set
                urlLinks.add(urls);
                // fetch the HTML code of the given URL by using the connect() and get() method and store the result in Document
                Document doc = Jsoup.connect(urls).get();

                // we use the select() method to parse the HTML code for extracting links of other URLs and store them into Elements
                Elements availableLinksOnPage = doc.select("a[href]");

                // increase depth
                depth++;

                // for each extracted URL, we repeat above process
                for (Element ele : availableLinksOnPage) {
                    if (ele.attr("abs:href").startsWith(urls)) {
                        // call getPageLinks() method and pass the extracted URL to it as an argument
                        if (!isIgnoredUrl(ele.attr("abs:href")))
                            getPageLinks(ele.attr("abs:href"), depth);
                    }
                }
            }
            // handle exception
            catch (IOException e) {
                // print exception messages
                System.err.println("For '" + urls + "': " + e.getMessage());
            }
        }
//        }
    }

    //Connect to each link saved in the article and find all the articles in the page
    public static void getArticles() {
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

    public static void writeToFile(String fName) {
        FileWriter wr;
        try {
            wr = new FileWriter(fName, true);
            List<String> sortedList = new ArrayList<>(urlLinks);
            Collections.sort(sortedList);
            for (String strings : sortedList) {
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
        String extractedName = decode(URL, StandardCharsets.UTF_8)//.replace(parentURL, "")
                .replace(".", " ");
        if (extractedName.length() == 0 /*|| extractedName.length() == URL.length()*/) return "";

        URI uri = new URI(URL);
        String path = uri.getPath();
        int lastIndexOfBackslash = (path.lastIndexOf("/") == path.length() - 1 ?
                path.substring(0, path.length() - 1).lastIndexOf("/") : path.lastIndexOf("/"));
        String lastPartName = (path.lastIndexOf("/") == path.length() - 1) ?
                path.substring(lastIndexOfBackslash + 1, path.length() - 1) :
                path.substring(lastIndexOfBackslash + 1);

        return (lastPartName.length() != 0 ? lastPartName : "");
    }

    public static void main(String[] args) throws UnsupportedEncodingException, URISyntaxException {
//        System.setProperty("http.proxyHost", "tmg-2.tosanltd.com");
//        System.setProperty("http.proxyPort", "8585");
        long startTime = System.currentTimeMillis();

        List<Callable<Void>> taskList = new ArrayList<>();
        for (String url : URL_TO_CRAWLS) {
            Callable<Void> callable = () -> {
                getPageLinks(url, 1);
                return null;
            };
            taskList.add(callable);
        }

        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());

        try {
            executor.invokeAll(taskList);
        } catch (InterruptedException ignored) {
        }

        ExtractLinks.writeToFile("extracted.txt");
        System.out.printf("total time ::: %d%n", (System.currentTimeMillis() - startTime) / 1000);
        executor.shutdownNow();
        // ExtractLinks.getArticles();
    }
}