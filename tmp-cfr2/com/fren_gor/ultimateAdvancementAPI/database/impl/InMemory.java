/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.database.impl;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.database.impl.SQLite;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class InMemory
extends SQLite {
    public InMemory(@NotNull AdvancementMain main) throws Exception {
        super(Objects.requireNonNull(main, "AdvancementMain is null.").getLogger());
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public InMemory(@NotNull Logger logger) throws Exception {
        super(logger);
    }
}

