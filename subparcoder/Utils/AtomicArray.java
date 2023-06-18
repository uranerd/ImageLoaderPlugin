package subparcoder.Utils;

public class AtomicArray<T> {
    /*This class is used so we can update arrays in other threads*/
    private T[] array;
    private boolean updated = false;

    public AtomicArray(T[] array) {
        this.array = array;
    }

    public void set(T[] array) {
        this.array = array;
        this.updated = true;
    }

    public boolean hasArray() {
        return this.array != null;
    }

    public boolean isArrayUpdated() {
        return this.updated;
    }

    public T[] get() {
        return this.array;
    }
}
