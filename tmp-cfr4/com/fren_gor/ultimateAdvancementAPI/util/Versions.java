/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package com.fren_gor.ultimateAdvancementAPI.util;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class Versions {
    private static final String API_VERSION = "2.6.0";
    private static final List<String> SUPPORTED_NMS_VERSIONS = List.of("v1_15_R1", "v1_16_R1", "v1_16_R2", "v1_16_R3", "v1_17_R1", "v1_18_R1", "v1_18_R2", "v1_19_R1", "v1_19_R2", "v1_19_R3", "v1_20_R1", "v1_20_R2", "v1_20_R3", "v1_20_R4", "v1_21_R1", "v1_21_R2", "v1_21_R3", "v1_21_R4", "v1_21_R5");
    private static final Map<String, List<String>> NMS_TO_VERSIONS = Map.ofEntries(Map.entry("v1_15_R1", List.of("1.15", "1.15.1", "1.15.2")), Map.entry("v1_16_R1", List.of("1.16", "1.16.1", "1.16.2")), Map.entry("v1_16_R2", List.of("1.16.3", "1.16.4")), Map.entry("v1_16_R3", List.of("1.16.5")), Map.entry("v1_17_R1", List.of("1.17", "1.17.1")), Map.entry("v1_18_R1", List.of("1.18", "1.18.1")), Map.entry("v1_18_R2", List.of("1.18.2")), Map.entry("v1_19_R1", List.of("1.19", "1.19.2")), Map.entry("v1_19_R2", List.of("1.19.3")), Map.entry("v1_19_R3", List.of("1.19.4")), Map.entry("v1_20_R1", List.of("1.20", "1.20.1")), Map.entry("v1_20_R2", List.of("1.20.2")), Map.entry("v1_20_R3", List.of("1.20.3", "1.20.4")), Map.entry("v1_20_R4", List.of("1.20.5", "1.20.6")), Map.entry("v1_21_R1", List.of("1.21", "1.21.1")), Map.entry("v1_21_R2", List.of("1.21.2", "1.21.3")), Map.entry("v1_21_R3", List.of("1.21.4")), Map.entry("v1_21_R4", List.of("1.21.5")), Map.entry("v1_21_R5", List.of("1.21.6", "1.21.7", "1.21.8")));
    private static final Map<String, String> NMS_TO_FANCY = Map.ofEntries(Map.entry("v1_15_R1", "1.15-1.15.2"), Map.entry("v1_16_R1", "1.16-1.16.2"), Map.entry("v1_16_R2", "1.16.3-1.16.4"), Map.entry("v1_16_R3", "1.16.5"), Map.entry("v1_17_R1", "1.17-1.17.1"), Map.entry("v1_18_R1", "1.18-1.18.1"), Map.entry("v1_18_R2", "1.18.2"), Map.entry("v1_19_R1", "1.19-1.19.2"), Map.entry("v1_19_R2", "1.19.3"), Map.entry("v1_19_R3", "1.19.4"), Map.entry("v1_20_R1", "1.20-1.20.1"), Map.entry("v1_20_R2", "1.20.2"), Map.entry("v1_20_R3", "1.20.3-1.20.4"), Map.entry("v1_20_R4", "1.20.5-1.20.6"), Map.entry("v1_21_R1", "1.21-1.21.1"), Map.entry("v1_21_R2", "1.21.2-1.21.3"), Map.entry("v1_21_R3", "1.21.4"), Map.entry("v1_21_R4", "1.21.5"), Map.entry("v1_21_R5", "1.21.6-1.21.8"));
    private static final List<String> SUPPORTED_VERSIONS = SUPPORTED_NMS_VERSIONS.stream().flatMap(s -> NMS_TO_VERSIONS.get(s).stream()).toList();
    @Nullable
    private static Optional<String> COMPLETE_VERSION;

    @NotNull
    public static Optional<String> getNMSVersion() {
        if (COMPLETE_VERSION != null) {
            return COMPLETE_VERSION;
        }
        String version = NMS_TO_VERSIONS.entrySet().stream().filter(e -> ((List)e.getValue()).contains(ReflectionUtil.MINECRAFT_VERSION)).map(Map.Entry::getKey).findFirst().orElse(null);
        COMPLETE_VERSION = version != null ? Optional.of(version) : Optional.empty();
        return COMPLETE_VERSION;
    }

    public static String getApiVersion() {
        return API_VERSION;
    }

    @NotNull
    public static @UnmodifiableView @NotNull List<@NotNull String> getSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    @NotNull
    public static @UnmodifiableView @NotNull List<@NotNull String> getSupportedNMSVersions() {
        return SUPPORTED_NMS_VERSIONS;
    }

    @Nullable
    public static String getNMSVersionsRange() {
        return Versions.getNMSVersion().map(Versions::getNMSVersionsRange).orElse(null);
    }

    @Nullable
    @Contract(value="null -> null")
    public static String getNMSVersionsRange(String version) {
        return NMS_TO_FANCY.get(version);
    }

    public static @UnmodifiableView @Nullable List<@NotNull String> getNMSVersionsList() {
        return Versions.getNMSVersion().map(Versions::getNMSVersionsList).orElse(null);
    }

    @Nullable
    @Contract(value="null -> null")
    public static @UnmodifiableView @Nullable List<@NotNull String> getNMSVersionsList(String version) {
        return NMS_TO_VERSIONS.get(version);
    }

    @Contract(value="null -> null; !null -> !null")
    public static String removeInitialV(String string) {
        return string == null || string.isEmpty() || string.charAt(0) != 'v' ? string : string.substring(1);
    }

    private Versions() {
        throw new UnsupportedOperationException("Utility class.");
    }
}

