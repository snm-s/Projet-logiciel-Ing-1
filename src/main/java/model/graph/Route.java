package model.graph;

import java.util.ArrayList;
import java.util.List;

public class Route {
    private List<Node> nodes = new ArrayList<>();

    /**
     * Returns the nodes.
     * @return the List<Node>.
     */
    public List<Node> getNodes() {
        return nodes;
    }
}
