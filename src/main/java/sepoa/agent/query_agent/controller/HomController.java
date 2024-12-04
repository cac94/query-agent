package sepoa.agent.query_agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomController {
    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html 파일을 반환
    }
}
