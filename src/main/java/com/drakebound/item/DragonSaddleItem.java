package com.drakebound.item;

import com.drakebound.entity.SaddleType;
import net.minecraft.world.item.Item;

/** Sela de ferro para dragões. A colocação é tratada pela DragonEntity ao interagir. */
public class DragonSaddleItem extends Item {
    public final SaddleType type;

    public DragonSaddleItem(SaddleType type, Item.Properties properties) {
        super(properties);
        this.type = type;
    }
}
