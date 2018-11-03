/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.orecruncher.dsurround.registry.biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.orecruncher.dsurround.ModBase;
import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.client.handlers.EnvironStateHandler.EnvironState;
import org.orecruncher.dsurround.registry.Registry;
import org.orecruncher.dsurround.registry.RegistryManager;
import org.orecruncher.dsurround.registry.config.BiomeConfig;
import org.orecruncher.dsurround.registry.config.ModConfiguration;
import org.orecruncher.dsurround.registry.dimension.DimensionInfo;
import org.orecruncher.lib.math.MathStuff;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class BiomeRegistry extends Registry {

	private static final int INSIDE_Y_ADJUST = 3;

	public static final FakeBiome UNDERGROUND = new FakeBiome("Underground");
	public static final FakeBiome PLAYER = new FakeBiome("Player");
	public static final FakeBiome UNDERWATER = new FakeBiome("Underwater");
	public static final FakeBiome UNDEROCEAN = new FakeBiome("UnderOCN");
	public static final FakeBiome UNDERDEEPOCEAN = new FakeBiome("UnderDOCN");
	public static final FakeBiome UNDERRIVER = new FakeBiome("UnderRVR");
	public static final FakeBiome OUTERSPACE = new FakeBiome("OuterSpace");
	public static final FakeBiome CLOUDS = new FakeBiome("Clouds");
	public static final FakeBiome VILLAGE = new FakeBiome("Village");
	public static final FakeBiome BATTLE_MUSIC = new FakeBiome("BattleMusic");

	public BiomeInfo UNDERGROUND_INFO;
	public BiomeInfo PLAYER_INFO;
	public BiomeInfo UNDERRIVER_INFO;
	public BiomeInfo UNDEROCEAN_INFO;
	public BiomeInfo UNDERDEEPOCEAN_INFO;
	public BiomeInfo UNDERWATER_INFO;

	public BiomeInfo VILLAGE_INFO;
	public BiomeInfo CLOUDS_INFO;
	public BiomeInfo OUTERSPACE_INFO;
	public BiomeInfo BATTLE_MUSIC_INFO;
	public BiomeInfo WTF_INFO;

	// This is for cases when the biome coming in doesn't make sense
	// and should default to something to avoid crap.
	private static final FakeBiome WTF = new WTFFakeBiome();

	private final Map<ResourceLocation, BiomeInfo> registry = new Object2ObjectOpenHashMap<>();
	private final Map<String, String> biomeAliases = new Object2ObjectOpenHashMap<>();

	public BiomeRegistry(@Nonnull final Side side) {
		super(side);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void worldLoad(@Nonnull final WorldEvent.Load evt) {
		if (evt.getWorld().isRemote) {
			ModBase.log().info("Reloading biome registry due to world load...");
			init();
			for (final ModConfiguration mcf : RegistryManager.DATA.get())
				configure(mcf);
			initComplete();
		}
	}

	private void register(@Nonnull final Biome biome) {
		register(new BiomeHandler(biome));
	}

	private void register(@Nonnull final IBiome biome) {
		this.registry.put(biome.getKey(), new BiomeInfo(biome));
	}

	@Override
	public void init() {
		this.biomeAliases.clear();
		this.registry.clear();

		for (final String entry : ModOptions.biomes.biomeAliases) {
			final String[] parts = StringUtils.split(entry, "=");
			if (parts.length == 2) {
				this.biomeAliases.put(parts[0], parts[1]);
			}
		}

		ForgeRegistries.BIOMES.getValuesCollection().forEach(biome -> register(biome));

		// Add our fake biomes
		register(UNDERWATER);
		register(UNDEROCEAN);
		register(UNDERDEEPOCEAN);
		register(UNDERRIVER);
		register(PLAYER);
		register(VILLAGE);
		register(UNDERGROUND);
		register(CLOUDS);
		register(OUTERSPACE);
		register(BATTLE_MUSIC);

		this.UNDERGROUND_INFO = resolve(UNDERGROUND);
		this.PLAYER_INFO = resolve(PLAYER);
		this.UNDERRIVER_INFO = resolve(UNDERRIVER);
		this.UNDEROCEAN_INFO = resolve(UNDEROCEAN);
		this.UNDERDEEPOCEAN_INFO = resolve(UNDERDEEPOCEAN);

		this.UNDERWATER_INFO = resolve(UNDERWATER);
		this.VILLAGE_INFO = resolve(VILLAGE);
		this.CLOUDS_INFO = resolve(CLOUDS);
		this.OUTERSPACE_INFO = resolve(OUTERSPACE);
		this.BATTLE_MUSIC_INFO = resolve(BATTLE_MUSIC);

		// WTF is a strange animal
		register(WTF);
		this.WTF_INFO = resolve(WTF);
	}

	@Override
	public void configure(@Nonnull final ModConfiguration cfg) {
		cfg.biomeAlias.forEach((alias, biome) -> registerBiomeAlias(alias, biome));
		cfg.biomes.forEach(biome -> register(biome));
	}

	@Override
	public void initComplete() {
		if (ModOptions.logging.enableDebugLogging) {
			ModBase.log().info("*** BIOME REGISTRY ***");
			final List<BiomeInfo> info = new ArrayList<>(this.registry.values());
			Collections.sort(info);
			info.forEach(e -> ModBase.log().info(e.toString()));
		}

		// Free memory because we no longer need
		this.biomeAliases.clear();
	}

	@Nullable
	protected BiomeInfo resolve(@Nonnull final IBiome biome) {
		return this.registry.get(biome.getKey());
	}

	@Nullable
	private BiomeInfo resolve(@Nonnull final Biome biome) {
		return this.registry.get(BiomeHandler.getKey(biome));
	}

	@Nonnull
	public BiomeInfo get(@Nonnull final Biome biome) {
		// This shouldn't happen, but...
		if (biome == null)
			return this.WTF_INFO;

		BiomeInfo result = resolve(biome);
		if (result == null) {
			// Biome information should have been loaded/detected with the world load
			ModBase.log().warn("Unable to locate biome [%s] (%s)!", biome.getBiomeName(), biome.getClass().getName());
			result = this.WTF_INFO;
		}
		return result;
	}

	@Nonnull
	public BiomeInfo getPlayerBiome(@Nonnull final EntityPlayer player, final boolean getTrue) {
		final Biome biome = player.getEntityWorld().getBiome(new BlockPos(player.posX, 0, player.posZ));
		BiomeInfo info = get(biome);

		if (!getTrue) {
			if (player.isInsideOfMaterial(Material.WATER)) {
				if (info.isRiver())
					info = this.UNDERRIVER_INFO;
				else if (info.isDeepOcean())
					info = this.UNDERDEEPOCEAN_INFO;
				else if (info.isOcean())
					info = this.UNDEROCEAN_INFO;
				else
					info = this.UNDERWATER_INFO;
			} else {
				final DimensionInfo dimInfo = EnvironState.getDimensionInfo();
				final int theY = MathStuff.floor(player.posY);
				if ((theY + INSIDE_Y_ADJUST) <= dimInfo.getSeaLevel())
					info = this.UNDERGROUND_INFO;
				else if (theY >= dimInfo.getSpaceHeight())
					info = this.OUTERSPACE_INFO;
				else if (theY >= dimInfo.getCloudHeight())
					info = this.CLOUDS_INFO;
			}
		}

		return info;
	}

	final boolean isBiomeMatch(@Nonnull final BiomeConfig entry, @Nonnull final BiomeInfo info) {
		if (Pattern.matches(entry.biomeName, info.getBiomeName()))
			return true;
		final String alias = this.biomeAliases.get(info.getBiomeName());
		return alias == null ? false : Pattern.matches(entry.biomeName, alias);
	}

	public void registerBiomeAlias(@Nonnull final String alias, @Nonnull final String biome) {
		this.biomeAliases.put(alias, biome);
	}

	public void register(@Nonnull final BiomeConfig entry) {
		final BiomeMatcher matcher = BiomeMatcher.getMatcher(entry);
		this.registry.values().forEach(info -> {
			if (matcher.match(info))
				info.update(entry);
		});
	}
}