/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import com.google.common.base.MoreObjects;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class QueuedColumn {

    final int x;
    final int z;
    @Nonnull final World world;

    QueuedColumn(int x, int z, World world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }

    @Override
    public int hashCode() {
        return (x * 31 + z * 29) ^ world.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object instanceof QueuedColumn) {
            QueuedColumn other = (QueuedColumn) object;
            return x == other.x && z == other.z && world == other.world;
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(this.world)
                .add("x", this.x)
                .add("z", this.z)
                .toString();
    }
}
