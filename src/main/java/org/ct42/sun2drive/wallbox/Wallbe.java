/*
 * Copyright 2018 Claas Thiele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.ct42.sun2drive.wallbox;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Adapts the charging controller of the Wallbe box (https://www.wallbe.de/)
 * using ModbusTCP
 */
public class Wallbe extends PhoenixContactChargeController {
    private static final int WALLBE_UNIT_ID = 255;
    public static final int WALLBE_DEFAULT_PORT = 502;

    public Wallbe(InetAddress address, int port) throws UnknownHostException {
        super(address, WALLBE_UNIT_ID, port);
    }

    public Wallbe(InetAddress address) throws UnknownHostException {
        this(address, WALLBE_DEFAULT_PORT);
    }
}
