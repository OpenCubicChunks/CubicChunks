package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import io.github.opencubicchunks.cubicchunks.chunk.util.NodeTree;

import java.util.Random;

class Scratch {
	static Random random = new Random(1);


	public static void main(String[] args) {
		NodeTree<NodeTree<CCHeightmap>> root = NodeTree.createTree(2, 3);

		root.setCcHeightmap(new CCHeightmap(2));

		NodeTree<CCHeightmap> parent = root.getNode(0);
		parent.setCcHeightmap(new CCHeightmap(1));

		NodeTree<CCHeightmap> parent2 = root.getNode(1);
		parent2.setCcHeightmap(new CCHeightmap(1));

		parent.setNode(0, initializeRandomCCHeightMap(0));
		parent.setNode(1, initializeRandomCCHeightMap(0));

		parent2.setNode(0, initializeRandomCCHeightMap(0));
		parent2.setNode(1, initializeRandomCCHeightMap(0));

		System.out.println("ROOT SCALE: " + root.getCcHeightmap().getScale());

		System.out.println("Root Child 0 Scale: " + root.getNode(0).getCcHeightmap().getScale());
		System.out.println("Root Child 0 Child 0 Scale: " + parent.getNode(0).getScale());
		System.out.println("Root Child 0 Child 1 Scale: " + parent.getNode(1).getScale());
		System.out.println("\n");

		System.out.println("Root Child 1 Scale: " + root.getNode(1).getCcHeightmap().getScale());
		System.out.println("Root Child 1 Child 0 Scale: " + parent2.getNode(0).getScale());
		System.out.println("Root Child 1 Child 1 Scale: " + parent2.getNode(1).getScale());
	}


	public static CCHeightmap initializeRandomCCHeightMap(int scale) {
		CCHeightmap ccHeightmap = new CCHeightmap(scale);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				ccHeightmap.setHeight(x, z, random.nextInt(16));
			}
		}

		return ccHeightmap;
	}
}
