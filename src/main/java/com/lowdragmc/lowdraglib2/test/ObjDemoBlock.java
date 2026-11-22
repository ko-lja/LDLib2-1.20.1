package com.lowdragmc.lowdraglib2.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ObjDemoBlock extends Block {

    public static final ObjDemoBlock BLOCK = new ObjDemoBlock();
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    private ObjDemoBlock() {
        super(Properties.of().noOcclusion().destroyTime(1));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
