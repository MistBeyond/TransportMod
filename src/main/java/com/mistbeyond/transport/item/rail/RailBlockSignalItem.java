package com.mistbeyond.transport.item.rail;

import com.mistbeyond.registry.RegisterItem;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import com.mistbeyond.transport.api.rail.section.SignalType;

@RegisterItem
public class RailBlockSignalItem extends RailSignalItem {
    public RailBlockSignalItem(Properties properties) {
        super(properties, SignalType.BLOCK);
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(ItemRegistration registration) {
        registration.register(
                "rail_block_signal",
                RailBlockSignalItem::new,
                props -> props.stacksTo(64)
        );
    }
}
