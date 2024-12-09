package sepoa.agent.query_agent.model;

public class SendFile {
    private String fileName;
    private String content;

    // Constructors
    public SendFile() {}

    public SendFile(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
