package com.bettercontent.bettersurvivalphysiology.clothing;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Four ordinary armor-slot garments. Insulation is owned by Cold Sweat's native item config. */
public final class WoolClothing {
    private WoolClothing() {}
    private static final ArmorMaterial FABRIC = new ArmorMaterial() {
        @Override public int getDurabilityForType(ArmorItem.Type type) {
            return ArmorMaterials.LEATHER.getDurabilityForType(type);
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { return 0; }
        @Override public int getEnchantmentValue() { return ArmorMaterials.LEATHER.getEnchantmentValue(); }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ItemTags.WOOL); }
        @Override public String getName() { return SystemicSalienceMod.MOD_ID + ":wool"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    };
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SystemicSalienceMod.MOD_ID);
    public static final RegistryObject<Item> HELMET = garment("wool_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHESTPLATE = garment("wool_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> LEGGINGS = garment("wool_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOOTS = garment("wool_boots", ArmorItem.Type.BOOTS);

    private static RegistryObject<Item> garment(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(FABRIC, type, new Item.Properties()));
    }
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
