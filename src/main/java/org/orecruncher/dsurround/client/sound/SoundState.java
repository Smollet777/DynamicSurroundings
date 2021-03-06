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

package org.orecruncher.dsurround.client.sound;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/*
 * Used by sounds that have long term state that gets manipulated
 * by the sound engine.  Intended to mitigate the constant polling
 * of the sound engine by mod logic to figure out what is happening
 * with a sound.
 */

@SideOnly(Side.CLIENT)
public enum SoundState {
	/*
	 * The sound was just created
	 */
	NONE(false, false),
	/*
	 * The sound is being held in an internal queue waiting for space down in the
	 * sound engine.
	 */
	QUEUED(true, false),
	/*
	 * Currently playing in the sound engine.
	 */
	PLAYING(true, false),
	/*
	 * The sound is in the sound engine DELAYED queue waiting to play.
	 */
	DELAYED(true, false),
	/*
	 * The sound play has been paused.
	 */
	PAUSED(true, false),
	/*
	 * The sound has completed it play.
	 */
	DONE(false, true),
	/*
	 * There was an error of some sort with the sound.
	 */
	ERROR(false, true);

	private final boolean isActive;
	private final boolean isTerminal;

	SoundState(final boolean active, final boolean terminal) {
		this.isActive = active;
		this.isTerminal = terminal;
	}

	/*
	 * A sound in this state is actively queued in the SoundManager.
	 */
	public boolean isActive() {
		return this.isActive;
	}

	/*
	 * A sound in this state is considered terminal. It was processed by the
	 * SoundManager and has reached a state where it has completed either because it
	 * ran it's course or ended in error.
	 */
	public boolean isTerminal() {
		return this.isTerminal;
	}
}
