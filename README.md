# switcher

一个基于特定策略选择代理的代理软件

# 使用

## 示例1

```java
import org.switcher.Switcher;

class Main {
    public static void main(String[] args) throws InterruptedException {
        Switcher switcher = new Switcher();
        // 启动代理
        switcher.boostrap().start();
        // 新增上游代理
        switcher.upstreamProxyManager.add("127.0.0.1", 10809);
        while (true) {
            // 每过1秒输出一次下载速度
            Thread.sleep(1000);
            System.out.println(switcher.speedRecorder.getSpeed());
        }
    }
}
```

## 示例2

```java
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.switcher.Switcher;

class Main {
    public static void main(String[] args) throws Exception {
        int numberOfProxies = 3;
        int firstPort = 8001;
        for (int i = 0; i < numberOfProxies; i++) {
            DefaultHttpProxyServer.bootstrap()
                    .withPort(firstPort + i)
                    .withThrottling(100 * 1024, 100 * 1024)
                    .start();
        }

        Switcher switcher = new Switcher();
        // 启动代理
        switcher.boostrap().start();
        // 新增上游代理
        for (int i = 0; i < numberOfProxies; i++) {
            switcher.upstreamProxyManager.add("127.0.0.1", firstPort + i);
        }
        while (true) {
            // 每过1秒输出一次下载速度
            Thread.sleep(1000);
            System.out.println(switcher.speedRecorder.getSpeed());
        }
    }
}
```