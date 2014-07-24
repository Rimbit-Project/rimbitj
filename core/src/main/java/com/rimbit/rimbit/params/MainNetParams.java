/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rimbit.rimbit.params;

import com.rimbit.rimbit.core.NetworkParameters;
import com.rimbit.rimbit.core.Sha256Hash;
import com.rimbit.rimbit.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends NetworkParameters {
    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 188;
        addressHeader = 60;
        p2shHeader = 122;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 8708;
        packetMagic = 0xaaf0e2bfL;
        genesisBlock.setDifficultyTarget(0x1e0fffffL);
        genesisBlock.setTime(1401202000L);
        genesisBlock.setNonce(1575379);
        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 30;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("3cdd9c2facce405f5cc220fb21a10e493041451c463a22e1ff6fe903fc5769fc"), genesisHash);


        checkpoints.put(1, new Sha256Hash("00000df334b33e08c9e10b351a4be5f5f11d0ce7c3b882150b0514174a23862a"));

        dnsSeeds = new String[] {
                "seed.rimbitx.com", "seed.rimbit.net"
        };
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
