package sepoa.agent.query_agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import sepoa.agent.query_agent.service.DatabaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DatabaseController {

    private final DatabaseService databaseService;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody String sql) {
        Map<String, Object> rtn = new HashMap<>();
        try {
            logger.info("=== sql:" + sql);
            List<Map<String, Object>> result = databaseService.executeQuery(sql);
            rtn.put("result", "SUCCESS");
            rtn.put("data", result);
            rtn.put("message", "");
        } catch (Exception e) {
            rtn.put("result", "FAIL");
            rtn.put("data", "");
            rtn.put("message", e.getMessage());
        } finally {
            String jsonString = "";
            try {
                jsonString = objectMapper.writeValueAsString(rtn);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.info(jsonString);
        }
        return ResponseEntity.ok(rtn);
    }

    @PostMapping("/upsert")
    public ResponseEntity<Map<String, Object>> executeUpsert(@RequestBody String sql) {
        Map<String, Object> rtn = new HashMap<>();
        try {
            int result = databaseService.executeUpsert(sql);
            rtn.put("result", "SUCCESS");
            rtn.put("data", result);
            rtn.put("message", "");
        } catch (Exception e) {
            rtn.put("result", "FAIL");
            rtn.put("data", "");
            rtn.put("message", e.getMessage());
        }
        return ResponseEntity.ok(rtn);
    }
}
