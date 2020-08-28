package cn.tx.lock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class DistributeLock1 {


    ZkClient zkClient;


    public DistributeLock1(){
       zkClient = new ZkClient("192.168.0.108:2181", 60000, 2000);
        boolean exists = zkClient.exists("/lock");
        if(!exists){
            zkClient.createPersistent("/lock");
        }
    }


    class Node{

        private String path;

        private Thread thread;

        public Node(String path, Thread thread) {
            this.path = path;
            this.thread = thread;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Thread getThread() {
            return thread;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }
    }


    public Node createNode(){
        String path = zkClient.createEphemeralSequential("/lock/node", "N");
        Node node = new Node(path, Thread.currentThread());
        return node;
    }


    public Node lock(){
        //创建节点
        Node node = createNode();
        if (!tryAcqire(node)) {
            LockSupport.park();
        }
        return node;
    }

    public void unlock(Node node){
        zkClient.delete(node.getPath());
    }


    public boolean tryAcqire(Node node){
        //定义一个锁的标识
        boolean isLock = false;

        List<String> list = zkClient.getChildren("/lock")
                .stream()
                .sorted()
                .map(n -> "/lock/"+n).collect(Collectors.toList());
        //获得第一个
        String firstPath = list.get(0);
        if(node.getPath().equals(firstPath)){
            isLock = true;
        }else{
            //获得前一个节点的路径
            String prePath = list.get(list.indexOf(node.getPath()) - 1);
            zkClient.subscribeDataChanges(prePath, new IZkDataListener(){

                @Override
                public void handleDataChange(String dataPath, Object data) throws Exception {

                }

                @Override
                public void handleDataDeleted(String dataPath) throws Exception {
                    System.out.println("删除："+dataPath);
                    LockSupport.unpark(node.getThread());
                    zkClient.unsubscribeDataChanges(dataPath, this);
                }
            });

        }

        return isLock;
    }

}
