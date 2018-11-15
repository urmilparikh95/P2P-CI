import java.util.*;

public class RFC {

    public String number;
    public String title;
    public List<Peer> peers;

    public RFC(String number, String title) {
        this.number = number;
        this.title = title;
        peers = new ArrayList<>();
    }

}