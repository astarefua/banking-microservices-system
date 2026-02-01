from sqlalchemy import Column, Integer, String, Float, DateTime, Enum as SQLEnum
from sqlalchemy.sql import func
from enum import Enum
from app.models.database import Base


class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class FraudAlert(Base):
    __tablename__ = "fraud_alerts"

    id = Column(Integer, primary_key=True, index=True)
    alert_id = Column(String(50), unique=True, nullable=False, index=True)
    transaction_id = Column(String(50), nullable=False, index=True)
    account_number = Column(String(20), nullable=False, index=True)
    risk_score = Column(Float, nullable=False)
    risk_level = Column(SQLEnum(RiskLevel), nullable=False)
    reason = Column(String(500), nullable=False)
    amount = Column(Float, nullable=False)
    transaction_type = Column(String(20), nullable=False)
    is_blocked = Column(Integer, default=0)  # 0 = false, 1 = true (SQLite doesn't have boolean)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    def __repr__(self):
        return f"<FraudAlert(alert_id={self.alert_id}, risk_score={self.risk_score})>"