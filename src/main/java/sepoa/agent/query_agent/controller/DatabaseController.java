package sepoa.agent.query_agent.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final DatabaseService databaseService;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.databaseService.init();
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> executeTest() {
        Map<String, Object> rtn = new HashMap<>();
        try {
            logger.info("=== test call");
            String sql = "SELECT TOP 1 TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES";
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

        // 기본 생성자로 생성시 현재 시간과 날짜 정보를 가진 Date 객체가 생성됩니다.
        Date nowDate = new Date();

        // 원하는 형태의 포맷으로 날짜, 시간을 표현하기 위해서는 SimpleDateFormat 클래스를 이용합니다.
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

        String date = dateFormat.format(nowDate);
        String time = timeFormat.format(nowDate);
        
        try {
            databaseService.Log("-------------------------------------------------------------------", date, time);

            databaseService.Log("folderPath : " + databaseService.folderPath, date, time);
            databaseService.Log("targetFolder : " + databaseService.targetFolder, date, time);
            databaseService.Log("sessionToken : " + databaseService.sessionToken, date, time);
            databaseService.Log("serviceToken : " + databaseService.serviceToken, date, time);

            rtn.put("message", "");
            File folder = new File(databaseService.folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                rtn.put("message", "폴더가 존재하지 않거나 폴더가 아닙니다.");
                return ResponseEntity.ok(rtn);
            }
            // 폴더 내의 모든 파일 및 하위 폴더를 검사합니다.
            List<File> resultFiles = new ArrayList<>();
            for (String type : databaseService.types) {
                resultFiles.addAll((List<File>) FileUtils.listFiles(folder, new WildcardFileFilter(type + "*"),TrueFileFilter.INSTANCE));
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
                            databaseService.Log("fileName : " + fileName, date, time);
                            databaseService.Log("type : " + type, date, time);

                            // 중복파일 체크파일 이름이 이미 있는 경우 예외 발생
                            if (!fileNameSet.add(fileName)) {
                                databaseService.Log("이미 처리한 파일입니다.: "+fileName, date, time);
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
                            databaseService.Log("g2bMap : " + g2bMap, date, time);
                            databaseService.Log("Send Message : " + g2bMap.get("message"), date, time);

                            if(g2bMap != null && "false".equals(g2bMap.get("flag"))) {
                                databaseService.Log("Send File Error : " + file.getPath(), date, time);
                            } else {
                                System.out.println(g2bMap);
                                //신규파일 이름 기록
                                FileUtils.writeStringToFile(writeFilePath, fileName+"\n", "UTF-8", true);
                                databaseService.Log("Send File Success : " + file.getPath(), date, time);
                                
                            }
                        }catch(Exception e){
                            databaseService.Log("Job File Fail01!!! : " + file.getPath(), date, time);
                            databaseService.Log("Job File Fail01!!! : " + databaseService.getPrintStackTrace(e), date, time);
                        }finally {
                            databaseService.moveFile(databaseService.folderPath + "/" + fileName, databaseService.targetFolder + "/" + fileName);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            databaseService.Log("Job File Fail02!!! : " + e.getMessage(), date, time);
            databaseService.Log("Job File Fail02!!! : " + databaseService.getPrintStackTrace(e), date, time);
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
}
