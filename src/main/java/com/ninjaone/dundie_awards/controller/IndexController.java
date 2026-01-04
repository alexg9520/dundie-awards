package com.ninjaone.dundie_awards.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.services.ActivityService;
import com.ninjaone.dundie_awards.services.EmployeeService;


@Controller
@RequestMapping("/")
public class IndexController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ActivityService activityService;

    @GetMapping()
    public String getIndex(Model model) throws InvalidArgumentException, LookupException {
        model.addAttribute("employees", employeeService.findAll(0, 100, "id").getContent());
        model.addAttribute("activities", activityService.findAll(0, 100, "id"));
        model.addAttribute("totalDundieAwards", employeeService.getTotalAwards());
        return "index";
    }
}
