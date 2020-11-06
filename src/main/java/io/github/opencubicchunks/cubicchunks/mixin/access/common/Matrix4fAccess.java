package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface Matrix4fAccess {

    @Accessor("m00") void setM00(float val);
    @Accessor("m01") void setM01(float val);
    @Accessor("m02") void setM02(float val);
    @Accessor("m03") void setM03(float val);
    @Accessor("m10") void setM10(float val);
    @Accessor("m11") void setM11(float val);
    @Accessor("m12") void setM12(float val);
    @Accessor("m13") void setM13(float val);
    @Accessor("m20") void setM20(float val);
    @Accessor("m21") void setM21(float val);
    @Accessor("m22") void setM22(float val);
    @Accessor("m23") void setM23(float val);
    @Accessor("m30") void setM30(float val);
    @Accessor("m31") void setM31(float val);
    @Accessor("m32") void setM32(float val);
    @Accessor("m33") void setM33(float val);
}
