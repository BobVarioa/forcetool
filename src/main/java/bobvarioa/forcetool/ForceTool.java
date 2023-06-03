package bobvarioa.forcetool;

import com.mojang.logging.LogUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;

@Mod(ForceTool.MODID)
public class ForceTool {
    public static final String MODID = "forcetool";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ForceTool() {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerBreakSpeed);
    }

    public void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.isCreative()) return;
        final var blockPos = event.getPosition().get();
        final var toolTypes = new HashSet<String>();

        BlockState blockState = player.level.getBlockState(blockPos);
        final var blockTags = blockState.getTags();
        for (Iterator<TagKey<Block>> it = blockTags.iterator(); it.hasNext(); ) {
            final var blockTagKey = it.next();
            final String path = blockTagKey.location().getPath();
            if (path.startsWith("mineable/")) {
                toolTypes.add(path.substring(9));
            }
        }

        // if the block is mineable by hand, don't cancel
        if (!toolTypes.add("hand")) {
            return;
        }

        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        final var itemTags = itemInHand.getTags();
        boolean canMine = itemTags.anyMatch(itemTagKey -> {
            final String path = itemTagKey.location().getPath();
            if (path.startsWith("tools/")) {
                String sub = path.substring(6);
                if (sub.endsWith("s")) {
                    sub = sub.substring(0, sub.length() - 1);
                }
                return !toolTypes.add(sub);
            }
            return false;
        });

        canMine = canMine || itemInHand.isCorrectToolForDrops(blockState);

        if (!canMine) {
            event.setCanceled(true);
        }
    }
}
