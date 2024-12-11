package sepoa.agent.query_agent.service;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import sepoa.agent.query_agent.model.SendFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Service
public class DatabaseService {
    @Value("${sepoa.url}")
    public String recvUrl;

    @Value("${sepoa.target}")
    public String targetFolder;

    @Value("${sepoa.recv}")
    private String folderRecv;

    @Value("${sepoa.send}")
    private String folderSend;

    @Value("${sepoa.types}")
    private String confTypes;

    @Value("${sepoa.types.file}")
    private String confTypesFile;

    @Value("${sepoa.log}")
	private String logFolder;

    @Value("${sepoa.token.session}")
	public String sessionToken;

    @Value("${sepoa.token.service}")
	public String serviceToken;

    private final JdbcTemplate jdbcTemplate;
    public String checkFile = "";
    public String folderPath = "";
    public List<String> types = new ArrayList<String>();

    public DatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void init() {
        // 기본 생성자로 생성시 현재 시간과 날짜 정보를 가진 Date 객체가 생성됩니다.
        Date nowDate = new Date();

        // 원하는 형태의 포맷으로 날짜, 시간을 표현하기 위해서는 SimpleDateFormat 클래스를 이용합니다.
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String year = dateFormat.format(nowDate).substring(0, 4) ;
        String month = dateFormat.format(nowDate).substring(4, 6) ;
		String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));		        

        this.checkFile = this.logFolder.replace("/", File.separator)+"/"+year+"/"+month+"/" +"RecvFileList.txt";
        this.folderPath = this.folderRecv + formattedDate;
        this.types.addAll(Arrays.asList(confTypes.split(","))); 
        this.types.addAll(Arrays.asList(confTypesFile.split(","))); 
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    public int executeUpsert(String sql) {
        return jdbcTemplate.update(sql);
    }

    public String sendFile(SendFile sendFile) {
        String rtn = "";
		Date nowDate = new Date();

		// 원하는 형태의 포맷으로 날짜, 시간을 표현하기 위해서는 SimpleDateFormat 클래스를 이용합니다.
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

		String date = dateFormat.format(nowDate);
        String time = timeFormat.format(nowDate);

        try{
			String lastTwo = sendFile.getFilename().substring(sendFile.getFilename().length() - 2);
			String sXml = sendFile.getContent(); //xml이 아니면 그냥 Base64 encoding 상태로 저장
			if(".0".equals(lastTwo)) { //xml인 경우 디코드해서 저장
                sXml = new String(Base64.getDecoder().decode(sXml.getBytes(StandardCharsets.UTF_8)));
            }else {
                byte[] decodedBytes = Base64.getDecoder().decode(sXml);
                try (FileOutputStream fos = new FileOutputStream(folderSend + sendFile.getFilename())) {
                    fos.write(decodedBytes);
                }
            }
        } catch (Exception e) {
            Log("Failed to move file: " + e.getMessage(), date, time);
            return rtn = e.getMessage();
        }

        return rtn;
    }
 
	public String callRestInterface(String url, String key, String xml, String type) throws Exception {
		// 기본 생성자로 생성시 현재 시간과 날짜 정보를 가진 Date 객체가 생성됩니다.
		Date nowDate = new Date();

		// 원하는 형태의 포맷으로 날짜, 시간을 표현하기 위해서는 SimpleDateFormat 클래스를 이용합니다.
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

		String date = dateFormat.format(nowDate);
        String time = timeFormat.format(nowDate);
		
		Log("Call URL : " + url, date, time);
		Log("key : " + key, date, time);
		Log("xml : " + xml, date, time);
		Log("type : " + type, date, time);

	    URL interfaceURL = null;
	    String readLine = null;
	    StringBuilder buffer = null;
	    OutputStream outputStream = null;
	    BufferedReader bufferedReader = null;
	    BufferedWriter bufferedWriter = null;
	    HttpURLConnection urlConnection = null;
	    
	    int connTimeout = 5000;
	    int readTimeout = 3000;
	    
	    interfaceURL = new URL(url);

		SSLContext ctx = SSLContext.getInstance("TLS");
		X509TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
    
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
    
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		ctx.init(null, new TrustManager[] { tm }, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
		
		String token = "";
		token = "Basic ";
		String authInfo = "f6547794-5c6c-4ada-b13b-ca17c824d509:d1459227-f142-412e-b534-18b11e5bb306";
		token += Base64.getEncoder().encodeToString(authInfo.getBytes(StandardCharsets.UTF_8));
		//xml = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        
        urlConnection = (HttpURLConnection)interfaceURL.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setConnectTimeout(connTimeout);
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setRequestProperty("Content-Type", "application/json;");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Authorization", token);
        urlConnection.setDoOutput(true);
        urlConnection.setInstanceFollowRedirects(true);
		String jsonData = "";
		jsonData += "{";
		jsonData += "\"token\":\"" + sessionToken + "\",";
		jsonData += "\"do\":\"" + serviceToken + "\",";
		jsonData += "\"data\":{";
		jsonData += "\"KEY\":\"" + key + "\",";
		jsonData += "\"TYPE\":\"" + type.toUpperCase() + "\",";
		jsonData += "\"XML\":\"" + xml + "\"";
		jsonData += "}";
		jsonData += "}";
		interfaceURL = new URL(url);

        outputStream = urlConnection.getOutputStream();

        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
        bufferedWriter.write(jsonData);
        bufferedWriter.flush();
        

        buffer = new StringBuilder();
        if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) 
        {
            bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),"UTF-8"));
            while((readLine = bufferedReader.readLine()) != null) 
            {
                buffer.append(readLine).append("\n");
            }
        }
        else 
        {
            buffer.append("\"code\" : \""+urlConnection.getResponseCode()+"\"");
            buffer.append(", \"message\" : \""+urlConnection.getResponseMessage()+"\"");
        }
        Log(buffer.toString(), date, time);
		return buffer.toString();
	}

    public boolean moveFile(String sourceFile, String targetFile) {
		Date nowDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

		String date = dateFormat.format(nowDate);
        String time = timeFormat.format(nowDate);
        
        Path source = Paths.get(sourceFile); // 원본 파일 경로
        Path target = Paths.get(targetFile); // 대상 파일 경로

        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // 파일 이동
            Log("File moved successfully.", date, time);
        } catch (Exception e) {
            Log("Failed to move file: " + e.getMessage(), date, time);
            return false;
        }
        return true;
    }
	
	// Helper method to read a file into a byte array
    public byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            fis.read(fileData);
            return fileData;
        }
    }
    
	public String getPrintStackTrace(Throwable e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}
	
	public Map<String, String> jsonToMap(String jsonString) {
        Gson gson = new Gson();

        // Define the type for Map<String, String>
        Type type = new TypeToken<Map<String, String>>() {}.getType();

        // Convert JSON string to Map<String, String>
        Map<String, String> map = gson.fromJson(jsonString, type);
        return map;
    }
	
	public void Log(String l, String date, String time) {
		System.out.println(l);
		String path = logFolder;
        String logFileName = "sepoa" + date + ".log";  
        File logFile = new File(path + File.separator + logFileName); 
        
		try {
			FileUtils.writeStringToFile(logFile, "\r\n[" + time + "]" + l, "UTF-8", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	public Properties readProperties(String propFileName) {
		Properties prop = new Properties();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

		try {
			if (inputStream != null) {
				prop.load(inputStream);
				return prop;
			} else {
				throw new FileNotFoundException("프로퍼티 파일 '" + propFileName + "'을 resource에서 찾을 수 없습니다.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
