package tijos.framework.sensor.ec20;

public interface IEC20MqttEventListener {

    /**
     * Data arrived from the CDP server
     */
    void onMQTTPublishDataArrived(int client_idx, int msgId, String topic, String message);

    void onMQTTLinkLost(int client_idx, int error);


}
