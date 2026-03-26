package sepoa.agent.query_agent.schedule;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import sepoa.agent.query_agent.controller.DatabaseController;

@Component
public class ScheduledTask {
// 1. DatabaseController 주입 (생성자 주입 권장)
    private final DatabaseController databaseController;
        
    // @Value("${sepoa.schedule.url}")
    // public String url;
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    public ScheduledTask(DatabaseController databaseController) {
        this.databaseController = databaseController;
    }

    // 2. fixedDelayString을 사용하여 안전하게 호출
    @Scheduled(fixedDelayString = "#{${sepoa.schedule.period}}") 
    public void callDatabaseJob() {
        try {
            logger.info("정기 스케줄 작업을 시작합니다: executeReadToSend 호출");
            
            // 컨트롤러의 메서드를 직접 호출
            // 리턴 타입이 ResponseEntity이므로 결과 확인 가능
            ResponseEntity<Map<String, Object>> response = databaseController.executeReadToSend();
            if (response != null) {
                logger.info("스케줄 작업 완료. 결과 상태: {}", response.getStatusCode());
                // 필요하다면 결과 데이터 추출
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    logger.info("응답 메시지: {}", body.get("message"));
                }
            }
        } catch (Exception e) {
            logger.error("스케줄 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    // private final RestTemplate restTemplate = new RestTemplate();

    // @Scheduled(fixedDelayString = "#{${sepoa.schedule.period}}") // Runs every period milliseconds
    // public void callHelloEndpoint() {
    //     try {
    //         if (url == null || "".equals(url)) return;
    //         String response = restTemplate.getForObject(url, String.class);
    //         logger.info("Response from API: {}", response);
    //     } catch (Exception e) {
    //         logger.error("Error calling the endpoint: {}", e.getMessage());
    //     }
    // }
}