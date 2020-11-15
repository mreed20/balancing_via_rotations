import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.*;
import org.assertj.core.api.Assertions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinaryTreeTest
{

    @Property
    void insertingDuplicateKeysTriggerException(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        // insert the unique keys
        BST tree = new BST(keys);

        // Trying to insert any of the keys again triggers an exception
        Collections.shuffle(keys);
        for (int k : keys)
        {
            Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> tree.insert(k))
                    .withMessageContaining(String.valueOf(k), "duplicate");
        }
    }

    /**
     * no duplicate keys allowed, since that should trigger an exception
     */
    @Property
    void insertMaintainSortedOrder(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        // insert the keys
        BST tree = new BST(keys);

        List<Integer> inOrderWalk = tree.inOrderKeys();

        // keys in the tree should correspond exactly to the keys inserted (ignoring order)
        Assertions.assertThat(inOrderWalk).hasSameElementsAs(keys);

        // keys should occur in sorted order during an in-order walk of the tree.
        Assertions.assertThat(inOrderWalk).isSorted();
    }

    @Property
    void searchFindsPresentKeysAndDoesNotFindMissingKeys(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST tree = new BST(keys);
        // now that all the keys are in the tree, we should find all of them
        for (int k : keys)
        {
            BSTNode n = tree.search(k).orElseThrow();
            Assertions.assertThat(n.key).isEqualTo(k);
        }
    }

    @Property
    void leftRotatingRootPreservesInorderTraversalProperty(@ForAll @Size(min = 2) List<@Unique Integer> keys)
    {
        BST tree = new BST(keys);

        // left rotation requires that the right child is not null
        Assume.that(tree.root.right != null);

        final List<Integer> beforeRotation = tree.inOrderKeys();
        Assertions.assertThat(beforeRotation).isSorted();
        Assertions.assertThat(beforeRotation).hasSameElementsAs(keys);

        // left-rotate the root
        tree.rotateLeft(tree.root);
        Assertions.assertThat(tree.inOrderKeys()).isEqualTo(beforeRotation);
    }

    @Property
    void rightRotatingRootPreservesInorderTraversalProperty(@ForAll @Size(min = 2) List<@Unique Integer> keys)
    {
        BST tree = new BST(keys);

        // right rotation requires that the left child is not null
        Assume.that(tree.root.left != null);

        final List<Integer> beforeRotation = tree.inOrderKeys();
        Assertions.assertThat(beforeRotation).isSorted();
        Assertions.assertThat(beforeRotation).hasSameElementsAs(keys);

        // right-rotate the root
        tree.rotateRight(tree.root);
        Assertions.assertThat(tree.inOrderKeys()).isEqualTo(beforeRotation);
    }

    @Property
    void leftRotateInverseOfRightRotate(@ForAll @Size(min = 2) List<@Unique Integer> keys)
    {
        // make the tree twice and only modify one of them, so we can compare them
        BST treeBefore = new BST(keys);
        BST treeAfter = new BST(keys);

        // left rotation requires that the right child is not null
        Assume.that(treeBefore.root.right != null);

        // left-rotate the root
        treeAfter.rotateLeft(treeAfter.root);
        // right-rotate the root
        treeAfter.rotateRight(treeAfter.root);

        // they should be the same
        Assertions.assertThat(treeBefore).isEqualTo(treeAfter);
    }

    @Property
    void rotateNodeToRootMovesNodeToTheRoot(@ForAll @Size(min = 2) List<@Unique Integer> keys,
                                            @ForAll @Positive int x)
    {
        BST t = new BST(keys);
        int rank = x % keys.size();  // ranks start at 0
        BSTNode movedNode = t.select(rank).orElseThrow();  // after calling rotateNodeToRoot, this should be the new root
        BalanceViaRotation.rotateNodeToRoot(t, rank);

        Assertions.assertThat(t.root).isEqualTo(movedNode);

        // keys in the tree should correspond exactly to the keys inserted (ignoring order)
        Assertions.assertThat(t.inOrderKeys()).hasSameElementsAs(keys);
        // keys should occur in sorted order.
        Assertions.assertThat(t.inOrderKeys()).isSorted();
    }

    @Property
    void makeForearmsDoesNotChangeInOrderTraversals(@ForAll @Size(min = 2) List<@Unique Integer> keys)
    {
        BST tOld = new BST(keys);
        BST t = new BST(keys);

        BalanceViaRotation.makeForearms(t);

        // root should not be modified (only check the key to avoid recursive comparison
        // involving left subtree, which we don't care about)
        Assertions.assertThat(t.root.key).isEqualTo(tOld.root.key);

        Assertions.assertThat(t.inOrderKeys()).isEqualTo(tOld.inOrderKeys());
    }

    @Property
    void makeForearmsHasCorrectHeightAndSize(@ForAll @Size(min = 2) List<@Unique Integer> keys)
    {
        BST t = new BST(keys);
        Assume.that(t.root.left != null);
        Assume.that(t.root.right != null);

        final int oldLeftSize = t.root.left.size();
        final int oldRightSize = t.root.right.size();

        BalanceViaRotation.makeForearms(t);

        // The size should be the same.
        Assertions.assertThat(t.root.left.size()).isEqualTo(oldLeftSize);
        Assertions.assertThat(t.root.right.size()).isEqualTo(oldRightSize);

        // If the left subtree has n nodes, then it should end up with height n-1
        Assertions.assertThat(t.root.left.height()).isEqualTo(oldLeftSize - 1);
        Assertions.assertThat(t.root.right.height()).isEqualTo(oldRightSize - 1);
    }

    @Property
    void identicalTreesAreEqual(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST t1 = new BST(keys);
        BST t2 = new BST(keys);
        Assertions.assertThat(t1.equals(t2)).isTrue();
    }

    @Property
    void differentTreesAreNotEqual(@ForAll @Size(min = 2) List<@Unique Integer> keys, @ForAll @Positive int x)
    {
        BST t1 = new BST(keys);
        keys.remove(x % keys.size());
        BST t2 = new BST(keys);
        Assertions.assertThat(t1.equals(t2)).isFalse();
    }


    @Property
    void makeAlmostCompleteBinaryTreePreservesInOrderProperty(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);

        List<Integer> inOrderWalk = t.inOrderKeys();

        // keys in the tree should correspond exactly to the keys inserted (ignoring order)
        Assertions.assertThat(inOrderWalk).hasSameElementsAs(keys);

        // keys should occur in sorted order during an in-order walk of the tree.
        Assertions.assertThat(inOrderWalk).isSorted();
    }

    @Property
    void makeAlmostCompleteBinaryTreeHasCorrectHeight(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);
        Assertions.assertThat(t.height()).isEqualTo(
                (long) Math.floor(Utilities.logBase(2, keys.size())));
    }

    @Property
    void binaryTreeSizeEqualsTheNumberOfKeysInTheTree(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST t = new BST(keys);
        Assertions.assertThat(t.size()).isEqualTo(keys.size());
        if (t.root != null)
        {
            Assertions.assertThat(t.size()).isEqualTo(t.root.size());
        }
    }

    @Property
    void binaryTreeSelectReturnsCorrectNodeFromInOrderWalk(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST t = new BST(keys);

        // Access all the possible ranks in order, making sure that the rank() method
        // returns the key of rank i in the list of sorted keys
        List<Integer> ranks = IntStream.range(0, keys.size()).boxed().collect(Collectors.toList());

        // we should have one rank per key
        Assertions.assertThat(ranks.size()).isEqualTo(keys.size());

        // We'll need the keys in sorted order
        Collections.sort(keys);

        for (int r : ranks)
        {
            BSTNode n = t.select(r).orElseThrow();
            Assertions.assertThat(keys.indexOf(n.key)).isEqualTo(r);
        }
    }

    @Property
    void binaryTreeSelectReturnsEmptyOnNegativeRanks(@ForAll @NotEmpty List<@Unique Integer> keys, @ForAll @Negative int badRank)
    {
        BST t = new BST(keys);
        Assertions.assertThat(t.select(badRank)).isEmpty();
    }

    @Property
    void binaryTreeSelectReturnEmptyOnTooBigPositiveIndex(@ForAll @NotEmpty List<@Unique Integer> keys,
                                                              @ForAll @IntRange(min = 0) int offset)
    {
        BST t = new BST(keys);

        // t.size() is already a bad rank (it's value is one more than the max possible rank)
        final int badRank = t.size() + offset;
        Assertions.assertThat(t.select(badRank)).isEmpty();
    }

    @Property
    void contrivedBinaryTreeHeightTest(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        // make sure the keys are in sorted order, then insert the keys
        // from smallest to largest, which should get us right-going chain.
        Collections.sort(keys);
        BST t = new BST(keys);
        // since we have a right-going chain, the height of the tree should be one
        // less than the number of nodes
        Assertions.assertThat(t.height()).isEqualTo(keys.size() - 1);
    }

    @Property
    void algorithm1TransformsSToTInExpectedNumberOfRotations(@ForAll @Size(min = 3) List<@Unique Integer> keys)
    {
        BST s = new BST(keys);
        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);
        Assume.that(!t.equals(s));

        BalanceViaRotation.Statistic stat = BalanceViaRotation.A1(s, t);
        Assertions.assertThat(s).isEqualTo(t);
        int n = keys.size();
        Assertions.assertThat(stat.rotationsActual()).isLessThanOrEqualTo(
                2 * n - 2 * (int) Math.floor(Utilities.logBase(2, n)) + BalanceViaRotation.p(n));
    }

    @Property
    void algorithm1PreservesTreeStructure(@ForAll @Size(min = 3) List<@Unique Integer> keys)
    {
        BST s = new BST(keys);
        final List<Integer> inOrderBefore = s.inOrderKeys();
        final int oldSize = s.size();

        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);
        BalanceViaRotation.A1(s, t);

        // make sure that s' size and inOrder traversal is the same as before
        Assertions.assertThat(s).isEqualTo(t);
        Assertions.assertThat(inOrderBefore).isEqualTo(s.inOrderKeys());
        Assertions.assertThat(oldSize).isEqualTo(s.size());
    }

    @Property
    void algorithm2TransformsSToTInExpectedNumberOfRotations(@ForAll @Size(min = 3) List<@Unique Integer> keys)
    {
        BST s = new BST(keys);
        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);
        Assume.that(!t.equals(s));

        final BalanceViaRotation.Statistic stat = BalanceViaRotation.A2(s, t);
        Assertions.assertThat(s).isEqualTo(t);
        Assertions.assertThat(stat.rotationsActual()).isLessThanOrEqualTo(stat.rotationsExpected());
    }

    @Property
    void algorithm2PreservesTreeStructure(@ForAll @Size(min = 3) List<@Unique Integer> keys)
    {
        BST s = new BST(keys);
        final List<Integer> inOrderBefore = s.inOrderKeys();
        final int oldSize = s.size();

        BST t = BalanceViaRotation.makeAlmostCompleteBST(keys);
        BalanceViaRotation.A2(s, t);

        // make sure that s' size and inOrder traversal is the same as before
        Assertions.assertThat(s).isEqualTo(t);
        Assertions.assertThat(inOrderBefore).isEqualTo(s.inOrderKeys());
        Assertions.assertThat(oldSize).isEqualTo(s.size());
    }

    @Property
    void assignVertexIntervalsAssignsCorrectVertexIntervals(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST s = new BST(keys);
        var intervals = BalanceViaRotation.vertexIntervals(s);
        Assertions.assertThat(intervals.size()).isEqualTo(keys.size());

        // check each node to make sure it's correct
        for (var entry : intervals.entrySet())
        {
            int key = entry.getKey();
            BalanceViaRotation.VertexInterval i = entry.getValue();
            BSTNode n = s.search(key).orElseThrow();
            Assertions.assertThat(s.select(i.min()).orElseThrow()).isEqualTo(s.search(key).orElseThrow().minimum());
            Assertions.assertThat(s.select(i.max()).orElseThrow()).isEqualTo(s.search(key).orElseThrow().maximum());
            Assertions.assertThat(i.max() - i.min()).isEqualTo(n.size() - 1);
        }
    }

    @Property
    void identicalTreesHaveOnlyOneMaximalSubtree(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        BST b = new BST(keys);
        Assertions.assertThat(BalanceViaRotation.findMaximalIdenticalSubtrees(a, b))
                .isEqualTo(Set.of(a.root.key));
    }

    @Property
    void maximalIdenticalSubtreesAreASubsetOfMaximalEquivalentSubtrees(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        BST b = new BST(keys);
        Assertions.assertThat(BalanceViaRotation.findMaximalIdenticalSubtrees(a, b))
                .isSubsetOf(BalanceViaRotation.findMaximalEquivalentSubtrees(a, b));
    }

    @Property
    void identicalSubtreesAreMaximalAndCommon(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        Collections.shuffle(keys);
        BST b = new BST(keys);

        Set<Integer> maximalKeys = BalanceViaRotation.findMaximalIdenticalSubtrees(a, b);

        Assume.that(maximalKeys.size() >= 3);

        for (int k : maximalKeys)
        {
            // Subtrees should be common to both a and b.
            BSTNode nodeA = a.search(k).orElseThrow();
            BSTNode nodeB = b.search(k).orElseThrow();
            Assertions.assertThat(nodeA).isEqualTo(nodeB);

            if (nodeA.parent == null || nodeB.parent == null)
            {
                Assertions.assertThat(nodeA.parent).isNull();
                Assertions.assertThat(nodeB.parent).isNull();
            }
            else
            {
                // Subtrees should be maximal, which means that the subtree rooted at the
                // parent (and any higher, although we don't check that far) are not equal.
                Assertions.assertThat(nodeA.parent).isNotEqualTo(nodeB.parent);
            }
        }
    }

    @Property
    void equivalentSubtreesAreMaximalAndCommon(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        Collections.shuffle(keys);
        BST b = new BST(keys);

        Set<Integer> maximalKeys = BalanceViaRotation.findMaximalEquivalentSubtrees(a, b);

        Assume.that(maximalKeys.size() >= 3);

        for (int k : maximalKeys)
        {
            // Subtrees should be common to both a and b.
            BSTNode nodeA = a.search(k).orElseThrow();
            BSTNode nodeB = b.search(k).orElseThrow();
            // TODO: for some reason this still works when equal() is used, which should not always be true
            // (in other words, all my equivalent roots seem to be identical...)
            Assertions.assertThat(nodeA.keySet()).isEqualTo(nodeB.keySet());

            if (nodeA.parent == null || nodeB.parent == null)
            {
                Assertions.assertThat(nodeA.parent).isNull();
                Assertions.assertThat(nodeB.parent).isNull();
            }
            else
            {
                // Subtrees should be maximal, which means that the subtree rooted at the
                // parent (and any higher, although we don't check that far) are not equal.
                Assertions.assertThat(nodeA.parent).isNotEqualTo(nodeB.parent);
            }
        }
    }

    @Property
    void identicalSubtreesAreDisjointInKeys(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        Collections.shuffle(keys);
        BST b = new BST(keys);

        Set<Integer> maximalKeys = BalanceViaRotation.findMaximalIdenticalSubtrees(a, b);

        Assume.that(maximalKeys.size() >= 3);

        for (int k : maximalKeys)
        {
            // maximal common subtrees should not be the children of any other maximal common subtrees
            BSTNode nodeA = a.search(k).orElseThrow();
            BSTNode nodeB = b.search(k).orElseThrow();

            // No other keys should be in the subtree rooted at this key, in either tree.
            for (int k2 : maximalKeys.stream().filter(x -> x != k).collect(Collectors.toList()))
            {
                Assertions.assertThat(nodeA.search(k2)).isEmpty();
                Assertions.assertThat(nodeB.search(k2)).isEmpty();
            }
        }
    }

    @Property
    void equivalentSubtreesAreDisjointInKeys(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST a = new BST(keys);
        Collections.shuffle(keys);
        BST b = new BST(keys);

        Set<Integer> maximalKeys = BalanceViaRotation.findMaximalEquivalentSubtrees(a, b);

        Assume.that(maximalKeys.size() >= 3);

        for (int k : maximalKeys)
        {
            // maximal common subtrees should not be the children of any other maximal common subtrees
            BSTNode nodeA = a.search(k).orElseThrow();
            BSTNode nodeB = b.search(k).orElseThrow();

            // No other keys should be in the subtree rooted at this key, in either tree.
            for (int k2 : maximalKeys.stream().filter(x -> x != k).collect(Collectors.toList()))
            {
                Assertions.assertThat(nodeA.search(k2)).isEmpty();
                Assertions.assertThat(nodeB.search(k2)).isEmpty();
            }
        }
    }

    @Property
    void ranksAndVertexIntervalsAreAssignedForAllVertices(@ForAll @NotEmpty List<@Unique Integer> keys)
    {
        BST s = new BST(keys);

        var intervals = BalanceViaRotation.vertexIntervals(s);
        Assertions.assertThat(intervals.size()).isEqualTo(keys.size());

        // Every node should have a vertex interval assigned.
        for (int key : keys)
        {
            Assertions.assertThat(intervals).containsKey(key);
        }
    }

    @Property
    void bstNodeMaximumReturnsNodeWithBiggestKey(@ForAll @NotEmpty Set<Integer> keys)
    {
        BST t = new BST(keys);
        Assertions.assertThat(t.root.maximum().key)
                .isEqualTo(keys.stream().max(Comparator.naturalOrder()).orElseThrow());
    }

    @Property
    void bstNodeMinimumReturnsNodeWithSmallestKey(@ForAll @NotEmpty Set<Integer> keys)
    {
        BST t = new BST(keys);
        Assertions.assertThat(t.root.minimum().key)
                .isEqualTo(keys.stream().min(Comparator.naturalOrder()).orElseThrow());
    }
}
