package sepoa.agent.query_agent.model;

public class SendFile {
    private String filename;
    private String content;

    // Constructors
    public SendFile() {}

    public SendFile(String filename, String content) {
        this.filename = filename;
        this.content = content;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
