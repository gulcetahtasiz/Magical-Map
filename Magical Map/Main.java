import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class Main {
    public static int objectiveCount = 0; // Keeps track of completed objectives
    public static int widthX; // Width of the grid in the X direction
    public static int widthY; // Width of the grid in the Y direction
    public static ArrayList<Node> changedNodes = new ArrayList<>(); // Stores nodes that changed if seen.
    public static ArrayList<Node> tempChangedNodes = new ArrayList<>(); // Stores nodes that changed if seen temporarily to be more timely efficient.


    public static void main(String[] args) {
        // Initialize node map which stores all nodes, and objectives list.
        MyHashMap<String, Node> nodeMap = new MyHashMap<>();
        ArrayList<Node> objectiveMap = new ArrayList<>();

        int radius = 0; // Visibility radius

        // Filenames for input and output files.
        try (BufferedReader nodeFile = new BufferedReader(new FileReader(args[0]));
             BufferedReader edgeFile = new BufferedReader(new FileReader(args[1]));
             BufferedReader objectiveFile = new BufferedReader(new FileReader(args[2]));
             BufferedWriter writer = new BufferedWriter(new FileWriter(args[3]))) {

            // Read visibility radius from the first line of the objective file
            String firstLine = objectiveFile.readLine();
            radius = Integer.parseInt(firstLine.trim());

            // Read grid dimensions from the first line of the node file
            String[] gridDimensions = nodeFile.readLine().split(" ");
            widthX = Integer.parseInt(gridDimensions[0]);
            widthY = Integer.parseInt(gridDimensions[1]);

            // Parse nodes from node file and add them to the node map
            String line;
            while ((line = nodeFile.readLine()) != null) {
                String[] parts = line.split(" ");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int type = Integer.parseInt(parts[2]);
                Node node = new Node(x, y, type);
                nodeMap.put(node.getName(), node);
            }

            // Parse edges and if there are edges between two nodes, add one to the others neighbors hashmap.
            while ((line = edgeFile.readLine()) != null) {
                String[] parts = line.split(" ");
                String[] nodes = parts[0].split(",");

                String sourceName = nodes[0];
                String targetName = nodes[1];
                double time = Double.parseDouble(parts[1]);

                Node source = nodeMap.get(sourceName);
                Node target = nodeMap.get(targetName);

                if (source != null && target != null) {
                    source.neighbors.put(target, time);
                    target.neighbors.put(source, time);
                }
            }

            // Parse objectives and update the objective map
            while ((line = objectiveFile.readLine()) != null) {
                String[] parts = line.split(" ");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                Node node = nodeMap.get(x + "-" + y);

                if (parts.length > 2) {
                    for (int i = 2; i < parts.length; i++) {
                        node.wizardsChoices.add(Integer.parseInt(parts[i]));
                    }
                }
                objectiveMap.add(node);
            }

            // Process objectives
            for (int i = 0; i < objectiveMap.size() - 1; i++) {
                Node startNode = objectiveMap.get(i);
                Node endNode = objectiveMap.get(i + 1);

                // If there is no wizard's choices (type1 and type2), move to the path without wizards help.
                if (startNode.wizardsChoices.isEmpty()) {
                    ArrayList<Node> initialPath = dijkstra(nodeMap, startNode, endNode);
                    ArrayList<Node> finalPath = checkVisibilityAndRecalculate(nodeMap, initialPath, startNode, endNode, radius);
                    if (finalPath == null) {
                        writer.write("Path from " + startNode.getName() + " to " + endNode.getName() + " is impassable!\n");
                    } else {
                        // Rota geçerliyse, finalPath'i dosyaya yazdır, sonra "Objective n reached!"
                        printPath(finalPath, writer);
                    }
                }
                // If there is wizard's choices (type3), move on the path with wizards help.
                else {
                    // Process wizard's choices and find the best path
                    ArrayList<Integer> types = startNode.wizardsChoices;
                    ArrayList<Node> bestPath = null;
                    ArrayList<Node> lastPath= null;
                    double shortestTime = Double.MAX_VALUE;
                    int bestChoice = -1;

                    ArrayList<Node> listForReverting = new ArrayList<>();
                    // Try the probable paths for all choices
                    for (int choice : types) {
                        // temporarily make passable to the nodes which are the selected types
                        for (Node node : nodeMap.values()) {
                            if (node.type == choice) {
                                node.oldType = node.type;
                                node.type = 0;
                                listForReverting.add(node);
                            }
                        }

                        // temporarily make passable to the nodes which type is changed by line-of-sight.
                        for (Node node : changedNodes) {
                            if (node.oldType == choice) {
                                node.type = 0;
                                node.tempWizardChanged = true;
                                listForReverting.add(node);
                            }
                        }

                        // find the new nodes with djkstra for finding the new path with changed nodeMap.
                        ArrayList<Node> candidatePath = dijkstra(nodeMap, startNode, endNode);

                        // The time it takes to moving on the path.
                        double candidateTime = getPathTime(candidatePath);

                        // With comparing, find the shortest path with given wizard choices.
                        if (candidatePath != null && candidateTime < shortestTime) {
                            shortestTime = candidateTime;
                            bestPath = candidatePath;
                            bestChoice = choice;
                        }

                        // Revert temporary changes to go back old nodeMap before trying the other choice
                        for (Node node: listForReverting) {
                            if (node.oldType == choice  && !containsNodeWithType(changedNodes,node)) {
                                node.type = choice;
                                node.tempWizardChanged = false;
                            }
                            if (node.tempWizardChanged&&node.type!=choice) {
                                node.type = 1;
                                node.tempWizardChanged = false;
                            }
                        }
                    }

                    // After finding the bestPath, make every node passable with the given choice and on the path.
                    for (Node node : nodeMap.values()) {
                        if (node.oldType == bestChoice || node.type == bestChoice) {
                            node.type = 0;
                            node.oldType = 0;
                        }
                    }

                    // go on the bestPath with line-of-sight checks
                    lastPath = checkVisibilityAndRecalculate(nodeMap, bestPath, startNode, endNode, radius);

                    if (bestChoice != -1 &&  lastPath != null) {
                        writer.write("Number " + bestChoice + " is chosen!\n");
                        printPath(lastPath, writer);
                    } else {
                        writer.write("There is no path between " + startNode.getName() + " to " + endNode.getName() + "\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns the shortest path without line-of-sight control
    public static ArrayList<Node> dijkstra(MyHashMap<String, Node> nodeMap, Node startNode, Node endNode) {

        // Initialize a MinBinaryHeap for efficiently managing nodes based on their shortest time.
        MinBinaryHeap binaryHeap = new MinBinaryHeap(nodeMap.size);
        startNode.time = 0.0; // Set the starting node's time to 0
        binaryHeap.add(startNode);

        while (!binaryHeap.isEmpty()) { // Process nodes until the heap is empty
            Node current = binaryHeap.poll(); // Extract the node with the smallest time
            if (current.visited) continue; // if current is already visited, don't add it to the path
            current.visited = true;

            // Get all neighbors of the current node
            ArrayList<Node> keys = current.neighbors.keys();
            ArrayList<Double> values = current.neighbors.values();

            // Iterate through all neighbors
            for (int i = 0; i < keys.size(); i++) {
                Node neighbor = keys.get(i);
                double edgeTime = values.get(i); // Get the weight of the edge to the neighbor

                if (neighbor.type == 1) {
                    continue; // Skip nodes that have type 1
                }

                if (!neighbor.visited) {
                    // Calculate the potential new shortest time to reach the neighbor
                    double newTime = current.time + edgeTime;
                    if (newTime < neighbor.time) {
                        neighbor.time = newTime; // Update the neighbor's shortest time
                        neighbor.parent = current; // Set the current node as the neighbor's parent
                        binaryHeap.add(neighbor); // Add the neighbor to the binary heap for processing
                    }
                }
            }
        }

        // Build the shortest path by backtracking from the endNode using parent references
        ArrayList<Node> path = new ArrayList<>();
        Node current = endNode; // Move to the parent node

        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path); // Reverse the path to get the correct order from startNode to endNode

        // clear all nodes' properties for future algorithm runs
        for (Node node : nodeMap.values()) {
            node.time = Double.MAX_VALUE;
            node.visited = false;
            node.parent = null;
        }
        return (path.size() > 1) ? path : null;
    }

    // Returns the shortest path with line-of-sight control, and recalculate if the path is impassable.
    public static ArrayList<Node> checkVisibilityAndRecalculate(MyHashMap<String, Node> nodeMap, ArrayList<Node> initialPath, Node startNode,Node endNode, int radius) {

        ArrayList<Node> path = new ArrayList<>(initialPath); // Copy the initial path to allow modifications

        while (true) {
            boolean foundObstacle = false; // Tracks if any obstacle is found in the current iteration

            // Iterate through the nodes in the path
            for (int i = 0; i < path.size(); i++) {
                Node current = path.get(i);

                // If the endNode is reached and is the last node in the path, return the current path
                if (current.equals(endNode) && i == path.size() - 1) {
                    return path;
                }

                // Check visibility and obstacles for nodes further along the path
                for (int j = i + 1; j < path.size(); j++) {
                    Node next = path.get(j);

                    // // Check all nodes within the visibility radius of the current node
                    nodesInRadius(nodeMap, current, radius);

                    // If an obstacle is found in the visibility radius
                    if (containsNodeWithType(tempChangedNodes,next) && next.type!=0) {
                        tempChangedNodes.clear(); // Clear temporary changed nodes to avoid big time complexity
                        current.markerForWritingImpassable=true; // Mark current node as impassable

                        // Recalculate the path from the current node to the end node
                        ArrayList<Node> newPath = dijkstra(nodeMap, current, endNode);

                        if (newPath == null) {
                            return null; // Return null if no valid path exists
                        } else {
                            // Merge the original path up to the current node with the new path
                            ArrayList<Node> merged = new ArrayList<>(path.subList(0, i + 1));
                            if (newPath.size() > 1) {
                                merged.addAll(newPath.subList(1, newPath.size()));
                            }
                            path = merged; // Update the path with the merged path
                        }
                        break; // Exit the inner loop as the path has been recalculated
                    }
                }
            }
            // If no obstacles were found, the path is valid and can be returned
            if (!foundObstacle) {
                return path;
            }
        }
    }

    // Returns the total time from start node to end node on the map
    public static double getPathTime(ArrayList<Node> path) {
        if (path == null || path.size() < 2) return Double.MAX_VALUE;

        double totalTime = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Node current = path.get(i - 1);
            Node next = path.get(i);
            Double edgeTime = current.neighbors.get(next);
            if (edgeTime == null) return Double.MAX_VALUE; // Invalid path if edge doesn't exist
            totalTime += edgeTime;
        }
        return totalTime;
    }

    // Checks the nodes which are in the line of sight and makes them impassable, then add them to the changedNodes Arraylist.
    public static void nodesInRadius(MyHashMap<String, Node> nodeMap, Node currentNode, int radius) {

        int x = currentNode.x;
        int y = currentNode.y;
        // Check only the part of grid that line-of-sight in it to avoid unnecessary controls.
        for(int i = x-radius; i <=x+radius ;i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                if(i>=0 && i<=widthX && j>=0 && j<=widthY){
                    String key = i + "-" + j;
                    Node node = nodeMap.get(key);
                    if (node != null) {
                        // if node is in the line of sight and has a type value >= 2, make them impassable
                        if (currentNode.distanceTo(node) <= radius*radius && node.type >= 2 && !node.tempLineOfSightChanged) {
                            node.oldType = node.type;
                            node.type = 1;

                            // add them to the necessary Arraylists.
                            changedNodes.add(node);
                            tempChangedNodes.add(node);
                        }
                    }
                }
            }
        }
    }

    // Writes the path to the output file.
    public static void printPath(ArrayList<Node> path, BufferedWriter writer) throws IOException {
        if (path == null) {
            writer.write("Path is impassable!\n");
            return;
        }
        for (int i = 1; i < path.size(); i++) {
            Node node = path.get(i);

            writer.write("Moving to " + node.getName() + "\n");
            if(node.markerForWritingImpassable==true && i!=path.size()-1){ // if we realize that path is impassable at the current node
                writer.write("Path is impassable!\n");
                node.markerForWritingImpassable=false;
            }
        }
        objectiveCount++;
        writer.write("Objective " + objectiveCount + " reached!\n");
    }

    // If the given Arraylist has the given node, returns true
    public static boolean containsNodeWithType(ArrayList<Node> list, Node node1) {
        for (Node node : list) {
            if (node==node1) {
                return true;
            }
        }
        return false;
    }

}




