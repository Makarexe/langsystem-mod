package com.langsystem.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Проверка "видит ли один игрок жесты другого" — нужна для языка жестов. */
public final class Visibility {

    private Visibility() {
    }

    public static boolean canSee(ServerPlayer viewer, ServerPlayer target, double maxRange) {
        if (viewer.level() != target.level()) {
            return false;
        }
        if (viewer.distanceToSqr(target) > maxRange * maxRange) {
            return false;
        }
        Vec3 from = viewer.getEyePosition();
        Vec3 to = target.getEyePosition();
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer);
        BlockHitResult hit = viewer.level().clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }
}
