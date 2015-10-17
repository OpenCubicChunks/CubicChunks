/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.proxy;

import cubicchunks.util.Coords;
import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.column.Column;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ClientDebugRender {
	int cubeDisplayList = -1;
	@SubscribeEvent
	public void onRender(RenderWorldLastEvent evt) {
		if(cubeDisplayList == -1) {
			cubeDisplayList = GL11.glGenLists(1);

			int[][] q = new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 0}};
			GL11.glNewList(cubeDisplayList, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_QUADS);
			{
				for(int[] a : q)
					GL11.glVertex3f(0, a[0], a[1]);
				for(int[] a : q)
					GL11.glVertex3f(1, a[0], a[1]);
				for(int[] a : q)
					GL11.glVertex3f(a[0], 0, a[1]);
				for(int[] a : q)
					GL11.glVertex3f(a[0], 1, a[1]);
				for(int[] a : q)
					GL11.glVertex3f(a[0], a[1], 0);
				for(int[] a : q)
					GL11.glVertex3f(a[0], a[1], 1);
			}
			GL11.glEnd();
			GL11.glEndList();
		}
		try{
			//I know it's really bad idea... But I really need to access it
			WorldServer serverWorld = (WorldServer) Minecraft.getMinecraft().getIntegratedServer().getEntityWorld();
			EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
			BlockPos pos = player.getPosition();

			WorldClient clientWorld = (WorldClient) player.getEntityWorld();

			Column serverColumn = (Column) serverWorld.getChunkFromBlockCoords(pos);
			Column clientColumn = (Column) clientWorld.getChunkFromBlockCoords(pos);

			OpacityIndex serverIndex = (OpacityIndex) serverColumn.getOpacityIndex();
			ClientOpacityIndex clientIndex = (ClientOpacityIndex) clientColumn.getOpacityIndex();

			GL11.glPushMatrix();
			{
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glAlphaFunc ( GL11.GL_GREATER, 0.1f ) ;
				GL11.glEnable(GL11.GL_BLEND);
				float fracX = (float) (player.posX - pos.getX());
				float fracY = (float) (player.posY - pos.getY());
				float fracZ = (float) (player.posZ - pos.getZ());

				GL11.glTranslatef(-fracX, -fracY, -fracZ);

				for(int y = -10; y <= 10; y++) {
					GL11.glPushMatrix();
					GL11.glTranslatef(0, y, 0);
					int serverOpacity = serverIndex.getOpacity(Coords.blockToLocal(pos.getX()), pos.getY() + y, Coords.blockToLocal(pos.getZ()));
					int clientOpacity = clientIndex.getOpacity(Coords.blockToLocal(pos.getX()), pos.getY() + y, Coords.blockToLocal(pos.getZ()));
					GL11.glColor4f(serverOpacity/255.0f, 1 - serverOpacity/255.0f, 0, Math.abs(clientOpacity - serverOpacity) + 0.2f);
					GL11.glCallList(cubeDisplayList);
					GL11.glPopMatrix();
				}
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
			GL11.glPopMatrix();
		} catch(Throwable t) {
			if(t instanceof  VirtualMachineError) {
				throw (VirtualMachineError)t;
			}
			//we are doing stuff in thread unsafe way. Weird things may happen. catch everything.
			t.printStackTrace();
		}
	}
}
