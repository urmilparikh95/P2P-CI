import java.util.*;

public class RFC {

    public int number;
    public String title;
    public List<String> hosts;

    public RFC(int number, String title) {
        this.number = number;
        this.title = title;
        hosts = new ArrayList<>();
    }

    public void addHost(String hostname) {
        this.hosts.add(hostname);
    }

    public void removeHost(String hostname) {
        this.hosts.remove(hostname);
    }

}