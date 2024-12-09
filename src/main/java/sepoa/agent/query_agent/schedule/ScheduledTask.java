package sepoa.agent.query_agent.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ScheduledTask {
    @Value("${sepoa.schedule.period}")
    public int period;

    @Value("${sepoa.schedule.url}")
    public String url;

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRateString = "#{${sepoa.schedule.period}}") // Runs every period milliseconds
    public void callHelloEndpoint() {
        try {
            if (url == null || "".equals(url)) return;
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Response from API: {}", response);
        } catch (Exception e) {
            logger.error("Error calling the endpoint: {}", e.getMessage());
        }
    }
}