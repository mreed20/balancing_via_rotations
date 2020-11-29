import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Node in a binary tree.
 */
class BSTNode
{
    // key of the node
    final int key;

    // left and right children, all potentially null
    BSTNode left = null;
    BSTNode right = null;
    BSTNode parent = null;

    /**
     * Construct a BinaryNode with the given key, left child, right child, and parent.
     * All fields except for `key` may be null.
     */
    BSTNode(int key)
    {
        this.key = key;
    }

    /**
     * Return the number of nodes in the subtree rooted at this node.
     */
    int size()
    {
        int size = 1;  // the size is at least one, since this node is non-null (duh)
        if (left != null) size += left.size();
        if (right != null) size += right.size();
        return size;
    }

    /**
     * Returns the height of the subtree rooted at this node.
     */
    int height()
    {
        int h = 0;
        if (left != null) h = Math.max(h, 1 + left.height());
        if (right != null) h = Math.max(h, 1 + right.height());
        return h;
    }

    /**
     * If a node with the given key is present in the tree, return it.
     * Otherwise return an empty optional.
     */
    Optional<BSTNode> search(int key)
    {
        if (this.key == key)
        {
            return Optional.of(this);
        }
        else if (key < this.key)
        {
            if (this.left == null) return Optional.empty();
            else return this.left.search(key);
        }
        else
        {
            if (this.right == null) return Optional.empty();
            else return this.right.search(key);
        }
    }

    /**
     * Returns the BSTNode of rank i for the tree rooted at this node. The node of rank is the
     * node that is larger than exactly i other nodes in the tree. If the rank is invalid then
     * return an empty optional.
     * <p>
     * Works Cited: CLRS (with modifications).
     */
    Optional<BSTNode> select(int i)
    {
        if (i < 0 || i > size() - 1) return Optional.empty();

        int r = left != null ? left.size() : 0;
        if (i == r)
        {
            return Optional.of(this);
        }
        else if (i < r)
        { // x must be in the left subtree
            assert left != null;
            return left.select(i);
        }
        else
        {
            assert right != null;
            return right.select(i - r - 1);
        }
    }

    /**
     * Return smallest node in the subtree rooted at this node.
     */
    BSTNode minimum()
    {
        if (left == null)
        {
            return this;
        }
        else
        {
            return this.left.minimum();
        }
    }

    /**
     * Return largest node in the subtree rooted at this node.
     */
    BSTNode maximum()
    {
        if (right == null)
        {
            return this;
        }
        else
        {
            return this.right.maximum();
        }
    }


    /**
     * Return a list of keys visited from an in-order walk of the subtree rooted at this node.
     * If visit is non-null, runs the provided unary function on each node.
     */
    void inOrder(Consumer<BSTNode> visit)
    {
        if (left != null) left.inOrder(visit);
        visit.accept(this);
        if (right != null) right.inOrder(visit);
    }

    /**
     * Perform an in-order walk of the tree, returning the nodes encountered on the walk
     * in the order they were encountered.
     */
    List<BSTNode> inOrderNodes()
    {
        List<BSTNode> inOrderWalk = new ArrayList<>();
        inOrder(inOrderWalk::add);
        return inOrderWalk;
    }

    /**
     * Perform an in-order walk of the tree, returning the keys encountered on the walk
     * in the order they were encountered.
     */
    public List<Integer> inOrderKeys()
    {
        return inOrderNodes().stream().map(x -> x.key).collect(Collectors.toList());
    }

    /**
     * returns set of all the keys in this tree.
     */
    public Set<Integer> keySet()
    {
        return new HashSet<>(inOrderKeys());
    }


    /**
     * Return a list of keys visited from a post-order walk of the subtree rooted at this node.
     * If visit is non-null, runs the provided unary function on each node.
     */
    public void postOrder(Consumer<BSTNode> visit)
    {
        if (left != null) left.postOrder(visit);
        if (right != null) right.postOrder(visit);
        visit.accept(this);
    }

    /**
     * NOTE: we deliberately leave the parent from the recursive check since it leads
     * to an infinite loop
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BSTNode that = (BSTNode) o;
        return key == that.key &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }


    /**
     * Just like in equals, we exclude the parent from the hash.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(key, left, right);
    }
}
