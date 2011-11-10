/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.spy.memcached.jcache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheManager;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import javax.cache.OptionalFeature;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import javax.transaction.UserTransaction;

/**
 */
public class SpyCacheManager implements CacheManager {
	private static final Logger LOGGER = Logger.getLogger("javax.cache");
	private final HashMap<String, Cache<?, ?>> caches = new HashMap<String, Cache<?, ?>>();
	private final HashSet<Class<?>> immutableClasses = new HashSet<Class<?>>();
	private String name;
	private ClassLoader classLoader;
	private volatile Status status;
	private String servers;

	/**
	 * Constructs a new RICacheManager with the specified name.
	 *
	 * @param classLoader
	 *            the ClassLoader that should be used in converting values into
	 *            Java Objects.
	 * @param name
	 *            the name of this cache manager
	 * @throws NullPointerException
	 *             if classLoader or name is null.
	 */
	public SpyCacheManager() {
		status = Status.UNINITIALISED;

	}

	public void start() {
		if (classLoader == null) {
			throw new NullPointerException("No classLoader specified");
		}
		if (name == null) {
			throw new NullPointerException("No name specified");
		}
		if (servers == null) {
			throw new NullPointerException("No servers specified");
		}
		status = Status.STARTED;
	}

	public void setClassLoader(ClassLoader classLoader) {
		if (classLoader == null) {
			throw new NullPointerException("No classLoader specified");
		}
		this.classLoader = classLoader;
	}

	public void setServers(String servers) {
		if (this.servers == null) {
			this.servers = servers;
		} else {
			this.servers = servers;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * The name returned will be that passed in to the constructor
	 * {@link #RICacheManager(String, ClassLoader)}
	 */
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null) {
			throw new NullPointerException("No name specified");
		}

		this.name = name;
	}

	/**
	 * Returns the status of this CacheManager.
	 * <p/>
	 *
	 * @return one of {@link javax.cache.Status}
	 */
	@Override
	public Status getStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <K, V> CacheBuilder<K, V> createCacheBuilder(String cacheName) {

		if (caches.get(cacheName) != null) {
			throw new CacheException("Cache " + cacheName + " already exists");
		}
		return new RICacheBuilder<K, V>(cacheName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <K, V> Cache<K, V> getCache(String cacheName) {
		if (status != Status.STARTED) {
			throw new IllegalStateException();
		}
		synchronized (caches) {
			/*
			 * Can't really verify that the K/V cast is safe but it is required
			 * by the API, using a local variable for the cast to allow for a
			 * minimal scoping of @SuppressWarnings
			 */
			@SuppressWarnings("unchecked")
			final Cache<K, V> cache = (Cache<K, V>) caches.get(cacheName);
			return cache;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <K, V> Set<Cache<K, V>> getCaches() {
		synchronized (caches) {
			HashSet<Cache<K, V>> set = new HashSet<Cache<K, V>>();
			for (Cache<?, ?> cache : caches.values()) {
				/*
				 * Can't really verify K/V cast but it is required by the API,
				 * using a local variable for the cast to allow for a minimal
				 * scoping of @SuppressWarnings
				 */
				@SuppressWarnings("unchecked")
				final Cache<K, V> castCache = (Cache<K, V>) cache;
				set.add(castCache);
			}
			return Collections.unmodifiableSet(set);
		}
	}

	private void addCacheInternal(Cache<?, ?> cache) {
		synchronized (caches) {
			if (caches.get(cache.getName()) != null) {
				throw new CacheException("Cache " + cache.getName()
						+ " already exists");
			}
			caches.put(cache.getName(), cache);
		}
		cache.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeCache(String cacheName) {
		if (status != Status.STARTED) {
			throw new IllegalStateException();
		}
		if (cacheName == null) {
			throw new NullPointerException();
		}
		Cache<?, ?> oldCache;
		synchronized (caches) {
			oldCache = caches.remove(cacheName);
		}
		if (oldCache != null) {
			oldCache.stop();
		}

		return oldCache != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserTransaction getUserTransaction() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupported(OptionalFeature optionalFeature) {
		return Caching.isSupported(optionalFeature);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addImmutableClass(Class<?> immutableClass) {
		if (immutableClass == null) {
			throw new NullPointerException();
		}
		immutableClasses.add(immutableClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		if (status != Status.STARTED) {
			throw new IllegalStateException();
		}
		synchronized (immutableClasses) {
			immutableClasses.clear();
		}
		ArrayList<Cache<?, ?>> cacheList;
		synchronized (caches) {
			cacheList = new ArrayList<Cache<?, ?>>(caches.values());
			caches.clear();
		}
		for (Cache<?, ?> cache : cacheList) {
			try {
				cache.stop();
			} catch (Exception e) {
				getLogger()
						.log(Level.WARNING, "Error stopping cache: " + cache);
			}
		}
		status = Status.STOPPED;
	}

	@Override
	public <T> T unwrap(java.lang.Class<T> cls) {
		if (cls.isAssignableFrom(this.getClass())) {
			return cls.cast(this);
		}

		throw new IllegalArgumentException("Unwapping to " + cls
				+ " is not a supported by this implementation");
	}

	/**
	 * Obtain the logger.
	 *
	 * @return the logger.
	 */
	Logger getLogger() {
		return LOGGER;
	}

	/**
	 * RI implementation of {@link CacheBuilder}
	 *
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 */
	private class RICacheBuilder<K, V> implements CacheBuilder<K, V> {
		private final SpyCache.Builder<K, V> cacheBuilder;

		public RICacheBuilder(String cacheName) {
			cacheBuilder = new SpyCache.Builder<K, V>(cacheName, name,
					immutableClasses, classLoader, servers);
		}

		@Override
		public Cache<K, V> build() {
			Cache<K, V> cache = cacheBuilder.build();
			addCacheInternal(cache);
			return cache;
		}

		@Override
		public CacheBuilder<K, V> setCacheLoader(CacheLoader<K, V> cacheLoader) {
			cacheBuilder.setCacheLoader(cacheLoader);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setCacheWriter(CacheWriter<K, V> cacheWriter) {
			cacheBuilder.setCacheWriter(cacheWriter);
			return this;
		}

		@Override
		public CacheBuilder<K, V> registerCacheEntryListener(
				CacheEntryListener<K, V> listener, NotificationScope scope,
				boolean synchronous) {
			cacheBuilder.registerCacheEntryListener(listener, scope,
					synchronous);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setStoreByValue(boolean storeByValue) {
			cacheBuilder.setStoreByValue(storeByValue);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setTransactionEnabled(
				IsolationLevel isolationLevel, Mode mode) {
			cacheBuilder.setTransactionEnabled(isolationLevel, mode);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setStatisticsEnabled(boolean enableStatistics) {
			cacheBuilder.setStatisticsEnabled(enableStatistics);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setReadThrough(boolean readThrough) {
			cacheBuilder.setReadThrough(readThrough);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setWriteThrough(boolean writeThrough) {
			cacheBuilder.setWriteThrough(writeThrough);
			return this;
		}

		@Override
		public CacheBuilder<K, V> setExpiry(CacheConfiguration.ExpiryType type,
				CacheConfiguration.Duration duration) {
			cacheBuilder.setExpiry(type, duration);
			return this;
		}
	}
}
