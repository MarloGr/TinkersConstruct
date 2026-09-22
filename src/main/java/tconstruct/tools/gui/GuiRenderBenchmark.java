package tconstruct.tools.gui;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class GuiRenderBenchmark {

    static boolean legacy, counting;
    static int calls, quads;
    private static final int WARMUP = 20, SAMPLES = 60, FRAMES = WARMUP + SAMPLES + 2;
    private final long[] cpu = new long[2], elapsed = new long[2];
    private final int[] drawCalls = new int[2], quadCounts = new int[2];
    private int frame;
    private long start;

    void start() {
        if (frame >= FRAMES) return;
        legacy = (frame & 1) == 0;
        counting = frame >= WARMUP + SAMPLES;
        calls = quads = 0;
        if (!counting) {
            GL11.glFinish();
            start = System.nanoTime();
        }
    }

    void end(EntityPlayer player) {
        if (frame >= FRAMES) return;
        int version = frame & 1;
        try {
            if (counting) {
                drawCalls[version] = calls;
                quadCounts[version] = quads;
            } else {
                long submitted = System.nanoTime() - start;
                GL11.glFinish();
                if (frame >= WARMUP) {
                    cpu[version] += submitted;
                    elapsed[version] += System.nanoTime() - start;
                }
            }
            if (frame == 0) {
                player.addChatMessage(
                        new ChatComponentText(
                                "[TiC background] Comparing old/new rendering; keep this GUI open for " + FRAMES
                                        + " frames."));
            }
            if (++frame == FRAMES) {
                for (int i = 0; i < 2; i++) {
                    player.addChatMessage(
                            new ChatComponentText(
                                    String.format(
                                            Locale.ROOT,
                                            "[TiC background] %s: CPU submit %.3f ms, completed %.3f ms; load %d draws / %d quads per frame (%d samples).",
                                            i == 0 ? "Old" : "New",
                                            cpu[i] / (SAMPLES / 2 * 1_000_000.0),
                                            elapsed[i] / (SAMPLES / 2 * 1_000_000.0),
                                            drawCalls[i],
                                            quadCounts[i],
                                            SAMPLES / 2)));
                }
                player.addChatMessage(
                        new ChatComponentText(
                                String.format(
                                        Locale.ROOT,
                                        "[TiC background] Old/new time ratio: %.2fx. Using new rendering.",
                                        elapsed[0] / (double) elapsed[1])));
            }
        } finally {
            legacy = counting = false;
        }
    }
}
