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

package org.blockartistry.mod.DynSurround.client.fx.particle.system;

import java.util.LinkedHashSet;

import javax.annotation.Nonnull;

import org.blockartistry.mod.DynSurround.client.fx.particle.ParticleBase;
import org.blockartistry.mod.DynSurround.client.fx.particle.ParticleHelper;
import org.blockartistry.mod.DynSurround.client.fx.particle.ParticleMoteAdapter;
import org.blockartistry.mod.DynSurround.client.fx.particle.mote.IParticleMote;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ParticleSystem extends ParticleBase {

	protected static final Predicate<IParticleMote> REMOVE_CRITERIA = new Predicate<IParticleMote>() {
		@Override
		public boolean apply(final IParticleMote input) {
			return !input.isAlive();
		}
	};

	protected final int fxLayer;
	protected final BlockPos position;

	protected final LinkedHashSet<IParticleMote> myParticles = new LinkedHashSet<IParticleMote>();
	protected int particleLimit;

	protected ParticleSystem(final World worldIn, final double posXIn, final double posYIn, final double posZIn) {
		this(0, worldIn, posXIn, posYIn, posZIn);

		setParticleLimit(6);
	}

	protected ParticleSystem(final int renderPass, final World worldIn, final double posXIn, final double posYIn,
			final double posZIn) {
		super(worldIn, posXIn, posYIn, posZIn);

		this.fxLayer = renderPass;
		this.position = new BlockPos(this.posX, this.posY, this.posZ);
	}

	@Nonnull
	public BlockPos getPos() {
		return this.position;
	}

	public void setParticleLimit(final int limit) {
		this.particleLimit = limit;
	}

	public int getCurrentParticleCount() {
		return this.myParticles.size();
	}

	public int getParticleLimit() {
		final int setting = Minecraft.getMinecraft().gameSettings.particleSetting;
		if (setting == 2)
			return 0;
		return setting == 0 ? this.particleLimit : this.particleLimit / 2;
	}

	public void addParticle(final Particle particle) {
		this.addParticle(new ParticleMoteAdapter(particle));
	}
	
	public void addParticle(final IParticleMote particle) {
		if (particle.getFXLayer() != this.getFXLayer()) {
			throw new RuntimeException("Invalid particle for fx layer!");
		} else if (this.myParticles.size() < getParticleLimit()) {
			this.myParticles.add(particle);
		}
	}

	@Override
	public void renderParticle(final VertexBuffer buffer, final Entity entityIn, final float partialTicks,
			final float rotX, final float rotZ, final float rotYZ, final float rotXY, final float rotXZ) {
		for (final IParticleMote p : this.myParticles)
			if(p.isAlive())
				p.renderParticle(buffer, entityIn, partialTicks, rotX, rotZ, rotYZ, rotXY, rotXZ);
	}

	/**
	 * By default a system will stay alive indefinitely until the
	 * ParticleSystemManager kills it. Override to provide termination
	 * capability.
	 */
	public boolean shouldDie() {
		return false;
	}

	/**
	 * Indicates whether to transfer the particle list over to the regular
	 * minecraft particle manager when the system dies. Useful for things like
	 * fire jets where the flames need to die out naturally.
	 */
	public boolean moveParticlesOnDeath() {
		return true;
	}

	protected void moveParticles() {
		if (!moveParticlesOnDeath())
			return;

		for (final IParticleMote p : this.myParticles)
			if (p.isAlive() && p.moveParticleOnExpire())
				ParticleHelper.addParticle(p.getParticle());
	}

	/*
	 * Perform any cleanup activities prior to dying.
	 */
	protected void cleanUp() {
		this.myParticles.clear();
	}
	
	@Override
	public void onUpdate() {
		// Let the system mull over what it wants to do
		this.think();

		if (this.shouldDie()) {
			this.moveParticles();
			this.setExpired();
			this.cleanUp();
		}

		if (!this.isAlive())
			return;

		// Iterate through the list doing updates
		for (final IParticleMote p : this.myParticles)
			p.onUpdate();

		// Remove the dead ones
		Iterables.removeIf(this.myParticles, REMOVE_CRITERIA);
		
		this.soundUpdate();
	}
	
	// Override to provide sound for the particle effect.  Will be invoked
	// whenever the particle system is updated by the particle manager.
	protected void soundUpdate() {
		
	}

	// Override to provide some sort of intelligence to the system. The
	// logic can do things like add new particles, remove old ones, update
	// positions, etc. Will be invoked during the systems onUpdate()
	// call.
	public abstract void think();

	@Override
	public int getFXLayer() {
		return this.fxLayer;
	}

}