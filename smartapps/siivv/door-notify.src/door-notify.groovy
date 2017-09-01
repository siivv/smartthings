/**
 *  Door Notify
 *
 *  Copyright 2017 Scott Van Velsor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
    name: "door notify",
    namespace: "siivv",
    author: "siivv",
    description: "subscribe to all door events and notify subscribed user",
    category: "Convenience")

preferences {
    section("Monitor these locks:") {
        input "locks", "capability.lock", multiple: true, required: false
    }
    section("Monitor these garage doors:") {
        input "garages", "capability.garageDoorControl", multiple: true, required: false
    }
    section("Monitor these contact sensors:") {
        input "contacts", "capability.contactSensor", multiple: true, required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}";
    initialize();
}

def updated() {
    log.debug "Updated with settings: ${settings}";
    unsubscribe();
    unschedule();
    initialize();
}

def initialize() {
    unsubscribe();
    unschedule();
    doSubscriptions();
    runEvery15Minutes(metricPollingHandler);
}

def doSubscriptions() {
    subscribe(locks, "lock", notifyHandler);
    subscribe(garages, "garagedoor", notifyHandler);
    subscribe(contacts, "contact", notifyHandler);
}

def metricPollingHandler() {
    def mDevice = [:];
    settings.thermostats.eachWithIndex { dev,i ->
        mDevice = [:];
        mDevice['device'] = dev.displayName;
        mDevice['deviceId'] = dev.id;
        mDevice['location'] = dev.currentValue("location");
        mDevice['temperature'] = dev.currentValue("temperature");
        mDevice['humidity'] = dev.currentValue("humidity");
        mDevice['thermostatSetpoint'] = dev.currentValue("thermostatSetpoint");
        mDevice['thermostatMode'] = dev.currentValue("thermostatMode");
        mDevice['thermostatFanMOde'] = dev.currentValue("thermostatFanMode");
        mDevice['thermostatOperatingState'] = dev.currentValue("thermostatOperatingState");
        //log.debug("processed ${dev.displayName}");
        logMetric(metricJSONBuilder(mDevice));
    }
    settings.temperatures.eachWithIndex { dev,i ->
        if (!dev.hasCapability("Thermostat")) {
            mDevice = [:];
            mDevice['device'] = dev.displayName;
            mDevice['deviceId'] = dev.id;
            mDevice['temperature'] = dev.currentValue("temperature");
            logMetric(metricJSONBuilder(mDevice));
        }
    }
}

def metricJSONBuilder(mDevice) {
    def json = "{";

    mDevice.each { mkey, mvalue ->
        json += "\"${mkey}\":\"${mvalue}\",";
    }

    json += "\"location\":\"${location.name}\",";
    json += "\"event\":\"smartthings\"";
    json += "}"    
    log.debug("metricJSONBuilder: ${json}");

    return json;
}

def notifyHandler(evt) {
    def json = "{"
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"${evt.name}\":\"${evt.value}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"event\":\"smartthings\""
    json += "}"
    log.debug("metricHandler: ${json}");
    logMetric(json);
}

def logMetric(json) {
    def params = [
        uri: httpUrl,
        headers: [
            "x-api-key": xApiKey,
            "content-type": "application/json"
        ],
        body: json
    ]

    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        if (ex.statusCode != 200) {
            log.debug "Unexpected response error: ${ex.statusCode}"
            log.debug ex
            log.debug ex.response.contentType
        }
    }
}

/*
    log.debug("------------------------------");
    log.debug("date: ${evt.date}");
    log.debug("name: ${evt.name}");
    log.debug("displayName: ${evt.displayName}");
    log.debug("device: ${evt.device}");
    log.debug("deviceId: ${evt.deviceId}");
    log.debug("value: ${evt.value}");
    log.debug("isStateChange: ${evt.isStateChange()}");
    log.debug("id: ${evt.id}");
    log.debug("description: ${evt.description}");
    log.debug("descriptionText: ${evt.descriptionText}");
    log.debug("installedSmartAppId: ${evt.installedSmartAppId}");
    log.debug("isoDate: ${evt.isoDate}");
    log.debug("isDigital: ${evt.isDigital()}");
    log.debug("isPhysical: ${evt.isPhysical()}");
    log.debug("location: ${evt.location}");
    log.debug("locationId: ${evt.locationId}");
    log.debug("source: ${evt.source}");
    log.debug("unit: ${evt.unit}");
*/
