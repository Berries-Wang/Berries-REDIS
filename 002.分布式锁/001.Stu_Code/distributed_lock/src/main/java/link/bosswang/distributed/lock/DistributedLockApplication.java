package link.bosswang.distributed.lock;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DistributedLockApplication {
    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(DistributedLockApplication.class);
        System.out.println("=====> DistributedLockApplication started");
    }
}
