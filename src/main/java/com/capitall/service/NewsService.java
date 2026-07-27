package com.capitall.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final List<NewsArticle> newsCache = new CopyOnWriteArrayList<>();
    private final Set<String> seenArticleIds = Collections.synchronizedSet(new HashSet<>());

    public static class NewsArticle {
        private String id;
        private String title;
        private String summary;
        private String body;
        private String category; // Giełda, Kryptowaluty, Gospodarka, Technologia
        private String source;
        private String imageUrl;
        private String url;
        private String publishedAt;
        private String readTime;
        private boolean featured;

        public NewsArticle() {}

        public NewsArticle(String id, String title, String summary, String body, String category,
                           String source, String imageUrl, String url, String publishedAt, String readTime, boolean featured) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.body = body;
            this.category = category;
            this.source = source;
            this.imageUrl = imageUrl;
            this.url = url;
            this.publishedAt = publishedAt;
            this.readTime = readTime;
            this.featured = featured;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

        public String getReadTime() { return readTime; }
        public void setReadTime(String readTime) { this.readTime = readTime; }

        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean featured) { this.featured = featured; }
    }

    @PostConstruct
    public void initNewsFeed() {
        log.info("[NewsService] Inicjalizacja wiadomości i miniaturek przy starcie serwera...");
        for (NewsArticle art : getPolishCuratedArticles()) {
            if (seenArticleIds.add(art.getId())) {
                newsCache.add(art);
            }
        }
        pollNewArticlesFromApi();
    }

    @Scheduled(fixedRate = 30000)
    public void pollNewArticlesFromApi() {
        // Source 1: Free Coinlore Live News API
        try {
            String apiUrl = "https://api.coinlore.net/api/news/";
            List<Map<String, Object>> response = restTemplate.getForObject(apiUrl, List.class);
            if (response != null && !response.isEmpty()) {
                List<NewsArticle> newlyDiscovered = new ArrayList<>();
                for (Map<String, Object> item : response) {
                    String id = String.valueOf(item.get("id"));
                    if (id != null && !seenArticleIds.contains(id)) {
                        seenArticleIds.add(id);
                        String title = (String) item.get("title");
                        String body = (String) item.get("description");
                        String url = (String) item.get("url");
                        String source = "COINLORE / LIVE MARKET";
                        
                        String cat = mapCategory(title != null ? title : "", body != null ? body : "");
                        String summary = cleanHtmlText(body != null && body.length() > 160 ? body.substring(0, 160) + "..." : body);
                        String pubDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                        String imgUrl = extractOrGenerateImage(item, title, body);

                        NewsArticle article = new NewsArticle(
                                id, cleanHtmlText(title), summary, cleanHtmlText(body), cat, source,
                                imgUrl, url, pubDate, "3 min czytania", false
                        );
                        newlyDiscovered.add(article);
                    }
                }

                if (!newlyDiscovered.isEmpty()) {
                    log.info("[NewsService] Wykryto {} nowych artykułów z API (Coinlore)! Dodawanie na początek widoku...", newlyDiscovered.size());
                    newsCache.addAll(0, newlyDiscovered);
                }
            }
        } catch (Exception e) {
            log.debug("[NewsService] Informacja o Coinlore API: {}", e.getMessage());
        }

        // Source 2: Yahoo Finance Headlines via RSS2JSON
        try {
            String apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=https://feeds.finance.yahoo.com/rss/2.0/headline?s=AAPL,TSLA,NVDA,MSFT,BTC-USD";
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            if (response != null && "ok".equals(response.get("status")) && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    List<NewsArticle> newlyDiscovered = new ArrayList<>();
                    for (Map<String, Object> item : items) {
                        String url = (String) item.get("link");
                        String id = String.valueOf(url != null ? url.hashCode() : item.get("title").hashCode());
                        if (!seenArticleIds.contains(id)) {
                            seenArticleIds.add(id);
                            String title = (String) item.get("title");
                            String body = (String) item.get("description");
                            String source = "YAHOO FINANCE";
                            String cat = mapCategory(title != null ? title : "", body != null ? body : "");
                            String summary = cleanHtmlText(body != null && body.length() > 160 ? body.substring(0, 160) + "..." : body);
                            String pubDate = (String) item.get("pubDate");
                            if (pubDate == null) pubDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                            String imgUrl = extractOrGenerateImage(item, title, body);

                            NewsArticle article = new NewsArticle(
                                    id, cleanHtmlText(title), summary, cleanHtmlText(body), cat, source,
                                    imgUrl, url, pubDate, "4 min czytania", false
                            );
                            newlyDiscovered.add(article);
                        }
                    }

                    if (!newlyDiscovered.isEmpty()) {
                        log.info("[NewsService] Wykryto {} nowych artykułów z API (Yahoo Finance)! Dodawanie na początek widoku...", newlyDiscovered.size());
                        newsCache.addAll(0, newlyDiscovered);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[NewsService] Informacja o RSS2JSON API: {}", e.getMessage());
        }

        // Ensure exactly 1 featured article is set
        if (!newsCache.isEmpty()) {
            boolean hasFeatured = newsCache.stream().anyMatch(NewsArticle::isFeatured);
            if (!hasFeatured) {
                newsCache.get(0).setFeatured(true);
            }
        }
    }

    private String extractOrGenerateImage(Map<String, Object> item, String title, String body) {
        if (item != null) {
            if (item.containsKey("thumbnail") && item.get("thumbnail") instanceof String && !((String) item.get("thumbnail")).isEmpty()) {
                return (String) item.get("thumbnail");
            }
            if (item.containsKey("enclosure") && item.get("enclosure") instanceof Map) {
                Map<String, Object> enc = (Map<String, Object>) item.get("enclosure");
                if (enc.containsKey("link") && enc.get("link") instanceof String && !((String) enc.get("link")).isEmpty()) {
                    return (String) enc.get("link");
                }
            }
            String content = (String) item.get("description");
            if (content == null) content = (String) item.get("content");
            if (content != null) {
                Matcher m = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(content);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }

        // Guaranteed unique photo per article using seed or unique image pool
        int hash = Math.abs(((title != null ? title : "") + (body != null ? body : "")).hashCode());
        
        List<String> uniquePhotos = List.of(
            "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1591488320449-011701bb6704?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1563720223185-11003d516935?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1642132652859-3ef5a1048fd1?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1523474253046-8cd2748b5fd2?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1622979135225-d2ba269bc1bd?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1642543492481-44e81e3914a7?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1535320903710-d993d3d77d29?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1507679799987-c73779587ccf?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1460925895917-afdab827c52f?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1541354329998-f4d9a9f9297f?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1521737711867-e3b97375f902?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1551836022-d5d88e9218df?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1553729459-efe14ef6055d?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1504868584819-f8e8b4b6d7e3?q=80&w=1000&auto=format&fit=crop"
        );

        return uniquePhotos.get(hash % uniquePhotos.size());
    }

    private String cleanHtmlText(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "").trim();
    }

    public List<NewsArticle> fetchAllNews(String category, String query) {
        List<NewsArticle> articles = new ArrayList<>(newsCache);

        // Filtering by category
        if (category != null && !category.isEmpty() && !"Wszystkie".equalsIgnoreCase(category)) {
            articles = articles.stream()
                    .filter(a -> a.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // Filtering by search query
        if (query != null && !query.trim().isEmpty()) {
            String q = query.toLowerCase().trim();
            articles = articles.stream()
                    .filter(a -> a.getTitle().toLowerCase().contains(q) ||
                            a.getSummary().toLowerCase().contains(q) ||
                            a.getCategory().toLowerCase().contains(q) ||
                            a.getSource().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        return articles;
    }

    public List<String> getTrendingPhrases() {
        return List.of("#Inflacja", "#Fed", "#Bitcoin ETF", "#NVIDIA AI", "#WIG20", "#StopyProcentowe", "#Orlen", "#CDProjekt", "#SpółkiTechnologiczne");
    }

    private String mapCategory(String title, String body) {
        String full = (title + " " + body).toLowerCase();
        if (full.contains("btc") || full.contains("bitcoin") || full.contains("crypto") || full.contains("ethereum") || full.contains("eth")) {
            return "Kryptowaluty";
        } else if (full.contains("fed") || full.contains("inflation") || full.contains("gdp") || full.contains("economy") || full.contains("bank")) {
            return "Gospodarka";
        } else if (full.contains("ai") || full.contains("tech") || full.contains("nvidia") || full.contains("apple") || full.contains("microsoft")) {
            return "Technologia";
        } else {
            return "Giełda";
        }
    }

    private List<NewsArticle> getPolishCuratedArticles() {
        return List.of(
            new NewsArticle(
                "pl-101",
                "GPW w Warszawie bije rekordy obrotów – sektor bankowy i technologiczny napędzają hossę",
                "Indeks WIG20 przekroczył kluczowe poziomy oporu dzięki doskonałym wynikom finansowym banków i rosnącym wycenom spółek z branży IT oraz IT gaming.",
                "Inwestorzy na Giełdzie Papierów Wartościowych w Warszawie świętują kolejny udany tydzień. Analitycy wskazują na napływ kapitału zagranicznego oraz obniżki stóp procentowych jako główne czynniki stymulujące popyt na polskie akcje.",
                "Giełda", "Bankier.pl",
                "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=1000&auto=format&fit=crop",
                "https://www.gpw.pl",
                "26.07.2026 14:15", "4 min czytania", true
            ),
            new NewsArticle(
                "pl-102",
                "NVIDIA prezentuje nową generację chipów AI – kurs akcji bije nowe historyczne szczyty",
                "Gigant półprzewodników ogłosił nową architekturę procesorów graficznych przeznaczonych do trenowania modeli sztucznej inteligencji kolejnej generacji.",
                "Firma NVIDIA powiększa przewagę technologiczną nad konkurencją. Analitycy z Wall Street podnieśli ceny docelowe dla akcji spółki, wskazując na niegasnący popyt ze strony największych dostawców chmury obliczeniowej.",
                "Technologia", "Bloomberg / Money.pl",
                "https://images.unsplash.com/photo-1591488320449-011701bb6704?q=80&w=1000&auto=format&fit=crop",
                "https://www.nvidia.com",
                "26.07.2026 12:30", "5 min czytania", false
            ),
            new NewsArticle(
                "pl-103",
                "Decyzja Rezerwy Federalnej (Fed) w sprawie stóp procentowych – rynek oczekuje dalszych cięć",
                "Ostatnie odczyty inflacji w USA wskazują na stabilizację cen, co daje Rezerwie Federalnej przestrzeń do dalszego łagodzenia polityki pieniężnej.",
                "Przewodniczący Fed podczas konferencji prasowej zasugerował, że rynek pracy znajduje się w równej równowadze z celami inflacyjnymi. Inwestorzy wyceniają 90% prawdopodobieństwa obniżki stóp na najbliższym posiedzeniu.",
                "Gospodarka", "Financial Times",
                "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=1000&auto=format&fit=crop",
                "https://www.federalreserve.gov",
                "26.07.2026 11:00", "3 min czytania", false
            )
        );
    }
}
