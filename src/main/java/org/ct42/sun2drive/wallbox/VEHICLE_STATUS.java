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

import java.util.HashMap;
import java.util.Map;

/**
 * EV status regarding IEC 61851-1
 *
 */
public enum VEHICLE_STATUS {
    NOT_CONNECTED(65),
    CONNECTED(66),
    CHARGING(67),
    CHARGING_VENTILATED(68),
    ERROR_SHORT_CIRCUIT(69),
    ERROR_NOT_AVAILABLE(70);

    private int code;

    private VEHICLE_STATUS(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static Map<Integer, VEHICLE_STATUS> CODE2STATUS = new HashMap(6);

    static {
        CODE2STATUS.put(65, NOT_CONNECTED); // A
        CODE2STATUS.put(66, CONNECTED); // B
        CODE2STATUS.put(67, CHARGING); // C
        CODE2STATUS.put(68, CHARGING_VENTILATED); // D
        CODE2STATUS.put(69, ERROR_SHORT_CIRCUIT); // E
        CODE2STATUS.put(70, ERROR_NOT_AVAILABLE); // F
    }

    public static VEHICLE_STATUS getStatusForCode(int code) {
        return CODE2STATUS.get(code);
    }
}
