/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.sampler;

import me.lucko.spark.proto.SparkProtos.SamplerMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function for grouping threads together
 */
public enum ThreadGrouper {

    /**
     * Implementation of {@link ThreadGrouper} that just groups by thread name.
     */
    BY_NAME {
        @Override
        public String getGroup(long threadId, String threadName) {
            return threadName;
        }
    },

    /**
     * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
     * the thread originated from.
     *
     * <p>The regex pattern used to match pools expects a digit at the end of the thread name,
     * separated from the pool name with any of one or more of ' ', '-', or '#'.</p>
     */
    BY_POOL {
        private final Map<Long, String> cache = new ConcurrentHashMap<>();
        private final Pattern pattern = Pattern.compile("^(.*?)[-# ]+\\d+$");

        @Override
        public String getGroup(long threadId, String threadName) {
            String group = this.cache.get(threadId);
            if (group != null) {
                return group;
            }

            Matcher matcher = this.pattern.matcher(threadName);
            if (!matcher.matches()) {
                return threadName;
            }

            group = matcher.group(1).trim() + " (Combined)";
            this.cache.put(threadId, group); // we don't care about race conditions here
            return group;
        }
    },

    /**
     * Implementation of {@link ThreadGrouper} which groups all threads as one, under
     * the name "All".
     */
    AS_ONE {
        @Override
        public String getGroup(long threadId, String threadName) {
            return "All";
        }
    };

    /**
     * Gets the group for the given thread.
     *
     * @param threadId the id of the thread
     * @param threadName the name of the thread
     * @return the group
     */
    public abstract String getGroup(long threadId, String threadName);

    public static SamplerMetadata.DataAggregator.ThreadGrouper asProto(ThreadGrouper threadGrouper) {
        switch (threadGrouper) {
            case BY_NAME:
                return SamplerMetadata.DataAggregator.ThreadGrouper.BY_NAME;
            case BY_POOL:
                return SamplerMetadata.DataAggregator.ThreadGrouper.BY_POOL;
            case AS_ONE:
                return SamplerMetadata.DataAggregator.ThreadGrouper.AS_ONE;
            default:
                throw new AssertionError();
        }
    }

}
