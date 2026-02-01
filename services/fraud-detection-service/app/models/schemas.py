from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional
from app.models.fraud_alert import RiskLevel


class TransactionEvent(BaseModel):
    transactionId: str
    fromAccount: str
    toAccount: Optional[str] = None
    type: str
    amount: float
    currency: str
    description: Optional[str] = None
    timestamp: datetime


class FraudCheckRequest(BaseModel):
    transaction_id: str
    account_number: str
    amount: float
    transaction_type: str
    to_account: Optional[str] = None


class FraudCheckResponse(BaseModel):
    transaction_id: str
    risk_score: float
    risk_level: RiskLevel
    is_fraud: bool
    reason: str
    recommendations: list[str]


class FraudAlertResponse(BaseModel):
    id: int
    alert_id: str
    transaction_id: str
    account_number: str
    risk_score: float
    risk_level: RiskLevel
    reason: str
    amount: float
    transaction_type: str
    is_blocked: bool
    created_at: datetime

    class Config:
        from_attributes = True