package com.cjk.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cjk.zookeeper.component.ZkIdGenerator;


@Controller
public class MainController {
	
	@Autowired
	private ZkIdGenerator idGenerator;

	@GetMapping("/index")
	@ResponseBody
	public long index() {
		long id = idGenerator.nextId();
		return id;
	}


}
