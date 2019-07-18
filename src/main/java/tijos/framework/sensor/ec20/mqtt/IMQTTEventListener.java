package tijos.framework.sensor.ec20.mqtt;

public interface IMQTTEventListener {

    /**
     * Data arrived from the CDP server
     */
    void onPublishDataArrived(int msgId, String topic, String message);

    void onLinkLost(int error);


}
