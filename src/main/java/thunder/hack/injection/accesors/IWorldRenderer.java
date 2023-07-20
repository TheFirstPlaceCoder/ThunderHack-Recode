package thunder.hack.injection.accesors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.render.BlockBreakingInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;

@Mixin(WorldRenderer.class)
public interface IWorldRenderer {

    @Accessor
    public abstract Frustum getFrustum();

    @Accessor
    public abstract void setFrustum(Frustum frustum);

    @Accessor("blockBreakingInfos")
    Int2ObjectMap<BlockBreakingInfo> getBlockBreakingInfos();
}