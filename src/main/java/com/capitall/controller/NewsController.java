package com.capitall.controller;

import com.capitall.model.User;
import com.capitall.service.NewsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public String showNewsPage(Model model,
                               @AuthenticationPrincipal User user,
                               @RequestParam(required = false, defaultValue = "Wszystkie") String category,
                               @RequestParam(required = false, defaultValue = "") String q) {

        List<NewsService.NewsArticle> allArticles = newsService.fetchAllNews(category, q);
        
        NewsService.NewsArticle featuredArticle = allArticles.stream()
                .filter(NewsService.NewsArticle::isFeatured)
                .findFirst()
                .orElse(allArticles.isEmpty() ? null : allArticles.get(0));

        List<NewsService.NewsArticle> gridArticles = new ArrayList<>(allArticles);
        if (featuredArticle != null) {
            gridArticles.remove(featuredArticle);
        }

        List<String> categories = List.of("Wszystkie", "Giełda", "Kryptowaluty", "Gospodarka", "Technologia");
        List<String> trendingPhrases = newsService.getTrendingPhrases();

        model.addAttribute("featured", featuredArticle);
        model.addAttribute("articles", gridArticles);
        model.addAttribute("categories", categories);
        model.addAttribute("activeCategory", category);
        model.addAttribute("query", q);
        model.addAttribute("trending", trendingPhrases);
        model.addAttribute("traderName", user != null ? user.getUsername() : "Inwestor");

        return "news";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> getNewsApi(@RequestParam(defaultValue = "Wszystkie") String category,
                                          @RequestParam(defaultValue = "") String q) {
        List<NewsService.NewsArticle> articles = newsService.fetchAllNews(category, q);
        return Map.of("success", true, "articles", articles);
    }
}
