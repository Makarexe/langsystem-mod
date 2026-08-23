package com.langsystem.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Табличка с текстом на выбранном языке — отдельный декоративный блок (не заменяет
 * ванильные таблички), формой напоминающий обычную ванильную табличку (столбик + доска),
 * поворачивается лицевой стороной к игроку при установке. У лицевой и обратной сторон —
 * независимый текст, как у настоящей вывески. Прямо в мире всегда видно "как написано"
 * (без учёта прогресса читателя, см. {@link com.langsystem.client.LanguageSignRenderer}).
 * Правый клик по ПУСТОЙ стороне открывает запись; по уже подписанной — открывает чтение,
 * персонально посчитанное под текущего игрока (его прогресс в языке).
 */
public final class LanguageSignBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape POST = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape PLANK_NORTH_SOUTH = Block.box(0, 8, 7, 16, 15, 9);
    private static final VoxelShape PLANK_EAST_WEST = Block.box(7, 8, 0, 9, 15, 16);
    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(POST, PLANK_NORTH_SOUTH);
    private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(POST, PLANK_EAST_WEST);
    private static final MapCodec<LanguageSignBlock> CODEC = simpleCodec(LanguageSignBlock::new);

    public LanguageSignBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<LanguageSignBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof LanguageSignBlockEntity sign) {
                boolean isFrontText = hit.getDirection() != state.getValue(FACING).getOpposite();
                com.langsystem.client.ClientSignScreens.open(pos, sign, isFrontText);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanguageSignBlockEntity(pos, state);
    }
}
