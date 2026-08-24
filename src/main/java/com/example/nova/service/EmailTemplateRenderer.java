package com.example.nova.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the HTML email bodies under {@code src/main/resources/email}.
 *
 * <p>Deliberately not Thymeleaf: two emails don't justify pulling in a
 * template engine, and a {@code {{placeholder}}} swap keeps the templates
 * editable by anyone who can read HTML.
 *
 * <p>Values substituted through {@link #render} are HTML-escaped. That matters
 * because some of them are user-controlled - a team member whose name contains
 * {@code <} would otherwise break the layout at best, and inject markup into
 * everyone's inbox at worst.
 */
@Slf4j
@Component
public class EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    /** Classpath templates can't change at runtime, so load each one once. */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Renders a body fragment, escaping every supplied value, then drops it
     * into the shared layout.
     *
     * @param templateName file name under {@code email/}, e.g. {@code password-reset.html}
     * @param title        {@code <title>} text
     * @param preheader    inbox preview line shown next to the subject
     * @param values       raw (unescaped) placeholder values
     */
    public String renderEmail(String templateName, String title, String preheader, Map<String, String> values) {
        Map<String, String> escaped = new ConcurrentHashMap<>();
        values.forEach((key, value) -> escaped.put(key, HtmlUtils.htmlEscape(value == null ? "" : value)));

        String body = substitute(load(templateName), escaped);

        // `body` is already-rendered HTML and must go in raw; title/preheader
        // are plain text and still need escaping.
        return substitute(load("layout.html"), Map.of(
                "title", HtmlUtils.htmlEscape(title),
                "preheader", HtmlUtils.htmlEscape(preheader),
                "body", body
        ));
    }

    /** Pure replacement - every value here must already be safe to emit as HTML. */
    private String substitute(String template, Map<String, String> safeValues) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = safeValues.get(key);
            if (value == null) {
                // Template and caller have drifted apart - shipping a mail with
                // a literal "{{token}}" in it is worse than failing loudly.
                throw new IllegalStateException("No value supplied for email placeholder '" + key + "'");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String load(String templateName) {
        return cache.computeIfAbsent(templateName, name -> {
            ClassPathResource resource = new ClassPathResource("email/" + name);
            try {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Email template not found on the classpath: email/" + name, e);
            }
        });
    }
}
