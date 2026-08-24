package com.mistbeyond.transport.item.rail;

import com.mistbeyond.registry.RegisterItem;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import com.mistbeyond.transport.api.rail.section.SignalType;

@RegisterItem
public class RailPathSignalItem extends RailSignalItem {
    public RailPathSignalItem(Properties properties) {
        super(properties, SignalType.PATH);
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(ItemRegistration registration) {
        registration.register(
                "rail_path_signal",
                RailPathSignalItem::new,
                props -> props.stacksTo(64)
        );
    }
}
