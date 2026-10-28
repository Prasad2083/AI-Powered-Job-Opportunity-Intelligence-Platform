package com.jobpulse.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobpulse.infrastructure.client.MockJobBoardClient.ExternalJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.*;

@Service
public class PublicJobFeedService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(PublicJobFeedService.class);
    private final RestClient client;
    public PublicJobFeedService() {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);factory.setReadTimeout(15000);
        client=RestClient.builder().requestFactory(factory).build();
    }
    PublicJobFeedService(RestClient client,String boards,String sites) {this.client=client;greenhouseBoards=boards;leverSites=sites;}
    @Value("${jobpulse.feeds.greenhouse-boards:canonical,reddit,gitlab}") private String greenhouseBoards;
    @Value("${jobpulse.feeds.lever-sites:ataccama,caseware}") private String leverSites;

    public record FeedBatch(List<ExternalJob> jobs,Set<String> successfulSources,Map<String,String> failures) {}
    public List<ExternalJob> fetch() {return fetchReport().jobs();}

    public FeedBatch fetchReport() {
        List<ExternalJob> jobs=new ArrayList<>();Set<String> success=new HashSet<>();Map<String,String> failures=new LinkedHashMap<>();
        for(String board:greenhouseBoards.split(",")) {
            String id=board.trim();if(id.isEmpty()) continue;
            String source="GREENHOUSE:"+id;
            try {
                validateSlug(id);
                JsonNode result=client.get().uri("https://boards-api.greenhouse.io/v1/boards/"+id+"/jobs?content=true").retrieve().body(JsonNode.class);
                if(result==null || !result.path("jobs").isArray()) throw new IllegalArgumentException("Unexpected provider response");
                List<ExternalJob> batch=new ArrayList<>();
                for(JsonNode item:result.path("jobs")) batch.add(greenhouse(item,id));
                jobs.addAll(batch);success.add(source);
            } catch(Exception ex) {failures.put(source,"Feed unavailable or malformed; previous listings retained");log.warn("Feed {} failed: {}",source,ex.getClass().getSimpleName());}
        }
        for(String site:leverSites.split(",")) {
            String id=site.trim();if(id.isEmpty()) continue;
            String source="LEVER:"+id;
            try {
                validateSlug(id);List<ExternalJob> batch=new ArrayList<>();boolean complete=false;
                for(int skip=0;skip<10000;skip+=100) {
                    JsonNode result=client.get().uri("https://api.lever.co/v0/postings/"+id+"?mode=json&limit=100&skip="+skip).retrieve().body(JsonNode.class);
                    if(result==null || !result.isArray()) throw new IllegalArgumentException("Unexpected provider response");
                    for(JsonNode item:result) batch.add(lever(item,id));
                    if(result.size()<100) {complete=true;break;}
                }
                if(!complete) throw new IllegalArgumentException("Pagination safety limit reached");
                jobs.addAll(batch);success.add(source);
            } catch(Exception ex) {failures.put(source,"Feed unavailable, incomplete or malformed; previous listings retained");log.warn("Feed {} failed: {}",source,ex.getClass().getSimpleName());}
        }
        return new FeedBatch(jobs,success,failures);
    }

    private static void validateSlug(String id) {
        if(!id.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Invalid employer board slug");
    }
    private ExternalJob greenhouse(JsonNode item,String id) {
        ExternalJob job=base(item,id,"GREENHOUSE:");
        job.title=item.path("title").asText();job.location=item.path("location").path("name").asText("");
        job.description=item.path("content").asText("");job.url=item.path("absolute_url").asText();
        // Greenhouse updated_at is not a publication date.
        return validate(job);
    }
    private ExternalJob lever(JsonNode item,String id) {
        ExternalJob job=base(item,id,"LEVER:");
        job.title=item.path("text").asText();job.location=item.path("categories").path("location").asText("");
        job.description=item.path("descriptionPlain").asText("");
        for(JsonNode section:item.path("lists")) job.description+="\n"+section.path("text").asText("")+"\n"+section.path("content").asText("");
        job.description+="\n"+item.path("additionalPlain").asText("");job.url=item.path("hostedUrl").asText();
        if(item.path("createdAt").isNumber()) job.postedAt=Instant.ofEpochMilli(item.path("createdAt").asLong());
        return validate(job);
    }
    private ExternalJob base(JsonNode item,String id,String prefix) {
        String externalId=item.path("id").asText();
        if(externalId.isBlank()) throw new IllegalArgumentException("Job ID missing");
        ExternalJob job=new ExternalJob();job.externalId=id+"-"+externalId;job.company=id;job.source=prefix+id;return job;
    }
    private ExternalJob validate(ExternalJob job) {
        if(job.title.isBlank() || job.title.length()>255 || job.externalId.length()>255 || job.url.length()>500)
            throw new IllegalArgumentException("Invalid provider job fields");
        java.net.URI uri=java.net.URI.create(job.url);
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
            throw new IllegalArgumentException("Invalid provider apply URL");
        return job;
    }
}
