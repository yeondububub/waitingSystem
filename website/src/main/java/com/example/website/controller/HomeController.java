package com.example.website.controller;

import com.example.common.QueueStatusResponse;
import com.example.website.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final WaitingQueueService waitingQueueService;

    @GetMapping("/index")
    public String home(@ModelAttribute(name = "userId") Long userId, Model model) {
        QueueStatusResponse response = waitingQueueService.accessibleCheck(userId);
        if(response.accessible()) {
            return "index";
        }

        model.addAttribute("rank", response.rank());
        return "waiting-room";
    }
}
