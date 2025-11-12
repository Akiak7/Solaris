package com.akira.noisetint.client;

import com.akira.noisetint.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FogHandler {

    private static float lastR = 1f, lastG = 1f, lastB = 1f;
    private static boolean hasLast = false;

    public static void reset() {
        hasLast = false;
        lastR = lastG = lastB = 1f;
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors e) {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient w = mc.world;
        if (w == null) return;

        EntityPlayerSP p = mc.player;
        if (p == null) return;

        int wx = (int)Math.floor(p.posX);
        int wz = (int)Math.floor(p.posZ);

        // Base fog color
        float r = e.getRed();
        float g = e.getGreen();
        float b = e.getBlue();

        // Sample tint at camera pos
        float[] tint = NoiseField.sampleTint(wx, wz);

        // Dimensional adjustments (gentler in Overworld storms/night)
        float amp = 1.0f;
        int dim = w.provider.getDimension();
        if (dim == 0) {
            // Overworld
            amp = 0.6f;
            if (w.isRaining() || w.isThundering()) amp *= 0.7f;
            // night fade
            float celestial = w.getCelestialAngle(1.0f);
            // crude night factor
            float night = (float)(0.5f - 0.5f * Math.cos(celestial * Math.PI * 2));
            amp = 0.4f + amp * (1.0f - 0.3f * night);
        } else if (dim == -1) {
            // Nether - go bolder if desired
            amp = 1.0f;
        } else if (dim == 1) {
            // End - very gentle
            amp = 0.3f;
        }

        // Apply to fog in HSV space
        int rgb = ColorUtil.rgbFloatToInt(r, g, b);
        float[] hsv = ColorUtil.rgbToHsv(rgb);
        hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0] * amp);
        hsv[1] = ColorUtil.clamp01(hsv[1] * (1.0f + (tint[1] - 1.0f) * amp));
        hsv[2] = ColorUtil.clamp01(hsv[2] * (1.0f + (tint[2] - 1.0f) * amp));

        int out = ColorUtil.hsvToRgbInt(hsv);
        float tr = ((out >> 16) & 0xFF) / 255.0f;
        float tg = ((out >> 8) & 0xFF) / 255.0f;
        float tb = (out & 0xFF) / 255.0f;

        // Temporal smoothing to avoid pops when crossing noise ridges
        if (!hasLast) {
            lastR = tr; lastG = tg; lastB = tb;
            hasLast = true;
        }
        float alpha = 0.01f; // stronger smoothing
        lastR = lastR * (1 - alpha) + tr * alpha;
        lastG = lastG * (1 - alpha) + tg * alpha;
        lastB = lastB * (1 - alpha) + tb * alpha;

        e.setRed(lastR);
        e.setGreen(lastG);
        e.setBlue(lastB);
    }
}