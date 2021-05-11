package carpettisaddition.mixins.command.lifetime.removal;

import carpettisaddition.commands.lifetime.interfaces.EntityDamageable;
import carpettisaddition.commands.lifetime.interfaces.IEntity;
import carpettisaddition.commands.lifetime.removal.DeathRemovalReason;
import carpettisaddition.commands.lifetime.removal.LiteralRemovalReason;
import carpettisaddition.commands.lifetime.removal.TransDimensionRemovalReason;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	@Shadow public abstract boolean hasVehicle();

	@Inject(method = "remove", at = @At("TAIL"))
	private void onEntityRemovedLifeTimeTracker(CallbackInfo ci)
	{
		if (this instanceof EntityDamageable)
		{
			DamageSource damageSource = ((EntityDamageable)this).getDeathDamageSource();
			if (damageSource != null)
			{
				((IEntity)this).recordRemoval(new DeathRemovalReason(damageSource));
				return;
			}
		}
		((IEntity)this).recordRemoval(LiteralRemovalReason.OTHER);
	}

	@Inject(
			method = "moveToWorld",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/server/world/ServerWorld;onDimensionChanged(Lnet/minecraft/entity/Entity;)V"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;removeFromDimension()V"
			),
			allow = 1
	)
	private void onEntityTransDimensionRemovedLifeTimeTracker(ServerWorld destination, CallbackInfoReturnable<@Nullable Entity> cir)
	{
		((IEntity)this).recordRemoval(new TransDimensionRemovalReason(destination.getRegistryKey()));
	}

	@Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("RETURN"))
	void onEntityStartRidingLifeTimeTracker(CallbackInfoReturnable<Boolean> cir)
	{
		if (this.hasVehicle())
		{
			((IEntity)this).recordRemoval(LiteralRemovalReason.ON_VEHICLE);
		}
	}

	@Inject(method = "tickInVoid", at = @At("HEAD"))
	private void onEntityDestroyedInVoid(CallbackInfo ci)
	{
		((IEntity)this).recordRemoval(LiteralRemovalReason.VOID);
	}
}
