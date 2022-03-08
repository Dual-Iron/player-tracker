package tracker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import tracker.Utils;

@Mixin(CompassItem.class)
public class CompassItemMixin {
	@Inject(method = "useOnBlock", at = @At("HEAD"))
	private void useOnBlock(ItemUsageContext ctx, CallbackInfoReturnable<ActionResult> ci) {
		// Can only apply to players when clicking a lodestone
		var plr = ctx.getPlayer();
		if (plr == null || !ctx.getWorld().getBlockState(ctx.getBlockPos()).isOf(Blocks.LODESTONE)) {
			return;
		}

		// Get compass and lapis
		ItemStack compass;
		ItemStack lapis;

		if (plr.getMainHandStack().isOf(Items.COMPASS) && plr.getOffHandStack().isOf(Items.LAPIS_LAZULI)) {
			compass = plr.getMainHandStack();
			lapis = plr.getOffHandStack();
		} else if (plr.getMainHandStack().isOf(Items.LAPIS_LAZULI) && plr.getOffHandStack().isOf(Items.COMPASS)) {
			lapis = plr.getMainHandStack();
			compass = plr.getOffHandStack();
		} else {
			return;
		}

		LivingEntity target = Utils.findTrackable(plr, ctx.getWorld());

		if (target == null) {
			var serverPlayer = plr.getServer().getPlayerManager().getPlayer(plr.getUuid());
			serverPlayer.sendMessage(Text.of("No enemy players or shells"), true);
			return;
		}
	}
}
