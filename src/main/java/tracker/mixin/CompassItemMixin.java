package tracker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import tracker.Utils;

@Mixin(CompassItem.class)
public class CompassItemMixin {
	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void useOnBlock(ItemUsageContext ctx, CallbackInfoReturnable<ActionResult> ci) {
		// Can only apply to players when clicking a lodestone
		var plr = ctx.getPlayer();
		var wld = ctx.getWorld();
		if (plr == null || !wld.getBlockState(ctx.getBlockPos()).isOf(Blocks.LODESTONE)) {
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

		wld.playSound(null, ctx.getBlockPos(), SoundEvents.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1f, 1f);

		if (wld instanceof ServerWorld swld) {
			LivingEntity target = Utils.findTrackable(plr, swld);

			if (target == null) {
				plr.sendMessage(Text.of("No enemy vessels"), true);
				return;
			}

			BlockPos plrPos = plr.getBlockPos(), targetPos = target.getBlockPos();

			// Give target glowing and play sounds
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200));
			wld.playSound(null, targetPos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1f, 1f);
			wld.playSound(null, plrPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.25f);

			Utils.giveTracker(plr, target, compass, lapis);
		}

		ci.setReturnValue(ActionResult.success(wld.isClient));
	}
}
