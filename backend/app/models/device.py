"""设备 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class DeviceRegister(BaseModel):
    device_name: str = Field(..., min_length=1, max_length=100)
    device_type: str = Field(..., pattern="^(camera|smoke_sensor|gas_sensor|wearable|door_sensor|bed_sensor)$")
    manufacturer: Optional[str] = None
    model: Optional[str] = None
    elderly_id: str
    location: Optional[str] = None
    stream_url: Optional[str] = None
    capabilities: Optional[str] = None  # JSON string


class DeviceHeartbeat(BaseModel):
    device_id: str
    timestamp: int  # Unix timestamp ms


class DeviceStatusUpdate(BaseModel):
    status: str = Field(..., pattern="^(online|offline|fault|maintenance)$")


class DeviceResponse(BaseModel):
    id: str
    device_name: str
    device_type: str
    manufacturer: str = ""
    model: str = ""
    elderly_id: str
    location: str = ""
    stream_url: Optional[str] = None
    status: str = "online"
    last_heartbeat: Optional[int] = None
    capabilities: Optional[str] = None
    install_date: str = ""
    warranty_expiry: str = ""
    created_at: str = ""
