import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * All the static methods I need for the project.
 */
public class BalanceViaRotation
{

    /**
     * Perform the project's main experiment.
     */
    public static void main(String[] args)
    {
        for (int n : List.of(1000, 1100, 1200))
        {
            for (Algorithm a : Algorithm.values())
            {
                // Perform 5 experiments for each possible pairing of algorithm and key size (n).
                performExperiments(a, n);
                System.out.println();
            }
        }
    }

    static void performExperiments(final Algorithm algo, final int n)
    {
        assert n >= 1;

        // create the keys, which we shuffle each trial
        final List<Integer> keys = IntStream
                .range(0, n)
                .boxed()
                .collect(Collectors.toList());

        System.out.println(algo + " with n=" + n);

        // perform the trials
        for (int i = 0; i < 5; i++)
        {

            // create the trees
            BST S = makeAlmostCompleteBST(keys);
            BST T = makeAlmostCompleteBST(keys);

            // spice things up by randomly rotating edges in S
            randomlyRotate(S);

            // run the requested algorithm
            switch (algo)
            {
                case A1 -> {
                    Statistic stat = A1(S, T);
                    System.out.println("rotations actual = " + stat.rotationsActual + ", expected = " + stat.rotationsExpected);
                }
                case A2 -> {
                    Statistic stat = A2(S, T);
                    System.out.println("rotations actual = " + stat.rotationsActual + ", expected = " + stat.rotationsExpected + " +- 1");
                }
                case A3 -> {
                    Statistic stat = A3(S, T);
                    System.out.println("rotations actual = " + stat.rotationsActual + ", upper bound = " + stat.rotationsExpected);
                }
            }
        }
    }

    /**
     * Randomly rotate edges in a given tree.
     */
    static void randomlyRotate(BST t)
    {
        // Choose edges (well, keys really) as candidates for rotation.
        Random r = new Random();
        var nodesToRotate = t.inOrderNodes().stream()
                // the rotation methods don't work on leaves, so filter them out
                .filter(n -> n.left != null || n.right != null)
                // do not rotate a node with probability 0.01
                .filter(x -> r.nextInt(100) != 0)
                // rotate the remaining nodes with probability 0.5
                .filter(x -> r.nextBoolean())
                .collect(Collectors.toList());

        // randomly rotate edges of S
        for (var node : nodesToRotate)
        {
            // make sure we don't have a leaf.
            if (node.left != null)
            {
                t.rotateRight(node);
            }
            else if (node.right != null)
            {
                t.rotateLeft(node);
            }
        }
    }

    /**
     * Calculate the value of p, used Theorem 1 in the paper, for a nearly complete tree of size n.
     */
    static int p(int n)
    {
        final int h = levels(n);
        final int v = (int) Math.pow(2, h - 1) + (int) Math.pow(2, h - 2);
        if (Math.pow(2, h - 1) <= n && n <= v - 2)
        {
            return +1;
        }
        else if (n == v - 1)
        {
            return 0;
        }
        else if (v <= n && n <= (int) Math.pow(2, h) - 1)
        {
            return -1;
        }
        else
        {
            throw new RuntimeException("shoot");
        }
    }

    /**
     * A_1 from the paper
     *
     * @param S Arbitrary binary tree of n nodes
     * @param T Almost complete binary tree
     */
    static Statistic A1(BST S, BST T)
    {
        assertSanity(S, T);

        // track the number of rotations performed across all steps
        int numRotations = 0;

        // (Step 2 from paper) compute rootT as in equation (1)
        final int rootTRank = computeRootT(T.size());
        final int csRootT = sizeOfForearms(S.select(rootTRank).orElseThrow());

        // (steps 3 and 4) If the node at rootT is not already in the root position, rotate it upwards so it becomes that way.
        numRotations += rotateNodeToRoot(S, rootTRank);

        // (step 5) transform the tree into just its forearms.
        numRotations += makeForearms(S, Set.of()).size();

        // make a copy of T
        BST TPrime = makeAlmostCompleteBST(T.keySet());
        assert TPrime.equals(T);

        // convert TPrime to a tree with only forearms, recording the sequence of rotations we perform
        List<HistoryEntry> history = makeForearms(TPrime);

        // apply the rotations in reverse to S to get the original T
        applyInvertedRotations(S, history);
        numRotations += history.size();

        assert S.equals(T);

        int n = S.size();
        return new Statistic(
                numRotations,
                2 * n - 2 * (int) Math.floor(Utilities.logBase(2, n)) + p(n) - csRootT - 1);
    }

    /**
     * A_2 from the paper, which is basically a version of A_1 that performs fewer unnecessary rotations.
     *
     * @param S Arbitrary binary tree of n nodes
     * @param T Almost complete binary tree
     */
    static Statistic A2(BST S, BST T)
    {
        assertSanity(S, T);

        // find the roots of all the maximal identical subtrees of S and T
        Set<Integer> maximalCommonSubtrees = findMaximalIdenticalSubtrees(S, T);

        // Calculate this now before performing any rotations.
        final int subtreeTerm = maximalCommonSubtrees.stream()
                .map(S::search)
                .map(Optional::orElseThrow)
                .mapToInt(BSTNode::size)
                .sum();

        if (maximalCommonSubtrees.size() == 0)
        {
            // S and T share no common subtrees, so apply algorithm 1 normally.
            return A1(S, T);
        }
        else
        {
            // Rotate the soon-to-be root of S into position.
            final int rootTRank = computeRootT(T.size());
            final int csRootT = sizeOfForearms(S.select(rootTRank).orElseThrow());
            final int rotationsRoot = rotateNodeToRoot(S, rootTRank);

            // Make the tree into just forearms, not rotations the maximal identical subtrees.
            final int rotationsForearms = makeForearms(S, maximalCommonSubtrees).size();

            // make a copy of T
            BST TPrime = makeAlmostCompleteBST(T.keySet());
            assert TPrime.equals(T);
            assert TPrime.keySet().containsAll(maximalCommonSubtrees);

            // Convert TPrime to a tree with only forearms,
            // recording the sequence of rotations we perform.
            List<HistoryEntry> history = makeForearms(TPrime, maximalCommonSubtrees);

            // apply the rotations in reverse to S to get the original T
            applyInvertedRotations(S, history);

            assert S.equals(T);
            final int n = S.size();
            return new Statistic(
                    rotationsRoot + rotationsForearms + history.size(),
                    2 * n - 2 * (int) Math.floor(Utilities.logBase(2, n)) - 2 * subtreeTerm - csRootT);
        }
    }

    /**
     * Return the combined number of nodes in the left and right forearm of the given node.
     */
    static int sizeOfForearms(final BSTNode node)
    {
        int size = 0;

        // walk the left forearm
        BSTNode walk = node.left;
        while (walk != null)
        {
            size++;
            walk = walk.right;
        }

        // walk the right forearm
        walk = node.right;
        while (walk != null)
        {
            size++;
            walk = walk.left;
        }

        return size;
    }

    static Statistic A3(BST S, BST T)
    {
        assertSanity(S, T);
        Set<Integer> maximalEquivalentSubtrees = findMaximalEquivalentSubtrees(S, T);

        // Calculate the subtree term (used later on) and the "g" term before performing rotations.
        final int subtreeTerm = maximalEquivalentSubtrees.stream()
                .map(S::search)
                .map(Optional::orElseThrow)
                .map(BSTNode::size)
                .mapToInt(n -> (int) Math.floor(Utilities.logBase(2, n)))
                .sum();

        // compute cs(rootT) for the equation below
        final int rootTRank = computeRootT(T.size());
        final int csRootT = sizeOfForearms(S.select(rootTRank).orElseThrow());

        // Apply A1 to each of the subtrees.
        int rotationsA1 = 0;
        int g = 0;
        for (int k : maximalEquivalentSubtrees)
        {
            BST subtreeS = new BST(S.search(k).orElseThrow());
            BST subtreeT = new BST(T.search(k).orElseThrow());
            if (!subtreeS.equals(subtreeT))
            {
                g++;
                rotationsA1 += A1(subtreeS, subtreeT).rotationsActual;
            }
            assert subtreeS.equals(subtreeT);
        }

        // Now that we've transformed all maximal equivalent subtrees into
        // maximal identical subtrees, we can take advantage of A2.
        Statistic statisticsA2 = A2(S, T);

        int n = S.size();
        return new Statistic(
                rotationsA1 + statisticsA2.rotationsActual,
                2 * n
                        - 2 * (int) Math.floor(Utilities.logBase(2, n))
                        - csRootT
                        - 2 * subtreeTerm
                        + g
                        + 1);
    }

    /**
     * Ensure that S and T are good to go for use in algorithm1, algorithm2, or algorithm3.
     */
    static void assertSanity(BST S, BST T)
    {
        if (S == T)
        {
            throw new IllegalArgumentException("S refers to the same object as T!");
        }
        else if (S == null || T == null)
        {
            throw new IllegalArgumentException("S and T must be non-null!");
        }
        else if (!S.keySet().equals(T.keySet()))
        {
            throw new IllegalArgumentException("S and T have different keysets!");
        }
    }

    /**
     * Compute the maximal identical subtrees between S and T (for A2) with a
     * bottom-up dynamic programming algorithm, implemented using a post-order
     * traversal.
     */
    static Set<Integer> findMaximalIdenticalSubtrees(BST S, BST T)
    {
        assertSanity(S, T);

        // Maximum Identical Subtree roots.
        Set<Integer> MISRoots = new HashSet<>();

        // Do a post order traversal to find all maximal subtrees in S and T from the bottom up.
        S.postOrder(node ->
        {
            // enumerate all children
            List<Integer> childKeys = Stream.of(node.left, node.right)
                    .filter(Objects::nonNull)
                    .map(n -> n.key)
                    .collect(Collectors.toList());

            // If all this node's children (which may be none) are maximal roots of *identical* subtrees,
            // and there is a node in T which equals this one, then this node is a maximal identical subtree.
            Optional<BSTNode> nodeT = T.search(node.key);
            if (MISRoots.containsAll(childKeys) && node.equals(nodeT.orElse(null)))
            {
                MISRoots.add(node.key);
                // This is a maximal subtree so its children are not anymore.
                MISRoots.removeAll(childKeys);
            }
        });

        return MISRoots;
    }

    /**
     * Return a set of keys which are the roots of maximal equivalent subtrees in S and T.
     */
    static Set<Integer> findMaximalEquivalentSubtrees(BST S, BST T)
    {
        // TODO
        return new HashSet<>();
    }

    /**
     * Dynamically compute the vertex intervals of all nodes in the tree.
     * We do this using an auxiliary data structure to avoid storing stuff
     * in the BSTNode.
     * <p>
     * NOTE: rotating the tree or inserting new elements will likely invalidate the vertex intervals.
     */
    static Map<Integer, VertexInterval> vertexIntervals(BST tree)
    {

        // Create a map of keys to ranks through an in-order traversal.
        Map<Integer, Integer> ranks = new HashMap<>();
        AtomicInteger currentRank = new AtomicInteger(0);
        tree.inOrder(n -> ranks.put(n.key, currentRank.getAndIncrement()));

        // Create a map
        Map<Integer, VertexInterval> intervals = new HashMap<>();

        // Then do a post-order traversal to calculate the vertex intervals of each node.
        // We do a post-order traversal so we visit the leaves first (this algorithm is a
        // a bottom-up dynamic programming algorithm).
        tree.postOrder(n ->
        {
            int min = ranks.get(n.key);
            int max = ranks.get(n.key);

            if (n.left != null)
            {
                // we have a left child
                min = intervals.get(n.left.key).min;
            }

            if (n.right != null)
            {
                // we have a right child
                max = intervals.get(n.right.key).max;
            }

            intervals.put(n.key, new VertexInterval(min, max));
        });

        return intervals;
    }

    /**
     * Given a tree and a sequence of rotations, apply the sequence of rotations to the tree in reverse,
     * swapping left rotations for right and vice versa.
     */
    static void applyInvertedRotations(BST t, List<HistoryEntry> history)
    {
        for (int i = history.size() - 1; i >= 0; i--)
        {
            HistoryEntry h = history.get(i);
            BSTNode n = t.search(h.key).orElseThrow();
            switch (h.rotation)
            {
                case Left -> t.rotateRight(n);
                case Right -> t.rotateLeft(n);
            }
        }
    }

    /**
     * Move the node with rank `rank` to the root of the binary tree S.
     * Note that the rank starts from 1.
     *
     * @return Number of rotations performed.
     */
    static int rotateNodeToRoot(BST S, int rank)
    {
        // (Step 3 from paper) First find the node in S that will become the new root
        BSTNode rootT = S.select(rank).orElseThrow();

        int numRotations = 0;

        // (Step 4 from paper) if rootT is not already in the rootT position, move it up w/ k-1 rotations
        while (rootT != S.root)
        {
            // if we're the left-child of our parent, then do a right-rotation to move ourselves up
            if (rootT.parent.left == rootT)
            {
                S.rotateRight(rootT.parent);
                numRotations++;
            }
            else if (rootT.parent.right == rootT)
            {
                // otherwise we're the right child of our parent, so do a left-rotation to move ourselves up
                S.rotateLeft(rootT.parent);
                numRotations++;
            }
            else
            {
                throw new IllegalStateException("should not happen");
            }
        }

        return numRotations;
    }

    /**
     * Convert the tree into just left and right forearms, recording the sequence of
     * rotations done to get there. We do not rotate the keys given in ignoredKeys.
     */
    static List<HistoryEntry> makeForearms(BST t, Set<Integer> ignoredKeys)
    {
        // Record the sequence of rotations performed.
        List<HistoryEntry> history = new ArrayList<>();

        // Fold the left subtree into the left forearm.
        BSTNode current = t.root.left;
        while (current != null)
        {
            final BSTNode child = current.left;
            if (child != null && !ignoredKeys.contains(child.key))
            {
                current = t.rotateRight(current);
                history.add(new HistoryEntry(Rotation.Right, current.key));
            }
            else
            {
                // move on to the next node in the right spine
                current = current.right;
            }
        }

        // Fold the right subtree into the right forearm.
        current = t.root.right;
        while (current != null)
        {
            final BSTNode child = current.right;
            if (child != null && !ignoredKeys.contains(child.key))
            {
                current = t.rotateLeft(current);
                history.add(new HistoryEntry(Rotation.Left, current.key));
            }
            else
            {
                current = current.left;
            }
        }

        return history;
    }

    /**
     * Special case of makeForearms which doesn't ignore any nodes for rotation.
     */
    static List<HistoryEntry> makeForearms(BST t)
    {
        return makeForearms(t, new HashSet<>());
    }

    /**
     * Find index/rank/whatever of a tree with n nodes.
     * Page 3 of paper.
     */
    private static int computeRootT(final int n)
    {
        // NOTE: h is the number of levels in the tree, NOT the length of the longest path
        final int h = levels(n);
        final int twoToHeight = (int) Math.pow(2, h);
        final int twoToHeightMinusOne = (int) Math.pow(2, h - 1);
        final int twoToHeightMinusTwo = (int) Math.pow(2, h - 2);
        if (twoToHeightMinusOne <= n && n <= twoToHeightMinusOne + twoToHeightMinusTwo - 2)
        {
            return n - twoToHeightMinusTwo;
        }
        else if (twoToHeightMinusOne + twoToHeightMinusTwo - 1 <= n && n <= twoToHeight - 1)
        {
            return twoToHeightMinusOne - 1;
        }
        else
        {
            throw new RuntimeException("not sure how this happened: n=" + n + ", h=" + h);
        }
    }

    /**
     * Create an almost complete binary search tree from a non-empty collection of keys.
     * All the magic happens inside almostCompleteHelper.
     */
    static BST makeAlmostCompleteBST(Collection<Integer> keys)
    {

        if (keys.size() == 0) throw new IllegalArgumentException("keys cannot be empty");

        BSTNode root = almostCompleteHelper(null,
                keys.stream().sorted().collect(Collectors.toList()));
        return new BST(root);
    }

    /**
     * Recursive helper function for makeAlmostCompleteBST
     * All the magic happens inside computeRootT.
     */
    static BSTNode almostCompleteHelper(BSTNode parent, List<Integer> keys)
    {
        if (keys.size() == 0)
        {
            return null;
        }
        else
        {
            int rootIndex = computeRootT(keys.size());
            int rootKey = keys.get(rootIndex);
            BSTNode root = new BSTNode(rootKey);
            root.parent = parent;

            // left subtree
            root.left = almostCompleteHelper(root, keys.subList(0, rootIndex));

            // right subtree
            root.right = almostCompleteHelper(root, keys.subList(rootIndex + 1, keys.size()));

            return root;
        }
    }

    /**
     * Returns the number of levels in an almost complete binary tree of n nodes.
     */
    static int levels(int n)
    {
        return (int) Math.floor(Utilities.logBase(2, n)) + 1;
    }


    /**
     * A left or right rotation.
     */
    enum Rotation
    {
        Left,
        Right
    }


    enum Algorithm
    {
        A1,
        A2,
        A3
    }

    /**
     * Returned by each algorithm.
     */
    record Statistic(int rotationsActual, int rotationsExpected) { }

    /**
     * A record of a node's rotation, which includes the node's key and the rotation type (left/right).
     */
    record HistoryEntry(Rotation rotation, int key) { }

    /**
     * A vertex interval.
     */
    record VertexInterval(int min, int max) { }
}
