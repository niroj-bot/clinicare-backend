package com.clinicare.websocket;

public class SlotUpdateMessage {

    private Long clinicId;
    private Long slotId;
    private boolean isBooked;

    public SlotUpdateMessage() {}

    public SlotUpdateMessage(Long clinicId, Long slotId, boolean isBooked) {
        this.clinicId = clinicId;
        this.slotId   = slotId;
        this.isBooked = isBooked;
    }

    public Long getClinicId()  { return clinicId; }
    public Long getSlotId()    { return slotId; }
    public boolean isBooked()  { return isBooked; }

    public void setClinicId(Long clinicId)   { this.clinicId = clinicId; }
    public void setSlotId(Long slotId)       { this.slotId = slotId; }
    public void setBooked(boolean isBooked)  { this.isBooked = isBooked; }
}
