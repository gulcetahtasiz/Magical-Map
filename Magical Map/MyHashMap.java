import java.util.ArrayList;

public class MyHashMap<K, V> {

    private static class Entry<K, V> { // A private static class representing a key-value pair in the hash table.
        K key;
        V value;
        Entry<K, V> next; // Reference to the next entry in the chain to use in collisions

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final int INITIAL_CAPACITY = 23; // Initial size of the hash table.
    private static final float LOAD_FACTOR = 1; // Load factor to determine when to resize.
    public int size = 0; // Current number of key-value pairs in the map.
    private Entry<K, V>[] table; // Array of linked list entries representing the hash table

    @SuppressWarnings("unchecked")
    public MyHashMap() {
        table = new Entry[INITIAL_CAPACITY];
    }

    // Find he index in the hash table for the key.
    private int hash(K key) {
        return (key == null) ? 0 : Math.abs(key.hashCode() % table.length);
    }

    // Adds or updates a key-value pair in the hash table.
    public void put(K key, V value) {
        int index = hash(key);
        Entry<K, V> entry = table[index];
        // No collision, directly add the entry.
        if (entry == null) {
            table[index] = new Entry<>(key, value);
            size++;
        } else {
            // Collision handling via chaining.
            while (entry.next != null && !entry.key.equals(key)) {
                entry = entry.next;
            }
            if (entry.key.equals(key)) {
                entry.value = value; // Update the value if the key already exists.
            } else {
                entry.next = new Entry<>(key, value); // if not, // Add a new entry at the end of the chain.
                size++;
            }
        }
        if (size > LOAD_FACTOR * table.length) { //// Resize the table if the load factor exceeds the limit.
            resize();
        }
    }

    public V get(K key) { // Retrieves the value associated with a given key.
        int index = hash(key);
        Entry<K, V> entry = table[index];
        while (entry != null) {
            if (entry.key.equals(key)) {
                return entry.value; // If key is found, return the associated value.
            }
            entry = entry.next; // Move to the next entry in the chain.
        }
        return null;
    }

    // Check if given key exists
    public boolean containsKey(K key) {
        if (get(key) != null){
            return true;
        }
        return false;
    }

    // Retrieves all the values stored in the hash table.
    public ArrayList<V> values() {
        ArrayList<V> values = new ArrayList<>();
        for (Entry<K, V> entry : table) {
            while (entry != null) {
                values.add(entry.value);
                entry = entry.next;
            }
        }
        return values;
    }

    // Retrieves all the keys stored in the hash table.
    public ArrayList<K> keys() {
        ArrayList<K> keys = new ArrayList<>();
        for (Entry<K, V> entry : table) {
            while (entry != null) {
                keys.add(entry.key);
                entry = entry.next;
            }
        }
        return keys;
    }

    // Resizes the hash table when the load factor exceeds the limit.
    private void resize() {
        Entry<K, V>[] newTable = new Entry[table.length * 2]; // New table with double capacity.
        for (Entry<K, V> entry : table) {
            while (entry != null) {
                int index = Math.abs(entry.key.hashCode() % newTable.length); //Recalculate index.
                Entry<K, V> next = entry.next; // Store reference to the next entry in the chain.
                entry.next = newTable[index];  // Reassign the entry to the new table.
                newTable[index] = entry;  // Add the entry to the new index.
                entry = next;   // Move to the next entry in the chain.
            }
        }
        table = newTable; // Update the table reference to the new table.
    }
}
