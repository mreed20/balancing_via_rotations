import java.util.*;
import java.util.function.Consumer;

/**
 * A non-empty binary tree rooted at this node.
 * Duplicate nodes are not permitted.
 */
public class BST
{

    // root node of this tree
    BSTNode root = null;

    /**
     * The only (public) way to construct this tree is with a non-empty list of keys.
     */
    public BST(Collection<Integer> keys)
    {
        if (keys.isEmpty()) throw new IllegalArgumentException("keys cannot be empty");
        keys.forEach(this::insert);
    }

    /**
     * Used in makeAlmostCompleteBinaryTree
     */
    BST(BSTNode root)
    {
        if (root == null) throw new IllegalArgumentException();
        this.root = root;
    }

    /**
     * Return the number of nodes in the binary search tree.
     */
    public int size()
    {
        return root.size();
    }

    /**
     * Returns the height of this tree, which is the number of edges on the longest path from
     * the root to a leaf.
     * <p>
     * Works Cited: Weiss, "Data Structures & Problem Solving Using Java", 4th ed., p. 667
     */
    public int height()
    {
        return root.height();
    }


    /**
     * Insert a new node into this tree.
     * <p>
     * works cited: CLRS 12.3
     *
     * @param newKey New key to add to the tree.
     * @throws IllegalArgumentException If the given key is already in the tree.
     */
    public void insert(int newKey)
    {
        BSTNode newNode = new BSTNode(newKey);
        BSTNode parent = null;
        BSTNode currentNode = root;
        while (currentNode != null)
        {
            parent = currentNode;
            if (newNode.key < currentNode.key)
            {
                currentNode = currentNode.left;
            }
            else if (newNode.key > currentNode.key)
            {
                currentNode = currentNode.right;
            }
            else
            {
                throw new IllegalArgumentException("key " + newNode.key + " already in tree!");
            }
        }

        newNode.parent = parent;
        if (parent == null)
        {
            root = newNode;
        }
        else if (newNode.key < parent.key)
        {
            parent.left = newNode;
        }
        else
        {
            parent.right = newNode;
        }
    }

    /**
     * If the key is present in the tree, find the associated BinaryNode.
     * Otherwise return null.
     */
    public Optional<BSTNode> search(int key)
    {
        return root.search(key);
    }

    /**
     * Gets the node of ith rank in the tree, which is the node that is larger than exactly
     * i other nodes in the tree. So the 0th rank is the smallest element in the tree, and the
     * (n-1)th rank is the largest (for a tree of n-1 keys).
     */
    public Optional<BSTNode> select(int i)
    {
        return root.select(i);
    }

    /**
     * Left rotate the edge whose endpoints are x and x.right;
     * Return the new parent.
     * <p>
     * Works cited: CLRS 13.2
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public BSTNode rotateLeft(BSTNode x)
    {
        boolean reassignRoot = x.parent == null;

        BSTNode y = x.right;
        x.right = y.left;
        if (y.left != null)
        {
            y.left.parent = x;
        }

        y.parent = x.parent;
        if (reassignRoot)
        {
            root = y;
        }
        else if (x == x.parent.left)
        {
            x.parent.left = y;
        }
        else
        {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
        return y;
    }

    /**
     * Right rotate the edge whose endpoints are x and x.left;
     * Return the new parent.
     * <p>
     * Works cited: done as solution to 13.2-1 in CLRS
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public BSTNode rotateRight(BSTNode y)
    {
        boolean reassignRoot = y.parent == null;

        BSTNode x = y.left;
        y.left = x.right;
        if (x.right != null)
        {
            x.right.parent = y;
        }

        x.parent = y.parent;
        if (reassignRoot)
        {
            root = x;
        }
        else if (y == y.parent.right)
        {
            y.parent.right = x;
        }
        else
        {
            y.parent.left = x;
        }
        x.right = y;
        y.parent = x;
        return x;
    }

    /**
     * Perform an in-order walk of the tree, running the visit function on each node.
     */
    public void inOrder(Consumer<BSTNode> visit)
    {
        root.inOrder(visit);
    }

    /**
     * Perform an in-order walk of the tree, returning the keys encountered on the walk
     * in the order they were encountered.
     */
    public List<Integer> inOrderKeys()
    {
        return root.inOrderKeys();
    }

    /**
     * Perform an in-order walk of the tree, returning the nodes encountered on the walk
     * in the order they were encountered.
     */
    public List<BSTNode> inOrderNodes()
    {
        return root.inOrderNodes();
    }

    /**
     * Performs an in-order walk of the tree, running the visit function on each node.
     */
    public void postOrder(Consumer<BSTNode> visit)
    {
        root.postOrder(visit);
    }


    /**
     * returns set of all the keys in this tree.
     */
    public Set<Integer> keySet()
    {
        return root.keySet();
    }

    /**
     * Is this BST equal to another object o?
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BST that = (BST) o;
        return Objects.equals(root, that.root);
    }

    /**
     * Computes the hashcode of this BST.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(root);
    }
}