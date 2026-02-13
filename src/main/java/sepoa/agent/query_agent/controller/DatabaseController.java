package sepoa.agent.query_agent.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import sepoa.agent.query_agent.model.SendFile;
import sepoa.agent.query_agent.service.DatabaseService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DatabaseController {
    @Value("${test.query}")
    private String testQuery;

    private final DatabaseService databaseService;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.databaseService.init();
    }

    @GetMapping("/testa")
    public ResponseEntity<Map<String, Object>> executeAlive() {
        Map<String, Object> rtn = new HashMap<>();
        rtn.put("result", "SUCCESS");
        rtn.put("message", "Alive!!");
        return ResponseEntity.ok(rtn);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> executeTest() {
        Map<String, Object> rtn = new HashMap<>();
        try {
            logger.info("=== test call");
            String sql = testQuery.replace("__", " ");
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
                e.printStackTrace();
            }
            logger.info(jsonString);
        }
        return ResponseEntity.ok(rtn);
    }
 
    @GetMapping("/read2send")
    public ResponseEntity<Map<String, Object>> executeReadToSend() {
        this.databaseService.init();
        Map<String, Object> rtn = new HashMap<>();
        // 조달청에서 넘어온 데이타 저장
        HashSet<String> fileNameSet =new HashSet<String>();
        
        try {
            logger.info("-------------------------------------------------------------------");

            logger.info("folderPath : {}", databaseService.folderPath);
            logger.info("targetFolder : {}", databaseService.targetFolder);
            logger.info("sessionToken : {}", databaseService.sessionToken);
            logger.info("serviceToken : {}", databaseService.serviceToken);

            rtn.put("message", "");
            File folder = new File(databaseService.folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                rtn.put("message", "폴더가 존재하지 않거나 폴더가 아닙니다.");
                return ResponseEntity.ok(rtn);
            }
            // 폴더 내의 모든 파일 및 하위 폴더를 검사합니다.
            List<File> resultFiles = new ArrayList<>();

            // 방어적으로 복사해서 사용
            for (String type : new ArrayList<>(databaseService.types)) { //다른 쓰레드에서 변경 시에 에러 발생해서 복사해서 실행
                resultFiles.addAll((List<File>) FileUtils.listFiles(folder, new WildcardFileFilter(type + "*.0"), FalseFileFilter.INSTANCE));
            }
            
            //이미 읽은 파일 목록을 가져옵니다.
            File writeFilePath = new File(databaseService.checkFile);

            // 부모 디렉토리 없으면 생성
            File parentDir = writeFilePath.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 파일 없으면 생성
            if (!writeFilePath.exists()) {
                writeFilePath.createNewFile();
            } 

            String checkXml = FileUtils.readFileToString(writeFilePath, "UTF-8");	
            fileNameSet = new HashSet<>(Arrays.asList(checkXml.split("\n")));
        
            rtn.put("files", resultFiles);
            String fileName = "";
            if (resultFiles.size() > 0) {
                for (File file : resultFiles) {
                    if (file.exists()) {
                        try {
                            String key = file.getPath().replaceAll("\\\\", "/");
                            fileName = file.getName();
                            String type = fileName.substring(0, 6 );
                            logger.info("fileName : {}", fileName);
                            logger.info("type : {}", type);

                            // 중복파일 체크파일 이름이 이미 있는 경우 예외 발생
                            if (!fileNameSet.add(fileName)) {
                                logger.info("이미 처리한 파일입니다.: {}", fileName);
                                continue;
                            }
                            key = key.substring(key.indexOf("recv"), key.length()); //경로 DB 저장용
                            String returnData = "";
                            String lastTwo = fileName.substring(fileName.length() - 2);
                            String base64String = "";
                            if(".0".equals(lastTwo)) { //xml
                                String xml = FileUtils.readFileToString(file, "UTF-8");
                                base64String = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
                            }else {
                                byte[] fileContent = databaseService.readFileToByteArray(file);
                                base64String = Base64.getEncoder().encodeToString(fileContent);
                            }
                            
                            returnData = databaseService.callRestInterface(databaseService.recvUrl, key, base64String, type);

                            Map<String, String> g2bMap = databaseService.jsonToMap(returnData);
                            logger.info("g2bMap : {}", g2bMap);
                            logger.info("Send Message : {}", g2bMap.get("message"));

                            if(g2bMap != null && "false".equals(g2bMap.get("flag"))) {
                                logger.error("Send File Error : {}", file.getPath());
                            } else {
                                System.out.println(g2bMap);
                                //신규파일 이름 기록
                                FileUtils.writeStringToFile(writeFilePath, fileName+"\n", "UTF-8", true);
                                logger.info("Send File Success : {}", file.getPath());
                                
                            }
                        }catch(Exception e){
                            logger.error("Job File Fail01!!! : {}", file.getPath(), e);
                        }finally {
                            databaseService.moveFile(databaseService.folderPath + "/" + fileName, databaseService.targetFolder + "/" + fileName);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Job File Fail02!!! : {}", e.getMessage(), e);
        } 

        return ResponseEntity.ok(rtn);
    }


    @PostMapping("/sendfile")
    public ResponseEntity<Map<String, Object>> executeSendFile(@RequestBody SendFile sendFile) {
        this.databaseService.init();
        Map<String, Object> rtn = new HashMap<>();
        String msg = this.databaseService.sendFile(sendFile);
        rtn.put("message", msg);
        return ResponseEntity.ok(rtn);
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

    @PostMapping("/soap")
    public ResponseEntity<Map<String, Object>> callSOAP(@RequestBody String bizNos) {
        Map<String, Object> rtn = new HashMap<>();
        try {
            List<Map<String, String>> result = databaseService.callSOAP(bizNos);
            rtn.put("result", "SUCCESS");
            rtn.put("data", result);
            rtn.put("message", "");
            logger.info("Response from SOAP: {}", result.size());
        } catch (Exception e) {
            rtn.put("result", "FAIL");
            rtn.put("data", "");
            rtn.put("message", e.getMessage());
            logger.error(">>>Error calling SOAP: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(rtn);
    }
}
