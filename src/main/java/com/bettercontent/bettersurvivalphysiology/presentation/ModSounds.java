package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SystemicSalienceMod.MOD_ID);
    private static final Map<AspectIdentity, RegistryObject<SoundEvent>> ASPECTS = new EnumMap<>(AspectIdentity.class);
    private static final RegistryObject<SoundEvent> BROKEN_TEMPO = SOUNDS.register("aspect.tempo_broken",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SystemicSalienceMod.MOD_ID, "aspect.tempo_broken")));

    static {
        for (AspectIdentity aspect : AspectIdentity.values()) {
            String path = "aspect." + aspect.name().toLowerCase();
            ASPECTS.put(aspect, SOUNDS.register(path,
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SystemicSalienceMod.MOD_ID, path))));
        }
    }

    private ModSounds() {}

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    public static SoundEvent get(AspectIdentity aspect) { return ASPECTS.get(aspect).get(); }
    public static SoundEvent brokenTempo() { return BROKEN_TEMPO.get(); }
}
