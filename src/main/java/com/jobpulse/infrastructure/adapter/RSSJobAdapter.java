package com.jobpulse.infrastructure.adapter;

import com.jobpulse.infrastructure.client.MockJobBoardClient.ExternalJob;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RSS Job Feed Adapter - Fetches jobs from RSS/Atom feeds
 * Supports job board feeds from various sources
 */
@Component
public class RSSJobAdapter {

    private final Logger log = LoggerFactory.getLogger(RSSJobAdapter.class);

    /**
     * Fetch jobs from an RSS feed URL
     */
    public List<ExternalJob> fetchFromRSSFeed(String feedUrl) {
        List<ExternalJob> jobs = new ArrayList<>();
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

            for (SyndEntry entry : feed.getEntries()) {
                ExternalJob job = new ExternalJob();
                job.externalId = "rss-" + UUID.nameUUIDFromBytes(entry.getLink().getBytes());
                job.title = entry.getTitle();
                job.company = feed.getTitle();
                job.location = "Remote";
                job.description = entry.getDescription() != null ? 
                    entry.getDescription().getValue() : entry.getTitle();
                job.url = entry.getLink();
                job.postedAt = entry.getPublishedDate() != null ? 
                    entry.getPublishedDate().toInstant() : Instant.now();
                job.source = "RSS";

                jobs.add(job);
                log.debug("Parsed RSS job: {}", job.title);
            }
        } catch (Exception e) {
            log.error("Error parsing RSS feed {}: {}", feedUrl, e.getMessage(), e);
        }
        return jobs;
    }

    /**
     * Common job board RSS feeds
     */
    public List<ExternalJob> fetchFromPopularFeeds() {
        List<ExternalJob> allJobs = new ArrayList<>();

        // Example: Stack Overflow Jobs RSS (if available)
        // String soFeed = "https://stackoverflow.com/jobs/feed";
        // allJobs.addAll(fetchFromRSSFeed(soFeed));

        // Add more feeds as needed
        log.info("Fetched jobs from RSS feeds");
        return allJobs;
    }
}

