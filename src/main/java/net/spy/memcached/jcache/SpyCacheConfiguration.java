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

import javax.cache.CacheConfiguration;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import javax.cache.InvalidConfigurationException;
import javax.cache.OptionalFeature;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public final class SpyCacheConfiguration implements CacheConfiguration {

    private final AtomicBoolean readThrough;
    private final AtomicBoolean writeThrough;
    private final AtomicBoolean storeByValue;
    private final AtomicBoolean statisticsEnabled;
    private volatile IsolationLevel isolationLevel;
    private volatile Mode transactionMode;
    private final Duration[] timeToLive;
    private volatile SpyCache riCache;

    private SpyCacheConfiguration(boolean readThrough,
                                 boolean writeThrough,
                                 boolean storeByValue,
                                 boolean statisticsEnabled,
                                 IsolationLevel isolationLevel, Mode transactionMode,
                                 Duration[] timeToLive) {
        this.readThrough = new AtomicBoolean(readThrough);
        this.writeThrough = new AtomicBoolean(writeThrough);
        this.storeByValue = new AtomicBoolean(storeByValue);
        this.statisticsEnabled = new AtomicBoolean(statisticsEnabled);
        this.isolationLevel = isolationLevel;
        this.transactionMode = transactionMode;
        this.timeToLive = timeToLive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return readThrough.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return writeThrough.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreByValue() {
        return storeByValue.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatisticsEnabled() {
        return statisticsEnabled.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatisticsEnabled(boolean enableStatistics) {
        statisticsEnabled.set(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionEnabled() {
        return isolationLevel != null && transactionMode != null;
    }

    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public Mode getTransactionMode() {
        return transactionMode;
    }

    /**
     * Set the backing cache to expose more configuration.
     * @param riCache the backing cache.
     */
    void setRiCache(SpyCache riCache) {
        this.riCache = riCache;
    }

    /**
     * Gets the registered {@link javax.cache.CacheLoader}, if any.
     *
     * @return the {@link javax.cache.CacheLoader} or null if none has been set.
     */
    @Override
    public CacheLoader getCacheLoader() {
        return riCache.getCacheLoader();
    }

    /**
     * Gets the registered {@link javax.cache.CacheWriter}, if any.
     *
     * @return
     */
    @Override
    public CacheWriter getCacheWriter() {
        return riCache.getCacheWriter();
    }

    @Override
    public Duration getExpiry(ExpiryType type) {
        return timeToLive[type.ordinal()];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheConfiguration)) return false;

        CacheConfiguration that = (CacheConfiguration) o;

        if (getTransactionIsolationLevel() != that.getTransactionIsolationLevel()) return false;
        if (isReadThrough() != isReadThrough()) return false;
        if (isStatisticsEnabled() != that.isStatisticsEnabled()) return false;
        if (isStoreByValue()  != that.isStoreByValue()) return false;
        for (ExpiryType ttyType : ExpiryType.values()) {
            if (getExpiry(ttyType) != that.getExpiry(ttyType)) return false;
        }
        if (getTransactionMode() != that.getTransactionMode()) return false;
        if (isWriteThrough() != that.isWriteThrough()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = readThrough.hashCode();
        Boolean b;
        b = writeThrough.get();
        result = 31 * result + (b ? 1 : 0);
        b = storeByValue.get();
        result = 31 * result + (b ? 1 : 0);
        b = statisticsEnabled.get();
        result = 31 * result + (b ? 1 : 0);
        result = 31 * result + (isolationLevel != null ? isolationLevel.hashCode() : 0);
        result = 31 * result + (transactionMode != null ? transactionMode.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(timeToLive);
        return result;
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public static class Builder {
        private static final boolean DEFAULT_READ_THROUGH = false;
        private static final boolean DEFAULT_WRITE_THROUGH = false;
        private static final boolean DEFAULT_STORE_BY_VALUE = true;
        private static final boolean DEFAULT_STATISTICS_ENABLED = false;
        private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ETERNAL;
        private static final IsolationLevel DEFAULT_TRANSACTION_ISOLATION_LEVEL = null;
        private static final Mode DEFAULT_TRANSACTION_MODE = null;

        private boolean readThrough = DEFAULT_READ_THROUGH;
        private boolean writeThrough = DEFAULT_WRITE_THROUGH;
        private boolean storeByValue = DEFAULT_STORE_BY_VALUE;
        private boolean statisticsEnabled = DEFAULT_STATISTICS_ENABLED;
        private IsolationLevel isolationLevel = DEFAULT_TRANSACTION_ISOLATION_LEVEL;
        private Mode transactionMode = DEFAULT_TRANSACTION_MODE;
        private final Duration[] timeToLive;

        /**
         * Constructor
         */
        public Builder() {
            timeToLive = new Duration[ExpiryType.values().length];
            for (int i = 0; i < timeToLive.length; i++) {
                timeToLive[i] = DEFAULT_TIME_TO_LIVE;
            }
        }

        /**
         * Set whether read through is active
         * @param readThrough whether read through is active
         * @return this Builder instance
         */
        public Builder setReadThrough(boolean readThrough) {
            this.readThrough = readThrough;
            return this;
        }

        /**
         * Set whether write through is active
         *
         * @param writeThrough whether write through is active
         * @return this Builder instance
         */
        public Builder setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
            return this;
        }

        /**
         * Set whether store by value is active
         *
         * @param storeByValue whether store by value is active
         * @return this Builder instance
         */
        public Builder setStoreByValue(boolean storeByValue) {
            if (!storeByValue && !Caching.isSupported(OptionalFeature.STORE_BY_REFERENCE)) {
                throw new InvalidConfigurationException("storeByValue");
            }
            this.storeByValue = storeByValue;
            return this;
        }

        /**
         * Set whether statistics are enabled
         *
         * @param statisticsEnabled statistics are enabled
         * @return this Builder instance
         */
        public Builder setStatisticsEnabled(boolean statisticsEnabled) {
            this.statisticsEnabled = statisticsEnabled;
            return this;
        }

        /**
         * Set expiry
         * @param type ttl type
         * @param duration time to live
         * @return this Builder instance
         */
        public Builder setExpiry(ExpiryType type, Duration duration) {
            if (type == null) {
                throw new NullPointerException();
            }
            if (duration == null) {
                throw new NullPointerException();
            }
            this.timeToLive[type.ordinal()] =
                    duration.getDurationAmount() == 0 ? Duration.ETERNAL : duration;
            return this;
        }

        /**
         * Set whether transactions are enabled
         *
         * @param isolationLevel isolation level
         * @param mode the transactionMode
         * @return this Builder instance
         */
        public Builder setTransactionEnabled(IsolationLevel isolationLevel, Mode mode) {
            if (!Caching.isSupported(OptionalFeature.TRANSACTIONS)) {
                throw new InvalidConfigurationException("transactionsEnabled");
            }
            this.isolationLevel = isolationLevel;
            this.transactionMode = mode;
            return this;
        }

        /**
         * Create a new RICacheConfiguration instance.
         *
         * @return a new RICacheConfiguration instance
         */
        public SpyCacheConfiguration build() {
            return new SpyCacheConfiguration(readThrough, writeThrough, storeByValue, statisticsEnabled,
                    isolationLevel, transactionMode, timeToLive);
        }
    }
}
