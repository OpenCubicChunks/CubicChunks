package io.github.opencubicchunks.cubicchunks.chunk.util;

import io.github.opencubicchunks.cubicchunks.chunk.heightmap.CCHeightmap;

import java.util.ArrayList;
import java.util.List;

public class NodeTree<T> {
	private final List<T> nodes; //Internal node array
	private final int nodeChildCount; //number of children this node has

	private CCHeightmap ccHeightmap;

	private boolean nodesInitialised = false;

	/**
	 * @param nodeChildCount the number of children for this node
	 */
	private NodeTree(int nodeChildCount) {
		this.nodeChildCount = nodeChildCount;
		nodes = new ArrayList<>(nodeChildCount);
	}

	public void setCcHeightmap(CCHeightmap ccHeightmap) {
		this.ccHeightmap = ccHeightmap;
	}

	public CCHeightmap getCcHeightmap() {
		return ccHeightmap;
	}

	/**
	 * @param nodeChildCount The number of children per node
	 * @param depth          how deep to initialise the tree
	 * @return the full node tree created.
	 * <p>
	 * A tree of depth 1 would have a return type of NodeTree<T>
	 * A tree of depth 2 would have a return type of NodeTree<NodeTree<T>>
	 * etc
	 */
	public static <T> NodeTree<T> createTree(int nodeChildCount, int depth) {
		if (depth < 1) {
			NodeTree<T> nodeTree = new NodeTree<>(nodeChildCount);
			nodeTree.initialiseNodesIfNot();
			return nodeTree;
		}
		NodeTree<T> tree = new NodeTree<>(nodeChildCount);
		unsafeAddTreeToNode(tree, depth);
		return tree;
	}

	public void initialiseNodesIfNot() {
		if (!nodesInitialised) {
			for (int i = 0; i < nodeChildCount; i++) {
				nodes.add(null);
			}
		}
	}

	public void nullNodes() {
		initialiseNodesIfNot();
		for (int i = 0; i < nodeChildCount; i++) {
			nodes.set(i, null);
		}
	}

	public static <T> void unsafeAddTreeToNode(NodeTree tree, int depth) {
		depth--;
		for (int i = tree.nodes.size(); i < tree.nodeChildCount; i++) {
			NodeTree node = new NodeTree<>(tree.nodeChildCount);
			if (depth > 1) {
				unsafeAddTreeToNode(node, depth);
				node.nodesInitialised = true;
			} else {
				node.initialiseNodesIfNot();
			}
			tree.nodes.add(i, node);
		}
	}

	public T getNode(int i) {
		initialiseNodesIfNot();
		return nodes.get(i);
	}

	public void setNode(int i, T val) {
		initialiseNodesIfNot();
		nodes.set(i, val);
	}

	public T getNodeUnchecked(int i) {
		return nodes.get(i);
	}

	public void setNodeUnchecked(int i, T val) {
		nodes.set(i, val);
	}
}
