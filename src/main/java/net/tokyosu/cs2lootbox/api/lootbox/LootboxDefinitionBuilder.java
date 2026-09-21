package net.tokyosu.cs2lootbox.api.lootbox;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.Util;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * KubeJS-friendly builder for one lootbox definition.
 */
@SuppressWarnings("unused")
public final class LootboxDefinitionBuilder {
    private final ResourceLocation id;

    private ResourceLocation caseItemId;
    private ResourceLocation keyItemId;
    private ResourceLocation model;
    private ResourceLocation texture;
    private ResourceLocation animation;
    private ResourceLocation keyTexture;
    private ResourceLocation itemJson = ResourceLocation.fromNamespaceAndPath(
            CS2LootBoxMod.MOD_ID, "item/lootbox_case");
    private ResourceLocation openSound = ModSounds.CASE_UNLOCK.getId();
    private ResourceLocation carouselTickSound = ModSounds.CRATE_ITEM_SCROLL.getId();
    private ResourceLocation rewardSound = ModSounds.CASE_AWARDED_COMMON.getId();
    private ResourceLocation uncommonSound = ModSounds.CASE_AWARDED_UNCOMMON.getId();
    private ResourceLocation rareSound = ModSounds.CASE_AWARDED_RARE.getId();
    private ResourceLocation mythicalSound = ModSounds.CASE_AWARDED_MYTHICAL.getId();
    private ResourceLocation classifiedSound = ModSounds.CASE_AWARDED_LEGENDARY.getId();
    private ResourceLocation covertSound = ModSounds.CASE_AWARDED_ANCIENT.getId();
    private ResourceLocation specialSound = ModSounds.CRATE_RESULT.getId();

    private ResourceLocation slotBorderTexture = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "textures/gui/slot_border.png");
    private ResourceLocation markerBarTexture = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "textures/gui/marker_bar.png");
    private ResourceLocation markerOuterCircleTexture = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "textures/gui/marker_outer_circle.png");
    private ResourceLocation markerInnerCircleTexture = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "textures/gui/marker_inner_circle.png");

    /** Default tuning used by every sound that does not define an explicit override. */
    private float defaultSoundVolume = 0.60F;
    private float defaultSoundPitch = 1.0F;

    private @Nullable LootboxDefinition.SoundTuning openSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning carouselTickSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning rewardSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning uncommonSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning rareSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning mythicalSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning classifiedSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning covertSoundTuning;
    private @Nullable LootboxDefinition.SoundTuning specialSoundTuning;

    private String fallAnimation = "fall";
    private String idleAnimation = "idle";
    private String openAnimation = "open";
    private String openIdleAnimation = "open_idle";
    private String itemIdleAnimation = "idle";

    private float offsetX = 70.0F;
    private float offsetY = 150.0F;
    private float scale = 300.0F;
    private float pitch = -3.0F;
    private float yaw = 160.0F;
    private float roll = -3.0F;
    private int blockLight = 12;
    private int skyLight = 12;

    private int caseStackSize = 16;
    private int keyStackSize = 64;
    private @Nullable String caseTranslationKey;
    private @Nullable String keyTranslationKey;
    private @Nullable String resultCollectionTranslationKey;
    private @Nullable ResourceLocation resultCollectionIconTexture;
    private final List<LootEntry> loot = new ArrayList<>();
    private final List<LegendaryLootBuilder> legendaryLoot = new ArrayList<>();

    public LootboxDefinitionBuilder(@NotNull String id) {
        this(parse(id, "kubejs"));
    }


    /**
     * Creates an editable copy of an existing immutable definition. Used by
     * KubeJS event.changeCase().
     */
    public LootboxDefinitionBuilder(@NotNull LootboxDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        this.id = definition.id();

        this.caseItemId = definition.caseItemId();
        this.keyItemId = definition.keyItemId();
        this.model = definition.model();
        this.texture = definition.texture();
        this.animation = definition.animation();
        this.keyTexture = definition.keyTexture();
        this.itemJson = definition.itemJson();
        this.openSound = definition.openSound();
        this.carouselTickSound = definition.carouselTickSound();
        this.rewardSound = definition.rewardSound();
        this.uncommonSound = definition.uncommonSound();
        this.rareSound = definition.rareSound();
        this.mythicalSound = definition.mythicalSound();
        this.classifiedSound = definition.classifiedSound();
        this.covertSound = definition.covertSound();
        this.specialSound = definition.specialSound();

        this.slotBorderTexture = definition.uiSkin().slotBorderTexture();
        this.markerBarTexture = definition.uiSkin().markerBarTexture();
        this.markerOuterCircleTexture = definition.uiSkin().markerOuterCircleTexture();
        this.markerInnerCircleTexture = definition.uiSkin().markerInnerCircleTexture();

        LootboxDefinition.SoundProfile soundProfile = definition.soundProfile();
        this.defaultSoundVolume = soundProfile.defaults().volume();
        this.defaultSoundPitch = soundProfile.defaults().pitch();
        this.openSoundTuning = soundProfile.open();
        this.carouselTickSoundTuning = soundProfile.carouselTick();
        this.rewardSoundTuning = soundProfile.reward();
        this.uncommonSoundTuning = soundProfile.uncommon();
        this.rareSoundTuning = soundProfile.rare();
        this.mythicalSoundTuning = soundProfile.mythical();
        this.classifiedSoundTuning = soundProfile.classified();
        this.covertSoundTuning = soundProfile.covert();
        this.specialSoundTuning = soundProfile.special();

        this.fallAnimation = definition.animations().fall();
        this.idleAnimation = definition.animations().idle();
        this.openAnimation = definition.animations().open();
        this.openIdleAnimation = definition.animations().openIdle();
        this.itemIdleAnimation = definition.animations().itemIdle();

        this.offsetX = definition.preview().offsetX();
        this.offsetY = definition.preview().offsetY();
        this.scale = definition.preview().scale();
        this.pitch = definition.preview().pitch();
        this.yaw = definition.preview().yaw();
        this.roll = definition.preview().roll();
        this.blockLight = definition.preview().blockLight();
        this.skyLight = definition.preview().skyLight();

        this.caseStackSize = definition.caseStackSize();
        this.keyStackSize = definition.keyStackSize();
        this.caseTranslationKey = definition.caseTranslationKey();
        this.keyTranslationKey = definition.keyTranslationKey();
        this.resultCollectionTranslationKey = definition.resultCollectionTranslationKey();
        this.resultCollectionIconTexture = definition.resultCollectionIconTexture();

        this.loot.addAll(definition.loot());
        for (LegendaryLoot legendary : definition.legendaryLoot()) {
            this.legendaryLoot.add(new LegendaryLootBuilder(this.id, legendary));
        }
    }

    public LootboxDefinitionBuilder(@NotNull ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "id");

        String namespace = id.getNamespace();
        String path = id.getPath();

        caseItemId = ResourceLocation.fromNamespaceAndPath(namespace, path + "_case");
        keyItemId = ResourceLocation.fromNamespaceAndPath(namespace, path + "_key");
        model = ResourceLocation.fromNamespaceAndPath(namespace, "geo/" + path + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(namespace, "textures/lootbox/" + path + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(namespace, "animations/" + path + ".animation.json");
        keyTexture = ResourceLocation.fromNamespaceAndPath(namespace, "item/" + path + "_key");
    }

    @Info("Sets the registered Minecraft item id used for the case.")
    public @NotNull LootboxDefinitionBuilder caseItem(@NotNull String id) {
        caseItemId = parse(id, this.id.getNamespace());
        return this;
    }

    @Info("Sets the registered Minecraft item id used for the matching key.")
    public @NotNull LootboxDefinitionBuilder keyItem(@NotNull String id) {
        keyItemId = parse(id, this.id.getNamespace());
        return this;
    }

    @Info("Sets the GeckoLib .geo.json resource, for example kubejs:geo/revolution.geo.json.")
    public @NotNull LootboxDefinitionBuilder model(@NotNull String resource) {
        model = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the diffuse crate texture, for example kubejs:textures/lootbox/revolution.png.")
    public @NotNull LootboxDefinitionBuilder texture(@NotNull String resource) {
        texture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the GeckoLib .animation.json resource.")
    public @NotNull LootboxDefinitionBuilder animation(@NotNull String resource) {
        animation = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the 2D key item texture. Use item texture syntax such as kubejs:item/revolution_key.")
    public @NotNull LootboxDefinitionBuilder keyTexture(@NotNull String resource) {
        keyTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the Minecraft item-model JSON used for first/third person, ground, GUI and fixed transforms. "
            + "The case geometry itself is still rendered by GeckoLib. Example: kubejs:item/lootbox_case.")
    public @NotNull LootboxDefinitionBuilder itemJson(@NotNull String resource) {
        itemJson = parse(resource, id.getNamespace());
        return this;
    }

    @Info(value = "Sets the default volume and pitch used by this case for sounds without an explicit per-sound override.", params = {
            @Param(name = "volume", value = "Volume from 0.0 (silent) to 1.0 (full volume)"),
            @Param(name = "pitch", value = "Pitch from 0.01 to 2.0; 1.0 is unchanged")
    })
    public @NotNull LootboxDefinitionBuilder defaultSound(float volume, float pitch) {
        LootboxDefinition.SoundTuning tuning = soundTuning(volume, pitch);
        defaultSoundVolume = tuning.volume();
        defaultSoundPitch = tuning.pitch();
        return this;
    }

    @Info("Sets only the default volume used by sounds without an explicit override. Range: 0.0 to 1.0.")
    public @NotNull LootboxDefinitionBuilder defaultSoundVolume(float volume) {
        defaultSoundVolume = soundTuning(volume, defaultSoundPitch).volume();
        return this;
    }

    @Info("Sets only the default pitch used by sounds without an explicit override. Range: 0.01 to 2.0.")
    public @NotNull LootboxDefinitionBuilder defaultSoundPitch(float pitch) {
        defaultSoundPitch = soundTuning(defaultSoundVolume, pitch).pitch();
        return this;
    }

    @Info(value = "Sets the sound played when the opening animation starts. It inherits defaultSound(...) tuning.", params = {
            @Param(name = "sound", value = "Registered sound event id, e.g. minecraft:block.chest.open")
    })
    public @NotNull LootboxDefinitionBuilder openSound(@NotNull String sound) {
        openSound = parse(sound, "minecraft");
        openSoundTuning = null;
        return this;
    }

    @Info("Sets opening sound id with an explicit volume and pitch override.")
    public @NotNull LootboxDefinitionBuilder openSound(@NotNull String sound, float volume, float pitch) {
        openSound = parse(sound, "minecraft");
        openSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the sound played each time the roulette bar passes an item. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder carouselTickSound(@NotNull String sound) {
        carouselTickSound = parse(sound, "minecraft");
        carouselTickSoundTuning = null;
        return this;
    }

    @Info("Sets the roulette tick sound id with an explicit volume and pitch override.")
    public @NotNull LootboxDefinitionBuilder carouselTickSound(@NotNull String sound, float volume, float pitch) {
        carouselTickSound = parse(sound, "minecraft");
        carouselTickSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets one fallback reward sound for Consumer through Restricted tiers. All affected tiers inherit defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder rewardSound(@NotNull String sound) {
        ResourceLocation parsed = parse(sound, "minecraft");
        rewardSound = parsed;
        uncommonSound = parsed;
        rareSound = parsed;
        mythicalSound = parsed;
        rewardSoundTuning = null;
        uncommonSoundTuning = null;
        rareSoundTuning = null;
        mythicalSoundTuning = null;
        return this;
    }

    @Info("Sets one fallback reward sound plus explicit volume/pitch for Consumer through Restricted tiers.")
    public @NotNull LootboxDefinitionBuilder rewardSound(@NotNull String sound, float volume, float pitch) {
        ResourceLocation parsed = parse(sound, "minecraft");
        LootboxDefinition.SoundTuning tuning = soundTuning(volume, pitch);
        rewardSound = parsed;
        uncommonSound = parsed;
        rareSound = parsed;
        mythicalSound = parsed;
        rewardSoundTuning = tuning;
        uncommonSoundTuning = tuning;
        rareSoundTuning = tuning;
        mythicalSoundTuning = tuning;
        return this;
    }

    @Info("Sets the reward sound for Industrial / uncommon rewards. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder uncommonSound(@NotNull String sound) {
        uncommonSound = parse(sound, "minecraft");
        uncommonSoundTuning = null;
        return this;
    }

    @Info("Sets the Industrial / uncommon reward sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder uncommonSound(@NotNull String sound, float volume, float pitch) {
        uncommonSound = parse(sound, "minecraft");
        uncommonSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the reward sound for Mil-Spec / rare rewards. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder rareSound(@NotNull String sound) {
        rareSound = parse(sound, "minecraft");
        rareSoundTuning = null;
        return this;
    }

    @Info("Sets the Mil-Spec / rare reward sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder rareSound(@NotNull String sound, float volume, float pitch) {
        rareSound = parse(sound, "minecraft");
        rareSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the reward sound for Restricted / mythical rewards. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder mythicalSound(@NotNull String sound) {
        mythicalSound = parse(sound, "minecraft");
        mythicalSoundTuning = null;
        return this;
    }

    @Info("Sets the Restricted / mythical reward sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder mythicalSound(@NotNull String sound, float volume, float pitch) {
        mythicalSound = parse(sound, "minecraft");
        mythicalSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the awarded sound for Classified / pink rewards when the reward UI closes. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder classifiedSound(@NotNull String sound) {
        classifiedSound = parse(sound, "minecraft");
        classifiedSoundTuning = null;
        return this;
    }

    @Info("Sets the Classified / pink awarded sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder classifiedSound(@NotNull String sound, float volume, float pitch) {
        classifiedSound = parse(sound, "minecraft");
        classifiedSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the awarded sound for Covert / red rewards when the reward UI closes. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder covertSound(@NotNull String sound) {
        covertSound = parse(sound, "minecraft");
        covertSoundTuning = null;
        return this;
    }

    @Info("Sets the Covert / red awarded sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder covertSound(@NotNull String sound, float volume, float pitch) {
        covertSound = parse(sound, "minecraft");
        covertSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the awarded sound for explicit Special / gold rewards when the reward UI closes. It inherits defaultSound(...) tuning.")
    public @NotNull LootboxDefinitionBuilder specialSound(@NotNull String sound) {
        specialSound = parse(sound, "minecraft");
        specialSoundTuning = null;
        return this;
    }

    @Info("Sets the Special / gold awarded sound with explicit volume and pitch.")
    public @NotNull LootboxDefinitionBuilder specialSound(@NotNull String sound, float volume, float pitch) {
        specialSound = parse(sound, "minecraft");
        specialSoundTuning = soundTuning(volume, pitch);
        return this;
    }

    @Info("Sets the texture used for the item rarity border in the loot list and roulette.")
    public @NotNull LootboxDefinitionBuilder slotBorderTexture(@NotNull String resource) {
        slotBorderTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the texture used for the center vertical roulette marker bar.")
    public @NotNull LootboxDefinitionBuilder markerBarTexture(@NotNull String resource) {
        markerBarTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the texture used for the large outer roulette circle.")
    public @NotNull LootboxDefinitionBuilder markerOuterCircleTexture(@NotNull String resource) {
        markerOuterCircleTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the texture used for the inner roulette circle.")
    public @NotNull LootboxDefinitionBuilder markerInnerCircleTexture(@NotNull String resource) {
        markerInnerCircleTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info("Sets the model X/Y offset inside the lootbox preview widget.")
    public @NotNull LootboxDefinitionBuilder position(float x, float y) {
        offsetX = x;
        offsetY = y;
        return this;
    }

    @Info("Sets the model scale inside the lootbox preview widget.")
    public @NotNull LootboxDefinitionBuilder scale(float value) {
        scale = value;
        return this;
    }

    @Info("Sets preview rotation in degrees: pitch, yaw, roll.")
    public @NotNull LootboxDefinitionBuilder rotation(float pitch, float yaw, float roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        return this;
    }

    @Info("Sets the fixed vanilla lightmap values used by the unlit Forge GUI render type (0-15 each).")
    public @NotNull LootboxDefinitionBuilder light(int blockLight, int skyLight) {
        this.blockLight = clampLight(blockLight);
        this.skyLight = clampLight(skyLight);
        return this;
    }

    @Info("Sets the GeckoLib stage names: fall, idle, open and optional open-idle. Pass null/empty for open-idle when the open animation holds its last frame.")
    public @NotNull LootboxDefinitionBuilder animations(@NotNull String fall, @NotNull String idle, @NotNull String open, @Nullable String openIdle) {
        this.fallAnimation = requireAnimationName(fall, "fall");
        this.idleAnimation = requireAnimationName(idle, "idle");
        this.openAnimation = requireAnimationName(open, "open");
        this.openIdleAnimation = optionalAnimationName(openIdle);
        return this;
    }

    @Info("Sets the GeckoLib stage names for a case with no open-idle animation. The open animation is held on its last frame while the UI waits to start the carousel.")
    public @NotNull LootboxDefinitionBuilder animations(@NotNull String fall, @NotNull String idle, @NotNull String open) {
        return animations(fall, idle, open, null);
    }

    @Info("Sets or clears the optional GeckoLib open-idle animation. Pass null or an empty string to use the held last frame of the open animation instead.")
    public @NotNull LootboxDefinitionBuilder openIdleAnimation(@Nullable String name) {
        this.openIdleAnimation = optionalAnimationName(name);
        return this;
    }

    @Info("Sets the looping animation used by the 3D case ItemStack.")
    public @NotNull LootboxDefinitionBuilder itemIdleAnimation(@NotNull String name) {
        itemIdleAnimation = requireAnimationName(name, "itemIdle");
        return this;
    }

    @Info("Sets the max stack size for the case item.")
    public @NotNull LootboxDefinitionBuilder caseStackSize(int size) {
        caseStackSize = clampStackSize(size);
        return this;
    }

    @Info("Sets the max stack size for the key item.")
    public @NotNull LootboxDefinitionBuilder keyStackSize(int size) {
        keyStackSize = clampStackSize(size);
        return this;
    }

    @Info("Sets the translation key used for the case item name, e.g. item.kubejs.revolution_case. If omitted, the normal item translation key is used.")
    public @NotNull LootboxDefinitionBuilder caseName(@NotNull String translationKey) {
        caseTranslationKey = requireTranslationKey(translationKey, "caseName");
        return this;
    }

    @Info("Sets the translation key used for the key item name, e.g. item.kubejs.revolution_key. If omitted, the normal item translation key is used.")
    public @NotNull LootboxDefinitionBuilder keyName(@NotNull String translationKey) {
        keyTranslationKey = requireTranslationKey(translationKey, "keyName");
        return this;
    }

    /** More explicit alias for {@link #caseName(String)}. */
    public @NotNull LootboxDefinitionBuilder caseTranslationKey(@NotNull String translationKey) {
        return caseName(translationKey);
    }

    /** More explicit alias for {@link #keyName(String)}. */
    public @NotNull LootboxDefinitionBuilder keyTranslationKey(@NotNull String translationKey) {
        return keyName(translationKey);
    }

    @Info("Sets the collection text shown below the won item. The value may be either literal text "
            + "(for example 'The Kilowatt Collection') or a translation key "
            + "(for example item.kubejs.kilowatt_collection).")
    public @NotNull LootboxDefinitionBuilder collectionText(@NotNull String textOrTranslationKey) {
        resultCollectionTranslationKey = requireTranslationKey(textOrTranslationKey, "collectionText");
        return this;
    }

    @Info("Sets the collection image shown beside the collection text, for example "
            + "kubejs:textures/gui/collections/kilowatt.png.")
    public @NotNull LootboxDefinitionBuilder collectionImage(@NotNull String resource) {
        resultCollectionIconTexture = parse(resource, id.getNamespace());
        return this;
    }

    @Info(value = "Sets both the collection text and image shown on the reward screen.", params = {
            @Param(name = "text", value = "Literal collection text or translation key"),
            @Param(name = "image", value = "Texture resource such as kubejs:textures/gui/collections/kilowatt.png")
    })
    public @NotNull LootboxDefinitionBuilder collection(@NotNull String text, @NotNull String image) {
        return collectionText(text).collectionImage(image);
    }

    /** Backwards-compatible alias for {@link #collectionText(String)}. */
    public @NotNull LootboxDefinitionBuilder resultCollectionName(@NotNull String translationKey) {
        return collectionText(translationKey);
    }

    /** Backwards-compatible alias for {@link #collectionImage(String)}. */
    public @NotNull LootboxDefinitionBuilder resultCollectionIcon(@NotNull String resource) {
        return collectionImage(resource);
    }

    /** Backwards-compatible alias for {@link #collectionText(String)}. */
    public @NotNull LootboxDefinitionBuilder resultInfo(@NotNull String translationKey) {
        return collectionText(translationKey);
    }

    /** Backwards-compatible alias for {@link #collectionImage(String)}. */
    public @NotNull LootboxDefinitionBuilder resultInfoIcon(@NotNull String resource) {
        return collectionImage(resource);
    }


    @Info(value = "Adds one weighted reward to this crate. Weight must be between 0 and 100 and is normalized against the crate total.", params = {
            @Param(name = "item", value = "Registered item id or #item_tag, for example minecraft:diamond or #forge:ingots/iron"),
            @Param(name = "weight", value = "Drop weight from 0 to 100")
    })
    public @NotNull LootboxDefinitionBuilder loot(@NotNull String item, double weight) {
        loot.add(createLootEntryBuilder(item, weight).build());
        return this;
    }

    @Info("Adds one weighted reward with a fixed stack count. Named separately so KubeJS/Rhino does not confuse an arrow-function callback with an integer overload.")
    public @NotNull LootboxDefinitionBuilder lootCount(@NotNull String item, double weight, int count) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        builder.count(count);
        loot.add(builder.build());
        return this;
    }

    @Info("Adds one weighted reward with an inclusive random stack-count range.")
    public @NotNull LootboxDefinitionBuilder loot(@NotNull String item, double weight, int minCount, int maxCount) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        builder.count(minCount, maxCount);
        loot.add(builder.build());
        return this;
    }

    @Info(value = "Adds and configures one weighted reward to this crate.", params = {
            @Param(name = "item", value = "Registered item id or #item_tag"),
            @Param(name = "weight", value = "Drop weight from 0 to 100"),
            @Param(name = "config", value = "Reward configuration callback")
    })
    public @NotNull LootboxDefinitionBuilder loot(@NotNull String item, double weight, @NotNull Consumer<LootEntryBuilder> config) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        Objects.requireNonNull(config, "config").accept(builder);
        loot.add(builder.build());
        return this;
    }

    @Info("Removes every normal loot entry copied/added to this case. Useful with event.changeCase() when replacing an existing loot table.")
    public @NotNull LootboxDefinitionBuilder clearLoot() {
        loot.clear();
        return this;
    }

    @Info("Removes all normal loot entries matching the supplied item id or #item_tag.")
    public @NotNull LootboxDefinitionBuilder removeLoot(@NotNull String item) {
        boolean tagSource = isItemTagSource(item);
        ResourceLocation itemId = parseLootSource(item, id.getNamespace());
        loot.removeIf(entry -> entry.itemTagSource() == tagSource && entry.itemId().equals(itemId));
        return this;
    }

    @Info("Removes every legendary panel copied/added to this case. Useful with event.changeCase() before declaring replacement legendary tables.")
    public @NotNull LootboxDefinitionBuilder clearLegendary() {
        legendaryLoot.clear();
        return this;
    }

    /** More explicit alias for clearLegendary(). */
    public @NotNull LootboxDefinitionBuilder clearLegendaryLoot() {
        return clearLegendary();
    }

    @Info("Creates one legendary/special panel with an absolute first-carousel chance from 0 to 100 percent. This chance is separate from normal loot weights. Panels are appended after all normal loot entries in declaration order.")
    public @NotNull LegendaryLootBuilder legendary(double weight) {
        LegendaryLootBuilder builder = new LegendaryLootBuilder(id, weight);
        legendaryLoot.add(builder);
        return builder;
    }

    /**
     * Backward-compatible overload. Prefer legendary(weight) so the absolute
     * first-carousel legendary chance is explicit.
     */
    @Info("Creates one legendary/special panel with a default first-carousel chance of 1 percent. Prefer legendary(weight) for explicit odds.")
    public @NotNull LegendaryLootBuilder legendary() {
        return legendary(1.0D);
    }

    public @NotNull ResourceLocation getId() {
        return id;
    }

    public @NotNull LootboxDefinition build() {
        List<LegendaryLoot> builtLegendaryLoot = legendaryLoot.stream()
                .map(LegendaryLootBuilder::build)
                .toList();

        double totalLegendaryChance = builtLegendaryLoot.stream()
                .mapToDouble(LegendaryLoot::weight)
                .filter(value -> Double.isFinite(value) && value > 0.0D)
                .sum();
        if (totalLegendaryChance > 100.0D + 0.0001D) {
            throw new IllegalStateException(
                    "Combined legendary first-carousel chance cannot exceed 100%, got " + totalLegendaryChance
            );
        }

        return new LootboxDefinition(
                id,
                Objects.requireNonNull(caseItemId, "caseItemId"),
                Objects.requireNonNull(keyItemId, "keyItemId"),
                Objects.requireNonNull(model, "model"),
                Objects.requireNonNull(texture, "texture"),
                Objects.requireNonNull(animation, "animation"),
                Objects.requireNonNull(keyTexture, "keyTexture"),
                Objects.requireNonNull(itemJson, "itemJson"),
                Objects.requireNonNull(openSound, "openSound"),
                Objects.requireNonNull(carouselTickSound, "carouselTickSound"),
                Objects.requireNonNull(rewardSound, "rewardSound"),
                Objects.requireNonNull(uncommonSound, "uncommonSound"),
                Objects.requireNonNull(rareSound, "rareSound"),
                Objects.requireNonNull(mythicalSound, "mythicalSound"),
                Objects.requireNonNull(classifiedSound, "classifiedSound"),
                Objects.requireNonNull(covertSound, "covertSound"),
                Objects.requireNonNull(specialSound, "specialSound"),
                new LootboxDefinition.SoundProfile(
                        soundTuning(defaultSoundVolume, defaultSoundPitch),
                        openSoundTuning,
                        carouselTickSoundTuning,
                        rewardSoundTuning,
                        uncommonSoundTuning,
                        rareSoundTuning,
                        mythicalSoundTuning,
                        classifiedSoundTuning,
                        covertSoundTuning,
                        specialSoundTuning
                ),
                new LootboxDefinition.AnimationSet(
                        requireAnimationName(fallAnimation, "fall"),
                        requireAnimationName(idleAnimation, "idle"),
                        requireAnimationName(openAnimation, "open"),
                        optionalAnimationName(openIdleAnimation),
                        requireAnimationName(itemIdleAnimation, "itemIdle")
                ),
                new LootboxDefinition.PreviewTransform(
                        offsetX,
                        offsetY,
                        scale,
                        pitch,
                        yaw,
                        roll,
                        clampLight(blockLight),
                        clampLight(skyLight)
                ),
                new LootboxDefinition.UiSkin(
                        Objects.requireNonNull(slotBorderTexture, "slotBorderTexture"),
                        Objects.requireNonNull(markerBarTexture, "markerBarTexture"),
                        Objects.requireNonNull(markerOuterCircleTexture, "markerOuterCircleTexture"),
                        Objects.requireNonNull(markerInnerCircleTexture, "markerInnerCircleTexture")
                ),
                clampStackSize(caseStackSize),
                clampStackSize(keyStackSize),
                caseTranslationKey != null ? caseTranslationKey : Util.makeDescriptionId("item", caseItemId),
                keyTranslationKey != null ? keyTranslationKey : Util.makeDescriptionId("item", keyItemId),
                resultCollectionTranslationKey,
                resultCollectionIconTexture,
                loot,
                builtLegendaryLoot
        );
    }


    private static @NotNull LootboxDefinition.SoundTuning soundTuning(float volume, float pitch) {
        return new LootboxDefinition.SoundTuning(volume, pitch);
    }


    private @NotNull LootEntryBuilder createLootEntryBuilder(@NotNull String source, double weight) {
        boolean tagSource = isItemTagSource(source);
        return new LootEntryBuilder(parseLootSource(source, id.getNamespace()), weight, tagSource);
    }

    private static boolean isItemTagSource(@NotNull String value) {
        return value != null && value.trim().startsWith("#");
    }

    private static @NotNull ResourceLocation parseLootSource(
            @NotNull String value,
            @NotNull String defaultNamespace) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Loot item/tag cannot be empty");
        }

        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Loot item tag cannot be empty");
            }
        }

        return parse(normalized, defaultNamespace);
    }


    private static @NotNull ResourceLocation parse(@NotNull String value, @NotNull String defaultNamespace) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resource location cannot be empty");
        }
        String normalized = value.trim();
        int separator = normalized.indexOf(':');

        if (separator >= 0) {
            String namespace = normalized.substring(0, separator);
            String path = normalized.substring(separator + 1);
            return ResourceLocation.fromNamespaceAndPath(namespace, path);
        }

        return ResourceLocation.fromNamespaceAndPath(defaultNamespace, normalized);
    }

    private static int clampLight(int value) {
        return Math.max(0, Math.min(15, value));
    }

    private static int clampStackSize(int value) {
        return Math.max(1, Math.min(64, value));
    }

    private static @NotNull String requireAnimationName(@NotNull String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lootbox animation name '" + field + "' cannot be empty");
        }
        return value.trim();
    }

    private static @Nullable String optionalAnimationName(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static @NotNull String requireTranslationKey(@NotNull String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lootbox translation key '" + field + "' cannot be empty");
        }
        return value.trim();
    }
}
