import java.awt.desktop.SystemSleepEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
public class IdcDm {
    public static void main(String[] args) {
        //String url = args[2];
        //int numOfConnections =  args.length > 3 ? Integer.parseInt(args[3]) : 1;

        //String url = "https://archive.org/download/Mario1_500/Mario1_500.avi";
        //String url = "CentOS-6.10-x86_64-netinstall.iso.list";
        String url = "http://centos.activecloud.co.il/6.10/isos/x86_64/CentOS-6.10-x86_64-netinstall.iso";
        int numOfConnections = 4;

        Manager manager = new Manager(url, numOfConnections);
        manager.initURL();
    }
}