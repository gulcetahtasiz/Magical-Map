
// A MinBinaryHeap implementation for managing nodes based on their time values.
public class MinBinaryHeap {
    private Node[] heap; // Array to store the heap elements.
    private int size;

    public MinBinaryHeap(int capacity) {
        heap = new Node[capacity]; //capacity is the maximum number of elements the heap can hold
        size = 0;
    }

    public void add(Node node) {
        heap[size] = node; // Add the node at the end of the heap.
        bubbleUp(size); // Restore the heap property by moving the node up.
        size++;
    }

    public Node poll() { // Removes and returns the node with the minimum "time" value from the heap.
        if (size == 0) return null;
        Node min = heap[0]; // The root of the heap.
        heap[0] = heap[size- 1];
        size--;
        bubbleDown(0); // Restore the heap property by moving the root down.
        return min;
    }

    public boolean isEmpty() {
        return size ==0;
    }

    private void bubbleUp(int index) { // Restores the heap property by moving a node up the tree.
        while (index > 0) {
            int parentIndex = (index - 1)/ 2; // Calculate the parent index.
            if (heap[index].time >= heap[parentIndex].time) break; // Stop if heap property is satisfied.
            swap(index, parentIndex); // Swap with the parent.
            index = parentIndex; // Move up to the parent index.
        }
    }

    private void bubbleDown(int index) { // Restores the heap property by moving a node down the tree.
        while (true) {
            int leftChild = (2 * index) + 1; // Index of the left child.
            int rightChild = (2 * index) + 2; // Index of the right child.
            int smallest = index;

            // Check if the left child is smaller.
            if (leftChild < size && heap[leftChild].time < heap[smallest].time) {
                smallest = leftChild;
            }
            // Check if the right child is smaller.
            if (rightChild < size && heap[rightChild].time< heap[smallest].time) {
                smallest = rightChild;
            }
            // Stop if the smallest node is already the current node.
            if (smallest == index) break;
            swap(index, smallest); // Swap with the smallest child.
            index = smallest; // Move down to the smallest child's index.
        }
    }

    // Swaps two nodes in the heap.
    private void swap(int i, int j) {
        Node temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
}



