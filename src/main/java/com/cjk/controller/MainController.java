package com.cjk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;


@Controller
public class MainController {

	@GetMapping("/index")
	@ResponseBody
	public String index() {
		return JSON.toJSONString("a:cjenk");
	}


}
