package cn.tx.lock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {


    public static void main(String[] args) throws Exception {

        Path path = Paths.get("d:/test.txt");
        if(!Files.exists(path)){
            Files.createFile(path);
        }

        DistributeLock1 lock = new DistributeLock1();
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0; i < 1000 ; i++) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    DistributeLock1.Node node = lock.lock();
                    String numStr = null;
                    try {
                        numStr = Files.lines(path).findFirst().orElse("0");
                        Integer integer = new Integer(numStr);
                        integer++;
                        String num  = new String(integer+"");
                        Files.write(path, num.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        lock.unlock(node);
                    }
                }
            });
        }

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println(Files.lines(path).findFirst().orElse("0"));
        System.out.println("-------------------");
    }
}
