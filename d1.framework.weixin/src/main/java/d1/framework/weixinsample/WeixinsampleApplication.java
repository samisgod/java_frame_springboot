package d1.framework.weixinsample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"d1"})
public class WeixinsampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeixinsampleApplication.class, args);
    }
}
