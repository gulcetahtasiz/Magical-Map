import java.util.ArrayList;

public class Node implements Comparable<Node> {
    int x, y; // coordinates of nodes
    int type;
    boolean markerForWritingImpassable= false; // marker for mark the nodes which we realize the path is impassable
    int oldType; // stores the old type values to revert changes
    boolean visited = false;
    double time = Double.MAX_VALUE; // for djkstra algorithm.
    Node parent = null; // for djkstra algorithm.
    MyHashMap<Node, Double> neighbors = new MyHashMap<>(); // to store nodes which have edges between them
    ArrayList<Integer> wizardsChoices = new ArrayList<>(); // list of choices for the start node
    boolean tempWizardChanged = false; //marker for mark the changed nodes
    boolean tempLineOfSightChanged = false;

    Node(int x, int y, int type){
        this.x = x;
        this.y = y;
        this.type = type;
    }

    String getName(){
        return x + "-" + y;
    }

    double distanceTo(Node other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx*dx + dy*dy;
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.time, o.time);
    }
}
