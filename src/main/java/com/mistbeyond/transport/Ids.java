package com.mistbeyond.transport;

import net.minecraft.resources.Identifier;

public class Ids {
    public static final String MOD_ID = "mtm";

    private Ids() {
    }

    public static Identifier thisMod(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }
}
