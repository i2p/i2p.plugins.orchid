package com.subgraph.orchid;

//import com.google.common.util.concurrent.CycleDetectingLockFactory;
//import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Threading {
	static {
		// Default policy goes here. If you want to change this, use one of the static methods before
		// instantiating any orchid objects. The policy change will take effect only on new objects
		// from that point onwards.
		throwOnLockCycles();
	}

	//private static CycleDetectingLockFactory.Policy policy;
	//public static CycleDetectingLockFactory factory;

	public static ReentrantLock lock(String name) {
		//return factory.newReentrantLock(name);
		return new ReentrantLock();
	}

	public static void warnOnLockCycles() {
		//setPolicy(CycleDetectingLockFactory.Policies.WARN);
	}

	public static void throwOnLockCycles() {
		//setPolicy(CycleDetectingLockFactory.Policies.THROW);
	}

	public static void ignoreLockCycles() {
		//setPolicy(CycleDetectingLockFactory.Policies.DISABLED);
	}

/****
	public static void setPolicy(CycleDetectingLockFactory.Policy policy) {
		Threading.policy = policy;
		factory = CycleDetectingLockFactory.newInstance(policy);
	}

	public static CycleDetectingLockFactory.Policy getPolicy() {
		return policy;
	}
****/

	public static ExecutorService newPool(final String name) {
		//ThreadFactory factory = new ThreadFactoryBuilder()
		//		.setDaemon(true)
		//		.setNameFormat(name + "-%d").build();
		ThreadFactory factory = new CustomThreadFactory(name);
		return Executors.newCachedThreadPool(factory);
	}

	public static ScheduledExecutorService newSingleThreadScheduledPool(final String name) {
		//ThreadFactory factory = new ThreadFactoryBuilder()
		//		.setDaemon(true)
		//		.setNameFormat(name + "-%d").build();
		ThreadFactory factory = new CustomThreadFactory(name);
		return Executors.newSingleThreadScheduledExecutor(factory);
	}

	public static ScheduledExecutorService newScheduledPool(final String name) {
		//ThreadFactory factory = new ThreadFactoryBuilder()
		//		.setDaemon(true)
		//		.setNameFormat(name + "-%d").build();
		ThreadFactory factory = new CustomThreadFactory(name);
		return Executors.newScheduledThreadPool(1, factory);
	}

	/*
	 * Modified from Guava ThreadFactorBuilder
	 *
	 * Copyright (C) 2010 The Guava Authors
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
	 * in compliance with the License. You may obtain a copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software distributed under the License
	 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	 * or implied. See the License for the specific language governing permissions and limitations under
	 * the License.
	 */
	private static class CustomThreadFactory implements ThreadFactory {
		private final String fmt;
		private final AtomicLong count = new AtomicLong();

		public CustomThreadFactory(String name) {
			fmt = name + "-%d";
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			thread.setName(String.format(fmt, count.getAndIncrement()));
			thread.setDaemon(true);
			return thread;
		}
	}
}
