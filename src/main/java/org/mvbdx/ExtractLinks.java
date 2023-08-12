package org.mvbdx;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.URLDecoder.decode;

public class ExtractLinks {
    private static final HashSet<Item> items = new HashSet<>();
    private static final int MAX_DEPTH = 5;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MMM-dd hh:mm", Locale.ENGLISH);
    private static final String ABS_HREF = "abs:href";
    private static final String FILENAME = "moviez.txt";
    public static final List<List<String>> URL_TO_CRAWLS = List.of(List.of("http://dl.gemescape.com/Series/", "http://dl2.gemescape.com/SERIES/", "http://dl3.gemescape.com/SERIES/", "http://dl4.gemescape.com/SERIES/", "http://dl5.gemescape.com/SERIES/"),
            List.of("http://dl.gemescape.com/film/", "http://dl2.gemescape.com/FILM/", "http://dl3.gemescape.com/FILM/", "http://dl4.gemescape.com/FILM/", "http://dl5.gemescape.com/MOVIES/"),
            List.of("https://dl.ahaang.com/mp3/fa/", "https://dl.ahaang.com/00/", "https://dl.ahaang.com/01/", "https://dl.ahaang.com/02/", "https://dl.ahaang.com/94/", "https://dl.ahaang.com/95/", "https://dl.ahaang.com/96/", "https://dl.ahaang.com/97/", "https://dl.ahaang.com/98/", "https://dl.ahaang.com/99/"));
    // https://dl5.gemexit.com/
    public static final List<String> IGNORE_NAMES = List.of(".mkv", ".mp4", ".avi", ".srt", ".jpg", ".rar", ".zip", ".mp3", ".flac",
            "/ORG/", "/Trailer/", "/Dub/", "/Dubbed/", "/dual/", "/Sub/", "/SoftSub/", "/Soft/", "/Shot/", "/Soundtrack/", "/Specials/",
            "/Blu-ray/", "/BluRay/", "/Bluray/", "/IMAX.WEB-DL/", "/WEB/", "/WEB-DL/", "/WEB.HMAX/", "/IMAX/",
            "/480p/", "/480p.x264.SoftSub/", "480p.x265", "/480p.Dubbed/", "/540p/", "/576p/", "/540p.SoftSub/",
            "/720p/", "/720p.x264/", "/720.x264/", "/720p.SS/", "/720p.GTV.Dubbed/", "/720p.Dubbed/", "/720pGTV/", "/720p.HQ/", "/720p.HD/", "/720p.GTV/", "/720p.x265/", "/720p.x265.SoftSub/", "/720p.SoftSub/", "/720p.x265.Dubbed/", "/720p.FHD/", "/BluRay.720p/", "/720p.x265.Dual/", "/720p.x265.BluRay/", "/720p.x264.SoftSub/", "/720p.x265.60FPS/",
            "/1080p/", "/1080p.HQ/", "/1080p.FHD/", "/1080p.x265/", "/1080p.x264/", "/1080p.x265.Dubbed/", "/1080p.SoftSub/", "/1080p.x265.SoftSub/", "/1080p.x264.SoftSub/", "/1080p.x265.HDR/", "/BluRay.1080p/", "/1080p.x265.Dual/", "/1080.x265.BluRay/", "/1080p.x265.60FPS/",
            "/2160p/", "/2160p.x265/", "/2160p.x265.HDR/", "/2160p.HDR/",
            "/PSA/", "/720p.x265.PSA/", "/1080p.x265.PSA/", "/2160p.x265.PSA/", "/Pahe/", "/480p.Pahe/", "/720.Pahe/", "/720p.Pahe/", "/720p.x265.Pahe/", "/1080p.Pahe/",
            "/?C=S&O=D", "/?C=S&O=A", "/?C=M&O=D", "/?C=M&O=A", "/?C=N&O=A", "/?C=N&O=D");

    public static boolean isIgnoredUrl(String url) {
        return IGNORE_NAMES.stream().anyMatch(url::contains) || url.matches(".*/S\\d{2}/.*");
    }

    public static void getPageLinks(Item item, int depth) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {

        if (!items.contains(item) && (depth < MAX_DEPTH)) { // urlLinks.size() != 50

            System.out.printf(">> Depth: %d ( %s ) [ %s ]\n", depth, extractURIName(item.getUrl()), item.getUrl());
            try {
                items.add(item);

                Document doc = Jsoup.connect(item.getUrl()).get();
                Elements availableLinksOnPage = doc.select("a[href]");
                depth++;

                for (Element ele : availableLinksOnPage) {
                    if (ele.attr(ABS_HREF).startsWith(item.getUrl()) && !isIgnoredUrl(ele.attr(ABS_HREF))) {
                        Date date = dateFormatter.parse(((Element) ele.parentNode().parentNode()).getElementsByClass("date").get(0).firstChild().toString());
                        getPageLinks(new Item(ele.attr(ABS_HREF), date), depth);
                    }
                }
            } catch (IOException e) {
                System.out.printf("For '%s' : %s\n", item.getUrl(), e.getMessage());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void writeToFile(String fileName) {
        FileWriter wr;
        try {
            wr = new FileWriter(fileName, true);
            List<Item> itemList = new ArrayList<>(items);
            itemList.sort(Collections.reverseOrder());
            for (Item item : itemList) {
                try {
                    String address = extractURIName(item.getUrl()) + " --> " + item.getUrl() + "\n";
                    wr.write(address);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            wr.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
//        System.setProperty("http.proxyHost", "tmg-2.tosanltd.com");
//        System.setProperty("http.proxyPort", "8585");
        long startTime = System.currentTimeMillis();

        List<Callable<Void>> taskList = new ArrayList<>();
        for (String url : URL_TO_CRAWLS.get(0)) {
            Callable<Void> callable = () -> {
                getPageLinks(new Item(url, null), 1);
                return null;
            };
            taskList.add(callable);
        }

        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());

        try {
            executor.invokeAll(taskList);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        ExtractLinks.writeToFile(FILENAME);
        System.out.printf("total time ::: %d%n", (System.currentTimeMillis() - startTime) / 1000);
        executor.shutdownNow();
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

    static class Item implements Comparable<Item> {
        private final String url;
        private final Date date;

        public String getUrl() {
            return url;
        }

        public Date getDate() {
            return date;
        }

        public Item(String url, Date date) {
            this.url = url;
            this.date = date;
        }

        @Override
        public int compareTo(Item o) {
            if (getDate() == null || o.getDate() == null) return -1;
            return getDate().compareTo(o.getDate());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item that = (Item) o;
            return Objects.equals(url, that.url) && Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, date);
        }
    }
}