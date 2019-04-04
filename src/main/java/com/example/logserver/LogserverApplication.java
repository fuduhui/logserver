package com.example.logserver;

import ch.qos.logback.classic.net.SimpleSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogserverApplication {

	public static void main(String[] args) {
		try{
			String[] argv=new String[2];
			argv[0]="7000";
			argv[1] = LogserverApplication.class.getClassLoader().
					getResource("logback.xml").getPath();//获取文件路径
			MySimpleSocketServer.main(argv);
			//SimpleSocketServer.main(argv);
		}catch (Exception e){
			e.printStackTrace();
		}

		SpringApplication.run(LogserverApplication.class, args);
	}

}
